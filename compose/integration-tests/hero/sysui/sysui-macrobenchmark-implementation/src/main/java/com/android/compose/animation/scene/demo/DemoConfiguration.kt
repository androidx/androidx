/*
 * Copyright (C) 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.compose.animation.scene.demo

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class DemoConfiguration(
    val notificationsInLockscreen: Int = 2,
    val notificationsInShade: Int = 10,
    val quickSettingsRows: Int = 4,
    val interactiveNotifications: Boolean = false,
    val showMediaPlayer: Boolean = true,
    val isFullscreen: Boolean = false,
    val canChangeSceneOrOverlays: Boolean = true,
    val transitionInterceptionThreshold: Float = 0.05f,
    val motion: MotionConfig = MotionConfig.Default,
    val lsToShadeRequiresFullSwipe: ToggleableState = ToggleableState.Indeterminate,
    val enableOverlays: Boolean = false,
    val transitionBorder: Boolean = true,
    val deferTransitionProgress: Boolean = false,
    val firstCompositionDelay: Long = 0L,
    val qsRevealEffect: Boolean = false,
    val enableMotionValueDebugger: Boolean = false,
) {
    companion object {
        val Saver = run {
            val notificationsInLockscreenKey = "notificationsInLockscreen"
            val notificationsInShadeKey = "notificationsInShade"
            val quickSettingsRowsKey = "quickSettingsRows"
            val interactiveNotificationsKey = "interactiveNotifications"
            val showMediaPlayerKey = "showMediaPlayer"
            val isFullscreenKey = "isFullscreen"
            val canChangeSceneOrOverlaysKey = "canChangeSceneOrOverlays"
            val transitionInterceptionThresholdKey = "transitionInterceptionThreshold"
            val motionSchemeKey = "motionScheme"
            val lsToShadeRequiresFullSwipe = "lsToShadeRequiresFullSwipe"
            val enableOverlays = "enableOverlays"
            val transitionBorder = "transitionBorder"
            val deferTransitionProgress = "deferTransitionProgress"
            val firstCompositionDelay = "firstCompositionDelay"
            val qsRevealEffect = "qsRevealEffect"
            val enableMotionValueDebugger = "enableMotionValueDebugger"

            mapSaver(
                save = {
                    mapOf(
                        notificationsInLockscreenKey to it.notificationsInLockscreen,
                        notificationsInShadeKey to it.notificationsInShade,
                        quickSettingsRowsKey to it.quickSettingsRows,
                        interactiveNotificationsKey to it.interactiveNotifications,
                        showMediaPlayerKey to it.showMediaPlayer,
                        isFullscreenKey to it.isFullscreen,
                        canChangeSceneOrOverlaysKey to it.canChangeSceneOrOverlays,
                        transitionInterceptionThresholdKey to it.transitionInterceptionThreshold,
                        motionSchemeKey to it.motion.name,
                        lsToShadeRequiresFullSwipe to it.lsToShadeRequiresFullSwipe,
                        enableOverlays to it.enableOverlays,
                        transitionBorder to it.transitionBorder,
                        deferTransitionProgress to it.deferTransitionProgress,
                        firstCompositionDelay to it.firstCompositionDelay,
                        qsRevealEffect to it.qsRevealEffect,
                        enableMotionValueDebugger to it.enableMotionValueDebugger,
                    )
                },
                restore = {
                    DemoConfiguration(
                        notificationsInLockscreen = it[notificationsInLockscreenKey] as Int,
                        notificationsInShade = it[notificationsInShadeKey] as Int,
                        quickSettingsRows = it[quickSettingsRowsKey] as Int,
                        interactiveNotifications = it[interactiveNotificationsKey] as Boolean,
                        showMediaPlayer = it[showMediaPlayerKey] as Boolean,
                        isFullscreen = it[isFullscreenKey] as Boolean,
                        canChangeSceneOrOverlays = it[canChangeSceneOrOverlaysKey] as Boolean,
                        transitionInterceptionThreshold =
                            it[transitionInterceptionThresholdKey] as Float,
                        motion = MotionConfig.fromName(it[motionSchemeKey] as String),
                        lsToShadeRequiresFullSwipe =
                            it[lsToShadeRequiresFullSwipe] as ToggleableState,
                        enableOverlays = it[enableOverlays] as Boolean,
                        transitionBorder = it[transitionBorder] as Boolean,
                        deferTransitionProgress = it[deferTransitionProgress] as Boolean,
                        firstCompositionDelay = it[firstCompositionDelay] as Long,
                        qsRevealEffect = it[qsRevealEffect] as Boolean,
                        enableMotionValueDebugger = it[enableMotionValueDebugger] as Boolean,
                    )
                },
            )
        }
    }
}

class MotionConfig(val name: String, val scheme: MotionScheme) {
    companion object {
        val Options =
            listOf(
                MotionConfig("standard", MotionScheme.standard()),
                MotionConfig("expressive", MotionScheme.expressive()),
                MotionConfig(
                    "bouncy",
                    CustomMotionScheme(
                        spatial = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessVeryLow),
                        effects = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow),
                    ),
                ),
                MotionConfig(
                    "stiff",
                    CustomMotionScheme(
                        spatial = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessHigh),
                        effects = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    ),
                ),
                MotionConfig(
                    "high bouncy & stiff",
                    CustomMotionScheme(
                        spatial = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
                        effects = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    ),
                ),
                MotionConfig(
                    "low bouncy & stiff",
                    CustomMotionScheme(
                        spatial = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow),
                        effects = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow),
                    ),
                ),
            )

        val Default: MotionConfig = Options[1]

        fun fromName(name: String) = Options.first { it.name == name }
    }

    // Implementation inspired by MotionScheme.standard()
    @Suppress("UNCHECKED_CAST")
    class CustomMotionScheme(
        private val spatial: FiniteAnimationSpec<Any>,
        private val effects: FiniteAnimationSpec<Any>,
        private val fastSpatial: FiniteAnimationSpec<Any> = spatial,
        private val fastEffects: FiniteAnimationSpec<Any> = effects,
        private val slowSpatial: FiniteAnimationSpec<Any> = spatial,
        private val slowEffects: FiniteAnimationSpec<Any> = effects,
    ) : MotionScheme {
        override fun <T> defaultSpatialSpec() = spatial as FiniteAnimationSpec<T>

        override fun <T> fastSpatialSpec() = fastSpatial as FiniteAnimationSpec<T>

        override fun <T> slowSpatialSpec() = slowSpatial as FiniteAnimationSpec<T>

        override fun <T> defaultEffectsSpec() = effects as FiniteAnimationSpec<T>

        override fun <T> fastEffectsSpec() = fastEffects as FiniteAnimationSpec<T>

        override fun <T> slowEffectsSpec() = slowEffects as FiniteAnimationSpec<T>
    }
}

@Composable
fun DemoConfigurationDialog(
    configuration: DemoConfiguration,
    onConfigurationChange: (DemoConfiguration) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Demo configuration") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = "Generic app settings", style = MaterialTheme.typography.titleMedium)

                // Fullscreen.
                Checkbox(
                    label = "Fullscreen",
                    checked = configuration.isFullscreen,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(isFullscreen = !configuration.isFullscreen)
                        )
                    },
                )

                // Can change scene.
                Checkbox(
                    label = "Can change scene or overlays",
                    checked = configuration.canChangeSceneOrOverlays,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                canChangeSceneOrOverlays = !configuration.canChangeSceneOrOverlays
                            )
                        )
                    },
                )

                // Overlays.
                Checkbox(
                    label = "Overlays",
                    checked = configuration.enableOverlays,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(enableOverlays = !configuration.enableOverlays)
                        )
                    },
                )

                // Transition border.
                Checkbox(
                    label = "Transition border",
                    checked = configuration.transitionBorder,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(transitionBorder = !configuration.transitionBorder)
                        )
                    },
                )

                // Defer transition progress.
                Checkbox(
                    label = "Defer transition progress",
                    checked = configuration.deferTransitionProgress,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                deferTransitionProgress = !configuration.deferTransitionProgress
                            )
                        )
                    },
                )

                Checkbox(
                    label = "MotionValue debugger",
                    checked = configuration.enableMotionValueDebugger,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                enableMotionValueDebugger = !configuration.enableMotionValueDebugger
                            )
                        )
                    },
                )

                // First composition delay.
                Text(text = "First composition delay: ${configuration.firstCompositionDelay}ms")
                Slider(
                    value = configuration.firstCompositionDelay,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(firstCompositionDelay = it))
                    },
                    values = List(21) { it * 10L },
                    onValueNotFound = { 0 },
                )

                Text(text = "Theme", style = MaterialTheme.typography.titleMedium)

                Text(text = "Motion: ${configuration.motion.name}")
                Slider(
                    value = configuration.motion,
                    onValueChange = { onConfigurationChange(configuration.copy(motion = it)) },
                    values = MotionConfig.Options,
                    onValueNotFound = { 0 },
                )

                Text(text = "Scrollable", style = MaterialTheme.typography.titleMedium)

                // Interception threshold.
                val thresholdString =
                    String.format("%.2f", configuration.transitionInterceptionThreshold)
                Text(text = "Interception threshold: $thresholdString")
                Slider(
                    value = configuration.transitionInterceptionThreshold,
                    onValueChange = {
                        onConfigurationChange(
                            configuration.copy(transitionInterceptionThreshold = it)
                        )
                    },
                    valueRange = 0f..0.5f,
                    stepSize = 0.01f,
                )

                Text(text = "Media", style = MaterialTheme.typography.titleMedium)

                // Whether we should show the media player.
                Checkbox(
                    label = "Show media player",
                    checked = configuration.showMediaPlayer,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(showMediaPlayer = !configuration.showMediaPlayer)
                        )
                    },
                )

                Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)

                // Whether notifications are interactive
                Checkbox(
                    label = "Interactive notifications",
                    checked = configuration.interactiveNotifications,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                interactiveNotifications = !configuration.interactiveNotifications
                            )
                        )
                    },
                )

                // Number of notifications in the Shade scene.
                Counter(
                    "# notifications in Shade",
                    configuration.notificationsInShade,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(notificationsInShade = it))
                    },
                )

                // Number of notifications in the Lockscreen scene.
                Counter(
                    "# notifications in Lockscreen",
                    configuration.notificationsInLockscreen,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(notificationsInLockscreen = it))
                    },
                )

                Text(text = "Quick Settings", style = MaterialTheme.typography.titleMedium)

                Counter(
                    "# quick settings rows",
                    configuration.quickSettingsRows,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(quickSettingsRows = it))
                    },
                )

                Checkbox(
                    label = "Show reveal effect (DualShade only)",
                    checked = configuration.qsRevealEffect,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(qsRevealEffect = !configuration.qsRevealEffect)
                        )
                    },
                )

                Text(text = "Lockscreen", style = MaterialTheme.typography.titleMedium)

                // Whether the LS => Shade transition requires a full distance swipe to be
                // committed.
                Checkbox(
                    label = "Require full LS => Shade swipe",
                    state = configuration.lsToShadeRequiresFullSwipe,
                    onStateChange = {
                        onConfigurationChange(configuration.copy(lsToShadeRequiresFullSwipe = it))
                    },
                )
            }
        },
        confirmButton = { Button(onClick = { onDismissRequest() }) { Text("Done") } },
        dismissButton = {
            Button(onClick = { onConfigurationChange(DemoConfiguration()) }) { Text("Reset") }
        },
    )
}

@Composable
private fun Checkbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Checkbox(
    label: String,
    state: ToggleableState,
    onStateChange: (ToggleableState) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onClick() {
        onStateChange(
            when (state) {
                ToggleableState.On -> ToggleableState.Off
                ToggleableState.Off -> ToggleableState.Indeterminate
                ToggleableState.Indeterminate -> ToggleableState.On
            }
        )
    }

    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = ::onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(state, onClick = ::onClick)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Counter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) {
            Icon(Icons.Default.Remove, null)
        }
        Text(value.toString(), Modifier.width(18.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onValueChange((value + 1)) }) { Icon(Icons.Default.Add, null) }
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun <T> Slider(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    onValueNotFound: () -> Int = { 0 },
) {
    Slider(
        value = (values.indexOf(value).takeIf { it != -1 } ?: onValueNotFound()).toFloat(),
        onValueChange = { onValueChange(values[it.roundToInt()]) },
        valueRange = 0f..values.lastIndex.toFloat(),
        steps = values.lastIndex - 1,
    )
}

@Composable
private fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    stepSize: Float,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = ((valueRange.endInclusive - valueRange.start) / stepSize).toInt() - 1,
    )
}
