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

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class MediaBrowserCompatApi21 {
    static final String NULL_MEDIA_ITEM_ID =
            "android.support.v4.media.MediaBrowserCompat.NULL_MEDIA_ITEM";

    public static Object createConnectionCallback(ConnectionCallback callback) {
        return new ConnectionCallbackProxy<>(callback);
    }

    public static Object createBrowser(Context context, ComponentName serviceComponent,
            Object callback, Bundle rootHints) {
        return new MediaBrowser(context, serviceComponent,
                (MediaBrowser.ConnectionCallback) callback, rootHints);
    }

    public static void connect(Object browserObj) {
        ((MediaBrowser)browserObj).connect();
    }

    public static void disconnect(Object browserObj) {
        ((MediaBrowser)browserObj).disconnect();

    }

    public static boolean isConnected(Object browserObj) {
        return ((MediaBrowser)browserObj).isConnected();
    }

    public static ComponentName getServiceComponent(Object browserObj) {
        return ((MediaBrowser)browserObj).getServiceComponent();
    }

    public static String getRoot(Object browserObj) {
        return ((MediaBrowser)browserObj).getRoot();
    }

    public static Bundle getExtras(Object browserObj) {
        return ((MediaBrowser)browserObj).getExtras();
    }

    public static Object getSessionToken(Object browserObj) {
        return ((MediaBrowser)browserObj).getSessionToken();
    }

    public static Object createSubscriptionCallback(SubscriptionCallback callback) {
        return new SubscriptionCallbackProxy<>(callback);
    }

    public static void subscribe(
            Object browserObj, String parentId, Object subscriptionCallbackObj) {
        ((MediaBrowser)browserObj).subscribe(parentId,
                (MediaBrowser.SubscriptionCallback) subscriptionCallbackObj);
    }

    public static void unsubscribe(Object browserObj, String parentId) {
        ((MediaBrowser)browserObj).unsubscribe(parentId);
    }

    interface ConnectionCallback {
        void onConnected();
        void onConnectionSuspended();
        void onConnectionFailed();
    }

    static class ConnectionCallbackProxy<T extends ConnectionCallback>
            extends MediaBrowser.ConnectionCallback {
        protected final T mConnectionCallback;

        public ConnectionCallbackProxy(T connectionCallback) {
            mConnectionCallback = connectionCallback;
        }

        @Override
        public void onConnected() {
            mConnectionCallback.onConnected();
        }

        @Override
        public void onConnectionSuspended() {
            mConnectionCallback.onConnectionSuspended();
        }

        @Override
        public void onConnectionFailed() {
            mConnectionCallback.onConnectionFailed();
        }
    }

    interface SubscriptionCallback {
        void onChildrenLoaded(@NonNull String parentId, List<Parcel> children);
        void onError(@NonNull String parentId);
    }

    static class SubscriptionCallbackProxy<T extends SubscriptionCallback>
            extends MediaBrowser.SubscriptionCallback {
        protected final T mSubscriptionCallback;

        public SubscriptionCallbackProxy(T callback) {
            mSubscriptionCallback = callback;
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                List<MediaBrowser.MediaItem> children) {
            mSubscriptionCallback.onChildrenLoaded(parentId, itemListToParcelList(children));
        }

        @Override
        public void onError(@NonNull String parentId) {
            mSubscriptionCallback.onError(parentId);
        }

        static List<Parcel> itemListToParcelList(List<MediaBrowser.MediaItem> itemList) {
            if (itemList == null || (itemList.size() == 1
                    && itemList.get(0).getMediaId().equals(NULL_MEDIA_ITEM_ID))) {
                return null;
            }
            List<Parcel> parcelList = new ArrayList<>();
            for (MediaBrowser.MediaItem item : itemList) {
                Parcel parcel = Parcel.obtain();
                item.writeToParcel(parcel, 0);
                parcelList.add(parcel);
            }
            return parcelList;
        }
    }
}
