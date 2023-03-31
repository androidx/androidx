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

package androidx.appactions.interaction.service;

import android.util.Log;
import android.util.SizeF;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.Capability;
import androidx.appactions.interaction.capabilities.core.HostProperties;
import androidx.appactions.interaction.capabilities.core.LibInfo;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger;
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal;
import androidx.appactions.interaction.proto.AppActionsContext;
import androidx.appactions.interaction.proto.FulfillmentRequest;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.GroundingRequest;
import androidx.appactions.interaction.proto.GroundingResponse;
import androidx.appactions.interaction.proto.Version;
import androidx.appactions.interaction.service.proto.AppInteractionServiceGrpc.AppInteractionServiceImplBase;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionRequest;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.CollectionResponse;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.RemoteViewsInfo;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Request;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Response;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionRequest;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.StartSessionResponse;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.Status.Code;
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto.UiUpdate;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * Implementation of {@link AppInteractionServiceImplBase} generated from the GRPC proto file. This
 * class delegates the requests to the appropriate capability session.
 */
final class AppInteractionServiceGrpcImpl extends AppInteractionServiceImplBase {

    private static final String TAG = "ActionsServiceGrpcImpl";

    static final String ERROR_NO_SESSION = "Session not available";
    static final String ERROR_NO_FULFILLMENT_REQUEST = "Fulfillment request missing";
    static final String ERROR_NO_ACTION_CAPABILITY = "Capability was not found";
    static final String ERROR_SESSION_ENDED = "Session already ended";
    private static final String ERROR_NO_COLLECTION_SUPPORT =
            "Session doesn't support collection view";
    private static final String ERROR_NO_UI = "No UI set";
    private static final String ERROR_MULTIPLE_UI_TYPES =
            "Multiple UI types used in current session";

    final AppInteractionService mAppInteractionService;
    List<Capability> mRegisteredCapabilities = new ArrayList<>();

    static {
        LoggerInternal.setLogger(
                new CapabilityLogger() {
                    @Override
                    public void log(
                            @NonNull LogLevel logLevel,
                            @NonNull String logTag,
                            @NonNull String message) {
                        switch (logLevel) {
                            case ERROR:
                                Log.e(logTag, message);
                                break;
                            case WARN:
                                Log.w(logTag, message);
                                break;
                            case INFO:
                                Log.i(logTag, message);
                                break;
                        }
                    }

                    @Override
                    public void log(
                            @NonNull LogLevel logLevel,
                            @NonNull String logTag,
                            @NonNull String message,
                            @NonNull Throwable throwable) {
                        switch (logLevel) {
                            case ERROR:
                                Log.e(logTag, message, throwable);
                                break;
                            case WARN:
                                Log.w(logTag, message, throwable);
                                break;
                            case INFO:
                                Log.i(logTag, message, throwable);
                                break;
                        }
                    }
                });
    }

    AppInteractionServiceGrpcImpl(AppInteractionService mAppInteractionService) {
        this.mAppInteractionService = mAppInteractionService;
    }

    @Override
    public StreamObserver<StartSessionRequest> startUpSession(
            StreamObserver<StartSessionResponse> responseObserver) {
        return new StartSessionRequestObserver(responseObserver);
    }

    private final class StartSessionRequestObserver implements StreamObserver<StartSessionRequest> {
        private final StreamObserver<StartSessionResponse> mStartSessionResponseObserver;
        // Every AppInteractionService connection is defined by this streaming RPC connection.
        // There should only be one session tied to each gRPC impl instance.
        private String mCurrentSessionId = null;

        StartSessionRequestObserver(StreamObserver<StartSessionResponse> responseObserver) {
            this.mStartSessionResponseObserver = responseObserver;
        }

        @Override
        public void onNext(StartSessionRequest request) {
            if (mCurrentSessionId != null) {
                return;
            }
            mCurrentSessionId = request.getSessionIdentifier();
            if (mRegisteredCapabilities.isEmpty()) {
                mRegisteredCapabilities = mAppInteractionService.getRegisteredCapabilities();
            }
            Optional<Capability> targetCapability =
                    mRegisteredCapabilities.stream()
                            .filter(cap -> request.getIdentifier().equals(cap.getId()))
                            .findFirst();
            if (!targetCapability.isPresent()) {
                mStartSessionResponseObserver.onError(
                        new StatusRuntimeException(
                                Status.FAILED_PRECONDITION.withDescription(
                                        ERROR_NO_ACTION_CAPABILITY)));
                return;
            }
            HostProperties hostProperties =
                    new HostProperties.Builder()
                            .setMaxHostSizeDp(new SizeF(
                                    request.getHostProperties().getHostViewHeightDp(),
                                    request.getHostProperties().getHostViewWidthDp()))
                            .build();
            CapabilitySession session = targetCapability.get().createSession(hostProperties);
            SessionManager.INSTANCE.putSession(mCurrentSessionId, session);
            mStartSessionResponseObserver.onNext(StartSessionResponse.getDefaultInstance());
        }

