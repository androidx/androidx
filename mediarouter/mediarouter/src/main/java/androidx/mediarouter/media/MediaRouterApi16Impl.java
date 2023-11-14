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

import android.content.Context;
import android.media.MediaRouter;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods for {@link MediaRouter} for API 16 and above. This class is used for API
 * Compatibility.
 *
 * @see <a href="http://go/androidx/api_guidelines/compat.md">Implementing compatibility</a>
 */
@RequiresApi(16)
/* package */ final class MediaRouterApi16Impl {
    private static final String TAG = "MediaRouterJellybean";

    public static final int ROUTE_TYPE_LIVE_AUDIO = 0x1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 0x2;
    public static final int ROUTE_TYPE_USER = 0x00800000;

    public static final int ALL_ROUTE_TYPES =
            MediaRouterApi16Impl.ROUTE_TYPE_LIVE_AUDIO
                    | MediaRouterApi16Impl.ROUTE_TYPE_LIVE_VIDEO
                    | MediaRouterApi16Impl.ROUTE_TYPE_USER;

    private MediaRouterApi16Impl() {}

    @DoNotInline
    public static android.media.MediaRouter getMediaRouter(Context context) {
        return (android.media.MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
    }

    @DoNotInline
    public static List<MediaRouter.RouteInfo> getRoutes(android.media.MediaRouter router) {
        final int count = router.getRouteCount();
        List<MediaRouter.RouteInfo> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(router.getRouteAt(i));
        }
        return out;
    }

    @DoNotInline
    public static MediaRouter.RouteInfo getSelectedRoute(
            android.media.MediaRouter router, int type) {
        return router.getSelectedRoute(type);
    }

    @DoNotInline
    public static void selectRoute(
            android.media.MediaRouter router,
            int types,
            android.media.MediaRouter.RouteInfo route) {
        router.selectRoute(types, route);
    }

    @DoNotInline
    public static void removeCallback(
            android.media.MediaRouter router, android.media.MediaRouter.Callback callback) {
        router.removeCallback(callback);
    }

    @DoNotInline
    public static android.media.MediaRouter.RouteCategory createRouteCategory(
            android.media.MediaRouter router, String name, boolean isGroupable) {
        return router.createRouteCategory(name, isGroupable);
    }

    @DoNotInline
    public static MediaRouter.UserRouteInfo createUserRoute(
            android.media.MediaRouter router, android.media.MediaRouter.RouteCategory category) {
        return router.createUserRoute(category);
    }

    @DoNotInline
    public static void addUserRoute(
            android.media.MediaRouter router, android.media.MediaRouter.UserRouteInfo route) {
        router.addUserRoute(route);
    }

    @DoNotInline
    public static void removeUserRoute(
            android.media.MediaRouter router, android.media.MediaRouter.UserRouteInfo route) {
        try {
            router.removeUserRoute(route);
        } catch (IllegalArgumentException e) {
            // Work around for https://issuetracker.google.com/issues/202931542.
            Log.w(TAG, "Failed to remove user route", e);
        }
    }

    @DoNotInline
    public static android.media.MediaRouter.VolumeCallback createVolumeCallback(
            VolumeCallback callback) {
        return new VolumeCallbackProxy<>(callback);
    }

    @RequiresApi(16)
    public static final class RouteInfo {

        private RouteInfo() {}

        @DoNotInline
        @NonNull
        public static CharSequence getName(
                @NonNull android.media.MediaRouter.RouteInfo route, @NonNull Context context) {
            return route.getName(context);
        }

        @DoNotInline
        public static int getSupportedTypes(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getSupportedTypes();
        }

        @DoNotInline
        public static int getPlaybackType(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getPlaybackType();
        }

        @DoNotInline
        public static int getPlaybackStream(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getPlaybackStream();
        }

        @DoNotInline
        public static int getVolume(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolume();
        }

        @DoNotInline
        public static int getVolumeMax(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolumeMax();
        }

        @DoNotInline
        public static int getVolumeHandling(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolumeHandling();
        }

        @DoNotInline
        @Nullable
        public static Object getTag(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getTag();
        }

        @DoNotInline
        public static void setTag(
                @NonNull android.media.MediaRouter.RouteInfo route, @Nullable Object tag) {
            route.setTag(tag);
        }

        @DoNotInline
        public static void requestSetVolume(
                @NonNull android.media.MediaRouter.RouteInfo route, int volume) {
            route.requestSetVolume(volume);
        }

        @DoNotInline
        public static void requestUpdateVolume(
                @NonNull android.media.MediaRouter.RouteInfo route, int direction) {
            route.requestUpdateVolume(direction);
        }
    }

    @RequiresApi(16)
    public static final class UserRouteInfo {

        private UserRouteInfo() {}

        @DoNotInline
        public static void setName(
                @NonNull android.media.MediaRouter.UserRouteInfo route,
                @NonNull CharSequence name) {
            route.setName(name);
        }

        @DoNotInline
        public static void setPlaybackType(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int type) {
            route.setPlaybackType(type);
        }

        @DoNotInline
        public static void setPlaybackStream(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int stream) {
            route.setPlaybackStream(stream);
        }

        @DoNotInline
        public static void setVolume(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int volume) {
            route.setVolume(volume);
        }

        @DoNotInline
        public static void setVolumeMax(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int volumeMax) {
            route.setVolumeMax(volumeMax);
        }

        @DoNotInline
        public static void setVolumeHandling(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int volumeHandling) {
            route.setVolumeHandling(volumeHandling);
        }

        @DoNotInline
        public static void setVolumeCallback(
                @NonNull android.media.MediaRouter.UserRouteInfo route,
                @NonNull android.media.MediaRouter.VolumeCallback volumeCallback) {
            route.setVolumeCallback(volumeCallback);
        }

        @DoNotInline
        public static void setRemoteControlClient(
                @NonNull android.media.MediaRouter.UserRouteInfo route,
                @Nullable android.media.RemoteControlClient rcc) {
            route.setRemoteControlClient(rcc);
        }
    }

    public interface Callback {
        void onRouteSelected(int type, @NonNull android.media.MediaRouter.RouteInfo route);

        void onRouteUnselected(int type, @NonNull android.media.MediaRouter.RouteInfo route);

        void onRouteAdded(@NonNull android.media.MediaRouter.RouteInfo route);

        void onRouteRemoved(@NonNull android.media.MediaRouter.RouteInfo route);

        void onRouteChanged(@NonNull android.media.MediaRouter.RouteInfo route);

        void onRouteGrouped(@NonNull android.media.MediaRouter.RouteInfo route,
                @NonNull android.media.MediaRouter.RouteGroup group, int index);

        void onRouteUngrouped(@NonNull android.media.MediaRouter.RouteInfo route,
                @NonNull android.media.MediaRouter.RouteGroup group);

        void onRouteVolumeChanged(@NonNull android.media.MediaRouter.RouteInfo route);
    }

    public interface VolumeCallback {
        void onVolumeSetRequest(@NonNull android.media.MediaRouter.RouteInfo route, int volume);

        void onVolumeUpdateRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                int direction);
    }

    static class CallbackProxy<T extends Callback>
            extends android.media.MediaRouter.Callback {
        protected final T mCallback;

        CallbackProxy(T callback) {
            mCallback = callback;
        }

        @Override
        public void onRouteSelected(android.media.MediaRouter router,
                int type, android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteSelected(type, route);
        }

        @Override
        public void onRouteUnselected(android.media.MediaRouter router,
                int type, android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteUnselected(type, route);
        }

        @Override
        public void onRouteAdded(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteAdded(route);
        }

        @Override
        public void onRouteRemoved(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteRemoved(route);
        }

        @Override
        public void onRouteChanged(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteChanged(route);
        }

        @Override
        public void onRouteGrouped(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route,
                android.media.MediaRouter.RouteGroup group, int index) {
            mCallback.onRouteGrouped(route, group, index);
        }

        @Override
        public void onRouteUngrouped(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route,
                android.media.MediaRouter.RouteGroup group) {
            mCallback.onRouteUngrouped(route, group);
        }

        @Override
        public void onRouteVolumeChanged(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRouteVolumeChanged(route);
        }
    }

    static class VolumeCallbackProxy<T extends VolumeCallback>
            extends android.media.MediaRouter.VolumeCallback {
        protected final T mCallback;

        VolumeCallbackProxy(T callback) {
            mCallback = callback;
        }

        @Override
        public void onVolumeSetRequest(android.media.MediaRouter.RouteInfo route, int volume) {
            mCallback.onVolumeSetRequest(route, volume);
        }

        @Override
        public void onVolumeUpdateRequest(android.media.MediaRouter.RouteInfo route,
                int direction) {
            mCallback.onVolumeUpdateRequest(route, direction);
        }
    }
}
