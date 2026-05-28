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

package androidx.a2ui.core.protocol

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class A2uiClientToServerMessageTest {

    @Test
    fun a2uiUserAction_allArgumentsProvided_allPropertiesAreStored() {
        val full =
            A2uiUserAction(
                TEST_ACTION_TYPE,
                TEST_SURFACE_ID,
                TEST_COMPONENT_ID,
                TEST_TIMESTAMP,
                TEST_CONTEXT,
            )

        assertThat(full.type).isEqualTo(TEST_ACTION_TYPE)
        assertThat(full.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(full.componentId).isEqualTo(TEST_COMPONENT_ID)
        assertThat(full.timestamp).isEqualTo(TEST_TIMESTAMP)
        assertThat(full.context).isEqualTo(TEST_CONTEXT)
    }

    @Test
    fun a2uiUserAction_onlyRequiredArgumentsProvided_contextIsEmpty() {
        val minimal =
            A2uiUserAction(TEST_ACTION_TYPE, TEST_SURFACE_ID, TEST_COMPONENT_ID, TEST_TIMESTAMP)

        assertThat(minimal.context).isEmpty()
    }

    @Test
    fun a2uiUserAction_instanceCreated_isInstanceOfClientToServerMessage() {
        val action =
            A2uiUserAction(
                TEST_ACTION_TYPE,
                TEST_SURFACE_ID,
                TEST_COMPONENT_ID,
                TEST_TIMESTAMP,
                TEST_CONTEXT,
            )

        assertThat(action).isInstanceOf(A2uiClientToServerMessage::class.java)
    }

    @Test
    fun a2uiUserAction_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT,
                ),
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT,
                ),
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE_2,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT,
                )
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID_2,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT,
                )
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID_2,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT,
                )
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP_2,
                    TEST_CONTEXT,
                )
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    TEST_CONTEXT_2,
                )
            )
            .addEqualityGroup(
                A2uiUserAction(
                    TEST_ACTION_TYPE,
                    TEST_SURFACE_ID,
                    TEST_COMPONENT_ID,
                    TEST_TIMESTAMP,
                    emptyMap(),
                )
            )
            .testEquals()
    }

    @Test
    fun a2uiUserAction_toString_returnsExpectedFormat() {
        val action =
            A2uiUserAction(
                TEST_ACTION_TYPE,
                TEST_SURFACE_ID,
                TEST_COMPONENT_ID,
                TEST_TIMESTAMP,
                TEST_CONTEXT,
            )

        assertThat(action.toString())
            .isEqualTo(
                "A2uiUserAction(type=$TEST_ACTION_TYPE, surfaceId=$TEST_SURFACE_ID, " +
                    "componentId=$TEST_COMPONENT_ID, timestamp=$TEST_TIMESTAMP_STR, context=$TEST_CONTEXT)"
            )
    }

    @Test
    fun a2uiClientError_allArgumentsProvided_allPropertiesAreStored() {
        val full = A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT)

        assertThat(full.code).isEqualTo(TEST_ERROR_CODE)
        assertThat(full.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(full.message).isEqualTo(TEST_MESSAGE)
        assertThat(full.context).isEqualTo(TEST_CONTEXT)
    }

    @Test
    fun a2uiClientError_onlyRequiredArgumentsProvided_contextIsEmpty() {
        val minimal = A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE)

        assertThat(minimal.context).isEmpty()
    }

    @Test
    fun a2uiClientError_validationError_withExactlyPath_instantiatesCorrectly() {
        val validErr =
            A2uiClientError(
                "VALIDATION_FAILED",
                TEST_SURFACE_ID,
                TEST_MESSAGE,
                mapOf("path" to TEST_PATH),
            )

        assertThat(validErr.context).isEqualTo(mapOf("path" to TEST_PATH))
    }

    @Test
    fun a2uiClientError_validationError_withoutPath_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            A2uiClientError("VALIDATION_FAILED", TEST_SURFACE_ID, TEST_MESSAGE)
        }
    }

    @Test
    fun a2uiClientError_validationError_withExtraContext_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            A2uiClientError(
                "VALIDATION_FAILED",
                TEST_SURFACE_ID,
                TEST_MESSAGE,
                mapOf("path" to TEST_PATH, "extra" to "value"),
            )
        }
    }

    @Test
    fun a2uiClientError_validationError_withNonStringPath_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            A2uiClientError(
                "VALIDATION_FAILED",
                TEST_SURFACE_ID,
                TEST_MESSAGE,
                mapOf("path" to 123),
            )
        }
    }

    @Test
    fun a2uiClientError_instanceCreated_isInstanceOfClientToServerMessage() {
        val error = A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT)

        assertThat(error).isInstanceOf(A2uiClientToServerMessage::class.java)
    }

    @Test
    fun a2uiClientError_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT),
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT),
            )
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE_2, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT)
            )
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID_2, TEST_MESSAGE, TEST_CONTEXT)
            )
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE_2, TEST_CONTEXT)
            )
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT_2)
            )
            .addEqualityGroup(
                A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, emptyMap())
            )
            .testEquals()
    }

    @Test
    fun a2uiClientError_toString_returnsExpectedFormat() {
        val error = A2uiClientError(TEST_ERROR_CODE, TEST_SURFACE_ID, TEST_MESSAGE, TEST_CONTEXT)

        assertThat(error.toString())
            .isEqualTo(
                "A2uiClientError(code=$TEST_ERROR_CODE, surfaceId=$TEST_SURFACE_ID, " +
                    "message=$TEST_MESSAGE, context=$TEST_CONTEXT)"
            )
    }

    companion object {
        private const val TEST_SURFACE_ID = "test-surface"
        private const val TEST_SURFACE_ID_2 = "test-surface-2"
        private const val TEST_COMPONENT_ID = "test-component"
        private const val TEST_COMPONENT_ID_2 = "test-component-2"
        private const val TEST_ACTION_TYPE = "test-action"
        private const val TEST_ACTION_TYPE_2 = "test-action-2"
        private const val TEST_TIMESTAMP = 1778595420000L
        private const val TEST_TIMESTAMP_STR = "2026-05-12T14:17:00.000Z"
        private const val TEST_TIMESTAMP_2 = 1778599020000L
        private const val TEST_PATH = "/test/path"
        private const val TEST_MESSAGE = "test-message"
        private const val TEST_MESSAGE_2 = "test-message-2"
        private const val TEST_ERROR_CODE = "TEST_ERROR"
        private const val TEST_ERROR_CODE_2 = "TEST_ERROR_ALT"

        private val TEST_CONTEXT = mapOf("context-prop" to "context-value")
        private val TEST_CONTEXT_2 = mapOf("context-prop-2" to "context-value-2")
    }
}
