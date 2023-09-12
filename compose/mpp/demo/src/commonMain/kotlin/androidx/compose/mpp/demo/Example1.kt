package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable // Not used at the moment, but added here to test compose with kotlinx.serialization compilation
public class Id(public val id: Applier<String>)

@Composable // Not used at the moment, but added here to test compose with kotlinx.serialization compilation
fun Abc(id: Id) {
    println("Id = $id")
}

@Composable
fun Example1() {
    var tick by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var clutz by remember { mutableStateOf(false) }
    var switched by remember { mutableStateOf(false) }
    var textFromClipboard by remember { mutableStateOf("click for clipboard") }
    var textFieldState by remember { mutableStateOf("I am TextField") }
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    Column {
        SearchBarSample()
        DockedSearchBarSample()

        TextField(
            value = textFieldState,
            onValueChange = {
                textFieldState = it
            }
        )
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(color = if (selected) Color.Gray else Color.Red)
                .width(100.dp).height(100.dp)
                .clickable {
                    println("Red box: clicked")
                }.pointerHoverIcon(PointerIcon.Text)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(color = if (tick) Color.Green else Color.Blue)
                    .width(20.dp).height(20.dp)
                    .clickable {
                        println("Small box: clicked")
                    }.pointerHoverIcon(PointerIcon.Hand)
            )
        }
        Spacer(
            Modifier.width(200.dp)
                .height(if (clutz) 4.dp else 12.dp)
                .background(color = if (clutz) Color.DarkGray else Color.Magenta)
        )
        Button(
            modifier = Modifier
                .padding(16.dp),
            onClick = {
                println("Button clicked!")
                tick = !tick
            }
        ) {
            Text(if (switched) "🦑 press 🐙" else "Press me!")
        }
        Row {
            RadioButton(
                modifier = Modifier
                    .padding(16.dp),
                selected = selected,
                onClick = {
                    println("RadioButton clicked!")
                    selected = !selected
                }
            )

            Checkbox(
                checked = clutz,
                modifier = Modifier.padding(16.dp),
                onCheckedChange = { clutz = !clutz }
            )
        }
        Switch(
            modifier = Modifier
                .padding(16.dp),
            checked = switched,
            onCheckedChange = { switched = it }
        )
        Row {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    uriHandler.openUri("https://kotlinlang.org")
                },
            ) {
                Text("Open URL")
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    textFromClipboard = clipboard.getText()?.text ?: "clipboard is empty"
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text(textFromClipboard)
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarSample() {
    var text by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(5.dp)) {
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter),
            query = text,
            onQueryChange = { text = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("SearchBar") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
        ) {
            SearchBarSampleContent {
                text = it
                active = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockedSearchBarSample() {
    var text by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(5.dp)) {
        DockedSearchBar(
            modifier = Modifier.align(Alignment.TopCenter),
            query = text,
            onQueryChange = { text = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("DockedSearchBar") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
        ) {
            SearchBarSampleContent {
                text = it
                active = false
            }
        }
    }
}

@Composable
private fun SearchBarSampleContent(onClick: (String) -> Unit) {
    repeat(4) { index ->
        val resultText = "Suggestion $index"
        ListItem(
            headlineContent = { Text(resultText) },
            supportingContent = { Text("Additional info") },
            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick(resultText) }
        )
    }
}
