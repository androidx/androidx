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

package androidx.xr.compose.testapp.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CUJButton
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme

class SampleAnimations : ComponentActivity() {

    enum class AnimationStyle {
        SequentialExample,
        ConcurrentExample1,
        ConcurrentExample2,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IntegrationTestsAppTheme { SampleAnimationsExamples() } }
    }

    @Composable
    @SubspaceComposable
    private fun SampleAnimationsExamples() {
        val (animationStyle, setAnimationStyle) =
            remember { mutableStateOf(AnimationStyle.SequentialExample) }
        MainPanelContent(setAnimationStyle)
        Subspace {
            SpatialCurvedRow(modifier = SubspaceModifier.fillMaxSize(), curveRadius = 1025.dp) {
                SpatialMainPanel(modifier = SubspaceModifier.width(600.dp).height(400.dp))
                SpatialColumn(modifier = SubspaceModifier.padding(50.dp)) {
                    when (animationStyle) {
                        AnimationStyle.SequentialExample -> {
                            SequentialAnimationExample()
                        }
                        AnimationStyle.ConcurrentExample1 -> {
                            ConcurrentAnimationExample1()
                        }
                        AnimationStyle.ConcurrentExample2 -> {
                            ConcurrentAnimationExample2()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainPanelContent(setAnimationStyle: (AnimationStyle) -> Unit) {
        CommonTestScaffold(
            title = getString(R.string.sample_animations_test),
            showBottomBar = true,
            bottomBarText = "",
            onClickBackArrow = { this@SampleAnimations.finish() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CUJButton("Sequential animation") {
                    setAnimationStyle(AnimationStyle.SequentialExample)
                }
                CUJButton("Concurrent animation") {
                    setAnimationStyle(AnimationStyle.ConcurrentExample1)
                }
                CUJButton("Concurrent animation 2") {
                    setAnimationStyle(AnimationStyle.ConcurrentExample2)
                }
            }
        }
    }
}
