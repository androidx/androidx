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

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRouter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(16)
final class MediaRouterJellybean {
    private static final String TAG = "MediaRouterJellybean";

    public static final int ROUTE_TYPE_LIVE_AUDIO = 0x1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 0x2;
    public static final int ROUTE_TYPE_USER = 0x00800000;

    public static final int ALL_ROUTE_TYPES =
            MediaRouterJellybean.ROUTE_TYPE_LIVE_AUDIO
                    | MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO
                    | MediaRouterJellybean.ROUTE_TYPE_USER;

    public static android.media.MediaRouter getMediaRouter(Context context) {
        return (android.media.MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
    }

    public static List<MediaRouter.RouteInfo> getRoutes(android.media.MediaRouter router) {
        final int count = router.getRouteCount();
        List<MediaRouter.RouteInfo> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(router.getRouteAt(i));
        }
        return out;
    }

    public static MediaRouter.RouteInfo getSelectedRoute(android.media.MediaRouter router,
            int type) {
        return router.getSelectedRoute(type);
    }

    public static void selectRoute(android.media.MediaRouter router, int types,
            android.media.MediaRouter.RouteInfo route) {
        router.selectRoute(types, route);
    }

    public static void addCallback(android.media.MediaRouter router, int types,
            android.media.MediaRouter.Callback callback) {
        router.addCallback(types, callback);
    }

    public static void removeCallback(android.media.MediaRouter router,
            android.media.MediaRouter.Callback callback) {
        router.removeCallback(callback);
    }

    public static android.media.MediaRouter.RouteCategory createRouteCategory(
            android.media.MediaRouter router,
            String name, boolean isGroupable) {
        return router.createRouteCategory(name, isGroupable);
    }

    public static MediaRouter.UserRouteInfo createUserRoute(android.media.MediaRouter router,
            android.media.MediaRouter.RouteCategory category) {
        return router.createUserRoute(category);
    }

    public static void addUserRoute(android.media.MediaRouter router,
            android.media.MediaRouter.UserRouteInfo route) {
        router.addUserRoute(route);
    }

    public static void removeUserRoute(android.media.MediaRouter router,
            android.media.MediaRouter.UserRouteInfo route) {
        try {
            router.removeUserRoute(route);
        } catch (IllegalArgumentException e) {
            // Work around for https://issuetracker.google.com/issues/202931542.
            Log.w(TAG, "Failed to remove user route", e);
        }
    }

    public static CallbackProxy<Callback> createCallback(Callback callback) {
        return new CallbackProxy<>(callback);
    }

    public static VolumeCallbackProxy<VolumeCallback> createVolumeCallback(
            VolumeCallback callback) {
        return new VolumeCallbackProxy<>(callback);
    }

    public static final class RouteInfo {
        @NonNull
        public static CharSequence getName(@NonNull android.media.MediaRouter.RouteInfo route,
                @NonNull Context context) {
            return route.getName(context);
        }

        public static int getSupportedTypes(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getSupportedTypes();
        }

        public static int getPlaybackType(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getPlaybackType();
        }

        public static int getPlaybackStream(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getPlaybackStream();
        }

        public static int getVolume(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolume();
        }

        public static int getVolumeMax(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolumeMax();
        }

        public static int getVolumeHandling(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getVolumeHandling();
        }

        @Nullable
        public static Object getTag(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.getTag();
        }

        public static void setTag(@NonNull android.media.MediaRouter.RouteInfo route,
                @Nullable Object tag) {
            route.setTag(tag);
        }

        public static void requestSetVolume(@NonNull android.media.MediaRouter.RouteInfo route,
                int volume) {
            route.requestSetVolume(volume);
        }

        public static void requestUpdateVolume(
                @NonNull android.media.MediaRouter.RouteInfo route, int direction) {
            route.requestUpdateVolume(direction);
        }

        private RouteInfo() {
        }
    }

    public static final class UserRouteInfo {
        public static void setName(@NonNull android.media.MediaRouter.UserRouteInfo route,
                @NonNull CharSequence name) {
            route.setName(name);
        }

        public static void setPlaybackType(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int type) {
            route.setPlaybackType(type);
        }

        public static void setPlaybackStream(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int stream) {
            route.setPlaybackStream(stream);
        }

