package io.github.ieswar23.forkly.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.domain.model.BillBreakdown
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatRupees

/** Converts a stored order back into a breakdown so the same bill UI can render it. */
fun Order.toBill(): BillBreakdown = BillBreakdown(
    itemTotalPaise = itemTotalPaise,
    packagingFeePaise = packagingFeePaise,
    deliveryFeeBeforeWaiverPaise = deliveryFeePaise,
    deliveryFeePaise = deliveryFeePaise,
    couponDiscountPaise = discountPaise,
    gstPaise = gstPaise,
    tipPaise = tipPaise,
    totalPaise = totalPaise,
    appliedCoupon = null,
    couponError = null,
    amountToFreeDeliveryPaise = 0,
)

@Composable
fun BillDetailsCard(
    bill: BillBreakdown,
    modifier: Modifier = Modifier,
    couponCode: String? = bill.appliedCoupon?.code,
    title: String = "Bill details",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            BillRow("Item total", formatRupees(bill.itemTotalPaise))
            when {
                bill.deliveryFeePaise == 0L && bill.deliveryFeeBeforeWaiverPaise > 0 -> BillRow(
                    label = "Delivery fee",
                    value = "FREE",
                    struck = formatRupees(bill.deliveryFeeBeforeWaiverPaise),
                    valueColor = ForklyTheme.extraColors.success,
                )
                bill.deliveryFeePaise == 0L -> BillRow("Delivery fee", "FREE", valueColor = ForklyTheme.extraColors.success)
                else -> BillRow("Delivery fee", formatRupees(bill.deliveryFeePaise))
            }
            BillRow("Packaging charges", formatRupees(bill.packagingFeePaise))
            BillRow("GST (5%)", formatRupees(bill.gstPaise))
            if (bill.couponDiscountPaise > 0) {
                BillRow(
                    label = if (couponCode != null) "Coupon discount ($couponCode)" else "Coupon discount",
                    value = "−${formatRupees(bill.couponDiscountPaise)}",
                    valueColor = ForklyTheme.extraColors.success,
                )
            }
            if (bill.tipPaise > 0) BillRow("Delivery partner tip", formatRupees(bill.tipPaise))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("To pay", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(formatRupees(bill.totalPaise), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, struck: String? = null, valueColor: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (struck != null) {
            Text(
                struck,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
