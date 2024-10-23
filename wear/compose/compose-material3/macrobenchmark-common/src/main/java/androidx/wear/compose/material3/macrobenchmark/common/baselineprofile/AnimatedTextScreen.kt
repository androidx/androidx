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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.macrobenchmark.common.FIND_OBJECT_TIMEOUT_MS
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.R
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import kotlinx.coroutines.launch

val AnimatedTextScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        val scope = rememberCoroutineScope()
                        val animatable = remember { Animatable(0.5f) }
                        val textStyle = remember {
                            TextStyle.Default.copy(
                                fontFamily = Font(R.font.robotoflex_variable).toFontFamily(),
                                fontSize = 50.sp
                            )
                        }
                        val fontRegistry =
                            rememberAnimatedTextFontRegistry(
                                startFontVariationSettings =
                                    FontVariation.Settings(
                                        FontVariation.width(10f),
                                        FontVariation.weight(100)
                                    ),
                                endFontVariationSettings =
                                    FontVariation.Settings(
                                        FontVariation.width(100f),
                                        FontVariation.weight(900)
                                    ),
                                textStyle = textStyle
                            )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var textValue by remember { mutableIntStateOf(150) }
                            Box(
                                modifier =
                                    Modifier.weight(1f)
                                        .semantics { contentDescription = MinusContentDescription }
                                        .clickable {
                                            textValue -= 1
                                            scope.launch {
                                                animatable.animateTo(0f)
                                                animatable.animateTo(0.5f)
                                            }
                                        },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = 30.sp, textAlign = TextAlign.Center)
                            }
                            Box(
                                modifier = Modifier.weight(2f),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedText(
                                    text = textValue.toString(),
                                    fontRegistry = fontRegistry,
                                    progressFraction = { animatable.value }
                                )
                            }
                            Box(
                                modifier =
                                    Modifier.weight(1f)
                                        .semantics { contentDescription = PlusContentDescription }
                                        .clickable {
                                            textValue += 1
                                            scope.launch {
                                                animatable.animateTo(1f)
                                                animatable.animateTo(0.5f)
                                            }
                                        },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 30.sp)
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                device.wait(
                    Until.findObject(By.desc(MinusContentDescription)),
                    FIND_OBJECT_TIMEOUT_MS
                )
                repeat(3) { device.findObject(By.desc(MinusContentDescription)).click() }
                repeat(3) { device.findObject(By.desc(PlusContentDescription)).click() }
                device.waitForIdle()
            }
    }

private const val MinusContentDescription = "MinusContentDescription"
private const val PlusContentDescription = "PlusContentDescription"
