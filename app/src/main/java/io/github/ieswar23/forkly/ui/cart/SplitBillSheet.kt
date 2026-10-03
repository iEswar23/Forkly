package io.github.ieswar23.forkly.ui.cart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.domain.pricing.BillShare
import io.github.ieswar23.forkly.domain.pricing.BillSplit
import io.github.ieswar23.forkly.domain.pricing.BillSplitter
import io.github.ieswar23.forkly.ui.common.EmojiCircle
import io.github.ieswar23.forkly.util.formatRupees

/** Lets the orderer see what each friend owes. Pure display: the maths lives in [BillSplitter]. */
@Composable
fun SplitBillSheet(
    split: BillSplit,
    onAddPerson: () -> Unit,
    onRemovePerson: () -> Unit,
    onSplitTipChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiCircle("👥", MaterialTheme.colorScheme.secondaryContainer, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Split the bill", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${formatRupees(split.totalPaise)} total • incl. taxes" + if (split.tipPaise > 0) " & tip" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SheetCard {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Number of people", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Including you • ${BillSplitter.MIN_PEOPLE} to ${BillSplitter.MAX_PEOPLE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PeopleStepper(people = split.people, onAdd = onAddPerson, onRemove = onRemovePerson)
                }
                if (split.tipPaise > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = split.splitTip, role = Role.Switch, onValueChange = onSplitTipChange)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Split the rider tip", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (split.splitTip) "Everyone chips in for the ${formatRupees(split.tipPaise)} tip"
                                else "You cover the ${formatRupees(split.tipPaise)} tip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        // The whole row is the toggle; the switch only mirrors its state.
                        Switch(checked = split.splitTip, onCheckedChange = null)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Each person pays", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            SheetCard(Modifier.animateContentSize()) {
                split.shares.forEachIndexed { index, share ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ShareRow(share)
                }
            }
            if (split.roundedUpCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    val who = if (split.roundedUpCount == 1) "1 person pays" else "${split.roundedUpCount} people pay"
                    Text(
                        "Exact to the paisa: $who 1 paisa more so the shares add up to ${formatRupees(split.totalPaise)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Done", style = MaterialTheme.typography.titleSmall) }
        }
    }
}

@Composable
private fun SheetCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) { content() }
}

@Composable
private fun PeopleStepper(people: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onRemove, enabled = people > BillSplitter.MIN_PEOPLE, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Remove, contentDescription = "Remove a person", modifier = Modifier.size(18.dp))
        }
        AnimatedContent(
            targetState = people,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "people",
        ) { value ->
            Text(
                "$value",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp),
            )
        }
        FilledTonalIconButton(onClick = onAdd, enabled = people < BillSplitter.MAX_PEOPLE, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "Add a person", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ShareRow(share: BillShare) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .background(
                    if (share.isOrderer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (share.isOrderer) "You" else "${share.person - 1}",
                style = MaterialTheme.typography.labelMedium,
                color = if (share.isOrderer) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (share.isOrderer) "You" else "Friend ${share.person - 1}", style = MaterialTheme.typography.titleSmall)
            if (share.tipCoveredPaise > 0) {
                Text(
                    "Includes the ${formatRupees(share.tipCoveredPaise)} rider tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(formatRupees(share.amountPaise), style = MaterialTheme.typography.titleMedium)
    }
}
