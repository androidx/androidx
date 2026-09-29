/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testapp.followingsubspace

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.animation.follow.FollowMode
import androidx.xr.compose.subspace.animation.follow.FollowTarget
import androidx.xr.compose.subspace.animation.follow.FollowThresholds
import androidx.xr.compose.subspace.animation.follow.TrackedDimensions
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode

private enum class UiBehaviorSelection {
    SOFT,
    TIGHT,
    SNAP,
}

class ViewFollowingSubspaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent { MainContent() }
    }

    @SuppressLint("RestrictedApiAndroidX")
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent() {
        val session = LocalSession.current ?: return
        session.configure(
            Config.Builder(session.config).setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
        )

        var uiBehaviorSelection by remember { mutableStateOf(UiBehaviorSelection.SOFT) }

        var isXTracked by remember { mutableStateOf(true) }
        var isYTracked by remember { mutableStateOf(true) }
        var isZTracked by remember { mutableStateOf(true) }
        var isPitchTracked by remember { mutableStateOf(true) }
        var isYawTracked by remember { mutableStateOf(true) }
        var isRollTracked by remember { mutableStateOf(true) }

        var softHalfLifeMs by remember { mutableFloatStateOf(175f) }
        var softStartDelay by remember { mutableFloatStateOf(200f) }
        var softStartTranslationThreshold by remember { mutableFloatStateOf(0.2f) }
        var softStartPitchThreshold by remember { mutableFloatStateOf(16.0f) }
        var softStartYawThreshold by remember { mutableFloatStateOf(24.0f) }
        var softStartRollThreshold by remember { mutableFloatStateOf(20.0f) }

        val selectedMode =
            remember(
                uiBehaviorSelection,
                isXTracked,
                isYTracked,
                isZTracked,
                isPitchTracked,
                isYawTracked,
                isRollTracked,
                softHalfLifeMs,
                softStartDelay,
                softStartTranslationThreshold,
                softStartPitchThreshold,
                softStartYawThreshold,
                softStartRollThreshold,
            ) {
                val dimensions =
                    TrackedDimensions(
                        isXTracked = isXTracked,
                        isYTracked = isYTracked,
                        isZTracked = isZTracked,
                        isPitchTracked = isPitchTracked,
                        isYawTracked = isYawTracked,
                        isRollTracked = isRollTracked,
                    )
                val startThresholds =
                    FollowThresholds(
                        translationMeters = softStartTranslationThreshold,
                        pitchDegrees = softStartPitchThreshold,
                        yawDegrees = softStartYawThreshold,
                        rollDegrees = softStartRollThreshold,
                    )
                when (uiBehaviorSelection) {
                    UiBehaviorSelection.SOFT ->
                        FollowMode.soft(
                            dimensions = dimensions,
                            halfLifeMillis = softHalfLifeMs.toLong(),
                            startDelay = softStartDelay.toLong(),
                            startThresholds = startThresholds,
                        )
                    UiBehaviorSelection.TIGHT -> FollowMode.tight(dimensions = dimensions)
                    UiBehaviorSelection.SNAP -> FollowMode.snap(dimensions = dimensions)
                }
            }

        Subspace(follow = FollowTarget.view(selectedMode)) {
            val panelHeight =
                if (uiBehaviorSelection == UiBehaviorSelection.SOFT) 600.dp else 270.dp
            SpatialPanel(SubspaceModifier.height(panelHeight).width(500.dp).scale(.7f).movable()) {
                Box(Modifier.fillMaxSize().background(Color.Cyan)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TopBarWithBackArrow(
                            scrollBehavior = null,
                            title = "",
                            onClick = { this@ViewFollowingSubspaceActivity.finish() },
                        )
                    }
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(top = 70.dp, bottom = 32.dp)
                                .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        ) {
                            Text(
                                "Translation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(90.dp),
                            )
                            CheckboxWithLabel("X", isXTracked) { isXTracked = it }
                            CheckboxWithLabel("Y", isYTracked) { isYTracked = it }
                            CheckboxWithLabel("Z", isZTracked) { isZTracked = it }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                        ) {
                            Text(
                                "Rotation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(90.dp),
                            )
                            CheckboxWithLabel("Pitch", isPitchTracked) { isPitchTracked = it }
                            CheckboxWithLabel("Yaw", isYawTracked) { isYawTracked = it }
                            CheckboxWithLabel("Roll", isRollTracked) { isRollTracked = it }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.clickable {
                                        uiBehaviorSelection = UiBehaviorSelection.SOFT
                                    },
                            ) {
                                RadioButton(
                                    selected = (uiBehaviorSelection == UiBehaviorSelection.SOFT),
                                    onClick = { uiBehaviorSelection = UiBehaviorSelection.SOFT },
                                )
                                Text("Soft", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.clickable {
                                        uiBehaviorSelection = UiBehaviorSelection.TIGHT
                                    },
                            ) {
                                RadioButton(
                                    selected = (uiBehaviorSelection == UiBehaviorSelection.TIGHT),
                                    onClick = { uiBehaviorSelection = UiBehaviorSelection.TIGHT },
                                )
                                Text("Tight", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.clickable {
                                        uiBehaviorSelection = UiBehaviorSelection.SNAP
                                    },
                            ) {
                                RadioButton(
                                    selected = (uiBehaviorSelection == UiBehaviorSelection.SNAP),
                                    onClick = { uiBehaviorSelection = UiBehaviorSelection.SNAP },
                                )
                                Text("Snap", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (uiBehaviorSelection == UiBehaviorSelection.SOFT) {
                            SliderWithLabel(
                                label = "HalfLife",
                                value = softHalfLifeMs,
                                valueRange = 1f..1000f,
                                formatValue = { "${it.toInt()}ms" },
                                onValueChange = { softHalfLifeMs = it },
                            )
                            SliderWithLabel(
                                label = "StartDelay",
                                value = softStartDelay,
                                valueRange = 0f..1000f,
                                formatValue = { "${it.toInt()}ms" },
                                onValueChange = { softStartDelay = it },
                            )
                            Text(
                                "Start Thresholds",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            SliderWithLabel(
                                label = "  translation",
                                value = softStartTranslationThreshold,
                                valueRange = 0f..1f,
                                formatValue = { "${(it * 100).toInt() / 100f}m" },
                                onValueChange = { softStartTranslationThreshold = it },
                            )
                            SliderWithLabel(
                                label = "  pitch",
                                value = softStartPitchThreshold,
                                valueRange = 0f..30f,
                                formatValue = { "${it.toInt()}°" },
                                onValueChange = { softStartPitchThreshold = it },
                            )
                            SliderWithLabel(
                                label = "  yaw",
                                value = softStartYawThreshold,
                                valueRange = 0f..30f,
                                formatValue = { "${it.toInt()}°" },
                                onValueChange = { softStartYawThreshold = it },
                            )
                            SliderWithLabel(
                                label = "  roll",
                                value = softStartRollThreshold,
                                valueRange = 0f..30f,
                                formatValue = { "${it.toInt()}°" },
                                onValueChange = { softStartRollThreshold = it },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CheckboxWithLabel(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onCheckedChange(!checked) },
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(label, fontSize = 12.sp)
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    private fun SliderWithLabel(
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        formatValue: (Float) -> String,
        onValueChange: (Float) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(110.dp),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
            )
            Text(formatValue(value), fontSize = 12.sp, modifier = Modifier.width(55.dp))
        }
    }
}
