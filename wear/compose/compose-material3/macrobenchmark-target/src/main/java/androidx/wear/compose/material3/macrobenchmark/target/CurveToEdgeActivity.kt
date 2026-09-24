/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.target

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ExperimentalWearComposeMaterial3Api
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curveToEdge

// These are approximations of the Curved Layout Activities
//
// It is not possible to perfectly recreate how CurvedLayout lays out content using
// Modifier.curveToEdge

class SimpleCurveToEdgeActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalWearComposeMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showCurvedText by remember { mutableStateOf(true) }
                if (showCurvedText) {
                    Text("Curved Text", Modifier.curveToEdge())
                }
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Button(
                        onClick = { showCurvedText = !showCurvedText },
                        modifier = Modifier.semantics { contentDescription = TOGGLE_DISPLAY },
                    ) {
                        Text("Toggle")
                    }
                }
            }
        }
    }
}

class ComplexCurveToEdgeActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalWearComposeMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showCurvedText by remember { mutableStateOf(true) }
                val style = MaterialTheme.typography.titleMedium
                if (showCurvedText) {
                    val height = 35.dp
                    Box(Modifier.curveToEdge(360f)) {
                        Row(
                            Modifier.padding(PaddingValues(2.dp))
                                .background(Color(0xff0077ff), RoundedCornerShape(50))
                                // Height is 5dp more than in Curved Layout for the same size
                                .height(height)
                                .padding(horizontal = 15.dp)
                        ) {
                            Box(
                                Modifier.paint(painterResource(R.drawable.ic_favorite_rounded))
                                    .size(height)
                            )
                            Text("Curved Text", style = style)
                            SwitchButton(showCurvedText, {}, Modifier.size(height)) {}
                            Text("More Text", style = style)
                        }
                    }
                }

                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Button(
                        onClick = { showCurvedText = !showCurvedText },
                        modifier = Modifier.semantics { contentDescription = TOGGLE_DISPLAY },
                    ) {
                        Text("Toggle")
                    }
                }
            }
        }
    }
}

private const val TOGGLE_DISPLAY = "TOGGLE_DISPLAY"
