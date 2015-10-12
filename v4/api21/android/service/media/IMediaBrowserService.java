/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.service.media;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

/**
 * A dummy implementation for overriding a hidden framework class, IMediaBrowserService.
 * When there are duplicated signatures between app and framework code, the framework code will be
 * run.
 * TODO: Consider using aidl instead of this.
 * @hide
 */
public interface IMediaBrowserService extends IInterface {

    public static abstract class Stub extends Binder
            implements IMediaBrowserService {
        public Stub() {
        }

        public static IMediaBrowserService asInterface(IBinder obj) {
            return null;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws android.os.RemoteException {
            return false;
        }
    }

    public void connect(String pkg, Bundle rootHints, IMediaBrowserServiceCallbacks callbacks)
            throws android.os.RemoteException;
    public void disconnect(IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
    public void addSubscription(String uri, IMediaBrowserServiceCallbacks callbacks)
            throws RemoteException;
    public void removeSubscription(String uri, IMediaBrowserServiceCallbacks callbacks)
            throws RemoteException;
    public void getMediaItem(String uri, ResultReceiver cb) throws android.os.RemoteException;
}
