/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.List;

@RequiresApi(26)
class MediaBrowserCompatApi26 {
    public static Object createSearchCallback(SearchCallback callback) {
        return new SearchCallbackProxy<>(callback);
    }

    public static void search(Object browserObj, String query, Bundle extras, Object callback) {
        ((MediaBrowser) browserObj).search(query, extras,
                (MediaBrowser.SearchCallback) callback);
    }

    interface SearchCallback {
        void onSearchResult(@NonNull String query, Bundle extras, @NonNull List<?> items);
        void onError(@NonNull String query, Bundle extras);
    }

    static class SearchCallbackProxy<T extends SearchCallback> extends MediaBrowser.SearchCallback {
        protected final T mSearchCallback;

        SearchCallbackProxy(T callback) {
            mSearchCallback = callback;
        }

        @Override
        public void onSearchResult(String query, Bundle extras,
                List<MediaBrowser.MediaItem> items) {
            mSearchCallback.onSearchResult(query, extras, items);
        }

        @Override
        public void onError(String query, Bundle extras) {
            mSearchCallback.onError(query, extras);
        }
    }
}
