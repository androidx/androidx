/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.demos

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

val LongScreenshotsDemos =
    listOf(
        ComposableDemo("Single, small, eager list") { SingleEagerListDemo() },
        ComposableDemo("Single, small, lazy list") { SingleLazyListDemo() },
        ComposableDemo("Single, full-screen list") { SingleFullScreenListDemo() },
        ComposableDemo("Lazy list with content padding") { LazyListContentPaddingDemo() },
        ComposableDemo("Big viewport nested in smaller outer viewport") { BigInLittleDemo() },
        ComposableDemo("Scrollable in dialog") { InDialogDemo() },
        ComposableDemo("Nested AndroidView") { AndroidViewDemo() },
        ComposableDemo("TextField in scrollable (legacy)") { LegacyTextFieldInScrollableDemo() },
        ComposableDemo("Single giant text field (legacy)") { LegacySingleGiantTextFieldDemo() },
        ComposableDemo("TextField in scrollable") { TextFieldInScrollableDemo() },
        ComposableDemo("Single giant text field") { SingleGiantTextFieldDemo() },
        ComposableDemo("Lazy list with sticky headers") { LazyListWithStickiesDemo() },
        ComposableDemo("Reverse layout") { ReverseScrollingCaptureDemo() },
    )

@Composable
private fun SingleEagerListDemo() {
    var fullWidth by remember { mutableStateOf(false) }
    var fullHeight by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "This is some scrollable content. When a screenshot is taken, it should let you " +
                "capture the entire content, not just the part currently visible.",
            style = MaterialTheme.typography.caption
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Full-width")
            Switch(fullWidth, onCheckedChange = { fullWidth = it })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Full-height")
            Switch(fullHeight, onCheckedChange = { fullHeight = it })
        }
        Divider()

        Column(
            Modifier.border(1.dp, Color.Black)
                .fillMaxWidth(fraction = if (fullWidth) 1f else 0.75f)
                .fillMaxHeight(fraction = if (fullHeight) 1f else 0.75f)
                .verticalScroll(rememberScrollState())
        ) {
            repeat(50) { index ->
                Button(onClick = {}, Modifier.padding(8.dp).fillMaxWidth()) {
                    Text("Button $index")
                }
            }
        }
    }
}

@Composable
private fun SingleLazyListDemo() {
    Column(
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "This is some scrollable content. When a screenshot is taken, it should let you " +
                "capture the entire content, not just the part currently visible.",
            style = MaterialTheme.typography.caption
        )

        LazyColumn(
            Modifier.border(1.dp, Color.Black).fillMaxWidth(fraction = 0.75f).height(200.dp)
        ) {
            items(50) { index ->
                Button(onClick = {}, Modifier.padding(8.dp).fillMaxWidth()) {
                    Text("Button $index")
                }
            }
        }
    }
}

@Composable
private fun SingleFullScreenListDemo() {
    LazyColumn(Modifier.fillMaxSize()) {
        items(50) { index ->
            Button(onClick = {}, Modifier.padding(8.dp).fillMaxWidth()) { Text("Button $index") }
        }
    }
}

@Composable
private fun LazyListContentPaddingDemo() {
    Scaffold(
        modifier = Modifier.padding(8.dp).border(1.dp, Color.Black),
        topBar = {
            TopAppBar(
                title = { Text("Top bar") },
                backgroundColor = MaterialTheme.colors.primarySurface.copy(alpha = 0.5f)
            )
        },
        bottomBar = {
            BottomAppBar(backgroundColor = MaterialTheme.colors.primarySurface.copy(alpha = 0.5f)) {
                Text("Bottom bar")
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color.Red),
            contentPadding = contentPadding
        ) {
            items(15) { index ->
                Button(
                    onClick = {},
                    Modifier.background(Color.LightGray).padding(8.dp).fillMaxWidth()
                ) {
                    Text("Button $index")
                }
            }
        }
    }
}