        public static void setVolume(@NonNull android.media.MediaRouter.UserRouteInfo route,
                int volume) {
            route.setVolume(volume);
        }

        public static void setVolumeMax(@NonNull android.media.MediaRouter.UserRouteInfo route,
                int volumeMax) {
            route.setVolumeMax(volumeMax);
        }

        public static void setVolumeHandling(
                @NonNull android.media.MediaRouter.UserRouteInfo route, int volumeHandling) {
            route.setVolumeHandling(volumeHandling);
        }

        public static void setVolumeCallback(@NonNull android.media.MediaRouter.UserRouteInfo route,
                @NonNull android.media.MediaRouter.VolumeCallback volumeCallback) {
            route.setVolumeCallback(volumeCallback);
        }

        public static void setRemoteControlClient(
                @NonNull android.media.MediaRouter.UserRouteInfo route,
                @Nullable android.media.RemoteControlClient rcc) {
            route.setRemoteControlClient(rcc);
        }

        private UserRouteInfo() {
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

    /**
     * Workaround for limitations of selectRoute() on JB and JB MR1.
     * Do not use on JB MR2 and above.
     */
    public static final class SelectRouteWorkaround {
        private Method mSelectRouteIntMethod;

        SelectRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                mSelectRouteIntMethod = android.media.MediaRouter.class.getMethod(
                        "selectRouteInt", int.class, android.media.MediaRouter.RouteInfo.class);
            } catch (NoSuchMethodException ex) {
            }
        }

        // Suppress BanUncheckedReflection as the lint raises false-positive exception around this
        // code: the reflection is used for a specific Android version and the real Android API
        // check is happening in the class' constructor and in SystemMediaRouteProvider#obtain
        @SuppressLint("BanUncheckedReflection")
        public void selectRoute(@NonNull android.media.MediaRouter router, int types,
                @NonNull android.media.MediaRouter.RouteInfo route) {
            int routeTypes = route.getSupportedTypes();
            if ((routeTypes & ROUTE_TYPE_USER) == 0) {
                // Handle non-user routes.
                // On JB and JB MR1, the selectRoute() API only supports programmatically
                // selecting user routes.  So instead we rely on the hidden selectRouteInt()
                // method on these versions of the platform.
                // This limitation was removed in JB MR2.
                if (mSelectRouteIntMethod != null) {
                    try {
                        mSelectRouteIntMethod.invoke(router, types, route);
                        return; // success!
                    } catch (IllegalAccessException ex) {
                        Log.w(TAG, "Cannot programmatically select non-user route.  "
                                + "Media routing may not work.", ex);
                    } catch (InvocationTargetException ex) {
                        Log.w(TAG, "Cannot programmatically select non-user route.  "
                                + "Media routing may not work.", ex);
                    }
                } else {
                    Log.w(TAG, "Cannot programmatically select non-user route "
                            + "because the platform is missing the selectRouteInt() "
                            + "method.  Media routing may not work.");
                }
            }

            // Default handling.
            router.selectRoute(types, route);
        }
    }

    /**
     * Workaround the fact that the getDefaultRoute() method does not exist in JB and JB MR1.
     * Do not use on JB MR2 and above.
     */
    public static final class GetDefaultRouteWorkaround {
        private Method mGetSystemAudioRouteMethod;

        GetDefaultRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                mGetSystemAudioRouteMethod =
                        android.media.MediaRouter.class.getMethod("getSystemAudioRoute");
            } catch (NoSuchMethodException ex) {
            }
        }

        // Suppress BanUncheckedReflection as the lint raises false-positive exception around this
        // code: the reflection is used for a specific Android version and the real Android API
        // check is happening in the class' constructor and in SystemMediaRouteProvider#obtain
        @SuppressLint("BanUncheckedReflection")
        @NonNull
        public Object getDefaultRoute(@NonNull android.media.MediaRouter router) {
            if (mGetSystemAudioRouteMethod != null) {
                try {
                    return mGetSystemAudioRouteMethod.invoke(router);
                } catch (IllegalAccessException ex) {
                } catch (InvocationTargetException ex) {
                }
            }

            // Could not find the method or it does not work.
            // Return the first route and hope for the best.
            return router.getRouteAt(0);
        }
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

    private MediaRouterJellybean() {
    }
}
