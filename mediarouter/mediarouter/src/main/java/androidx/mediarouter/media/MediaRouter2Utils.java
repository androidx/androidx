/*
 * Copyright 2020 The Android Open Source Project
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

import static android.media.MediaRoute2Info.FEATURE_LIVE_AUDIO;
import static android.media.MediaRoute2Info.FEATURE_LIVE_VIDEO;
import static android.media.MediaRoute2Info.FEATURE_REMOTE_AUDIO_PLAYBACK;
import static android.media.MediaRoute2Info.FEATURE_REMOTE_PLAYBACK;
import static android.media.MediaRoute2Info.FEATURE_REMOTE_VIDEO_PLAYBACK;
import static android.media.MediaRoute2Info.TYPE_GROUP;
import static android.media.MediaRoute2Info.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_SPEAKER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_TV;
import static android.media.MediaRoute2Info.TYPE_UNKNOWN;

import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_CAR;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_COMPUTER;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_GAME_CONSOLE;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_GROUP;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_SMARTPHONE;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_SMARTWATCH;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_TABLET;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_TABLET_DOCKED;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_TV;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN;

import android.content.IntentFilter;
import android.media.MediaRoute2Info;
import android.media.RouteDiscoveryPreference;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRouter2Utils {
    static final String FEATURE_EMPTY = "android.media.route.feature.EMPTY";
    static final String FEATURE_REMOTE_GROUP_PLAYBACK =
            "android.media.route.feature.REMOTE_GROUP_PLAYBACK";

    // Used in MediaRoute2Info#getExtras()
    static final String KEY_EXTRAS = "androidx.mediarouter.media.KEY_EXTRAS";
    static final String KEY_CONTROL_FILTERS = "androidx.mediarouter.media.KEY_CONTROL_FILTERS";
    static final String KEY_DEVICE_TYPE = "androidx.mediarouter.media.KEY_DEVICE_TYPE";
    static final String KEY_PLAYBACK_TYPE = "androidx.mediarouter.media.KEY_PLAYBACK_TYPE";
    static final String KEY_ORIGINAL_ROUTE_ID = "androidx.mediarouter.media.KEY_ORIGINAL_ROUTE_ID";

    // Used in RoutingController#getControlHints()
    static final String KEY_MESSENGER = "androidx.mediarouter.media.KEY_MESSENGER";
    static final String KEY_SESSION_NAME = "androidx.mediarouter.media.KEY_SESSION_NAME";
    static final String KEY_GROUP_ROUTE = "androidx.mediarouter.media.KEY_GROUP_ROUTE";

    // TODO(b/282263784): Remove the following constants in favor of using instead SDK constants,
    // once the SDK constants become available.
    private static final int TYPE_REMOTE_AUDIO_VIDEO_RECEIVER = 1003;
    private static final int TYPE_REMOTE_TABLET = 1004;
    private static final int TYPE_REMOTE_TABLET_DOCKED = 1005;
    private static final int TYPE_REMOTE_COMPUTER = 1006;
    private static final int TYPE_REMOTE_GAME_CONSOLE = 1007;
    private static final int TYPE_REMOTE_CAR = 1008;
    private static final int TYPE_REMOTE_SMARTWATCH = 1009;
    private static final int TYPE_REMOTE_SMARTPHONE = 1010;
    private static final int TYPE_GROUP = 2000;

    private MediaRouter2Utils() {}

    @OptIn(markerClass = androidx.core.os.BuildCompat.PrereleaseSdkCheck.class)
    @Nullable
    public static MediaRoute2Info toFwkMediaRoute2Info(@Nullable MediaRouteDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        } else if (TextUtils.isEmpty(descriptor.getId())
                || TextUtils.isEmpty(descriptor.getName())) {
            // Skip descriptors with invalid name or id. See b/178554913.
            return null;
        }

        MediaRoute2Info.Builder builder = new MediaRoute2Info.Builder(descriptor.getId(),
                descriptor.getName())
                .setDescription(descriptor.getDescription())
                .setConnectionState(descriptor.getConnectionState())
                .setVolumeHandling(descriptor.getVolumeHandling())
                .setVolume(descriptor.getVolume())
                .setVolumeMax(descriptor.getVolumeMax())
                .addFeatures(toFeatures(descriptor.getControlFilters()))
                .setIconUri(descriptor.getIconUri())
                //TODO: set client package name
                //.setClientPackageName(clientMap.get(device.getDeviceId()))
                ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.setDeduplicationIds(builder, descriptor.getDeduplicationIds());
            Api34Impl.copyDescriptorVisibilityToBuilder(builder, descriptor);
            Api34Impl.setDeviceType(
                    builder, androidXDeviceTypeToFwkDeviceType(descriptor.getDeviceType()));
        }

        switch (descriptor.getDeviceType()) {
            case DEVICE_TYPE_TV:
                builder.addFeature(FEATURE_REMOTE_VIDEO_PLAYBACK);
                // fall through
            case DEVICE_TYPE_SPEAKER:
                builder.addFeature(FEATURE_REMOTE_AUDIO_PLAYBACK);
        }
        if (!descriptor.getGroupMemberIds().isEmpty()) {
            builder.addFeature(FEATURE_REMOTE_GROUP_PLAYBACK);
        }

        // Since MediaRouter2Info has no public APIs to get/set device types and control filters,
        // We use extras for passing those kinds of information.
        Bundle extras = new Bundle();
        extras.putBundle(KEY_EXTRAS, descriptor.getExtras());
        extras.putParcelableArrayList(KEY_CONTROL_FILTERS,
                new ArrayList<>(descriptor.getControlFilters()));
        extras.putInt(KEY_DEVICE_TYPE, descriptor.getDeviceType());
        extras.putInt(KEY_PLAYBACK_TYPE, descriptor.getPlaybackType());
        extras.putString(KEY_ORIGINAL_ROUTE_ID, descriptor.getId());
        builder.setExtras(extras);

        // This is a workaround for preventing IllegalArgumentException in MediaRoute2Info.
        if (descriptor.getControlFilters().isEmpty()) {
            builder.addFeature(FEATURE_EMPTY);
        }

        return builder.build();
    }

    @OptIn(markerClass = androidx.core.os.BuildCompat.PrereleaseSdkCheck.class)
    @Nullable
    public static MediaRouteDescriptor toMediaRouteDescriptor(
            @Nullable MediaRoute2Info fwkMediaRoute2Info) {
        if (fwkMediaRoute2Info == null) {
            return null;
        }

        MediaRouteDescriptor.Builder builder = new MediaRouteDescriptor.Builder(
                fwkMediaRoute2Info.getId(), fwkMediaRoute2Info.getName().toString())
                .setConnectionState(fwkMediaRoute2Info.getConnectionState())
                .setVolumeHandling(fwkMediaRoute2Info.getVolumeHandling())
                .setVolumeMax(fwkMediaRoute2Info.getVolumeMax())
                .setVolume(fwkMediaRoute2Info.getVolume())
                .setExtras(fwkMediaRoute2Info.getExtras())
                .setEnabled(true)
                .setCanDisconnect(false);

        @RouteInfo.DeviceType int deviceTypeInRouteInfo = DEVICE_TYPE_UNKNOWN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setDeduplicationIds(Api34Impl.getDeduplicationIds(fwkMediaRoute2Info));
            deviceTypeInRouteInfo =
                    fwkDeviceTypeToAndroidXDeviceType(Api34Impl.getType(fwkMediaRoute2Info));
        }

        CharSequence description = fwkMediaRoute2Info.getDescription();
        if (description != null) {
            builder.setDescription(description.toString());
        }

        Uri iconUri = fwkMediaRoute2Info.getIconUri();
        if (iconUri != null) {
            builder.setIconUri(iconUri);
        }

        Bundle extras = fwkMediaRoute2Info.getExtras();
        if (extras == null || !extras.containsKey(KEY_EXTRAS)
                || !extras.containsKey(KEY_DEVICE_TYPE)
                || !extras.containsKey(KEY_CONTROL_FILTERS)) {
            // Filter out routes from non-AndroidX MediaRouteProviderService.
            return null;
        }

        builder.setExtras(extras.getBundle(KEY_EXTRAS));
        builder.setDeviceType(
                deviceTypeInRouteInfo != DEVICE_TYPE_UNKNOWN
                        ? deviceTypeInRouteInfo
                        : extras.getInt(KEY_DEVICE_TYPE, DEVICE_TYPE_UNKNOWN));
        builder.setPlaybackType(extras.getInt(KEY_PLAYBACK_TYPE, RouteInfo.PLAYBACK_TYPE_REMOTE));

        List<IntentFilter> controlFilters = extras.getParcelableArrayList(KEY_CONTROL_FILTERS);
        if (controlFilters != null) {
            builder.addControlFilters(controlFilters);
        }

        // TODO: Set 'dynamic group route' related values properly
        // builder.setIsDynamicGroupRoute();
        // builder.addGroupMemberIds();

        return builder.build();
    }

    static Collection<String> toFeatures(List<IntentFilter> controlFilters) {
        Set<String> features = new HashSet<>();
        for (IntentFilter filter : controlFilters) {
            int count = filter.countCategories();
            for (int i = 0; i < count; i++) {
                features.add(toRouteFeature(filter.getCategory(i)));
            }
        }
        return features;
    }

    @NonNull
    static List<IntentFilter> toControlFilters(@Nullable Collection<String> features) {
        if (features == null) {
            return new ArrayList<>();
        }

        List<IntentFilter> controlFilters = new ArrayList<>();
        Set<String> featuresSet = new ArraySet<>();
        for (String feature : features) {
            // A feature should be unique.
            if (featuresSet.contains(feature)) {
                continue;
            }
            featuresSet.add(feature);

            IntentFilter filter = new IntentFilter();
            filter.addCategory(toControlCategory(feature));
            // TODO: Add actions by using extras. (see RemotePlaybackClient#detectFeatures())
            // filter.addAction(MediaControlIntent.ACTION_PLAY);
            // filter.addAction(MediaControlIntent.ACTION_SEEK);

            controlFilters.add(filter);
        }

        return controlFilters;
    }

    @NonNull
    static List<String> getRouteIds(@Nullable List<MediaRoute2Info> routes) {
        if (routes == null) {
            return new ArrayList<>();
        }

        List<String> routeIds = new ArrayList<>();
        for (MediaRoute2Info route : routes) {
            if (route == null) {
                continue;
            }
            routeIds.add(route.getId());
        }
        return routeIds;
    }

    @NonNull
    static MediaRouteDiscoveryRequest toMediaRouteDiscoveryRequest(
            @NonNull RouteDiscoveryPreference preference) {
        List<String> controlCategories = new ArrayList<>();
        for (String feature : preference.getPreferredFeatures()) {
            controlCategories.add(MediaRouter2Utils.toControlCategory(feature));
        }
        MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategories(controlCategories)
                .build();

        return new MediaRouteDiscoveryRequest(selector, preference.shouldPerformActiveScan());
    }

    @NonNull
    static RouteDiscoveryPreference toDiscoveryPreference(
            @Nullable MediaRouteDiscoveryRequest discoveryRequest) {
        if (discoveryRequest == null || !discoveryRequest.isValid()) {
            return new RouteDiscoveryPreference.Builder(new ArrayList<>(), false).build();
        }
        boolean activeScan = discoveryRequest.isActiveScan();

        List<String> routeFeatures = new ArrayList<>();
        for (String controlCategory : discoveryRequest.getSelector().getControlCategories()) {
            routeFeatures.add(MediaRouter2Utils.toRouteFeature(controlCategory));
        }
        return new RouteDiscoveryPreference.Builder(routeFeatures, activeScan).build();
    }

    static String toRouteFeature(String controlCategory) {
        switch (controlCategory) {
            case MediaControlIntent.CATEGORY_LIVE_AUDIO:
                return FEATURE_LIVE_AUDIO;
            case MediaControlIntent.CATEGORY_LIVE_VIDEO:
                return FEATURE_LIVE_VIDEO;
            case MediaControlIntent.CATEGORY_REMOTE_PLAYBACK:
                return FEATURE_REMOTE_PLAYBACK;
        }
        return controlCategory;
    }

    static String toControlCategory(String routeFeature) {
        switch (routeFeature) {
            case FEATURE_LIVE_AUDIO:
                return MediaControlIntent.CATEGORY_LIVE_AUDIO;
            case FEATURE_LIVE_VIDEO:
                return MediaControlIntent.CATEGORY_LIVE_VIDEO;
            case FEATURE_REMOTE_PLAYBACK:
                return MediaControlIntent.CATEGORY_REMOTE_PLAYBACK;
        }
        return routeFeature;
    }

    @RouteInfo.DeviceType
    private static int fwkDeviceTypeToAndroidXDeviceType(int fwkDeviceType) {
        switch (fwkDeviceType) {
            case TYPE_REMOTE_TV:
                return DEVICE_TYPE_TV;
            case TYPE_REMOTE_SPEAKER:
                return DEVICE_TYPE_SPEAKER;
            case TYPE_REMOTE_AUDIO_VIDEO_RECEIVER:
                return DEVICE_TYPE_AUDIO_VIDEO_RECEIVER;
            case TYPE_REMOTE_TABLET:
                return DEVICE_TYPE_TABLET;
            case TYPE_REMOTE_TABLET_DOCKED:
                return DEVICE_TYPE_TABLET_DOCKED;
            case TYPE_REMOTE_COMPUTER:
                return DEVICE_TYPE_COMPUTER;
            case TYPE_REMOTE_GAME_CONSOLE:
                return DEVICE_TYPE_GAME_CONSOLE;
            case TYPE_REMOTE_CAR:
                return DEVICE_TYPE_CAR;
            case TYPE_REMOTE_SMARTWATCH:
                return DEVICE_TYPE_SMARTWATCH;
            case TYPE_REMOTE_SMARTPHONE:
                return DEVICE_TYPE_SMARTPHONE;
            case TYPE_GROUP:
                return DEVICE_TYPE_GROUP;
            default:
                return DEVICE_TYPE_UNKNOWN;
        }
    }

    private static int androidXDeviceTypeToFwkDeviceType(
            @RouteInfo.DeviceType int androidXDeviceType) {
        switch (androidXDeviceType) {
            case DEVICE_TYPE_TV:
                return TYPE_REMOTE_TV;
            case DEVICE_TYPE_SPEAKER:
                return TYPE_REMOTE_SPEAKER;
            case DEVICE_TYPE_AUDIO_VIDEO_RECEIVER:
                return TYPE_REMOTE_AUDIO_VIDEO_RECEIVER;
            case DEVICE_TYPE_TABLET:
                return TYPE_REMOTE_TABLET;
            case DEVICE_TYPE_TABLET_DOCKED:
                return TYPE_REMOTE_TABLET_DOCKED;
            case DEVICE_TYPE_COMPUTER:
                return TYPE_REMOTE_COMPUTER;
            case DEVICE_TYPE_GAME_CONSOLE:
                return TYPE_REMOTE_GAME_CONSOLE;
            case DEVICE_TYPE_CAR:
                return TYPE_REMOTE_CAR;
            case DEVICE_TYPE_SMARTWATCH:
                return TYPE_REMOTE_SMARTWATCH;
            case DEVICE_TYPE_SMARTPHONE:
                return TYPE_REMOTE_SMARTPHONE;
            case DEVICE_TYPE_GROUP:
                return TYPE_GROUP;
            default:
                return TYPE_UNKNOWN;
        }
    }

    @RequiresApi(api = 34)
    private static final class Api34Impl {

        @DoNotInline
        public static void setDeduplicationIds(
                MediaRoute2Info.Builder builder, Set<String> deduplicationIds) {
            builder.setDeduplicationIds(deduplicationIds);
        }

        @DoNotInline
        public static Set<String> getDeduplicationIds(MediaRoute2Info fwkMediaRoute2Info) {
            return fwkMediaRoute2Info.getDeduplicationIds();
        }

        @DoNotInline
        public static void copyDescriptorVisibilityToBuilder(MediaRoute2Info.Builder builder,
                MediaRouteDescriptor descriptor) {
            if (descriptor.isVisibilityPublic()) {
                builder.setVisibilityPublic();
            } else {
                builder.setVisibilityRestricted(descriptor.getAllowedPackages());
            }
        }

        @DoNotInline
        public static void setDeviceType(MediaRoute2Info.Builder builder, int deviceType) {
            builder.setType(deviceType);
        }

        @DoNotInline
        public static int getType(MediaRoute2Info fwkMediaRoute2Info) {
            return fwkMediaRoute2Info.getType();
        }
    }
}
