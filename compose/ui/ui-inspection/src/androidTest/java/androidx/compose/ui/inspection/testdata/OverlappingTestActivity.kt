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

package androidx.compose.ui.inspection.testdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class OverlappingTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This box is drawing a background:
            Box(modifier = Modifier.background(Color.Red)) {
                // Text is drawing text:
                Text(text = "Hello Android")
            }
            // This box is not drawing:
            Box {
                Column {
                    Spacer(modifier = Modifier.height(20.dp).width(20.dp))
                    Row {
                        Spacer(modifier = Modifier.width(20.dp))
                        // This box is drawing a background:
                        Box(modifier = Modifier.background(Color.Yellow)) {
                            // Text is drawing text:
                            Text("Studio Layout Inspector")
                        }
                    }
                }
            }
        }
    }
}
