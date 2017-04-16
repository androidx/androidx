/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.service.media.MediaBrowserService;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(21)
class MediaBrowserServiceCompatApi21 {

    public static Object createService(Context context, ServiceCompatProxy serviceProxy) {
        return new MediaBrowserServiceAdaptor(context, serviceProxy);
    }

    public static void onCreate(Object serviceObj) {
        ((MediaBrowserService) serviceObj).onCreate();
    }

    public static IBinder onBind(Object serviceObj, Intent intent) {
        return ((MediaBrowserService) serviceObj).onBind(intent);
    }

    public static void setSessionToken(Object serviceObj, Object token) {
        ((MediaBrowserService) serviceObj).setSessionToken((MediaSession.Token) token);
    }

    public static void notifyChildrenChanged(Object serviceObj, String parentId) {
        ((MediaBrowserService) serviceObj).notifyChildrenChanged(parentId);
    }

    public interface ServiceCompatProxy {
        BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints);
        void onLoadChildren(String parentId, ResultWrapper<List<Parcel>> result);
    }

    static class ResultWrapper<T> {
        MediaBrowserService.Result mResultObj;

        ResultWrapper(MediaBrowserService.Result result) {
            mResultObj = result;
        }

        public void sendResult(T result) {
            if (result instanceof List) {
                mResultObj.sendResult(parcelListToItemList((List<Parcel>)result));
            } else if (result instanceof Parcel) {
                Parcel parcel = (Parcel) result;
                parcel.setDataPosition(0);
                mResultObj.sendResult(MediaBrowser.MediaItem.CREATOR.createFromParcel(parcel));
                parcel.recycle();
            } else {
                // The result is null or an invalid instance.
                mResultObj.sendResult(null);
            }
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

    static class BrowserRoot {
        final String mRootId;
        final Bundle mExtras;

        BrowserRoot(String rootId, Bundle extras) {
            mRootId = rootId;
            mExtras = extras;
        }
    }

    static class MediaBrowserServiceAdaptor extends MediaBrowserService {
        final ServiceCompatProxy mServiceProxy;

        MediaBrowserServiceAdaptor(Context context, ServiceCompatProxy serviceWrapper) {
            attachBaseContext(context);
            mServiceProxy = serviceWrapper;
        }

        @Override
        public MediaBrowserService.BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                Bundle rootHints) {
            MediaBrowserServiceCompatApi21.BrowserRoot browserRoot = mServiceProxy.onGetRoot(
                    clientPackageName, clientUid, rootHints == null ? null : new Bundle(rootHints));
            return browserRoot == null ? null : new MediaBrowserService.BrowserRoot(
                    browserRoot.mRootId, browserRoot.mExtras);
        }

        @Override
        public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
            mServiceProxy.onLoadChildren(parentId, new ResultWrapper<List<Parcel>>(result));
        }
    }
}
