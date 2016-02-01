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

import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.List;

class MediaBrowserServiceCompatApi24 extends MediaBrowserServiceCompatApi23 {
    public static Object createService() {
        return new MediaBrowserServiceAdaptorApi24();
    }

    public interface ServiceImplApi24 extends ServiceImplApi23 {
        void connect(String pkg, Bundle rootHints, ServiceCallbacksApi24 callbacks);
        void addSubscription(String id, Bundle options, ServiceCallbacksApi24 callbacks);
        void removeSubscription(String id, Bundle options, ServiceCallbacksApi24 callbacks);
    }

    public interface ServiceCallbacksApi24 extends ServiceCallbacksApi21 {
        void onLoadChildren(String mediaId, List<Parcel> list, Bundle options)
                throws RemoteException;
    }

    public static class ServiceCallbacksImplApi24 extends ServiceCallbacksImplApi21
            implements ServiceCallbacksApi24 {
        ServiceCallbacksImplApi24(Object callbacksObj) {
            super(callbacksObj);
        }

        @Override
        public void onLoadChildren(String mediaId, List<Parcel> list, Bundle options)
                throws RemoteException {
            ((IMediaBrowserServiceCallbacksAdapterApi24)mCallbacks).onLoadChildrenWithOptions(
                    mediaId, parcelListToParceledListSliceObject(list), options);
        }

        @Override
        IMediaBrowserServiceCallbacksAdapterApi24 createCallbacks(Object callbacksObj) {
            return new IMediaBrowserServiceCallbacksAdapterApi24(callbacksObj);
        }
    }

    static class MediaBrowserServiceAdaptorApi24 extends MediaBrowserServiceAdaptorApi23 {
        protected Binder createServiceBinder(ServiceImplApi21 serviceImpl) {
            return new IMediaBrowserServiceAdapterApi24((ServiceImplApi24) serviceImpl);
        }
    }
}
