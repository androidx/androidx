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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.animateRemoteDpAsState
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end integration tests for Remote Compose animation APIs using the embedded player
 * ([RcPlayer]).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerAnimationTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private fun loadDocument(bytes: ByteArray): CoreDocument =
        CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }

    @Test
    fun animateRemoteFloatAsState_linearlyAnimatesDimensionsAcrossFrames() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animatedWidth =
                                animateRemoteFloatAsState(
                                    targetValue = 120f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 1000,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 20f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "box".rs }
                                        .width(animatedWidth)
                                        .height(50.rdp)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            val boxNode = rule.onNodeWithContentDescription("box")
            fun width(): Float =
                boxNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }

            // Initial frame (t=0ms): width is at initial value 20dp
            rule.mainClock.advanceTimeBy(0)
            val w0 = width()
            assertThat(w0).isWithin(2f).of(20f)

            // At t=250ms (25%): width is ~45dp
            rule.mainClock.advanceTimeBy(250)
            val w250 = width()
            assertThat(w250).isWithin(5f).of(45f)
            assertThat(w250).isGreaterThan(w0)

            // At t=500ms (50%): width is ~70dp
            rule.mainClock.advanceTimeBy(250)
            val w500 = width()
            assertThat(w500).isWithin(5f).of(70f)
            assertThat(w500).isGreaterThan(w250)

            // At t=750ms (75%): width is ~95dp
            rule.mainClock.advanceTimeBy(250)
            val w750 = width()
            assertThat(w750).isWithin(5f).of(95f)
            assertThat(w750).isGreaterThan(w500)

            // At t=1000ms (100%): width reaches target 120dp
            rule.mainClock.advanceTimeBy(250)
            val w1000 = width()
            assertThat(w1000).isWithin(2f).of(120f)
        }
    }

    @Test
    fun animateRemoteDpAsState_animatesWidthAndHeight() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animatedSize =
                                animateRemoteDpAsState(
                                    targetValue = 100.rdp,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 600,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 40f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "dpBox".rs }
                                        .size(animatedSize)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            val boxNode = rule.onNodeWithContentDescription("dpBox")
            fun bounds() = boxNode.getUnclippedBoundsInRoot()

            // At t=0ms: size is 40dp
            rule.mainClock.advanceTimeBy(0)
            val b0 = bounds()
            assertThat(b0.right.value - b0.left.value).isWithin(2f).of(40f)
            assertThat(b0.bottom.value - b0.top.value).isWithin(2f).of(40f)

            // At t=300ms (halfway): size is ~70dp
            rule.mainClock.advanceTimeBy(300)
            val b300 = bounds()
            assertThat(b300.right.value - b300.left.value).isWithin(5f).of(70f)
            assertThat(b300.bottom.value - b300.top.value).isWithin(5f).of(70f)

            // At t=600ms: size reaches target 100dp
            rule.mainClock.advanceTimeBy(300)
            val b600 = bounds()
            assertThat(b600.right.value - b600.left.value).isWithin(2f).of(100f)
            assertThat(b600.bottom.value - b600.top.value).isWithin(2f).of(100f)
        }
    }

    @Test
    fun animateRemoteFloatAsState_reactiveTargetUpdateViaClick() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val targetWidth = rememberMutableRemoteFloat(50f)
                            val animatedWidth =
                                animateRemoteFloatAsState(
                                    targetValue = targetWidth,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 500,
                                            easing = RemoteEasing.Linear,
                                        ),
                                )
                            RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color.Blue.rc)
                                            .clickable(action = valueChange(targetWidth, 150f.rf))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "reactiveBox".rs
                                            }
                                            .width(animatedWidth)
                                            .height(40.rdp)
                                            .background(Color.Green.rc)
                                )
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            val targetNode = rule.onNodeWithContentDescription("reactiveBox")
            fun width(): Float =
                targetNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }

            // Initially settles at 50dp
            rule.mainClock.advanceTimeBy(100)
            assertThat(width()).isWithin(2f).of(50f)

            // Click button to change target to 150dp
            rule.onNode(hasClickAction()).performClick()
            rule.mainClock.advanceTimeByFrame()

            // At t=250ms into transition: width is ~100dp
            rule.mainClock.advanceTimeBy(250)
            val midWidth = width()
            assertThat(midWidth).isWithin(10f).of(100f)

            // Allow animation to complete (remaining 250ms + settle frame)
            rule.mainClock.advanceTimeBy(300)
            val finalWidth = width()
            assertThat(finalWidth).isWithin(2f).of(150f)
        }
    }

    @Test
    fun animateRemoteFloatAsState_easingCurvesDifferProgression() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val linear =
                                animateRemoteFloatAsState(
                                    targetValue = 100f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 1000,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 0f,
                                )
                            val accelerate =
                                animateRemoteFloatAsState(
                                    targetValue = 100f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 1000,
                                            easing = RemoteEasing.Accelerate,
                                        ),
                                    initialValue = 0f,
                                )
                            val decelerate =
                                animateRemoteFloatAsState(
                                    targetValue = 100f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 1000,
                                            easing = RemoteEasing.Decelerate,
                                        ),
                                    initialValue = 0f,
                                )
                            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "linear".rs
                                            }
                                            .width(linear)
                                            .height(20.rdp)
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "accelerate".rs
                                            }
                                            .width(accelerate)
                                            .height(20.rdp)
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "decelerate".rs
                                            }
                                            .width(decelerate)
                                            .height(20.rdp)
                                )
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }

            fun widthOf(tag: String): Float =
                rule.onNodeWithContentDescription(tag).getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            // At t=250ms (early):
            // Accelerate starts slower than Linear; Decelerate starts faster than Linear
            rule.mainClock.advanceTimeBy(250)
            val wLin250 = widthOf("linear")
            val wAcc250 = widthOf("accelerate")
            val wDec250 = widthOf("decelerate")

            assertThat(wAcc250).isLessThan(wLin250)
            assertThat(wLin250).isLessThan(wDec250)

            // At t=1000ms: all reach target 100dp
            rule.mainClock.advanceTimeBy(750)
            assertThat(widthOf("linear")).isWithin(2f).of(100f)
            assertThat(widthOf("accelerate")).isWithin(2f).of(100f)
            assertThat(widthOf("decelerate")).isWithin(2f).of(100f)
        }
    }

    @Test
    fun multipleSimultaneousAnimations_animateIndependently() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animatedWidth =
                                animateRemoteFloatAsState(
                                    targetValue = 100f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 400,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 0f,
                                )
                            val animatedHeight =
                                animateRemoteFloatAsState(
                                    targetValue = 200f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 800,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 0f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "multiBox".rs }
                                        .width(animatedWidth)
                                        .height(animatedHeight)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }

            val boxNode = rule.onNodeWithContentDescription("multiBox")
            fun bounds() = boxNode.getUnclippedBoundsInRoot()

            // At t=400ms (+ extra frame to settle): width has completed (100dp), height is halfway
            // (~100dp)
            rule.mainClock.advanceTimeBy(450)
            val b400 = bounds()
            assertThat(b400.right.value - b400.left.value).isWithin(2f).of(100f)
            assertThat(b400.bottom.value - b400.top.value).isWithin(15f).of(100f)

            // At t=800ms (+ extra frame to settle): width remains 100dp, height reaches 200dp
            rule.mainClock.advanceTimeBy(450)
            val b800 = bounds()
            assertThat(b800.right.value - b800.left.value).isWithin(2f).of(100f)
            assertThat(b800.bottom.value - b800.top.value).isWithin(2f).of(200f)
        }
    }

    @Test
    fun animateRemoteFloatAsState_cubicCustomEasing_animatesToTarget() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animatedWidth =
                                animateRemoteFloatAsState(
                                    targetValue = 100f.rf,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 500,
                                            easing = RemoteEasing.Cubic(0.25f, 0.1f, 0.25f, 1.0f),
                                        ),
                                    initialValue = 0f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "cubicBox".rs }
                                        .width(animatedWidth)
                                        .height(50.rdp)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            val boxNode = rule.onNodeWithContentDescription("cubicBox")
            fun width(): Float =
                boxNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }

            rule.mainClock.advanceTimeBy(0)
            assertThat(width()).isWithin(2f).of(0f)

            rule.mainClock.advanceTimeBy(600)
            assertThat(width()).isWithin(2f).of(100f)
        }
    }

    @Test
    fun animateRemoteDpAsState_reachesIdleWithoutTimeout() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animatedSize =
                                animateRemoteDpAsState(
                                    targetValue = 80.rdp,
                                    animationSpec = remoteTween(durationMillis = 200),
                                    initialValue = 20f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "idleBox".rs }
                                        .size(animatedSize)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
            }

            rule.waitForIdle()
            val boxNode = rule.onNodeWithContentDescription("idleBox")
            val bounds = boxNode.getUnclippedBoundsInRoot()
            assertThat(bounds.right.value - bounds.left.value).isWithin(2f).of(80f)
            assertThat(bounds.bottom.value - bounds.top.value).isWithin(2f).of(80f)
        }
    }

    @Test
    fun stateLayout_sharedElementTransition_interpolatesSizeAcrossStates() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val checked = rememberMutableRemoteBoolean(false)
                            RemoteColumn {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "toggleButton".rs
                                            }
                                            .size(20.rdp)
                                            .clickable(action = valueChange(checked, !checked))
                                )
                                RemoteStateLayout(currentState = checked) { state ->
                                    if (!state) {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.semantics {
                                                        contentDescription =
                                                            "sharedSizeBox_start".rs
                                                    }
                                                    .animationSpec(
                                                        animationId = 10,
                                                        motionDuration = 1000f,
                                                        motionEasingType =
                                                            GeneralEasing.CUBIC_LINEAR,
                                                        visibilityDuration = 1000f,
                                                        visibilityEasingType =
                                                            GeneralEasing.CUBIC_LINEAR,
                                                        enterAnimation =
                                                            AnimationSpec.ANIMATION.FADE_IN,
                                                        exitAnimation =
                                                            AnimationSpec.ANIMATION.FADE_OUT,
                                                    )
                                                    .size(40.rdp)
                                                    .background(Color.Red.rc)
                                        )
                                    } else {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.semantics {
                                                        contentDescription = "sharedSizeBox_end".rs
                                                    }
                                                    .animationSpec(
                                                        animationId = 10,
                                                        motionDuration = 1000f,
                                                        motionEasingType =
                                                            GeneralEasing.CUBIC_LINEAR,
                                                        visibilityDuration = 1000f,
                                                        visibilityEasingType =
                                                            GeneralEasing.CUBIC_LINEAR,
                                                        enterAnimation =
                                                            AnimationSpec.ANIMATION.FADE_IN,
                                                        exitAnimation =
                                                            AnimationSpec.ANIMATION.FADE_OUT,
                                                    )
                                                    .size(100.rdp)
                                                    .background(Color.Blue.rc)
                                        )
                                    }
                                }
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            fun startWidth(): Float =
                rule
                    .onNodeWithContentDescription("sharedSizeBox_start")
                    .getUnclippedBoundsInRoot()
                    .let { it.right.value - it.left.value }

            fun endWidth(): Float =
                rule
                    .onNodeWithContentDescription("sharedSizeBox_end")
                    .getUnclippedBoundsInRoot()
                    .let { it.right.value - it.left.value }

            rule.mainClock.advanceTimeBy(0)
            assertThat(startWidth()).isWithin(2f).of(40f)

            rule.onNodeWithContentDescription("toggleButton").performClick()
            rule.mainClock.advanceTimeByFrame()

            rule.mainClock.advanceTimeBy(484)
            assertThat(endWidth()).isWithin(5f).of(70f)

            rule.mainClock.advanceTimeBy(500)
            assertThat(endWidth()).isWithin(2f).of(100f)
        }
    }

    @Test
    fun stateLayout_sharedElementTransition_interpolatesPositionAcrossStates() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val stateIndex = rememberMutableRemoteInt(0)
                            RemoteColumn {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "nextStateButton".rs
                                            }
                                            .size(20.rdp)
                                            .clickable(
                                                action = valueChange(stateIndex, RemoteInt(1))
                                            )
                                )
                                RemoteStateLayout(currentState = stateIndex, 0, 1) { state ->
                                    if (state == 0) {
                                        RemoteRow {
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.width(100.rdp).height(40.rdp)
                                            )
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription =
                                                                "movingBox_start".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 20,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(40.rdp)
                                                        .background(Color.Green.rc)
                                            )
                                        }
                                    } else {
                                        RemoteRow {
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription = "movingBox_end".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 20,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(40.rdp)
                                                        .background(Color.Yellow.rc)
                                            )
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.width(100.rdp).height(40.rdp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }

            fun startLeft(): Float =
                rule
                    .onNodeWithContentDescription("movingBox_start")
                    .getUnclippedBoundsInRoot()
                    .left
                    .value

            fun endLeft(): Float =
                rule
                    .onNodeWithContentDescription("movingBox_end")
                    .getUnclippedBoundsInRoot()
                    .left
                    .value

            rule.mainClock.advanceTimeBy(0)
            assertThat(startLeft()).isWithin(2f).of(100f)

            rule.onNodeWithContentDescription("nextStateButton").performClick()
            rule.mainClock.advanceTimeByFrame()

            rule.mainClock.advanceTimeBy(484)
            assertThat(endLeft()).isWithin(5f).of(50f)

            rule.mainClock.advanceTimeBy(500)
            assertThat(endLeft()).isWithin(2f).of(0f)
        }
    }

    @Test
    fun stateLayout_multipleSharedElements_transitionConcurrently() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val state = rememberMutableRemoteBoolean(false)
                            RemoteColumn {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "toggleBtn".rs
                                            }
                                            .size(20.rdp)
                                            .clickable(action = valueChange(state, !state))
                                )
                                RemoteStateLayout(currentState = state) { isState1 ->
                                    if (!isState1) {
                                        RemoteRow {
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription = "elementA_start".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 100,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(40.rdp)
                                            )
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription = "elementB_start".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 200,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(80.rdp)
                                            )
                                        }
                                    } else {
                                        RemoteRow {
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription = "elementA_end".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 100,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(80.rdp)
                                            )
                                            RemoteBox(
                                                modifier =
                                                    RemoteModifier.semantics {
                                                            contentDescription = "elementB_end".rs
                                                        }
                                                        .animationSpec(
                                                            animationId = 200,
                                                            motionDuration = 1000f,
                                                            motionEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            visibilityDuration = 1000f,
                                                            visibilityEasingType =
                                                                GeneralEasing.CUBIC_LINEAR,
                                                            enterAnimation =
                                                                AnimationSpec.ANIMATION.FADE_IN,
                                                            exitAnimation =
                                                                AnimationSpec.ANIMATION.FADE_OUT,
                                                        )
                                                        .size(40.rdp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }

            fun aWidth(): Float =
                rule.onNodeWithContentDescription("elementA_end").getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            fun bWidth(): Float =
                rule.onNodeWithContentDescription("elementB_end").getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            rule.onNodeWithContentDescription("toggleBtn").performClick()
            rule.mainClock.advanceTimeByFrame()

            // At t=0ms: elementA starts at 40, elementB starts at 80
            assertThat(aWidth()).isWithin(2f).of(40f)
            assertThat(bWidth()).isWithin(2f).of(80f)

            // At t=500ms: elementA is 60, elementB is 60
            rule.mainClock.advanceTimeBy(484)
            assertThat(aWidth()).isWithin(5f).of(60f)
            assertThat(bWidth()).isWithin(5f).of(60f)

            // At t=1000ms: elementA reaches 80, elementB reaches 40
            rule.mainClock.advanceTimeBy(500)
            assertThat(aWidth()).isWithin(2f).of(80f)
            assertThat(bWidth()).isWithin(2f).of(40f)
        }
    }

    @Test
    fun stateLayout_instantTransition_whenDurationIsZero() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val state = rememberMutableRemoteBoolean(false)
                            RemoteColumn {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "snapToggle".rs
                                            }
                                            .size(20.rdp)
                                            .clickable(action = valueChange(state, !state))
                                )
                                RemoteStateLayout(currentState = state) { isState1 ->
                                    if (!isState1) {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.semantics {
                                                        contentDescription = "snapBox_start".rs
                                                    }
                                                    .animationSpec(
                                                        animationId = 300,
                                                        motionDuration = 0f,
                                                        motionEasingType = 0,
                                                        visibilityDuration = 0f,
                                                        visibilityEasingType = 0,
                                                        enterAnimation =
                                                            AnimationSpec.ANIMATION.FADE_IN,
                                                        exitAnimation =
                                                            AnimationSpec.ANIMATION.FADE_OUT,
                                                    )
                                                    .size(30.rdp)
                                        )
                                    } else {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.semantics {
                                                        contentDescription = "snapBox_end".rs
                                                    }
                                                    .animationSpec(
                                                        animationId = 300,
                                                        motionDuration = 0f,
                                                        motionEasingType = 0,
                                                        visibilityDuration = 0f,
                                                        visibilityEasingType = 0,
                                                        enterAnimation =
                                                            AnimationSpec.ANIMATION.FADE_IN,
                                                        exitAnimation =
                                                            AnimationSpec.ANIMATION.FADE_OUT,
                                                    )
                                                    .size(90.rdp)
                                        )
                                    }
                                }
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            rule.onNodeWithContentDescription("snapToggle").performClick()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            val endNode = rule.onNodeWithContentDescription("snapBox_end")
            val width = endNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }
            assertThat(width).isWithin(2f).of(90f)
        }
    }
}
