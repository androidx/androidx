/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.EventBuilders.TileAddEvent;
import androidx.wear.tiles.EventBuilders.TileEnterEvent;
import androidx.wear.tiles.EventBuilders.TileLeaveEvent;
import androidx.wear.tiles.EventBuilders.TileRemoveEvent;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.ResourceBuilders.Resources;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.TileBuilders.Version;
import androidx.wear.tiles.proto.EventProto;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.proto.TileProto;
import androidx.wear.tiles.protobuf.InvalidProtocolBufferException;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Base class for a service providing data for an app tile.
 *
 * <p>A provider service must implement {@link #onTileRequest} and {@link #onResourcesRequest} to
 * respond to requests for updates from the system.
 *
 * <p>The manifest declaration of this service must include an intent filter for {@link
 * #ACTION_BIND_TILE_PROVIDER}.
 *
 * <p>The manifest entry should also include {@code
 * android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER"} to ensure that
 * only the system can bind to it.
 */
public abstract class TileProviderService extends Service {

    private static final String TAG = "TileProviderService";

    /**
     * The intent action used to send update requests to the provider. Tile provider services must
     * declare an intent filter for this action in the manifest.
     */
    public static final String ACTION_BIND_TILE_PROVIDER =
            "androidx.wear.tiles.action.BIND_TILE_PROVIDER";

    /** The ID for the Intent extra containing the ID of the Clickable. */
    public static final String EXTRA_CLICKABLE_ID = "androidx.wear.tiles.extra.CLICKABLE_ID";

    /**
     * The name of the metadata key that should contain a drawable to be presented as a Tile
     * preview.
     */
    public static final String METADATA_PREVIEW_KEY = "androidx.wear.tiles.PREVIEW";

    /**
     * Called when the system is requesting a new timeline from this Tile Provider. The returned
     * future must complete after at most 10 seconds from the moment this method is called (exact
     * timeout length subject to change).
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileRequest} for more info.
     */
    @MainThread
    @NonNull
    protected abstract ListenableFuture<Tile> onTileRequest(@NonNull TileRequest requestParams);

    /**
     * Called when the system is requesting a resource bundle from this Tile Provider. The returned
     * future must complete after at most 10 seconds from the moment this method is called (exact
     * timeout length subject to change).
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link ResourcesRequest} for more
     *     info.
     */
    @MainThread
    @NonNull
    protected abstract ListenableFuture<Resources> onResourcesRequest(
            @NonNull ResourcesRequest requestParams);

    /**
     * Called when a tile provided by this Tile Provider is added to the carousel.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileAddEvent} for more info.
     */
    @MainThread
    protected void onTileAddEvent(@NonNull TileAddEvent requestParams) {}

    /**
     * Called when a tile provided by this Tile Provider is removed from the carousel.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileRemoveEvent} for more info.
     */
    @MainThread
    protected void onTileRemoveEvent(@NonNull TileRemoveEvent requestParams) {}

    /**
     * Called when a tile provided by this Tile Provider becomes into view, on screen.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileEnterEvent} for more info.
     */
    @MainThread
    protected void onTileEnterEvent(@NonNull TileEnterEvent requestParams) {}

    /**
     * Called when a tile provided by this Tile Provider goes out of view, on screen.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileLeaveEvent} for more info.
     */
    @MainThread
    protected void onTileLeaveEvent(@NonNull TileLeaveEvent requestParams) {}

    /**
     * Gets an instance of {@link TileUpdateRequester} to allow a Tile Provider to notify the tile's
     * renderer that it should request a new Timeline from this {@link TileProviderService}.
     *
     * @param context The application context.
     */
    @NonNull
    public static TileUpdateRequester getUpdater(@NonNull Context context) {
        // TODO(b/181747932): Detect which UpdateRequester to use rather than dispatching using
        // both.
        List<TileUpdateRequester> requesters = new ArrayList<>();
        requesters.add(new SysUiTileUpdateRequester(context));
        requesters.add(new ViewerTileUpdateRequester(context));

        return new CompositeTileUpdateRequester(requesters);
    }

    private TileProvider.Stub mBinder;

    @Override
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        if (ACTION_BIND_TILE_PROVIDER.equals(intent.getAction())) {
            if (mBinder == null) {
                mBinder = new TileProviderWrapper(this, new Handler(getMainLooper()));
            }
            return mBinder;
        }
        return null;
    }

    @SuppressWarnings("ExecutorTaskName")
    private static class TileProviderWrapper extends TileProvider.Stub {

        private final WeakReference<TileProviderService> mServiceRef;
        private final Handler mHandler;

        TileProviderWrapper(TileProviderService tileProviderService, Handler handler) {
            mServiceRef = new WeakReference<>(tileProviderService);
            this.mHandler = handler;
        }

        @Override
        public int getApiVersion() {
            return TileProvider.API_VERSION;
        }

        @Override
        public void onTileRequest(
                int tileId, TileRequestData requestParams, TileCallback callback) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();
                        if (tileProviderService != null) {
                            if (requestParams.getVersion() != TileRequestData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileRequestData had unexpected version: "
                                                + requestParams.getVersion());
                                return;
                            }

                            TileRequest tileRequest;

                            try {
                                tileRequest =
                                        TileRequest.fromProto(
                                                RequestProto.TileRequest.parseFrom(
                                                        requestParams.getContents()));
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileRequest payload.", ex);
                                return;
                            }

                            ListenableFuture<Tile> tileFuture =
                                    tileProviderService.onTileRequest(tileRequest);

                            tileFuture.addListener(
                                    () -> {
                                        try {
                                            // Inject the current schema version.
                                            TileProto.Tile tile =
                                                    tileFuture.get().toProto().toBuilder()
                                                            .setSchemaVersion(Version.CURRENT)
                                                            .build();

                                            callback.updateTileData(
                                                    new TileData(
                                                            tile.toByteArray(),
                                                            TileData.VERSION_PROTOBUF));
                                        } catch (ExecutionException | InterruptedException ex) {
                                            Log.e(TAG, "onTileRequest Future failed", ex);
                                        } catch (RemoteException ex) {
                                            Log.e(
                                                    TAG,
                                                    "RemoteException while returning tile payload",
                                                    ex);
                                        }
                                    },
                                    mHandler::post);
                        }
                    });
        }

        @Override
        public void onResourcesRequest(
                int tileId, ResourcesRequestData requestParams, ResourcesCallback callback) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();
                        if (tileProviderService != null) {
                            if (requestParams.getVersion()
                                    != ResourcesRequestData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "ResourcesRequestData had unexpected version: "
                                                + requestParams.getVersion());
                                return;
                            }

                            ResourcesRequest req;

                            try {
                                req =
                                        ResourcesRequest.fromProto(
                                                RequestProto.ResourcesRequest.parseFrom(
                                                        requestParams.getContents()));
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing ResourcesRequest payload.", ex);
                                return;
                            }

                            ListenableFuture<Resources> resourcesFuture =
                                    tileProviderService.onResourcesRequest(req);

                            resourcesFuture.addListener(
                                    () -> {
                                        try {
                                            callback.updateResources(
                                                    new ResourcesData(
                                                            resourcesFuture
                                                                    .get()
                                                                    .toProto()
                                                                    .toByteArray(),
                                                            ResourcesData.VERSION_PROTOBUF));
                                        } catch (ExecutionException | InterruptedException ex) {
                                            Log.e(TAG, "onResourcesRequest Future failed", ex);
                                        } catch (RemoteException ex) {
                                            Log.e(
                                                    TAG,
                                                    "RemoteException while returning resources"
                                                            + " payload",
                                                    ex);
                                        }
                                    },
                                    mHandler::post);
                        }
                    });
        }

        @Override
        public void onTileAddEvent(TileAddEventData data) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();

                        if (tileProviderService != null) {
                            if (data.getVersion() != TileAddEventData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileAddEventData had unexpected version: "
                                                + data.getVersion());
                                return;
                            }

                            try {
                                TileAddEvent evt =
                                        TileAddEvent.fromProto(
                                                EventProto.TileAddEvent.parseFrom(
                                                        data.getContents()));
                                tileProviderService.onTileAddEvent(evt);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileAddEvent payload.", ex);
                            }
                        }
                    });
        }

        @Override
        public void onTileRemoveEvent(TileRemoveEventData data) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();

                        if (tileProviderService != null) {
                            if (data.getVersion() != TileRemoveEventData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileRemoveEventData had unexpected version: "
                                                + data.getVersion());
                                return;
                            }

                            try {
                                TileRemoveEvent evt =
                                        TileRemoveEvent.fromProto(
                                                EventProto.TileRemoveEvent.parseFrom(
                                                        data.getContents()));
                                tileProviderService.onTileRemoveEvent(evt);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileRemoveEvent payload.", ex);
                            }
                        }
                    });
        }

        @Override
        public void onTileEnterEvent(TileEnterEventData data) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();

                        if (tileProviderService != null) {
                            if (data.getVersion() != TileEnterEventData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileEnterEventData had unexpected version: "
                                                + data.getVersion());
                                return;
                            }

                            try {
                                TileEnterEvent evt =
                                        TileEnterEvent.fromProto(
                                                EventProto.TileEnterEvent.parseFrom(
                                                        data.getContents()));
                                tileProviderService.onTileEnterEvent(evt);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileEnterEvent payload.", ex);
                            }
                        }
                    });
        }

        @Override
        public void onTileLeaveEvent(TileLeaveEventData data) {
            mHandler.post(
                    () -> {
                        TileProviderService tileProviderService = mServiceRef.get();

                        if (tileProviderService != null) {
                            if (data.getVersion() != TileLeaveEventData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileLeaveEventData had unexpected version: "
                                                + data.getVersion());
                                return;
                            }

                            try {
                                TileLeaveEvent evt =
                                        TileLeaveEvent.fromProto(
                                                EventProto.TileLeaveEvent.parseFrom(
                                                        data.getContents()));
                                tileProviderService.onTileLeaveEvent(evt);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileLeaveEvent payload.", ex);
                            }
                        }
                    });
        }
    }
}
