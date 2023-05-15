/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.tools.testing

import androidx.room.compiler.processing.util.Source

private val syntheticUiLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/ui/core/SandboxedUiAdapter.kt", """
        |package androidx.privacysandbox.ui.core
        |
        |import android.os.IBinder
        |
        |interface SdkActivityLauncher {
        |    suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/ui/client/SdkActivityLaunchers.kt", """
        |@file:JvmName("SdkActivityLaunchers")
        |
        |package androidx.privacysandbox.ui.client
        |
        |import android.os.Bundle
        |import androidx.privacysandbox.ui.core.SdkActivityLauncher
        |
        |fun SdkActivityLauncher.toLauncherInfo(): Bundle {
        |    TODO("Stub!")
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/ui/provider/SdkActivityLauncherFactory.kt", """
        |package androidx.privacysandbox.ui.provider
        |
        |import android.os.Bundle
        |import androidx.privacysandbox.ui.core.SdkActivityLauncher
        |
        |object SdkActivityLauncherFactory {
        |
        |    @JvmStatic
        |    @Suppress("UNUSED_PARAMETER")
        |    fun fromLauncherInfo(launcherInfo: Bundle): SdkActivityLauncher {
        |        TODO("Stub!")
        |    }
        |}""".trimMargin()
    ),
    Source.kotlin(
        "androidx/core/os/BundleCompat.kt", """
        |package androidx.core.os
        |
        |import android.os.IBinder
        |import android.os.Bundle
        |
        |object BundleCompat {
        |    @Suppress("UNUSED_PARAMETER")
        |    fun getBinder(bundle: Bundle, key: String?): IBinder? {
        |        TODO("Stub!")
        |    }
        |}
        |""".trimMargin()
    ),
)

private val syntheticAidlGeneratedCode = listOf(
    Source.java(
        "androidx/privacysandbox/ui/core/ISdkActivityLauncher", """
        |package androidx.privacysandbox.ui.core;
        |/** @hide */
        |public interface ISdkActivityLauncher extends android.os.IInterface
        |{
        |  /** Default implementation for ISdkActivityLauncher. */
        |  public static class Default implements androidx.privacysandbox.ui.core.ISdkActivityLauncher
        |  {
        |    @Override public void launchSdkActivity(android.os.IBinder sdkActivityHandlerToken, androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback callback) throws android.os.RemoteException
        |    {
        |    }
        |    @Override
        |    public android.os.IBinder asBinder() {
        |      return null;
        |    }
        |  }
        |  /** Local-side IPC implementation stub class. */
        |  public static abstract class Stub extends android.os.Binder implements androidx.privacysandbox.ui.core.ISdkActivityLauncher
        |  {
        |    /** Construct the stub at attach it to the interface. */
        |    public Stub()
        |    {
        |      this.attachInterface(this, DESCRIPTOR);
        |    }
        |    /**
        |     * Cast an IBinder object into an androidx.privacysandbox.ui.core.ISdkActivityLauncher interface,
        |     * generating a proxy if needed.
        |     */
        |    public static androidx.privacysandbox.ui.core.ISdkActivityLauncher asInterface(android.os.IBinder obj)
        |    {
        |      if ((obj==null)) {
        |        return null;
        |      }
        |      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
        |      if (((iin!=null)&&(iin instanceof androidx.privacysandbox.ui.core.ISdkActivityLauncher))) {
        |        return ((androidx.privacysandbox.ui.core.ISdkActivityLauncher)iin);
        |      }
        |      return new androidx.privacysandbox.ui.core.ISdkActivityLauncher.Stub.Proxy(obj);
        |    }
        |    @Override public android.os.IBinder asBinder()
        |    {
        |      return this;
        |    }
        |    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        |    {
        |      java.lang.String descriptor = DESCRIPTOR;
        |      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        |        data.enforceInterface(descriptor);
        |      }
        |      switch (code)
        |      {
        |        case INTERFACE_TRANSACTION:
        |        {
        |          reply.writeString(descriptor);
        |          return true;
        |        }
        |      }
        |      switch (code)
        |      {
        |        case TRANSACTION_launchSdkActivity:
        |        {
        |          android.os.IBinder _arg0;
        |          _arg0 = data.readStrongBinder();
        |          androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback _arg1;
        |          _arg1 = androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback.Stub.asInterface(data.readStrongBinder());
        |          this.launchSdkActivity(_arg0, _arg1);
        |          break;
        |        }
        |        default:
        |        {
        |          return super.onTransact(code, data, reply, flags);
        |        }
        |      }
        |      return true;
        |    }
        |    private static class Proxy implements androidx.privacysandbox.ui.core.ISdkActivityLauncher
        |    {
        |      private android.os.IBinder mRemote;
        |      Proxy(android.os.IBinder remote)
        |      {
        |        mRemote = remote;
        |      }
        |      @Override public android.os.IBinder asBinder()
        |      {
        |        return mRemote;
        |      }
        |      public java.lang.String getInterfaceDescriptor()
        |      {
        |        return DESCRIPTOR;
        |      }
        |      @Override public void launchSdkActivity(android.os.IBinder sdkActivityHandlerToken, androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback callback) throws android.os.RemoteException
        |      {
        |        android.os.Parcel _data = android.os.Parcel.obtain();
        |        try {
        |          _data.writeInterfaceToken(DESCRIPTOR);
        |          _data.writeStrongBinder(sdkActivityHandlerToken);
        |          _data.writeStrongInterface(callback);
        |          boolean _status = mRemote.transact(Stub.TRANSACTION_launchSdkActivity, _data, null, android.os.IBinder.FLAG_ONEWAY);
        |        }
        |        finally {
        |          _data.recycle();
        |        }
        |      }
        |    }
        |    static final int TRANSACTION_launchSdkActivity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        |  }
        |  public static final java.lang.String DESCRIPTOR = "androidx.privacysandbox.ui.core.ISdkActivityLauncher";
        |  public void launchSdkActivity(android.os.IBinder sdkActivityHandlerToken, androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback callback) throws android.os.RemoteException;
        |}""".trimMargin()
    ),
    Source.java(
        "androidx/privacysandbox/ui/core/ISdkActivityLauncherCallback", """
        |package androidx.privacysandbox.ui.core;
        |/** @hide */
        |public interface ISdkActivityLauncherCallback extends android.os.IInterface
        |{
        |  /** Default implementation for ISdkActivityLauncherCallback. */
        |  public static class Default implements androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback
        |  {
        |    @Override public void onLaunchAccepted(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException
        |    {
        |    }
        |    @Override public void onLaunchRejected(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException
        |    {
        |    }
        |    @Override public void onLaunchError(java.lang.String message) throws android.os.RemoteException
        |    {
        |    }
        |    @Override
        |    public android.os.IBinder asBinder() {
        |      return null;
        |    }
        |  }
        |  /** Local-side IPC implementation stub class. */
        |  public static abstract class Stub extends android.os.Binder implements androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback
        |  {
        |    /** Construct the stub at attach it to the interface. */
        |    public Stub()
        |    {
        |      this.attachInterface(this, DESCRIPTOR);
        |    }
        |    /**
        |     * Cast an IBinder object into an androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback interface,
        |     * generating a proxy if needed.
        |     */
        |    public static androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback asInterface(android.os.IBinder obj)
        |    {
        |      if ((obj==null)) {
        |        return null;
        |      }
        |      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
        |      if (((iin!=null)&&(iin instanceof androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback))) {
        |        return ((androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback)iin);
        |      }
        |      return new androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback.Stub.Proxy(obj);
        |    }
        |    @Override public android.os.IBinder asBinder()
        |    {
        |      return this;
        |    }
        |    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        |    {
        |      java.lang.String descriptor = DESCRIPTOR;
        |      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        |        data.enforceInterface(descriptor);
        |      }
        |      switch (code)
        |      {
        |        case INTERFACE_TRANSACTION:
        |        {
        |          reply.writeString(descriptor);
        |          return true;
        |        }
        |      }
        |      switch (code)
        |      {
        |        case TRANSACTION_onLaunchAccepted:
        |        {
        |          android.os.IBinder _arg0;
        |          _arg0 = data.readStrongBinder();
        |          this.onLaunchAccepted(_arg0);
        |          break;
        |        }
        |        case TRANSACTION_onLaunchRejected:
        |        {
        |          android.os.IBinder _arg0;
        |          _arg0 = data.readStrongBinder();
        |          this.onLaunchRejected(_arg0);
        |          break;
        |        }
        |        case TRANSACTION_onLaunchError:
        |        {
        |          java.lang.String _arg0;
        |          _arg0 = data.readString();
        |          this.onLaunchError(_arg0);
        |          break;
        |        }
        |        default:
        |        {
        |          return super.onTransact(code, data, reply, flags);
        |        }
        |      }
        |      return true;
        |    }
        |    private static class Proxy implements androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback
        |    {
        |      private android.os.IBinder mRemote;
        |      Proxy(android.os.IBinder remote)
        |      {
        |        mRemote = remote;
        |      }
        |      @Override public android.os.IBinder asBinder()
        |      {
        |        return mRemote;
        |      }
        |      public java.lang.String getInterfaceDescriptor()
        |      {
        |        return DESCRIPTOR;
        |      }
        |      @Override public void onLaunchAccepted(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException
        |      {
        |        android.os.Parcel _data = android.os.Parcel.obtain();
        |        try {
        |          _data.writeInterfaceToken(DESCRIPTOR);
        |          _data.writeStrongBinder(sdkActivityHandlerToken);
        |          boolean _status = mRemote.transact(Stub.TRANSACTION_onLaunchAccepted, _data, null, android.os.IBinder.FLAG_ONEWAY);
        |        }
        |        finally {
        |          _data.recycle();
        |        }
        |      }
        |      @Override public void onLaunchRejected(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException
        |      {
        |        android.os.Parcel _data = android.os.Parcel.obtain();
        |        try {
        |          _data.writeInterfaceToken(DESCRIPTOR);
        |          _data.writeStrongBinder(sdkActivityHandlerToken);
        |          boolean _status = mRemote.transact(Stub.TRANSACTION_onLaunchRejected, _data, null, android.os.IBinder.FLAG_ONEWAY);
        |        }
        |        finally {
        |          _data.recycle();
        |        }
        |      }
        |      @Override public void onLaunchError(java.lang.String message) throws android.os.RemoteException
        |      {
        |        android.os.Parcel _data = android.os.Parcel.obtain();
        |        try {
        |          _data.writeInterfaceToken(DESCRIPTOR);
        |          _data.writeString(message);
        |          boolean _status = mRemote.transact(Stub.TRANSACTION_onLaunchError, _data, null, android.os.IBinder.FLAG_ONEWAY);
        |        }
        |        finally {
        |          _data.recycle();
        |        }
        |      }
        |    }
        |    static final int TRANSACTION_onLaunchAccepted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        |    static final int TRANSACTION_onLaunchRejected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        |    static final int TRANSACTION_onLaunchError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        |  }
        |  public static final java.lang.String DESCRIPTOR = "androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback";
        |  public void onLaunchAccepted(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException;
        |  public void onLaunchRejected(android.os.IBinder sdkActivityHandlerToken) throws android.os.RemoteException;
        |  public void onLaunchError(java.lang.String message) throws android.os.RemoteException;
        |}""".trimMargin()
    ),
)

val libraryStubs = syntheticUiLibraryStubs + syntheticAidlGeneratedCode