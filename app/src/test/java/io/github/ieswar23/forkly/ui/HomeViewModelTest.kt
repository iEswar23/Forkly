package io.github.ieswar23.forkly.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.MainDispatcherRule
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption
import io.github.ieswar23.forkly.fakes.FakeAddressRepository
import io.github.ieswar23.forkly.fakes.FakePreferencesRepository
import io.github.ieswar23.forkly.fakes.FakeRestaurantRepository
import io.github.ieswar23.forkly.fakes.TestData.restaurant
import io.github.ieswar23.forkly.ui.home.HomeFeed
import io.github.ieswar23.forkly.ui.home.HomeViewModel
import io.github.ieswar23.forkly.util.UiState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val alpha = restaurant("alpha", rating = 4.5, deliveryTimeMins = 35, costForTwo = 500, offerText = "50% OFF", cuisines = listOf("Biryani"))
    private val bean = restaurant("bean", rating = 3.9, deliveryTimeMins = 20, costForTwo = 600, cuisines = listOf("Cafe"))
    private val curry = restaurant("curry", rating = 4.1, deliveryTimeMins = 25, costForTwo = 200, isPureVeg = true, offerText = "Free delivery", cuisines = listOf("South Indian"))
    private val dosa = restaurant("dosa", rating = 4.7, deliveryTimeMins = 40, costForTwo = 300, isPureVeg = true, cuisines = listOf("South Indian", "Breakfast"))

    private val repository = FakeRestaurantRepository(listOf(alpha, bean, curry, dosa))

    private fun TestScope.createViewModel(): HomeViewModel {
        val vm = HomeViewModel(repository, FakeAddressRepository(), FakePreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    private fun HomeViewModel.feed(): HomeFeed = (uiState.value.feed as UiState.Success).data
    private fun HomeViewModel.ids(): List<String> = feed().restaurants.map { it.id }

    @Test
    fun `refreshes on start and shows every restaurant`() = runTest {
        val vm = createViewModel()

        assertThat(repository.refreshCount).isEqualTo(1)
        assertThat(vm.ids()).containsExactly("alpha", "bean", "curry", "dosa")
        assertThat(vm.feed().totalRestaurants).isEqualTo(4)
        assertThat(vm.uiState.value.address?.locality).isEqualTo("Banjara Hills")
    }

    @Test
    fun `failed refresh with no cache shows an error, retry recovers`() = runTest {
        repository.refreshError = IOException("No internet connection")
        val vm = createViewModel()

        assertThat(vm.uiState.value.feed).isEqualTo(UiState.Error("No internet connection"))

        repository.refreshError = null
        vm.refresh()
        assertThat(vm.uiState.value.feed).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `failed refresh keeps showing cached restaurants`() = runTest {
        repository.seedCache(listOf(alpha, bean))
        repository.refreshError = IOException("timeout")

        val vm = createViewModel()

        assertThat(vm.ids()).containsExactly("alpha", "bean")
        assertThat(vm.uiState.value.isRefreshing).isFalse()
    }

    @Test
    fun `veg only filter keeps pure veg restaurants`() = runTest {
        val vm = createViewModel()
        vm.toggleVegOnly()
        assertThat(vm.ids()).containsExactly("curry", "dosa")
    }

    @Test
    fun `rating filter removes places under 4 stars`() = runTest {
        val vm = createViewModel()
        vm.toggleRating4Plus()
        assertThat(vm.ids()).doesNotContain("bean")
        assertThat(vm.ids()).hasSize(3)
    }

    @Test
    fun `offers filter keeps restaurants running an offer`() = runTest {
        val vm = createViewModel()
        vm.toggleOffersOnly()
        assertThat(vm.ids()).containsExactly("alpha", "curry")
    }

    @Test
    fun `fast delivery filter keeps places delivering within 30 minutes`() = runTest {
        val vm = createViewModel()
        vm.toggleFastDelivery()
        assertThat(vm.ids()).containsExactly("bean", "curry")
    }

    @Test
    fun `sort by rating orders best first`() = runTest {
        val vm = createViewModel()
        vm.applyFiltersAndSort(RestaurantFilters(), SortOption.RATING)
        assertThat(vm.ids()).containsExactly("dosa", "alpha", "curry", "bean").inOrder()
    }

    @Test
    fun `sort by delivery time orders fastest first`() = runTest {
        val vm = createViewModel()
        vm.applyFiltersAndSort(RestaurantFilters(), SortOption.DELIVERY_TIME)
        assertThat(vm.ids()).containsExactly("bean", "curry", "alpha", "dosa").inOrder()
    }

    @Test
    fun `sort by cost works in both directions`() = runTest {
        val vm = createViewModel()
        vm.applyFiltersAndSort(RestaurantFilters(), SortOption.COST_LOW_TO_HIGH)
        assertThat(vm.ids()).containsExactly("curry", "dosa", "alpha", "bean").inOrder()

        vm.applyFiltersAndSort(RestaurantFilters(), SortOption.COST_HIGH_TO_LOW)
        assertThat(vm.ids()).containsExactly("bean", "alpha", "dosa", "curry").inOrder()
    }

    @Test
    fun `filters and sort combine`() = runTest {
        val vm = createViewModel()
        vm.applyFiltersAndSort(RestaurantFilters(vegOnly = true), SortOption.COST_HIGH_TO_LOW)
        assertThat(vm.ids()).containsExactly("dosa", "curry").inOrder()
        assertThat(vm.uiState.value.hasActiveRefinements).isTrue()
    }

    @Test
    fun `selecting a category filters by cuisine and tapping it again clears it`() = runTest {
        val vm = createViewModel()

        vm.selectCategory("South Indian")
        assertThat(vm.ids()).containsExactly("curry", "dosa")
        assertThat(vm.uiState.value.selectedCategory).isEqualTo("South Indian")

        vm.selectCategory("South Indian")
        assertThat(vm.ids()).hasSize(4)
        assertThat(vm.uiState.value.selectedCategory).isNull()
    }

    @Test
    fun `clearing refinements restores the default feed`() = runTest {
        val vm = createViewModel()
        vm.selectCategory("Cafe")
        vm.toggleVegOnly()
        assertThat(vm.ids()).isEmpty()

        vm.clearRefinements()

        assertThat(vm.ids()).hasSize(4)
        assertThat(vm.uiState.value.hasActiveRefinements).isFalse()
    }

    @Test
    fun `top rated rail only shows highly rated nearby restaurants`() = runTest {
        val vm = createViewModel()
        assertThat(vm.feed().topRated.map { it.id }).containsExactly("dosa", "alpha").inOrder()
    }

    @Test
    fun `toggling a favourite is reflected in the feed`() = runTest {
        val vm = createViewModel()
        vm.toggleFavorite("curry")
        assertThat(vm.feed().restaurants.first { it.id == "curry" }.isFavorite).isTrue()
    }
}
