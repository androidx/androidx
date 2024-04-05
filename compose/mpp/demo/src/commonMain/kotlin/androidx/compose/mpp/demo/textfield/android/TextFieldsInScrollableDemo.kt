/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.compose.mpp.demo.textfield.android

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private enum class ScrollableType {
    ScrollableColumn,
    LazyColumn,
    EditTextsInScrollView,
}

@Composable
fun TextFieldsInScrollableDemo() {
    var scrollableType by remember { mutableStateOf(ScrollableType.values().first()) }

    Column(Modifier.windowInsetsPadding(WindowInsets.ime)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = scrollableType == ScrollableType.ScrollableColumn,
                onClick = { scrollableType = ScrollableType.ScrollableColumn }
            )
            Text("Scrollable column")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = scrollableType == ScrollableType.LazyColumn,
                onClick = { scrollableType = ScrollableType.LazyColumn }
            )
            Text("LazyColumn")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = scrollableType == ScrollableType.EditTextsInScrollView,
                onClick = { scrollableType = ScrollableType.EditTextsInScrollView }
            )
            Text("ScrollView")
        }

        when (scrollableType) {
            ScrollableType.ScrollableColumn -> TextFieldInScrollableColumn()
            ScrollableType.LazyColumn -> TextFieldInLazyColumn()
            ScrollableType.EditTextsInScrollView -> EditTextsInScrollView()
        }
    }
}

@Composable
private fun TextFieldInScrollableColumn() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        repeat(50) { index ->
            DemoTextField(index)
        }
    }
}

@Composable
private fun TextFieldInLazyColumn() {
    LazyColumn {
        items(50) { index ->
            DemoTextField(index)
        }
    }
}

@Composable
private fun EditTextsInScrollView() {
    Text("Can't be implemented in commonMain")
}

@Composable
private fun DemoTextField(index: Int) {
    var text by rememberSaveable { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        leadingIcon = { Text(index.toString()) },
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.Black)
            .fillMaxWidth()
    )
}
