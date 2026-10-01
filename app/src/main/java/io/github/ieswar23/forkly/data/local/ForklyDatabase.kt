package io.github.ieswar23.forkly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.ieswar23.forkly.data.local.dao.AddressDao
import io.github.ieswar23.forkly.data.local.dao.CartDao
import io.github.ieswar23.forkly.data.local.dao.MenuDao
import io.github.ieswar23.forkly.data.local.dao.OrderDao
import io.github.ieswar23.forkly.data.local.dao.RestaurantDao
import io.github.ieswar23.forkly.data.local.entity.AddressEntity
import io.github.ieswar23.forkly.data.local.entity.BannerEntity
import io.github.ieswar23.forkly.data.local.entity.CartItemEntity
import io.github.ieswar23.forkly.data.local.entity.CartMetaEntity
import io.github.ieswar23.forkly.data.local.entity.CategoryEntity
import io.github.ieswar23.forkly.data.local.entity.FavoriteEntity
import io.github.ieswar23.forkly.data.local.entity.MenuItemEntity
import io.github.ieswar23.forkly.data.local.entity.OrderEntity
import io.github.ieswar23.forkly.data.local.entity.OrderItemEntity
import io.github.ieswar23.forkly.data.local.entity.RecentSearchEntity
import io.github.ieswar23.forkly.data.local.entity.RestaurantEntity

@Database(
    entities = [
        RestaurantEntity::class,
        MenuItemEntity::class,
        BannerEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        RecentSearchEntity::class,
        CartItemEntity::class,
        CartMetaEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        AddressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ForklyDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
    abstract fun menuDao(): MenuDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun addressDao(): AddressDao

    companion object {
        const val NAME = "forkly.db"
    }
}
