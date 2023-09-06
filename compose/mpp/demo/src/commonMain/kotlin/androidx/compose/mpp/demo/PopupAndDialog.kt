package androidx.compose.mpp.demo

import androidx.compose.material3.AlertDialog as AlertDialog3
import androidx.compose.material3.Button as Button3
import androidx.compose.material3.DropdownMenu as DropdownMenu3
import androidx.compose.material3.DropdownMenuItem as DropdownMenuItem3
import androidx.compose.material3.ExposedDropdownMenuBox as ExposedDropdownMenuBox3
import androidx.compose.material3.TextField as TextField3
import androidx.compose.material3.Text as Text3
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.mpp.demo.textfield.android.loremIpsum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun PopupAndDialog() {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .padding(5.dp)
            .verticalScroll(scrollState)
    ) {
        PopupSample()
        DialogSamples()
        AlertDialogSample()
        AlertDialog3Sample()
        DropdownMenuSample()
        DropdownMenu3Sample()
        ExposedDropdownMenu3Sample()
    }
}

@Composable
private fun AlertDialogSample() {
    var short by remember { mutableStateOf(false) }
    var long by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onClick = { short = true }) {
            Text("AlertDialog (short)")
        }
        Button(onClick = { long = true }) {
            Text("AlertDialog (long)")
        }
    }
    if (short) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = { short = false }) {
                    Text("OK")
                }
            },
            text = { Text("Meow") },
        )
    }
    if (long) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = { long = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { long = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Alert Dialog") },
            text = { Text(loremIpsum()) },
        )
    }
}

@Composable
private fun AlertDialog3Sample() {
    var short by remember { mutableStateOf(false) }
    var long by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Button3(onClick = { short = true }) {
            Text3("AlertDialog3 (short)")
        }
        Button3(onClick = { long = true }) {
            Text3("AlertDialog3 (long)")
        }
    }
    if (short) {
        AlertDialog3(
            onDismissRequest = { },
            confirmButton = {
                Button3(onClick = { short = false }) {
                    Text3("OK")
                }
            },
            text = { Text3("Meow") },
        )
    }
    if (long) {
        AlertDialog3(
            onDismissRequest = { },
            confirmButton = {
                Button3(onClick = { long = false }) {
                    Text3("OK")
                }
            },
            dismissButton = {
                Button3(onClick = { long = false }) {
                    Text3("Cancel")
                }
            },
            title = { Text3("Alert Dialog") },
            text = { Text3(loremIpsum()) },
        )
    }
}

@Composable
private fun PopupSample() {
    var popup1 by remember { mutableStateOf(0) }
    var popup2 by remember { mutableStateOf(0) }
    var popup3 by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onClick = { popup1++ }) {
            Text("Popup")
        }
    }
    if (popup1 > 0) {
        MyPopup(
            text = "Click count = $popup1",
            offset = IntOffset(50, 50),
            focusable = false,
            onClose = { popup1 = 0 },
            onNext = { popup2++ }
        )
    }
    if (popup2 > 0) {
        MyPopup(
            text = "Click count = $popup2",
            offset = IntOffset(200, 100),
            focusable = true,
            onClose = { popup2 = 0 },
            onNext = { popup3++ }
        )
    }
    if (popup3 > 0) {
        MyPopup(
            text = "Click count = $popup3",
            offset = IntOffset(100, 200),
            focusable = false,
            onClose = { popup3 = 0 }
        )
    }
}

@Composable
private fun MyPopup(
    text: String,
    offset: IntOffset,
    focusable: Boolean,
    onClose: () -> Unit,
    onNext: (() -> Unit)? = null
) {
    val properties = PopupProperties(
        focusable = focusable,
        dismissOnBackPress = true,
        dismissOnClickOutside = true
    )
    Popup(
        offset = offset,
        onDismissRequest = onClose,
        properties = properties
    ) {
        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier
                .size(300.dp, 200.dp)
                .border(1.dp, Color.Black)
        ) {
            Column(Modifier.padding(5.dp)) {
                Text(text = text)
                Text(text = "focusable = $focusable")
                Text(text = "dismissOnBackPress = ${properties.dismissOnBackPress}")
                Text(text = "dismissOnClickOutside = ${properties.dismissOnClickOutside}")
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onClose
                    ) {
                        Text(text = "Close")
                    }
                    if (onNext != null) {
                        Spacer(Modifier.size(5.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onNext
                        ) {
                            Text(text = "Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogSamples() {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        DialogSample(
            modifier = Modifier.size(400.dp, 300.dp),
            text = "Dialog: 400x300"
        )
        DialogSample(
            modifier = Modifier.fillMaxSize(),
            text = "Dialog: max size"
        )
        DialogSample(
            modifier = Modifier.fillMaxSize(),
            text = "Dialog: max size (unrestricted)",
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        )
    }
}

@Composable
private fun DialogSample(
    modifier: Modifier = Modifier,
    text: String = "Dialog",
    properties: DialogProperties = DialogProperties()
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = properties
        ) {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                color = Color.Yellow
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showDialog = false }
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
    Button(
        onClick = { showDialog = true }
    ) {
        Text(text = text)
    }
}

@Composable
private fun DropdownMenuSample() {
    val horizontalScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .background(Color.Gray)
            .horizontalScroll(horizontalScrollState),
        horizontalArrangement = Arrangement.spacedBy(100.dp)
    ) {
        repeat(10) {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.width(180.dp)
                ) {
                    Text("DropdownMenu")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(180.dp),
                    properties = PopupProperties(focusable = false)
                ) {
                    repeat(it + 5) {
                        DropdownMenuItem(onClick = { expanded = false }) {
                            Text("Item $it")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownMenu3Sample() {
    val horizontalScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .background(Color.LightGray)
            .horizontalScroll(horizontalScrollState),
        horizontalArrangement = Arrangement.spacedBy(100.dp)
    ) {
        repeat(10) {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Button3(
                    onClick = { expanded = true },
                    modifier = Modifier.width(180.dp)
                ) {
                    Text3("DropdownMenu3")
                }
                DropdownMenu3(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(180.dp),
                    properties = PopupProperties(focusable = false)
                ) {
                    repeat(it + 5) {
                        DropdownMenuItem3(
                            text = { Text3("Item $it") },
                            onClick = { expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenu3Sample() {
    val options = List(5) { "Item $it" }
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    ExposedDropdownMenuBox3(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField3(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text3("ExposedDropdownMenuBox3") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem3(
                    text = { Text3(selectionOption) },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
