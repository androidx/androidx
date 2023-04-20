/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service

import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_ACTION_CAPABILITY
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_FULFILLMENT_REQUEST
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_SESSION
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_SESSION_ENDED
import androidx.appactions.interaction.service.testing.internal.FakeAppInteractionService
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc.AppInteractionServiceStub
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Request
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionRequest
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionResponse
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcCleanupRule
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/271929200) Implement tests for the 2 UI related RPCs
@RunWith(AndroidJUnit4::class)
class AppInteractionServiceGrpcImplTest {

    @get:Rule val grpcCleanup = GrpcCleanupRule()

    private val remoteViewsInterceptor: ServerInterceptor = RemoteViewsOverMetadataInterceptor()
    private val testServerName = InProcessServerBuilder.generateName()
    private val testBiiName = "actions.intent.SAMPLE_BII_NAME"
    private val sessionId = "session-123"
    private val capabilityId = "capability-123"
    private val defaultStartSessionRequest =
        StartSessionRequest.newBuilder()
            .setIntentName(testBiiName)
            .setIdentifier(capabilityId)
            .setSessionIdentifier(sessionId)
            .build()
    private val testFulfillmentRequest =
        FulfillmentRequest.newBuilder()
            .addFulfillments(
                Fulfillment.newBuilder().setName(testBiiName).setIdentifier(capabilityId).build()
            )
            .build()
    private val testFulfillmentResponse =
        FulfillmentResponse.newBuilder()
            .setExecutionOutput(
                StructuredOutput.newBuilder()
                    .addOutputValues(OutputValue.newBuilder().setName("bio_arg1")),
            )
            .build()
    private lateinit var capability1: Capability
    private lateinit var appInteractionService: FakeAppInteractionService

    @Before
    fun before() {
        capability1 = mock()
        whenever(capability1.id).thenReturn(capabilityId)
        whenever(capability1.appAction).thenReturn(AppAction.getDefaultInstance())
        val mockCapabilitySession = createMockSession()
        whenever(capability1.createSession(any(), any())).thenReturn(mockCapabilitySession)
        appInteractionService = Robolectric.buildService(
            FakeAppInteractionService::class.java
        ).get()
        appInteractionService.registeredCapabilities = listOf(capability1)
    }

    @Test
    fun startUpSession_validRequest_success(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)

        // Set up gRPC response capture
        val startUpSessionCallback = CompletableDeferred<Unit>()
        val startSessionResponseObserver = mock<StreamObserver<StartSessionResponse>>()
        whenever(startSessionResponseObserver.onNext(any())) doAnswer
            {
                startUpSessionCallback.complete(Unit)
                Unit
            }

        // Send startup request
        val startSessionRequestObserver = stub.startUpSession(startSessionResponseObserver)
        startSessionRequestObserver.onNext(defaultStartSessionRequest)

        // Assert startup response
        startUpSessionCallback.await()
        val responseCaptor = argumentCaptor<StartSessionResponse>()
        verify(startSessionResponseObserver).onNext(responseCaptor.capture())
        val startSessionResponse = responseCaptor.firstValue
        assertThat(startSessionResponse).isEqualTo(StartSessionResponse.getDefaultInstance())
        verify(startSessionResponseObserver, times(1)).onNext(any())
        assertThat(SessionManager.getSession(sessionId)).isNotNull()

        // end startSession stream
        startSessionRequestObserver.onCompleted()
        assertThat(SessionManager.getSession(sessionId)).isNull()

