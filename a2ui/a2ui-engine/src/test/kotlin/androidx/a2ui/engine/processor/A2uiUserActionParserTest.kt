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

package androidx.a2ui.engine.processor

import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.protocol.A2uiUserAction
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiUserActionParserTest {

    companion object {
        private const val TEST_SURFACE_ID = "surface-123"
        private const val TEST_COMPONENT_ID = "btn-abc"
        private const val TEST_TIMESTAMP = 1672531200000L // arbitrary epoch millis
    }

    @Test
    fun fromPayload_eventAction_allFieldsProvided_parsesCorrectly() {
        val payload =
            mapOf("event" to mapOf("name" to "my_event", "context" to mapOf("x" to 10, "y" to 20)))

        val action =
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)

        assertThat(action).isInstanceOf(A2uiEventAction::class.java)
        action as A2uiEventAction
        assertThat(action.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(action.componentId).isEqualTo(TEST_COMPONENT_ID)
        assertThat(action.timestamp).isEqualTo(TEST_TIMESTAMP)
        assertThat(action.eventName).isEqualTo("my_event")
        assertThat(action.context).isEqualTo(mapOf("x" to 10, "y" to 20))
    }

    @Test
    fun fromPayload_eventAction_minimalFieldsProvided_usesDefaults() {
        val payload = mapOf("event" to mapOf("name" to "simple_click"))

        val action =
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)

        assertThat(action).isInstanceOf(A2uiEventAction::class.java)
        action as A2uiEventAction
        assertThat(action.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(action.componentId).isEqualTo(TEST_COMPONENT_ID)
        assertThat(action.timestamp).isEqualTo(TEST_TIMESTAMP)
        assertThat(action.eventName).isEqualTo("simple_click")
        assertThat(action.context).isEmpty()
    }

    @Test
    fun fromPayload_functionCallAction_allFieldsProvided_parsesCorrectly() {
        val payload =
            mapOf(
                "functionCall" to
                    mapOf(
                        "call" to "doSomethingLocal",
                        "args" to mapOf("argA" to "valueA", "argB" to 42),
                    )
            )

        val action =
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)

        assertThat(action).isInstanceOf(A2uiFunctionCallAction::class.java)
        action as A2uiFunctionCallAction
        assertThat(action.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(action.componentId).isEqualTo(TEST_COMPONENT_ID)
        assertThat(action.timestamp).isEqualTo(TEST_TIMESTAMP)
        assertThat(action.functionName).isEqualTo("doSomethingLocal")
        assertThat(action.args).isEqualTo(mapOf("argA" to "valueA", "argB" to 42))
    }

    @Test
    fun fromPayload_functionCallAction_minimalFieldsProvided_usesDefaults() {
        val payload = mapOf("functionCall" to mapOf("call" to "pingLocal"))

        val action =
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)

        assertThat(action).isInstanceOf(A2uiFunctionCallAction::class.java)
        action as A2uiFunctionCallAction
        assertThat(action.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(action.componentId).isEqualTo(TEST_COMPONENT_ID)
        assertThat(action.timestamp).isEqualTo(TEST_TIMESTAMP)
        assertThat(action.functionName).isEqualTo("pingLocal")
        assertThat(action.args).isEmpty()
    }

    @Test
    fun fromPayload_invalidPayload_missingEventName_throwsException() {
        val payload =
            mapOf(
                "event" to mapOf<String, Any?>() // Missing "name"
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                A2uiUserAction.fromPayload(
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    payload,
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Event payload is missing a valid String 'name'.")
    }

    @Test
    fun fromPayload_invalidPayload_missingFunctionName_throwsException() {
        val payload =
            mapOf(
                "functionCall" to mapOf<String, Any?>() // Missing "call"
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                A2uiUserAction.fromPayload(
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    payload,
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("FunctionCall payload is missing a valid String 'call'.")
    }

    @Test
    fun fromPayload_invalidPayload_unknownType_throwsException() {
        val payload = mapOf("unknown_type" to mapOf("someKey" to "someValue"))

        val exception =
            assertFailsWith<IllegalStateException> {
                A2uiUserAction.fromPayload(
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    payload,
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Action payload failed to match either 'event' or 'functionCall'")
    }

    @Test
    fun fromPayload_invalidPayload_eventIsNotMap_throwsException() {
        val payload = mapOf("event" to "not_a_map")

        assertFailsWith<IllegalStateException> {
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)
        }
    }

    @Test
    fun fromPayload_invalidPayload_functionCallIsNotMap_throwsException() {
        val payload = mapOf("functionCall" to listOf("not", "a", "map"))

        assertFailsWith<IllegalStateException> {
            A2uiUserAction.fromPayload(TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP, payload)
        }
    }
}
