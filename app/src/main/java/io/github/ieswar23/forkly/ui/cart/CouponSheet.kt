package io.github.ieswar23.forkly.ui.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatRupees

@Composable
fun CouponSheet(
    coupons: List<CouponOption>,
    appliedCode: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var code by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            Text("Apply coupon", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(16) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Enter coupon code") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (code.isNotBlank()) onApply(code) }),
                trailingIcon = {
                    TextButton(onClick = { onApply(code) }, enabled = code.isNotBlank()) { Text("APPLY") }
                },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Available coupons",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(coupons, key = { it.coupon.code }) { option ->
                    CouponCard(option, isApplied = option.coupon.code == appliedCode, onApply = { onApply(option.coupon.code) })
                }
            }
        }
    }
}

@Composable
private fun CouponCard(option: CouponOption, isApplied: Boolean, onApply: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isApplied) ForklyTheme.extraColors.success else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().alpha(if (option.isEligible) 1f else 0.7f),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    option.coupon.code,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onApply, enabled = option.isEligible && !isApplied) {
                    Text(if (isApplied) "APPLIED" else "APPLY")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(option.coupon.title, style = MaterialTheme.typography.titleSmall)
            Text(option.coupon.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            when {
                option.isEligible && option.savingsPaise > 0 -> Text(
                    "Save ${formatRupees(option.savingsPaise)} on this order",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForklyTheme.extraColors.success,
                )
                option.isEligible -> Text(
                    "Applicable on this order",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForklyTheme.extraColors.success,
                )
                option.shortByPaise > 0 -> Row {
                    Text(
                        "Add ${formatRupees(option.shortByPaise)} more to unlock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                else -> Text(
                    "Add items to your cart to use this coupon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
