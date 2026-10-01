package io.github.ieswar23.forkly.ui.restaurant

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.domain.model.CustomizationGroup
import io.github.ieswar23.forkly.domain.model.CustomizationSelection
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.SelectionType
import io.github.ieswar23.forkly.ui.common.EmojiCircle
import io.github.ieswar23.forkly.ui.common.QuantityStepper
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.util.formatRupees

@Composable
fun CustomizationSheet(
    item: MenuItem,
    onDismiss: () -> Unit,
    onAddToCart: (CustomizationSelection, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selection by remember(item.id) { mutableStateOf(CustomizationSelection.defaultFor(item)) }
    var quantity by remember(item.id) { mutableIntStateOf(1) }
    val unitPrice = selection.unitPricePaise(item)
    val complete = selection.isComplete(item.customizations)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                EmojiCircle(item.emoji, MaterialTheme.colorScheme.secondaryContainer, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VegIndicator(item.isVeg, size = 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Customise as per your taste",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item.customizations.forEach { group ->
                    GroupCard(group = group, selection = selection, onToggle = { optionId -> selection = selection.toggle(group, optionId) })
                }
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuantityStepper(
                    quantity = quantity,
                    onIncrement = { quantity = (quantity + 1).coerceAtMost(20) },
                    onDecrement = { quantity = (quantity - 1).coerceAtLeast(1) },
                )
                Button(
                    onClick = { onAddToCart(selection, quantity) },
                    enabled = complete,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    AnimatedContent(targetState = unitPrice * quantity, label = "sheetTotal") { total ->
                        Text("Add item  |  ${formatRupees(total)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: CustomizationGroup, selection: CustomizationSelection, onToggle: (String) -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(group.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
            val rule = when {
                group.type == SelectionType.SINGLE && group.required -> "Required • Select any 1"
                group.type == SelectionType.SINGLE -> "Optional • Select up to 1"
                else -> "Optional • Select up to ${group.maxSelections}"
            }
            Text(
                rule,
                style = MaterialTheme.typography.bodySmall,
                color = if (group.required) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            group.options.forEach { option ->
                val checked = selection.isSelected(group.id, option.id)
                val selectedCount = selection.selected[group.id].orEmpty().size
                val enabled = group.type == SelectionType.SINGLE || checked || selectedCount < group.maxSelections
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = checked,
                            enabled = enabled,
                            role = if (group.type == SelectionType.SINGLE) Role.RadioButton else Role.Checkbox,
                            onValueChange = { onToggle(option.id) },
                        )
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        option.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (option.priceDeltaPaise > 0) {
                        Text(
                            "+${formatRupees(option.priceDeltaPaise)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (group.type == SelectionType.SINGLE) {
                        RadioButton(selected = checked, onClick = null, modifier = Modifier.padding(12.dp))
                    } else {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}
