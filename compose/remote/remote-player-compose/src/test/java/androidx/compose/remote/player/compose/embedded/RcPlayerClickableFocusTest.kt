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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.clickable as composeClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll as composeVerticalScroll
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription as composeContentDescription
import androidx.compose.ui.semantics.semantics as composeSemantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests covering the embedded player's focus behavior that works natively on
 * `androidx-main` via Compose `combinedClickable`, `verticalScroll`, visibility, and
 * `FitBoxSlot.Probe` focus suppression.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerClickableFocusTest {

    @get:Rule val rule = RcPlayerTestRule()

    private lateinit var inputModeManager: InputModeManager
    private lateinit var focusManager: FocusManager
    private var playerHasFocus = false

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    private fun setContent(
        widthDp: Int = 200,
        heightDp: Int = 200,
        customPlugins: CustomPluginRegistry? = null,
        onNamedAction: (String, Any?) -> Unit = { _, _ -> },
        content: @Composable () -> Unit,
    ) {
        playerHasFocus = false
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            customPlugins = customPlugins,
            profile = experimentalProfile,
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(
                    context = context,
                    size = Size(widthDp * density, heightDp * density),
                ),
            onNamedAction = { name, value, _ -> onNamedAction(name, value) },
            playComposableWrapper = { playerContent ->
                inputModeManager = LocalInputModeManager.current
                focusManager = LocalFocusManager.current
                Box(
                    modifier =
                        Modifier.size(widthDp.dp, heightDp.dp)
                            .onFocusChanged { playerHasFocus = it.hasFocus }
                            .testTag("player")
                ) {
                    playerContent()
                }
            },
            content = content,
        )
        setTouchMode(true)
    }

    private fun setTouchMode(inTouchMode: Boolean) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(inTouchMode)
        rule.runOnIdle {
            inputModeManager.requestInputMode(
                if (inTouchMode) InputMode.Touch else InputMode.Keyboard
            )
        }
        rule.waitForIdle()
    }

    @Test
    fun hostCompose_tabAndShiftTab_traversesPlayerClickablesAndBackToHost() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            profile = experimentalProfile,
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(
                    context = context,
                    size = Size(200f * density, 120f * density),
                ),
            playComposableWrapper = { playerContent ->
                inputModeManager = LocalInputModeManager.current
                Column {
                    Box(
                        modifier =
                            Modifier.size(200.dp, 40.dp).focusable().composeSemantics {
                                composeContentDescription = "HostBefore"
                            }
                    )
                    Box(modifier = Modifier.size(200.dp, 120.dp)) { playerContent() }
                    Box(
                        modifier =
                            Modifier.size(200.dp, 40.dp).focusable().composeSemantics {
                                composeContentDescription = "HostAfter"
                            }
                    )
                }
            },
        ) {
            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 120.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(200.rdp, 60.rdp)
                            .clickable(hostAction("rc1".rs))
                            .semantics { contentDescription = "Rc1".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(200.rdp, 60.rdp)
                            .clickable(hostAction("rc2".rs))
                            .semantics { contentDescription = "Rc2".rs }
                )
            }
        }
        setTouchMode(false)

        val hostBefore = rule.onNodeWithContentDescription("HostBefore")
        val rc1 = rule.onNodeWithContentDescription("Rc1")
        val rc2 = rule.onNodeWithContentDescription("Rc2")
        val hostAfter = rule.onNodeWithContentDescription("HostAfter")

        hostBefore.requestFocus()
        rule.waitForIdle()
        hostBefore.assertIsFocused()

        hostBefore.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        rc1.assertIsFocused()

        rc1.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        rc2.assertIsFocused()

        rc2.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        hostAfter.assertIsFocused()

        hostAfter.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        rule.waitForIdle()
        rc2.assertIsFocused()
    }

    @Test
    fun clickable_touchModeDoesNotStealFocus_whileKeyboardTabFocusesAndEnterActivates() {
        val clicks = mutableListOf<String>()
        setContent(
            widthDp = 100,
            heightDp = 100,
            onNamedAction = { name, _ -> clicks.add(name) },
        ) {
            val clicked2 = remember { MutableRemoteFloat(0f) }
            val box2Color = clicked2.isEqualTo(1.rf).select(Color.Green.rc, Color.Red.rc)
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .background(Color.Blue.rc)
                            .clickable(hostAction("btn1".rs))
                            .semantics { contentDescription = "Btn1".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .background(box2Color)
                            .clickable(
                                combinedAction(
                                    valueChangeAction(clicked2, 1.rf),
                                    hostAction("btn2".rs),
                                )
                            )
                            .semantics { contentDescription = "Btn2".rs }
                )
            }
        }

        val btn1 = rule.onNodeWithContentDescription("Btn1")
        val btn2 = rule.onNodeWithContentDescription("Btn2")

        // Compose's native clickable does not take focus on touch.
        setTouchMode(true)
        btn1.performClick()
        rule.waitForIdle()
        assertThat(clicks).containsExactly("btn1")
        btn1.assertIsNotFocused()
        val beforeEnterBmp = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(beforeEnterBmp.getPixel(50, 75)).isEqualTo(AndroidColor.RED)

        // Keyboard navigation focuses clickables and Enter dispatches actions + updates pixels.
        setTouchMode(false)
        btn1.requestFocus()
        rule.waitForIdle()
        btn1.assertIsFocused()

        btn1.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        btn2.assertIsFocused()

        btn2.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertThat(clicks).containsExactly("btn1", "btn2").inOrder()
        val focusedGreenPixel =
            rule.onNodeWithTag("player").captureToImage().asAndroidBitmap().getPixel(50, 75)
        assertThat(AndroidColor.red(focusedGreenPixel)).isEqualTo(0)
        assertThat(AndroidColor.green(focusedGreenPixel)).isGreaterThan(200)
        assertThat(AndroidColor.blue(focusedGreenPixel)).isEqualTo(0)

        // Leaving focus removes Compose's focus indication scrim, revealing pure Color.Green.
        btn1.requestFocus()
        rule.waitForIdle()
        val unfocusedGreenBmp = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(unfocusedGreenBmp.getPixel(50, 75)).isEqualTo(AndroidColor.GREEN)
    }

    @Test
    fun combinedClickable_touchModeDoesNotStealFocus_whileKeyboardFocusesAndEnterActivates() {
        val clicks = mutableListOf<String>()
        setContent(
            widthDp = 100,
            heightDp = 100,
            onNamedAction = { name, _ -> clicks.add(name) },
        ) {
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .clickable(hostAction("first".rs))
                            .semantics { contentDescription = "First".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .combinedClickable(
                                onClick = hostAction("single".rs),
                                onLongClick = hostAction("long".rs),
                                onDoubleClick = hostAction("double".rs),
                            )
                            .semantics { contentDescription = "MultiClick".rs }
                )
            }
        }

        val first = rule.onNodeWithContentDescription("First")
        val multiClick = rule.onNodeWithContentDescription("MultiClick")

        setTouchMode(true)
        multiClick.performClick()
        // Wait past Compose's double-tap timeout so combinedClickable dispatches single click.
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        assertThat(clicks).containsExactly("single")
        multiClick.assertIsNotFocused()

        setTouchMode(false)
        first.requestFocus()
        rule.waitForIdle()
        first.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        multiClick.assertIsFocused()

        multiClick.performKeyInput { pressKey(Key.Enter) }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        assertThat(clicks).containsExactly("single", "single").inOrder()
    }

    @Test
    fun verticalScroll_tabbingToOffscreenClickable_bringsIntoView() {
        setContent(widthDp = 120, heightDp = 100) {
            val scrollState = remember { RemoteScrollState() }
            RemoteColumn(
                modifier = RemoteModifier.size(120.rdp, 100.rdp).verticalScroll(scrollState)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(120.rdp, 80.rdp)
                            .clickable(hostAction("i0".rs))
                            .semantics { contentDescription = "item0".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(120.rdp, 80.rdp)
                            .clickable(hostAction("i1".rs))
                            .semantics { contentDescription = "item1".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(120.rdp, 80.rdp)
                            .clickable(hostAction("i2".rs))
                            .semantics { contentDescription = "item2".rs }
                )
            }
        }

        val item0 = rule.onNodeWithContentDescription("item0")
        val item2 = rule.onNodeWithContentDescription("item2")

        assertThat(item2.getUnclippedBoundsInRoot().top.value).isAtLeast(159f)

        setTouchMode(false)
        item0.requestFocus()
        rule.waitForIdle()
        item0.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("item1").performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()

        item2.assertIsFocused()
        val boundsAfterTab = item2.getBoundsInRoot()
        assertThat(boundsAfterTab.top.value).isAtLeast(-0.5f)
        assertThat(boundsAfterTab.bottom.value).isAtMost(100.5f)
    }

    @Test
    fun fitBox_unselectedClickableProbe_cannotCaptureInitial2DFocus() {
        val clicks = mutableListOf<String>()
        setContent(
            widthDp = 100,
            heightDp = 100,
            onNamedAction = { name, _ -> clicks.add(name) },
        ) {
            RemoteFitBox(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                // Selected candidate fits (80x80) and is non-clickable.
                RemoteBox(modifier = RemoteModifier.size(80.rdp, 80.rdp))
                // Unselected candidate stays in FitBoxSlot.Probe (200x200) with click + touch ops.
                RemoteBox(
                    modifier =
                        RemoteModifier.size(200.rdp, 200.rdp)
                            .onTouchDown(hostAction("probeTouchDown".rs))
                            .clickable(hostAction("probeClicked".rs))
                            .semantics { contentDescription = "UnselectedProbe".rs }
                )
            }
        }

        setTouchMode(false)
        // TwoDimensionalFocusSearch inspects attached unplaced FocusTargetNodes;
        // LocalRcIsMeasureProbe ensures the unselected probe leaves no focusable or clickable node.
        rule.onNodeWithContentDescription("UnselectedProbe").assertDoesNotExist()
        rule.runOnIdle {
            assertThat(focusManager.moveFocus(FocusDirection.Enter)).isFalse()
            assertThat(focusManager.moveFocus(FocusDirection.Down)).isFalse()
            assertThat(playerHasFocus).isFalse()
        }
        rule.onNodeWithTag("player").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.waitForIdle()
        assertThat(playerHasFocus).isFalse()
        assertThat(clicks).isEmpty()
    }

    @Test
    fun fitBox_unselectedCustomPluginProbe_cannotCaptureInitial2DFocus() {
        val clicks = mutableListOf<String>()
        var observedNonNullOnClickInProbe = false
        val customPlugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:probe-custom"
                override val handlesClick: Boolean = true

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(
                    data: Unit,
                    component: RcCustomComponent,
                    modifier: Modifier,
                ) {
                    if (component.onClick != null) {
                        observedNonNullOnClickInProbe = true
                    }
                    // Include both a ScrollModifier and an inherently focusable Compose node.
                    Box(
                        modifier =
                            modifier
                                .size(200.dp)
                                .composeVerticalScroll(rememberScrollState())
                                .composeClickable {
                                    clicks.add("composeClick")
                                    component.onClick?.invoke()
                                }
                                .focusable()
                    )
                }
            }

        setContent(
            widthDp = 100,
            heightDp = 100,
            customPlugins = CustomPluginRegistry(customPlugin),
            onNamedAction = { name, _ -> clicks.add(name) },
        ) {
            RemoteFitBox(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                RemoteBox(modifier = RemoteModifier.size(80.rdp, 80.rdp))
                RemoteCustomComponent(
                    name = "test:probe-custom",
                    modifier =
                        RemoteModifier.size(200.rdp, 200.rdp)
                            .verticalScroll(remember { RemoteScrollState() })
                            .clickable(hostAction("customClicked".rs)),
                ) {}
            }
        }

        setTouchMode(false)
        assertThat(observedNonNullOnClickInProbe).isFalse()
        rule.runOnIdle {
            assertThat(focusManager.moveFocus(FocusDirection.Enter)).isFalse()
            assertThat(focusManager.moveFocus(FocusDirection.Down)).isFalse()
            assertThat(playerHasFocus).isFalse()
        }
        rule.onNodeWithTag("player").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.waitForIdle()
        assertThat(playerHasFocus).isFalse()
        assertThat(clicks).isEmpty()
    }

    @Test
    fun fitBox_probeTransitioningToSelected_becomesFocusableAndClickable() {
        val clicks = mutableListOf<String>()
        val containerSize = mutableStateOf(DpSize(80.dp, 80.dp))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            profile = experimentalProfile,
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(
                    context = context,
                    size = Size(80f * density, 80f * density),
                ),
            onNamedAction = { name, _, _ -> clicks.add(name) },
            playComposableWrapper = { playerContent ->
                inputModeManager = LocalInputModeManager.current
                focusManager = LocalFocusManager.current
                Box(
                    modifier =
                        Modifier.size(containerSize.value.width, containerSize.value.height)
                            .onFocusChanged { playerHasFocus = it.hasFocus }
                            .testTag("player")
                ) {
                    playerContent()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(200.rdp, 200.rdp)
                            .clickable(hostAction("large".rs))
                            .semantics { contentDescription = "LargeCandidate".rs }
                )
                RemoteBox(modifier = RemoteModifier.size(80.rdp, 80.rdp))
            }
        }

        // At 80x80 the 200x200 candidate sits in FitBoxSlot.Probe and cannot be focused or clicked.
        setTouchMode(false)
        rule.onNodeWithContentDescription("LargeCandidate").assertDoesNotExist()
        rule.runOnIdle {
            assertThat(focusManager.moveFocus(FocusDirection.Enter)).isFalse()
            assertThat(focusManager.moveFocus(FocusDirection.Down)).isFalse()
            assertThat(playerHasFocus).isFalse()
        }

        // Growing to 220x220 promotes the 200x200 candidate to FitBoxSlot.Content.
        containerSize.value = DpSize(220.dp, 220.dp)
        rule.waitForIdle()
        val large = rule.onNodeWithContentDescription("LargeCandidate")
        large.assertExists()
        rule.runOnIdle { assertThat(focusManager.moveFocus(FocusDirection.Down)).isTrue() }
        rule.waitForIdle()
        large.assertIsFocused()

        large.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertThat(clicks).containsExactly("large")

        // Shrinking back to 80x80 returns the candidate to FitBoxSlot.Probe and drops focusability.
        containerSize.value = DpSize(80.dp, 80.dp)
        rule.waitForIdle()
        large.assertDoesNotExist()
        assertThat(playerHasFocus).isFalse()
        rule.runOnIdle { assertThat(focusManager.moveFocus(FocusDirection.Down)).isFalse() }
    }

    @Test
    fun invisibleClickableRemainsFocusableAndClickable_whileGoneIsRemovedFromLayoutAndFocus() {
        val clicks = mutableListOf<String>()
        setContent(onNamedAction = { name, _ -> clicks.add(name) }) {
            RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .clickable(hostAction("first".rs))
                            .semantics { contentDescription = "First".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .visibility(Component.Visibility.INVISIBLE.ri)
                            .clickable(hostAction("invisible".rs))
                            .semantics { contentDescription = "InvisibleTarget".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .visibility(Component.Visibility.GONE.ri)
                            .clickable(hostAction("gone".rs))
                            .semantics { contentDescription = "GoneTarget".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .clickable(hostAction("last".rs))
                            .semantics { contentDescription = "Last".rs }
                )
            }
        }

        val first = rule.onNodeWithContentDescription("First")
        val invisible = rule.onNodeWithContentDescription("InvisibleTarget")
        val gone = rule.onNodeWithContentDescription("GoneTarget")
        val last = rule.onNodeWithContentDescription("Last")

        // GONE omits the subtree from layout, composition, semantics, and focus;
        // INVISIBLE still reserves its 50.dp height below First (50.dp).
        gone.assertDoesNotExist()
        assertThat(last.getUnclippedBoundsInRoot().top.value).isWithin(0.5f).of(100f)

        // Deliberate divergence from the View player (Component.isVisible()): RcPlayer renders
        // INVISIBLE via graphicsLayer { alpha = 0f }, so the node remains composed, focusable,
        // and clickable in both keyboard and touch modes.
        setTouchMode(false)
        first.requestFocus()
        rule.waitForIdle()
        first.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        invisible.assertIsFocused()

        invisible.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertThat(clicks).containsExactly("invisible")

        invisible.performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()
        last.assertIsFocused()

        setTouchMode(true)
        invisible.performClick()
        rule.waitForIdle()
        assertThat(clicks).containsExactly("invisible", "invisible").inOrder()
        invisible.assertIsNotFocused()
    }
}
