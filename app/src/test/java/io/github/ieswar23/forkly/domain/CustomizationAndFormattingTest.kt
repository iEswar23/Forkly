package io.github.ieswar23.forkly.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.CustomizationSelection
import io.github.ieswar23.forkly.fakes.TestData
import io.github.ieswar23.forkly.ui.checkout.AddressField
import io.github.ieswar23.forkly.ui.checkout.AddressForm
import io.github.ieswar23.forkly.util.formatCountdown
import io.github.ieswar23.forkly.util.formatRupees
import org.junit.Test

class CustomizationAndFormattingTest {

    private val pizza = TestData.menuItem("p1", "r1", 299_00, customizations = listOf(TestData.sizeGroup, TestData.toppingsGroup))

    @Test
    fun `default selection preselects required single choice groups`() {
        val selection = CustomizationSelection.defaultFor(pizza)
        assertThat(selection.isSelected("size", "regular")).isTrue()
        assertThat(selection.isComplete(pizza.customizations)).isTrue()
        assertThat(selection.unitPricePaise(pizza)).isEqualTo(299_00)
    }

    @Test
    fun `multi-select groups respect their maximum`() {
        val selection = CustomizationSelection.defaultFor(pizza)
            .toggle(TestData.toppingsGroup, "cheese")
            .toggle(TestData.toppingsGroup, "olives")
            .toggle(TestData.toppingsGroup, "jalapeno") // ignored: max 2

        assertThat(selection.selected["toppings"]).containsExactly("cheese", "olives")
        assertThat(selection.unitPricePaise(pizza)).isEqualTo(299_00 + 60_00 + 40_00)
    }

    @Test
    fun `single-choice groups swap rather than accumulate`() {
        val selection = CustomizationSelection.defaultFor(pizza).toggle(TestData.sizeGroup, "large")
        assertThat(selection.selected["size"]).containsExactly("large")
        assertThat(selection.summary(pizza.customizations)).isEqualTo("Large")
    }

    @Test
    fun `cart line ids are stable regardless of option order`() {
        assertThat(CartLine.lineIdFor("p1", listOf("b", "a"))).isEqualTo(CartLine.lineIdFor("p1", listOf("a", "b")))
        assertThat(CartLine.lineIdFor("p1", emptyList())).isEqualTo("p1")
    }

    @Test
    fun `rupees use Indian digit grouping and hide zero paise`() {
        assertThat(formatRupees(249_00)).isEqualTo("₹249")
        assertThat(formatRupees(1_249_50)).isEqualTo("₹1,249.50")
        assertThat(formatRupees(1_23_456_05)).isEqualTo("₹1,23,456.05")
        assertThat(formatRupees(-100_00)).isEqualTo("-₹100")
    }

    @Test
    fun `countdown rounds up to whole seconds`() {
        assertThat(formatCountdown(102_000)).isEqualTo("01:42")
        assertThat(formatCountdown(1)).isEqualTo("00:01")
        assertThat(formatCountdown(0)).isEqualTo("00:00")
    }

    @Test
    fun `address form validates pincode and phone`() {
        val form = AddressForm(houseDetails = "Flat 1", area = "Madhapur", pincode = "50008", receiverName = "Aarav", receiverPhone = "12345")
        assertThat(form.errors().keys).containsExactly(AddressField.PINCODE, AddressField.PHONE)

        val fixed = form.copy(pincode = "500081", receiverPhone = "+91 98480 12345")
        assertThat(fixed.isValid).isTrue()
    }
}
