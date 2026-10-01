package io.github.ieswar23.forkly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.ieswar23.forkly.R

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val PROFILE = "profile"
    const val RESTAURANT = "restaurant/{restaurantId}"
    const val CHECKOUT = "checkout"
    const val ORDER_SUCCESS = "order_success/{orderId}"
    const val TRACKING = "tracking/{orderId}"
    const val FAVORITES = "favorites"
    const val ADDRESSES = "addresses"

    fun restaurant(id: String) = "restaurant/$id"
    fun orderSuccess(orderId: String) = "order_success/$orderId"
    fun tracking(orderId: String) = "tracking/$orderId"
}

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Rounded.Search, Icons.Outlined.Search),
    CART(Routes.CART, R.string.nav_cart, Icons.Rounded.ShoppingBag, Icons.Outlined.ShoppingBag),
    ORDERS(Routes.ORDERS, R.string.nav_orders, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    PROFILE(Routes.PROFILE, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person),
}
