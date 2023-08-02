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
import androidx.annotation.RestrictTo;

/** Gms Reference: com.google.android.gms.nearby.uwb.UwbClient
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint({"MutableBareField", "ParcelNotFinal", "ExecutorRegistration"})
public interface IUwbClient extends android.os.IInterface
{
    /**
     * The version of this interface that the caller is built against.
     * This might be different from what {@link #getInterfaceVersion()
     * getInterfaceVersion} returns as that is the version of the interface
     * that the remote object is implementing.
     */
    public static final int VERSION = 1;
    public static final String HASH = "notfrozen";
    /** Default implementation for IUwbClient. */
    public static class Default implements androidx.core.uwb.backend.IUwbClient
    {
        @Override public boolean isAvailable() throws android.os.RemoteException
        {
            return false;
        }

        @Nullable
        @Override
        public androidx.core.uwb.backend.RangingCapabilities getRangingCapabilities() throws android.os.RemoteException
        {
            return null;
        }
        @Nullable
        @Override public androidx.core.uwb.backend.UwbAddress getLocalAddress() throws android.os.RemoteException
        {
            return null;
        }
        @Nullable
        @Override public androidx.core.uwb.backend.UwbComplexChannel getComplexChannel() throws android.os.RemoteException
        {
            return null;
        }
        @SuppressLint("ExecutorRegistration")
        @Override public void startRanging(@NonNull RangingParameters parameters,
                @NonNull IRangingSessionCallback callback) throws android.os.RemoteException
        {
        }
        @SuppressLint("ExecutorRegistration")
        @Override public void stopRanging(@NonNull IRangingSessionCallback callback) throws android.os.RemoteException
        {
        }
        @Override public void addControlee(@NonNull UwbAddress address) throws android.os.RemoteException
        {
        }
        @Override public void removeControlee(@NonNull UwbAddress address) throws android.os.RemoteException
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
    public static abstract class Stub extends android.os.Binder implements androidx.core.uwb.backend.IUwbClient
    {
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an androidx.core.uwb.backend.IUwbClient interface,
         * generating a proxy if needed.
         */
        @Nullable
        public static androidx.core.uwb.backend.IUwbClient asInterface(@Nullable IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof androidx.core.uwb.backend.IUwbClient))) {
                return ((androidx.core.uwb.backend.IUwbClient)iin);
            }
            return new androidx.core.uwb.backend.IUwbClient.Stub.Proxy(obj);
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
                case TRANSACTION_isAvailable:
                {
                    boolean _result = this.isAvailable();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    break;
                }
                case TRANSACTION_getRangingCapabilities:
                {
                    androidx.core.uwb.backend.RangingCapabilities _result = this.getRangingCapabilities();
                    reply.writeNoException();
                    reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    break;
                }
                case TRANSACTION_getLocalAddress:
                {
                    androidx.core.uwb.backend.UwbAddress _result = this.getLocalAddress();
                    reply.writeNoException();
                    reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    break;
                }
                case TRANSACTION_getComplexChannel:
                {
                    androidx.core.uwb.backend.UwbComplexChannel _result = this.getComplexChannel();
                    reply.writeNoException();
                    reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    break;
                }
                case TRANSACTION_startRanging:
                {
                    androidx.core.uwb.backend.RangingParameters _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.RangingParameters.CREATOR);
                    androidx.core.uwb.backend.IRangingSessionCallback _arg1;
                    _arg1 = androidx.core.uwb.backend.IRangingSessionCallback.Stub.asInterface(data.readStrongBinder());
                    this.startRanging(_arg0, _arg1);
                    reply.writeNoException();
                    break;
                }
                case TRANSACTION_stopRanging:
                {
                    androidx.core.uwb.backend.IRangingSessionCallback _arg0;
                    _arg0 = androidx.core.uwb.backend.IRangingSessionCallback.Stub.asInterface(data.readStrongBinder());
                    this.stopRanging(_arg0);
                    reply.writeNoException();
                    break;
                }
                case TRANSACTION_addControlee:
                {
                    androidx.core.uwb.backend.UwbAddress _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.UwbAddress.CREATOR);
                    this.addControlee(_arg0);
                    reply.writeNoException();
                    break;
                }
                case TRANSACTION_removeControlee:
                {
                    androidx.core.uwb.backend.UwbAddress _arg0;
                    _arg0 = data.readTypedObject(androidx.core.uwb.backend.UwbAddress.CREATOR);
                    this.removeControlee(_arg0);
                    reply.writeNoException();
                    break;
                }
                default:
                {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }
        private static class Proxy implements androidx.core.uwb.backend.IUwbClient
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
            @Override public boolean isAvailable() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_isAvailable, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method isAvailable is unimplemented.");
                    }
                    _reply.readException();
                    _result = _reply.readInt() == 1;
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @NonNull
            @Override public androidx.core.uwb.backend.RangingCapabilities getRangingCapabilities() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                androidx.core.uwb.backend.RangingCapabilities _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getRangingCapabilities, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method getRangingCapabilities is unimplemented.");
                    }
                    _reply.readException();
                    _result = _reply.readTypedObject(androidx.core.uwb.backend.RangingCapabilities.CREATOR);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public androidx.core.uwb.backend.UwbAddress getLocalAddress() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                androidx.core.uwb.backend.UwbAddress _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getLocalAddress, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method getLocalAddress is unimplemented.");
                    }
                    _reply.readException();
                    _result = _reply.readTypedObject(androidx.core.uwb.backend.UwbAddress.CREATOR);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public androidx.core.uwb.backend.UwbComplexChannel getComplexChannel() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                androidx.core.uwb.backend.UwbComplexChannel _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getComplexChannel, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method getComplexChannel is unimplemented.");
                    }
                    _reply.readException();
                    _result = _reply.readTypedObject(androidx.core.uwb.backend.UwbComplexChannel.CREATOR);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void startRanging(@NonNull androidx.core.uwb.backend.RangingParameters parameters, @NonNull androidx.core.uwb.backend.IRangingSessionCallback callback) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(parameters, 0);
                    _data.writeStrongInterface(callback);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_startRanging, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method startRanging is unimplemented.");
                    }
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void stopRanging(@NonNull androidx.core.uwb.backend.IRangingSessionCallback callback) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_stopRanging, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method stopRanging is unimplemented.");
                    }
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void addControlee(@NonNull androidx.core.uwb.backend.UwbAddress address) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(address, 0);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_addControlee, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method addControlee is unimplemented.");
                    }
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void removeControlee(@NonNull androidx.core.uwb.backend.UwbAddress address) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(address, 0);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_removeControlee, _data, _reply, 0);
                    if (!_status) {
                        throw new android.os.RemoteException("Method removeControlee is unimplemented.");
                    }
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
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
        static final int TRANSACTION_isAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_getRangingCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_getLocalAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_getComplexChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        static final int TRANSACTION_startRanging = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
        static final int TRANSACTION_stopRanging = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
        static final int TRANSACTION_addControlee = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
        static final int TRANSACTION_removeControlee = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
        static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
        static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    }
    public static final java.lang.String DESCRIPTOR =
            "androidx.core.uwb.backend.IUwbClient";
    public boolean isAvailable() throws android.os.RemoteException;

    @Nullable
    public androidx.core.uwb.backend.RangingCapabilities getRangingCapabilities() throws android.os.RemoteException;

    @Nullable
    public androidx.core.uwb.backend.UwbAddress getLocalAddress() throws android.os.RemoteException;

    @Nullable
    public androidx.core.uwb.backend.UwbComplexChannel getComplexChannel() throws android.os.RemoteException;
    @SuppressLint("ExecutorRegistration")
    public void startRanging(@NonNull RangingParameters parameters,
            @NonNull IRangingSessionCallback callback) throws android.os.RemoteException;
    @SuppressLint("ExecutorRegistration")
    public void stopRanging(@NonNull IRangingSessionCallback callback) throws android.os.RemoteException;
    public void addControlee(@NonNull UwbAddress address) throws android.os.RemoteException;
    public void removeControlee(@NonNull UwbAddress address) throws android.os.RemoteException;
    public int getInterfaceVersion() throws android.os.RemoteException;

    @NonNull
    public String getInterfaceHash() throws android.os.RemoteException;
}
