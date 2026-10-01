package io.github.ieswar23.forkly.domain.model

/** Every amount is in paise. */
data class BillBreakdown(
    val itemTotalPaise: Long,
    val packagingFeePaise: Long,
    /** The fee before any waiver; shown struck-through when delivery is free. */
    val deliveryFeeBeforeWaiverPaise: Long,
    val deliveryFeePaise: Long,
    val couponDiscountPaise: Long,
    val gstPaise: Long,
    val tipPaise: Long,
    val totalPaise: Long,
    val appliedCoupon: Coupon?,
    val couponError: CouponError?,
    /** How much more the customer must add to unlock free delivery (0 when already unlocked). */
    val amountToFreeDeliveryPaise: Long,
) {
    val isDeliveryFree: Boolean get() = deliveryFeeBeforeWaiverPaise > 0 && deliveryFeePaise == 0L
    val deliverySavingsPaise: Long get() = deliveryFeeBeforeWaiverPaise - deliveryFeePaise
    val totalSavingsPaise: Long get() = couponDiscountPaise + deliverySavingsPaise
    val taxesAndChargesPaise: Long get() = packagingFeePaise + gstPaise

    companion object {
        val EMPTY = BillBreakdown(0, 0, 0, 0, 0, 0, 0, 0, null, null, 0)
    }
}
