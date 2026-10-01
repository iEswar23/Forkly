package io.github.ieswar23.forkly.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.RestaurantCard
import io.github.ieswar23.forkly.ui.common.RestaurantCardPlaceholder
import io.github.ieswar23.forkly.util.UiState
import kotlinx.coroutines.launch

@Composable
fun FavoritesRoute(
    onBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favourites") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> RestaurantCardPlaceholder(Modifier.padding(padding).padding(16.dp))
            UiState.Empty, is UiState.Error -> EmptyState(
                emoji = "💛",
                title = "No favourites yet",
                message = "Tap the heart on any restaurant to save it here for quick reordering.",
                actionLabel = "Explore restaurants",
                onAction = onBrowse,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(s.data, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.id) },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(restaurant.id)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar("Removed ${restaurant.name}", actionLabel = "UNDO")
                                if (result == SnackbarResult.ActionPerformed) viewModel.toggleFavorite(restaurant.id)
                            }
                        },
                        modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
