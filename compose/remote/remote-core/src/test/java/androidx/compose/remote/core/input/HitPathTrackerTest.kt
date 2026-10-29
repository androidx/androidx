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

package androidx.compose.remote.core.input

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.events.Event
import androidx.compose.remote.core.events.EventHandler
import androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_INITIAL
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NONE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.remote.core.serialize.MapSerializer
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * The hit paths that the [HitPathTracker] keeps for several pointers, seen through the real
 * [PointerInputRouter]: which components get an event, and in which order.
 *
 * The player sends one pointer today. These tests send several pointer ids to the router directly.
 */
class HitPathTrackerTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    /** The names of the components that got the event in the initial pass, in order. */
    private val initialPass = mutableListOf<String>()

    /** The names of the components that got the event in the main pass, in order. */
    private val mainPass = mutableListOf<String>()

    @Test
    fun `second pointer on another child - the shared parent gets each event once`() {
        // Arrange - Root(200x100) > parent Box(200x100) > [left Box(100x100), right Box(100x100)].
        // The boxes are side by side. Pointer 0 is on the left box, pointer 1 on the right box.
        val root =
            RootLayoutComponent(0, 0f, 0f, 200f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 200f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("parent"))
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("left"))
                                    },
                                )
                                addChild(
                                    context,
                                    BoxLayout(null, 3, 0, 100f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("right"))
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 1),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 150f, y = 50f, pointerId = 1),
        )

        // Assert - the left box is under pointer 0 only, so it does not get the event of pointer 1.
        assertThat(initialPass).containsExactly("parent", "right").inOrder()
        assertThat(mainPass).containsExactly("right", "parent").inOrder()
    }

    @Test
    fun `pointer down outside the root - no component gets its events`() {
        // Arrange - Root(100x100) > Box(100x100). Pointer 0 is on the box. Pointer 1 goes down
        // outside the root.
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("box"))
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 0),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        val statuses =
            listOf(
                router.routeEvent(
                    context,
                    document,
                    pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 1),
                ),
                router.routeEvent(
                    context,
                    document,
                    pointerEvent(POINTER_SUBTYPE_MOVE, x = 160f, y = 50f, pointerId = 1),
                ),
                router.routeEvent(
                    context,
                    document,
                    pointerEvent(POINTER_SUBTYPE_UP, x = 160f, y = 50f, pointerId = 1),
                ),
            )

        // Assert.
        assertThat(initialPass).isEmpty()
        assertThat(mainPass).isEmpty()
        assertThat(statuses)
            .containsExactly(POINTER_STATUS_NONE, POINTER_STATUS_NONE, POINTER_STATUS_NONE)
    }

    @Test
    fun `pointer up removes its own branch and keeps the other one`() {
        // Arrange - Root(200x100) > parent Box(200x100) > [left Box(100x100), right Box(100x100)].
        // The boxes are side by side. Pointer 0 was on the left box, pointer 1 is on the right box.
        val root =
            RootLayoutComponent(0, 0f, 0f, 200f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 200f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("parent"))
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("left"))
                                    },
                                )
                                addChild(
                                    context,
                                    BoxLayout(null, 3, 0, 100f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("right"))
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 1),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 50f, pointerId = 0),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 150f, y = 50f, pointerId = 1),
        )

        // Assert.
        assertThat(initialPass).containsExactly("parent", "right").inOrder()
        assertThat(mainPass).containsExactly("right", "parent").inOrder()
    }

    @Test
    fun `two pointers on one child - the child stays while one of them is down`() {
        // Arrange - Root(100x100) > parent Box(100x100) > child Box(100x100).
        // Pointers 0 and 1 went down on the child, and pointer 0 is up again.
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("parent"))
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("child"))
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 40f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 60f, y = 50f, pointerId = 1),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_UP, x = 40f, y = 50f, pointerId = 0),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 60f, y = 50f, pointerId = 1),
        )

        // Assert.
        assertThat(initialPass).containsExactly("parent", "child").inOrder()
        assertThat(mainPass).containsExactly("child", "parent").inOrder()
    }

    @Test
    fun `two pointers on one child - the child is removed when both are up`() {
        // Arrange - Root(200x100) > parent Box(200x100) > [left Box(100x100), right Box(100x100)].
        // The boxes are side by side. Pointers 0 and 1 went down on the left box, and both are up
        // again.
        val root =
            RootLayoutComponent(0, 0f, 0f, 200f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 200f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("parent"))
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("left"))
                                    },
                                )
                                addChild(
                                    context,
                                    BoxLayout(null, 3, 0, 100f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("right"))
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 40f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 60f, y = 50f, pointerId = 1),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_UP, x = 40f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_UP, x = 60f, y = 50f, pointerId = 1),
        )
        initialPass.clear()
        mainPass.clear()

        // Act - A new pointer goes down on the right box.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 0),
        )

        // Assert.
        assertThat(initialPass).containsExactly("parent", "right").inOrder()
        assertThat(mainPass).containsExactly("right", "parent").inOrder()
    }

    @Test
    fun `pointers at different depths - a child that is hit again gets each event once`() {
        // Arrange - Root(200x100) > parent Box(200x100) > [left Box(100x100) > inner Box(100x100),
        // right Box(100x100)]. The left and the right box are side by side. Pointer 0 is on the
        // right box, pointer 1 on the inner box, and pointer 2 on the right box again.
        val root =
            RootLayoutComponent(0, 0f, 0f, 200f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 200f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("parent"))
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(
                                            LayoutComponentContent(-1).apply {
                                                addChild(
                                                    context,
                                                    BoxLayout(null, 3, 0, 0f, 0f, 100f, 100f, 0, 0),
                                                )
                                            }
                                        )
                                    },
                                )
                                addChild(
                                    context,
                                    BoxLayout(null, 4, 0, 100f, 0f, 100f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("right"))
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 1),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 160f, y = 50f, pointerId = 2),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 150f, y = 50f, pointerId = 0),
        )

        // Assert.
        assertThat(initialPass).containsExactly("parent", "right").inOrder()
        assertThat(mainPass).containsExactly("right", "parent").inOrder()
    }

    @Test
    fun `child added while another branch is down - it gets each event once when hit again`() {
        // Arrange - Root(300x100) > parent Box(300x100) > [left Box(200x100) > [inner 1
        // Box(100x100), inner 2 Box(100x100)], right Box(100x100)]. The boxes with the same parent
        // are side by side. Pointer 0 is on inner 1, pointer 1 on the right box, and pointers 2
        // and 3 on inner 2.
        val root =
            RootLayoutComponent(0, 0f, 0f, 300f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 300f, 100f, 0, 0).apply {
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 200f, 100f, 0, 0).apply {
                                        mList.add(EventRecorder("left"))
                                        mList.add(
                                            LayoutComponentContent(-1).apply {
                                                addChild(
                                                    context,
                                                    BoxLayout(null, 3, 0, 0f, 0f, 100f, 100f, 0, 0),
                                                )
                                                addChild(
                                                    context,
                                                    BoxLayout(
                                                            null,
                                                            4,
                                                            0,
                                                            100f,
                                                            0f,
                                                            100f,
                                                            100f,
                                                            0,
                                                            0,
                                                        )
                                                        .apply {
                                                            mList.add(EventRecorder("inner 2"))
                                                        },
                                                )
                                            }
                                        )
                                    },
                                )
                                addChild(context, BoxLayout(null, 5, 0, 200f, 0f, 100f, 100f, 0, 0))
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 0),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 250f, y = 50f, pointerId = 1),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 150f, y = 50f, pointerId = 2),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 160f, y = 50f, pointerId = 3),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 150f, y = 50f, pointerId = 2),
        )

        // Assert.
        assertThat(initialPass).containsExactly("left", "inner 2").inOrder()
        assertThat(mainPass).containsExactly("inner 2", "left").inOrder()
    }

    @Test
    fun `path of twenty components - every component gets the event in order`() {
        // Arrange - Root(100x100) > 20 Boxes(100x100), each one inside the one before it. The
        // path is longer than the room that the tracker starts with.
        var box: BoxLayout =
            BoxLayout(null, 20, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                mList.add(EventRecorder("box 20"))
            }
        for (i in 19 downTo 1) {
            val inner = box
            box =
                BoxLayout(null, i, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                    mList.add(EventRecorder("box $i"))
                    mList.add(LayoutComponentContent(-1).apply { addChild(context, inner) })
                }
        }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply { addChild(context, box) }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f))

        // Assert.
        val outerToInner = (1..20).map { "box $it" }
        assertThat(initialPass).containsExactlyElementsIn(outerToInner).inOrder()
        assertThat(mainPass).containsExactlyElementsIn(outerToInner.reversed()).inOrder()
    }

    @Test
    fun `pointer id 31 and another pointer on one component - it gets each event once`() {
        // Arrange - Root(100x100) > Box(100x100). 31 is the largest pointer id.
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(EventRecorder("box"))
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 40f, y = 50f, pointerId = 31),
        )
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_DOWN, x = 60f, y = 50f, pointerId = 0),
        )
        initialPass.clear()
        mainPass.clear()

        // Act.
        router.routeEvent(
            context,
            document,
            pointerEvent(POINTER_SUBTYPE_MOVE, x = 40f, y = 50f, pointerId = 31),
        )

        // Assert.
        assertThat(initialPass).containsExactly("box")
        assertThat(mainPass).containsExactly("box")
    }

    @Test
    fun `pointer id above 31 is rejected`() {
        // Arrange - Root(100x100).
        val root = RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0)
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        val event = pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = 32)

        // Act and assert.
        assertFailsWith<IllegalArgumentException> { router.routeEvent(context, document, event) }
    }

    @Test
    fun `negative pointer id is rejected`() {
        // Arrange - Root(100x100).
        val root = RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0)
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        val event = pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 50f, pointerId = -1)

        // Act and assert.
        assertFailsWith<IllegalArgumentException> { router.routeEvent(context, document, event) }
    }

    /**
     * A modifier that adds its [name] to [initialPass] or [mainPass] when it gets a pointer event
     * in that pass.
     */
    private inner class EventRecorder(private val name: String) :
        Operation(), ModifierOperation, EventHandler {

        override fun matchesEvent(eventType: Int): Boolean = eventType == EVENT_TYPE_POINTER

        override fun onEvent(context: RemoteContext, document: CoreDocument, event: Event): Int {
            when (MetadataUtil.unpackPass(event.metadata)) {
                POINTER_PASS_INITIAL -> initialPass.add(name)
                POINTER_PASS_MAIN -> mainPass.add(name)
            }
            return POINTER_STATUS_NOT_CONSUMED
        }

        override fun serializeToString(indent: Int, serializer: StringSerializer) {}

        override fun serialize(serializer: MapSerializer) {}

        override fun write(buffer: WireBuffer) {}

        override fun apply(context: RemoteContext) {}

        override fun deepToString(indent: String): String = "EventRecorder"
    }
}
