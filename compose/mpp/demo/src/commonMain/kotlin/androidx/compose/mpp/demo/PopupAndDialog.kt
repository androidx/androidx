package androidx.compose.mpp.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
    Column(Modifier.padding(5.dp)) {
        PopupSample()
        DialogSample()
    }
}

@Composable
private fun PopupSample() {
    var popup1 by remember { mutableStateOf(0) }
    var popup2 by remember { mutableStateOf(0) }
    var popup3 by remember { mutableStateOf(0) }
    Button(onClick = { popup1++ }) {
        Text("Popup")
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
private fun DialogSample() {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .size(400.dp, 300.dp),
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
        Text(text = "Dialog")
    }
}