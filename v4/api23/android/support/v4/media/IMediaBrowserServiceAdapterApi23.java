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

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.MediaBrowserService;
import android.util.Log;

/**
 * A class for replacing the auto generated hidden class, IMediaBrowserService.Stub
 */
class IMediaBrowserServiceAdapterApi23 extends IMediaBrowserServiceAdapterApi21 {
    private static final String TAG = "IMediaBrowserServiceAdapterApi23";

    // Following TRANSACTION_XXX values are synchronized with the auto generated java file
    // from IMediaBrowserService.aidl
    private static final int TRANSACTION_getMediaItem = IBinder.FIRST_CALL_TRANSACTION + 4;

    final MediaBrowserServiceCompatApi23.ServiceImplApi23 mServiceImpl;

    public IMediaBrowserServiceAdapterApi23(
            MediaBrowserServiceCompatApi23.ServiceImplApi23 serviceImpl) {
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

    void getMediaItem(final String mediaId, final ResultReceiver receiver) {
        final String KEY_MEDIA_ITEM;
        try {
            KEY_MEDIA_ITEM = (String) MediaBrowserService.class.getDeclaredField(
                    "KEY_MEDIA_ITEM").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Log.i(TAG, "Failed to get KEY_MEDIA_ITEM via reflection", e);
            return;
        }

        mServiceImpl.getMediaItem(mediaId, new MediaBrowserServiceCompatApi23.ItemCallback() {
            @Override
            public void onItemLoaded(int resultCode, Bundle resultData, Parcel itemParcel) {
                if (itemParcel != null) {
                    itemParcel.setDataPosition(0);
                    MediaBrowser.MediaItem item =
                            MediaBrowser.MediaItem.CREATOR.createFromParcel(itemParcel);
                    resultData.putParcelable(KEY_MEDIA_ITEM, item);
                    itemParcel.recycle();
                }
                receiver.send(resultCode, resultData);
            }
        });

    }
}

