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

import android.media.MediaRouter;
import android.util.Log;
import android.view.Display;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Provides methods for {@link MediaRouter} for API 17 and above. This class is used for API
 * Compatibility.
 *
 * @see <a href="http://go/androidx/api_guidelines/compat.md">Implementing compatibility</a>
 */
@RequiresApi(17)
/* package */ final class MediaRouterApi17Impl {
    private static final String TAG = "MediaRouterJellybeanMr1";

    private MediaRouterApi17Impl() {}

    public static android.media.MediaRouter.Callback createCallback(Callback callback) {
        return new CallbackProxy<>(callback);
    }

    public static final class RouteInfo {

        private RouteInfo() {}

        @DoNotInline
        public static boolean isEnabled(@NonNull android.media.MediaRouter.RouteInfo route) {
            return route.isEnabled();
        }

        @DoNotInline
        @Nullable
        public static Display getPresentationDisplay(
                @NonNull android.media.MediaRouter.RouteInfo route) {
            // android.media.MediaRouter.RouteInfo.getPresentationDisplay() was
            // added in API 17. However, some factory releases of JB MR1 missed it.
            try {
                return route.getPresentationDisplay();
            } catch (NoSuchMethodError ex) {
                Log.w(TAG, "Cannot get presentation display for the route.", ex);
            }
            return null;
        }
    }

    public interface Callback extends MediaRouterApi16Impl.Callback {
        void onRoutePresentationDisplayChanged(@NonNull android.media.MediaRouter.RouteInfo route);
    }

    static class CallbackProxy<T extends Callback> extends MediaRouterApi16Impl.CallbackProxy<T> {
        CallbackProxy(T callback) {
            super(callback);
        }

        @Override
        public void onRoutePresentationDisplayChanged(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRoutePresentationDisplayChanged(route);
        }
    }
}
