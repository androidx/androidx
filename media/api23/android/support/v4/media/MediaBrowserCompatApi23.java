/*
 * Copyright 2018 The Android Open Source Project
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
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(23)
class MediaBrowserCompatApi23 {

    public static Object createItemCallback(ItemCallback callback) {
        return new ItemCallbackProxy<>(callback);
    }

    public static void getItem(Object browserObj, String mediaId, Object itemCallbackObj) {
        ((MediaBrowser) browserObj).getItem(mediaId, ((MediaBrowser.ItemCallback) itemCallbackObj));
    }

    interface ItemCallback {
        void onItemLoaded(Parcel itemParcel);
        void onError(@NonNull String itemId);
    }

    static class ItemCallbackProxy<T extends ItemCallback> extends MediaBrowser.ItemCallback {
        protected final T mItemCallback;

        public ItemCallbackProxy(T callback) {
            mItemCallback = callback;
        }

        @Override
        public void onItemLoaded(MediaBrowser.MediaItem item) {
            if (item == null) {
                mItemCallback.onItemLoaded(null);
            } else {
                Parcel parcel = Parcel.obtain();
                item.writeToParcel(parcel, 0);
                mItemCallback.onItemLoaded(parcel);
            }
        }

        @Override
        public void onError(@NonNull String itemId) {
            mItemCallback.onError(itemId);
        }
    }

    private MediaBrowserCompatApi23() {
    }
}
