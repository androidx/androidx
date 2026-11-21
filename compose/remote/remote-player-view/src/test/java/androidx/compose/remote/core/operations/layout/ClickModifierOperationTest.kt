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

package androidx.compose.remote.core.operations.layout

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.input.MetadataUtil
import androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.view.RemoteComposePlayer
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Click modifier tests, run against both implementations of the click modifier:
 * - `unifiedPointerInput = false`: [ClickModifierOperation], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [ClickModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClickModifierOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @Test
    fun tapInsideBox_runsAllClickActions() {
        // Arrange.
        var intId = 0
        var floatId = 0
        val player = createPlayer {
            intId = addInteger(0).toInt()
            floatId = Utils.idFromNan(addFloatConstant(0f))
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onClick(
                            ValueIntegerChange(intId, 5),
                            ValueFloatChange(floatId, 7.5f),
                            HostAction(42),
                        ),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        player.document.document.setHapticEngine { type -> triggeredHaptics.add(type) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getInteger(intId)).isEqualTo(5)
        assertThat(state.getFloat(floatId)).isWithin(1e-5f).of(7.5f)
        assertThat(triggeredActions).containsExactly(42 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun tapOutsideBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 150f, y = 150f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun dragReleasingOutsideBox_doesNotRunClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 175f, endY = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun tapInsideInvisibleBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            val visibilityId = addInteger(Component.Visibility.INVISIBLE).toInt()
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .visibility(visibilityId)
                        .onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun dragWithinBox_runsClickActions() {
        // The legacy touch path cancels the click on any drag beyond the touch slop.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 75f, endY = 65f)

        // Assert. Like Views and Compose: a drag that stays inside the box is still a click.
        // Only leaving the box, or another handler taking over the gesture, cancels it.
        assertThat(triggeredActions).containsExactly(42 to "")
    }

    @Test
    fun dragAlongLeftEdgeOfBox_runsClickActions() {
        // The legacy touch path cancels the click on any drag beyond the touch slop.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 0f, startY = 20f, endX = 0f, endY = 80f)

        // Assert. The left edge is still inside the box.
        assertThat(triggeredActions).containsExactly(42 to "")
    }

    @Test
    fun cancelledTap_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchCancel(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun tapInsideClickableChild_runsOnlyChildActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(2)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    @Test
    fun tapBesideClickableChild_runsParentActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(2)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 150f, y = 150f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun accessibilityClick_runsClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val document = player.document.document
        document.setHapticEngine { type -> triggeredHaptics.add(type) }
        val box = document.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()

        // Act. This is what the player does for an accessibility click.
        player.performClick(document, box, "")

        // Assert. The listener is also told which component was clicked.
        assertThat(triggeredActions).contains(42 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun tapAfterDocumentIsReplaced_runsClickActionsOfNewDocument() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        dispatchTap(player, x = 25f, y = 35f)
        val newDocument =
            RemoteComposeWriter(
                    RcPlatformProfiles.ANDROIDX,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, 200),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                    RemoteComposeWriter.hTag(
                        Header.FEATURE_UNIFIED_POINTER_INPUT,
                        if (unifiedPointerInput) 1 else 0,
                    ),
                )
                .apply {
                    root {
                        box(
                            RecordingModifier().size(100, 100).onClick(HostAction(2)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
        player.setDocument(newDocument.encodeToByteArray())
        val parent = player.parent as View
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, 200, 200)
        parent.draw(Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)))
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only: pointer events consumed by other handlers on the hit path.
    // Skipped on the legacy path, which has no notion of consumption.
    // ------------------------------------------------------------------------------------------

    @Test
    fun downConsumedByChild_doesNotRunActions() {
        assumeTrue(unifiedPointerInput)

        // Arrange. A child claims the down before the clickable parent sees it.
        val player =
            createPlayer(experimental = true) {
                root {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(42)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {
                        box(
                            RecordingModifier()
                                .size(100, 100)
                                .onEvent(
                                    /* type = */ EVENT_TYPE_POINTER,
                                    /* filter = */ MetadataUtil.pack(
                                        POINTER_PASS_MAIN,
                                        POINTER_SUBTYPE_DOWN,
                                        0,
                                    ),
                                    /* flags = */ POINTER_STATUS_CONSUMED,
                                    /* dataIds = */ null,
                                    /* condition = */ null,
                                ),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun upConsumedByChild_doesNotRunActions() {
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player =
            createPlayer(experimental = true) {
                root {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(42)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {
                        box(
                            RecordingModifier()
                                .size(100, 100)
                                .onEvent(
                                    /* type = */ EVENT_TYPE_POINTER,
                                    /* filter = */ MetadataUtil.pack(
                                        POINTER_PASS_MAIN,
                                        POINTER_SUBTYPE_UP,
                                        0,
                                    ),
                                    /* flags = */ POINTER_STATUS_CONSUMED,
                                    /* dataIds = */ null,
                                    /* condition = */ null,
                                ),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun moveConsumedByChild_cancelsClick() {
        assumeTrue(unifiedPointerInput)

        // Arrange. The child takes over the gesture on move (in the main pass, before the
        // parent), like a nested scrollable or slider would.
        val player =
            createPlayer(experimental = true) {
                root {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(42)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {
                        box(
                            RecordingModifier()
                                .size(100, 100)
                                .onEvent(
                                    /* type = */ EVENT_TYPE_POINTER,
                                    /* filter = */ MetadataUtil.pack(
                                        POINTER_PASS_MAIN,
                                        POINTER_SUBTYPE_MOVE,
                                        0,
                                    ),
                                    /* flags = */ POINTER_STATUS_CONSUMED,
                                    /* dataIds = */ null,
                                    /* condition = */ null,
                                ),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 75f, endY = 65f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun moveConsumedByParent_cancelsClick() {
        assumeTrue(unifiedPointerInput)

        // Arrange. The parent takes over the gesture on move in its own main pass, which runs
        // after the child's; the child observes the consumption in the final pass, like a
        // button inside a scrolling list.
        val player =
            createPlayer(experimental = true) {
                root {
                    box(
                        RecordingModifier()
                            .size(200, 200)
                            .onEvent(
                                /* type = */ EVENT_TYPE_POINTER,
                                /* filter = */ MetadataUtil.pack(
                                    POINTER_PASS_MAIN,
                                    POINTER_SUBTYPE_MOVE,
                                    0,
                                ),
                                /* flags = */ POINTER_STATUS_CONSUMED,
                                /* dataIds = */ null,
                                /* condition = */ null,
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {
                        box(
                            RecordingModifier().size(100, 100).onClick(HostAction(42)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 75f, endY = 65f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    /**
     * Creates a player for the document written by [buildDocument].
     *
     * @param experimental writes the document with the experimental profile, which is needed for
     *   the
     *   [androidx.compose.remote.core.operations.layout.modifiers.EventHandlerModifierOperation]
     */
    private fun createPlayer(
        width: Int = 200,
        height: Int = 200,
        experimental: Boolean = false,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): RemoteComposePlayer {
        val unifiedPointerInputTag =
            RemoteComposeWriter.hTag(
                Header.FEATURE_UNIFIED_POINTER_INPUT,
                if (unifiedPointerInput) 1 else 0,
            )
        val writer =
            if (experimental) {
                val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
                val profile =
                    Profile(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        profileMask,
                        RcPlatformServices.None,
                    ) { displayInfo, p, _ ->
                        RemoteComposeWriter(
                            p,
                            RemoteComposeBuffer(),
                            RemoteComposeWriter.hTag(Header.DOC_WIDTH, displayInfo.width),
                            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, displayInfo.height),
                            RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                            unifiedPointerInputTag,
                        )
                    }
                RemoteComposeWriter(
                    profile,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                    unifiedPointerInputTag,
                )
            } else {
                RemoteComposeWriter(
                    RcPlatformProfiles.ANDROIDX,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                    unifiedPointerInputTag,
                )
            }
        writer.buildDocument()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        val player = RemoteComposePlayer(activity)
        player.setDocument(writer.encodeToByteArray())
        parent.addView(player, FrameLayout.LayoutParams(width, height))
        activity.setContentView(parent)

        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)
        parent.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
        return player
    }

    private fun dispatchTap(view: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        val upTime = downTime + 10L
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    private fun dispatchCancel(view: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        val cancelTime = downTime + 10L
        val cancelEvent =
            MotionEvent.obtain(downTime, cancelTime, MotionEvent.ACTION_CANCEL, x, y, 0)
        view.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }

    private fun dispatchSwipe(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 5,
    ) {
        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val downEvent =
            MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        for (step in 1..steps) {
            eventTime += 20L
            val x = startX + (endX - startX) * step / steps
            val y = startY + (endY - startY) * step / steps
            val moveEvent =
                MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0)
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        }

        eventTime += 20L
        val upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, endX, endY, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
