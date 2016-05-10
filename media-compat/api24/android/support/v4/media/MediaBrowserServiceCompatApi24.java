/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Parcel;
import android.service.media.MediaBrowserService;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

class MediaBrowserServiceCompatApi24 {
    private static final String TAG = "MBSCompatApi24";

    private static Field sResultFlags;
    static {
        try {
            sResultFlags = MediaBrowserService.Result.class.getDeclaredField("mFlags");
            sResultFlags.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.w(TAG, e);
        }
    }

    public static Object createService(Context context, ServiceCompatProxy serviceProxy) {
        return new MediaBrowserServiceAdaptor(context, serviceProxy);
    }

    public static void notifyChildrenChanged(Object serviceObj, String parentId, Bundle options) {
        ((MediaBrowserService) serviceObj).notifyChildrenChanged(parentId, options);
    }

    public static Bundle getBrowserRootHints(Object serviceObj) {
        return ((MediaBrowserService) serviceObj).getBrowserRootHints();
    }

    public interface ServiceCompatProxy extends MediaBrowserServiceCompatApi23.ServiceCompatProxy {
        void onLoadChildren(String parentId, ResultWrapper result, Bundle options);
    }

    static class ResultWrapper {
        MediaBrowserService.Result mResultObj;

        ResultWrapper(MediaBrowserService.Result result) {
            mResultObj = result;
        }

        public void sendResult(List<Parcel> result, int flags) {
            try {
                sResultFlags.setInt(mResultObj, flags);
            } catch (IllegalAccessException e) {
                Log.w(TAG, e);
            }
            mResultObj.sendResult(parcelListToItemList(result));
        }

        public void detach() {
            mResultObj.detach();
        }

        List<MediaBrowser.MediaItem> parcelListToItemList(List<Parcel> parcelList) {
            if (parcelList == null) {
                return null;
            }
            List<MediaBrowser.MediaItem> items = new ArrayList<>();
            for (Parcel parcel : parcelList) {
                parcel.setDataPosition(0);
                items.add(MediaBrowser.MediaItem.CREATOR.createFromParcel(parcel));
                parcel.recycle();
            }
            return items;
        }
    }

    static class MediaBrowserServiceAdaptor extends
            MediaBrowserServiceCompatApi23.MediaBrowserServiceAdaptor {
        MediaBrowserServiceAdaptor(Context context, ServiceCompatProxy serviceWrapper) {
            super(context, serviceWrapper);
        }

        @Override
        public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result,
                Bundle options) {
            ((ServiceCompatProxy) mServiceProxy).onLoadChildren(
                    parentId, new ResultWrapper(result), options);
        }
    }
}
