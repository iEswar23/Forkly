package io.github.ieswar23.forkly.domain.pricing

import io.github.ieswar23.forkly.domain.model.BillBreakdown
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.Coupon
import io.github.ieswar23.forkly.domain.model.CouponError
import io.github.ieswar23.forkly.domain.model.CouponType
import io.github.ieswar23.forkly.domain.model.CouponValidation
import kotlin.math.ceil

/** Tunable fee structure. All money is in paise to avoid floating-point rounding errors. */
data class PricingConfig(
    val baseDeliveryFeePaise: Long = 35_00,
    val includedDistanceKm: Double = 3.0,
    val perExtraKmFeePaise: Long = 7_00,
    val maxDeliveryFeePaise: Long = 65_00,
    val freeDeliveryThresholdPaise: Long = 499_00,
    val packagingFeePerUnitPaise: Long = 5_00,
    val maxPackagingFeePaise: Long = 30_00,
    val gstPercent: Int = 5,
)

/**
 * Pure, deterministic bill calculation used by the cart, checkout and order placement.
 *
 * Rules:
 * - Item total = Σ unit price × quantity.
 * - Packaging = ₹5 per unit, capped at ₹30.
 * - Delivery = ₹35 for the first 3 km + ₹7 per extra (started) km, capped at ₹65.
 *   It is waived when the item total reaches ₹499 or a free-delivery coupon is applied.
 * - Coupon discounts apply to the item total only; percentage coupons respect their cap and
 *   flat discounts never exceed the item total.
 * - GST (5%) is charged on (item total − discount + packaging), rounded half-up to the paisa.
 * - Rider tip is passed through untaxed.
 */
class PricingCalculator(val config: PricingConfig = PricingConfig()) {

    fun validateCoupon(code: String, itemTotalPaise: Long): CouponValidation {
        if (itemTotalPaise <= 0) return CouponValidation.Invalid(CouponError.EmptyCart)
        val coupon = CouponCatalog.find(code)
            ?: return CouponValidation.Invalid(CouponError.NotFound(code.trim().uppercase()))
        if (itemTotalPaise < coupon.minOrderPaise) {
            return CouponValidation.Invalid(CouponError.MinOrderNotMet(coupon, coupon.minOrderPaise - itemTotalPaise))
        }
        return CouponValidation.Valid(coupon)
    }

    fun deliveryFeeFor(distanceKm: Double): Long {
        val extraKm = ceil((distanceKm - config.includedDistanceKm).coerceAtLeast(0.0)).toLong()
        return (config.baseDeliveryFeePaise + extraKm * config.perExtraKmFeePaise)
            .coerceAtMost(config.maxDeliveryFeePaise)
    }

    fun packagingFeeFor(units: Int): Long =
        (units * config.packagingFeePerUnitPaise).coerceAtMost(config.maxPackagingFeePaise)

    fun discountFor(coupon: Coupon, itemTotalPaise: Long): Long = when (val type = coupon.type) {
        is CouponType.PercentOff -> (itemTotalPaise * type.percent / 100).coerceAtMost(type.maxDiscountPaise)
        is CouponType.FlatOff -> type.amountPaise.coerceAtMost(itemTotalPaise)
        CouponType.FreeDelivery -> 0L
    }

    fun calculate(
        lines: List<CartLine>,
        distanceKm: Double,
        couponCode: String? = null,
        tipPaise: Long = 0,
    ): BillBreakdown {
        if (lines.isEmpty()) return BillBreakdown.EMPTY
        require(tipPaise >= 0) { "Tip cannot be negative" }

        val itemTotal = lines.sumOf { it.totalPaise }
        val units = lines.sumOf { it.quantity }
        val packaging = packagingFeeFor(units)

        val validation = couponCode?.takeIf { it.isNotBlank() }?.let { validateCoupon(it, itemTotal) }
        val coupon = (validation as? CouponValidation.Valid)?.coupon
        val couponError = (validation as? CouponValidation.Invalid)?.error

        val discount = coupon?.let { discountFor(it, itemTotal) } ?: 0L
        val baseDelivery = deliveryFeeFor(distanceKm)
        val reachedThreshold = itemTotal >= config.freeDeliveryThresholdPaise
        val deliveryWaived = reachedThreshold || coupon?.type == CouponType.FreeDelivery
        val delivery = if (deliveryWaived) 0L else baseDelivery

        val taxable = itemTotal - discount + packaging
        val gst = roundHalfUpDiv(taxable * config.gstPercent, 100)

        return BillBreakdown(
            itemTotalPaise = itemTotal,
            packagingFeePaise = packaging,
            deliveryFeeBeforeWaiverPaise = baseDelivery,
            deliveryFeePaise = delivery,
            couponDiscountPaise = discount,
            gstPaise = gst,
            tipPaise = tipPaise,
            totalPaise = taxable + gst + delivery + tipPaise,
            appliedCoupon = coupon,
            couponError = couponError,
            amountToFreeDeliveryPaise = (config.freeDeliveryThresholdPaise - itemTotal).coerceAtLeast(0),
        )
    }

    private fun roundHalfUpDiv(numerator: Long, denominator: Long): Long = (numerator + denominator / 2) / denominator
}
