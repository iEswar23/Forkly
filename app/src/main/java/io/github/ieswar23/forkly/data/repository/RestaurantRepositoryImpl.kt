package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.data.local.dao.MenuDao
import io.github.ieswar23.forkly.data.local.dao.RestaurantDao
import io.github.ieswar23.forkly.data.local.entity.FavoriteEntity
import io.github.ieswar23.forkly.data.local.entity.RecentSearchEntity
import io.github.ieswar23.forkly.data.mapper.toDomain
import io.github.ieswar23.forkly.data.mapper.toEntities
import io.github.ieswar23.forkly.data.mapper.toEntity
import io.github.ieswar23.forkly.data.remote.ForklyApi
import io.github.ieswar23.forkly.di.ApplicationScope
import io.github.ieswar23.forkly.di.IoDispatcher
import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.DishResult
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.MenuSection
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    private val api: ForklyApi,
    private val restaurantDao: RestaurantDao,
    private val menuDao: MenuDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : RestaurantRepository {

    private val favoriteIds: Flow<Set<String>> = restaurantDao.observeFavoriteIds().map { it.toSet() }

    override val restaurants: Flow<List<Restaurant>> =
        combine(restaurantDao.observeAll(), favoriteIds) { entities, favs ->
            entities.map { it.toDomain(isFavorite = it.id in favs) }
        }.flowOn(io)

    override val banners: Flow<List<Banner>> =
        restaurantDao.observeBanners().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override val categories: Flow<List<Category>> =
        restaurantDao.observeCategories().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override val favorites: Flow<List<Restaurant>> =
        combine(restaurantDao.observeAll(), restaurantDao.observeFavoriteIds()) { entities, ids ->
            val byId = entities.associateBy { it.id }
            ids.mapNotNull { byId[it]?.toDomain(isFavorite = true) }
        }.flowOn(io)

    override val recentSearches: Flow<List<String>> =
        restaurantDao.observeRecentSearches(RECENT_SEARCH_LIMIT).map { list -> list.map { it.query } }

    override fun observeRestaurant(id: String): Flow<Restaurant?> =
        combine(restaurantDao.observeById(id), favoriteIds) { entity, favs ->
            entity?.toDomain(isFavorite = id in favs)
        }.flowOn(io)

    override fun observeMenu(restaurantId: String): Flow<List<MenuSection>> =
        menuDao.observeMenu(restaurantId)
            .distinctUntilChanged()
            .map { entities ->
                entities.map { it.toDomain() }
                    .groupBy { it.section }
                    .map { (section, items) -> MenuSection(section, items) }
            }
            .flowOn(io)

    override fun searchDishes(query: String): Flow<List<DishResult>> {
        val needle = query.trim()
        if (needle.length < 2) return flowOf(emptyList())
        return combine(menuDao.search(needle, DISH_RESULT_LIMIT), restaurantDao.observeAll()) { items, restaurants ->
            val byId = restaurants.associateBy { it.id }
            items.mapNotNull { entity ->
                val restaurant = byId[entity.restaurantId] ?: return@mapNotNull null
                DishResult(entity.toDomain(), restaurant.name, restaurant.emoji)
            }
        }.flowOn(io)
    }

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        try {
            coroutineScope {
                val restaurants = async { api.getRestaurants() }
                val banners = async { api.getBanners() }
                val categories = async { api.getCategories() }
                restaurantDao.replaceAll(restaurants.await().mapIndexed { i, dto -> dto.toEntity(i) })
                restaurantDao.replaceBanners(banners.await().mapIndexed { i, dto -> dto.toEntity(i) })
                restaurantDao.replaceCategories(categories.await().mapIndexed { i, dto -> dto.toEntity(i) })
            }
            prefetchMenus()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Warms the menu cache in the background so dish search works right away. */
    private fun prefetchMenus() {
        appScope.launch(io) {
            val ids = restaurantDao.getAllIds()
            val permits = Semaphore(MENU_PREFETCH_PARALLELISM)
            ids.map { id ->
                async { permits.withPermit { ensureMenu(id) } }
            }.awaitAll()
        }
    }

    override suspend fun ensureMenu(restaurantId: String, forceRefresh: Boolean): Result<Unit> = withContext(io) {
        try {
            if (!forceRefresh && menuDao.countFor(restaurantId) > 0) return@withContext Result.success(Unit)
            val menu = api.getMenu(restaurantId)
            menuDao.replaceMenu(restaurantId, menu.toEntities())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMenuItems(ids: List<String>): List<MenuItem> = withContext(io) {
        menuDao.getByIds(ids).map { it.toDomain() }
    }

    override suspend fun toggleFavorite(restaurantId: String) = withContext(io) {
        if (restaurantDao.isFavorite(restaurantId)) {
            restaurantDao.removeFavorite(restaurantId)
        } else {
            restaurantDao.addFavorite(FavoriteEntity(restaurantId, clock.now()))
        }
    }

    override suspend fun saveRecentSearch(query: String) = withContext(io) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) restaurantDao.upsertRecentSearch(RecentSearchEntity(trimmed, clock.now()))
    }

    override suspend fun clearRecentSearches() = withContext(io) { restaurantDao.clearRecentSearches() }

    private companion object {
        const val RECENT_SEARCH_LIMIT = 6
        const val DISH_RESULT_LIMIT = 30
        const val MENU_PREFETCH_PARALLELISM = 4
    }
}
