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

import static io.grpc.binder.ParcelableUtils.metadataKey;

import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * Server interceptor that copies the RemoteViews provided in context to metadata.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RemoteViewsOverMetadataInterceptor implements ServerInterceptor {

    private static final String KEY = "RemoteViews-bin";
    public static final Metadata.Key<RemoteViews> METADATA_KEY = metadataKey(KEY,
            RemoteViews.CREATOR);
    static final Context.Key<AtomicReference<RemoteViews>> RESPONSE = Context.key("RES-" + KEY);

    static void setRemoteViews(@NonNull RemoteViews remoteViews) {
        RESPONSE.get().set(remoteViews);
    }

    @Override
    @NonNull
    public <ReqT, RespT> Listener<ReqT> interceptCall(
            @NonNull ServerCall<ReqT, RespT> call,
            @NonNull Metadata headers,
            @NonNull ServerCallHandler<ReqT, RespT> next) {
        Context context = Context.current().withValue(RESPONSE, new AtomicReference<>());

        return Contexts.interceptCall(
                context,
                new SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        RemoteViews value = RESPONSE.get(context).get();
                        if (value != null) {
                            trailers.put(METADATA_KEY, value);
                        }
                        super.close(status, trailers);
                    }
                },
                headers,
                next);
    }
}
