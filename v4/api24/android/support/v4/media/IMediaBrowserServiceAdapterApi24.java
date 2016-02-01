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
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * A class for replacing the auto generated hidden class, IMediaBrowserService.Stub
 */
class IMediaBrowserServiceAdapterApi24 extends IMediaBrowserServiceAdapterApi23 {
    // Following TRANSACTION_XXX values are synchronized with the auto generated java file
    // from IMediaBrowserService.aidl
    private static final int TRANSACTION_addSubscriptionWithOptions =
            IBinder.FIRST_CALL_TRANSACTION + 5;
    private static final int TRANSACTION_removeSubscriptionWithOptions =
            IBinder.FIRST_CALL_TRANSACTION + 6;

    final MediaBrowserServiceCompatApi24.ServiceImplApi24 mServiceImpl;

    public IMediaBrowserServiceAdapterApi24(
            MediaBrowserServiceCompatApi24.ServiceImplApi24 serviceImpl) {
        super(serviceImpl);
        mServiceImpl = serviceImpl;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        switch (code) {
            case TRANSACTION_addSubscriptionWithOptions: {
                data.enforceInterface(DESCRIPTOR);
                String arg0 = data.readString();
                Bundle arg1 = (data.readInt() == 0)
                        ? null : Bundle.CREATOR.createFromParcel(data);
                Object arg2 = IMediaBrowserServiceCallbacksAdapterApi24.Stub.asInterface(
                        data.readStrongBinder());
                addSubscription(arg0, arg1, arg2);
                return true;
            }
            case TRANSACTION_removeSubscriptionWithOptions: {
                data.enforceInterface(DESCRIPTOR);
                String arg0 = data.readString();
                Bundle arg1 = (data.readInt() == 0)
                        ? null : Bundle.CREATOR.createFromParcel(data);
                Object arg2 = IMediaBrowserServiceCallbacksAdapterApi24.Stub.asInterface(
                        data.readStrongBinder());
                removeSubscription(arg0, arg1, arg2);
                return true;
            }
        }
        return super.onTransact(code, data, reply, flags);
    }

    @Override
    void connect(String pkg, Bundle rootHints, Object callbacks) {
        mServiceImpl.connect(pkg, rootHints,
                new MediaBrowserServiceCompatApi24.ServiceCallbacksImplApi24(callbacks));
    }

    void addSubscription(final String id, final Bundle options, final Object callbacks) {
        mServiceImpl.addSubscription(id, options,
                new MediaBrowserServiceCompatApi24.ServiceCallbacksImplApi24(callbacks));
    }

    void removeSubscription(final String id, final Bundle options, final Object callbacks) {
        mServiceImpl.removeSubscription(id, options,
                new MediaBrowserServiceCompatApi24.ServiceCallbacksImplApi24(callbacks));
    }
}


