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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.compose.animation.scene.demo

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeInactive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChargingStation
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NearbyOff
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.DefaultEdgeDetector
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.OverlayKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.SceneTransitions
import com.android.compose.animation.scene.demo.notification.NotificationList
import com.android.compose.animation.scene.demo.notification.notifications
import com.android.compose.animation.scene.demo.transitions.systemUiTransitions
import com.android.compose.gesture.effect.rememberOffsetOverscrollEffectFactory
import com.android.compose.modifiers.height
import com.android.compose.modifiers.thenIf
import com.android.mechanics.behavior.VerticalExpandContainerSpec
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.LocalMotionValueDebugController
import com.android.mechanics.debug.MotionValueDebuggerProvider
import kotlin.math.max

object Scenes {
    val AlwaysOnDisplay = SceneKey("AlwaysOnDisplay")
    val Bouncer = SceneKey("Bouncer")
    val Camera = SceneKey("Camera")
    val Launcher = SceneKey("Launcher")
    val Lockscreen = SceneKey("Lockscreen")
    val SplitLockscreen = SceneKey("SplitLockscreen")
    val QuickSettings = SceneKey("QuickSettings")
    val Shade = SceneKey("Shade")
    val SplitShade = SceneKey("SplitShade")

    // Stub scenes on the start and end of the lockscreen.
    val StubStart = SceneKey("StubStart")
    val StubEnd = SceneKey("StubEnd")

    val AllScenes =
        listOf(
                AlwaysOnDisplay,
                Bouncer,
                Camera,
                Launcher,
                Lockscreen,
                SplitLockscreen,
                QuickSettings,
                Shade,
                SplitShade,
                StubStart,
                StubEnd,
            )
            .associateBy { it.debugName }

    /**
     * A smart saver that restores the right scene depending on the current [lockscreenScene] and
     * [shadeScene].
     */
    class SceneSaver(private val lockscreenScene: SceneKey, private val shadeScene: SceneKey) :
        Saver<SceneKey, String> {
        override fun SaverScope.save(value: SceneKey): String = value.debugName

        override fun restore(value: String): SceneKey {
            return ensureCorrectScene(AllScenes.getValue(value), lockscreenScene, shadeScene)
        }
    }

    fun ensureCorrectScene(
        scene: SceneKey,
        lockscreenScene: SceneKey,
        shadeScene: SceneKey,
    ): SceneKey {
        return when (scene) {
            Lockscreen,
            SplitLockscreen -> lockscreenScene
            Shade,
            SplitShade -> shadeScene
            // We should never be in the QuickSettings page if the SplitShade is a possible scene.
            QuickSettings -> if (shadeScene == SplitShade) SplitShade else QuickSettings
            else -> scene
        }
    }
}

object Overlays {
    val Notifications = OverlayKey("NotificationsOverlay")
    val QuickSettings = OverlayKey("QuickSettingsOverlay")
}

/** A [Saver] that restores a [MutableSceneTransitionLayoutState] to its previous [currentScene]. */
class MutableSceneTransitionLayoutSaver(
    private val sceneSaver: Scenes.SceneSaver,
    private val transitions: SceneTransitions,
    private val canChangeScene: (SceneKey) -> Boolean,
    private val motionScheme: MotionScheme,
) : Saver<MutableSceneTransitionLayoutState, String> {
    override fun SaverScope.save(value: MutableSceneTransitionLayoutState): String {
        val currentScene = value.transitionState.currentScene
        return with(sceneSaver) { save(currentScene) }
    }

    override fun restore(value: String): MutableSceneTransitionLayoutState {
        val currentScene = sceneSaver.restore(value)
        @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
        return MutableSceneTransitionLayoutState(
            initialScene = currentScene,
            motionScheme = motionScheme,
            transitions = transitions,
            canChangeScene = canChangeScene,
        )
    }
}

