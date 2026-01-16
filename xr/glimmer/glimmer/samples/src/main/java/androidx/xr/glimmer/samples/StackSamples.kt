/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.CardDefaults
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.stack.VerticalStack

@Sampled
@Composable
fun VerticalStackSample() {
    VerticalStack(modifier = Modifier.fillMaxWidth().height(364.dp)) {
        item(key = 0) {
            Card(modifier = Modifier.fillMaxSize().itemDecoration(CardDefaults.shape)) {
                Text(
                    "Item-0",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
        items(count = 10, key = { it + 1 }) { index ->
            Card(modifier = Modifier.fillMaxSize().itemDecoration(CardDefaults.shape)) {
                Text(
                    "Item-${index + 1}",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
    }
}

@Preview
@Composable
private fun VerticalStackPreview() {
    GlimmerTheme { VerticalStackSample() }
}
