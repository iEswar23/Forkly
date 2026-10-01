package io.github.ieswar23.forkly.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ieswar23.forkly.domain.model.ThemeMode
import io.github.ieswar23.forkly.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun updateProfile(name: String, phone: String, email: String)
    suspend fun setSelectedAddressId(id: Long?)
    suspend fun setOrderUpdates(enabled: Boolean)
    suspend fun setOffersAndPromos(enabled: Boolean)
    suspend fun isHistorySeeded(): Boolean
    suspend fun markHistorySeeded()
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val defaults = UserPreferences()
        UserPreferences(
            themeMode = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            userName = prefs[NAME] ?: defaults.userName,
            phone = prefs[PHONE] ?: defaults.phone,
            email = prefs[EMAIL] ?: defaults.email,
            selectedAddressId = prefs[SELECTED_ADDRESS],
            orderUpdates = prefs[ORDER_UPDATES] ?: defaults.orderUpdates,
            offersAndPromos = prefs[OFFERS] ?: defaults.offersAndPromos,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun updateProfile(name: String, phone: String, email: String) {
        dataStore.edit {
            it[NAME] = name.trim()
            it[PHONE] = phone.trim()
            it[EMAIL] = email.trim()
        }
    }

    override suspend fun setSelectedAddressId(id: Long?) {
        dataStore.edit { if (id == null) it.remove(SELECTED_ADDRESS) else it[SELECTED_ADDRESS] = id }
    }

    override suspend fun setOrderUpdates(enabled: Boolean) {
        dataStore.edit { it[ORDER_UPDATES] = enabled }
    }

    override suspend fun setOffersAndPromos(enabled: Boolean) {
        dataStore.edit { it[OFFERS] = enabled }
    }

    override suspend fun isHistorySeeded(): Boolean = dataStore.data.first()[HISTORY_SEEDED] == true

    override suspend fun markHistorySeeded() {
        dataStore.edit { it[HISTORY_SEEDED] = true }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val NAME = stringPreferencesKey("user_name")
        val PHONE = stringPreferencesKey("user_phone")
        val EMAIL = stringPreferencesKey("user_email")
        val SELECTED_ADDRESS = longPreferencesKey("selected_address_id")
        val ORDER_UPDATES = booleanPreferencesKey("order_updates")
        val OFFERS = booleanPreferencesKey("offers_promos")
        val HISTORY_SEEDED = booleanPreferencesKey("history_seeded")
    }
}
