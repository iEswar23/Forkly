package io.github.ieswar23.forkly.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.data.repository.PreferencesRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.ThemeMode
import io.github.ieswar23.forkly.domain.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val preferences: UserPreferences = UserPreferences(),
    val orderCount: Int = 0,
    val favoriteCount: Int = 0,
    val addressCount: Int = 0,
    val totalSavedPaise: Long = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    orderRepository: OrderRepository,
    restaurantRepository: RestaurantRepository,
    addressRepository: AddressRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        preferencesRepository.preferences,
        orderRepository.orders,
        restaurantRepository.favorites,
        addressRepository.addresses,
    ) { prefs, orders, favorites, addresses ->
        ProfileUiState(
            preferences = prefs,
            orderCount = orders.size,
            favoriteCount = favorites.size,
            addressCount = addresses.size,
            totalSavedPaise = orders.sumOf { it.discountPaise },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setOrderUpdates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOrderUpdates(enabled) }
    }

    fun setOffers(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOffersAndPromos(enabled) }
    }

    fun updateProfile(name: String, phone: String, email: String) {
        viewModelScope.launch { preferencesRepository.updateProfile(name, phone, email) }
    }
}
