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

import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.List;

class MediaBrowserServiceCompatApi24 extends MediaBrowserServiceCompatApi23 {
    public static Object createService() {
        return new MediaBrowserServiceAdaptorApi24();
    }

    public static void onCreate(Object serviceObj, ServiceImplApi24 serviceImpl) {
        ((MediaBrowserServiceAdaptorApi24) serviceObj).onCreate(serviceImpl);
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
            mCallbacks.onLoadChildrenWithOptions(mediaId,
                    parcelListToParceledListSliceObject(list), options);
        }
    }

    static class MediaBrowserServiceAdaptorApi24 extends MediaBrowserServiceAdaptorApi23 {
        public void onCreate(ServiceImplApi24 serviceImpl) {
            mBinder = new ServiceBinderProxyApi24(serviceImpl);
        }

        static class ServiceBinderProxyApi24 extends ServiceBinderProxyApi23 {
            ServiceImplApi24 mServiceImpl;

            ServiceBinderProxyApi24(ServiceImplApi24 serviceImpl) {
                super(serviceImpl);
                mServiceImpl = serviceImpl;
            }

            @Override
            public void connect(String pkg, Bundle rootHints, Object callbacks) {
                mServiceImpl.connect(pkg, rootHints, new ServiceCallbacksImplApi24(callbacks));
            }

            @Override
            public void addSubscription(String id, Bundle options, Object callbacks) {
                mServiceImpl.addSubscription(id, options, new ServiceCallbacksImplApi24(callbacks));
            }

            @Override
            public void removeSubscription(String id, Bundle options, Object callbacks) {
                mServiceImpl.removeSubscription(id, options,
                        new ServiceCallbacksImplApi24(callbacks));
            }
        }
    }
}
