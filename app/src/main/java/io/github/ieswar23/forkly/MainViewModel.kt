package io.github.ieswar23.forkly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.PreferencesRepository
import io.github.ieswar23.forkly.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cartItemCount: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    cartRepository: CartRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(preferencesRepository.preferences, cartRepository.cart) { prefs, cart ->
        MainUiState(isReady = true, themeMode = prefs.themeMode, cartItemCount = cart.itemCount)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())
}
