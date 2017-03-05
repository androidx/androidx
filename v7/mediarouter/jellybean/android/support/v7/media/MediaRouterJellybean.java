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

package android.support.v7.media;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(16)
final class MediaRouterJellybean {
    private static final String TAG = "MediaRouterJellybean";

    // android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP = 0x80;
    // android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 0x100;
    // android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 0x200;
    public static final int DEVICE_OUT_BLUETOOTH = 0x80 | 0x100 | 0x200;

    public static final int ROUTE_TYPE_LIVE_AUDIO = 0x1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 0x2;
    public static final int ROUTE_TYPE_USER = 0x00800000;

    public static final int ALL_ROUTE_TYPES =
            MediaRouterJellybean.ROUTE_TYPE_LIVE_AUDIO
            | MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO
            | MediaRouterJellybean.ROUTE_TYPE_USER;

    public static Object getMediaRouter(Context context) {
        return context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List getRoutes(Object routerObj) {
        final android.media.MediaRouter router = (android.media.MediaRouter)routerObj;
        final int count = router.getRouteCount();
        List out = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            out.add(router.getRouteAt(i));
        }
        return out;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List getCategories(Object routerObj) {
        final android.media.MediaRouter router = (android.media.MediaRouter)routerObj;
        final int count = router.getCategoryCount();
        List out = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            out.add(router.getCategoryAt(i));
        }
        return out;
    }

    public static Object getSelectedRoute(Object routerObj, int type) {
        return ((android.media.MediaRouter)routerObj).getSelectedRoute(type);
    }

    public static void selectRoute(Object routerObj, int types, Object routeObj) {
        ((android.media.MediaRouter)routerObj).selectRoute(types,
                (android.media.MediaRouter.RouteInfo)routeObj);
    }

    public static void addCallback(Object routerObj, int types, Object callbackObj) {
        ((android.media.MediaRouter)routerObj).addCallback(types,
                (android.media.MediaRouter.Callback)callbackObj);
    }

    public static void removeCallback(Object routerObj, Object callbackObj) {
        ((android.media.MediaRouter)routerObj).removeCallback(
                (android.media.MediaRouter.Callback)callbackObj);
    }

    public static Object createRouteCategory(Object routerObj,
            String name, boolean isGroupable) {
        return ((android.media.MediaRouter)routerObj).createRouteCategory(name, isGroupable);
    }

    public static Object createUserRoute(Object routerObj, Object categoryObj) {
        return ((android.media.MediaRouter)routerObj).createUserRoute(
                (android.media.MediaRouter.RouteCategory)categoryObj);
    }

    public static void addUserRoute(Object routerObj, Object routeObj) {
        ((android.media.MediaRouter)routerObj).addUserRoute(
                (android.media.MediaRouter.UserRouteInfo)routeObj);
    }

    public static void removeUserRoute(Object routerObj, Object routeObj) {
        ((android.media.MediaRouter)routerObj).removeUserRoute(
                (android.media.MediaRouter.UserRouteInfo)routeObj);
    }

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static Object createVolumeCallback(VolumeCallback callback) {
        return new VolumeCallbackProxy<VolumeCallback>(callback);
    }

    static boolean checkRoutedToBluetooth(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(
                    Context.AUDIO_SERVICE);
            Method method = audioManager.getClass().getDeclaredMethod(
                    "getDevicesForStream", int.class);
            int device = (Integer) method.invoke(audioManager, AudioManager.STREAM_MUSIC);
            return (device & DEVICE_OUT_BLUETOOTH) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static final class RouteInfo {
        public static CharSequence getName(Object routeObj, Context context) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getName(context);
        }

        public static CharSequence getStatus(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getStatus();
        }

        public static int getSupportedTypes(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getSupportedTypes();
        }

        public static Object getCategory(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getCategory();
        }

        public static Drawable getIconDrawable(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getIconDrawable();
        }

        public static int getPlaybackType(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getPlaybackType();
        }

        public static int getPlaybackStream(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getPlaybackStream();
        }

        public static int getVolume(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getVolume();
        }

        public static int getVolumeMax(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getVolumeMax();
        }

        public static int getVolumeHandling(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getVolumeHandling();
        }

        public static Object getTag(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getTag();
        }

        public static void setTag(Object routeObj, Object tag) {
            ((android.media.MediaRouter.RouteInfo)routeObj).setTag(tag);
        }

        public static void requestSetVolume(Object routeObj, int volume) {
            ((android.media.MediaRouter.RouteInfo)routeObj).requestSetVolume(volume);
        }

        public static void requestUpdateVolume(Object routeObj, int direction) {
            ((android.media.MediaRouter.RouteInfo)routeObj).requestUpdateVolume(direction);
        }

        public static Object getGroup(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getGroup();
        }

        public static boolean isGroup(Object routeObj) {
            return routeObj instanceof android.media.MediaRouter.RouteGroup;
        }
    }

