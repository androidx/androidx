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
import androidx.compose.remote.core.RemoteContext.ContextMode
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.events.Event
import androidx.compose.remote.core.events.EventHandler
import androidx.compose.remote.core.events.EventManager
import androidx.compose.remote.core.events.EventRouter
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class EventActionOperationRegistrationTest {

    @Test
    fun `apply in DATA mode registers handler with matching EventRouters`() {
        // Arrange.
        val eventManager = EventManager()
        val router = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_1, router)

        val context = RegistrationRemoteContext(ContextMode.DATA, eventManager)
        val eventActionOperation = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)

        // Act.
        eventActionOperation.apply(context)

        // Assert.
        assertThat(router.registeredHandlers).containsExactly(eventActionOperation)
    }

    @Test
    fun `apply in DATA mode does not register with non-matching EventRouters`() {
        // Arrange.
        val eventManager = EventManager()
        val router = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_2, router)

        val context = RegistrationRemoteContext(ContextMode.DATA, eventManager)
        val eventActionOperation = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)

        // Act.
        eventActionOperation.apply(context)

        // Assert.
        assertThat(router.registeredHandlers).isEmpty()
    }

    @Test
    fun `apply in PAINT mode does not register handler`() {
        // Arrange.
        val eventManager = EventManager()
        val router = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_1, router)

        val context = RegistrationRemoteContext(ContextMode.PAINT, eventManager)
        val eventActionOperation = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)

        // Act.
        eventActionOperation.apply(context)

        // Assert.
        assertThat(router.registeredHandlers).isEmpty()
    }

    @Test
    fun `apply in UNSET mode does not register handler`() {
        // Arrange.
        val eventManager = EventManager()
        val router = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_1, router)

        val context = RegistrationRemoteContext(ContextMode.UNSET, eventManager)
        val eventActionOperation = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)

        // Act.
        eventActionOperation.apply(context)

        // Assert.
        assertThat(router.registeredHandlers).isEmpty()
    }

    @Test
    fun `apply with multiple operations of same event type registers all with matching EventRouter`() {
        // Arrange.
        val eventManager = EventManager()
        val router = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_1, router)

        val context = RegistrationRemoteContext(ContextMode.DATA, eventManager)
        val eventActionOperation1 = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)
        val eventActionOperation2 = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)

        // Act.
        eventActionOperation1.apply(context)
        eventActionOperation2.apply(context)

        // Assert.
        assertThat(router.registeredHandlers)
            .containsExactly(eventActionOperation1, eventActionOperation2)
            .inOrder()
    }

    @Test
    fun `apply with multiple operations of different event types registers each with correct matching EventRouter`() {
        // Arrange.
        val eventManager = EventManager()
        val router1 = TestEventRouter()
        val router2 = TestEventRouter()
        eventManager.registerRouter(EVENT_TYPE_1, router1)
        eventManager.registerRouter(EVENT_TYPE_2, router2)

        val context = RegistrationRemoteContext(ContextMode.DATA, eventManager)
        val eventActionOperation1 = EventActionOperation(EVENT_TYPE_1, 0, 0, null, null)
        val eventActionOperation2 = EventActionOperation(EVENT_TYPE_2, 0, 0, null, null)

        // Act.
        eventActionOperation1.apply(context)
        eventActionOperation2.apply(context)

        // Assert.
        assertThat(router1.registeredHandlers).containsExactly(eventActionOperation1)
        assertThat(router2.registeredHandlers).containsExactly(eventActionOperation2)
    }

    companion object {
        const val EVENT_TYPE_1 = 100
        const val EVENT_TYPE_2 = 200
    }

    private class TestEventRouter : EventRouter {
        val registeredHandlers = mutableListOf<EventHandler>()

        override fun registerHandler(eventHandler: EventHandler) {
            registeredHandlers.add(eventHandler)
        }

        override fun routeEvent(context: RemoteContext, document: CoreDocument, event: Event): Int {
            return EventManager.STATUS_UNHANDLED
        }
    }

    private class RegistrationRemoteContext(
        private val mode: ContextMode,
        private val eventManager: EventManager,
    ) : RemoteContext() {
        override fun getMode(): ContextMode = mode

        override fun getEventManager(): EventManager = eventManager

        override fun loadFloat(id: Int, value: Float) {}

        override fun overrideFloat(id: Int, value: Float) {}

        override fun getFloat(id: Int): Float = 0.0f

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
