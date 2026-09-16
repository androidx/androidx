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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.events.Event
import androidx.compose.remote.core.events.EventManager
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.IntegerConstant
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class EventActionOperationTest {

    @Test
    fun `matchEvent returns false for wrong event type`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_2)

        // Assert.
        assertThat(matchesEvent).isFalse()
    }

    @Test
    fun `match event returns true when event type matches`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `filter ignored for match event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 123,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `flags don't affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 123,
                /* dataIds = */ null,
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying empty data ids don't affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(),
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying uninitialized data ids don't affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(12, 13, 14),
                /* condition = */ null,
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying empty condition does not affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(),
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying false condition does not affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(FALSE),
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `specifying true condition does not affect matches event`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ 0,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val matchesEvent = eventAction.matchesEvent(EVENT_TYPE_1)

        // Assert.
        assertThat(matchesEvent).isTrue()
    }

    @Test
    fun `onEvent doesn't handle events that don't match the filter`() {
        // Arrange.
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val result =
            eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(0),
                /* condition = */ null,
            )

        // Act.
        eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(0, DATA_ID_1, 0, DATA_ID_2),
                /* condition = */ null,
            )

        // Act.
        eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2, DATA_ID_3),
                /* condition = */ null,
            )

        // Act.
        eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ null,
            )

        // Act.
        eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ floatArrayOf(FALSE),
            )

        // Act.
        val result =
            eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ intArrayOf(DATA_ID_1, DATA_ID_2),
                /* condition = */ floatArrayOf(TRUE),
            )

        // Act.
        val result =
            eventAction.onEvent(
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
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventAction.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        val result =
            eventAction.onEvent(
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
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        remoteContext.loadFloat(DATA_ID_4, 10f)
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventAction.list.apply {
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(ValueFloatChangeActionOperation(DATA_ID_3, 40f))
        }

        // Act.
        val result =
            eventAction.onEvent(
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
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventAction.list.apply {
            add(IntegerConstant(500, 10))
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(IntegerConstant(501, 10))
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(IntegerConstant(502, 10))
        }

        // Act.
        val result =
            eventAction.onEvent(
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
    fun `onEvent ignores nested Event Action Operations`() {
        // Arrange.
        val remoteContext = TestRemoteContext()
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        remoteContext.loadFloat(DATA_ID_2, 10f)
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(TRUE),
            )
        eventAction.list.apply {
            add(
                EventActionOperation(
                    EVENT_TYPE_1,
                    FILTER_1,
                    ROUTER_FLAGS,
                    intArrayOf(DATA_ID_5),
                    floatArrayOf(TRUE),
                )
            )
            add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))
            add(
                EventActionOperation(
                    EVENT_TYPE_2,
                    FILTER_2,
                    ROUTER_FLAGS,
                    intArrayOf(DATA_ID_6),
                    floatArrayOf(TRUE),
                )
            )
            add(ValueFloatChangeActionOperation(DATA_ID_2, 30f))
            add(
                EventActionOperation(
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
            eventAction.onEvent(
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
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ 0,
                /* dataIds = */ intArrayOf(DATA_ID_1),
                /* condition = */ null,
            )

        // Act.
        val result =
            eventAction.onEvent(
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
        remoteContext.mLastComponent = BoxLayout(null, 0, 0, 0f, 0f, 0f, 0f, 0, 0)
        remoteContext.loadFloat(DATA_ID_1, 10f)
        val eventAction =
            EventActionOperation(
                /* type = */ EVENT_TYPE_1,
                /* filter = */ FILTER_1,
                /* flags = */ ROUTER_FLAGS,
                /* dataIds = */ null,
                /* condition = */ floatArrayOf(2.5f),
            )
        eventAction.list.add(ValueFloatChangeActionOperation(DATA_ID_1, 20f))

        // Act.
        val result =
            eventAction.onEvent(
                remoteContext,
                CoreDocument(),
                Event(/* type= */ EVENT_TYPE_1, /* metadata= */ FILTER_1, /* data= */ null),
            )

        // Assert.
        assertThat(remoteContext.getFloat(DATA_ID_1)).isEqualTo(20f)
        assertThat(result).isEqualTo(ROUTER_FLAGS)
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
    }

    private class TestRemoteContext : RemoteContext() {

        // TODO(b/555326918): Using HashMap instead of IntFloatMap.
        val floatMap = HashMap<Int, Float>()

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

        override fun loadText(id: Int, text: String) {}

        override fun getText(id: Int): String? = null

        override fun loadInteger(id: Int, value: Int) {}

        override fun overrideInteger(id: Int, value: Int) {}

        override fun overrideText(id: Int, valueId: Int) {}

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
