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

package androidx.wear.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import androidx.wear.compose.material.Scaffold

class PositionIndicatorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val fraction = remember {
                mutableFloatStateOf(0f)
            }
            val sizeFraction = remember {
                mutableFloatStateOf(.25f)
            }

            val visibility = remember {
                mutableStateOf(PositionIndicatorVisibility.Show)
            }

            val pIState = remember { CustomState(fraction, sizeFraction, visibility) }

            Scaffold(modifier = Modifier.fillMaxSize(), positionIndicator = {
                PositionIndicator(
                    state = pIState,
                    indicatorHeight = 50.dp,
                    indicatorWidth = 4.dp,
                    paddingHorizontal = 5.dp
                )
            }) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            visibility.value = when (visibility.value) {
                                PositionIndicatorVisibility.Show ->
                                    PositionIndicatorVisibility.AutoHide

                                PositionIndicatorVisibility.AutoHide ->
                                    PositionIndicatorVisibility.Hide

                                PositionIndicatorVisibility.Hide ->
                                    PositionIndicatorVisibility.Show

                                else -> throw IllegalArgumentException("Invalid visibility type")
                            }
                        }
                        .semantics {
                            contentDescription = CHANGE_VISIBILITY
                        }
                    )

                    Box(modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            fraction.floatValue += 0.05f
                        }
                        .semantics {
                            contentDescription = INCREASE_POSITION
                        }
                    )

                    Box(modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            fraction.floatValue -= 0.05f
                        }
                        .semantics {
                            contentDescription = DECREASE_POSITION
                        }
                    )
                }
            }
        }
    }

    private class CustomState(
        private val fraction: State<Float>,
        private val sizeFraction: State<Float>,
        private val visibility: State<PositionIndicatorVisibility>
    ) : PositionIndicatorState {
        override val positionFraction: Float
            get() = fraction.value

        override fun sizeFraction(scrollableContainerSizePx: Float): Float = sizeFraction.value

        override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility =
            visibility.value
    }
}

private const val INCREASE_POSITION = "PI_INCREASE_POSITION"
private const val DECREASE_POSITION = "PI_DECREASE_POSITION"
private const val CHANGE_VISIBILITY = "PI_VISIBILITY"
