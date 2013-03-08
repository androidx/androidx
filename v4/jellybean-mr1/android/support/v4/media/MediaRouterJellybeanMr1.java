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

import android.view.Display;

final class MediaRouterJellybeanMr1 {
    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static final class RouteInfo {
        public static boolean isEnabled(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).isEnabled();
        }

        public static Display getPresentationDisplay(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getPresentationDisplay();
        }
    }

    public static interface Callback extends MediaRouterJellybean.Callback {
        public void onRoutePresentationDisplayChanged(Object routeObj);
    }

    static class CallbackProxy<T extends Callback>
            extends MediaRouterJellybean.CallbackProxy<T> {
        public CallbackProxy(T callback) {
            super(callback);
        }

        @Override
        public void onRoutePresentationDisplayChanged(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRoutePresentationDisplayChanged(route);
        }
    }
}
