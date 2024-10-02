/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.mediarouter.media;

import static android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH;
import static android.media.MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER;
import static android.media.MediaRouter.RouteInfo.DEVICE_TYPE_TV;
import static android.media.MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.mediarouter.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides routes for built-in system destinations such as the local display and speaker. it
 * queries the framework {@link android.media.MediaRouter} for framework-provided routes and
 * registers non-framework-provided routes as user routes.
 */
abstract class PlatformMediaRouter1RouteProvider extends MediaRouteProvider {

    public static final String TAG = "AxSysMediaRouteProvider";
    public static final String PACKAGE_NAME = "android";
    public static final String DEFAULT_ROUTE_ID = "DEFAULT_ROUTE";

    public static final int ROUTE_TYPE_LIVE_AUDIO = 0x1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 0x2;
    public static final int ROUTE_TYPE_USER = 0x00800000;

    public static final int ALL_ROUTE_TYPES =
            ROUTE_TYPE_LIVE_AUDIO | ROUTE_TYPE_LIVE_VIDEO | ROUTE_TYPE_USER;

    protected PlatformMediaRouter1RouteProvider(Context context) {
        super(context, new ProviderMetadata(new ComponentName(PACKAGE_NAME,
                PlatformMediaRouter1RouteProvider.class.getName())));
    }

    public static PlatformMediaRouter1RouteProvider obtain(
            Context context, SyncCallback syncCallback) {
        if (Build.VERSION.SDK_INT >= 24) {
            return new Api24Impl(context, syncCallback);
        }
        return new JellybeanMr2Impl(context, syncCallback);
    }

    /**
     * Called by the media router when a route is added to synchronize state with
     * the framework media router.
     */
    public void onSyncRouteAdded(MediaRouter.RouteInfo route) {
    }

    /**
     * Called by the media router when a route is removed to synchronize state with
     * the framework media router.
     */
    public void onSyncRouteRemoved(MediaRouter.RouteInfo route) {
    }

    /**
     * Called by the media router when a route is changed to synchronize state with
     * the framework media router.
     */
    public void onSyncRouteChanged(MediaRouter.RouteInfo route) {
    }

    /**
     * Called by the media router when a route is selected to synchronize state with
     * the framework media router.
     */
    public void onSyncRouteSelected(MediaRouter.RouteInfo route) {
    }

    /**
     * Callbacks into the media router to synchronize state with the framework media router.
     */
    public interface SyncCallback {
        void onPlatformRouteSelectedByDescriptorId(@NonNull String id);
    }

    /** Jellybean MR2 implementation. */
    private static class JellybeanMr2Impl extends PlatformMediaRouter1RouteProvider
            implements MediaRouterUtils.Callback, MediaRouterUtils.VolumeCallback {

        private static final ArrayList<IntentFilter> LIVE_AUDIO_CONTROL_FILTERS;

        static {
            IntentFilter f = new IntentFilter();
            f.addCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);

            LIVE_AUDIO_CONTROL_FILTERS = new ArrayList<>();
            LIVE_AUDIO_CONTROL_FILTERS.add(f);
        }

        private static final ArrayList<IntentFilter> LIVE_VIDEO_CONTROL_FILTERS;

        static {
            IntentFilter f = new IntentFilter();
            f.addCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);