    public static final class RouteGroup {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static List getGroupedRoutes(Object groupObj) {
            final android.media.MediaRouter.RouteGroup group =
                    (android.media.MediaRouter.RouteGroup)groupObj;
            final int count = group.getRouteCount();
            List out = new ArrayList(count);
            for (int i = 0; i < count; i++) {
                out.add(group.getRouteAt(i));
            }
            return out;
        }
    }

    public static final class UserRouteInfo {
        public static void setName(Object routeObj, CharSequence name) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setName(name);
        }

        public static void setStatus(Object routeObj, CharSequence status) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setStatus(status);
        }

        public static void setIconDrawable(Object routeObj, Drawable icon) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setIconDrawable(icon);
        }

        public static void setPlaybackType(Object routeObj, int type) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setPlaybackType(type);
        }

        public static void setPlaybackStream(Object routeObj, int stream) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setPlaybackStream(stream);
        }

        public static void setVolume(Object routeObj, int volume) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setVolume(volume);
        }

        public static void setVolumeMax(Object routeObj, int volumeMax) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setVolumeMax(volumeMax);
        }

        public static void setVolumeHandling(Object routeObj, int volumeHandling) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setVolumeHandling(volumeHandling);
        }

        public static void setVolumeCallback(Object routeObj, Object volumeCallbackObj) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setVolumeCallback(
                    (android.media.MediaRouter.VolumeCallback)volumeCallbackObj);
        }

        public static void setRemoteControlClient(Object routeObj, Object rccObj) {
            ((android.media.MediaRouter.UserRouteInfo)routeObj).setRemoteControlClient(
                    (android.media.RemoteControlClient)rccObj);
        }
    }

    public static final class RouteCategory {
        public static CharSequence getName(Object categoryObj, Context context) {
            return ((android.media.MediaRouter.RouteCategory)categoryObj).getName(context);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static List getRoutes(Object categoryObj) {
            ArrayList out = new ArrayList();
            ((android.media.MediaRouter.RouteCategory)categoryObj).getRoutes(out);
            return out;
        }

        public static int getSupportedTypes(Object categoryObj) {
            return ((android.media.MediaRouter.RouteCategory)categoryObj).getSupportedTypes();
        }

        public static boolean isGroupable(Object categoryObj) {
            return ((android.media.MediaRouter.RouteCategory)categoryObj).isGroupable();
        }
    }

    public static interface Callback {
        public void onRouteSelected(int type, Object routeObj);
        public void onRouteUnselected(int type, Object routeObj);
        public void onRouteAdded(Object routeObj);
        public void onRouteRemoved(Object routeObj);
        public void onRouteChanged(Object routeObj);
        public void onRouteGrouped(Object routeObj, Object groupObj, int index);
        public void onRouteUngrouped(Object routeObj, Object groupObj);
        public void onRouteVolumeChanged(Object routeObj);
    }

    public static interface VolumeCallback {
        public void onVolumeSetRequest(Object routeObj, int volume);
        public void onVolumeUpdateRequest(Object routeObj, int direction);
    }

    /**
     * Workaround for limitations of selectRoute() on JB and JB MR1.
     * Do not use on JB MR2 and above.
     */
    public static final class SelectRouteWorkaround {
        private Method mSelectRouteIntMethod;

        public SelectRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                mSelectRouteIntMethod = android.media.MediaRouter.class.getMethod(
                        "selectRouteInt", int.class, android.media.MediaRouter.RouteInfo.class);
            } catch (NoSuchMethodException ex) {
            }
        }

        public void selectRoute(Object routerObj, int types, Object routeObj) {
            android.media.MediaRouter router = (android.media.MediaRouter)routerObj;
            android.media.MediaRouter.RouteInfo route =
                    (android.media.MediaRouter.RouteInfo)routeObj;

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

        public GetDefaultRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                mGetSystemAudioRouteMethod =
                        android.media.MediaRouter.class.getMethod("getSystemAudioRoute");
            } catch (NoSuchMethodException ex) {
            }
        }

        public Object getDefaultRoute(Object routerObj) {
            android.media.MediaRouter router = (android.media.MediaRouter)routerObj;

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

        public CallbackProxy(T callback) {
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

        public VolumeCallbackProxy(T callback) {
            mCallback = callback;
        }

        @Override
        public void onVolumeSetRequest(android.media.MediaRouter.RouteInfo route,
                int volume) {
            mCallback.onVolumeSetRequest(route, volume);
        }

        @Override
        public void onVolumeUpdateRequest(android.media.MediaRouter.RouteInfo route,
                int direction) {
            mCallback.onVolumeUpdateRequest(route, direction);
        }
    }
}
