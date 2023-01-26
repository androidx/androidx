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

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingSessionCallback
 *
 * @hide
 */
@SuppressLint({"MutableBareField", "ParcelNotFinal", "CallbackMethodName"})
public interface IRangingSessionCallback extends android.os.IInterface
{
    /**
     * The version of this interface that the caller is built against.
     * This might be different from what {@link #getInterfaceVersion()
     * getInterfaceVersion} returns as that is the version of the interface
     * that the remote object is implementing.
     */
    public static final int VERSION = 1;
    public static final String HASH = "notfrozen";
    /** Default implementation for IRangingSessionCallback. */
    public static class Default implements androidx.core.uwb.backend.IRangingSessionCallback
    {
        @Override public void onRangingInitialized(@NonNull UwbDevice device) throws android.os.RemoteException
        {
        }
        @Override public void onRangingResult(@NonNull UwbDevice device,
                @NonNull RangingPosition position) throws android.os.RemoteException
        {
        }
        @Override public void onRangingSuspended(@NonNull UwbDevice device, int reason) throws android.os.RemoteException
        {
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
    public static abstract class Stub extends android.os.Binder implements androidx.core.uwb.backend.IRangingSessionCallback
    {
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an androidx.core.uwb.backend.IRangingSessionCallback interface,
         * generating a proxy if needed.
         */
        @Nullable
        public static androidx.core.uwb.backend.IRangingSessionCallback asInterface(
                @Nullable IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof androidx.core.uwb.backend.IRangingSessionCallback))) {
                return ((androidx.core.uwb.backend.IRangingSessionCallback)iin);
            }
            return new androidx.core.uwb.backend.IRangingSessionCallback.Stub.Proxy(obj);
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
                case TRANSACTION_onRangingInitialized:
                {
                    androidx.core.uwb.backend.UwbDevice _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.UwbDevice.CREATOR);
                    this.onRangingInitialized(_arg0);
                    break;
                }
                case TRANSACTION_onRangingResult:
                {
                    androidx.core.uwb.backend.UwbDevice _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.UwbDevice.CREATOR);
                    androidx.core.uwb.backend.RangingPosition _arg1;
                    _arg1 = data.readTypedObject(androidx.core.uwb.backend.RangingPosition.CREATOR);
                    this.onRangingResult(_arg0, _arg1);
                    break;
                }
                case TRANSACTION_onRangingSuspended:
                {
                    androidx.core.uwb.backend.UwbDevice _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.UwbDevice.CREATOR);
                    int _arg1;
                    _arg1 = data.readInt();
                    this.onRangingSuspended(_arg0, _arg1);
                    break;
                }
                default:
                {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }
        private static class Proxy implements androidx.core.uwb.backend.IRangingSessionCallback
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
            @Override public void onRangingInitialized(@NonNull androidx.core.uwb.backend.UwbDevice device) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onRangingInitialized, _data, null, android.os.IBinder.FLAG_ONEWAY);
                    if (!_status) {
                        throw new android.os.RemoteException("Method onRangingInitialized is unimplemented.");
                    }
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void onRangingResult(@NonNull androidx.core.uwb.backend.UwbDevice device, @NonNull androidx.core.uwb.backend.RangingPosition position) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeTypedObject(position, 0);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onRangingResult, _data, null, android.os.IBinder.FLAG_ONEWAY);
                    if (!_status) {
                        throw new android.os.RemoteException("Method onRangingResult is unimplemented.");
                    }
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void onRangingSuspended(@NonNull androidx.core.uwb.backend.UwbDevice device, int reason) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeInt(reason);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onRangingSuspended, _data, null, android.os.IBinder.FLAG_ONEWAY);
                    if (!_status) {
                        throw new android.os.RemoteException("Method onRangingSuspended is unimplemented.");
                    }
                }
                finally {
                    _data.recycle();
                }
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
            @SuppressLint({"BanSynchronizedMethods", "UnusedVariable"})
            @Override
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
        static final int TRANSACTION_onRangingInitialized = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_onRangingResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_onRangingSuspended = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
        static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    }
    public static final java.lang.String DESCRIPTOR = "androidx"
            + ".core.uwb.backend.IRangingSessionCallback";
    /** Reasons for suspend */
    public static final int UNKNOWN = 0;
    public static final int WRONG_PARAMETERS = 1;
    public static final int FAILED_TO_START = 2;
    public static final int STOPPED_BY_PEER = 3;
    public static final int STOP_RANGING_CALLED = 4;
    @SuppressLint("MinMaxConstant")
    public static final int MAX_RANGING_ROUND_RETRY_REACHED = 5;
    public void onRangingInitialized(@NonNull UwbDevice device) throws android.os.RemoteException;
    public void onRangingResult(@NonNull UwbDevice device, @NonNull RangingPosition position) throws android.os.RemoteException;
    public void onRangingSuspended(@NonNull UwbDevice device, int reason) throws android.os.RemoteException;
    @SuppressLint("CallbackMethodName")
    public int getInterfaceVersion() throws android.os.RemoteException;

    @NonNull
    @SuppressLint("CallbackMethodName")
    public String getInterfaceHash() throws android.os.RemoteException;
}