        @Override
        public void onError(Throwable t) {
            synchronized (mStartSessionResponseObserver) {
                mStartSessionResponseObserver.onError(t);
            }
            if (mCurrentSessionId != null) {
                destroySession(mCurrentSessionId);
            }
            mCurrentSessionId = null;
        }

        @Override
        public void onCompleted() {
            synchronized (mStartSessionResponseObserver) {
                mStartSessionResponseObserver.onCompleted();
            }
            if (mCurrentSessionId != null) {
                destroySession(mCurrentSessionId);
            }
            mCurrentSessionId = null;
        }
    }

    @Override
    public void sendRequestFulfillment(Request request, StreamObserver<Response> responseObserver) {
        if (request.getFulfillmentRequest().getFulfillmentsList().isEmpty()) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(
                                    ERROR_NO_FULFILLMENT_REQUEST)));
            return;
        }
        FulfillmentRequest.Fulfillment selectedFulfillment =
                request.getFulfillmentRequest().getFulfillments(0);
        Optional<Capability> capability =
                mRegisteredCapabilities.stream()
                        .filter(cap -> selectedFulfillment.getIdentifier().equals(cap.getId()))
                        .findFirst();
        if (!capability.isPresent()) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(
                                    ERROR_NO_ACTION_CAPABILITY)));
            return;
        }
        String sessionId = request.getSessionIdentifier();
        CapabilitySession currentSession = SessionManager.INSTANCE.getSession(sessionId);
        if (currentSession == null) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION)));
            return;
        }
        if (currentSession.getStatus() == CapabilitySession.Status.COMPLETED
                || currentSession.getStatus() == CapabilitySession.Status.DESTROYED) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_SESSION_ENDED)));
            return;
        }
        Futures.addCallback(
                executeFulfillmentRequest(currentSession, selectedFulfillment),
                new FutureCallback<FulfillmentResponse>() {
                    @Override
                    public void onSuccess(FulfillmentResponse fulfillmentResponse) {
                        Response.Builder responseBuilder =
                                convertFulfillmentResponse(fulfillmentResponse, capability.get())
                                        .toBuilder();
                        UiCache uiCache = UiSessions.INSTANCE.getUiCacheOrNull(sessionId);
                        if (uiCache != null && uiCache.hasUnreadUiResponse()) {
                            responseBuilder.setUiUpdate(UiUpdate.getDefaultInstance());
                            if (!uiCache.getCachedChangedViewIds().isEmpty()) {
                                responseBuilder.setCollectionUpdate(
                                        AppInteractionServiceProto.CollectionUpdate.newBuilder()
                                                .addAllViewIds(uiCache.getCachedChangedViewIds()));
                            }
                            uiCache.resetUnreadUiResponse();
                        }
                        respondAndComplete(responseBuilder.build(), responseObserver);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Throwable outputThrowable;
                        if (t instanceof CapabilityExecutionException) {
                            outputThrowable =
                                    convertToGrpcException((CapabilityExecutionException) t);
                        } else if (t instanceof StatusRuntimeException
                                || t instanceof StatusException) {
                            outputThrowable = t;
                        } else {
                            outputThrowable =
                                    new StatusRuntimeException(
                                            Status.INTERNAL.withDescription(
                                                    t.getMessage()).withCause(t));
                        }
                        responseObserver.onError(outputThrowable);
                        // Assistant will terminate the connection, which will reach
                        // startUpSession.onError(t) / onCompleted()
                    }
                },
                Runnable::run);
    }

    @Override
    public void requestUi(
            AppInteractionServiceProto.UiRequest req,
            StreamObserver<AppInteractionServiceProto.UiResponse> responseObserver) {
        String sessionId = req.getSessionIdentifier();
        CapabilitySession currentSession = SessionManager.INSTANCE
                .getSession(sessionId);
        if (currentSession == null) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION)));
            return;
        }
        if (currentSession.getStatus() == CapabilitySession.Status.COMPLETED) {
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_SESSION_ENDED)));
            return;
        }
        UiCache uiCache = UiSessions.INSTANCE.getUiCacheOrNull(sessionId);
        if (uiCache == null) {
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)));
            return;
        }

        TileLayoutInternal tileLayout = uiCache.getCachedTileLayout();
        SizeF remoteViewsSize = uiCache.getCachedRemoteViewsSize();
        RemoteViews remoteViews = uiCache.getCachedRemoteViews();
        if (tileLayout != null && remoteViews != null) {
            // TODO(b/272379825): Decide if this is really an invalid state.
            // both types of UI are present, this is a misused of API. We will treat it as error.
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.INTERNAL.withDescription(ERROR_MULTIPLE_UI_TYPES)));
            return;
        }
        if (tileLayout != null) {
            respondAndComplete(
                    AppInteractionServiceProto.UiResponse.newBuilder()
                            .setTileLayout(tileLayout.toProto())
                            .build(),
                    responseObserver);
            return;
        }
        if (remoteViews != null && remoteViewsSize != null) {
            RemoteViewsOverMetadataInterceptor.setRemoteViews(remoteViews);
            respondAndComplete(
                    AppInteractionServiceProto.UiResponse.newBuilder()
                            .setRemoteViewsInfo(
                                    RemoteViewsInfo.newBuilder()
                                            .setWidthDp(remoteViewsSize.getWidth())
                                            .setHeightDp(remoteViewsSize.getHeight()))
                            .build(),
                    responseObserver);
            return;
        }
        destroySession(req.getSessionIdentifier());
        responseObserver.onError(
                new StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)));
    }

    @Override
    public void requestCollection(
            CollectionRequest req, StreamObserver<CollectionResponse> responseObserver) {
        String sessionId = req.getSessionIdentifier();
        CapabilitySession currentSession = SessionManager.INSTANCE
                .getSession(sessionId);
        if (currentSession == null) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_NO_SESSION)));
            return;
        }
        if (currentSession.getStatus() == CapabilitySession.Status.COMPLETED) {
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.FAILED_PRECONDITION.withDescription(ERROR_SESSION_ENDED)));
            return;
        }
        UiCache uiCache = UiSessions.INSTANCE.getUiCacheOrNull(sessionId);
        if (uiCache == null) {
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(ERROR_NO_UI)));
            return;
        }
        RemoteViewsFactory factory = uiCache.onGetViewFactoryInternal(req.getViewId());
        if (factory == null) {
            destroySession(req.getSessionIdentifier());
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.UNIMPLEMENTED.withDescription(ERROR_NO_COLLECTION_SUPPORT)));
            return;
        }
        switch (req.getRequestDataCase()) {
            case ON_DESTROY: {
                requestCollectionOnDestroy(factory, responseObserver);
                break;
            }
            case GET_COUNT: {
                requestCollectionGetCount(factory, responseObserver);
                break;
            }
            case GET_VIEW_AT: {
                requestCollectionGetViewAt(factory, responseObserver,
                        req.getGetViewAt().getPosition());
                break;
            }

            case GET_LOADING_VIEW: {
                requestCollectionGetLoadingView(factory, responseObserver);
                break;
            }
            case GET_VIEW_TYPE_COUNT: {
                requestCollectionGetViewTypeCount(factory, responseObserver);
                break;
            }
            case GET_ITEM_ID: {
                requestCollectionGetItemId(factory, responseObserver,
                        req.getGetItemId().getPosition());
                break;
            }
            case HAS_STABLE_IDS: {
                requestCollectionHasStableIds(factory, responseObserver);
                break;
            }
            default: {
                // ignore it
                Log.d(TAG, "received CollectionRequest with unknown RequestData case.");
                responseObserver.onCompleted();
                break;
            }
        }
    }

    @Override
    public void requestGrounding(
            GroundingRequest request, StreamObserver<GroundingResponse> responseObserver) {
        // TODO(b/268265068): Implement grounding API
    }

    private void requestCollectionOnDestroy(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer) {
        factory.onDestroy();
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer);
    }

    private void requestCollectionGetCount(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer) {
        respondAndComplete(
                CollectionResponse.newBuilder()
                        .setGetCount(CollectionResponse.GetCount.newBuilder()
                                .setCount(factory.getCount()))
                        .build(),
                observer);
    }

    private void requestCollectionGetViewAt(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer, int position) {
        RemoteViews view = factory.getViewAt(position);
        if (view != null) {
            RemoteViewsOverMetadataInterceptor.setRemoteViews(view);
        }
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer);
    }

    private void requestCollectionGetLoadingView(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer) {
        RemoteViews loadingView = factory.getLoadingView();
        if (loadingView != null) {
            RemoteViewsOverMetadataInterceptor.setRemoteViews(loadingView);
        }
        respondAndComplete(CollectionResponse.getDefaultInstance(), observer);
    }

    private void requestCollectionGetViewTypeCount(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer) {
        respondAndComplete(
                CollectionResponse.newBuilder()
                        .setGetViewTypeCount(
                                CollectionResponse.GetViewTypeCount.newBuilder()
                                        .setViewTypeCount(factory.getViewTypeCount()))
                        .build(),
                observer);
    }

    private void requestCollectionGetItemId(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer, int position) {
        respondAndComplete(
                CollectionResponse.newBuilder()
                        .setGetItemId(
                                CollectionResponse.GetItemId.newBuilder()
                                        .setItemId(factory.getItemId(position)))
                        .build(),
                observer);
    }

    private void requestCollectionHasStableIds(
            RemoteViewsFactory factory, StreamObserver<CollectionResponse> observer) {
        respondAndComplete(
                CollectionResponse.newBuilder()
                        .setHasStableIds(
                                CollectionResponse.HasStableIds.newBuilder()
                                        .setHasStableIds(factory.hasStableIds()))
                        .build(),
                observer);
    }

    @NonNull
    private Version convertToAppActionsContextVersion(@NonNull LibInfo.Version libInfoVersion) {
        Version.Builder builder = Version.newBuilder()
                .setMajor(libInfoVersion.getMajor())
                .setMinor(libInfoVersion.getMinor())
                .setPatch(libInfoVersion.getPatch());
        if (libInfoVersion.getPreReleaseId() != null) {
            builder.setPrereleaseId(libInfoVersion.getPreReleaseId());
        }
        return builder.build();
    }

    void destroySession(@NonNull String sessionId) {
        CapabilitySession session = SessionManager.INSTANCE.getSession(sessionId);
        if (session != null) {
            session.destroy();
        }
        SessionManager.INSTANCE.removeSession(sessionId);
    }

    @NonNull
    StatusRuntimeException convertToGrpcException(CapabilityExecutionException e) {
        if (e.getErrorStatus() == ErrorStatusInternal.TIMEOUT) {
            return new StatusRuntimeException(
                    Status.DEADLINE_EXCEEDED.withDescription(e.getMessage()).withCause(e));
        }
        return new StatusRuntimeException(
                Status.INTERNAL.withDescription(e.getMessage()).withCause(e));
    }

    @NonNull
    Response convertFulfillmentResponse(
            @NonNull FulfillmentResponse fulfillmentResponse,
            @NonNull Capability capability) {
        AppActionsContext.AppAction appAction = capability.getAppAction();
        boolean isDialogSession = appAction.getTaskInfo().getSupportsPartialFulfillment();
        Version version = convertToAppActionsContextVersion(
                new LibInfo(mAppInteractionService.getApplicationContext()).getVersion());
        Response.Builder responseBuilder =
                // TODO(b/269638788): Add DialogState to the Response proto.
                Response.newBuilder()
                        .setFulfillmentResponse(fulfillmentResponse)
                        .setAppActionsContext(
                                AppActionsContext.newBuilder()
                                        .addActions(appAction)
                                        .setVersion(version)
                                        .build());
        if (!isDialogSession) {
            responseBuilder.setEndingStatus(
                    AppInteractionServiceProto.Status.newBuilder()
                            .setStatusCode(Code.COMPLETE)
                            .build());
        }
        return responseBuilder.build();
    }

    @NonNull
    ListenableFuture<FulfillmentResponse> executeFulfillmentRequest(
            @NonNull CapabilitySession session,
            @NonNull FulfillmentRequest.Fulfillment fulfillmentRequest) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    session.execute(
                            ArgumentsWrapper.create(fulfillmentRequest),
                            new CapabilityCallback(completer));
                    return "executing action capability";
                });
    }

    static <T> void respondAndComplete(T response, StreamObserver<T> responseObserver) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}