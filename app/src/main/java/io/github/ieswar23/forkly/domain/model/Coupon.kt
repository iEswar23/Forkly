package io.github.ieswar23.forkly.domain.model

sealed interface CouponType {
    data class PercentOff(val percent: Int, val maxDiscountPaise: Long) : CouponType
    data class FlatOff(val amountPaise: Long) : CouponType
    data object FreeDelivery : CouponType
}

data class Coupon(
    val code: String,
    val title: String,
    val description: String,
    val type: CouponType,
    val minOrderPaise: Long,
)

sealed interface CouponError {
    data class NotFound(val code: String) : CouponError
    data class MinOrderNotMet(val coupon: Coupon, val shortByPaise: Long) : CouponError
    data object EmptyCart : CouponError
}

sealed interface CouponValidation {
    data class Valid(val coupon: Coupon) : CouponValidation
    data class Invalid(val error: CouponError) : CouponValidation
}
