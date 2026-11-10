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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.CaptureUpdateThrottle
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.testing.RecordedDocument
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests [RemoteButtonGroup] on the embedded player, driving its frames with the test clock and
 * pressing its buttons with touch input.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
// Wide enough for the widest group, so the player doesn't clip it.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h480dp-mdpi")
class RemoteButtonGroupAnimationTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zeroPadding = RemotePaddingValues(0.rdp)
    private val namedActions = mutableListOf<String>()

    private companion object {
        /** Tolerance of the widths, in dp, since the player lays the buttons out in pixels. */
        const val WidthTolerance = 1.0

        /** Time between two frames, in milliseconds. */
        const val FrameMs = 16L
    }

    @Test
    fun equalWeights_distributesWidthEqually() {
        // Row width = 304dp, 3 buttons -> 2 spacings of 4dp = 8dp -> 296dp / 3 = 98.667dp each
        setGroup(width = 304) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(304.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertWidths(296f / 3f, 296f / 3f, 296f / 3f)
    }

    @Test
    fun defaultContentPadding_subtractsFullWidthHorizontalPaddings() {
        // Row width = 330dp, default contentPadding = 11dp * 2 = 22dp, 2 spacings of 4dp = 8dp
        // -> 300dp usable width / 3 = 100dp each
        setGroup(width = 330) {
            RemoteButtonGroup(modifier = RemoteModifier.width(330.rdp)) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun unequalWeights_distributesWidthProportionally() {
        // Row width = 358dp, 3 buttons -> 2 spacings of 4dp = 8dp -> 350dp total
        // Weights 1 : 1.5 : 1 (sum = 3.5) -> 100dp, 150dp, 100dp
        setGroup(width = 358) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(358.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1.5f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertWidths(100f, 150f, 100f)
    }

    @Test
    fun minWidthDeficit_redistributesProportionallyFromDonors() {
        // Row width = 308dp, 3 buttons -> 2 spacings of 4dp = 8dp -> 300dp available
        // Weights 0.2 : 1.0 : 1.8 (sum = 3.0) -> raw weighted widths: 20dp, 100dp, 180dp
        // Button 0 has minWidth = 50dp -> deficit = 30dp.
        // Donors 1 and 2 have weights 1.0 and 1.8 (donor sum = 2.8), plenty of slack above 48dp:
        // Donor 1 gives 30 * (1.0 / 2.8) = 10.714dp -> 89.286dp
        // Donor 2 gives 30 * (1.8 / 2.8) = 19.286dp -> 160.714dp
        setGroup(width = 308) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(0.2f.rf).minWidth(50.rdp).animateWidth())
                TestButton("b1", RemoteModifier.weight(1.0f.rf).minWidth(48.rdp).animateWidth())
                TestButton("b2", RemoteModifier.weight(1.8f.rf).minWidth(48.rdp).animateWidth())
            }
        }

        assertWidths(50f, 100f - 30f * (1.0f / 2.8f), 180f - 30f * (1.8f / 2.8f))
    }

    @Test
    fun minWidthDeficit_cascadesWhenOneDonorHitsMinWidthFloor() {
        // Row width = 208dp, 3 buttons -> 2 spacings of 4dp = 8dp -> 200dp available
        // Weights 0.2 : 0.6 : 1.2 (sum = 2.0) -> raw weighted widths: 20dp, 60dp, 120dp
        // Button 0 has minWidth = 60dp -> deficit = 40dp.
        // Donors 1 and 2 have minWidth = 50dp -> slacks are 10dp and 70dp.
        // Proportional share of 40dp deficit (weights 0.6 : 1.2 = 1 : 2):
        // Donor 1 would owe 13.333dp, but only has 10dp slack -> clamped at 50dp!
        // Remaining 3.333dp cascades to Donor 2 -> Donor 2 gives 30dp total -> 90dp!
        setGroup(width = 208) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(208.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(0.2f.rf).minWidth(60.rdp).animateWidth())
                TestButton("b1", RemoteModifier.weight(0.6f.rf).minWidth(50.rdp).animateWidth())
                TestButton("b2", RemoteModifier.weight(1.2f.rf).minWidth(50.rdp).animateWidth())
            }
        }

        assertWidths(60f, 50f, 90f)
    }

    @Test
    fun pressLeftButton_expandsLeftAndShrinksMiddleNeighbor_thenRestoresOnRelease() {
        // 3 buttons of 100dp each (308dp total with 2 * 4dp spacing), expansionWidth = 24dp
        setGroup { ThreeButtonGroup() }
        assertWidths(100f, 100f, 100f)

        press("b0")
        val pressingWidths = widthsOverTime(400)

        // Settled: b0 = 124dp, b1 = 76dp, b2 = 100dp
        assertWidths(124f, 76f, 100f)
        // On the way: b0 is between 100 and 124, b1 is between 76 and 100, b2 stays 100
        assertThat(pressingWidths.any { it[0] in 102f..122f && it[1] in 78f..98f }).isTrue()
        assertThat(pressingWidths.all { it[2] == 100f }).isTrue()

        // Release -> settles back to resting 100dp each
        release("b0")
        val releasingWidths = widthsOverTime(2000)

        assertWidths(100f, 100f, 100f)
        assertThat(releasingWidths.any { it[0] in 102f..122f && it[1] in 78f..98f }).isTrue()
    }

    @Test
    fun pressMiddleButton_expandsMiddleAndShrinksBothNeighborsEqually_thenRestoresOnCancel() {
        // 3 buttons of 100dp each, expansionWidth = 24dp -> middle +24dp, left and right -12dp
        setGroup { ThreeButtonGroup() }

        press("b1")
        advance(400)
        assertWidths(88f, 124f, 88f)

        cancel("b1")
        advance(2000)
        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun pressRightButton_expandsRightAndShrinksMiddleNeighbor() {
        setGroup { ThreeButtonGroup() }

        press("b2")
        advance(400)

        assertWidths(100f, 76f, 124f)
    }

    @Test
    fun pressAndRelease_afterTransition_clickStillFires() {
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                contentPadding = zeroPadding,
            ) {
                RemoteIconButton(
                    onClick = hostAction("click".rs, 1.rf),
                    enabled = true.rb,
                    modifier =
                        RemoteModifier.weight(1f.rf).animateWidth().semantics {
                            contentDescription = "b0".rs
                        },
                ) {
                    RemoteText("b0".rs)
                }
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        press("b0")
        advance(400)
        release("b0")
        advance(100)

        assertThat(namedActions).containsExactly("click")
    }

    @Test
    fun pressExpansion_canShrinkNeighborBelowMinWidth() {
        // 2 buttons in 124dp row (1 spacing of 4dp -> 120dp available -> 60dp each).
        // minWidth = 48dp is enforced at rest, but while pressed the neighbor can shrink below
        // minWidth (60 - 24 = 36dp) while b0 expands by 24dp (to 84dp).
        setGroup(width = 124) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(124.rdp),
                expansionWidth = 24.rdp,
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).minWidth(48.rdp).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).minWidth(48.rdp).animateWidth())
            }
        }
        assertWidths(60f, 60f)

        press("b0")
        advance(400)

        assertWidths(84f, 36f)
    }

    @Test
    fun pressButtonWithoutAnimateWidth_doesNotChangeWidths() {
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf))
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        press("b1")
        advance(400)
        assertWidths(100f, 100f, 100f)

        // The second animated button is the third item: it takes the growth from its only
        // neighbor.
        release("b1")
        advance(500)
        press("b2")
        advance(400)
        assertWidths(100f, 76f, 124f)
    }

    @Test
    fun pressEachButtonInTurn_withClickActions_expandsEachPressedButton() {
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                expansionWidth = 24.rdp,
                contentPadding = zeroPadding,
            ) {
                for (label in listOf("b0", "b1", "b2")) {
                    RemoteIconButton(
                        onClick = hostAction("click".rs, 1.rf),
                        enabled = true.rb,
                        modifier =
                            RemoteModifier.weight(1f.rf).animateWidth().semantics {
                                contentDescription = label.rs
                            },
                    ) {
                        RemoteText(label.rs)
                    }
                }
            }
        }
        val pressedWidths = mutableListOf<List<Float>>()

        for (label in listOf("b0", "b1", "b2", "b0")) {
            press(label)
            advance(500)
            pressedWidths += buttonWidths()
            release(label)
            advance(2000)
        }

        assertThat(pressedWidths)
            .containsExactly(
                listOf(124f, 76f, 100f),
                listOf(88f, 124f, 88f),
                listOf(100f, 76f, 124f),
                listOf(124f, 76f, 100f),
            )
            .inOrder()
    }

    @Test
    fun pressIsFasterThanRelease() {
        // The press spring reaches halfway sooner than the release spring gets back halfway.
        setGroup { ThreeButtonGroup() }

        press("b0")
        val pressingWidths = widthsOverTime(1000)
        release("b0")
        val releasingWidths = widthsOverTime(2000)

        val pressHalfwayFrame = pressingWidths.indexOfFirst { it[0] >= 112f }
        val releaseHalfwayFrame = releasingWidths.indexOfFirst { it[0] <= 112f }
        assertThat(pressHalfwayFrame).isAtLeast(0)
        assertThat(releaseHalfwayFrame).isGreaterThan(pressHalfwayFrame)
    }

    @Test
    fun expressiveMotionScheme_pressOvershoots() {
        // The expressive fast spatial spring is underdamped, so the pressed button grows past
        // its pressed width before it settles.
        setGroup {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.expressive()) {
                ThreeButtonGroup()
            }
        }

        press("b0")
        val pressingWidths = widthsOverTime(300)
        advance(700)

        assertThat(pressingWidths.maxOf { it[0] }).isGreaterThan(124.5f)
        assertWidths(124f, 76f, 100f)
    }

    @Test
    fun withoutTouchSupport_animateWidthIsSkipped() {
        setGroup(profile = TestProfiles.wearWidgets) { ThreeButtonGroup() }

        press("b0")
        advance(400)

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun negativeExpansionWidth_animateWidthIsSkipped() {
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                expansionWidth = (-8).rdp,
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        press("b0")
        advance(400)

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun groupNarrowerThanDocument_widthsFollowTheGroup() {
        // The group is narrower than the 308dp document: 154dp - 2 * 4dp spacing = 146dp / 3 =
        // 48.667dp each.
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(154.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf))
                TestButton("b1", RemoteModifier.weight(1f.rf))
                TestButton("b2", RemoteModifier.weight(1f.rf))
            }
        }

        assertWidths(146f / 3f, 146f / 3f, 146f / 3f)
    }

    @Test
    fun groupNarrowerThanDocument_minWidthsStayExact() {
        // Weights 0.4 : 1.0 : 1.6 with 48dp minWidths. In a group as wide as the document,
        // only b0 would be at its minWidth. In the 158dp group, 150dp are available: 20, 50 and
        // 80dp by weight, so b0 owes 28dp, b1 can only give 2dp before reaching its minWidth,
        // and b2 gives the remaining 26dp -> 48, 48 and 54dp.
        setGroup {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(158.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(0.4f.rf).minWidth(48.rdp))
                TestButton("b1", RemoteModifier.weight(1.0f.rf).minWidth(48.rdp))
                TestButton("b2", RemoteModifier.weight(1.6f.rf).minWidth(48.rdp))
            }
        }

        assertWidths(48f, 48f, 54f)
    }

    @Test
    fun staticButtonGroup_withoutAnimateWidth_worksOnWearWidgetsProfile() {
        setGroup(profile = TestProfiles.wearWidgets) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf))
                TestButton("b1", RemoteModifier.weight(1f.rf))
                TestButton("b2", RemoteModifier.weight(1f.rf))
            }
        }

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun rcPlayer_rendersButtonGroupWithoutDuplicateDimensionModifiers() {
        // 258dp - 2 * 4dp spacing = 250dp; weights 1 : 1.5 : 1 (sum 3.5) -> 71.43, 107.14, 71.43
        val doc =
            setGroup(width = 258) {
                RemoteButtonGroup(
                    modifier = RemoteModifier.width(258.rdp),
                    contentPadding = zeroPadding,
                ) {
                    RemoteIconButton(
                        onClick = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).animateWidth().semantics {
                                contentDescription = "b0".rs
                            },
                    ) {
                        RemoteText("A".rs)
                    }
                    RemoteButton(
                        onClick = RemoteAction.None,
                        contentPadding = zeroPadding,
                        modifier =
                            RemoteModifier.weight(1.5f.rf).animateWidth().semantics {
                                contentDescription = "b1".rs
                            },
                    ) {
                        RemoteText("B".rs)
                    }
                    RemoteIconButton(
                        onClick = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).animateWidth().semantics {
                                contentDescription = "b2".rs
                            },
                    ) {
                        RemoteText("C".rs)
                    }
                }
            }

        assertNoDuplicateDimensionModifiers(doc)
        assertWidths(250f * (1f / 3.5f), 250f * (1.5f / 3.5f), 250f * (1f / 3.5f))
    }

    @Test
    fun defaultModifier_fillsDocumentWidth() {
        setGroup {
            RemoteButtonGroup(contentPadding = zeroPadding) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun defaultModifier_fillsDocumentWidthWithoutTouchSupport() {
        setGroup(profile = TestProfiles.wearWidgets) {
            RemoteButtonGroup(contentPadding = zeroPadding) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun fourAnimatedButtons_lastButtonExpands() {
        setGroup(width = 412) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(412.rdp),
                expansionWidth = 24.rdp,
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b3", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        press("b3")
        advance(400)

        assertWidths(100f, 100f, 76f, 124f)
    }

    @Test
    fun singleButtonWithAnimateWidth_pressingKeepsFullWidth() {
        setGroup(width = 200) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(200.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }
        assertWidths(200f)

        press("b0")
        advance(400)

        assertWidths(200f)
    }

    @Test
    fun sharedWidthBelowSumOfMinWidths_distributesProportionallyByWeight() {
        setGroup(width = 94) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(94.rdp),
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).minWidth(60.rdp))
                TestButton("b1", RemoteModifier.weight(2f.rf).minWidth(60.rdp))
            }
        }

        assertWidths(30f, 60f)
    }

    @Test
    fun pressExpansion_clampsNeighborAtZeroWhenNarrowerThanGrowth() {
        setGroup(width = 124) {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(124.rdp),
                expansionWidth = 30.rdp,
                contentPadding = zeroPadding,
            ) {
                TestButton("b0", RemoteModifier.weight(1f.rf).minWidth(10.rdp).animateWidth())
                TestButton("b1", RemoteModifier.weight(0.2f.rf).minWidth(10.rdp).animateWidth())
            }
        }

        press("b0")
        advance(400)

        assertWidths(120f, 0f)
    }

    @Test
    fun stableContent_composesOnce() {
        val compositions = AtomicInteger(0)

        capture {
            RemoteButtonGroup(
                modifier = RemoteModifier.width(308.rdp),
                contentPadding = zeroPadding,
            ) {
                compositions.incrementAndGet()
                TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
                TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
            }
        }

        assertThat(compositions.get()).isEqualTo(1)
    }

    @Test
    fun siblingRecomposition_preservesButtonGroupWidths() {
        val header = mutableStateOf("first")
        val documents = mutableListOf<ByteArray>()
        runTest {
            val flow =
                captureRemoteDocument(
                    context = context,
                    creationDisplayInfo =
                        RemoteCreationDisplayInfo(width = 308, height = 100, densityDpi = 160),
                    updateThrottle = CaptureUpdateThrottle.None,
                    coroutineContext = coroutineContext,
                ) {
                    RemoteMaterialTheme {
                        RemoteText(header.value.rs)
                        ThreeButtonGroup()
                    }
                }
            val job = launch { flow.take(2).toList(documents) }
            testScheduler.advanceUntilIdle()
            Snapshot.withMutableSnapshot { header.value = "second" }
            Snapshot.sendApplyNotifications()
            testScheduler.advanceUntilIdle()
            job.join()
        }

        playDocument(documents[1])

        assertWidths(100f, 100f, 100f)
    }

    @Test
    fun invalidParameters_throwIllegalArgumentException() {
        val dynamicWeight = MutableRemoteFloat(1f)
        val dynamicMinWidth = MutableRemoteFloat(48f).asRemoteDp()

        assertFailsWith<IllegalArgumentException> {
            capture { RemoteButtonGroup { TestButton("b0", RemoteModifier.weight(0f.rf)) } }
        }
        assertFailsWith<IllegalArgumentException> {
            capture {
                RemoteButtonGroup {
                    TestButton("b0", RemoteModifier.weight(Float.POSITIVE_INFINITY.rf))
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            capture { RemoteButtonGroup { TestButton("b0", RemoteModifier.weight(dynamicWeight)) } }
        }
        assertFailsWith<IllegalArgumentException> {
            capture { RemoteButtonGroup { TestButton("b0", RemoteModifier.minWidth(0.rdp)) } }
        }
        assertFailsWith<IllegalArgumentException> {
            capture {
                RemoteButtonGroup { TestButton("b0", RemoteModifier.minWidth(dynamicMinWidth)) }
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun TestButton(label: String, modifier: RemoteModifier) {
        RemoteIconButton(
            onClick = RemoteAction.None,
            enabled = true.rb,
            modifier = modifier.semantics { contentDescription = label.rs },
        ) {
            RemoteText(label.rs)
        }
    }

    @Composable
    @RemoteComposable
    private fun ThreeButtonGroup() {
        RemoteButtonGroup(
            modifier = RemoteModifier.width(308.rdp),
            expansionWidth = 24.rdp,
            contentPadding = zeroPadding,
        ) {
            TestButton("b0", RemoteModifier.weight(1f.rf).animateWidth())
            TestButton("b1", RemoteModifier.weight(1f.rf).animateWidth())
            TestButton("b2", RemoteModifier.weight(1f.rf).animateWidth())
        }
    }

    /**
     * Plays [content] on the embedded player, in a document [width] dp wide, with the frames driven
     * by [advance], and lays it out.
     */
    private fun setGroup(
        width: Int = 308,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        playerRule.mainClock.autoAdvance = false
        val document =
            playerRule.setRemoteContent(
                profile = profile,
                onNamedAction = { name, _, _ -> namedActions += name },
                remoteCreationDisplayInfo =
                    RemoteCreationDisplayInfo(width = width, height = 100, densityDpi = 160),
            ) {
                RemoteMaterialTheme { content() }
            }
        advance(100)
        return document
    }

    /** Plays the document [bytes] on the embedded player, like [setGroup]. */
    private fun playDocument(bytes: ByteArray, width: Int = 308) {
        playerRule.mainClock.autoAdvance = false
        val document = RecordedDocument(bytes, RcPlatformProfiles.ANDROIDX)
        playerRule.baseRule.setContent(
            creation =
                object : RemoteBaseContentTestRule.Creation {
                    @Composable
                    override fun rememberRemoteDocument(
                        composable: @RemoteComposable @Composable () -> Unit
                    ): MutableState<RecordedDocument?> = remember { mutableStateOf(document) }
                },
            player =
                object : RemoteBaseContentTestRule.Player {
                    @Composable
                    override fun Play(document: RecordedDocument, size: Size) {
                        RcPlayer(document = document.coreDocument)
                    }
                },
            size = Size(width.toFloat(), 100f),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(width.dp, 100.dp)) { content() }
            },
            composable = {},
        )
        advance(100)
    }

    /** Captures [content] into a document, without playing it. */
    private fun capture(content: @Composable @RemoteComposable () -> Unit) {
        runBlocking {
            captureRule.captureRecordedDocument(
                context = context,
                creationDisplayInfo =
                    RemoteCreationDisplayInfo(width = 308, height = 100, densityDpi = 160),
                content = { RemoteMaterialTheme { content() } },
            )
        }
    }

    /** Advances the frames by [ms], handling input between frames like a device does. */
    private fun advance(ms: Long) {
        var remainingMs = ms
        while (remainingMs > 0) {
            val stepMs = minOf(FrameMs, remainingMs)
            playerRule.mainClock.advanceTimeBy(stepMs)
            playerRule.waitForIdle()
            remainingMs -= stepMs
        }
    }

    private fun press(label: String) {
        playerRule.onNodeWithContentDescription(label).performTouchInput { down(center) }
    }

    private fun release(label: String) {
        playerRule.onNodeWithContentDescription(label).performTouchInput { up() }
    }

    private fun cancel(label: String) {
        playerRule.onNodeWithContentDescription(label).performTouchInput { cancel() }
    }

    /** The widths, in dp, of the buttons labeled `b0`, `b1`, and so on. */
    private fun buttonWidths(): List<Float> =
        generateSequence(0) { it + 1 }
            .map { "b$it" }
            .takeWhile {
                playerRule.onAllNodesWithContentDescription(it).fetchSemanticsNodes().isNotEmpty()
            }
            .map {
                val bounds = playerRule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot()
                (bounds.right - bounds.left).value
            }
            .toList()

    /** The [buttonWidths] of each frame of the next [durationMs]. */
    private fun widthsOverTime(durationMs: Long): List<List<Float>> =
        (1..durationMs / FrameMs).map {
            advance(FrameMs)
            buttonWidths()
        }

    private fun assertWidths(vararg expected: Float) {
        assertThat(buttonWidths().toFloatArray())
            .usingTolerance(WidthTolerance)
            .containsExactly(expected)
            .inOrder()
    }

    private fun collectLayoutComponents(operations: List<Operation>): List<LayoutComponent> {
        val result = mutableListOf<LayoutComponent>()
        fun visit(op: Any?) {
            if (op is LayoutComponent) {
                result.add(op)
                op.childrenComponents.forEach { visit(it) }
            } else if (op is Component) {
                op.list.forEach { visit(it) }
            }
        }
        operations.forEach { visit(it) }
        return result
    }

    private fun assertNoDuplicateDimensionModifiers(doc: CoreDocument) {
        val layouts =
            collectLayoutComponents(doc.rootLayoutComponent?.let { listOf(it) } ?: emptyList())
        assertThat(layouts).isNotEmpty()
        for (layout in layouts) {
            val widthModifiers =
                layout.componentModifiers.modifiersList.filterIsInstance<WidthModifierOperation>()
            val heightModifiers =
                layout.componentModifiers.modifiersList.filterIsInstance<HeightModifierOperation>()
            assertThat(widthModifiers.size).isAtMost(1)
            assertThat(heightModifiers.size).isAtMost(1)
        }
    }
}