@Composable
fun SystemUi(modifier: Modifier = Modifier) {
    var configuration by
        rememberSaveable(stateSaver = DemoConfiguration.Saver) {
            mutableStateOf(DemoConfiguration())
        }
    MotionValueDebuggerProvider(enableDebugger = configuration.enableMotionValueDebugger) {
        SystemUi(configuration, { configuration = it }, modifier)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SystemUi(
    configuration: DemoConfiguration,
    onConfigurationChange: (DemoConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    initialScene: SceneKey? = null,
) {
    val windowSizeClass = calculateWindowSizeClass()
    val shouldUseSplitScenes = shouldUseSplitScenes(windowSizeClass)

    val lockscreenScene: SceneKey
    val shadeScene: SceneKey
    val launcherColumns: Int
    if (shouldUseSplitScenes) {
        lockscreenScene = Scenes.SplitLockscreen
        shadeScene = Scenes.SplitShade
        launcherColumns = 8
    } else {
        lockscreenScene = Scenes.Lockscreen
        shadeScene = Scenes.Shade
        launcherColumns = 4
    }

    val notificationCountInLockscreen = configuration.notificationsInLockscreen
    val notificationCount = max(notificationCountInLockscreen, configuration.notificationsInShade)
    val interactiveNotifications = configuration.interactiveNotifications
    val notificationTextMeasurer = rememberTextMeasurer(cacheSize = notificationCount * 2)
    val motionScheme = MaterialTheme.motionScheme
    val notifications =
        remember(
            interactiveNotifications,
            notificationCount,
            notificationCountInLockscreen,
            notificationTextMeasurer,
            motionScheme,
        ) {
            notifications(
                interactiveNotifications,
                notificationCount,
                notificationCountInLockscreen,
                notificationTextMeasurer,
                motionScheme,
            )
        }
    val expectedQsSize = 12
    val quickSettingsTextMeasurer = rememberTextMeasurer(cacheSize = expectedQsSize * 2)
    val quickSettingsTiles = remember { quickSettingsTiles(quickSettingsTextMeasurer) }
    check(expectedQsSize == quickSettingsTiles.size)

    var isLockscreenDismissable by remember { mutableStateOf(false) }
    var isLockscreenDismissed by remember { mutableStateOf(false) }
    var showConfigurationDialog by remember { mutableStateOf(false) }

    val nQuickSettingsColumns =
        if (configuration.enableOverlays) {
            2
        } else {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 2
                WindowWidthSizeClass.Medium,
                WindowWidthSizeClass.Expanded ->
                    when (windowSizeClass.heightSizeClass) {
                        // Phone landscape.
                        WindowHeightSizeClass.Compact -> 2
                        else -> 3
                    }

                else -> error("Unknown size class: ${windowSizeClass.widthSizeClass}")
            }
        }
    val nQuickSettingsRow = configuration.quickSettingsRows
    val nQuickSettingsSplitShadeRows = nQuickSettingsColumns

    // The state of the quick settings pager in the phone (one column) layout.
    val nQuickSettingsPages =
        nQuickSettingsPages(
            nTiles = quickSettingsTiles.size,
            nRows = nQuickSettingsRow,
            nColumns = nQuickSettingsColumns,
        )
    val quickSettingsPagerState = rememberPagerState { nQuickSettingsPages }

    val hapticFeedback = LocalHapticFeedback.current
    val revealHaptics = remember(hapticFeedback) { DemoContainerRevealHaptics(hapticFeedback) }
    val shadeMotionSpec =
        remember(shouldUseSplitScenes) {
            VerticalExpandContainerSpec(isFloating = shouldUseSplitScenes)
        }
    val transitions =
        remember(quickSettingsPagerState, revealHaptics, shouldUseSplitScenes) {
            systemUiTransitions(quickSettingsPagerState, revealHaptics, shadeMotionSpec)
        }

    val sceneSaver =
        remember(lockscreenScene, shadeScene) { Scenes.SceneSaver(lockscreenScene, shadeScene) }

    fun maybeUpdateLockscreenDismissed(scene: SceneKey) {
        when (scene) {
            Scenes.Launcher -> isLockscreenDismissed = true
            Scenes.Lockscreen,
            Scenes.SplitLockscreen -> isLockscreenDismissed = false

            else -> {}
        }
    }

    val canChangeScene =
        remember(configuration) {
            { scene: SceneKey ->
                if (configuration.canChangeSceneOrOverlays) {
                    maybeUpdateLockscreenDismissed(scene)
                    true
                } else {
                    false
                }
            }
        }

    val stateSaver =
        remember(sceneSaver, transitions, canChangeScene, motionScheme) {
            MutableSceneTransitionLayoutSaver(
                sceneSaver = sceneSaver,
                transitions = transitions,
                canChangeScene = canChangeScene,
                motionScheme = motionScheme,
            )
        }
    val layoutState =
        rememberSaveable(
            transitions,
            canChangeScene,
            configuration,
            motionScheme,
            saver = stateSaver,
        ) {
            val initialScene =
                initialScene?.let {
                    Scenes.ensureCorrectScene(
                        initialScene,
                        lockscreenScene = lockscreenScene,
                        shadeScene = shadeScene,
                    )
                } ?: lockscreenScene

            @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
            MutableSceneTransitionLayoutState(
                initialScene = initialScene,
                motionScheme = motionScheme,
                transitions = transitions,
                canChangeScene = canChangeScene,
                canShowOverlay = { configuration.canChangeSceneOrOverlays },
                canHideOverlay = { configuration.canChangeSceneOrOverlays },
                canReplaceOverlay = { _, _ -> configuration.canChangeSceneOrOverlays },
                deferTransitionProgress = configuration.deferTransitionProgress,
            )
        }

    val coroutineScope = rememberCoroutineScope()
    fun onChangeScene(scene: SceneKey) {
        maybeUpdateLockscreenDismissed(scene)

        // Enforce that we are going to the right shade/lockscreen here depending on the windows
        // size class.
        layoutState.setTargetScene(
            Scenes.ensureCorrectScene(scene, lockscreenScene, shadeScene),
            coroutineScope,
        )
    }

    fun onPowerButtonClicked() {
        isLockscreenDismissable = false
        isLockscreenDismissed = false

        if (layoutState.transitionState.currentScene == Scenes.AlwaysOnDisplay) {
            onChangeScene(lockscreenScene)
        } else {
            onChangeScene(Scenes.AlwaysOnDisplay)
        }
    }

    fun onSettingsButtonClicked() {
        showConfigurationDialog = true
    }

    fun onExpandButtonClicked() {
        onConfigurationChange(configuration.copy(isFullscreen = true))
    }

    @Composable
    fun ContentScope.NotificationList(
        maxNotificationCount: Int,
        isScrollable: Boolean = true,
        overscrollEffect: OverscrollEffect? = null,
    ) {
        NotificationList(
            notifications = notifications,
            maxNotificationCount = maxNotificationCount,
            demoConfiguration = configuration,
            isScrollable = isScrollable,
            overscrollEffect = overscrollEffect,
        )
    }

    if (showConfigurationDialog) {
        DemoConfigurationDialog(
            configuration,
            onConfigurationChange,
            onDismissRequest = { showConfigurationDialog = false },
        )
    }

    Column(modifier) {
        if (!configuration.isFullscreen) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                IconButton(::onPowerButtonClicked) { Icon(Icons.Default.PowerSettingsNew, null) }
                IconButton(::onSettingsButtonClicked) { Icon(Icons.Default.Settings, null) }
                IconButton(::onExpandButtonClicked) { Icon(Icons.Default.ZoomOutMap, null) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                            Scenes.AlwaysOnDisplay to "AOD",
                            Scenes.Lockscreen to "Lock",
                            Scenes.Bouncer to "Bouncer",
                            Scenes.Launcher to "Gone",
                            Scenes.Shade to "Shade",
                            Scenes.QuickSettings to "QS",
                        )
                        .forEach { (scene, name) ->
                            Button(onClick = { onChangeScene(scene) }) { Text(name) }
                        }

                    listOf(Overlays.Notifications to "NS", Overlays.QuickSettings to "QSS")
                        .forEach { (overlay, name) ->
                            Button(
                                onClick = {
                                    if (layoutState.currentOverlays.contains(overlay)) {
                                        layoutState.hideOverlay(overlay, coroutineScope)
                                    } else {
                                        layoutState.showOverlay(overlay, coroutineScope)
                                    }
                                }
                            ) {
                                Text(name)
                            }
                        }
                }
            }
        }

        // Provide an easy way to leave full screen mode by going back.
        BackHandler(enabled = configuration.isFullscreen) {
            onConfigurationChange(configuration.copy(isFullscreen = false))
        }

        val shape = RoundedCornerShape(Shade.Dimensions.ScrimCornerSize)
        val borderColor = MaterialTheme.colorScheme.onSurface

        Surface(
            Modifier.semantics { testTagsAsResourceId = true }
                .thenIf(!configuration.isFullscreen) {
                    Modifier.padding(3.dp)
                        .then(
                            if (configuration.transitionBorder) {
                                Modifier.border(
                                    5.dp,
                                    if (layoutState.isTransitioning()) Color.Red else Color.Green,
                                    shape,
                                )
                            } else {
                                Modifier.border(1.dp, borderColor, shape)
                            }
                        )
                        .clip(shape)
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            val stretchOverscrollFactory = LocalOverscrollFactory.current
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                LocalOverscrollFactory provides rememberOffsetOverscrollEffectFactory(),
            ) {
                var isMediaPlayerPlaying by remember { mutableStateOf(false) }
                val mediaPlayer:
                    (@Composable
                    ContentScope.(
                        presentationStyle: DemoMediaPresentationStyle, revealEffect: Boolean,
                    ) -> Unit)? =
                    if (configuration.showMediaPlayer) {
                        { presentationStyle, revealEffect ->
                            MediaPlayer(
                                presentationStyle = presentationStyle,
                                isPlaying = isMediaPlayerPlaying,
                                onIsPlayingChange = { isMediaPlayerPlaying = it },
                                onVisibilityChange = { isVisible ->
                                    onConfigurationChange(
                                        configuration.copy(showMediaPlayer = isVisible)
                                    )
                                },
                                revealEffect = revealEffect,
                            )
                        }
                    } else {
                        null
                    }
                val defaultMediaPlayer: (@Composable ContentScope.() -> Unit)? =
                    mediaPlayer?.let { { it(DemoMediaPresentationStyle.Default, false) } }
                val compactMediaPlayer:
                    (@Composable
                    ContentScope.(revealEffect: Boolean) -> Unit)? =
                    mediaPlayer?.let { player ->
                        { revealEffect -> player(DemoMediaPresentationStyle.Compact, revealEffect) }
                    }

                val qsPager: (@Composable ContentScope.(revealEffect: Boolean) -> Unit) =
                    { revealEffect ->
                        QuickSettingsPager(
                            pagerState = quickSettingsPagerState,
                            tiles = quickSettingsTiles,
                            nRows = nQuickSettingsRow,
                            nColumns = nQuickSettingsColumns,
                            revealEffect = revealEffect,
                        )
                    }

                // SceneTransitionLayout can only be bound to one SceneTransitionLayoutState, so
                // make sure we recompose it fully when we create a new state object.
                key(layoutState) {
                    val overlayEffectFactory =
                        if (shouldUseSplitScenes) null else stretchOverscrollFactory

                    SceneTransitionLayout(
                        state = layoutState,
                        transitionInterceptionThreshold =
                            configuration.transitionInterceptionThreshold,
                        modifier =
                            // Make this layout accessible to UiAutomator.
                            Modifier.thenIf(layoutState.currentTransition == null) {
                                Modifier.testTag("SystemUiSceneTransitionLayout:idle")
                            },
                        swipeSourceDetector =
                            if (configuration.enableOverlays) {
                                remember { SceneContainerSwipeDetector(edgeSize = 60.dp) }
                            } else {
                                DefaultEdgeDetector
                            },
                        implicitTestTags = true,
                    ) {
                        scene(Scenes.Launcher, Launcher.userActions(shadeScene, configuration)) {
                            FirstCompositionDelay(configuration)
                            Launcher(launcherColumns)
                        }
                        scene(
                            Scenes.Lockscreen,
                            Lockscreen.userActions(
                                isLockscreenDismissable,
                                shadeScene,
                                requiresFullDistanceSwipeToShade =
                                    when (configuration.lsToShadeRequiresFullSwipe) {
                                        ToggleableState.On -> true
                                        ToggleableState.Off -> false
                                        ToggleableState.Indeterminate ->
                                            configuration.interactiveNotifications
                                    },
                                configuration,
                            ),
                        ) {
                            FirstCompositionDelay(configuration)
                            Lockscreen(
                                notificationList = {
                                    NotificationList(
                                        maxNotificationCount =
                                            configuration.notificationsInLockscreen
                                    )
                                },
                                mediaPlayer = defaultMediaPlayer,
                                isDismissable = isLockscreenDismissable,
                                onToggleDismissable = {
                                    isLockscreenDismissable = !isLockscreenDismissable
                                },
                                ::onChangeScene,
                            )
                        }
                        scene(
                            Scenes.SplitLockscreen,
                            SplitLockscreen.userActions(
                                isLockscreenDismissable,
                                shadeScene,
                                configuration,
                            ),
                        ) {
                            FirstCompositionDelay(configuration)
                            SplitLockscreen(
                                notificationList = {
                                    NotificationList(
                                        maxNotificationCount =
                                            configuration.notificationsInLockscreen
                                    )
                                },
                                mediaPlayer = defaultMediaPlayer,
                                isDismissable = isLockscreenDismissable,
                                onToggleDismissable = {
                                    isLockscreenDismissable = !isLockscreenDismissable
                                },
                                ::onChangeScene,
                                configuration = configuration,
                            )
                        }
                        scene(Scenes.StubStart, Stub.startUserActions(lockscreenScene)) {
                            FirstCompositionDelay(configuration)
                            Stub(
                                rootKey = Stub.Elements.SceneStart,
                                textKey = Stub.Elements.TextStart,
                                text = "Stub scene (start)",
                            )
                        }
                        scene(Scenes.StubEnd, Stub.endUserActions(lockscreenScene)) {
                            FirstCompositionDelay(configuration)
                            Stub(
                                rootKey = Stub.Elements.SceneEnd,
                                textKey = Stub.Elements.TextEnd,
                                text = "Stub scene (end)",
                            )
                        }
                        scene(Scenes.Camera, Camera.userActions(lockscreenScene)) {
                            FirstCompositionDelay(configuration)
                            Camera()
                        }
                        scene(Scenes.Bouncer, Bouncer.userActions(lockscreenScene)) {
                            FirstCompositionDelay(configuration)
                            Bouncer(
                                onBouncerCancelled = { onChangeScene(lockscreenScene) },
                                onBouncerSolved = { onChangeScene(Scenes.Launcher) },
                            )
                        }
                        scene(
                            Scenes.QuickSettings,
                            QuickSettings.userActions(
                                shadeScene,
                                lockscreenScene,
                                isLockscreenDismissed,
                            ),
                        ) {
                            FirstCompositionDelay(configuration)
                            QuickSettings(
                                qsPager,
                                mediaPlayer = defaultMediaPlayer,
                                ::onSettingsButtonClicked,
                                ::onPowerButtonClicked,
                            )
                        }
                        scene(
                            Scenes.Shade,
                            Shade.userActions(isLockscreenDismissed, lockscreenScene),
                        ) {
                            FirstCompositionDelay(configuration)
                            Shade(
                                notificationList = { overscrollEffect ->
                                    NotificationList(
                                        maxNotificationCount = configuration.notificationsInShade,
                                        overscrollEffect = overscrollEffect,
                                    )
                                },
                                mediaPlayer = defaultMediaPlayer,
                                quickSettingsTiles,
                                nQuickSettingsColumns,
                            )
                        }
                        scene(
                            Scenes.SplitShade,
                            SplitShade.userActions(isLockscreenDismissed, lockscreenScene),
                        ) {
                            FirstCompositionDelay(configuration)
                            SplitShade(
                                notificationList = {
                                    NotificationList(
                                        maxNotificationCount = configuration.notificationsInShade
                                    )
                                },
                                mediaPlayer = defaultMediaPlayer,
                                quickSettingsTiles,
                                nQuickSettingsSplitShadeRows,
                                nQuickSettingsColumns,
                                ::onSettingsButtonClicked,
                                ::onPowerButtonClicked,
                            )
                        }

                        scene(Scenes.AlwaysOnDisplay) {
                            FirstCompositionDelay(configuration)
                            AlwaysOnDisplay(Modifier.clickable { onChangeScene(lockscreenScene) })
                        }

                        overlay(
                            Overlays.QuickSettings,
                            userActions = QuickSettingsShade.UserActions,
                            alignment = Alignment.TopEnd,
                            effectFactory = overlayEffectFactory,
                        ) {
                            FirstCompositionDelay(configuration)
                            QuickSettingsShade(
                                qsPager = qsPager,
                                mediaPlayer = compactMediaPlayer,
                                revealEffect = configuration.qsRevealEffect,
                            )
                        }

                        overlay(
                            Overlays.Notifications,
                            userActions = NotificationShade.UserActions,
                            alignment = Alignment.TopStart,
                            effectFactory = overlayEffectFactory,
                        ) {
                            FirstCompositionDelay(configuration)
                            NotificationShade(
                                clock =
                                    if (shouldUseSplitScenes) {
                                        null
                                    } else {
                                        { Clock(MaterialTheme.colorScheme.onSurfaceVariant) }
                                    },
                                mediaPlayer = defaultMediaPlayer,
                                notificationList = {
                                    NotificationList(
                                        maxNotificationCount = configuration.notificationsInShade,
                                        isScrollable = false,
                                    )
                                },
                            )
                        }
                    }

                    // Add 2 empty boxes for each half of the STL. This is used by overlay benchmark
                    // tests to swipe on the start or end half of the STL.
                    Row {
                        Box(Modifier.testTag("StlStartHalf").fillMaxHeight().weight(1f))
                        Box(Modifier.testTag("StlEndHalf").fillMaxHeight().weight(1f))
                    }
                }

                val debuggerState = LocalMotionValueDebugController.current
                if (debuggerState != null) {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier =
                                Modifier.padding(bottom = 32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                                    )
                                    .align(Alignment.BottomStart),
                        ) {
                            debuggerState.observed.forEach {
                                key(it) {
                                    var rowExpanded by remember { mutableStateOf(false) }

                                    val height by
                                        animateDpAsState(if (rowExpanded) 96.dp else 24.dp)

                                    DebugMotionValueVisualization(
                                        it,
                                        0f..2000f,
                                        modifier =
                                            Modifier.height { height.toPx().toInt() }
                                                .fillMaxWidth()
                                                .clickable { rowExpanded = !rowExpanded },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstCompositionDelay(configuration: DemoConfiguration) {
    val delay = configuration.firstCompositionDelay
    if (delay > 0L) {
        remember<Any> { Thread.sleep(delay) }
    }
}

// Adapted from [androidx.compose.material3.windowsizeclass.calculateWindowSizeClass].
@Composable
internal fun calculateWindowSizeClass(): WindowSizeClass {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current

    return calculateWindowSizeClass(LocalContext.current, LocalDensity.current)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun calculateWindowSizeClass(context: Context, density: Density): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
    return WindowSizeClass.calculateFromSize(size)
}

fun shouldUseSplitScenes(windowSizeClass: WindowSizeClass): Boolean {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact,
        WindowWidthSizeClass.Medium -> false
        WindowWidthSizeClass.Expanded -> true
        else -> error("Unknown size class: ${windowSizeClass.widthSizeClass}")
    }
}

private fun quickSettingsTiles(textMeasurer: TextMeasurer): List<QuickSettingsTileViewModel> {
    return listOf(
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.NetworkWifi,
            title = "Internet",
            description = "Google Guest",
            isActive = true,
            showChevron = true,
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            isActive = true,
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.DoNotDisturb,
            title = "Do Not Disturb",
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.FlashlightOff,
            title = "Flashlight",
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.AirplanemodeInactive,
            title = "Airplane mode",
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.Home,
            title = "Home",
            description = "1600 Amphitheatre Pkwy",
            isActive = true,
            showChevron = true,
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.CreditCard,
            title = "GPay",
            description = "•••• 0061",
            isActive = true,
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.ScreenRotation,
            title = "Auto-rotate",
            isActive = true,
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.Bedtime,
            title = "Night Light",
            description = "On",
            inactiveDescription = "Off",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.Videocam,
            title = "Screen record",
            description = "Start",
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.NearbyOff,
            title = "Nearby Share",
            showChevron = true,
        ),
        quickSettingsTile(
            textMeasurer = textMeasurer,
            icon = Icons.Default.ChargingStation,
            title = "Battery Share",
            description = "On",
            inactiveDescription = "Off",
        ),
    )
}

private fun quickSettingsTile(
    textMeasurer: TextMeasurer,
    icon: ImageVector,
    title: String,
    key: ElementKey = ElementKey("Tile:$title", identity = QuickSettingsTileIdentity()),
    isActive: Boolean = false,
    description: String? = null,
    inactiveDescription: String? = description,
    showChevron: Boolean = false,
): QuickSettingsTileViewModel {
    val activeDescription = description

    var isActive by mutableStateOf(isActive)
    var description by mutableStateOf(if (isActive) activeDescription else inactiveDescription)

    return object : QuickSettingsTileViewModel {
        override val key: ElementKey = key
        override val isActive: Boolean
            get() = isActive

        override val icon: ImageVector = icon
        override val title: String = title
        override val description: String?
            get() = description

        override val showChevron: Boolean = showChevron
        override val onClick: () -> Unit = {
            isActive = !isActive
            description =
                if (isActive) {
                    activeDescription
                } else {
                    inactiveDescription
                }
        }

        override val textMeasurer: TextMeasurer = textMeasurer
    }
}
