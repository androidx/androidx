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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.expression.proto.VersionProto.VersionInfo;
import androidx.wear.protolayout.proto.DeviceParametersProto.DeviceParameters;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;
import androidx.wear.tiles.EventBuilders.TileAddEvent;
import androidx.wear.tiles.EventBuilders.TileEnterEvent;
import androidx.wear.tiles.EventBuilders.TileLeaveEvent;
import androidx.wear.tiles.EventBuilders.TileRemoveEvent;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.TileBuilders.Version;
import androidx.wear.tiles.proto.EventProto;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.proto.TileProto;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.wear.Sdk;
import com.google.wear.services.tiles.TileInstance;
import com.google.wear.services.tiles.TilesManager;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Base class for a service providing data for an app tile.
 *
 * <p>A provider service must implement {@link #onTileRequest} and {@link #onTileResourcesRequest}
 * to respond to requests for updates from the system.
 *
 * <p>The manifest declaration of this service must include an intent filter for {@link
 * #ACTION_BIND_TILE_PROVIDER}.
 *
 * <p>The manifest entry should also include {@code
 * android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER"} to ensure that
 * only the system can bind to it.
 */
public abstract class TileService extends Service {
    private static final String TAG = "TileService";
    static final VersionInfo DEFAULT_VERSION =
            VersionInfo.newBuilder().setMajor(1).setMinor(0).build();

    @SuppressWarnings("deprecation") // For backward compatibility
    private static final ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources>
            ON_RESOURCES_REQUEST_NOT_IMPLEMENTED =
                    createFailedFuture(
                            new UnsupportedOperationException(
                                    "onResourcesRequest not implemented"));

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
     * Name of the SharedPreferences file used for getting the preferences from the application
     * context. The preferences are shared by all TileService implementations from the same app and
     * store information regarding the tiles considered to be active. The SharedPreferences key is
     * the result retrieved from {@link ActiveTileIdentifier#flattenToString} and the value is a
     * timestamp.
     *
     * <p>The presence of a tile in the SharedPreferences means the tile instance is considered to
     * be active (in the carousel). An entry will not be added with an invalid timestamp. The
     * timestamp value is from when the entry was first recorded and is updated once every {@code
     * UPDATE_TILE_TIMESTAMP_PERIOD_MS} on user interactions to indicate it is still active. If the
     * timestamp hasn't been updated for longer than {@code INACTIVE_TILE_PERIOD_MS} the tile will
     * be considered inactive and will be removed from the preferences so that entries are not left
     * in the app's storage indefinitely if an {@link TileService#onTileRemoveEvent} callback,
     * signaling the tile has become inactive, is missed.
     */
    private static final String ACTIVE_TILES_SHARED_PREF_NAME = "active_tiles_shared_preferences";

    /**
     * 1 day in milliseconds for the timestamp refresh period indicating the tile instance stored in
     * {@code ACTIVE_TILES_SHARED_PREF_NAME} is still active if the tile is acted upon.
     */
    private static final long UPDATE_TILE_TIMESTAMP_PERIOD_MS = Duration.ofDays(1).toMillis();

    /**
     * 60 days in milliseconds for the period after which a tile instances will be removed from
     * {@code ACTIVE_TILES_SHARED_PREF_NAME} if timestamp has not been updated since.
     */
    private static final long INACTIVE_TILE_PERIOD_MS = Duration.ofDays(60).toMillis();

    private static final TimeSourceClockImpl sTimeSourceClock = new TimeSourceClockImpl();

    private static Boolean sUseWearSdkImpl;

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
     * @deprecated Use {@link #onTileResourcesRequest} instead.
     */
    @MainThread
    @NonNull
    @Deprecated
    protected ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources> onResourcesRequest(
            @NonNull ResourcesRequest requestParams) {
        return ON_RESOURCES_REQUEST_NOT_IMPLEMENTED;
    }

    /**
     * Called when the system is requesting a resource bundle from this Tile Provider. This can
     * happen on the first time a Tile is being loaded or whenever the resource version requested by
     * a Tile (in {@link #onTileRequest}) changes.
     *
     * <p>The returned future must complete after at most 10 seconds from the moment this method is
     * called (exact timeout length subject to change).
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     * If {@link #onTileResourcesRequest} is not implemented, the {@link TileService} will fallback
     * to {@link #onResourcesRequest}.
     *
     * @param requestParams Parameters about the request. See {@link ResourcesRequest} for more
     *     info.
     */
    @MainThread
    @NonNull
    @SuppressWarnings({"AsyncSuffixFuture", "deprecation"}) // For backward compatibility
    protected ListenableFuture<Resources> onTileResourcesRequest(
            @NonNull ResourcesRequest requestParams) {
        // We are offering a default implementation for onTileResourcesRequest for backward
        // compatibility as older clients are overriding onResourcesRequest.
        ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources>
                legacyResourcesRequestResult = onResourcesRequest(requestParams);
        if (legacyResourcesRequestResult == ON_RESOURCES_REQUEST_NOT_IMPLEMENTED) {
            return createFailedFuture(
                    new UnsupportedOperationException(
                            "onTileResourcesRequest " + "not implemented."));
        }

        ResolvableFuture<Resources> result = ResolvableFuture.create();
        legacyResourcesRequestResult.addListener(
                () -> {
                    try {
                        result.set(
                                Resources.fromProto(legacyResourcesRequestResult.get().toProto()));
                    } catch (RuntimeException | InterruptedException | ExecutionException e) {
                        result.setException(e);
                    }
                },
                Runnable::run);
        return result;
    }

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
     * renderer that it should request a new Timeline from this {@link TileService}.
     *
     * @param context The application context.
     */
    @NonNull
    public static TileUpdateRequester getUpdater(@NonNull Context context) {

        List<TileUpdateRequester> requesters = new ArrayList<>();
        requesters.add(new SysUiTileUpdateRequester(context));
        requesters.add(new ViewerTileUpdateRequester(context));

        return new CompositeTileUpdateRequester(requesters);
    }

    /**
     * Returns the list of active tiles belonging to the passed {@code context}'s package name. A
     * tile is considered to be active if it is present in the carousel.
     *
     * <p>The result reflects the state of carousel at the time the call was made, which might've
     * changed by the time the result is received. {@link TileService#onTileAddEvent} and {@link
     * TileService#onTileRemoveEvent} should be used instead for live updates.
     *
     * <p>Compatibility behavior:
     *
     * <p>On SDKs older than U, this method is a best-effort to match platform behavior, but may not
     * always return all tiles present in the carousel. The possibly omitted tiles being the
     * pre-installed tiles, all tiles if the user has cleared the app data, or the tiles a user
     * hasn't visited in the last 60 days, while tiles removed by an app update may be shown as
     * active for 60 days afterwards.
     *
     * @param context The application context.
     * @param executor The executor on which methods should be invoked. To dispatch events through
     *     the main thread of your application, you can use {@link Context#getMainExecutor()}.
     * @return A list of {@link ActiveTileIdentifier} for the tiles belonging to the passed {@code
     *     context} present in the carousel, or a value based on platform-specific fallback
     *     behavior.
     */
    @NonNull
    public static ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsync(
            @NonNull Context context, @NonNull Executor executor) {
        if (useWearSdkImpl(context)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return Api34Impl.getActiveTilesAsync(
                    Sdk.getWearManager(context, TilesManager.class), executor);
        }
        return getActiveTilesAsyncLegacy(context, executor, sTimeSourceClock);
    }

    @VisibleForTesting
    @NonNull
    static ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsyncLegacy(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull TimeSourceClock timeSourceClock) {
        return readActiveTilesSharedPrefLegacy(
                getActiveTilesSharedPrefLegacy(context),
                context.getPackageName(),
                executor,
                timeSourceClock);
    }

    TimeSourceClock getTimeSourceClock() {
        return sTimeSourceClock;
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

        private final WeakReference<TileService> mServiceRef;
        private final Handler mHandler;

        TileProviderWrapper(TileService tileService, Handler handler) {
            mServiceRef = new WeakReference<>(tileService);
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
                        TileService tileService = mServiceRef.get();
                        if (tileService != null) {
                            if (requestParams.getVersion() != TileRequestData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "TileRequestData had unexpected version: "
                                                + requestParams.getVersion());
                                return;
                            }
                            tileService.markTileAsActiveLegacy(tileId);
                            TileRequest tileRequest;

                            try {
                                RequestProto.TileRequest tileRequestProto =
                                        RequestProto.TileRequest.parseFrom(
                                                requestParams.getContents());

                                RequestProto.TileRequest.Builder tileRequestProtoBuilder =
                                        tileRequestProto.toBuilder();
                                tileRequestProtoBuilder.setTileId(tileId);

                                // If schema version is missing, go and fill it back in again.
                                // Explicitly check that device_config is set though. If not, then
                                // skip entirely.
                                if (tileRequestProto.hasDeviceConfiguration()
                                        && !tileRequestProto
                                                .getDeviceConfiguration()
                                                .hasRendererSchemaVersion()) {
                                    DeviceParameters deviceParams =
                                            tileRequestProto.getDeviceConfiguration().toBuilder()
                                                    .setRendererSchemaVersion(DEFAULT_VERSION)
                                                    .build();
                                    tileRequestProtoBuilder.setDeviceConfiguration(deviceParams);
                                }

                                tileRequest =
                                        TileRequest.fromProto(tileRequestProtoBuilder.build());
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileRequest payload.", ex);
                                return;
                            }

                            ListenableFuture<Tile> tileFuture =
                                    tileService.onTileRequest(tileRequest);

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
                                        } catch (ExecutionException
                                                | InterruptedException
                                                | CancellationException ex) {
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
        @SuppressWarnings("deprecation") // for backward compatibility
        public void onResourcesRequest(
                int tileId, ResourcesRequestData requestParams, ResourcesCallback callback) {
            mHandler.post(
                    () -> {
                        TileService tileService = mServiceRef.get();
                        if (tileService != null) {
                            if (requestParams.getVersion()
                                    != ResourcesRequestData.VERSION_PROTOBUF) {
                                Log.e(
                                        TAG,
                                        "ResourcesRequestData had unexpected version: "
                                                + requestParams.getVersion());
                                return;
                            }
                            tileService.markTileAsActiveLegacy(tileId);

                            ResourcesRequest req;

                            try {
                                RequestProto.ResourcesRequest resourcesRequestProto =
                                        RequestProto.ResourcesRequest.parseFrom(
                                                requestParams.getContents());

                                RequestProto.ResourcesRequest.Builder resourcesRequestProtoBuilder =
                                        resourcesRequestProto.toBuilder();
                                resourcesRequestProtoBuilder.setTileId(tileId);

                                if (resourcesRequestProto.hasDeviceConfiguration()
                                        && !resourcesRequestProto
                                                .getDeviceConfiguration()
                                                .hasRendererSchemaVersion()) {
                                    DeviceParameters deviceParams =
                                            resourcesRequestProto
                                                    .getDeviceConfiguration()
                                                    .toBuilder()
                                                    .setRendererSchemaVersion(DEFAULT_VERSION)
                                                    .build();
                                    resourcesRequestProtoBuilder.setDeviceConfiguration(
                                            deviceParams);
                                }

                                req =
                                        ResourcesRequest.fromProto(
                                                resourcesRequestProtoBuilder.build());
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing ResourcesRequest payload.", ex);
                                return;
                            }

                            ListenableFuture<Resources> resourcesFuture =
                                    tileService.onTileResourcesRequest(req);

                            if (resourcesFuture.isDone()) {
                                try {
                                    Resources resources = resourcesFuture.get();
                                    updateResources(callback, resources.toProto().toByteArray());
                                } catch (ExecutionException
                                        | InterruptedException
                                        | CancellationException ex) {
                                    Log.e(TAG, "onTileResourcesRequest Future failed", ex);
                                }
                            } else {
                                resourcesFuture.addListener(
                                        () -> {
                                            try {
                                                updateResources(
                                                        callback,
                                                        resourcesFuture
                                                                .get()
                                                                .toProto()
                                                                .toByteArray());
                                            } catch (ExecutionException
                                                    | InterruptedException
                                                    | CancellationException ex) {
                                                Log.e(
                                                        TAG,
                                                        "onTileResourcesRequest Future failed",
                                                        ex);
                                            }
                                        },
                                        mHandler::post);
                            }
                        }
                    });
        }

        @Override
        public void onTileAddEvent(TileAddEventData data) {
            mHandler.post(
                    () -> {
                        TileService tileService = mServiceRef.get();

                        if (tileService != null) {
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
                                tileService.markTileAsActiveLegacy(evt.getTileId());
                                tileService.onTileAddEvent(evt);
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
                        TileService tileService = mServiceRef.get();

                        if (tileService != null) {
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

                                tileService.markTileAsInactiveLegacy(evt.getTileId());
                                tileService.onTileRemoveEvent(evt);
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
                        TileService tileService = mServiceRef.get();

                        if (tileService != null) {
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
                                tileService.markTileAsActiveLegacy(evt.getTileId());
                                tileService.onTileEnterEvent(evt);
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
                        TileService tileService = mServiceRef.get();

                        if (tileService != null) {
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
                                tileService.markTileAsActiveLegacy(evt.getTileId());
                                tileService.onTileLeaveEvent(evt);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileLeaveEvent payload.", ex);
                            }
                        }
                    });
        }
    }

    static void updateResources(ResourcesCallback callback, byte[] resources) {
        try {
            callback.updateResources(new ResourcesData(resources, ResourcesData.VERSION_PROTOBUF));
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while returning resources payload", ex);
        }
    }

    private static <T> ListenableFuture<T> createFailedFuture(@NonNull Throwable throwable) {
        ResolvableFuture<T> errorFuture = ResolvableFuture.create();
        errorFuture.setException(throwable);
        return errorFuture;
    }

    private static boolean packageNameMatches(
            String packageName, List<ActiveTileIdentifier> activeTileIdentifiers) {
        return activeTileIdentifiers.stream()
                .allMatch(i -> i.getComponentName().getPackageName().equals(packageName));
    }

    private static Boolean useWearSdkImpl(Context context) {
        if (sUseWearSdkImpl == null) {
            setUseWearSdkImpl(context);
        }
        return sUseWearSdkImpl;
    }

    private static void setUseWearSdkImpl(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            sUseWearSdkImpl = (Sdk.getWearManager(context, TilesManager.class) != null);
            return;
        }
        sUseWearSdkImpl = false;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public static void setUseWearSdkImpl(boolean value) {
        sUseWearSdkImpl = value;
    }

    @RequiresApi(34)
    private static class Api34Impl {
        @NonNull
        static ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsync(
                @NonNull TilesManager tilesManager, @NonNull Executor executor) {
            return CallbackToFutureAdapter.getFuture(
                    completer -> {
                        tilesManager.getActiveTiles(
                                executor,
                                new OutcomeReceiver<List<TileInstance>, Exception>() {
                                    @Override
                                    public void onResult(List<TileInstance> tileInstanceList) {
                                        completer.set(
                                                tileInstanceToActiveTileIdentifier(
                                                        tileInstanceList));
                                    }

                                    @Override
                                    public void onError(@NonNull Exception error) {
                                        completer.setException(error.getCause());
                                    }
                                });
                        return "getActiveTilesAsync";
                    });
        }
    }

    private static List<ActiveTileIdentifier> tileInstanceToActiveTileIdentifier(
            @NonNull List<TileInstance> tileInstanceList) {
        return tileInstanceList.stream()
                .map(
                        i ->
                                new ActiveTileIdentifier(
                                        i.getTileProvider().getComponentName(), i.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Mark tile instance as active by adding it to the {@code ACTIVE_TILES_SHARED_PREF_NAME} shared
     * preferences if it doesn't already exist. If the tile instance is already present the
     * timestamp is updated if necessary to indicate the tile is still active.
     *
     * <p>This method is called from {@link TileService#onTileAddEvent}, {@link
     * TileService#onTileEnterEvent}, {@link TileService#onTileLeaveEvent}, {@link
     * TileService#onTileRequest}, {@link TileService#onTileResourcesRequest} when an interaction
     * with the tile is observed, indicating its presence in the carousel.
     */
    private void markTileAsActiveLegacy(int tileId) {
        if (!useWearSdkImpl(this)) {
            ComponentName componentName = new ComponentName(this, this.getClass().getName());
            DiskAccessAllowedPrefs sharedPref = getActiveTilesSharedPrefLegacy(this);
            cleanupActiveTilesSharedPrefLegacy(sharedPref, getTimeSourceClock());
            String key = new ActiveTileIdentifier(componentName, tileId).flattenToString();
            if (sharedPref.contains(key)
                    && !timestampNeedsUpdateLegacy(
                            sharedPref.getLong(key, -1L), getTimeSourceClock())) {
                return;
            }
            sharedPref.putLong(key, getTimeSourceClock().getCurrentTimestampMillis());
        }
    }

    /**
     * Mark tile instance as inactive by removing it from the {@code ACTIVE_TILES_SHARED_PREF_NAME}
     * shared preferences if it exists.
     *
     * <p>This method is called from {@link TileService#onTileRemoveEvent} when a tile instance is
     * removed from the carousel.
     */
    private void markTileAsInactiveLegacy(int tileId) {
        if (!useWearSdkImpl(this)) {
            DiskAccessAllowedPrefs sharedPref = getActiveTilesSharedPrefLegacy(this);
            String key =
                    new ActiveTileIdentifier(
                                    new ComponentName(this, this.getClass().getName()), tileId)
                            .flattenToString();
            if (!sharedPref.contains(key)) {
                return;
            }
            sharedPref.remove(key);
        }
    }

    /**
     * Clean-up method to remove entries with timestamps that haven't been updated for longer than
     * {@code INACTIVE_TILE_PERIOD_MS}. In such cases the tiles are considered inactive and will be
     * removed from the {@code ACTIVE_TILES_SHARED_PREF_NAME} preferences so that entries are not
     * left in the app's storage indefinitely if an {@link TileService#onTileRemoveEvent} callback,
     * signaling the tile was removed from the carousel, is missed.
     *
     * <p>This method is called on any user interactions with the tiles and before the
     * SharedPreferences are read.
     */
    private static void cleanupActiveTilesSharedPrefLegacy(
            @NonNull DiskAccessAllowedPrefs activeTilesSharedPref,
            @NonNull TimeSourceClock timeSourceClock) {
        for (String key : activeTilesSharedPref.getAll().keySet()) {
            if (isTileInactiveLegacy(activeTilesSharedPref.getLong(key, -1L), timeSourceClock)) {
                activeTilesSharedPref.remove(key);
            }
        }
    }

    private static ListenableFuture<List<ActiveTileIdentifier>> readActiveTilesSharedPrefLegacy(
            @NonNull DiskAccessAllowedPrefs activeTilesSharedPref,
            @NonNull String packageName,
            @NonNull Executor executor,
            @NonNull TimeSourceClock timeSourceClock) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    executor.execute(
                            () -> {
                                if (activeTilesSharedPref != null) {
                                    cleanupActiveTilesSharedPrefLegacy(
                                            activeTilesSharedPref, timeSourceClock);
                                    List<ActiveTileIdentifier> activeTilesList =
                                            activeTilesSharedPref.getAll().entrySet().stream()
                                                    .map(
                                                            entry ->
                                                                    ActiveTileIdentifier
                                                                            .unflattenFromString(
                                                                                    entry.getKey()))
                                                    .collect(Collectors.toList());
                                    if (!packageNameMatches(packageName, activeTilesList)) {
                                        completer.setException(
                                                new IllegalArgumentException(
                                                        "The information from the provided "
                                                                + "context doesn't match."));
                                    } else {
                                        completer.set(activeTilesList);
                                    }
                                } else {
                                    completer.setException(
                                            new IllegalArgumentException(
                                                    "The information from the provided "
                                                            + "context doesn't match."));
                                }
                            });

                    return "readActiveTilesSharedPrefLegacy";
                });
    }

    private static DiskAccessAllowedPrefs getActiveTilesSharedPrefLegacy(@NonNull Context context) {
        return DiskAccessAllowedPrefs.wrap(context, ACTIVE_TILES_SHARED_PREF_NAME);
    }

    /**
     * Returns true if the timestamp hasn't been updated for longer than {@code
     * UPDATE_TILE_TIMESTAMP_PERIOD_MS}. Returns false if the timestamp has been updated in the past
     * {@code UPDATE_TILE_TIMESTAMP_PERIOD_MS} or if the current time cannot be obtained.
     */
    private static boolean timestampNeedsUpdateLegacy(
            long timestampMs, @NonNull TimeSourceClock timeSourceClock) {
        return timeSourceClock.getCurrentTimestampMillis() - timestampMs
                >= UPDATE_TILE_TIMESTAMP_PERIOD_MS;
    }

    /**
     * Returns true if the timestamp hasn't been updated for longer than {@code
     * INACTIVE_TILE_PERIOD_MS}. Returns false if the timestamp has been updated in the past {@code
     * INACTIVE_TILE_PERIOD_MS} or if the current time cannot be obtained.
     */
    private static boolean isTileInactiveLegacy(
            long timestampMs, @NonNull TimeSourceClock timeSourceClock) {
        return timeSourceClock.getCurrentTimestampMillis() - timestampMs >= INACTIVE_TILE_PERIOD_MS;
    }

    interface TimeSourceClock {
        /** Returns time agnostic timestamp with the current time. */
        long getCurrentTimestampMillis();
    }

    static class TimeSourceClockImpl implements TimeSourceClock {
        @Override
        public long getCurrentTimestampMillis() {
            return System.currentTimeMillis();
        }
    }
}
