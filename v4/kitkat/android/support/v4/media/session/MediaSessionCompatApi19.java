/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v4.media.session;

import android.graphics.Bitmap;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.os.Bundle;

public class MediaSessionCompatApi19 {
    /***** PlaybackState actions *****/
    private static final long ACTION_SET_RATING = 1 << 7;

    /***** MediaMetadata keys ********/
    private static final String METADATA_KEY_USER_RATING = "android.media.metadata.USER_RATING";
    private static final String METADATA_KEY_RATING = "android.media.metadata.RATING";
    private static final String METADATA_KEY_YEAR = "android.media.metadata.YEAR";

    public static void setTransportControlFlags(Object rccObj, long actions) {
        ((RemoteControlClient) rccObj).setTransportControlFlags(
                getRccTransportControlFlagsFromActions(actions));
    }

    public static Object createMetadataUpdateListener(MediaSessionCompatApi14.Callback callback) {
        return new OnMetadataUpdateListener<MediaSessionCompatApi14.Callback>(callback);
    }

    public static void setMetadata(Object rccObj, Bundle metadata, long actions) {
        RemoteControlClient.MetadataEditor editor = ((RemoteControlClient) rccObj).editMetadata(
                true);
        MediaSessionCompatApi14.buildOldMetadata(metadata, editor);
        addNewMetadata(metadata, editor);
        if ((actions & ACTION_SET_RATING) != 0) {
            editor.addEditableKey(RemoteControlClient.MetadataEditor.RATING_KEY_BY_USER);
        }
        editor.apply();
    }

    public static void setOnMetadataUpdateListener(Object rccObj, Object onMetadataUpdateObj) {
        ((RemoteControlClient) rccObj).setMetadataUpdateListener(
                (RemoteControlClient.OnMetadataUpdateListener) onMetadataUpdateObj);
    }

    static int getRccTransportControlFlagsFromActions(long actions) {
        int transportControlFlags =
                MediaSessionCompatApi18.getRccTransportControlFlagsFromActions(actions);
        if ((actions & ACTION_SET_RATING) != 0) {
            transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_RATING;
        }
        return transportControlFlags;
    }

    static void addNewMetadata(Bundle metadata, RemoteControlClient.MetadataEditor editor) {
        if (metadata == null) {
            return;
        }
        if (metadata.containsKey(METADATA_KEY_YEAR)) {
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_YEAR,
                    metadata.getLong(METADATA_KEY_YEAR));
        }
        if (metadata.containsKey(METADATA_KEY_RATING)) {
            editor.putObject(MediaMetadataEditor.RATING_KEY_BY_OTHERS,
                    metadata.getParcelable(METADATA_KEY_RATING));
        }
        if (metadata.containsKey(METADATA_KEY_USER_RATING)) {
            editor.putObject(MediaMetadataEditor.RATING_KEY_BY_USER,
                    metadata.getParcelable(METADATA_KEY_USER_RATING));
        }
    }

    static class OnMetadataUpdateListener<T extends MediaSessionCompatApi14.Callback> implements
            RemoteControlClient.OnMetadataUpdateListener {
        protected final T mCallback;

        public OnMetadataUpdateListener(T callback) {
            mCallback = callback;
        }

        @Override
        public void onMetadataUpdate(int key, Object newValue) {
            if (key == MediaMetadataEditor.RATING_KEY_BY_USER && newValue instanceof Rating) {
                mCallback.onSetRating(newValue);
            }
        }
    }
}
