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

import android.util.Log
import android.util.SizeF
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.LibInfo
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger.LogLevel
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.GroundingRequest
import androidx.appactions.interaction.proto.GroundingResponse
import androidx.appactions.interaction.proto.Version
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc.AppInteractionServiceImplBase
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest.RequestDataCase
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionResponse
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.RemoteViewsInfo
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Request
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Response
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionRequest
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionResponse
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Status.Code
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.UiUpdate
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Implementation of [AppInteractionServiceImplBase] generated from the GRPC proto file. This
 * class delegates the requests to the appropriate capability session.
 */
internal class AppInteractionServiceGrpcImpl(
    private val appInteractionService: AppInteractionService,
) : AppInteractionServiceImplBase() {

    var registeredCapabilities = listOf<Capability>()

    companion object {
        private const val TAG = "ActionsServiceGrpcImpl"
        private const val ERROR_NO_COLLECTION_SUPPORT = "Session doesn't support collection view"
        private const val ERROR_NO_UI = "No UI set"
        const val ERROR_NO_SESSION = "Session not available"
        const val ERROR_NO_FULFILLMENT_REQUEST = "Fulfillment request missing"
        const val ERROR_NO_ACTION_CAPABILITY = "Capability was not found"
        const val ERROR_SESSION_ENDED = "Session already ended"

        fun <T> respondAndComplete(response: T, responseObserver: StreamObserver<T>) {
            synchronized(responseObserver) {
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }
        }

        fun respondWithError(t: Throwable, responseObserver: StreamObserver<*>) {
            synchronized(responseObserver) {
                responseObserver.onError(t)
            }
        }

        init {
            LoggerInternal.setLogger(
                object : CapabilityLogger {
                    override fun log(
                        logLevel: LogLevel,
                        logTag: String,
                        message: String,
                    ) {
                        when (logLevel) {
                            LogLevel.ERROR -> Log.e(logTag, message)
                            LogLevel.WARN -> Log.w(logTag, message)
                            LogLevel.INFO -> Log.i(logTag, message)
                        }
                    }

                    override fun log(
                        logLevel: LogLevel,
                        logTag: String,
                        message: String,
                        throwable: Throwable,
                    ) {
                        when (logLevel) {
                            LogLevel.ERROR -> Log.e(logTag, message, throwable)
                            LogLevel.WARN -> Log.w(logTag, message, throwable)
                            LogLevel.INFO -> Log.i(logTag, message, throwable)
                        }
                    }
                },
            )
        }
    }

    override fun startUpSession(
        responseObserver: StreamObserver<StartSessionResponse>,
    ): StreamObserver<StartSessionRequest> = StartSessionRequestObserver(responseObserver)

    private inner class StartSessionRequestObserver internal constructor(
        private val startSessionResponseObserver: StreamObserver<StartSessionResponse>,
    ) : StreamObserver<StartSessionRequest> {

        // Every AppInteractionService connection is defined by this streaming RPC connection.
        // There should only be one session tied to each gRPC impl instance.
        private var currentSessionId: String? = null

        override fun onNext(request: StartSessionRequest) {
            if (currentSessionId != null) {
                return
            }
            val sessionId = request.getSessionIdentifier()!!
            currentSessionId = sessionId
            if (registeredCapabilities.size == 0) {
                registeredCapabilities = appInteractionService.registeredCapabilities
            }
            val targetCapability = registeredCapabilities
                .filter { request.getIdentifier() == it.id }
                .firstOrNull()
            if (targetCapability == null) {
                return respondWithError(
                    StatusRuntimeException(
                        Status.FAILED_PRECONDITION.withDescription(
                            ERROR_NO_ACTION_CAPABILITY,
                        ),
                    ),
                    startSessionResponseObserver,
                )
            }
            val hostProperties = HostProperties.Builder()
                .setMaxHostSizeDp(
                    SizeF(
                        request.getHostProperties().getHostViewHeightDp(),
                        request.getHostProperties().getHostViewWidthDp(),
                    ),
                ).build()
            val session = targetCapability.createSession(
                sessionId,
                hostProperties,
            )
            SessionManager.putSession(sessionId, session)
            startSessionResponseObserver.onNext(StartSessionResponse.getDefaultInstance())
        }

        override fun onError(t: Throwable) {
            respondWithError(t, startSessionResponseObserver)
            currentSessionId?.let {
                destroyAndRemoveSession(it)
                UiSessions.removeUiCache(it)
            }
        }

        override fun onCompleted() {
            synchronized(startSessionResponseObserver) {
                startSessionResponseObserver.onCompleted()
            }
            currentSessionId?.let {
                destroyAndRemoveSession(it)
                UiSessions.removeUiCache(it)
            }
        }
    }

    override fun sendRequestFulfillment(
        request: Request,
        responseObserver: StreamObserver<Response>,
    ) {
        if (request.getFulfillmentRequest().getFulfillmentsList().isEmpty()) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(
                        ERROR_NO_FULFILLMENT_REQUEST,
                    ),
                ),
                responseObserver,
            )
        }
        val selectedFulfillment = request.getFulfillmentRequest().getFulfillments(0)
        val capability = registeredCapabilities
            .filter { selectedFulfillment.getIdentifier() == it.id }
            .firstOrNull()
        if (capability == null) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(
                        ERROR_NO_ACTION_CAPABILITY,
                    ),
                ),
                responseObserver,
            )
        }
        val sessionId = request.getSessionIdentifier()!!
        val currentSession = SessionManager.getSession(sessionId)
        if (currentSession == null) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION),
                ),
                responseObserver,
            )
        }
        if (!currentSession.isActive) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(ERROR_SESSION_ENDED),
                ),
                responseObserver,
            )
        }
        Futures.addCallback(
            executeFulfillmentRequest(currentSession, selectedFulfillment),
            object : FutureCallback<FulfillmentResponse> {
                override fun onSuccess(fulfillmentResponse: FulfillmentResponse) {
                    val responseBuilder =
                        convertFulfillmentResponse(fulfillmentResponse, capability)
                            .toBuilder()
                    val uiCache = UiSessions.getUiCacheOrNull(sessionId)
                    if (uiCache != null && uiCache.hasUnreadUiResponse) {
                        val cachedRemoteViewsInternal = uiCache.cachedRemoteViewsInternal
                        responseBuilder.setUiUpdate(UiUpdate.getDefaultInstance())
                        if (cachedRemoteViewsInternal != null &&
                            !cachedRemoteViewsInternal.collectionViewFactories.keys.isEmpty()) {
                            responseBuilder.setCollectionUpdate(
                                AppInteractionServiceProto.CollectionUpdate.newBuilder()
                                    .addAllViewIds(
                                        cachedRemoteViewsInternal.collectionViewFactories.keys
                                    ),
                            )
                        }
                        uiCache.resetUnreadUiResponse()
                    }
                    respondAndComplete(responseBuilder.build(), responseObserver)
                }

                override fun onFailure(t: Throwable) {
                    respondWithError(
                        when {
                            t is CapabilityExecutionException -> convertToGrpcException(t)
                            t is StatusRuntimeException || t is StatusException -> t
                            else -> StatusRuntimeException(
                                Status.INTERNAL.withDescription(
                                    t.message,
                                ).withCause(t),
                            )
                        },
                        responseObserver,
                    )
                    // Assistant will terminate the connection, which will reach
                    // startUpSession.onError(t) / onCompleted()
                }
            },
            Runnable::run,
        )
    }

    override fun requestUi(
        req: AppInteractionServiceProto.UiRequest,
        responseObserver: StreamObserver<AppInteractionServiceProto.UiResponse>,
    ) {
        val sessionId = req.getSessionIdentifier()!!
        if (SessionManager.getSession(sessionId) == null) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION),
                ),
                responseObserver,
            )
        }
        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        if (uiCache == null) {
            return respondWithError(
                StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)),
                responseObserver,
            )
        }
        val tileLayoutInternal = uiCache.cachedTileLayoutInternal
        val remoteViewsInternal = uiCache.cachedRemoteViewsInternal

        if (tileLayoutInternal == null && remoteViewsInternal == null) {
            UiSessions.removeUiCache(sessionId)
            return respondWithError(
                StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)),
                responseObserver
            )
        }
        val uiResponseBuilder = AppInteractionServiceProto.UiResponse.newBuilder()
        tileLayoutInternal?.let { uiResponseBuilder.tileLayout = it.toProto() }
        if (remoteViewsInternal != null) {
            RemoteViewsOverMetadataInterceptor.setRemoteViews(remoteViewsInternal.remoteViews)
            uiResponseBuilder
                .setRemoteViewsInfo(
                    RemoteViewsInfo.newBuilder()
                        .setWidthDp(remoteViewsInternal.size.width)
                        .setHeightDp(remoteViewsInternal.size.height)
                )
                .build()
        }
        respondAndComplete(uiResponseBuilder.build(), responseObserver)
    }

    override fun requestCollection(
        req: CollectionRequest,
        responseObserver: StreamObserver<CollectionResponse>,
    ) {
        val sessionId = req.getSessionIdentifier()!!
        if (SessionManager.getSession(sessionId) == null) {
            return respondWithError(
                StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION),
                ),
                responseObserver,
            )
        }
        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        if (uiCache == null) {
            return respondWithError(
                StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)),
                responseObserver,
            )
        }
        val factory = uiCache.cachedRemoteViewsInternal?.collectionViewFactories?.get(
            req.getViewId()
        )
        if (factory == null) {
            return respondWithError(
                StatusRuntimeException(
                    Status.UNIMPLEMENTED.withDescription(ERROR_NO_COLLECTION_SUPPORT),
                ),
                responseObserver,
            )
        }
        when (req.getRequestDataCase()) {
            RequestDataCase.ON_DESTROY -> {
                requestCollectionOnDestroy(factory, responseObserver)
            }
            RequestDataCase.GET_COUNT -> {
                requestCollectionGetCount(factory, responseObserver)
            }
            RequestDataCase.GET_VIEW_AT -> {
                requestCollectionGetViewAt(
                    factory,
                    responseObserver,
                    req.getGetViewAt().getPosition(),
                )
            }
            RequestDataCase.GET_LOADING_VIEW -> {
                requestCollectionGetLoadingView(factory, responseObserver)
            }
            RequestDataCase.GET_VIEW_TYPE_COUNT -> {
                requestCollectionGetViewTypeCount(factory, responseObserver)
            }
            RequestDataCase.GET_ITEM_ID -> {
                requestCollectionGetItemId(
                    factory,
                    responseObserver,
                    req.getGetItemId().getPosition(),
                )
            }
            RequestDataCase.HAS_STABLE_IDS -> {
                requestCollectionHasStableIds(factory, responseObserver)
            }
            else -> {
                // ignore it
                Log.d(TAG, "received CollectionRequest with unknown RequestData case.")
                responseObserver.onCompleted()
            }
        }
    }

    override fun requestGrounding(
        request: GroundingRequest,
        responseObserver: StreamObserver<GroundingResponse>,
    ) {
        val entityProvider = appInteractionService.registeredEntityProviders.filter {
            it.id == request.getRequest().getEntityProviderId()
        }.firstOrNull()
        if (entityProvider == null) {
            return respondAndComplete(
                GroundingResponse.newBuilder()
                    .setResponse(
                        GroundingResponse.Response.newBuilder().setStatus(
                            GroundingResponse.Status.INVALID_ENTITY_PROVIDER,
                        ),
                    ).build(),
                responseObserver,
            )
        }
        CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                respondAndComplete(
                    entityProvider.lookupInternal(request),
                    responseObserver,
                )
            } catch (t: Throwable) {
                respondWithError(
                    when {
                        t is StatusRuntimeException || t is StatusException -> t
                        else -> StatusRuntimeException(
                            Status.INTERNAL.withDescription(
                                t.message,
                            ).withCause(t),
                        )
                    },
                    responseObserver,
                )
            }
        }
    }

    private fun requestCollectionOnDestroy(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
    ) {
        factory.onDestroy()
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer)
    }

    private fun requestCollectionGetCount(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
    ) {
        respondAndComplete(
            CollectionResponse.newBuilder()
                .setGetCount(
                    CollectionResponse.GetCount.newBuilder()
                        .setCount(factory.getCount()),
                )
                .build(),
            observer,
        )
    }

    private fun requestCollectionGetViewAt(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
        position: Int,
    ) {
        factory.getViewAt(position)?.let(RemoteViewsOverMetadataInterceptor::setRemoteViews)
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer)
    }

    private fun requestCollectionGetLoadingView(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
    ) {
        factory.getLoadingView()?.let(RemoteViewsOverMetadataInterceptor::setRemoteViews)
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer)
    }

    private fun requestCollectionGetViewTypeCount(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
    ) {
        respondAndComplete(
            CollectionResponse.newBuilder()
                .setGetViewTypeCount(
                    CollectionResponse.GetViewTypeCount.newBuilder()
                        .setViewTypeCount(factory.getViewTypeCount()),
                )
                .build(),
            observer,
        )
    }

    private fun requestCollectionGetItemId(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
        position: Int,
    ) {
        respondAndComplete(
            CollectionResponse.newBuilder()
                .setGetItemId(
                    CollectionResponse.GetItemId.newBuilder()
                        .setItemId(factory.getItemId(position)),
                )
                .build(),
            observer,
        )
    }

    private fun requestCollectionHasStableIds(
        factory: RemoteViewsFactory,
        observer: StreamObserver<CollectionResponse>,
    ) {
        respondAndComplete(
            CollectionResponse.newBuilder()
                .setHasStableIds(
                    CollectionResponse.HasStableIds.newBuilder()
                        .setHasStableIds(factory.hasStableIds()),
                )
                .build(),
            observer,
        )
    }

    private fun convertToAppActionsContextVersion(
        libInfoVersion: LibInfo.Version,
    ): Version {
        val builder = Version.newBuilder()
            .setMajor(libInfoVersion.major.toLong())
            .setMinor(libInfoVersion.minor.toLong())
            .setPatch(libInfoVersion.patch.toLong())
        libInfoVersion.preReleaseId?.let(builder::setPrereleaseId)
        return builder.build()
    }

    /**
     * Calls destroy on the session if it's found in SessionManager.
     * Also removes the session from map.
     */
    internal fun destroyAndRemoveSession(sessionId: String) {
        SessionManager.getSession(sessionId)?.destroy()
        SessionManager.removeSession(sessionId)
    }

    internal fun convertToGrpcException(e: CapabilityExecutionException): StatusRuntimeException {
        return when (e.getErrorStatus()) {
            ErrorStatusInternal.TIMEOUT -> StatusRuntimeException(
                Status.DEADLINE_EXCEEDED.withDescription(e.message).withCause(e),
            )
            else -> StatusRuntimeException(
                Status.INTERNAL.withDescription(e.message).withCause(e),
            )
        }
    }

    internal fun convertFulfillmentResponse(
        fulfillmentResponse: FulfillmentResponse,
        capability: Capability,
    ): Response {
        val appAction = capability.appAction
        val isDialogSession = appAction.getTaskInfo().getSupportsPartialFulfillment()
        val version = convertToAppActionsContextVersion(
            LibInfo(appInteractionService.getApplicationContext()).getVersion(),
        )
        val responseBuilder: Response.Builder =
            // TODO(b/269638788): Add DialogState to the Response proto.
            Response.newBuilder()
                .setFulfillmentResponse(fulfillmentResponse)
                .setAppActionsContext(
                    AppActionsContext.newBuilder()
                        .addActions(appAction)
                        .setVersion(version)
                        .build(),
                )
        if (!isDialogSession) {
            responseBuilder.setEndingStatus(
                AppInteractionServiceProto.Status.newBuilder()
                    .setStatusCode(Code.COMPLETE)
                    .build(),
            )
        }
        return responseBuilder.build()
    }

    private fun executeFulfillmentRequest(
        session: CapabilitySession,
        fulfillmentRequest: FulfillmentRequest.Fulfillment,
    ): ListenableFuture<FulfillmentResponse> = CallbackToFutureAdapter.getFuture { completer ->
        session.execute(
            ArgumentsWrapper.create(fulfillmentRequest),
            CapabilityCallback(completer),
        )
        "executing action capability"
    }
}