package io.github.ieswar23.forkly.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel

/** Add / edit address form. Validation errors appear after the first save attempt. */
@Composable
fun AddressEditorSheet(
    initial: Address?,
    onSave: (Address) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var form by remember { mutableStateOf(initial?.let { AddressForm.from(it) } ?: AddressForm()) }
    var showErrors by remember { mutableStateOf(false) }
    val errors = if (showErrors) form.errors() else emptyMap()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (initial == null) "Add new address" else "Edit address",
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Save address as", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressLabel.entries.forEach { label ->
                    FilterChip(
                        selected = form.label == label,
                        onClick = { form = form.copy(label = label) },
                        label = { Text(label.title) },
                        leadingIcon = {
                            Icon(
                                when (label) {
                                    AddressLabel.HOME -> Icons.Rounded.Home
                                    AddressLabel.WORK -> Icons.Rounded.Apartment
                                    AddressLabel.OTHER -> Icons.Rounded.Place
                                },
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            if (form.label == AddressLabel.OTHER) {
                FormField("Label (e.g. Mom's place)", form.customLabel, null) { form = form.copy(customLabel = it) }
            }
            FormField("Flat / house no. / building", form.houseDetails, errors[AddressField.HOUSE]) { form = form.copy(houseDetails = it) }
            FormField("Area / street / locality", form.area, errors[AddressField.AREA]) { form = form.copy(area = it) }
            FormField("Nearby landmark (optional)", form.landmark, null) { form = form.copy(landmark = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("City", form.city, errors[AddressField.CITY], Modifier.weight(1f)) { form = form.copy(city = it) }
                FormField(
                    "Pincode",
                    form.pincode,
                    errors[AddressField.PINCODE],
                    Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                ) { form = form.copy(pincode = it.filter(Char::isDigit).take(6)) }
            }
            FormField("Receiver's name", form.receiverName, errors[AddressField.NAME]) { form = form.copy(receiverName = it) }
            FormField(
                "Receiver's phone",
                form.receiverPhone,
                errors[AddressField.PHONE],
                keyboardType = KeyboardType.Phone,
            ) { form = form.copy(receiverPhone = it.take(16)) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (form.isValid) onSave(form.toAddress())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Save address") }
            if (initial != null && onDelete != null) {
                TextButton(onClick = { onDelete(initial.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete this address", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, keyboardType = keyboardType),
        modifier = modifier,
    )
}
