package io.github.ieswar23.forkly.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption

/** Sort + filter bottom sheet. Edits a local draft and only commits on "Apply". */
@Composable
fun FilterSortSheet(
    filters: RestaurantFilters,
    sort: SortOption,
    onApply: (RestaurantFilters, SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftFilters by remember { mutableStateOf(filters) }
    var draftSort by remember { mutableStateOf(sort) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Sort & filter", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text("Sort by", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.selectableGroup()) {
                SortOption.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = draftSort == option, onClick = { draftSort = option }, role = Role.RadioButton)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = draftSort == option, onClick = null)
                        Text(option.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Filters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SheetChip("Pure veg", draftFilters.vegOnly) { draftFilters = draftFilters.copy(vegOnly = !draftFilters.vegOnly) }
                SheetChip("Rating 4.0+", draftFilters.rating4Plus) { draftFilters = draftFilters.copy(rating4Plus = !draftFilters.rating4Plus) }
                SheetChip("Offers", draftFilters.offersOnly) { draftFilters = draftFilters.copy(offersOnly = !draftFilters.offersOnly) }
                SheetChip("Under 30 mins", draftFilters.fastDelivery) { draftFilters = draftFilters.copy(fastDelivery = !draftFilters.fastDelivery) }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        draftFilters = RestaurantFilters()
                        draftSort = SortOption.RELEVANCE
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Clear all") }
                Button(
                    onClick = { onApply(draftFilters, draftSort) },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Apply") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null) }
        } else {
            null
        },
    )
}
