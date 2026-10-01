package io.github.ieswar23.forkly.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.ieswar23.forkly.ui.cart.CartRoute
import io.github.ieswar23.forkly.ui.checkout.CheckoutRoute
import io.github.ieswar23.forkly.ui.checkout.OrderSuccessRoute
import io.github.ieswar23.forkly.ui.favorites.FavoritesRoute
import io.github.ieswar23.forkly.ui.home.HomeRoute
import io.github.ieswar23.forkly.ui.orders.OrdersRoute
import io.github.ieswar23.forkly.ui.profile.AddressesRoute
import io.github.ieswar23.forkly.ui.profile.ProfileRoute
import io.github.ieswar23.forkly.ui.restaurant.RestaurantRoute
import io.github.ieswar23.forkly.ui.restaurant.RestaurantViewModel
import io.github.ieswar23.forkly.ui.search.SearchRoute
import io.github.ieswar23.forkly.ui.tracking.TrackingRoute
import io.github.ieswar23.forkly.ui.tracking.TrackingViewModel

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

@Composable
fun ForklyAppUi(cartItemCount: Int, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination.route) },
                            icon = {
                                if (destination == TopLevelDestination.CART && cartItemCount > 0) {
                                    BadgedBox(badge = {
                                        Badge {
                                            AnimatedContent(
                                                targetState = cartItemCount,
                                                transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                                                label = "cartBadge",
                                            ) { Text("$it") }
                                        }
                                    }) {
                                        Icon(if (selected) destination.selectedIcon else destination.unselectedIcon, contentDescription = label)
                                    }
                                } else {
                                    Icon(if (selected) destination.selectedIcon else destination.unselectedIcon, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 12 } },
        ) {
            composable(Routes.HOME) {
                HomeRoute(
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onSearchClick = { navController.navigateToTopLevel(Routes.SEARCH) },
                    onProfileClick = { navController.navigateToTopLevel(Routes.PROFILE) },
                    onAddressClick = { navController.navigate(Routes.ADDRESSES) },
                )
            }
            composable(Routes.SEARCH) {
                SearchRoute(onRestaurantClick = { navController.navigate(Routes.restaurant(it)) })
            }
            composable(Routes.CART) {
                CartRoute(
                    onBrowse = { navController.navigateToTopLevel(Routes.HOME) },
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onCheckout = { navController.navigate(Routes.CHECKOUT) },
                    onChangeAddress = { navController.navigate(Routes.ADDRESSES) },
                )
            }
            composable(Routes.ORDERS) {
                OrdersRoute(
                    onOrderClick = { navController.navigate(Routes.tracking(it)) },
                    onBrowse = { navController.navigateToTopLevel(Routes.HOME) },
                    onViewCart = { navController.navigateToTopLevel(Routes.CART) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileRoute(
                    onFavorites = { navController.navigate(Routes.FAVORITES) },
                    onAddresses = { navController.navigate(Routes.ADDRESSES) },
                    onOrders = { navController.navigateToTopLevel(Routes.ORDERS) },
                )
            }
            composable(
                Routes.RESTAURANT,
                arguments = listOf(navArgument(RestaurantViewModel.ARG_RESTAURANT_ID) { type = NavType.StringType }),
            ) {
                RestaurantRoute(
                    onBack = { navController.popBackStack() },
                    onViewCart = { navController.navigateToTopLevel(Routes.CART) },
                )
            }
            composable(Routes.CHECKOUT) {
                CheckoutRoute(
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Routes.orderSuccess(orderId)) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    },
                )
            }
            composable(
                Routes.ORDER_SUCCESS,
                arguments = listOf(navArgument(TrackingViewModel.ARG_ORDER_ID) { type = NavType.StringType }),
                enterTransition = { fadeIn(tween(300)) },
            ) {
                OrderSuccessRoute(
                    onTrackOrder = { orderId ->
                        navController.navigate(Routes.tracking(orderId)) {
                            popUpTo(Routes.ORDER_SUCCESS) { inclusive = true }
                        }
                    },
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Routes.TRACKING,
                arguments = listOf(navArgument(TrackingViewModel.ARG_ORDER_ID) { type = NavType.StringType }),
            ) {
                TrackingRoute(onBack = { navController.popBackStack() })
            }
            composable(Routes.FAVORITES) {
                FavoritesRoute(
                    onBack = { navController.popBackStack() },
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onBrowse = { navController.navigateToTopLevel(Routes.HOME) },
                )
            }
            composable(Routes.ADDRESSES) {
                AddressesRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
