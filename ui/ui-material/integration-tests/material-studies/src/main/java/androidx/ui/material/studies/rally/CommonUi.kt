/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.studies.rally

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.dp

/**
 * A row within the Accounts card in the Rally Overview screen.
 */
@Composable
fun AccountRow(name: String, number: String, amount: String, color: Color) {
    Row(LayoutPadding(top = 12.dp, bottom = 12.dp)) {
        val typography = MaterialTheme.typography()
        AccountIndicator(color = color)
        Spacer(LayoutWidth(8.dp))
        Column {
            Text(text = name, style = typography.body1)
            Text(text = "•••••$number", style = typography.subtitle1)
        }
        Spacer(LayoutFlexible(1f))
        Text(text = "$ $amount", style = typography.h6)
    }
}

/**
 * A vertical colored line that is used in a [AccountRow] to differentiate accounts.
 */
@Composable
private fun AccountIndicator(color: Color) {
    ColoredRect(color = color, width = 4.dp, height = 36.dp)
}

@Composable
fun RallyDivider() = Divider(color = MaterialTheme.colors().background, height = 2.dp)
