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

package androidx.compose.ui.inspection.testdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class RecompositionTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Item(1)
                Item(2)
            }
        }
    }

    @Composable
    fun Item(number: Int) {
        val clickCount = remember { mutableStateOf(0) }
        val list = remember { mutableStateListOf("a", "b", "c", "d", "e", "f") }
        Row {
            Column {
                Button(
                    onClick = { clickCount.value = clickCount.value + 1 },
                    modifier = Modifier.padding(16.dp, 4.dp),
                ) {
                    Text("Click row $number")
                }
            }
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text("Row $number click count: ${clickCount.value}, ${list.joinToString("")}")
            }
        }
    }
}
