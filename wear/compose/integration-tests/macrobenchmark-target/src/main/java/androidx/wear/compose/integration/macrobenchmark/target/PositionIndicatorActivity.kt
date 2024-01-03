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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text

class PositionIndicatorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var fraction by remember {
                mutableFloatStateOf(0f)
            }
            var sizeFraction by remember {
                mutableFloatStateOf(.25f)
            }

            var visibility by remember {
                mutableStateOf(PositionIndicatorVisibility.Show)
            }

            val pIState = CustomState(fraction, sizeFraction, visibility)

            Scaffold(modifier = Modifier.fillMaxSize(), positionIndicator = {
                PositionIndicator(
                    state = pIState,
                    indicatorHeight = 50.dp,
                    indicatorWidth = 4.dp,
                    paddingHorizontal = 5.dp
                )
            }) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(.8f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.semantics {
                            contentDescription = CHANGE_VISIBILITY
                        }, onClick = {
                            visibility = when (visibility) {
                                PositionIndicatorVisibility.AutoHide ->
                                    PositionIndicatorVisibility.Hide
                                PositionIndicatorVisibility.Hide ->
                                    PositionIndicatorVisibility.Show
                                PositionIndicatorVisibility.Show ->
                                    PositionIndicatorVisibility.AutoHide
                                else -> throw IllegalArgumentException("Invalid visibility type")
                            }
                        }) {
                        val text = when (visibility) {
                            PositionIndicatorVisibility.Show -> "Show"
                            PositionIndicatorVisibility.AutoHide -> "Auto Hide"
                            else -> "Hide"
                        }
                        Text(text = text)
                    }

                    Button(
                        modifier = Modifier.semantics {
                            contentDescription = INCREASE_POSITION
                        }, onClick = {
                            fraction += 0.05f
                        }) {
                        Text(text = "Increase position fraction")
                    }

                    Button(
                        modifier = Modifier.semantics {
                            contentDescription = DECREASE_POSITION
                        }, onClick = {
                            fraction -= 0.05f
                        }) {
                        Text(text = "Decrease position fraction")
                    }
                }
            }
        }
    }

    private class CustomState(
        private val fraction: Float,
        private val sizeFraction: Float,
        private val visibility: PositionIndicatorVisibility
    ) : PositionIndicatorState {
        override val positionFraction: Float
            get() = fraction

        override fun sizeFraction(scrollableContainerSizePx: Float): Float = sizeFraction

        override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility =
            visibility
    }
}

private const val INCREASE_POSITION = "PI_INCREASE_POSITION"
private const val DECREASE_POSITION = "PI_DECREASE_POSITION"
private const val CHANGE_VISIBILITY = "PI_VISIBILITY"
