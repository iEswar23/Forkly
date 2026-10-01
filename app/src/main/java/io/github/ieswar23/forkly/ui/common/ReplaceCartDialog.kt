package io.github.ieswar23.forkly.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ReplaceCartDialog(
    currentRestaurant: String,
    newRestaurant: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace cart items?") },
        text = {
            Text(
                "Your cart contains dishes from $currentRestaurant. " +
                    "Do you want to discard them and add dishes from $newRestaurant instead?",
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Replace") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } },
    )
}
