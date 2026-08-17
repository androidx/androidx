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

package androidx.a2ui.model.processor

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiProtocolConstants
import androidx.a2ui.model.protocol.A2uiServerToClientMessage
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiMessageProcessorTest {

    @Test
    fun processInput_validMessage_callsProcessMessage() {
        val processor = TestMessageProcessor()
        val mockMessage = A2uiCreateSurfaceMessage("surface_1", "catalog_1")
        val parser = A2uiMessageParser<String> { mockMessage }

        processor.processInput(parser, "inputData")

        assertThat(processor.processedMessages).containsExactly(mockMessage)
        assertThat(processor.processedErrors).isEmpty()
    }

    @Test
    fun processInput_validationException_callsProcessErrorWithValidationCodeAndPath() {
        val processor = TestMessageProcessor()
        val parser =
            A2uiMessageParser<String> {
                throw A2uiException.A2uiValidationException(
                    "Missing required property",
                    "/components/0",
                )
            }

        processor.processInput(parser, "invalidInput")

        assertThat(processor.processedMessages).isEmpty()
        assertThat(processor.processedErrors).hasSize(1)

        val error = processor.processedErrors[0]
        assertThat(error.code).isEqualTo("VALIDATION_FAILED")
        assertThat(error.surfaceId).isEqualTo(A2uiProtocolConstants.GLOBAL_SURFACE_ID)
        assertThat(error.message).isEqualTo("Missing required property")
        assertThat(error.context).isEqualTo(mapOf("path" to "/components/0"))
    }

    @Test
    fun processInput_runtimeException_callsProcessErrorWithRuntimeExceptionDetails() {
        val processor = TestMessageProcessor()
        val customContext = mapOf("reason" to "Evaluator failure")
        val parser =
            A2uiMessageParser<String> {
                throw A2uiException.A2uiRuntimeException("Runtime execution failed", customContext)
            }

        processor.processInput(parser, "inputData")

        assertThat(processor.processedMessages).isEmpty()
        assertThat(processor.processedErrors).hasSize(1)

        val error = processor.processedErrors[0]
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.surfaceId).isEqualTo(A2uiProtocolConstants.GLOBAL_SURFACE_ID)
        assertThat(error.message).isEqualTo("Runtime execution failed")
        assertThat(error.context).isEqualTo(customContext)
    }

    @Test
    fun processInput_exceptionWithSurfaceIdInContext_usesContextSurfaceId() {
        val processor = TestMessageProcessor()
        val parser =
            A2uiMessageParser<String> {
                throw A2uiException.A2uiRuntimeException(
                    message = "Surface specific failure",
                    context = mapOf("surfaceId" to "surface_42"),
                )
            }

        processor.processInput(parser, "inputData")

        assertThat(processor.processedErrors).hasSize(1)
        val error = processor.processedErrors[0]
        assertThat(error.surfaceId).isEqualTo("surface_42")
    }

    @Test
    fun processInput_unexpectedException_rethrowsWithoutCallingProcessError() {
        val processor = TestMessageProcessor()
        val parser =
            A2uiMessageParser<String> {
                throw NullPointerException("Unexpected internal parser bug")
            }

        val exception =
            assertFailsWith<NullPointerException> { processor.processInput(parser, "inputData") }

        assertThat(exception).hasMessageThat().contains("Unexpected internal parser bug")
        assertThat(processor.processedMessages).isEmpty()
        assertThat(processor.processedErrors).isEmpty()
    }

    private class TestMessageProcessor : A2uiMessageProcessor {
        val processedMessages = mutableListOf<A2uiServerToClientMessage>()
        val processedErrors = mutableListOf<A2uiClientErrorMessage>()

        override val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> =
            MutableStateFlow(emptyList())
        override val outboundEvents: Flow<A2uiClientToServerMessage> =
            MutableSharedFlow<A2uiClientToServerMessage>().asSharedFlow()

        override fun processMessage(message: A2uiServerToClientMessage) {
            processedMessages.add(message)
        }

        override fun processError(error: A2uiClientErrorMessage) {
            processedErrors.add(error)
        }

        override suspend fun collectMessages() {}
    }
}
