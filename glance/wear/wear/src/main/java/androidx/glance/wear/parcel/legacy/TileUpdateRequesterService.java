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

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface, implemented by Tile Renderers, which allows a Tile Provider to request that a Tile
 * Renderer fetches a new Timeline from it.
 */
@RestrictTo(Scope.LIBRARY)
public interface TileUpdateRequesterService extends IInterface {
    /** Default implementation for TileUpdateRequesterService. */
    class Default implements TileUpdateRequesterService {
        /**
         * Gets the version of this TileProvider interface implemented by this service. Note that
         * this does not imply any schema version; just which calls within this interface are
         * supported.
         *
         * @since version 1
         */
        @Override
        public int getApiVersion() throws RemoteException {
            return 0;
        }

        /**
         * Request that the Tile Renderer fetches a new Timeline from the Tile Provider service
         * identified by {@code component}. The package name in {@code component} must have the same
         * UID as the calling process, otherwise this call will be ignored.
         *
         * <p>{@code updateData} is provided as a placeholder to allow a payload of parameters to be
         * passed in future. This is currently blank in all implementations, but allows for easy
         * expansion.
         *
         * <p>Note that this call may be rate limited, hence the tile fetch request may not occur
         * immediately after calling this method.
         *
         * @since version 1
         */
        @Override
        public void requestUpdate(
                @Nullable ComponentName component, @Nullable TileUpdateRequestData updateData)
                throws RemoteException {}

        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    /** Local-side IPC implementation stub class. */
    abstract class Stub extends Binder implements TileUpdateRequesterService {
        /** Construct the stub and attach it to the interface. */
        @SuppressWarnings("this-escape")
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into a TileUpdateRequesterService interface, generating a proxy if
         * needed.
         */
        public static @Nullable TileUpdateRequesterService asInterface(@Nullable IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof TileUpdateRequesterService))) {
                return ((TileUpdateRequesterService) iin);
            }
            return new TileUpdateRequesterService.Stub.Proxy(obj);
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
                case TRANSACTION_requestUpdate: {
                    ComponentName _arg0;
                    _arg0 = ParcelHelper.readTypedObject(data, ComponentName.CREATOR);
                    TileUpdateRequestData _arg1;
                    _arg1 = ParcelHelper.readTypedObject(data, TileUpdateRequestData.CREATOR);
                    this.requestUpdate(_arg0, _arg1);
                    break;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }

        private static class Proxy implements TileUpdateRequesterService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @SuppressWarnings("UnusedMethod")
            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            /**
             * Gets the version of this TileProvider interface implemented by this service. Note
             * that this does not imply any schema version; just which calls within this interface
             * are supported.
             *
             * @since version 1
             */
            @Override
            public int getApiVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean unused_status =
                            mRemote.transact(Stub.TRANSACTION_getApiVersion, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            /**
             * Request that the Tile Renderer fetches a new Timeline from the Tile Provider service
             * identified by {@code component}. The package name in {@code component} must have the
             * same UID as the calling process, otherwise this call will be ignored.
             *
             * <p>{@code updateData} is provided as a placeholder to allow a payload of parameters
             * to be passed in future. This is currently blank in all implementations, but allows
             * for easy expansion.
             *
             * <p>Note that this call may be rate limited, hence the tile fetch request may not
             * occur immediately after calling this method.
             *
             * @since version 1
             */
            @Override
            public void requestUpdate(ComponentName component, TileUpdateRequestData updateData)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    ParcelHelper.writeTypedObject(_data, component, 0);
                    ParcelHelper.writeTypedObject(_data, updateData, 0);
                    boolean unused_status =
                            mRemote.transact(
                                    Stub.TRANSACTION_requestUpdate,
                                    _data,
                                    null,
                                    IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_getApiVersion = (IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_requestUpdate = (IBinder.FIRST_CALL_TRANSACTION + 1);
    }

    /** Descriptor for the IInterface. */
    String DESCRIPTOR = "androidx.wear.tiles.TileUpdateRequesterService";

    int API_VERSION = 1;

    /**
     * Gets the version of this TileProvider interface implemented by this service. Note that this
     * does not imply any schema version; just which calls within this interface are supported.
     *
     * @since version 1
     */
    int getApiVersion() throws RemoteException;

    /**
     * Request that the Tile Renderer fetches a new Timeline from the Tile Provider service
     * identified by {@code component}. The package name in {@code component} must have the same UID
     * as the calling process, otherwise this call will be ignored.
     *
     * <p>{@code updateData} is provided as a placeholder to allow a payload of parameters to be
     * passed in future. This is currently blank in all implementations, but allows for easy
     * expansion.
     *
     * <p>Note that this call may be rate limited, hence the tile fetch request may not occur
     * immediately after calling this method.
     *
     * @since version 1
     */
    void requestUpdate(
            @Nullable ComponentName component, @Nullable TileUpdateRequestData updateData)
            throws RemoteException;

    /** Helpers for Parcel. */
    class ParcelHelper {

        private ParcelHelper() {}

        private static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> c) {
            if (parcel.readInt() != 0) {
                return c.createFromParcel(parcel);
            } else {
                return null;
            }
        }

        private static <T extends Parcelable> void writeTypedObject(
                Parcel parcel, T value, int parcelableFlags) {
            if (value != null) {
                parcel.writeInt(1);
                value.writeToParcel(parcel, parcelableFlags);
            } else {
                parcel.writeInt(0);
            }
        }
    }
}