@Composable
private fun BigInLittleDemo() {
    Column(
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "This is a small scroll container that has a much larger scroll container inside it. " +
                "The inner scroll container should be captured.",
            style = MaterialTheme.typography.caption
        )

        LazyColumn(
            Modifier.border(1.dp, Color.Black).weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(4) {
                Text(
                    "Header $it",
                    Modifier.fillMaxWidth().background(Color.LightGray).padding(16.dp)
                )
                Box(
                    Modifier.background(Color.Magenta)
                        .fillParentMaxHeight(0.5f)
                        .padding(horizontal = 16.dp)
                ) {
                    SingleFullScreenListDemo()
                }
                Text(
                    "Footer $it",
                    Modifier.fillMaxWidth().background(Color.LightGray).padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InDialogDemo() {
    Column {
        // Need a scrolling list in the below screen to check that the scrollable in the dialog is
        // selected instead.
        LazyColumn(Modifier.fillMaxSize()) {
            items(50) { index ->
                var showDialog by remember { mutableStateOf(false) }
                Button(onClick = { showDialog = true }, Modifier.padding(8.dp).fillMaxWidth()) {
                    Text("Open dialog ($index)")
                }

                if (showDialog) {
                    Dialog(onDismissRequest = { showDialog = false }) {
                        Box(Modifier.fillMaxSize(fraction = 0.5f).background(Color.LightGray)) {
                            SingleFullScreenListDemo()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidViewDemo() {
    class DemoAndroidView(context: Context) : LinearLayout(context) {
        init {
            orientation = VERTICAL
            addView(TextView(context).also { it.text = "AndroidView Header" })
            addView(
                ScrollView(context).apply {
                    setBackgroundColor(android.graphics.Color.CYAN)
                    addView(
                        LinearLayout(context).apply {
                            orientation = VERTICAL
                            repeat(20) {
                                addView(
                                    TextView(context).apply {
                                        setPadding(20, 20, 20, 20)
                                        text = "Item $it"
                                    }
                                )
                            }
                        }
                    )
                },
                LayoutParams(MATCH_PARENT, 0, 1f)
            )
            addView(TextView(context).also { it.text = "AndroidView Footer" })
        }
    }

    Column {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(10) { Text("Compose item", Modifier.padding(16.dp)) }
            item {
                AndroidView(
                    factory = ::DemoAndroidView,
                    modifier =
                        Modifier.background(Color.Magenta)
                            .fillParentMaxHeight(0.5f)
                            .padding(horizontal = 16.dp)
                )
            }
            items(5) { Text("Compose item", Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun LegacyTextFieldInScrollableDemo() {
    LazyColumn(Modifier.fillMaxSize().imePadding()) {
        repeat(10) {
            item {
                var text by remember { mutableStateOf("") }
                OutlinedTextField(
                    label = { Text("Single line") },
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
            item {
                var text by remember { mutableStateOf("one\ntwo\nthree\nfour\nfive\nsix") }
                OutlinedTextField(
                    label = { Text("Multiline, scrollable field") },
                    value = text,
                    onValueChange = { text = it },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun LegacySingleGiantTextFieldDemo() {
    var text by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TextFieldInScrollableDemo() {
    LazyColumn(Modifier.fillMaxSize().imePadding()) {
        repeat(10) {
            item {
                val text = rememberTextFieldState()
                Text("Single line")
                BasicTextField(
                    state = text,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
            item {
                val text = rememberTextFieldState("one\ntwo\nthree\nfour\nfive\nsix")
                Text("Multiline, scrollable field")
                BasicTextField(
                    state = text,
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun SingleGiantTextFieldDemo() {
    val text = rememberTextFieldState()
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        BasicTextField(state = text, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyListWithStickiesDemo() {
    LazyColumn(Modifier.fillMaxSize()) {
        // Header with a big section.
        stickyHeader {
            Text(
                "Header 1",
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Green).fillMaxWidth().padding(16.dp)
            )
        }
        item { Box(Modifier.background(Color.Magenta).fillMaxWidth().fillParentMaxHeight()) }

        // Headers with small sections.
        val sectionCount = 4
        repeat(sectionCount) {
            stickyHeader {
                Text(
                    "Header ${it + 2}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.background(Color.Green).fillMaxWidth().padding(16.dp)
                )
            }
            item {
                Box(
                    Modifier.background(Color.Magenta)
                        .fillMaxWidth()
                        .fillParentMaxHeight(1f / (sectionCount - 1))
                )
            }
        }
    }
}

@Composable
private fun ReverseScrollingCaptureDemo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState(), reverseScrolling = true)
    ) {
        repeat(50) { index -> Text("Row $index", Modifier.heightIn(min = 40.dp).padding(8.dp)) }
    }
}