        server.shutdownNow()
    }

    @Test
    fun startUpSession_shouldFailWhenNoStaticCapability(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val startSessionResponseObserver = mock<StreamObserver<StartSessionResponse>>()

        // Send startup request
        val invalidStartSessionRequest =
            StartSessionRequest.newBuilder()
                .setIntentName(testBiiName)
                .setIdentifier("UNKNOWN_FULFILLMENT_ID")
                .setSessionIdentifier(sessionId)
                .build()
        val startSessionRequestObserver = stub.startUpSession(startSessionResponseObserver)
        startSessionRequestObserver.onNext(invalidStartSessionRequest)

        // Assert.
        val exceptionCaptor = argumentCaptor<StatusRuntimeException>()
        verify(startSessionResponseObserver).onError(exceptionCaptor.capture())
        assertThat(Status.fromThrowable(exceptionCaptor.firstValue).code)
            .isEqualTo(Status.Code.FAILED_PRECONDITION)
        assertThat(Status.fromThrowable(exceptionCaptor.firstValue).description)
            .isEqualTo(ERROR_NO_ACTION_CAPABILITY)
        verify(capability1, never()).createSession(any(), any())

        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_shouldGetValidResponse(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)
        assertStartupSession(stub)
        verify(capability1, times(1)).createSession(any(), any())

        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequest)
                .build()
        val responseFuture = futureStub.sendRequestFulfillment(request)

        val response = responseFuture.await()
        assertThat(response).isNotNull()
        assertThat(response.fulfillmentResponse).isEqualTo(testFulfillmentResponse)
        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_shouldFailWhenNoFulfillment(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)
        assertStartupSession(stub)
        verify(capability1, times(1)).createSession(any(), any())

        // Ensure a failed future is returned when missing fulfillment
        val requestWithMissingFulfillment =
            Request.newBuilder()
                .setFulfillmentRequest(FulfillmentRequest.getDefaultInstance())
                .build()
        val exception =
            assertFailsWith<StatusRuntimeException> {
                futureStub.sendRequestFulfillment(requestWithMissingFulfillment).await()
            }
        assertThat(Status.fromThrowable(exception).code).isEqualTo(Status.Code.FAILED_PRECONDITION)
        assertThat(Status.fromThrowable(exception).description)
            .isEqualTo(ERROR_NO_FULFILLMENT_REQUEST)

        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_shouldFailWhenNoStaticCapability(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)
        assertStartupSession(stub)
        verify(capability1, times(1)).createSession(any(), any())

        val requestWithUnknownFulfillmentId =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(
                    FulfillmentRequest.newBuilder()
                        .addFulfillments(
                            Fulfillment.newBuilder().setIdentifier("UNKNOWN_FULFILLMENT_ID")
                        ),
                )
                .build()
        val exception =
            assertFailsWith<StatusRuntimeException> {
                futureStub.sendRequestFulfillment(requestWithUnknownFulfillmentId).await()
            }
        assertThat(Status.fromThrowable(exception).code).isEqualTo(Status.Code.FAILED_PRECONDITION)
        assertThat(Status.fromThrowable(exception).description)
            .isEqualTo(ERROR_NO_ACTION_CAPABILITY)

        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_shouldFailWhenNoSession(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)
        assertStartupSession(stub)
        verify(capability1, times(1)).createSession(any(), any())

        val requestWithUnknownFulfillmentId =
            Request.newBuilder()
                .setSessionIdentifier("UNKNOWN_SESSION_ID")
                .setFulfillmentRequest(
                    FulfillmentRequest.newBuilder()
                        .addFulfillments(Fulfillment.newBuilder().setIdentifier(capabilityId)),
                )
                .build()
        val exception =
            assertFailsWith<StatusRuntimeException> {
                futureStub.sendRequestFulfillment(requestWithUnknownFulfillmentId).await()
            }
        assertThat(Status.fromThrowable(exception).code).isEqualTo(Status.Code.FAILED_PRECONDITION)
        assertThat(Status.fromThrowable(exception).description).isEqualTo(ERROR_NO_SESSION)

        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_shouldFailWhenSessionEnded(): Unit = runBlocking {
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor,
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        // Verify capability session is created
        val mockSession = createMockSession()
        whenever(mockSession.isActive).thenReturn(false)
        whenever(capability1.createSession(any(), any())).thenReturn(mockSession)
        assertStartupSession(stub)
        verify(capability1, times(1)).createSession(any(), any())

        // Send request to completed session.
        val requestToEndedSession =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequest)
                .build()
        val exception =
            assertFailsWith<StatusRuntimeException> {
                futureStub.sendRequestFulfillment(requestToEndedSession).await()
            }
        assertThat(Status.fromThrowable(exception).code).isEqualTo(Status.Code.FAILED_PRECONDITION)
        assertThat(Status.fromThrowable(exception).description).isEqualTo(ERROR_SESSION_ENDED)

        server.shutdownNow()
    }

    private suspend fun assertStartupSession(stub: AppInteractionServiceStub) {
        // Set up gRPC response capture
        val startUpSession = CompletableDeferred<Unit>()
        val startSessionResponseObserver = mock<StreamObserver<StartSessionResponse>>()
        whenever(startSessionResponseObserver.onNext(any())) doAnswer
            {
                startUpSession.complete(Unit)
                Unit
            }

        // Send startup request
        val startSessionRequestObserver = stub.startUpSession(startSessionResponseObserver)
        startSessionRequestObserver.onNext(defaultStartSessionRequest)

        // Assert startup response
        startUpSession.await()
        val responseCaptor = argumentCaptor<StartSessionResponse>()
        verify(startSessionResponseObserver).onNext(responseCaptor.capture())
        val startSessionResponse = responseCaptor.firstValue
        assertThat(startSessionResponse).isEqualTo(StartSessionResponse.getDefaultInstance())
        verify(startSessionResponseObserver, times(1)).onNext(any())
    }

    @Throws(IOException::class)
    private fun createInProcessServer(
        service: BindableService,
        vararg interceptors: ServerInterceptor,
    ): Server {
        return grpcCleanup.register(
            InProcessServerBuilder.forName(testServerName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(service, *interceptors))
                .build()
                .start(),
        )
    }

    private fun createInProcessChannel(): ManagedChannel {
        return grpcCleanup.register(
            InProcessChannelBuilder.forName(testServerName).directExecutor().build(),
        )
    }

    private fun createMockSession(): CapabilitySession {
        val mockSession = mock<CapabilitySession>()
        whenever(mockSession.execute(any(), any())).thenAnswer { invocation ->
            (invocation.arguments[1] as CallbackInternal).onSuccess(testFulfillmentResponse)
        }
        whenever(mockSession.sessionId).thenReturn(sessionId)
        whenever(mockSession.state).thenReturn(AppDialogState.getDefaultInstance())
        whenever(mockSession.isActive).thenReturn(true)
        whenever(mockSession.uiHandle).thenReturn(Any())
        return mockSession
    }
}
