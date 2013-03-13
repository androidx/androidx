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

package android.support.v4.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Provides routes for built-in system destinations such as the local display
 * and speaker.  On Jellybean and newer platform releases, queries the framework
 * MediaRouter for framework-provided routes and registers non-framework-provided
 * routes as user routes.
 */
abstract class SystemMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "SystemMediaRouteProvider";

    public static final String PACKAGE_NAME = "android";
    public static final String DEFAULT_ROUTE_ID = "DEFAULT_ROUTE";

    protected SystemMediaRouteProvider(Context context) {
        super(context);
    }

    public static SystemMediaRouteProvider obtain(Context context, SyncCallback syncCallback) {
        if (Build.VERSION.SDK_INT >= 18) {
            return new JellybeanMr2Impl(context, syncCallback);
        }
        if (Build.VERSION.SDK_INT >= 17) {
            return new JellybeanMr1Impl(context, syncCallback);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            return new JellybeanImpl(context, syncCallback);
        }
        return new LegacyImpl(context);
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
        public MediaRouter.RouteInfo getSystemRouteByDescriptorId(String id);
    }

    /**
     * Legacy implementation for platform versions prior to Jellybean.
     */
    static class LegacyImpl extends SystemMediaRouteProvider {
        private static final int PLAYBACK_STREAM = AudioManager.STREAM_MUSIC;

        private static final IntentFilter[] CONTROL_FILTERS;
        static {
            CONTROL_FILTERS = new IntentFilter[1];
            CONTROL_FILTERS[0] = new IntentFilter();
            CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
            CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        private final AudioManager mAudioManager;
        private final VolumeChangeReceiver mVolumeChangeReceiver;
        private int mLastReportedVolume = -1;

        public LegacyImpl(Context context) {
            super(context);
            mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            mVolumeChangeReceiver = new VolumeChangeReceiver();

            context.registerReceiver(mVolumeChangeReceiver,
                    new IntentFilter(VolumeChangeReceiver.VOLUME_CHANGED_ACTION));
            publishRoutes();
        }

        private void publishRoutes() {
            // TODO: get route names from resources
            // TODO: add an icon for the route
            RouteDescriptor defaultRoute = new RouteDescriptor(DEFAULT_ROUTE_ID, "System");
            defaultRoute.setControlFilters(CONTROL_FILTERS);
            defaultRoute.setPlaybackStream(PLAYBACK_STREAM);
            defaultRoute.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL);
            defaultRoute.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE);
            defaultRoute.setVolumeMax(mAudioManager.getStreamMaxVolume(PLAYBACK_STREAM));
            mLastReportedVolume = mAudioManager.getStreamVolume(PLAYBACK_STREAM);
            defaultRoute.setVolume(mLastReportedVolume);

            RouteProviderDescriptor providerDescriptor = new RouteProviderDescriptor(PACKAGE_NAME);
            providerDescriptor.setRoutes(new RouteDescriptor[] { defaultRoute });
            setDescriptor(providerDescriptor);
        }

        @Override
        public RouteController onCreateRouteController(String routeId) {
            if (routeId.equals(DEFAULT_ROUTE_ID)) {
                return new DefaultRouteController();
            }
            return null;
        }

        final class DefaultRouteController extends RouteController {
            @Override
            public void setVolume(int volume) {
                mAudioManager.setStreamVolume(PLAYBACK_STREAM, volume, 0);
                publishRoutes();
            }

            @Override
            public void updateVolume(int delta) {
                int volume = mAudioManager.getStreamVolume(PLAYBACK_STREAM);
                int maxVolume = mAudioManager.getStreamMaxVolume(PLAYBACK_STREAM);
                int newVolume = Math.min(maxVolume, Math.max(0, volume + delta));
                if (newVolume != volume) {
                    mAudioManager.setStreamVolume(PLAYBACK_STREAM, volume, 0);
                }
                publishRoutes();
            }
        }

        final class VolumeChangeReceiver extends BroadcastReceiver {
            // These constants come from AudioManager.
            public static final String VOLUME_CHANGED_ACTION =
                    "android.media.VOLUME_CHANGED_ACTION";
            public static final String EXTRA_VOLUME_STREAM_TYPE =
                    "android.media.EXTRA_VOLUME_STREAM_TYPE";
            public static final String EXTRA_VOLUME_STREAM_VALUE =
                    "android.media.EXTRA_VOLUME_STREAM_VALUE";

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(VOLUME_CHANGED_ACTION)) {
                    final int streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                    if (streamType == PLAYBACK_STREAM) {
                        final int volume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1);
                        if (volume >= 0 && volume != mLastReportedVolume) {
                            publishRoutes();
                        }
                    }
                }
            }
        }
    }

    /**
     * Jellybean implementation.
     */
    static class JellybeanImpl extends SystemMediaRouteProvider
            implements MediaRouterJellybean.Callback, MediaRouterJellybean.VolumeCallback {
        protected static final int ALL_ROUTE_TYPES =
                MediaRouterJellybean.ROUTE_TYPE_LIVE_AUDIO
                | MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO
                | MediaRouterJellybean.ROUTE_TYPE_USER;

        private static final IntentFilter[] LIVE_AUDIO_CONTROL_FILTERS;
        static {
            LIVE_AUDIO_CONTROL_FILTERS = new IntentFilter[1];
            LIVE_AUDIO_CONTROL_FILTERS[0] = new IntentFilter();
            LIVE_AUDIO_CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
        }

        private static final IntentFilter[] LIVE_VIDEO_CONTROL_FILTERS;
        static {
            LIVE_VIDEO_CONTROL_FILTERS = new IntentFilter[1];
            LIVE_VIDEO_CONTROL_FILTERS[0] = new IntentFilter();
            LIVE_VIDEO_CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        private static final IntentFilter[] ALL_CONTROL_FILTERS;
        static {
            ALL_CONTROL_FILTERS = new IntentFilter[1];
            ALL_CONTROL_FILTERS[0] = new IntentFilter();
            ALL_CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
            ALL_CONTROL_FILTERS[0].addCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        private final SyncCallback mSyncCallback;
        private Method mSelectRouteIntMethod;
        private Method mGetSystemAudioRouteMethod;

        protected final Object mRouterObj;
        protected final Object mCallbackObj;
        protected final Object mVolumeCallbackObj;
        protected final Object mUserRouteCategoryObj;

        // Maintains an association from framework routes to support library routes.
        // Note that we cannot use the tag field for this because an application may
        // have published its own user routes to the framework media router and already
        // used the tag for its own purposes.
        protected final ArrayList<SystemRouteRecord> mSystemRouteRecords =
                new ArrayList<SystemRouteRecord>();

        // Maintains an association from support library routes to framework routes.
        protected final ArrayList<UserRouteRecord> mUserRouteRecords =
                new ArrayList<UserRouteRecord>();

        public JellybeanImpl(Context context, SyncCallback syncCallback) {
            super(context);
            mSyncCallback = syncCallback;
            mRouterObj = MediaRouterJellybean.getMediaRouter(context);
            mCallbackObj = createCallbackObj();
            mVolumeCallbackObj = createVolumeCallbackObj();

            // TODO: get category name from a resource
            mUserRouteCategoryObj = MediaRouterJellybean.createRouteCategory(
                    mRouterObj, "Devices", false);

            addInitialSystemRoutes();
            MediaRouterJellybean.addCallback(mRouterObj, ALL_ROUTE_TYPES, mCallbackObj);
        }

        @Override
        public void onRouteAdded(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index < 0) {
                addSystemRouteNoPublish(routeObj);
                publishRoutes();
            }
        }

        private void addInitialSystemRoutes() {
            for (Object routeObj : MediaRouterJellybean.getRoutes(mRouterObj)) {
                addSystemRouteNoPublish(routeObj);
            }
            publishRoutes();
        }

        private void addSystemRouteNoPublish(Object routeObj) {
            boolean isDefault = (getDefaultRoute() == routeObj);
            SystemRouteRecord record = new SystemRouteRecord(routeObj, isDefault);
            updateSystemRouteDescriptor(record);
            mSystemRouteRecords.add(record);
        }

        @Override
        public void onRouteRemoved(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                mSystemRouteRecords.remove(index);
                publishRoutes();
            }
        }

        @Override
        public void onRouteChanged(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                SystemRouteRecord record = mSystemRouteRecords.get(index);
                updateSystemRouteDescriptor(record);
                publishRoutes();
            }
        }

        @Override
        public void onRouteVolumeChanged(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                SystemRouteRecord record = mSystemRouteRecords.get(index);
                int newVolume = MediaRouterJellybean.RouteInfo.getVolume(routeObj);
                if (newVolume != record.mRouteDescriptor.getVolume()) {
                    record.mRouteDescriptor = new RouteDescriptor(record.mRouteDescriptor);
                    record.mRouteDescriptor.setVolume(newVolume);
                    publishRoutes();
                }
            }
        }

        @Override
        public void onRouteSelected(int type, Object routeObj) {
            if (routeObj != MediaRouterJellybean.getSelectedRoute(mRouterObj, ALL_ROUTE_TYPES)) {
                // The currently selected route has already changed so this callback
                // is stale.  Drop it to prevent getting into sync loops.
                return;
            }

            Object tag = MediaRouterJellybean.RouteInfo.getTag(routeObj);
            if (tag instanceof UserRouteRecord) {
                UserRouteRecord record = (UserRouteRecord)tag;
                record.mRoute.select();
            } else {
                // Select the route if it already exists in the compat media router.
                // If not, we will select it instead when the route is added.
                int index = findSystemRouteRecord(routeObj);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    MediaRouter.RouteInfo route = mSyncCallback.getSystemRouteByDescriptorId(
                            record.mRouteDescriptorId);
                    if (route != null) {
                        route.select();
                    }
                }
            }
        }

        @Override
        public void onRouteUnselected(int type, Object routeObj) {
            // Nothing to do when a route is unselected.
            // We only need to handle when a route is selected.
        }

        @Override
        public void onRouteGrouped(Object routeObj, Object groupObj, int index) {
            // Route grouping is deprecated and no longer supported.
        }

        @Override
        public void onRouteUngrouped(Object routeObj, Object groupObj) {
            // Route grouping is deprecated and no longer supported.
        }

        @Override
        public void onVolumeSetRequest(Object routeObj, int volume) {
            Object tag = MediaRouterJellybean.RouteInfo.getTag(routeObj);
            if (tag instanceof UserRouteRecord) {
                UserRouteRecord record = (UserRouteRecord)tag;
                record.mRoute.requestSetVolume(volume);
            }
        }

        @Override
        public void onVolumeUpdateRequest(Object routeObj, int direction) {
            Object tag = MediaRouterJellybean.RouteInfo.getTag(routeObj);
            if (tag instanceof UserRouteRecord) {
                UserRouteRecord record = (UserRouteRecord)tag;
                record.mRoute.requestUpdateVolume(direction);
            }
        }

        @Override
        public void onSyncRouteAdded(MediaRouter.RouteInfo route) {
            if (route.getProvider() != this) {
                Object routeObj = MediaRouterJellybean.createUserRoute(
                        mRouterObj, mUserRouteCategoryObj);
                UserRouteRecord record = new UserRouteRecord(route, routeObj);
                MediaRouterJellybean.RouteInfo.setTag(routeObj, record);
                MediaRouterJellybean.UserRouteInfo.setVolumeCallback(routeObj, mVolumeCallbackObj);
                updateUserRouteProperties(record);
                mUserRouteRecords.add(record);
                MediaRouterJellybean.addUserRoute(mRouterObj, routeObj);
            } else {
                // If the newly added route is the counterpart of the currently selected
                // route in the framework media router then ensure it is selected in
                // the compat media router.
                Object routeObj = MediaRouterJellybean.getSelectedRoute(
                        mRouterObj, ALL_ROUTE_TYPES);
                int index = findSystemRouteRecord(routeObj);
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    if (record.mRouteDescriptorId.equals(route.getDescriptorId())) {
                        route.select();
                    }
                }
            }
        }

        @Override
        public void onSyncRouteRemoved(MediaRouter.RouteInfo route) {
            if (route.getProvider() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = mUserRouteRecords.remove(index);
                    MediaRouterJellybean.RouteInfo.setTag(record.mRouteObj, null);
                    MediaRouterJellybean.UserRouteInfo.setVolumeCallback(record.mRouteObj, null);
                    MediaRouterJellybean.removeUserRoute(mRouterObj, record.mRouteObj);
                }
            }
        }

        @Override
        public void onSyncRouteChanged(MediaRouter.RouteInfo route) {
            if (route.getProvider() != this) {
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

            if (route.getProvider() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = mUserRouteRecords.get(index);
                    selectRoute(record.mRouteObj);
                }
            } else {
                int index = findSystemRouteRecordByDescriptorId(route.getDescriptorId());
                if (index >= 0) {
                    SystemRouteRecord record = mSystemRouteRecords.get(index);
                    selectRoute(record.mRouteObj);
                }
            }
        }

        protected void publishRoutes() {
            int count = mSystemRouteRecords.size();
            RouteDescriptor[] routeDescriptors = new RouteDescriptor[count];
            for (int i = 0; i < count; i++) {
                routeDescriptors[i] = mSystemRouteRecords.get(i).mRouteDescriptor;
            }

            RouteProviderDescriptor providerDescriptor = new RouteProviderDescriptor(PACKAGE_NAME);
            providerDescriptor.setRoutes(routeDescriptors);
            setDescriptor(providerDescriptor);
        }

        protected int findSystemRouteRecord(Object routeObj) {
            final int count = mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (mSystemRouteRecords.get(i).mRouteObj == routeObj) {
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

        protected void updateSystemRouteDescriptor(SystemRouteRecord record) {
            // We must always recreate the route descriptor when making any changes
            // because they are intended to be immutable once published.
            String name = MediaRouterJellybean.RouteInfo.getName(
                    record.mRouteObj, getContext()).toString();
            record.mRouteDescriptor = new RouteDescriptor(
                    record.mRouteDescriptorId, name);

            int supportedTypes = MediaRouterJellybean.RouteInfo.getSupportedTypes(
                    record.mRouteObj);
            if ((supportedTypes & MediaRouterJellybean.ROUTE_TYPE_LIVE_AUDIO) != 0) {
                if ((supportedTypes & MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO) != 0) {
                    record.mRouteDescriptor.setControlFilters(ALL_CONTROL_FILTERS);
                } else {
                    record.mRouteDescriptor.setControlFilters(LIVE_AUDIO_CONTROL_FILTERS);
                }
            } else if ((supportedTypes & MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO) != 0) {
                record.mRouteDescriptor.setControlFilters(LIVE_VIDEO_CONTROL_FILTERS);
            }

            CharSequence status = MediaRouterJellybean.RouteInfo.getStatus(record.mRouteObj);
            if (status != null) {
                record.mRouteDescriptor.setStatus(status.toString());
            }
            record.mRouteDescriptor.setIconDrawable(
                    MediaRouterJellybean.RouteInfo.getIconDrawable(record.mRouteObj));
            record.mRouteDescriptor.setPlaybackType(
                    MediaRouterJellybean.RouteInfo.getPlaybackType(record.mRouteObj));
            record.mRouteDescriptor.setPlaybackStream(
                    MediaRouterJellybean.RouteInfo.getPlaybackStream(record.mRouteObj));
            record.mRouteDescriptor.setVolume(
                    MediaRouterJellybean.RouteInfo.getVolume(record.mRouteObj));
            record.mRouteDescriptor.setVolumeMax(
                    MediaRouterJellybean.RouteInfo.getVolumeMax(record.mRouteObj));
            record.mRouteDescriptor.setVolumeHandling(
                    MediaRouterJellybean.RouteInfo.getVolumeHandling(record.mRouteObj));
        }

        protected void updateUserRouteProperties(UserRouteRecord record) {
            MediaRouterJellybean.UserRouteInfo.setName(
                    record.mRouteObj, record.mRoute.getName());
            MediaRouterJellybean.UserRouteInfo.setStatus(
                    record.mRouteObj, normalizeStatus(record.mRoute.getStatus()));
            MediaRouterJellybean.UserRouteInfo.setIconDrawable(
                    record.mRouteObj, record.mRoute.getIconDrawable());
            MediaRouterJellybean.UserRouteInfo.setPlaybackType(
                    record.mRouteObj, record.mRoute.getPlaybackType());
            MediaRouterJellybean.UserRouteInfo.setPlaybackStream(
                    record.mRouteObj, record.mRoute.getPlaybackStream());
            MediaRouterJellybean.UserRouteInfo.setVolume(
                    record.mRouteObj, record.mRoute.getVolume());
            MediaRouterJellybean.UserRouteInfo.setVolumeMax(
                    record.mRouteObj, record.mRoute.getVolumeMax());
            MediaRouterJellybean.UserRouteInfo.setVolumeHandling(
                    record.mRouteObj, record.mRoute.getVolumeHandling());
        }

        // The framework MediaRouter crashes if we set a null status even though
        // RouteInfo.getStatus() may return null.  So we need to use a different
        // value instead.
        private static String normalizeStatus(String status) {
            return status != null ? status : "";
        }

        protected Object createCallbackObj() {
            return MediaRouterJellybean.createCallback(this);
        }

        protected Object createVolumeCallbackObj() {
            return MediaRouterJellybean.createVolumeCallback(this);
        }

        protected void selectRoute(Object routeObj) {
            int types = MediaRouterJellybean.RouteInfo.getSupportedTypes(routeObj);
            if ((types & MediaRouterJellybean.ROUTE_TYPE_USER) == 0) {
                // Handle non-user routes.
                // On JB and JB MR1, the selectRoute() API only supports programmatically
                // selecting user routes.  So instead we rely on the hidden selectRouteInt()
                // method on these versions of the platform.  This limitation was removed
                // in JB MR2.  See also the JellybeanMr2Impl implementation of this method.
                if (mSelectRouteIntMethod == null) {
                    try {
                        mSelectRouteIntMethod = mRouterObj.getClass().getMethod(
                                "selectRouteInt", int.class, MediaRouterJellybean.RouteInfo.clazz);
                    } catch (NoSuchMethodException ex) {
                        Log.w(TAG, "Cannot programmatically select non-user route "
                                + "because the platform is missing the selectRouteInt() "
                                + "method.  Media routing may not work.", ex);
                        return;
                    }
                }
                try {
                    mSelectRouteIntMethod.invoke(mRouterObj, ALL_ROUTE_TYPES, routeObj);
                } catch (IllegalAccessException ex) {
                    Log.w(TAG, "Cannot programmatically select non-user route.  "
                            + "Media routing may not work.", ex);
                } catch (InvocationTargetException ex) {
                    Log.w(TAG, "Cannot programmatically select non-user route.  "
                            + "Media routing may not work.", ex);
                }
            } else {
                // Handle user routes.
                MediaRouterJellybean.selectRoute(mRouterObj, ALL_ROUTE_TYPES, routeObj);
            }
        }

        protected Object getDefaultRoute() {
            // On JB and JB MR1, the getDefaultRoute() API does not exist.
            // Instead there is a hidden getSystemAudioRoute() that does the same thing.
            // See also the JellybeanMr2Impl implementation of this method.
            if (mGetSystemAudioRouteMethod == null) {
                try {
                    mGetSystemAudioRouteMethod = mRouterObj.getClass().getMethod(
                            "getSystemAudioRoute");
                } catch (NoSuchMethodException ex) {
                    // Fall through.
                }
            }
            if (mGetSystemAudioRouteMethod != null) {
                try {
                    return mGetSystemAudioRouteMethod.invoke(mRouterObj);
                } catch (IllegalAccessException ex) {
                    // Fall through.
                } catch (InvocationTargetException ex) {
                    // Fall through.
                }
            }
            // Could not find the method or it does not work.
            // Return the first route and hope for the best.
            return MediaRouterJellybean.getRoutes(mRouterObj).get(0);
        }

        /**
         * Represents a route that is provided by the framework media router
         * and published by this route provider to the support library media router.
         */
        protected static final class SystemRouteRecord {
            private static int sNextId;

            public final Object mRouteObj;
            public final String mRouteDescriptorId;
            public RouteDescriptor mRouteDescriptor; // assigned immediately after creation

            public SystemRouteRecord(Object routeObj, boolean isDefault) {
                mRouteObj = routeObj;
                mRouteDescriptorId = isDefault ? DEFAULT_ROUTE_ID : "ROUTE_" + (sNextId++);
            }
        }

        /**
         * Represents a route that is provided by the support library media router
         * and published by this route provider to the framework media router.
         */
        protected static final class UserRouteRecord {
            public final MediaRouter.RouteInfo mRoute;
            public final Object mRouteObj;

            public UserRouteRecord(MediaRouter.RouteInfo route, Object routeObj) {
                mRoute = route;
                mRouteObj = routeObj;
            }
        }
    }

    /**
     * Jellybean MR1 implementation.
     */
    private static class JellybeanMr1Impl extends JellybeanImpl
            implements MediaRouterJellybeanMr1.Callback {
        public JellybeanMr1Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @Override
        public void onRoutePresentationDisplayChanged(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                SystemRouteRecord record = mSystemRouteRecords.get(index);
                Display newPresentationDisplay =
                        MediaRouterJellybeanMr1.RouteInfo.getPresentationDisplay(routeObj);
                int newPresentationDisplayId = (newPresentationDisplay != null
                        ? newPresentationDisplay.getDisplayId() : -1);
                if (newPresentationDisplayId
                        != record.mRouteDescriptor.getPresentationDisplayId()) {
                    record.mRouteDescriptor = new RouteDescriptor(record.mRouteDescriptor);
                    record.mRouteDescriptor.setPresentationDisplayId(newPresentationDisplayId);
                    publishRoutes();
                }
            }
        }

        @Override
        protected void updateSystemRouteDescriptor(SystemRouteRecord record) {
            super.updateSystemRouteDescriptor(record);

            if (!MediaRouterJellybeanMr1.RouteInfo.isEnabled(record.mRouteObj)) {
                record.mRouteDescriptor.setEnabled(false);
            }
            Display presentationDisplay =
                    MediaRouterJellybeanMr1.RouteInfo.getPresentationDisplay(record.mRouteObj);
            if (presentationDisplay != null) {
                record.mRouteDescriptor.setPresentationDisplayId(
                        presentationDisplay.getDisplayId());
            }
        }

        @Override
        protected Object createCallbackObj() {
            return MediaRouterJellybeanMr1.createCallback(this);
        }
    }

    /**
     * Jellybean MR2 implementation.
     */
    private static class JellybeanMr2Impl extends JellybeanMr1Impl {
        public JellybeanMr2Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @Override
        protected void selectRoute(Object routeObj) {
            MediaRouterJellybean.selectRoute(mRouterObj, ALL_ROUTE_TYPES, routeObj);
        }

        @Override
        protected Object getDefaultRoute() {
            return MediaRouterJellybeanMr2.getDefaultRoute(mRouterObj);
        }
    }
}
