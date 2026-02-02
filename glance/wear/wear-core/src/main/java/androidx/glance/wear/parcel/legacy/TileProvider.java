/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.wear.parcel.legacy;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Interface to be implemented by a service which provides Tiles to a Wear device. */
public interface TileProvider extends IInterface {
    /** Local-side IPC implementation stub class. */
    abstract class Stub extends Binder implements TileProvider {
        /** Construct the stub at attach it to the interface. */
        @SuppressWarnings("this-escape")
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, @NonNull Parcel data, @NonNull Parcel reply, int flags)
                throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= IBinder.FIRST_CALL_TRANSACTION && code <= IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(descriptor);
            }
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(descriptor);
                return true;
            }
            switch (code) {
                case TRANSACTION_getApiVersion: {
                    int _result = this.getApiVersion();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    break;
                }
                case TRANSACTION_onTileAddEvent: {
                    TileAddEventData _arg0;
                    _arg0 = ParcelHelper.readTypedObject(data, TileAddEventData.CREATOR);
                    this.onTileAddEvent(_arg0);
                    break;
                }
                case TRANSACTION_onTileRemoveEvent: {
                    TileRemoveEventData _arg0;
                    _arg0 = ParcelHelper.readTypedObject(data, TileRemoveEventData.CREATOR);
                    this.onTileRemoveEvent(_arg0);
                    break;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }

        static final int TRANSACTION_getApiVersion = (IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_onTileAddEvent = (IBinder.FIRST_CALL_TRANSACTION + 5);
        static final int TRANSACTION_onTileRemoveEvent = (IBinder.FIRST_CALL_TRANSACTION + 6);
    }

    String DESCRIPTOR = "androidx.wear.tiles.TileProvider";

    int API_VERSION = 4;

    /**
     * Gets the version of this TileProvider interface implemented by this service. Note that this
     * does not imply any schema version; just which calls within this interface are supported.
     *
     * @since version 1
     */
    int getApiVersion() throws RemoteException;

    /**
     * Called when the Tile is added to the carousel. This will be followed by a call to
     * onTileRequest when the system is ready to render the tile.
     *
     * @since version 1
     */
    void onTileAddEvent(@Nullable TileAddEventData requestData) throws RemoteException;

    /**
     * Called when the Tile is removed from the carousel.
     *
     * @since version 1
     */
    void onTileRemoveEvent(@Nullable TileRemoveEventData requestData) throws RemoteException;

    /** hide */
    class ParcelHelper {
        private ParcelHelper() {}

        private static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> c) {
            if (parcel.readInt() != 0) {
                return c.createFromParcel(parcel);
            } else {
                return null;
            }
        }
    }
}