            LIVE_VIDEO_CONTROL_FILTERS = new ArrayList<>();
            LIVE_VIDEO_CONTROL_FILTERS.add(f);
        }

        private final SyncCallback mSyncCallback;
        protected final android.media.MediaRouter mRouter;
        protected final android.media.MediaRouter.Callback mCallback;
        protected final android.media.MediaRouter.VolumeCallback mVolumeCallback;
        protected final android.media.MediaRouter.RouteCategory mUserRouteCategory;
        protected int mRouteTypes;
        protected boolean mActiveScan;
        protected boolean mCallbackRegistered;

        // Maintains an association from framework routes to support library routes.
        // Note that we cannot use the tag field for this because an application may
        // have published its own user routes to the framework media router and already
        // used the tag for its own purposes.
        protected final ArrayList<SystemRouteRecord> mSystemRouteRecords =
                new ArrayList<>();

        // Maintains an association from support library routes to framework routes.
        protected final ArrayList<UserRouteRecord> mUserRouteRecords =
                new ArrayList<>();

        /* package */ JellybeanMr2Impl(Context context, SyncCallback syncCallback) {
            super(context);
            mSyncCallback = syncCallback;
            mRouter =
                    (android.media.MediaRouter)
                            context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
            mCallback = MediaRouterUtils.createCallback(this);
            mVolumeCallback = MediaRouterUtils.createVolumeCallback(this);

            Resources r = context.getResources();
            mUserRouteCategory =
                    mRouter.createRouteCategory(
                            r.getString(R.string.mr_user_route_category_name),
                            /* isGroupable= */ false);
            updateSystemRoutes();
        }

        @Override
        public RouteController onCreateRouteController(@NonNull String routeId) {
            int index = findSystemRouteRecordByDescriptorId(routeId);
            if (index >= 0) {
                SystemRouteRecord record = mSystemRouteRecords.get(index);
                return new SystemRouteController(record.mRoute);
            }
            return null;
        }

        @Override
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
            int newRouteTypes = 0;
            boolean newActiveScan = false;
            if (request != null) {
                final MediaRouteSelector selector = request.getSelector();
                final List<String> categories = selector.getControlCategories();
                final int count = categories.size();
                for (int i = 0; i < count; i++) {
                    String category = categories.get(i);
                    if (category.equals(MediaControlIntent.CATEGORY_LIVE_AUDIO)) {
                        newRouteTypes |= ROUTE_TYPE_LIVE_AUDIO;
                    } else if (category.equals(MediaControlIntent.CATEGORY_LIVE_VIDEO)) {
                        newRouteTypes |= ROUTE_TYPE_LIVE_VIDEO;
                    } else {
                        newRouteTypes |= ROUTE_TYPE_USER;
                    }
                }
                newActiveScan = request.isActiveScan();
            }

            if (mRouteTypes != newRouteTypes || mActiveScan != newActiveScan) {
                mRouteTypes = newRouteTypes;
                mActiveScan = newActiveScan;
                updateSystemRoutes();
            }
        }

        @Override
        public void onRouteAdded(@NonNull android.media.MediaRouter.RouteInfo route) {
            if (addSystemRouteNoPublish(route)) {
                publishRoutes();
            }
        }

        private void updateSystemRoutes() {
            updateCallback();
            boolean changed = false;
            for (android.media.MediaRouter.RouteInfo route : getRoutes()) {
                changed |= addSystemRouteNoPublish(route);
            }
            if (changed) {
                publishRoutes();
            }
        }

        private List<android.media.MediaRouter.RouteInfo> getRoutes() {
            final int count = mRouter.getRouteCount();
            List<android.media.MediaRouter.RouteInfo> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                out.add(mRouter.getRouteAt(i));
            }
            return out;
        }

        private boolean addSystemRouteNoPublish(android.media.MediaRouter.RouteInfo route) {
            if (getUserRouteRecord(route) == null && findSystemRouteRecord(route) < 0) {
                String id = assignRouteId(route);
                SystemRouteRecord record = new SystemRouteRecord(route, id);
                updateSystemRouteDescriptor(record);
                mSystemRouteRecords.add(record);
                return true;
            }
            return false;
        }

        private String assignRouteId(android.media.MediaRouter.RouteInfo route) {
            // TODO: The framework media router should supply a unique route id that
            // we can use here.  For now we use a hash of the route name and take care
            // to dedupe it.
            boolean isDefault = (getDefaultRoute() == route);
            String id = isDefault ? DEFAULT_ROUTE_ID :
                    String.format(Locale.US, "ROUTE_%08x", getRouteName(route).hashCode());
            if (findSystemRouteRecordByDescriptorId(id) < 0) {
                return id;
            }
            for (int i = 2; ; i++) {
                String newId = String.format(Locale.US, "%s_%d", id, i);
                if (findSystemRouteRecordByDescriptorId(newId) < 0) {
                    return newId;
                }
            }
        }

        @Override
        public void onRouteRemoved(@NonNull android.media.MediaRouter.RouteInfo route) {
            if (getUserRouteRecord(route) == null) {
                int index = findSystemRouteRecord(route);
                if (index >= 0) {
                    mSystemRouteRecords.remove(index);
                    publishRoutes();
                }
            }
        }

        @Override
        public void onRouteChanged(@NonNull android.media.MediaRouter.RouteInfo route) {
            if (getUserRouteRecord(route) == null) {
                int index = findSystemRouteRecord(route);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    updateSystemRouteDescriptor(record);
                    publishRoutes();
                }
            }
        }

        @Override
        public void onRouteVolumeChanged(@NonNull android.media.MediaRouter.RouteInfo route) {
            if (getUserRouteRecord(route) == null) {
                int index = findSystemRouteRecord(route);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    int newVolume = route.getVolume();
                    if (newVolume != record.mRouteDescriptor.getVolume()) {
                        record.mRouteDescriptor =
                                new MediaRouteDescriptor.Builder(record.mRouteDescriptor)
                                        .setVolume(newVolume)
                                        .build();
                        publishRoutes();
                    }
                }
            }
        }

        @Override
        public void onRouteSelected(int type,
                @NonNull android.media.MediaRouter.RouteInfo route) {
            if (route != mRouter.getSelectedRoute(ALL_ROUTE_TYPES)) {
                // The currently selected route has already changed so this callback
                // is stale.  Drop it to prevent getting into sync loops.
                return;
            }

            UserRouteRecord userRouteRecord = getUserRouteRecord(route);
            if (userRouteRecord != null) {
                userRouteRecord.mRoute.select(/* syncMediaRoute1Provider= */ false);
            } else {
                // Select the route if it already exists in the compat media router.
                // If not, we will select it instead when the route is added.
                int index = findSystemRouteRecord(route);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    mSyncCallback.onPlatformRouteSelectedByDescriptorId(record.mRouteDescriptorId);
                }
            }
        }

        @Override
        public void onRouteUnselected(int type,
                @NonNull android.media.MediaRouter.RouteInfo route) {
            // Nothing to do when a route is unselected.
            // We only need to handle when a route is selected.
        }

        @Override
        public void onRouteGrouped(@NonNull android.media.MediaRouter.RouteInfo route,
                @NonNull android.media.MediaRouter.RouteGroup group, int index) {
            // Route grouping is deprecated and no longer supported.
        }

        @Override
        public void onRouteUngrouped(@NonNull android.media.MediaRouter.RouteInfo route,
                @NonNull android.media.MediaRouter.RouteGroup group) {
            // Route grouping is deprecated and no longer supported.
        }

        @Override
        public void onVolumeSetRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                int volume) {
            UserRouteRecord record = getUserRouteRecord(route);
            if (record != null) {
                record.mRoute.requestSetVolume(volume);
            }
        }

        @Override
        public void onVolumeUpdateRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                int direction) {
            UserRouteRecord record = getUserRouteRecord(route);
            if (record != null) {
                record.mRoute.requestUpdateVolume(direction);
            }
        }

        @Override
        public void onSyncRouteAdded(MediaRouter.RouteInfo route) {
            if (route.getProviderInstance() != this) {
                android.media.MediaRouter.UserRouteInfo userRoute =
                        mRouter.createUserRoute(mUserRouteCategory);
                UserRouteRecord record = new UserRouteRecord(route, userRoute);
                userRoute.setTag(record);
                userRoute.setVolumeCallback(mVolumeCallback);
                updateUserRouteProperties(record);
                mUserRouteRecords.add(record);
                mRouter.addUserRoute(userRoute);
            } else {
                // If the newly added route is the counterpart of the currently selected
                // route in the framework media router then ensure it is selected in
                // the compat media router.
                android.media.MediaRouter.RouteInfo routeObj =
                        mRouter.getSelectedRoute(ALL_ROUTE_TYPES);
                int index = findSystemRouteRecord(routeObj);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    if (record.mRouteDescriptorId.equals(route.getDescriptorId())) {
                        route.select(/* syncMediaRoute1Provider= */ false);
                    }
                }
            }
        }

        @Override
        public void onSyncRouteRemoved(MediaRouter.RouteInfo route) {
            if (route.getProviderInstance() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = mUserRouteRecords.remove(index);
                    record.mUserRoute.setTag(null);
                    record.mUserRoute.setVolumeCallback(null);

                    try {
                        mRouter.removeUserRoute(record.mUserRoute);
                    } catch (IllegalArgumentException e) {
                        // Work around for https://issuetracker.google.com/issues/202931542.
                        Log.w(TAG, "Failed to remove user route", e);
                    }
                }
            }
        }

        @Override
        public void onSyncRouteChanged(MediaRouter.RouteInfo route) {
            if (route.getProviderInstance() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = mUserRouteRecords.get(index);
                    updateUserRouteProperties(record);
                }
            }
        }

        @Override
        public void onSyncRouteSelected(MediaRouter.RouteInfo route) {
            if (!route.isSelected()) {
                // The currently selected route has already changed so this callback
                // is stale.  Drop it to prevent getting into sync loops.
                return;
            }

            if (route.getProviderInstance() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = mUserRouteRecords.get(index);
                    selectRoute(record.mUserRoute);
                }
            } else {
                int index = findSystemRouteRecordByDescriptorId(route.getDescriptorId());
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    selectRoute(record.mRoute);
                }
            }
        }

        protected void publishRoutes() {
            MediaRouteProviderDescriptor.Builder builder =
                    new MediaRouteProviderDescriptor.Builder();
            int count = mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                builder.addRoute(mSystemRouteRecords.get(i).mRouteDescriptor);
            }

            setDescriptor(builder.build());
        }

        protected int findSystemRouteRecord(android.media.MediaRouter.RouteInfo route) {
            final int count = mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (mSystemRouteRecords.get(i).mRoute == route) {
                    return i;
                }
            }
            return -1;
        }

        protected int findSystemRouteRecordByDescriptorId(String id) {
            final int count = mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (mSystemRouteRecords.get(i).mRouteDescriptorId.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        protected int findUserRouteRecord(MediaRouter.RouteInfo route) {
            final int count = mUserRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (mUserRouteRecords.get(i).mRoute == route) {
                    return i;
                }
            }
            return -1;
        }

        protected UserRouteRecord getUserRouteRecord(android.media.MediaRouter.RouteInfo route) {
            Object tag = route.getTag();
            return tag instanceof UserRouteRecord ? (UserRouteRecord) tag : null;
        }

        protected void updateSystemRouteDescriptor(SystemRouteRecord record) {
            // We must always recreate the route descriptor when making any changes
            // because they are intended to be immutable once published.
            MediaRouteDescriptor.Builder builder = new MediaRouteDescriptor.Builder(
                    record.mRouteDescriptorId, getRouteName(record.mRoute));
            onBuildSystemRouteDescriptor(record, builder);
            record.mRouteDescriptor = builder.build();
        }

        protected String getRouteName(android.media.MediaRouter.RouteInfo route) {
            // Routes with null or empty names are discarded by MediaRouter as not valid. This may
            // happen for badly configured user routes, or for system routes (which are not
            // guaranteed to have a non-empty name). For system routes, we replace the empty name
            // with a placeholder one so that the route is not swallowed. Otherwise, that can mean
            // that media router thinks no bluetooth route is available, and selects the default
            // route instead. See b/294968421.
            CharSequence routeInfoName = route.getName(getContext());
            if (!TextUtils.isEmpty(routeInfoName)) {
                return routeInfoName.toString();
            } else if ((route.getSupportedTypes() & ROUTE_TYPE_USER) == 0) {
                int fallbackRouteNameResourceId =
                        getStringResourceIdForType(
                                Build.VERSION.SDK_INT >= 24
                                        ? route.getDeviceType()
                                        : DEVICE_TYPE_UNKNOWN);
                return getContext().getString(fallbackRouteNameResourceId);
            } else {
                return "";
            }
        }

        private static int getStringResourceIdForType(int deviceType) {
            switch (deviceType) {
                case DEVICE_TYPE_BLUETOOTH:
                    return R.string.mr_route_name_bluetooth;
                case DEVICE_TYPE_TV:
                    return R.string.mr_route_name_tv;
                case DEVICE_TYPE_SPEAKER:
                    return R.string.mr_route_name_speaker;
                case DEVICE_TYPE_UNKNOWN:
                default:
                    return R.string.mr_route_name_unknown;
            }
        }

        protected void onBuildSystemRouteDescriptor(SystemRouteRecord record,
                MediaRouteDescriptor.Builder builder) {
            int supportedTypes = record.mRoute.getSupportedTypes();
            if ((supportedTypes & ROUTE_TYPE_LIVE_AUDIO) != 0) {
                builder.addControlFilters(LIVE_AUDIO_CONTROL_FILTERS);
            }
            if ((supportedTypes & ROUTE_TYPE_LIVE_VIDEO) != 0) {
                builder.addControlFilters(LIVE_VIDEO_CONTROL_FILTERS);
            }

            builder.setPlaybackType(record.mRoute.getPlaybackType());
            builder.setPlaybackStream(record.mRoute.getPlaybackStream());
            builder.setVolume(record.mRoute.getVolume());
            builder.setVolumeMax(record.mRoute.getVolumeMax());
            builder.setVolumeHandling(record.mRoute.getVolumeHandling());
            builder.setIsSystemRoute((supportedTypes & ROUTE_TYPE_USER) == 0);

            if (!record.mRoute.isEnabled()) {
                builder.setEnabled(false);
            }

            if (isConnecting(record)) {
                builder.setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING);
            }

            Display presentationDisplay = record.mRoute.getPresentationDisplay();
            if (presentationDisplay != null) {
                builder.setPresentationDisplayId(presentationDisplay.getDisplayId());
            }

            CharSequence description = record.mRoute.getDescription();
            if (description != null) {
                builder.setDescription(description.toString());
            }
        }

        @Override
        public void onRoutePresentationDisplayChanged(
                @NonNull android.media.MediaRouter.RouteInfo route) {
            int index = findSystemRouteRecord(route);
            if (index >= 0) {
                SystemRouteRecord record = mSystemRouteRecords.get(index);
                Display newPresentationDisplay = route.getPresentationDisplay();
                int newPresentationDisplayId = (newPresentationDisplay != null
                        ? newPresentationDisplay.getDisplayId() : -1);
                if (newPresentationDisplayId
                        != record.mRouteDescriptor.getPresentationDisplayId()) {
                    record.mRouteDescriptor =
                            new MediaRouteDescriptor.Builder(record.mRouteDescriptor)
                                    .setPresentationDisplayId(newPresentationDisplayId)
                                    .build();
                    publishRoutes();
                }
            }
        }

        protected void selectRoute(android.media.MediaRouter.RouteInfo route) {
            mRouter.selectRoute(ALL_ROUTE_TYPES, route);
        }

        protected android.media.MediaRouter.RouteInfo getDefaultRoute() {
            return mRouter.getDefaultRoute();
        }

        @SuppressLint("WrongConstant") // False positive. See b/310913043.
        protected void updateUserRouteProperties(UserRouteRecord record) {
            android.media.MediaRouter.UserRouteInfo userRoute = record.mUserRoute;
            MediaRouter.RouteInfo routeInfo = record.mRoute;
            userRoute.setName(routeInfo.getName());
            userRoute.setPlaybackType(routeInfo.getPlaybackType());
            userRoute.setPlaybackStream(routeInfo.getPlaybackStream());
            userRoute.setVolume(routeInfo.getVolume());
            userRoute.setVolumeMax(routeInfo.getVolumeMax());
            userRoute.setVolumeHandling(routeInfo.getVolumeHandling());
            userRoute.setDescription(routeInfo.getDescription());
        }

        protected void updateCallback() {
            if (mCallbackRegistered) {
                mRouter.removeCallback(mCallback);
            }

            mCallbackRegistered = true;
            int flags = MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS
                    | (mActiveScan ? MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN : 0);
            mRouter.addCallback(mRouteTypes, mCallback, flags);
        }

        protected boolean isConnecting(SystemRouteRecord record) {
            return record.mRoute.isConnecting();
        }

        /**
         * Represents a route that is provided by the framework media router
         * and published by this route provider to the support library media router.
         */
        protected static final class SystemRouteRecord {
            public final android.media.MediaRouter.RouteInfo mRoute;
            public final String mRouteDescriptorId;
            public MediaRouteDescriptor mRouteDescriptor; // assigned immediately after creation

            public SystemRouteRecord(android.media.MediaRouter.RouteInfo route, String id) {
                mRoute = route;
                mRouteDescriptorId = id;
            }
        }

        /**
         * Represents a route that is provided by the support library media router
         * and published by this route provider to the framework media router.
         */
        protected static final class UserRouteRecord {
            public final MediaRouter.RouteInfo mRoute;
            public final android.media.MediaRouter.UserRouteInfo mUserRoute;

            public UserRouteRecord(MediaRouter.RouteInfo route,
                    android.media.MediaRouter.UserRouteInfo userRoute) {
                mRoute = route;
                mUserRoute = userRoute;
            }
        }

        protected static final class SystemRouteController extends RouteController {
            private final android.media.MediaRouter.RouteInfo mRoute;

            public SystemRouteController(android.media.MediaRouter.RouteInfo route) {
                mRoute = route;
            }

            @Override
            public void onSetVolume(int volume) {
                mRoute.requestSetVolume(volume);
            }

            @Override
            public void onUpdateVolume(int delta) {
                mRoute.requestUpdateVolume(delta);
            }
        }
    }

    /**
     * Api24 implementation.
     */
    @RequiresApi(24)
    private static class Api24Impl extends JellybeanMr2Impl {
        /* package */ Api24Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @SuppressLint("WrongConstant") // False positive. See b/283059575.
        @Override
        protected void onBuildSystemRouteDescriptor(SystemRouteRecord record,
                MediaRouteDescriptor.Builder builder) {
            super.onBuildSystemRouteDescriptor(record, builder);
            builder.setDeviceType(record.mRoute.getDeviceType());
        }
    }
}
