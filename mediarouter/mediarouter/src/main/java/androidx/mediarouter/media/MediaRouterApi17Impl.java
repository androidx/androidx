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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Provides methods for {@link MediaRouter} for API 17 and above. This class is used for API
 * Compatibility.
 *
 * @see <a href="http://go/androidx/api_guidelines/compat.md">Implementing compatibility</a>
 */
@RequiresApi(17)
/* package */ final class MediaRouterApi17Impl {
    private MediaRouterApi17Impl() {}

    public static android.media.MediaRouter.Callback createCallback(Callback callback) {
        return new CallbackProxy<>(callback);
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
