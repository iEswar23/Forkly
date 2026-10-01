package io.github.ieswar23.forkly.domain.model

enum class ThemeMode(val title: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val userName: String = "Aarav Reddy",
    val phone: String = "+91 98480 12345",
    val email: String = "aarav.reddy@example.com",
    val selectedAddressId: Long? = null,
    val orderUpdates: Boolean = true,
    val offersAndPromos: Boolean = false,
)
