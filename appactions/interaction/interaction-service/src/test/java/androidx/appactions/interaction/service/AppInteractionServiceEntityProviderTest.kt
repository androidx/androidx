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

import androidx.appactions.builtintypes.experimental.types.Alarm
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils.buildSearchActionParamValue
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.appactions.interaction.proto.GroundingRequest
import androidx.appactions.interaction.proto.GroundingRequest.Request
import androidx.appactions.interaction.proto.GroundingResponse
import androidx.appactions.interaction.proto.GroundingResponse.Candidate
import androidx.appactions.interaction.proto.GroundingResponse.Response
import androidx.appactions.interaction.proto.GroundingResponse.Status
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc
import androidx.appactions.interaction.service.testing.internal.FakeAlarmEntityProvider
import androidx.appactions.interaction.service.testing.internal.FakeAppInteractionService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.times
import org.robolectric.Robolectric

// TODO(b/271929200) Implement tests for the 2 UI related RPCs
@RunWith(AndroidJUnit4::class)
class AppInteractionServiceEntityProviderTest {
    val testServerName = "testServer"

    @get:Rule val grpcCleanup = GrpcCleanupRule()

    private lateinit var appInteractionService: FakeAppInteractionService

    @Before
    fun before() {
        appInteractionService = Robolectric.buildService(
            FakeAppInteractionService::class.java
        ).get()
    }

    @Test
    fun alarmProvider_incorrectProviderId(): Unit = runBlocking {
        val morningAlarm = Alarm.Builder().setIdentifier("alarm1").setName("Morning Alarm").build()
        val alarmProvider = FakeAlarmEntityProvider(
            "alarmProvider",
            listOf(morningAlarm)
        )
        appInteractionService.registeredEntityProviders = listOf(alarmProvider)
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService)
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)

        val groundingRequest = GroundingRequest
            .newBuilder()
            .setRequest(
                Request.newBuilder()
                    .setEntityProviderId("randomIncorrectId")
                    .setSearchAction(
                        buildSearchActionParamValue(
                            "alarm search query"
                        )
                    )
            )
            .build()

        val responseObserver = TestStreamObserver<GroundingResponse>()
        stub.requestGrounding(groundingRequest, responseObserver)
        val groundingResponse = responseObserver.firstResultDeferred.awaitSync()
        assertThat(groundingResponse).isEqualTo(
            GroundingResponse.newBuilder()
            .setResponse(
                GroundingResponse.Response.newBuilder().setStatus(
                    GroundingResponse.Status.INVALID_ENTITY_PROVIDER,
                ),
            ).build()
        )

        server.shutdownNow()
    }

    @Test
    fun alarmProvider_success(): Unit = runBlocking {
        val morningAlarm = Alarm.Builder().setIdentifier("alarm1").setName("Morning Alarm").build()
        val alarmProvider = FakeAlarmEntityProvider(
            "alarmProvider",
            listOf(morningAlarm)
        )
        appInteractionService.registeredEntityProviders = listOf(alarmProvider)
        val server =
            createInProcessServer(
                AppInteractionServiceGrpcImpl(appInteractionService)
            )

        val channel = createInProcessChannel()
        val stub = AppInteractionServiceGrpc.newStub(channel)

        val groundingRequest = GroundingRequest
            .newBuilder()
            .setRequest(
                Request.newBuilder()
                    .setEntityProviderId("alarmProvider")
                    .setSearchAction(
                        buildSearchActionParamValue(
                            "alarm search query"
                        )
                    )
            )
            .build()

        val responseObserver = TestStreamObserver<GroundingResponse>()
        stub.requestGrounding(groundingRequest, responseObserver)
        val groundingResponse = responseObserver.firstResultDeferred.awaitSync()
        assertThat(groundingResponse).isEqualTo(
            GroundingResponse.newBuilder().setResponse(
                Response.newBuilder()
                    .setStatus(Status.SUCCESS)
                    .addCandidates(
                        Candidate.newBuilder().setGroundedEntity(
                            EntityConverter.of(TypeConverters.ALARM_TYPE_SPEC).convert(morningAlarm)
                        )
                    )
            ).build()
        )

        server.shutdownNow()
    }

    private fun createInProcessServer(
        service: BindableService
    ): Server {
        return grpcCleanup.register(
            InProcessServerBuilder.forName(testServerName)
                .directExecutor()
                .addService(service)
                .build()
                .start()
        )
    }

    private fun createInProcessChannel(): ManagedChannel {
        return grpcCleanup.register(
            InProcessChannelBuilder.forName(testServerName).directExecutor().build()
        )
    }

    /** Captures the first error or value received by the stream observer */
    private class TestStreamObserver<T> : StreamObserver<T> {
        val firstResultDeferred = CompletableDeferred<T>()

        override fun onNext(value: T) {
            firstResultDeferred.complete(value)
        }

        override fun onError(t: Throwable) {
            firstResultDeferred.completeExceptionally(t)
        }

        override fun onCompleted() {
            firstResultDeferred.cancel()
        }
    }
}
