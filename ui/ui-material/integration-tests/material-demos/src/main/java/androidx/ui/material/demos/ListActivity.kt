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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.foundation.AdapterList
import androidx.ui.text.TextStyle
import androidx.ui.unit.sp

class ListActivity : MaterialDemoActivity() {
    @Composable
    override fun materialContent() {
        AdapterList(
            data = listOf(
                "Hello,", "World:", "It works!", "",
                "this one is really long and spans a few lines for scrolling purposes",
                "these", "are", "offscreen"
            ) + (1..100).map { "$it" }.toList()
        ) {
            Text(text = it, style = TextStyle(fontSize = 80.sp))

            if (it.contains("works")) {
                Text("You can even emit multiple components per item.")
            }

            println("Composed item: $it")
        }
    }
}
