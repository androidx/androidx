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

import android.content.Context
import android.util.SizeF
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState
import androidx.appactions.interaction.proto.AppInteractionMetadata.ErrorStatus
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_ACTION_CAPABILITY
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_FULFILLMENT_REQUEST
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_NO_SESSION
import androidx.appactions.interaction.service.AppInteractionServiceGrpcImpl.Companion.ERROR_SESSION_ENDED
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc.AppInteractionServiceStub
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.GetCount
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.GetItemId
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.GetLoadingView
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.GetViewAt
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.GetViewTypeCount
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.HasStableIds
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.OnDestroy
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Request
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionRequest
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionResponse
import androidx.appactions.interaction.service.test.R
import androidx.appactions.interaction.service.testing.internal.FakeAppInteractionService
import androidx.appactions.interaction.service.testing.internal.FakeCapability
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

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
    private val testFulfillmentRequestUi =
        FulfillmentRequest.newBuilder()
            .addFulfillments(
                Fulfillment.newBuilder()
                    .setName(testBiiName)
                    .setIdentifier(capabilityId)
                    .addParams(
                        FulfillmentParam.newBuilder()
                            .setName("fieldOne")
                            .addFulfillmentValues(
                                FulfillmentValue.newBuilder()
                                    .setValue(
                                        ParamValue.newBuilder().setStringValue("hello").build())
                                    .build())
                            .build())
                    .setType(Fulfillment.Type.SYNC)
                    .setSyncStatus(Fulfillment.SyncStatus.SLOTS_COMPLETE)
                    .build()
            )
            .build()
    private val testAppDialogState = AppDialogState.newBuilder().setFulfillmentIdentifier("id")
        .addParams(
            AppActionsContext.DialogParameter.newBuilder()
                .setName("Sample dialog")
                .addCurrentValue(
                    CurrentValue.newBuilder().setValue(
                        ParamValue.newBuilder().setStringValue("Sample Test").build()
                    )
                )
        ).build()
    private lateinit var capability1: Capability
    private lateinit var appInteractionService: FakeAppInteractionService

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteViewsFactoryId = 123
    private val remoteViews = RemoteViews(context.packageName, R.layout.remote_view)

    private fun createFakeCapability(vararg uiResponses: UiResponse):
        Capability {
        return FakeCapability.CapabilityBuilder()
            .setId(capabilityId)
            .setExecutionSessionFactory { _ ->
                object : FakeCapability.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: FakeCapability.Arguments,
                    ): ExecutionResult<FakeCapability.Output> {
                        for (uiResponse in uiResponses) {
                            this.updateUi(uiResponse)
                        }
                        return ExecutionResult.Builder<FakeCapability.Output>().build()
                    }
                }
            }
            .build()
    }

    @Before
    fun setup() {
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

    @After
    fun cleanup() {
        UiSessions.removeUiCache(sessionId)
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
    fun requestUi_respondWithRemoteViewsInMetadata(): Unit = runBlocking {

        val remoteViewsUiResponse =
            UiResponse.RemoteViewsUiBuilder()
                .setRemoteViews(remoteViews, SizeF(10f, 15f))
                .addRemoteViewsFactory(remoteViewsFactoryId, FakeRemoteViewsFactory())
                .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)

        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val responseObserver = mock<StreamObserver<AppInteractionServiceProto.UiResponse>>()
        stub.requestUi(
            AppInteractionServiceProto.UiRequest.newBuilder()
                .setSessionIdentifier(sessionId).build(),
            responseObserver
        )
        val responseCaptor = argumentCaptor<AppInteractionServiceProto.UiResponse>()
        verify(responseObserver).onNext(responseCaptor.capture())
        assertThat(responseCaptor.firstValue).isEqualTo(
            AppInteractionServiceProto.UiResponse.newBuilder().setRemoteViewsInfo(
                AppInteractionServiceProto.RemoteViewsInfo.newBuilder()
                    .setWidthDp(10f)
                    .setHeightDp(15f)
                    .build()
            ).build()
        )

        server.shutdownNow()
    }

    @Test
    fun requestUi_noUi_failWithStatusRuntimeException(): Unit = runBlocking {

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability() // Not setting any Ui response
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val responseObserver = mock<StreamObserver<AppInteractionServiceProto.UiResponse>>()
        stub.requestUi(
            AppInteractionServiceProto.UiRequest.newBuilder()
                .setSessionIdentifier(sessionId).build(),
            responseObserver
        )

        val errorCaptor = argumentCaptor<StatusRuntimeException>()
        verify(responseObserver).onError(errorCaptor.capture())
        assertThat(errorCaptor.firstValue.status.code).isEqualTo(
            Status.INTERNAL.code
        )
        assertThat(errorCaptor.firstValue.status.description).isEqualTo("No UI set")

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersRemoteViewFactory(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetViewTypeCount(GetViewTypeCount.newBuilder().build()).build()
        futureStub.requestCollection(collectionRequest)
        verify(mockedRemoteViewFactory, times(1)).viewTypeCount

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersGetCount(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        whenever(mockedRemoteViewFactory.count) doReturn (3)
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetCount(GetCount.newBuilder().build())
            .build()
        val collectionResponse = futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory).count
        assertThat(collectionResponse.getCount.count).isEqualTo(3)

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersOnDestroy(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setOnDestroy(OnDestroy.newBuilder().build()).build()
        futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory, times(1)).onDestroy()

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersGetViewAt(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetViewAt(GetViewAt.newBuilder().setPosition(1).build()).build()
        futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory, times(1)).getViewAt(1)

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersGetLoadingView(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetLoadingView(GetLoadingView.newBuilder().build()).build()
        futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory, times(1)).loadingView

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersGetViewTypeCount(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        whenever(mockedRemoteViewFactory.viewTypeCount) doReturn (1)
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetViewTypeCount(GetViewTypeCount.newBuilder().build())
            .build()
        val collectionResponse = futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory).viewTypeCount
        assertThat(collectionResponse.getViewTypeCount.viewTypeCount).isEqualTo(1)

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersGetItemId(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        whenever(mockedRemoteViewFactory.getItemId(any())) doReturn (2)
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setGetItemId(GetItemId.newBuilder().setPosition(2).build())
            .build()
        val collectionResponse = futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory).getItemId(2)
        assertThat(collectionResponse.getItemId.itemId).isEqualTo(2)

        server.shutdownNow()
    }

    @Test
    fun collectionRequestTriggersHasStableIds(): Unit = runBlocking {

        val mockedRemoteViewFactory = mock<RemoteViewsFactory>()
        whenever(mockedRemoteViewFactory.hasStableIds()) doReturn (true)
        val remoteViewsUiResponse = UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, mockedRemoteViewFactory)
            .build()

        appInteractionService.registeredCapabilities = listOf(
            createFakeCapability(remoteViewsUiResponse)
        )
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService),
                remoteViewsInterceptor
            )
        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)
        val futureStub = AppInteractionServiceGrpc.newFutureStub(channel)

        assertStartupSession(stub)
        // Send fulfillment request
        val request =
            Request.newBuilder()
                .setSessionIdentifier(sessionId)
                .setFulfillmentRequest(testFulfillmentRequestUi)
                .build()

        val responseFuture = futureStub.sendRequestFulfillment(request)
        responseFuture.await()

        val collectionRequest = AppInteractionServiceProto.CollectionRequest.newBuilder()
            .setSessionIdentifier(sessionId)
            .setViewId(123)
            .setHasStableIds(HasStableIds.newBuilder().build())
            .build()
        val collectionResponse = futureStub.requestCollection(collectionRequest).await()
        verify(mockedRemoteViewFactory).hasStableIds()
        assertThat(collectionResponse.hasStableIds.hasStableIds).isEqualTo(true)

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
        val metadataErrorStatus = assertAndGetErrorStatus(exceptionCaptor.firstValue)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.CAPABILITY_NOT_FOUND)
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

        assertThat(response.appActionsContext.dialogStatesList).containsExactly(testAppDialogState)
        server.shutdownNow()
    }

    @Test
    fun sendRequestFulfillment_errorFromCallback_errorPassedInGrpcMetadata(): Unit = runBlocking {
        val mockCapabilitySession = createMockSession()
        whenever(mockCapabilitySession.execute(any(), any())).thenAnswer { invocation ->
            (invocation.arguments[1] as CallbackInternal)
                .onError(ErrorStatusInternal.EXTERNAL_EXCEPTION)
        }
        whenever(capability1.createSession(any(), any())).thenReturn(mockCapabilitySession)

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
        val exception =
            assertFailsWith<StatusRuntimeException> {
                futureStub.sendRequestFulfillment(request).await()
            }
        assertThat(Status.fromThrowable(exception).code).isEqualTo(Status.Code.INTERNAL)
        assertThat(Status.fromThrowable(exception).description)
            .isEqualTo("Error executing action capability")
        val metadataErrorStatus = assertAndGetErrorStatus(exception)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.EXTERNAL_EXCEPTION)

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
        val metadataErrorStatus = assertAndGetErrorStatus(exception)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.INVALID_REQUEST)

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
        val metadataErrorStatus = assertAndGetErrorStatus(exception)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.CAPABILITY_NOT_FOUND)

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
        val metadataErrorStatus = assertAndGetErrorStatus(exception)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.SESSION_NOT_FOUND)

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
        val metadataErrorStatus = assertAndGetErrorStatus(exception)
        assertThat(metadataErrorStatus).isEqualTo(ErrorStatus.SESSION_NOT_FOUND)

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
        whenever(mockSession.state).thenReturn(testAppDialogState)
        whenever(mockSession.isActive).thenReturn(true)
        whenever(mockSession.uiHandle).thenReturn(Any())
        return mockSession
    }

    private fun assertAndGetErrorStatus(ex: StatusRuntimeException): ErrorStatus {
        val appInteractionMetadata =
            ex.trailers?.get(AppInteractionGrpcMetadata.INTERACTION_SERVICE_STATUS_KEY)
        assertThat(appInteractionMetadata).isNotNull()
        return appInteractionMetadata!!.errorStatus
    }
}
