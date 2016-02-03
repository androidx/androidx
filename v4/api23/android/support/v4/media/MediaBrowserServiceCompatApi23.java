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

package android.support.v4.media;

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.service.media.MediaBrowserService;
import android.util.Log;

class MediaBrowserServiceCompatApi23 extends MediaBrowserServiceCompatApi21 {
    private static final String TAG = "MediaBrowserServiceCompatApi21";

    public static Object createService() {
        return new MediaBrowserServiceAdaptorApi23();
    }

    public static void onCreate(Object serviceObj, ServiceImplApi23 serviceImpl) {
        ((MediaBrowserServiceAdaptorApi23) serviceObj).onCreate(serviceImpl);
    }

    public interface ServiceImplApi23 extends ServiceImplApi21 {
        void getMediaItem(final String mediaId, final ItemCallback cb);
    }

    public interface ItemCallback {
        void onItemLoaded(int resultCode, Bundle resultData, Parcel itemParcel);
    }

    static class MediaBrowserServiceAdaptorApi23 extends MediaBrowserServiceAdaptorApi21 {

        public void onCreate(ServiceImplApi23 serviceImpl) {
            mBinder = new ServiceBinderProxyApi23(serviceImpl);
        }

        private static class ServiceBinderProxyApi23 extends ServiceBinderProxyApi21 {
            ServiceImplApi23 mServiceImpl;

            ServiceBinderProxyApi23(ServiceImplApi23 serviceImpl) {
                super(serviceImpl);
                mServiceImpl = serviceImpl;
            }

            @Override
            public void getMediaItem(final String mediaId, final ResultReceiver receiver) {
                final String KEY_MEDIA_ITEM;
                try {
                    KEY_MEDIA_ITEM = (String) MediaBrowserService.class.getDeclaredField(
                            "KEY_MEDIA_ITEM").get(null);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    Log.i(TAG, "Failed to get KEY_MEDIA_ITEM via reflection", e);
                    return;
                }

                mServiceImpl.getMediaItem(mediaId, new ItemCallback() {
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
    }
}
