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

package androidx.media;

import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.Parcel;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
class MediaBrowserServiceCompatApi23 {

    public static Object createService(Context context, ServiceCompatProxy serviceProxy) {
        return new MediaBrowserServiceAdaptor(context, serviceProxy);
    }

    public interface ServiceCompatProxy extends MediaBrowserServiceCompatApi21.ServiceCompatProxy {
        void onLoadItem(String itemId, MediaBrowserServiceCompatApi21.ResultWrapper<Parcel> result);
    }

    static class MediaBrowserServiceAdaptor extends
            MediaBrowserServiceCompatApi21.MediaBrowserServiceAdaptor {
        MediaBrowserServiceAdaptor(Context context, ServiceCompatProxy serviceWrapper) {
            super(context, serviceWrapper);
        }

        @Override
        public void onLoadItem(String itemId, Result<MediaBrowser.MediaItem> result) {
            ((ServiceCompatProxy) mServiceProxy).onLoadItem(itemId,
                    new MediaBrowserServiceCompatApi21.ResultWrapper<Parcel>(result));
        }
    }

    private MediaBrowserServiceCompatApi23() {
    }
}
