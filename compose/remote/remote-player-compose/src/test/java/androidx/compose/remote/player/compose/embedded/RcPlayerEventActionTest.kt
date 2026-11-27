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

package androidx.compose.remote.player.compose.embedded

import android.view.KeyEvent.KEYCODE_A
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.input.MetadataUtil
import androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_FINAL
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_INITIAL
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.embedded.doc.preprocessDocument
import androidx.compose.remote.player.compose.embedded.modifier.RcPlayerEvents
import androidx.compose.remote.player.compose.embedded.modifier.pointerEventActions
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Key and pointer input dispatched to `EventHandlerModifierOperation`s, run against both players.
 *
 * Interactions are observed through the host named actions the handlers run.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerEventActionTest(player: PlayerUnderTest) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters() = PlayerUnderTest.parameters()
    }

    @get:Rule val rule = RemotePlayerTestRule(player)

    @Test
    fun pointerEvents_dispatchTunnelingBubblingAndFinal_withLocalPosition() {
        val document = pointerBoxDocument { writer ->
            val pointerIdVar = Utils.idFromNan(writer.addFloatConstant(-1f))
            val xId = Utils.idFromNan(writer.addFloatConstant(0f))
            val yId = Utils.idFromNan(writer.addFloatConstant(0f))
            RecordingModifier()
                .onPointer(POINTER_PASS_INITIAL, POINTER_SUBTYPE_DOWN) {
                    HostAction("tunnelingDown")
                }
                .onPointer(
                    POINTER_PASS_MAIN,
                    POINTER_SUBTYPE_DOWN,
                    flags = POINTER_STATUS_CONSUMED,
                    dataIds = intArrayOf(pointerIdVar, xId, yId),
                ) {
                    HostAction("bubblingDown", HostNamedActionOperation.FLOAT_TYPE, xId)
                }
                .onPointer(
                    POINTER_PASS_FINAL,
                    POINTER_SUBTYPE_DOWN,
                    consumed = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("finalDownConsumed")
                }
                .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                    HostAction("bubblingUp", HostNamedActionOperation.FLOAT_TYPE, yId)
                }
                .onPointer(POINTER_PASS_FINAL, POINTER_SUBTYPE_UP) {
                    HostAction("finalUp", HostNamedActionOperation.FLOAT_TYPE, pointerIdVar)
                }
        }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 20f))
            up()
        }

        // The y and pointerId data are written on the down/up, and read as host action values.
        assertThat(rule.takeNamedActions())
            .containsExactly(
                "tunnelingDown" to null,
                "bubblingDown" to 10f,
                "finalDownConsumed" to null,
                "bubblingUp" to 20f,
                "finalUp" to 0f,
            )
            .inOrder()
    }

    @Test
    fun pointerMove_dispatchesThePosition() {
        val document = pointerBoxDocument { writer ->
            val xId = Utils.idFromNan(writer.addFloatConstant(0f))
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_MOVE,
                dataIds = intArrayOf(0, xId),
            ) {
                HostAction("move", HostNamedActionOperation.FLOAT_TYPE, xId)
            }
        }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(30f, 10f))
            moveTo(Offset(50f, 10f))
            up()
        }

        assertThat(rule.takeNamedActions()).containsExactly("move" to 30f, "move" to 50f).inOrder()
    }

    @Test
    fun pointerUp_dispatchesVelocity() {
        val document = pointerBoxDocument { writer ->
            val vxId = Utils.idFromNan(writer.addFloatConstant(0f))
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_UP,
                dataIds = intArrayOf(0, 0, 0, vxId),
            ) {
                HostAction("up", HostNamedActionOperation.FLOAT_TYPE, vxId)
            }
        }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            advanceEventTime(16L)
            moveTo(Offset(40f, 10f))
            advanceEventTime(16L)
            moveTo(Offset(80f, 10f))
            advanceEventTime(16L)
            up()
        }

        val actions = rule.takeNamedActions()
        assertThat(actions).hasSize(1)
        assertThat(actions.single().first).isEqualTo("up")
        assertThat(actions.single().second as Float).isGreaterThan(0f)
    }

    @Test
    fun pointerEvents_withPadding_hitPaddingAndUseOuterOrigin() {
        val document =
            pointerDocument(100, 100) { writer ->
                val xId = Utils.idFromNan(writer.addFloatConstant(0f))
                val yId = Utils.idFromNan(writer.addFloatConstant(0f))
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .padding(20)
                        .onPointer(
                            POINTER_PASS_MAIN,
                            POINTER_SUBTYPE_DOWN,
                            dataIds = intArrayOf(0, xId, yId),
                        ) {
                            HostAction("downX", HostNamedActionOperation.FLOAT_TYPE, xId)
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                            HostAction("upY", HostNamedActionOperation.FLOAT_TYPE, yId)
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        rule.play(document, 100, 100)

        // Tap inside the 20px padding: should hit the component and report outer-bounds
        // coordinates.
        rule.tap(10f, 15f)

        assertThat(rule.takeNamedActions()).containsExactly("downX" to 10f, "upY" to 15f).inOrder()
    }

    @Test
    fun pointerConsumedByAChild_cancelsTheParentsStream() {
        // PointerInputRouter forwards the consumed MOVE rather than synthesizing CANCEL; Compose
        // synthesizes CANCEL for the bubbling stream when a descendant consumes a tracked pointer.
        assumeTrue(
            "View player delivers consumed MOVE rather than synthetic CANCEL",
            rule.player != PlayerUnderTest.View,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                            HostAction("parentDown")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                            HostAction("parentCancel")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                            HostAction("parentUp")
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    writer.box(
                        RecordingModifier()
                            .size(50, 50)
                            // The child takes the stream once the pointer moves.
                            .onPointer(
                                POINTER_PASS_MAIN,
                                POINTER_SUBTYPE_MOVE,
                                flags = POINTER_STATUS_CONSUMED,
                            ) {
                                HostAction("childMove")
                            },
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(20f, 10f))
            up()
        }

        assertThat(rule.takeNamedActions())
            .containsExactly("parentDown" to null, "childMove" to null, "parentCancel" to null)
            .inOrder()
    }

    @Test
    fun pointerDownConsumedByChild_doesNotTrackOrFireUnconsumedParent() {
        assumeTrue(
            "Embedded player skips tracking consumed DOWN on ancestors",
            rule.player != PlayerUnderTest.View,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                            HostAction("parentDown")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_MOVE) {
                            HostAction("parentMove")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                            HostAction("parentCancel")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                            HostAction("parentUp")
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    writer.box(
                        RecordingModifier().size(50, 50).onPointer(
                            POINTER_PASS_MAIN,
                            POINTER_SUBTYPE_DOWN,
                            flags = POINTER_STATUS_CONSUMED,
                        ) {
                            HostAction("childDown")
                        },
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(20f, 10f))
            up()
        }

        assertThat(rule.takeNamedActions()).containsExactly("childDown" to null)
    }

    @Test
    fun pointerConsumedWithoutMovement_cancelsTrackedPointerImmediately() {
        assumeTrue(
            "Embedded pointer modifier test",
            rule.player == PlayerUnderTest.Embedded,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                            HostAction("parentDown")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                            HostAction("parentCancel")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                            HostAction("parentUp")
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        val boxComponent =
            document.rootLayoutComponent!!.list.first { it is LayoutComponent } as LayoutComponent
        val namedActions = mutableListOf<Pair<String, Any?>>()
        var consumeNextStationaryChange = false
        val remoteContext = AndroidRemoteContext().apply { document.initializeContext(this) }
        rule.composeRule.setContent {
            CompositionLocalProvider(
                LocalCoreDocument provides document,
                LocalRemoteContext provides remoteContext,
                LocalRemoteActionHandler provides { _, _ -> },
                LocalRemoteNamedActionHandler provides
                    { name, value ->
                        namedActions += name to value
                    },
            ) {
                Box(Modifier.size(100.dp).pointerEventActions(boxComponent)) {
                    Box(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (consumeNextStationaryChange) {
                                        consumeNextStationaryChange = false
                                        event.changes.fastForEach { it.consume() }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        rule.composeRule.onRoot().performTouchInput {
            down(0, Offset(10f, 10f))
        }
        assertThat(namedActions).containsExactly("parentDown" to null)

        // Trigger a stationary pointer event for pointer 0 (only pointer 1 moves) that a child
        // consumes: pointerEventActions must cancel pointer 0 without waiting for movement/up.
        consumeNextStationaryChange = true
        rule.composeRule.onRoot().performTouchInput {
            down(1, Offset(80f, 80f))
        }
        assertThat(namedActions)
            .containsExactly("parentDown" to null, "parentCancel" to null)
            .inOrder()

        rule.composeRule.onRoot().performTouchInput {
            up(1)
            up(0)
        }
        assertThat(namedActions)
            .containsExactly("parentDown" to null, "parentCancel" to null)
            .inOrder()
    }

    @Test
    fun pointerOutsideTheComponent_isNotDispatched() {
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                    writer.box(
                        RecordingModifier().size(50, 50).onPointer(
                            POINTER_PASS_MAIN,
                            POINTER_SUBTYPE_DOWN,
                        ) {
                            HostAction("childDown")
                        },
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        rule.play(document, 100, 100)

        rule.tap(75f, 75f)

        assertThat(rule.takeNamedActions()).isEmpty()
    }

    @Test
    fun keyEvents_dispatchBothPassesAndKeyUps_toFocusedComponent() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { writer ->
            val keyCodeId = Utils.idFromNan(writer.addFloatConstant(0f))
            RecordingModifier()
                .onKey(
                    RcPlayerEvents.PASS_TUNNELING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    dataIds = intArrayOf(keyCodeId),
                ) {
                    HostAction("tunnelingDown", HostNamedActionOperation.FLOAT_TYPE, keyCodeId)
                }
                .onKey(RcPlayerEvents.PASS_BUBBLING, RcPlayerEvents.KEY_SUBTYPE_DOWN) {
                    HostAction("bubblingDown")
                }
                .onKey(RcPlayerEvents.PASS_BUBBLING, RcPlayerEvents.KEY_SUBTYPE_UP) {
                    HostAction("bubblingUp")
                }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        rule.pressKey(KEYCODE_A)

        assertThat(rule.takeNamedActions())
            .containsExactly(
                "tunnelingDown" to KEYCODE_A.toFloat(),
                "bubblingDown" to null,
                "bubblingUp" to null,
            )
            .inOrder()
    }

    @Test
    fun keyConsumedInTheTunnelingPass_isNotBubbled() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { _ ->
            RecordingModifier()
                .onKey(
                    RcPlayerEvents.PASS_TUNNELING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    flags = RcPlayerEvents.STATUS_CONSUMED,
                ) {
                    HostAction("tunnelingDown")
                }
                .onKey(RcPlayerEvents.PASS_BUBBLING, RcPlayerEvents.KEY_SUBTYPE_DOWN) {
                    HostAction("bubblingDown")
                }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        assertThat(rule.pressKey(KEYCODE_A)).isTrue()
        assertThat(rule.takeNamedActions()).containsExactly("tunnelingDown" to null)
    }

    @Test
    fun keyNotConsumed_isNotHandled() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { _ ->
            RecordingModifier().onKey(
                RcPlayerEvents.PASS_BUBBLING,
                RcPlayerEvents.KEY_SUBTYPE_DOWN,
            ) {
                HostAction("observed")
            }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        assertThat(rule.pressKey(KEYCODE_A)).isFalse()
        assertThat(rule.takeNamedActions()).containsExactly("observed" to null)
    }

    @Test
    fun handlerCondition_gatesTheActions() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { _ ->
            RecordingModifier()
                .onKey(
                    RcPlayerEvents.PASS_BUBBLING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    condition = floatArrayOf(0f),
                ) {
                    HostAction("never")
                }
                .onKey(
                    RcPlayerEvents.PASS_BUBBLING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    condition = floatArrayOf(1f),
                ) {
                    HostAction("always")
                }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        rule.pressKey(KEYCODE_A)

        assertThat(rule.takeNamedActions()).containsExactly("always" to null)
    }

    @Test
    fun pointerAndClickCoexistence_outerConsumingOnEventDoesNotSwallowClick() {
        // FEATURE_UNIFIED_POINTER_INPUT swaps ClickModifierOperation for ClickModifierOperation2,
        // which only the View player handles (see POINTER_INPUT.md); the embedded player uses
        // ClickModifierOperation without the header flag.
        val buildBox: (RemoteComposeWriter) -> Unit = { writer ->
            writer.box(
                RecordingModifier()
                    .onPointer(
                        POINTER_PASS_MAIN,
                        POINTER_SUBTYPE_DOWN,
                        flags = POINTER_STATUS_CONSUMED,
                    ) {
                        HostAction("eventDown")
                    }
                    .onClick(HostAction("click"))
                    .size(100, 100),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {}
        }
        val document =
            if (rule.player == PlayerUnderTest.View) {
                pointerDocument(100, 100, buildBox)
            } else {
                experimentalDocument(100, 100, buildBox)
            }
        rule.play(document, 100, 100)

        rule.tap(50f, 50f)

        assertThat(rule.takeNamedActions()).containsExactly("click" to null)
    }

    @Test
    fun pointerIds_reuseSlotIdsAcrossGestures() {
        val document = pointerBoxDocument { writer ->
            val pointerIdVar = Utils.idFromNan(writer.addFloatConstant(-1f))
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_DOWN,
                dataIds = intArrayOf(pointerIdVar),
            ) {
                HostAction("down", HostNamedActionOperation.FLOAT_TYPE, pointerIdVar)
            }
        }
        rule.play(document, 100, 100)

        rule.tap(10f, 10f)
        rule.tap(20f, 20f)

        assertThat(rule.takeNamedActions()).containsExactly("down" to 0f, "down" to 0f).inOrder()
    }

    @Test
    fun pointerMainPass_sameComponentRunsInnerToOuter() {
        val document = pointerBoxDocument { _ ->
            RecordingModifier()
                .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                    HostAction("outerUnconsumed")
                }
                .onPointer(
                    POINTER_PASS_MAIN,
                    POINTER_SUBTYPE_DOWN,
                    consumed = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("outerConsumed")
                }
                .onPointer(
                    POINTER_PASS_MAIN,
                    POINTER_SUBTYPE_DOWN,
                    flags = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("innerDown")
                }
        }
        rule.play(document, 100, 100)

        rule.tap(10f, 10f)

        assertThat(rule.takeNamedActions())
            .containsExactly("innerDown" to null, "outerConsumed" to null)
            .inOrder()
    }

    @Test
    fun keyBubblingPass_sameComponentRunsInnerToOuterAndStopsOnConsume() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { _ ->
            RecordingModifier()
                .onKey(RcPlayerEvents.PASS_BUBBLING, RcPlayerEvents.KEY_SUBTYPE_DOWN) {
                    HostAction("outer")
                }
                .onKey(
                    RcPlayerEvents.PASS_BUBBLING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    flags = RcPlayerEvents.STATUS_CONSUMED,
                ) {
                    HostAction("inner")
                }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        assertThat(rule.pressKey(KEYCODE_A)).isTrue()
        assertThat(rule.takeNamedActions()).containsExactly("inner" to null)
    }

    @Test
    fun keyTunnelingPass_sameComponentRunsOuterToInnerAndStopsOnConsume() {
        assumeKeyEventsSupported()
        val document = focusableBoxDocument { _ ->
            RecordingModifier()
                .onKey(
                    RcPlayerEvents.PASS_TUNNELING,
                    RcPlayerEvents.KEY_SUBTYPE_DOWN,
                    flags = RcPlayerEvents.STATUS_CONSUMED,
                ) {
                    HostAction("outer")
                }
                .onKey(RcPlayerEvents.PASS_TUNNELING, RcPlayerEvents.KEY_SUBTYPE_DOWN) {
                    HostAction("inner")
                }
        }
        rule.play(document, 100, 100)
        rule.focusHost()

        assertThat(rule.pressKey(KEYCODE_A)).isTrue()
        assertThat(rule.takeNamedActions()).containsExactly("outer" to null)
    }

    @Test
    fun handlerCondition_evaluatesComputedExpressionFromDataIds() {
        assumeTrue(
            "Embedded player evaluates computed condition expressions live via GraphContext",
            rule.player == PlayerUnderTest.Embedded,
        )
        val document = pointerBoxDocument { writer ->
            val xNan = writer.addFloatConstant(0f)
            val xId = Utils.idFromNan(xNan)
            val isRightHalf =
                writer.floatExpression(
                    xNan,
                    50f,
                    AnimatedFloatExpression.SUB,
                    0f,
                    AnimatedFloatExpression.MAX,
                )
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_DOWN,
                dataIds = intArrayOf(0, xId),
                condition = floatArrayOf(isRightHalf),
            ) {
                HostAction("rightHalf", HostNamedActionOperation.FLOAT_TYPE, xId)
            }
        }
        rule.play(document, 100, 100)

        rule.tap(20f, 10f)
        rule.tap(80f, 10f)

        assertThat(rule.takeNamedActions()).containsExactly("rightHalf" to 80f)
    }

    @Test
    fun preprocessDocument_scansEventHandlerConditionForTimeAndTouchVariables() {
        assumeTrue(
            "Embedded player preprocessing test",
            rule.player == PlayerUnderTest.Embedded,
        )
        val timeDoc = pointerBoxDocument { _ ->
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_DOWN,
                condition = floatArrayOf(RemoteContext.FLOAT_ANIMATION_TIME),
            ) {
                HostAction("tick")
            }
        }
        val touchDoc = pointerBoxDocument { _ ->
            RecordingModifier().onPointer(
                POINTER_PASS_MAIN,
                POINTER_SUBTYPE_DOWN,
                condition = floatArrayOf(RemoteContext.FLOAT_TOUCH_POS_X),
            ) {
                HostAction("touch")
            }
        }

        val timePreprocessed = preprocessDocument(timeDoc)
        val touchPreprocessed = preprocessDocument(touchDoc)

        assertThat(timePreprocessed.hasContinuousTime).isTrue()
        assertThat(touchPreprocessed.readsTouchPosition).isTrue()
    }

    @Test
    fun pointerConsumedByAncestorInMain_cancelsChildStream() {
        assumeTrue(
            "Embedded player synthesizes CANCEL when an ancestor consumes MOVE in Main",
            rule.player == PlayerUnderTest.Embedded,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier().size(100, 100).onPointer(
                        POINTER_PASS_MAIN,
                        POINTER_SUBTYPE_MOVE,
                        flags = POINTER_STATUS_CONSUMED,
                    ) {
                        HostAction("parentMove")
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    writer.box(
                        RecordingModifier()
                            .size(100, 100)
                            .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                                HostAction("childDown")
                            }
                            .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_MOVE) {
                                HostAction("childMove")
                            }
                            .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                                HostAction("childCancel")
                            }
                            .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                                HostAction("childUp")
                            },
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(30f, 10f))
            moveTo(Offset(50f, 10f))
            up()
        }

        assertThat(rule.takeNamedActions())
            .containsExactly(
                "childDown" to null,
                "childMove" to null,
                "parentMove" to null,
                "childCancel" to null,
                "parentMove" to null,
            )
            .inOrder()
    }

    @Test
    fun pointerCancelledByChild_stopsInitialAndFinalForPointer() {
        assumeTrue(
            "Embedded player stops Initial and Final passes after cancelling a pointer",
            rule.player == PlayerUnderTest.Embedded,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .onPointer(POINTER_PASS_INITIAL, POINTER_SUBTYPE_MOVE) {
                            HostAction("parentInitialMove")
                        }
                        .onPointer(POINTER_PASS_INITIAL, POINTER_SUBTYPE_UP) {
                            HostAction("parentInitialUp")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_DOWN) {
                            HostAction("parentDown")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                            HostAction("parentCancel")
                        }
                        .onPointer(
                            POINTER_PASS_FINAL,
                            POINTER_SUBTYPE_MOVE,
                            consumed = POINTER_STATUS_CONSUMED,
                        ) {
                            HostAction("parentFinalMove")
                        }
                        .onPointer(POINTER_PASS_FINAL, POINTER_SUBTYPE_UP) {
                            HostAction("parentFinalUp")
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    writer.box(
                        RecordingModifier().size(50, 50).onPointer(
                            POINTER_PASS_MAIN,
                            POINTER_SUBTYPE_MOVE,
                            flags = POINTER_STATUS_CONSUMED,
                        ) {
                            HostAction("childMove")
                        },
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(20f, 10f))
            moveTo(Offset(30f, 10f))
            up()
        }

        assertThat(rule.takeNamedActions())
            .containsExactly(
                "parentDown" to null,
                "parentInitialMove" to null,
                "childMove" to null,
                "parentCancel" to null,
                "childMove" to null,
            )
            .inOrder()
    }

    @Test
    fun pointerActionCancel_dispatchesCancelWithoutFakeUp() {
        val document = pointerBoxDocument { _ ->
            RecordingModifier()
                .onPointer(POINTER_PASS_INITIAL, POINTER_SUBTYPE_UP) { HostAction("initialUp") }
                .onPointer(
                    POINTER_PASS_INITIAL,
                    POINTER_SUBTYPE_UP,
                    consumed = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("initialUpConsumed")
                }
                .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) { HostAction("mainUp") }
                .onPointer(
                    POINTER_PASS_MAIN,
                    POINTER_SUBTYPE_UP,
                    consumed = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("mainUpConsumed")
                }
                .onPointer(POINTER_PASS_FINAL, POINTER_SUBTYPE_UP) { HostAction("finalUp") }
                .onPointer(
                    POINTER_PASS_FINAL,
                    POINTER_SUBTYPE_UP,
                    consumed = POINTER_STATUS_CONSUMED,
                ) {
                    HostAction("finalUpConsumed")
                }
                .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) { HostAction("cancel") }
        }
        rule.play(document, 100, 100)

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            cancel()
        }

        assertThat(rule.takeNamedActions()).containsExactly("cancel" to null)
    }

    @Test
    fun pointerUpConsumedByOuterInitial_cancelsChildInsteadOfDispatchingUp() {
        assumeTrue(
            "Embedded pointer modifier test",
            rule.player == PlayerUnderTest.Embedded,
        )
        val document =
            pointerDocument(100, 100) { writer ->
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .onPointer(
                            POINTER_PASS_INITIAL,
                            POINTER_SUBTYPE_UP,
                            consumed = POINTER_STATUS_CONSUMED,
                            flags = POINTER_STATUS_CONSUMED,
                        ) {
                            HostAction("childInitialUpConsumed")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_UP) {
                            HostAction("childMainUp")
                        }
                        .onPointer(
                            POINTER_PASS_MAIN,
                            POINTER_SUBTYPE_UP,
                            consumed = POINTER_STATUS_CONSUMED,
                        ) {
                            HostAction("childMainUpConsumed")
                        }
                        .onPointer(POINTER_PASS_MAIN, POINTER_SUBTYPE_CANCEL) {
                            HostAction("childCancel")
                        },
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        val boxComponent =
            document.rootLayoutComponent!!.list.first { it is LayoutComponent } as LayoutComponent
        val namedActions = mutableListOf<Pair<String, Any?>>()
        val remoteContext = AndroidRemoteContext().apply { document.initializeContext(this) }
        rule.composeRule.setContent {
            CompositionLocalProvider(
                LocalCoreDocument provides document,
                LocalRemoteContext provides remoteContext,
                LocalRemoteActionHandler provides { _, _ -> },
                LocalRemoteNamedActionHandler provides
                    { name, value ->
                        namedActions += name to value
                    },
            ) {
                Box(
                    Modifier.size(100.dp).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.fastForEach { change ->
                                    if (!change.pressed && change.previousPressed) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Box(Modifier.fillMaxSize().pointerEventActions(boxComponent))
                }
            }
        }

        rule.composeRule.onRoot().performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        assertThat(namedActions)
            .containsExactly("childInitialUpConsumed" to null, "childCancel" to null)
            .inOrder()
    }

    // TODO: RemoteComposeView dispatches key downs to EventManager with no key router yet.
    //   Remove this once it routes key events in the layout the embedded player uses.
    private fun assumeKeyEventsSupported() {
        assumeTrue(
            "The View player doesn't route key events",
            rule.player != PlayerUnderTest.View,
        )
    }

    private fun pointerDocument(
        width: Int,
        height: Int,
        content: (RemoteComposeWriter) -> Unit,
    ): CoreDocument {
        val profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) { creationDisplayInfo, p, callback ->
                RemoteComposeWriterAndroid(creationDisplayInfo, null, p, callback)
            }
        val writer =
            RemoteComposeWriterAndroid(
                profile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.FEATURE_UNIFIED_POINTER_INPUT, 1),
            )
        writer.root { content(writer) }
        return CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(writer.buffer(), 0, writer.bufferSize()).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    /** A 100x100 box carrying the modifiers [buildModifiers] adds. */
    private fun pointerBoxDocument(
        buildModifiers: (RemoteComposeWriter) -> RecordingModifier
    ): CoreDocument =
        pointerDocument(100, 100) { writer ->
            writer.box(
                buildModifiers(writer).size(100, 100),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {}
        }

    /**
     * A 100x100 clickable box, which takes focus so that key events reach it, carrying the
     * modifiers [buildModifiers] adds.
     */
    private fun focusableBoxDocument(
        buildModifiers: (RemoteComposeWriter) -> RecordingModifier
    ): CoreDocument =
        experimentalDocument(100, 100) { writer ->
            writer.box(
                buildModifiers(writer).size(100, 100).onClick(HostAction("click")),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {}
        }

    private fun RecordingModifier.onPointer(
        pass: Int,
        subtype: Int,
        consumed: Int = POINTER_STATUS_NOT_CONSUMED,
        flags: Int = POINTER_STATUS_NOT_CONSUMED,
        dataIds: IntArray? = null,
        condition: FloatArray? = null,
        action: () -> HostAction,
    ): RecordingModifier =
        onEvent(
            EVENT_TYPE_POINTER,
            MetadataUtil.pack(pass, subtype, consumed),
            flags,
            dataIds,
            condition,
            action(),
        )

    private fun RecordingModifier.onKey(
        pass: Int,
        subtype: Int,
        flags: Int = 0,
        dataIds: IntArray? = null,
        condition: FloatArray? = null,
        action: () -> HostAction,
    ): RecordingModifier =
        onEvent(
            RcPlayerEvents.EVENT_TYPE_KEY,
            RcPlayerEvents.metadata(pass, subtype),
            flags,
            dataIds,
            condition,
            action(),
        )
}
