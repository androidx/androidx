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

package androidx.compose.remote.core.operations.layout.modifiers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.events.Event
import androidx.compose.remote.core.events.EventHandler
import androidx.compose.remote.core.events.EventManager
import androidx.compose.remote.core.events.EventRouter
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.IntegerConstant
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class EventHandlerModifierOperationTest {

    @Test
    fun `matchEvent returns false for wrong event type`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_2)

        // Assert.
        assertThat(matchesEvent).isFalse()
    }

    @Test
    fun `match event returns true when event type matches`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `filter ignored for match event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 123,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `flags don't affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 123,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying empty data ids don't affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(),
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying uninitialized data ids don't affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(12, 13, 14),
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying empty condition does not affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(),
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying false condition does not affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(FALSE),
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying true condition does not affect matches event`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val matchesEvent = eventHandlerModifier.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `onEvent doesn't handle events that don't match the filter`() {
        // Arrange.
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                TestRemoteContext(),
                CoreDocument(),
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_2, /* data= */ null),
            )

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED)
    }

    @Test
    fun `onEvent doesn't write data when data id is 0`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(0),
                /* condition = */ null,
            )

        // Act.
        eventHandlerModifier.onEvent(
            remoteContext,
            CoreDocument(),
            Event(
                /* type = */ EVENT_TYPE_1,
                /* metadata = */ FILTER_1,
                /* data = */ floatArrayOf(123f),
            ),
        )

        // Assert.
        assertThat(remoteContext.floatMap).doesNotContainKey(0)
    }

    @Test
    fun `onEvent doesn't write data only for data id = 0`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(0, DATA_ID_1, 0, DATA_ID_2),
                /* condition = */ null,
            )

        // Act.
        eventHandlerModifier.onEvent(
            remoteContext,
            CoreDocument(),
            Event(
                /* type = */ EVENT_TYPE_1,
                /* metadata = */ FILTER_1,
                /* data = */ floatArrayOf(123f, 456f, 789f, 101f),
            ),
        )

        // Assert.
        assertThat(remoteContext.floatMap).doesNotContainKey(0)
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(456f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(101f)
    }

    @Test
    fun `onEvent doesn't write data if event has less data than specified by the handler`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2, DATA_ID_3),
                /* condition = */ null,
            )

        // Act.
        eventHandlerModifier.onEvent(
            remoteContext,
            CoreDocument(),
            Event(
                /* type = */ EVENT_TYPE_1,
                /* metadata = */ FILTER_1,
                /* data = */ floatArrayOf(123f, 456f),
            ),
        )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(123f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(456f)

        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_3)
    }

    @Test
    fun `onEvent doesn't write data if handler doesn't specify a data id for it`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ null,
            )

        // Act.
        eventHandlerModifier.onEvent(
            remoteContext,
            CoreDocument(),
            Event(
                /* type = */ EVENT_TYPE_1,
                /* metadata = */ FILTER_1,
                /* data = */ floatArrayOf(123f, 456f, 789f),
            ),
        )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(123f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(456f)

        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_3)
    }

    @Test
    fun `onEvent writes data even if condition is false`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ floatArrayOf(FALSE),
            )

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(123f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(456f)
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED)
    }

    @Test
    fun `onEvent writes data when condition is true and returns flag value`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(123f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(456f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent runs action when condition is true and returns flag value`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventHandlerModifier.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent runs all actions in container when condition is true`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        remoteContext.loadFloat(DATA_ID_4, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventHandlerModifier.list.apply {
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(ValueFloatChangeActionOperation(DATA_ID_3, 40f))
        }

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(30f)
        assertThat(remoteContext.getFloat(DATA_ID_3)).isEqualTo(40f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent ignores non action operations`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventHandlerModifier.list.apply {
            add(IntegerConstant(500, 10))
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(IntegerConstant(501, 10))
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(IntegerConstant(502, 10))
        }

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(30f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent ignores nested event handler modifiers`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventHandlerModifier.list.apply {
            add(
                EventHandlerModifierOperation(
                    EVENT_TYPE_1,
                    FILTER_1,
                    ROUTER_FLAGS,
                    intArrayOf(DATA_ID_5),
                    floatArrayOf(TRUE),
                )
            )
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(
                EventHandlerModifierOperation(
                    EVENT_TYPE_2,
                    FILTER_2,
                    ROUTER_FLAGS,
                    intArrayOf(DATA_ID_6),
                    floatArrayOf(TRUE),
                )
            )
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(
                EventHandlerModifierOperation(
                    EVENT_TYPE_1,
                    FILTER_1,
                    ROUTER_FLAGS,
                    intArrayOf(DATA_ID_7),
                    floatArrayOf(TRUE),
                )
            )
        }

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(
                    /* type = */ EVENT_TYPE_1,
                    /* metadata = */ FILTER_1,
                    /* data = */ floatArrayOf(123f, 456f),
                ),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(remoteContext.getFloat(DATA_ID_2)).isEqualTo(30f)
        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_5)
        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_6)
        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_7)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent does not crash when event data is null but dataIds are specified`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1),
                /* condition = */ null,
            )

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_1, /* data= */ null),
            )

        // Assert.
        assertThat(remoteContext.floatMap).doesNotContainKey(DATA_ID_1)
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED)
    }

    @Test
    fun `onEvent treats any non-zero float condition as true`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(2.5f),
            )
        eventHandlerModifier.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_1, /* data= */ null),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `onEvent runs action when mLastComponent is null`() {
        // Arrange.
        val remoteContext = TestRemoteContext().apply { mLastComponent = null }
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventHandlerModifier =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventHandlerModifier.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        val result =
            eventHandlerModifier.onEvent(
                remoteContext,
                CoreDocument(),
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_1, /* data= */ null),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
    }

    @Test
    fun `apply loads nested TextData so it is available when the event fires`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        val eventHandler =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ null,
            )
        // Mirrors what the writer emits for a ValueStringChange(id, "hello") action.
        eventHandler.list.apply {
            add(TextData(TEXT_ID_1, "hello"))
            add(ValueStringChangeActionOperation(TEXT_ID_2, TEXT_ID_1))
        }

        // Act.
        eventHandler.apply(remoteContext)

        // Assert.
        assertThat(remoteContext.getText(TEXT_ID_1)).isEqualTo("hello")
        assertThat(remoteContext.getText(TEXT_ID_2)).isNull()
    }

    @Test
    fun `apply does not run nested actions`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventHandler =
            EventHandlerModifierOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ null,
            )
        eventHandler.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        eventHandler.apply(remoteContext)

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(10f)
    }

    @Test
    fun `document with nested TextData action loads text on initializeContext and runs it on dispatchEvent`() {
        // Arrange - Root > Box carrying an event handler whose action sets a text variable.
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ FILTER_1,
                    /* flags = */ ROUTER_FLAGS,
                    /* dataIds = */ null,
                    /* condition = */ null,
                )
                // What ValueStringChange(TEXT_ID_2, "hello") writes: the string, then the action.
                addText(TEXT_ID_1, "hello")
                addValueStringChangeActionOperation(TEXT_ID_2, TEXT_ID_1)
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }
        val document = CoreDocument().useExperimental()
        document.initFromBuffer(buffer)
        val remoteContext = TestRemoteContext()
        val router = DispatchingEventRouter()
        remoteContext.eventManager.registerRouter(EVENT_TYPE_1, router)

        // Act - DATA pass.
        document.initializeContext(remoteContext)

        // Assert - the nested TextData was applied and the handler reached the router.
        assertThat(remoteContext.getText(TEXT_ID_1)).isEqualTo("hello")
        assertThat(remoteContext.getText(TEXT_ID_2)).isNull()
        assertThat(router.handlers).hasSize(1)

        // Act - dispatch the event. mLastComponent stands in for the component painted last.
        remoteContext.mLastComponent = document.rootLayoutComponent
        val result =
            remoteContext.eventManager.dispatchEvent(
                remoteContext,
                document,
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_1, /* data= */ null),
            )

        // Assert - the action copied the text into the target variable.
        assertThat(result).isEqualTo(ROUTER_FLAGS)
        assertThat(remoteContext.getText(TEXT_ID_2)).isEqualTo("hello")
    }

    private fun RemoteComposeBuffer.useExperimental(): RemoteComposeBuffer = apply {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val map = Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, profileMask)!!
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map.keySet())
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map)
    }

    private fun CoreDocument.useExperimental(): CoreDocument = apply {
        mProfileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
    }

    /** Router forwarding events to every registered handler until one handles it. */
    private class DispatchingEventRouter : EventRouter {
        val handlers = mutableListOf<EventHandler>()

        override fun registerHandler(eventHandler: EventHandler) {
            handlers.add(eventHandler)
        }

        override fun routeEvent(context: RemoteContext, document: CoreDocument, event: Event): Int {
            for (handler in handlers) {
                val status = handler.onEvent(context, document, event)
                if (status != EventManager.STATUS_UNHANDLED) {
                    return status
                }
            }
            return EventManager.STATUS_UNHANDLED
        }
    }

    companion object {
        const val EVENT_TYPE_1 = 100
        const val EVENT_TYPE_2 = 200
        const val FILTER_1 = 300
        const val FILTER_2 = 400

        const val TRUE = 1.0f

        const val FALSE = 0.0f

        const val ROUTER_FLAGS = 345

        const val DATA_ID_1 = 1001
        const val DATA_ID_2 = 1002
        const val DATA_ID_3 = 1003
        const val DATA_ID_4 = 1004
        const val DATA_ID_5 = 1005
        const val DATA_ID_6 = 1006
        const val DATA_ID_7 = 1007

        const val TEXT_ID_1 = 2001
        const val TEXT_ID_2 = 2002
    }

    private class TestRemoteContext : RemoteContext() {

        // TODO(b/555326918): Using HashMap instead of IntFloatMap.
        val floatMap = HashMap<Int, Float>()
        val textMap = HashMap<Int, String>()

        override fun loadFloat(id: Int, value: Float) {
            floatMap[id] = value
        }

        override fun overrideFloat(id: Int, value: Float) {
            floatMap[id] = value
        }

        override fun getFloat(id: Int): Float {
            return floatMap.getValue(id)
        }

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

        override fun getPathData(instanceId: Int): FloatArray? = null

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

        override fun loadColor(id: Int, color: Int) {}

        override fun setNamedColorOverride(colorName: String, color: Int) {}

        override fun setNamedStringOverride(stringName: String, value: String) {}

        override fun clearNamedStringOverride(stringName: String) {}

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

        override fun clearNamedBooleanOverride(booleanName: String) {}

        override fun setNamedIntegerOverride(integerName: String, value: Int) {}

        override fun clearNamedIntegerOverride(integerName: String) {}

        override fun setNamedFloatOverride(floatName: String, value: Float) {}

        override fun clearNamedFloatOverride(floatName: String) {}

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {}

        override fun clearNamedDataOverride(dataName: String) {}

        override fun addCollection(id: Int, collection: ArrayAccess) {}

        override fun putDataMap(id: Int, map: DataMap) {}

        override fun getDataMap(id: Int): DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {}

        override fun getObject(id: Int): Any? = null

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {
            textMap[id] = text
        }

        override fun getText(id: Int): String? = textMap[id]

        override fun loadInteger(id: Int, value: Int) {}

        override fun overrideInteger(id: Int, value: Int) {}

        override fun overrideText(id: Int, valueId: Int) {
            textMap[valueId]?.let { textMap[id] = it }
        }

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

        override fun loadShader(id: Int, value: ShaderData) {}

        override fun getInteger(id: Int): Int = 0

        override fun getLong(id: Int): Long = 0L

        override fun getColor(id: Int): Int = 0

        override fun listensTo(id: Int, variableSupport: VariableSupport) {}

        override fun updateOps(): Int = 0

        override fun getShader(id: Int): ShaderData? = null

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
    }
}
