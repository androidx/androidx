/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun SearchBarPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchBar(Modifier.fillMaxWidth())
        OutlinedSearchBar(Modifier.fillMaxWidth())
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.LightGray
) {
    CommonSearchBar(
        modifier = modifier,
        outlined = false,
        borderOrBackgroundColor = backgroundColor
    )
}

@Composable
fun OutlinedSearchBar(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.LightGray
) {
    CommonSearchBar(
        modifier = modifier,
        outlined = true,
        borderOrBackgroundColor = borderColor
    )
}

@Composable
private fun CommonSearchBar(
    modifier: Modifier,
    outlined: Boolean,
    borderOrBackgroundColor: Color
) {
    var placeholder: String by remember { mutableStateOf("Search...") }
    val backgroundModifier = if (outlined) {
        Modifier.border(BorderStroke(2.dp, borderOrBackgroundColor), RoundedCornerShape(32.dp))
    } else {
        Modifier.background(borderOrBackgroundColor, RoundedCornerShape(32.dp))
    }
    OutlinedTextField(
        modifier = modifier
            .then(backgroundModifier)
            .onFocusChanged {
                placeholder = if (it.isFocused) {
                    "I'm not implemented yet!"
                } else {
                    "Search..."
                }
            },
        value = "",
        onValueChange = { _ ->
        },
        placeholder = {
            Text(text = placeholder, maxLines = 1, overflow = TextOverflow.Clip)
        },
        trailingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            backgroundColor = Color.Transparent,
        )
    )
}
