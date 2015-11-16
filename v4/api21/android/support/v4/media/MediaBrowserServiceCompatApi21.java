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

import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService;
import android.service.media.IMediaBrowserServiceCallbacks;
import android.service.media.MediaBrowserService;

import java.util.ArrayList;
import java.util.List;

class MediaBrowserServiceCompatApi21 {

    public static Object createService() {
        return new MediaBrowserServiceAdaptorApi21();
    }

    public static void onCreate(Object serviceObj, ServiceImplApi21 serviceImpl) {
        ((MediaBrowserServiceAdaptorApi21) serviceObj).onCreate(serviceImpl);
    }

    public static IBinder onBind(Object serviceObj, Intent intent) {
        return ((MediaBrowserServiceAdaptorApi21) serviceObj).onBind(intent);
    }

    public interface ServiceImplApi21 {
        void connect(final String pkg, final Bundle rootHints, final ServiceCallbacks callbacks);
        void disconnect(final ServiceCallbacks callbacks);
        void addSubscription(final String id, final ServiceCallbacks callbacks);
        void removeSubscription(final String id, final ServiceCallbacks callbacks);
    }

    public interface ServiceCallbacks {
        IBinder asBinder();
        void onConnect(String root, Object session, Bundle extras) throws RemoteException;
        void onConnectFailed() throws RemoteException;
        void onLoadChildren(String mediaId, List<Parcel> list) throws RemoteException;
    }

    public static class ServiceCallbacksApi21 implements ServiceCallbacks {
        private static final ParceledListSlice sNullParceledListSlice;
        static {
            MediaDescription nullDescription = new MediaDescription.Builder().setMediaId(
                    MediaBrowserCompatApi21.NULL_MEDIA_ITEM_ID).build();
            MediaBrowser.MediaItem nullMediaItem = new MediaBrowser.MediaItem(nullDescription, 0);
            List<MediaBrowser.MediaItem> nullMediaItemList = new ArrayList<>();
            nullMediaItemList.add(nullMediaItem);
            sNullParceledListSlice = new ParceledListSlice(nullMediaItemList);
        }

        private final IMediaBrowserServiceCallbacks mCallbacks;

        ServiceCallbacksApi21(IMediaBrowserServiceCallbacks callbacks) {
            mCallbacks = callbacks;
        }

        public IBinder asBinder() {
            return mCallbacks.asBinder();
        }

        public void onConnect(String root, Object session, Bundle extras) throws RemoteException {
            mCallbacks.onConnect(root, (MediaSession.Token) session, extras);
        }

        public void onConnectFailed() throws RemoteException {
            mCallbacks.onConnectFailed();
        }

        public void onLoadChildren(String mediaId, List<Parcel> list) throws RemoteException {
            List<MediaBrowser.MediaItem> itemList = null;
            if (list != null) {
                itemList = new ArrayList<>();
                for (Parcel parcel : list) {
                    parcel.setDataPosition(0);
                    itemList.add(MediaBrowser.MediaItem.CREATOR.createFromParcel(parcel));
                    parcel.recycle();
                }
            }
            ParceledListSlice<MediaBrowser.MediaItem> pls;
            if (Build.VERSION.SDK_INT > 23) {
                pls = itemList == null ? null : new ParceledListSlice(itemList);
            } else {
                pls = itemList == null ? sNullParceledListSlice : new ParceledListSlice(itemList);
            }
            mCallbacks.onLoadChildren(mediaId, pls);
        }
    }

    static class MediaBrowserServiceAdaptorApi21 {
        ServiceBinderProxyApi21 mBinder;

        public void onCreate(ServiceImplApi21 serviceImpl) {
            mBinder = new ServiceBinderProxyApi21(serviceImpl);
        }

        public IBinder onBind(Intent intent) {
            if (MediaBrowserService.SERVICE_INTERFACE.equals(intent.getAction())) {
                return mBinder;
            }
            return null;
        }

        static class ServiceBinderProxyApi21 extends IMediaBrowserService.Stub {
            final ServiceImplApi21 mServiceImpl;

            ServiceBinderProxyApi21(ServiceImplApi21 serviceImpl) {
                mServiceImpl = serviceImpl;
            }

            @Override
            public void connect(final String pkg, final Bundle rootHints,
                    final IMediaBrowserServiceCallbacks callbacks) {
                mServiceImpl.connect(pkg, rootHints, new ServiceCallbacksApi21(callbacks));
            }

            @Override
            public void disconnect(final IMediaBrowserServiceCallbacks callbacks) {
                mServiceImpl.disconnect(new ServiceCallbacksApi21(callbacks));
            }


            @Override
            public void addSubscription(final String id,
                    final IMediaBrowserServiceCallbacks callbacks) {
                mServiceImpl.addSubscription(id, new ServiceCallbacksApi21(callbacks));
            }

            @Override
            public void removeSubscription(final String id,
                    final IMediaBrowserServiceCallbacks callbacks) {
                mServiceImpl.removeSubscription(id, new ServiceCallbacksApi21(callbacks));
            }

            @Override
            public void getMediaItem(final String mediaId, final ResultReceiver receiver) {
                // No operation since this method is added in API 23.
            }
        }
    }
}
