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

import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.List;

@RequiresApi(26)
class MediaBrowserServiceCompatApi26 {

    public static Object createService(Context context, ServiceCompatProxy serviceProxy) {
        return new MediaBrowserServiceAdaptor(context, serviceProxy);
    }

    public interface ServiceCompatProxy extends MediaBrowserServiceCompatApi24.ServiceCompatProxy {
        void onSearch(@NonNull String query, Bundle extras,
                MediaBrowserServiceCompatApi24.ResultWrapper result);
    }

    static class MediaBrowserServiceAdaptor extends
            MediaBrowserServiceCompatApi24.MediaBrowserServiceAdaptor {
        MediaBrowserServiceAdaptor(Context context, ServiceCompatProxy serviceWrapper) {
            super(context, serviceWrapper);
        }

        @Override
        public void onSearch(@NonNull String query, Bundle extras,
                @NonNull Result<List<MediaBrowser.MediaItem>> result) {
            ((ServiceCompatProxy) mServiceProxy).onSearch(query, extras,
                    new MediaBrowserServiceCompatApi24.ResultWrapper(result));
        }
    }
}
