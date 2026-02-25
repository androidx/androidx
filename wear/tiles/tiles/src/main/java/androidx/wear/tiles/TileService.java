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

import static java.util.stream.Collectors.toList;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.protolayout.ProtoLayoutScope;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.expression.VersionBuilders;
import androidx.wear.protolayout.expression.proto.VersionProto.VersionInfo;
import androidx.wear.protolayout.proto.DeviceParametersProto.DeviceParameters;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;
import androidx.wear.tiles.EventBuilders.TileAddEvent;
import androidx.wear.tiles.EventBuilders.TileEnterEvent;
import androidx.wear.tiles.EventBuilders.TileInteractionEvent;
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * The name of the metadata key that should contain the name of the group the tile belongs to.
     * If not set, the fully qualified class name of the TileService class will be used.
     *
     * <p>Tile providers in the same group represent the same tile on the device. Only one provider
     * service should be enabled at a time for a given group.
     *
     * <p>This can be used to replace which provider service is associated with a widget on the
     * device.
     *
     * <p> This attribute is only used on devices on API version 37 and above. For backwards
     * compatibility with services being used on older devices, the default value of the fully
     * qualified name of the older service should be used.
     */
    public static final String METADATA_GROUP_KEY = "androidx.wear.tiles.GROUP";

    /**
     * The name of the metadata key that contains the major schema version that the tile requires to
     * be rendered properly.
     *
     * <p>If the device's renderer schema version is lower than the one provided, the tile provider
     * will be ignored and not listed as a valid tile.
     *
     * <p>This metadata should be provided together with {@link #METADATA_SCHEMA_MINOR_VERSION_KEY}.
     */
    public static final String METADATA_SCHEMA_MAJOR_VERSION_KEY = "tiles_schema_major_version";

    /**
     * The name of the metadata key that contains the minor schema version that the tile requires to
     * be rendered properly.
     *
     * <p>If the device's renderer schema version is lower than the one provided, the tile provider
     * will be ignored and not listed as a valid tile.
     *
     * <p>This metadata should be provided together with {@link #METADATA_SCHEMA_MAJOR_VERSION_KEY}.
     */
    public static final String METADATA_SCHEMA_MINOR_VERSION_KEY = "tiles_schema_minor_version";

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
    @VisibleForTesting
    static final String ACTIVE_TILES_SHARED_PREF_NAME = "active_tiles_shared_preferences";

    /**
     * Name of the SharedPreferences file used for getting the preferences from the application
     * context. The preferences are shared by all TileService implementations from the same app and
     * store information regarding the tiles's resources once the TileService is about to be
     * destroyed so it can be retrieved once the Service is back on.
     */
    @VisibleForTesting
    static final String SAVED_RESOURCES_SHARED_PREF_NAME = "saved_resources_shared_preferences";

    /**
     * 1 day in milliseconds for the timestamp refresh period indicating the tile instance stored in
     * {@code ACTIVE_TILES_SHARED_PREF_NAME} is still active if the tile is acted upon.
     */
    private static final long UPDATE_TILE_TIMESTAMP_PERIOD_MS = Duration.ofDays(1).toMillis();

    private static final String ACTION_BIND_WIDGET_PROVIDER =
            "androidx.glance.wear.action.BIND_WIDGET_PROVIDER";

    /**
     * 60 days in milliseconds for the period after which a tile instances will be removed from
     * {@code ACTIVE_TILES_SHARED_PREF_NAME} if timestamp has not been updated since.
     */
    private static final long INACTIVE_TILE_PERIOD_MS = Duration.ofDays(60).toMillis();

    private static final TimeSourceClockImpl sTimeSourceClock = new TimeSourceClockImpl();

    private static Boolean sUseWearSdkImpl;

    /**
     * Pattern for matching the resources version.
     *
     * <p>The saved resources version is expected to be in the format of "tileId;string;hashCode"
     * where the tileId is an integer value.
     */
    private static final Pattern RESOURCES_VERSION_PATTERN =
            Pattern.compile("^(-?\\d+);[^;]+;-?\\d+$");

    /**
     * Map of all {@link Resources} for the tile instances that have requested new layout, but are
     * not yet requested resources (i.e. on older renderers, who don't handle it in one call).
     *
     * <p>This field is **not** thread safe and should only be accessed from main thread.
     */
    @VisibleForTesting
    final Map<String, Resources> mResourcesToSend = new HashMap<>();

    /**
     * Map of all resources version counters for the tile instances that have requested new layout,
     * but are not yet requested resources (i.e. on older renderers, who don't handle it in one
     * call).
     *
     * <p>This field is **not** thread safe and should only be accessed from main thread.
     *
     * <p>The key is a combination of the resources version and a counter, formatted as
     * "resourcesVersion;counter". The value is the number of times this specific resources version
     * has been requested for a given tile instance.
     *
     * <p>The `resourcesVersion` part of the key is expected to be in the format
     * "tileId;string;hashCode". Since the `tileId` is included, different tile instances, even if
     * they generate the same underlying resource content, will have distinct keys in this map, thus
     * maintaining separate counters.
     */
    @VisibleForTesting
    final Map<String, Integer> mResourcesToSendCounter = new HashMap<>();

    /**
     * Shared preferences for saved resources.
     *
     * <p>This field will be initialized lazily when needed.
     */
    private @Nullable DiskAccessAllowedPrefs mSavedResourcesSharedPref = null;

    /**
     * Returns saved {@link Resources} for the tile instance with the given ID that should be
     * sent for resources request as layout has already been requested. If no resources  are
     * saved, scope wasn't used or tile didn't have its layout requested, return null.
     *
     * <p>This method is not thread safe and should be called only from main thread.
     */
    @SuppressWarnings("RestrictedApiAndroidX") // Tiles is allowed to use ProtoLayout's APIs
    @MainThread
    @Nullable Resources getSavedResources(String resourcesVersion) {
        String resourcesVersionCounter = getSavedResourcesCounterKey(resourcesVersion);

        loadSavedResourcesCounter(resourcesVersionCounter);
        int updatedCounter =
                mResourcesToSendCounter.compute(
                        resourcesVersionCounter, (unusedKey, value) -> value - 1);

        if (updatedCounter < 0) {
            // No resources available for this version, this should never happen.
            Log.e(TAG, "No resources available for version: " + resourcesVersion);
            mResourcesToSendCounter.remove(resourcesVersionCounter);
            return null;
        }

        DiskAccessAllowedPrefs sharedPref = getSavedResourcesSharedPref();
        if (updatedCounter > 0) {
            sharedPref.putInt(resourcesVersionCounter, updatedCounter);
        }

        Resources resource =
                mResourcesToSend.computeIfAbsent(
                        resourcesVersion,
                        (key) -> {
                            try {
                                return Resources.fromProto(
                                        ResourceProto.Resources.parseFrom(
                                                Base64.decode(
                                                        sharedPref.getString(
                                                                key, /* defValue= */ ""),
                                                        Base64.DEFAULT)));
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing Resources payload.", ex);
                                return null;
                            }
                        });

        // Remove if there was any saved on disk.
        if (updatedCounter == 0 || resource == null) {
            removeSavedResources(resourcesVersion);
        }

        return resource;
    }

    /**
     * Saves the given {@link Resources} for the tile instance with the given ID.
     *
     * <p>This method is not thread safe and should be called only from main thread.
     */
    @MainThread
    void saveResources(@NonNull Resources resources) {
        String resourcesVersion = resources.getVersion();
        String resourcesVersionCounter = getSavedResourcesCounterKey(resourcesVersion);

        loadSavedResourcesCounter(resourcesVersionCounter);
        int updatedCounter =
                mResourcesToSendCounter.compute(
                        resourcesVersionCounter, (unusedKey, value) -> value + 1);

        DiskAccessAllowedPrefs sharedPref = getSavedResourcesSharedPref();
        sharedPref.putInt(resourcesVersionCounter, updatedCounter);

        // Save in memory
        mResourcesToSend.put(resourcesVersion, resources);
        if (updatedCounter > 1) {
            // We already have resources cached for this version, nothing to do.
            return;
        }

        // Save on disk if service gets destroyed
        sharedPref.putString(
                /* key= */ resourcesVersion,
                /* value= */ Base64.encodeToString(
                        resources.toProto().toByteArray(), Base64.DEFAULT));
    }

    /**
     * Loads the counter for the given resources version.
     *
     * <p>If the counter doesn't exist, it will be created with a value of 0.
     */
    private void loadSavedResourcesCounter(String resourcesVersionCounter) {
        mResourcesToSendCounter.computeIfAbsent(
                resourcesVersionCounter, (key) -> getSavedResourcesSharedPref().getInt(key, 0));
    }

    /** Returns the counter key for the given resources version. */
    private String getSavedResourcesCounterKey(String resourcesVersion) {
        return resourcesVersion + ";counter";
    }

    /** Removes the saved resources for the given resources version. */
    private void removeSavedResources(String resourcesVersion) {
        DiskAccessAllowedPrefs sharedPref = getSavedResourcesSharedPref();

        String resourcesVersionCounter = getSavedResourcesCounterKey(resourcesVersion);
        mResourcesToSendCounter.remove(resourcesVersionCounter);
        sharedPref.remove(resourcesVersionCounter);

        mResourcesToSend.remove(resourcesVersion);
        sharedPref.remove(resourcesVersion);
    }

    /** Clears all resources associated with the given tile ID. */
    private void clearSavedResources(int tileId) {
        DiskAccessAllowedPrefs sharedPrefs = getSavedResourcesSharedPref();
        String prefix = tileId + ";";
        sharedPrefs.getAll().keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .forEach(this::removeSavedResources);
    }

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
    protected abstract @NonNull ListenableFuture<Tile> onTileRequest(
            @NonNull TileRequest requestParams);

    /**
     * Called when the system is requesting a resource bundle from this Tile Provider. The returned
     * future must complete after at most 10 seconds from the moment this method is called (exact
     * timeout length subject to change).
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link ResourcesRequest} for more
     *                      info.
     * @deprecated Use {@link #onTileResourcesRequest} instead.
     */
    @MainThread
    @Deprecated
    protected @NonNull ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources>
    onResourcesRequest(@NonNull ResourcesRequest requestParams) {
        return ON_RESOURCES_REQUEST_NOT_IMPLEMENTED;
    }

    /**
     * Called when the system is requesting a resource bundle from this Tile Provider. This can
     * happen on the first time a Tile is being loaded or whenever the resource version requested by
     * a Tile (in {@link #onTileRequest}) changes.
     *
     * <p>If using methods that accept {@link ProtoLayoutScope} for creating image elements, this
     * method shouldn't be implemented as resources bundle will be automatically registered by the
     * {@link ProtoLayoutScope} itself.
     *
     * <p>The returned future must complete after at most 10 seconds from the moment this method is
     * called (exact timeout length subject to change).
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * <p>If {@link #onTileResourcesRequest} is not implemented, the {@link TileService} will
     * fallback to {@link #onResourcesRequest}.
     *
     * @param requestParams Parameters about the request. See {@link ResourcesRequest} for more
     *                      info.
     */
    @MainThread
    @SuppressWarnings({"AsyncSuffixFuture", "deprecation", "RestrictedApiAndroidX"})
    // For backward compatibility
    // Tiles is allowed to use ProtoLayout's APIs
    protected @NonNull ListenableFuture<Resources> onTileResourcesRequest(
            @NonNull ResourcesRequest requestParams) {
        // We are offering a default implementation for onTileResourcesRequest for backward
        // compatibility as older clients are overriding onResourcesRequest.
        ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources>
                legacyResourcesRequestResult = onResourcesRequest(requestParams);
        if (legacyResourcesRequestResult == ON_RESOURCES_REQUEST_NOT_IMPLEMENTED) {
            Log.w(
                    TAG,
                    "onTileResourcesRequest not implemented but it was called as tile is not using"
                            + " ProtoLayoutScope concept. Tile won't show any resources.");
            return createImmediateFuture(
                    new Resources.Builder().setVersion(requestParams.getVersion()).build());
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
    protected void onTileAddEvent(@NonNull TileAddEvent requestParams) {
    }

    /**
     * Called when a tile provided by this Tile Provider is removed from the carousel.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileRemoveEvent} for more info.
     */
    @MainThread
    protected void onTileRemoveEvent(@NonNull TileRemoveEvent requestParams) {
    }

    /**
     * Called when a tile provided by this Tile Provider becomes into view, on screen.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileEnterEvent} for more info.
     * @deprecated use {@link #onRecentInteractionEventsAsync(List)}.
     */
    @MainThread
    @Deprecated
    protected void onTileEnterEvent(@NonNull TileEnterEvent requestParams) {
    }

    /**
     * Called when a tile provided by this Tile Provider goes out of view, on screen.
     *
     * <p>Note that this is called from your app's main thread, which is usually also the UI thread.
     *
     * @param requestParams Parameters about the request. See {@link TileLeaveEvent} for more info.
     * @deprecated use {@link #onRecentInteractionEventsAsync(List)}.
     */
    @MainThread
    @Deprecated
    protected void onTileLeaveEvent(@NonNull TileLeaveEvent requestParams) {
    }

    /**
     * Called when the system sends a batch of Tile interaction events that happened since the last
     * time this method was called. The time between calls to this method may vary, do not depend on
     * it for time-sensitive or critical tasks.
     *
     * <p>Interaction events represent user direct interaction with a tile, when a tile comes into
     * view (enter event) or when the tiles goes out of view (leave event).
     *
     * <p>The returned future must complete after at most 10 seconds from the moment this method is
     * called (exact timeout length subject to change, but 10 seconds is guaranteed).
     *
     * <p>This method is called from your app's main thread, which is usually also the UI thread.
     *
     * @param events A list of {@link TileInteractionEvent} representing interactions that occurred.
     */
    @MainThread
    protected @NonNull ListenableFuture<Void> onRecentInteractionEventsAsync(
            @NonNull List<TileInteractionEvent> events) {
        return createImmediateFuture();
    }

    /**
     * Gets an instance of {@link TileUpdateRequester} to allow a Tile Provider to notify the tile's
     * renderer that it should request a new Timeline from this {@link TileService}.
     *
     * @param context The application context.
     */
    public static @NonNull TileUpdateRequester getUpdater(@NonNull Context context) {

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
     * @param context  The application context.
     * @param executor The executor on which methods should be invoked. To dispatch events through
     *                 the main thread of your application, you can use
     *                 {@link Context#getMainExecutor()}.
     * @return A list of {@link ActiveTileIdentifier} for the tiles belonging to the passed {@code
     * context} present in the carousel, or a value based on platform-specific fallback
     * behavior.
     */
    public static @NonNull ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsync(
            @NonNull Context context, @NonNull Executor executor) {
        if (useWearSdkImpl(context)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return Api34Impl.getActiveTilesAsync(
                    Sdk.getWearManager(context, TilesManager.class), executor);
        }
        return getActiveTilesAsyncLegacy(context, executor, sTimeSourceClock);
    }

    @VisibleForTesting
    static @NonNull ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsyncLegacy(
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
    public @Nullable IBinder onBind(@NonNull Intent intent) {
        if (Objects.equals(intent.getAction(), ACTION_BIND_TILE_PROVIDER)
                || Objects.equals(intent.getAction(), ACTION_BIND_WIDGET_PROVIDER)) {
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

        @SuppressWarnings("RestrictedApiAndroidX")
        @Override
        public void onTileRequest(
                int tileId, TileRequestData requestParams, TileCallback callback) {
            mHandler.post(
                    () -> {
                        TileService tileService = mServiceRef.get();
                        if (tileService == null) {
                            return;
                        }
                        if (requestParams.getVersion() != TileRequestData.VERSION_PROTOBUF) {
                            Log.e(
                                    TAG,
                                    "TileRequestData had unexpected version: "
                                            + requestParams.getVersion());
                            return;
                        }
                        tileService.markTileAsActiveLegacy(tileId);
                        TileRequest tileRequest;
                        ProtoLayoutScope scope;
                        try {
                            RequestProto.TileRequest tileRequestProto =
                                    RequestProto.TileRequest.parseFrom(requestParams.getContents());

                            RequestProto.TileRequest.Builder tileRequestProtoBuilder =
                                    tileRequestProto.toBuilder();
                            tileRequestProtoBuilder.setTileId(tileId);

                            // If schema version is missing, go and fill it back in again.
                            // Explicitly check that device_config is set though. If not, then
                            // skip entirely.
                            VersionInfo rendererVersion;
                            if (tileRequestProto.hasDeviceConfiguration()
                                    && !tileRequestProto
                                    .getDeviceConfiguration()
                                    .hasRendererSchemaVersion()) {
                                rendererVersion = DEFAULT_VERSION;
                                DeviceParameters deviceParams =
                                        tileRequestProto.getDeviceConfiguration().toBuilder()
                                                .setRendererSchemaVersion(DEFAULT_VERSION)
                                                .build();
                                tileRequestProtoBuilder.setDeviceConfiguration(deviceParams);
                            } else {
                                rendererVersion =
                                        tileRequestProto
                                                .getDeviceConfiguration()
                                                .getRendererSchemaVersion();
                            }

                            scope =
                                    new ProtoLayoutScope(
                                            VersionBuilders.VersionInfo.fromProto(rendererVersion));
                            tileRequest =
                                    TileRequest.fromProto(tileRequestProtoBuilder.build(), scope);
                        } catch (InvalidProtocolBufferException ex) {
                            Log.e(TAG, "Error deserializing TileRequest payload.", ex);
                            return;
                        }

                        ListenableFuture<Tile> tileFuture = tileService.onTileRequest(tileRequest);

                        tileFuture.addListener(
                                () -> {
                                    try {
                                        // Inject the current schema version.
                                        TileProto.Tile.Builder tileBuilder =
                                                tileFuture.get().toProto().toBuilder()
                                                        .setSchemaVersion(Version.CURRENT);

                                        // Collect the PendingIntents used in the layout clickable
                                        // if any
                                        Bundle pendingIntents = scope.collectPendingIntents();
                                        Bundle extras = new Bundle();
                                        extras.putParcelable(
                                                TileData.PENDING_INTENT_KEY, pendingIntents);

                                        String incomingResVer = tileBuilder.getResourcesVersion();
                                        Resources resourcesFromScope = scope.collectResources();

                                        boolean hasScopeResources = scope.hasResources();
                                        if (hasScopeResources) {
                                            // Take resources from scope. They will have their own
                                            // version based on hash codes of actual resources
                                            // inside, and we will append version set in Tile
                                            // response. Update incoming version and version in
                                            // resources to match generated one.
                                            incomingResVer =
                                                    mergeTileAndScopeResourcesVersion(
                                                            tileId,
                                                            incomingResVer,
                                                            resourcesFromScope);
                                            tileBuilder.setResourcesVersion(incomingResVer);
                                            resourcesFromScope =
                                                    Resources.fromProto(
                                                            resourcesFromScope.toProto().toBuilder()
                                                                    .setVersion(incomingResVer)
                                                                    .build());
                                        }

                                        // Legacy behaviour for older renderers, where tile and
                                        // resources are separated.
                                        // If scope has resources, they will be preserved until
                                        // onResourcesRequest is called.
                                        if (!isResourcesWithTileAndExtrasEnabled(tileRequest)) {
                                            // Save resources for onResReq if they were in the scope
                                            if (hasScopeResources) {
                                                // This will override any previously saved resources
                                                tileService.saveResources(resourcesFromScope);
                                            }
                                            // Generated version will be propagated from Tile's
                                            // version (updated above) via ResourcesRequest
                                            updateTileDataV1(callback, tileBuilder.build());
                                            return;
                                        }

                                        // Last resources version is set in the renderer, as
                                        // `resources.getVersion` of resources applied to the tile.
                                        // That is why we update resources version above to match
                                        // the merged one in case scope is used.
                                        String lastResVer =
                                                tileRequest.toProto().getLastResourcesVersion();

                                        if (incomingResVer.isEmpty()
                                                || incomingResVer.equals(lastResVer)) {
                                            // If the tile has no resources, or the resources
                                            // version is the same as the last one, then the
                                            // renderer will use the cached resources. We can skip
                                            // the resources fetch and send the tile data directly.
                                            updateTileDataV2(
                                                    callback,
                                                    tileBuilder,
                                                    /* resources= */ null,
                                                    extras);
                                            return;
                                        }

                                        // Scope has resources, we will send those.
                                        if (hasScopeResources) {
                                            updateTileDataV2(
                                                    callback,
                                                    tileBuilder,
                                                    resourcesFromScope.toProto(),
                                                    extras);
                                            return;
                                        }

                                        // Since scope doesn't have resources, we will fetch new
                                        // ones. There could be the case where resources are
                                        // actually empty, but in that case this call will be quick.
                                        String finalIncomingResVer = incomingResVer;
                                        ResourcesRequest resourcesRequest =
                                                new ResourcesRequest.Builder()
                                                        .setVersion(finalIncomingResVer)
                                                        .setDeviceConfiguration(
                                                                tileRequest
                                                                        .getDeviceConfiguration())
                                                        .setTileId(tileId)
                                                        .build();
                                        onResourcesRequestInternal(
                                                resourcesRequest,
                                                /* onSuccess= */ resources -> {
                                                    if (finalIncomingResVer.equals(
                                                            resources.getVersion())) {
                                                        updateTileDataV2(
                                                                callback,
                                                                tileBuilder,
                                                                resources.toProto(),
                                                                extras);
                                                    }
                                                });
                                    } catch (ExecutionException
                                             | InterruptedException
                                             | CancellationException ex) {
                                        Log.e(TAG, "onTileRequest Future failed", ex);
                                    }
                                },
                                mHandler::post);
                    });
        }

        @Override
        @SuppressWarnings("deprecation") // for backward compatibility
        public void onResourcesRequest(
                int tileId, ResourcesRequestData requestParams, ResourcesCallback callback) {
            mHandler.post(
                    () -> {
                        TileService tileService = mServiceRef.get();

                        if (tileService == null) {
                            return;
                        }

                        if (requestParams.getVersion() != ResourcesRequestData.VERSION_PROTOBUF) {
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
                                        resourcesRequestProto.getDeviceConfiguration().toBuilder()
                                                .setRendererSchemaVersion(DEFAULT_VERSION)
                                                .build();
                                resourcesRequestProtoBuilder.setDeviceConfiguration(deviceParams);
                            }

                            req = ResourcesRequest.fromProto(resourcesRequestProtoBuilder.build());
                        } catch (InvalidProtocolBufferException ex) {
                            Log.e(TAG, "Error deserializing ResourcesRequest payload.", ex);
                            return;
                        }

                        onResourcesRequestInternal(
                                req,
                                /* onSuccess= */ resources ->
                                        updateResources(
                                                callback, resources.toProto().toByteArray()));
                    });
        }

        /**
         * Request resources from the TileService, either via saved resources or via {@link
         * TileService#onTileResourcesRequest}.
         */
        private void onResourcesRequestInternal(
                @NonNull ResourcesRequest resourcesRequest,
                @NonNull Consumer<Resources> onSuccess) {
            TileService tileService = mServiceRef.get();
            if (tileService == null) {
                return;
            }

            String resourcesVersion = resourcesRequest.getVersion();
            // The resources version can be generated by the `ProtoLayoutScope` in `onTileRequest`.
            // This generated version follows a specific pattern including the tile ID. If the
            // requested `resourcesVersion` matches this pattern and the tile ID, it means these
            // resources were generated from a `ProtoLayoutScope` during a prior `onTileRequest`
            // call. We attempt to retrieve these saved resources, which could be in memory or
            // persisted to disk. If the saved resources are found, they are used. If not found, an
            // error is logged as they were expected to be present.
            Matcher matcher = RESOURCES_VERSION_PATTERN.matcher(resourcesVersion);
            if (matcher.matches()
                    && Integer.parseInt(matcher.group(1)) == resourcesRequest.getTileId()) {
                Resources savedResources = tileService.getSavedResources(resourcesVersion);
                if (savedResources == null) {
                    Log.e(
                            TAG,
                            "Saved resources not found for version: "
                                    + resourcesRequest.getVersion());
                    return;
                }
                onSuccess.accept(savedResources);
                return;
            }

            ListenableFuture<Resources> resourcesFuture =
                    tileService.onTileResourcesRequest(resourcesRequest);
            if (resourcesFuture.isDone()) {
                try {
                    onSuccess.accept(resourcesFuture.get());
                } catch (ExecutionException | InterruptedException | CancellationException ex) {
                    Log.e(TAG, "onTileResourcesRequest Future failed", ex);
                }
            } else {
                resourcesFuture.addListener(
                        () -> {
                            try {
                                onSuccess.accept(resourcesFuture.get());
                            } catch (ExecutionException
                                     | InterruptedException
                                     | CancellationException ex) {
                                Log.e(TAG, "onTileResourcesRequest Future failed", ex);
                            }
                        },
                        mHandler::post);
            }
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

                                tileService.clearSavedResources(evt.getTileId());
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
                                sendRecentInteractionEventsInternal(
                                        List.of(
                                                new TileInteractionEvent.Builder(
                                                        evt.getTileId(),
                                                        TileInteractionEvent.ENTER)
                                                        .build()),
                                        /* callback= */ null);
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
                                sendRecentInteractionEventsInternal(
                                        List.of(
                                                new TileInteractionEvent.Builder(
                                                        evt.getTileId(),
                                                        TileInteractionEvent.LEAVE)
                                                        .build()),
                                        /* callback= */ null);
                            } catch (InvalidProtocolBufferException ex) {
                                Log.e(TAG, "Error deserializing TileLeaveEvent payload.", ex);
                            }
                        }
                    });
        }

        @Override
        @SuppressWarnings("JdkCollectors")
        public void onRecentInteractionEvents(
                List<TileInteractionEventData> data, InteractionEventsCallback callback) {
            mHandler.post(
                    () -> {
                        TileService tileService = mServiceRef.get();

                        if (data.isEmpty() || tileService == null) {
                            return;
                        }

                        if (data.get(0).getVersion() != TileInteractionEventData.VERSION_PROTOBUF) {
                            Log.e(
                                    TAG,
                                    "TileInteractionEventData had unexpected version: "
                                            + data.get(0).getVersion());
                            return;
                        }

                        List<TileInteractionEvent> events =
                                data.stream()
                                        .map(TileProviderWrapper::tileInteractionEventFromProto)
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .collect(toList());
                        sendRecentInteractionEventsInternal(events, callback);
                    });
        }

        private void sendRecentInteractionEventsInternal(
                @NonNull List<TileInteractionEvent> events,
                @Nullable InteractionEventsCallback callback) {
            TileService tileService = mServiceRef.get();
            ListenableFuture<Void> future = tileService.onRecentInteractionEventsAsync(events);
            future.addListener(
                    () -> {
                        try {
                            future.get();
                            if (callback != null) {
                                callback.finish();
                            }
                        } catch (ExecutionException
                                 | InterruptedException
                                 | CancellationException
                                 | RemoteException ex) {
                            Log.e(TAG, "onRecentInteractionEventsAsync Future failed", ex);
                        }
                    },
                    mHandler::post);
        }

        private static @NonNull Optional<TileInteractionEvent> tileInteractionEventFromProto(
                TileInteractionEventData data) {
            try {
                return Optional.of(
                        TileInteractionEvent.fromProto(
                                EventProto.TileInteractionEvent.parseFrom(data.getContents())));
            } catch (InvalidProtocolBufferException ex) {
                Log.e(TAG, "Error deserializing TileInteractionEvent payload.", ex);
                return Optional.empty();
            }
        }
    }

    /**
     * Merges the resources version, that is usually generated from scope with the version that
     * developer could put in Tile.
     */
    @VisibleForTesting
    static @NonNull String mergeTileAndScopeResourcesVersion(
            int tileId, String incomingTileResVer, Resources resourcesFromScope) {
        return tileId + ";" + incomingTileResVer + ";" + resourcesFromScope.getVersion();
    }

    private static void updateTileDataV1(
            @NonNull TileCallback callback, TileProto.@NonNull Tile tile) {
        try {
            callback.updateTileData(new TileData(tile.toByteArray(), TileData.VERSION_PROTOBUF_1));
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while returning tile payload", ex);
        }
    }

    /**
     * Updates {@link TileCallback} as Tile V2 which accepts resources and Bundle within the
     * response. Resources will only be set if they are not null.
     */
    @SuppressWarnings("RestrictedApiAndroidX")
    private static void updateTileDataV2(
            @NonNull TileCallback callback,
            TileProto.Tile.@NonNull Builder tileBuilder,
            ResourceProto.@Nullable Resources resources,
            @Nullable Bundle extras) {
        try {
            if (resources != null) {
                tileBuilder.setResources(resources);
            }
            callback.updateTileData(
                    new TileData(
                            tileBuilder.build().toByteArray(),
                            extras,
                            TileData.VERSION_PROTOBUF_2));
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while returning tile payload", ex);
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
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !"robolectric".equalsIgnoreCase(Build.FINGERPRINT)) {
            sUseWearSdkImpl = (Sdk.getWearManager(context, TilesManager.class) != null);
            return;
        }
        sUseWearSdkImpl = false;
    }

    @VisibleForTesting
    static void setUseWearSdkImpl(@Nullable Boolean value) {
        sUseWearSdkImpl = value;
    }

    @RequiresApi(34)
    private static class Api34Impl {
        static @NonNull ListenableFuture<List<ActiveTileIdentifier>> getActiveTilesAsync(
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
                                        completer.setException(error);
                                    }
                                });
                        return "getActiveTilesAsync";
                    });
        }

        private static List<ActiveTileIdentifier> tileInstanceToActiveTileIdentifier(
                @NonNull List<TileInstance> tileInstanceList) {
            return tileInstanceList.stream()
                    .map(
                            i ->
                                    new ActiveTileIdentifier(
                                            i.getTileProvider().getComponentName(), i.getId()))
                    .collect(toList());
        }
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

    /** Returns the {@code SAVED_RESOURCES_SHARED_PREF_NAME} shared preferences. */
    private DiskAccessAllowedPrefs getSavedResourcesSharedPref() {
        if (mSavedResourcesSharedPref == null) {
            mSavedResourcesSharedPref =
                    DiskAccessAllowedPrefs.wrap(this, SAVED_RESOURCES_SHARED_PREF_NAME);
        }
        return mSavedResourcesSharedPref;
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
                                                    .collect(toList());
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

    /** Creates immediate future with empty result of the given type. */
    private static <T> ListenableFuture<T> createImmediateFuture() {
        ResolvableFuture<T> future = ResolvableFuture.create();
        future.set(null);
        return future;
    }

    /** Creates immediate future with the given result of the given type. */
    static <T> ListenableFuture<T> createImmediateFuture(T result) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        future.set(result);
        return future;
    }

    /**
     * Checks if the given renderer version is at least the minimum supported version for fetching
     * resources in the same tile request and extra {@code Bundle} of {@code PendingIntent}.
     */
    private static boolean isResourcesWithTileAndExtrasEnabled(@NonNull TileRequest tileRequest) {
        VersionBuilders.VersionInfo versionInfo =
                tileRequest.getDeviceConfiguration().getRendererSchemaVersion();
        int major = 1;
        int minor = 526;
        return (versionInfo.getMajor() == major && versionInfo.getMinor() >= minor)
                || versionInfo.getMajor() > major;
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
