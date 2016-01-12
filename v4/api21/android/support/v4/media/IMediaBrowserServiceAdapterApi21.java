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
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

/**
 * An adapter class for replacing the auto generated hidden class, IMediaBrowserService.Stub
 */
class IMediaBrowserServiceAdapterApi21 {
    static abstract class Stub extends Binder implements IInterface {
        private static final String DESCRIPTOR = "android.service.media.IMediaBrowserService";
        // Following TRANSACTION_XXX values are synchronized with the auto generated java file
        // from IMediaBrowserService.aidl
        private static final int TRANSACTION_connect = IBinder.FIRST_CALL_TRANSACTION + 0;
        private static final int TRANSACTION_disconnect = IBinder.FIRST_CALL_TRANSACTION + 1;
        private static final int TRANSACTION_addSubscription = IBinder.FIRST_CALL_TRANSACTION + 2;
        private static final int TRANSACTION_removeSubscription =
                IBinder.FIRST_CALL_TRANSACTION + 3;
        private static final int TRANSACTION_getMediaItem = IBinder.FIRST_CALL_TRANSACTION + 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_connect: {
                    data.enforceInterface(DESCRIPTOR);
                    String arg0 = data.readString();
                    Bundle arg1;
                    if (data.readInt() != 0) {
                        arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        arg1 = null;
                    }
                    Object arg2 = IMediaBrowserServiceCallbacksAdapterApi21.Stub.asInterface(
                            data.readStrongBinder());
                    connect(arg0, arg1, arg2);
                    return true;
                }
                case TRANSACTION_disconnect: {
                    data.enforceInterface(DESCRIPTOR);
                    Object arg0 = IMediaBrowserServiceCallbacksAdapterApi21.Stub.asInterface(
                            data.readStrongBinder());
                    disconnect(arg0);
                    return true;
                }
                case TRANSACTION_addSubscription: {
                    data.enforceInterface(DESCRIPTOR);
                    String arg0 = data.readString();
                    Object arg1 = IMediaBrowserServiceCallbacksAdapterApi21.Stub.asInterface(
                            data.readStrongBinder());
                    addSubscription(arg0, arg1);
                    return true;
                }
                case TRANSACTION_removeSubscription: {
                    data.enforceInterface(DESCRIPTOR);
                    String arg0 = data.readString();
                    Object arg1 = IMediaBrowserServiceCallbacksAdapterApi21.Stub.asInterface(
                            data.readStrongBinder());
                    removeSubscription(arg0, arg1);
                    return true;
                }
                case TRANSACTION_getMediaItem: {
                    data.enforceInterface(DESCRIPTOR);
                    String arg0 = data.readString();
                    ResultReceiver arg1;
                    if (data.readInt() != 0) {
                        arg1 = android.os.ResultReceiver.CREATOR.createFromParcel(data);
                    } else {
                        arg1 = null;
                    }
                    getMediaItem(arg0, arg1);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        public abstract void connect(final String pkg, final Bundle rootHints,
                final Object callbacks);
        public abstract void disconnect(final Object callbacks);
        public abstract void addSubscription(final String id, final Object callbacks);
        public abstract void removeSubscription(final String id, final Object callbacks);
        public abstract void getMediaItem(final String mediaId, final ResultReceiver receiver);
    }
}
