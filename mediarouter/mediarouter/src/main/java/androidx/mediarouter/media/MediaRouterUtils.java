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

package androidx.mediarouter.media;

import androidx.annotation.NonNull;

/** Utils for usage with platform {@link android.media.MediaRouter} */
class MediaRouterUtils {

    private MediaRouterUtils() {}

    public static android.media.MediaRouter.Callback createCallback(Callback callback) {
        return new CallbackProxy<>(callback);
    }

    public static android.media.MediaRouter.VolumeCallback createVolumeCallback(
            VolumeCallback callback) {
        return new VolumeCallbackProxy<>(callback);
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

        void onRoutePresentationDisplayChanged(@NonNull android.media.MediaRouter.RouteInfo route);
    }

    public interface VolumeCallback {
        void onVolumeSetRequest(@NonNull android.media.MediaRouter.RouteInfo route, int volume);

        void onVolumeUpdateRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                int direction);
    }

    /**
     * This proxy callback class provides a mechanism for {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} to circumvent the fact that it cannot
     * extend {@link android.media.MediaRouter.Callback}. This is because {@link
     * android.media.MediaRouter.Callback} is an abstract class (rather than an interface), and a
     * class cannot extend more than one class. Instead, {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} implements the {@link Callback} interface
     * and references an instance of this proxy class that wraps the {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} instance, to use where {@link
     * MediaRouter} expects an instance of {@link android.media.MediaRouter.Callback}.
     */
    static class CallbackProxy<T extends Callback> extends android.media.MediaRouter.Callback {
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

        @Override
        public void onRoutePresentationDisplayChanged(
                android.media.MediaRouter router, android.media.MediaRouter.RouteInfo route) {
            mCallback.onRoutePresentationDisplayChanged(route);
        }
    }

    /**
     * This proxy callback class provides a mechanism for {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} to circumvent the fact that it cannot
     * extend {@link android.media.MediaRouter.VolumeCallback}. This is because {@link
     * android.media.MediaRouter.VolumeCallback} is an abstract class (rather than an interface),
     * and a class cannot extend more than one class. Instead, {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} implements the {@link VolumeCallback}
     * interface and references an instance of this proxy class that wraps the {@link
     * PlatformMediaRouter1RouteProvider.JellybeanMr2Impl} instance, to use where {@link
     * MediaRouter} expects an instance of {@link android.media.MediaRouter.VolumeCallback}.
     */
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
