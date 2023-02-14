/*
 * Copyright (C) 2022 The Android Open Source Project
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
 *
 * This file is auto-generated.  DO NOT MODIFY.
 */
package androidx.core.uwb.backend;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

 /**
  * @hide
 */
@SuppressLint({"MutableBareField", "ParcelNotFinal"})
public interface IUwb extends android.os.IInterface
{
    /**
     * The version of this interface that the caller is built against.
     * This might be different from what {@link #getInterfaceVersion()
     * getInterfaceVersion} returns as that is the version of the interface
     * that the remote object is implementing.
     */
    public static final int VERSION = 1;
    public static final String HASH = "notfrozen";
    /** Default implementation for IUwb. */
    public static class Default implements androidx.core.uwb.backend.IUwb
    {
        @NonNull
        @Override
        public androidx.core.uwb.backend.IUwbClient getControleeClient() throws android.os.RemoteException
        {
            return null;
        }

        @NonNull
        @Override
        public androidx.core.uwb.backend.IUwbClient getControllerClient() throws android.os.RemoteException
        {
            return null;
        }
        @Override
        public int getInterfaceVersion() {
            return 0;
        }

        @NonNull
        @Override
        public String getInterfaceHash() {
            return "";
        }
        @Override
        @SuppressLint("MissingNullability")
        public android.os.IBinder asBinder() {
            return null;
        }
    }
    /** Local-side IPC implementation stub class. */
    @SuppressLint("RawAidl")
    public static abstract class Stub extends android.os.Binder implements androidx.core.uwb.backend.IUwb
    {
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an androidx.core.uwb.backend.IUwb interface,
         * generating a proxy if needed.
         */
        @Nullable
        public static androidx.core.uwb.backend.IUwb asInterface(@Nullable IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof androidx.core.uwb.backend.IUwb))) {
                return ((androidx.core.uwb.backend.IUwb)iin);
            }
            return new androidx.core.uwb.backend.IUwb.Stub.Proxy(obj);
        }
        @SuppressLint("MissingNullability")
        @Override public android.os.IBinder asBinder()
        {
            return this;
        }
        @Override public boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply,
                int flags) throws android.os.RemoteException
        {
            java.lang.String descriptor = DESCRIPTOR;
            if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(descriptor);
            }
            switch (code)
            {
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_getInterfaceVersion:
                {
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                }
                case TRANSACTION_getInterfaceHash:
                {
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                }
            }
            switch (code)
            {
                case TRANSACTION_getControleeClient:
                {
                    androidx.core.uwb.backend.IUwbClient _result = this.getControleeClient();
                    reply.writeNoException();
                    reply.writeStrongInterface(_result);
                    break;
                }
                case TRANSACTION_getControllerClient:
                {
                    androidx.core.uwb.backend.IUwbClient _result = this.getControllerClient();
                    reply.writeNoException();
                    reply.writeStrongInterface(_result);
                    break;
                }
                default:
                {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }
        private static class Proxy implements androidx.core.uwb.backend.IUwb
        {
            private android.os.IBinder mRemote;
            Proxy(android.os.IBinder remote)
            {
                mRemote = remote;
            }
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";
            @Override public android.os.IBinder asBinder()
            {
                return mRemote;
            }
            @SuppressLint("UnusedMethod")
            public java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }
            @NonNull
            @Override public androidx.core.uwb.backend.IUwbClient getControleeClient() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                androidx.core.uwb.backend.IUwbClient _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getControleeClient, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method getControleeClient is unimplemented.");
                    }
                    _reply.readException();
                    _result = androidx.core.uwb.backend.IUwbClient.Stub.asInterface(_reply.readStrongBinder());
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @NonNull
            @Override public androidx.core.uwb.backend.IUwbClient getControllerClient() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                androidx.core.uwb.backend.IUwbClient _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getControllerClient, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method getControllerClient is unimplemented.");
                    }
                    _reply.readException();
                    _result = androidx.core.uwb.backend.IUwbClient.Stub.asInterface(_reply.readStrongBinder());
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override
            @SuppressLint("UnusedVariable")
            public int getInterfaceVersion() throws android.os.RemoteException {
                if (mCachedVersion == -1) {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
                        reply.readException();
                        mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return mCachedVersion;
            }
            @NonNull
            @Override
            @SuppressLint({"BanSynchronizedMethods", "UnusedVariable"})
            public synchronized String getInterfaceHash() throws android.os.RemoteException {
                if ("-1".equals(mCachedHash)) {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                        reply.readException();
                        mCachedHash = reply.readString();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return mCachedHash;
            }
        }
        static final int TRANSACTION_getControleeClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_getControllerClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
        static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    }
    public static final java.lang.String DESCRIPTOR =
            "androidx.core.uwb.backend.IUwb";

    @NonNull
    public androidx.core.uwb.backend.IUwbClient getControleeClient() throws android.os.RemoteException;

    @NonNull
    public androidx.core.uwb.backend.IUwbClient getControllerClient() throws android.os.RemoteException;
    public int getInterfaceVersion() throws android.os.RemoteException;

    @NonNull
    public String getInterfaceHash() throws android.os.RemoteException;
}
