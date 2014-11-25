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
import android.media.Rating;
import android.media.RemoteControlClient;
import android.os.Bundle;

public class MediaSessionCompatApi19 {
    /***** MediaMetadata keys ********/
    private static final String METADATA_KEY_ART = "android.media.metadata.ART";
    private static final String METADATA_KEY_ALBUM_ART = "android.media.metadata.ALBUM_ART";
    private static final String METADATA_KEY_USER_RATING = "android.media.metadata.USER_RATING";
    private static final String METADATA_KEY_RATING = "android.media.metadata.RATING";

    public static Object createMetadataUpdateListener(MediaSessionCompatApi14.Callback callback) {
        return new OnMetadataUpdateListener<MediaSessionCompatApi14.Callback>(callback);
    }

    public static void setMetadata(Object rccObj, Bundle metadata, boolean supportRating) {
        RemoteControlClient.MetadataEditor editor = ((RemoteControlClient) rccObj).editMetadata(
                true);
        MediaSessionCompatApi14.buildOldMetadata(metadata, editor);
        addNewMetadata(metadata, editor);
        if (supportRating && android.os.Build.VERSION.SDK_INT > 19) {
            editor.addEditableKey(RemoteControlClient.MetadataEditor.RATING_KEY_BY_USER);
        }
        editor.apply();
    }

    public static void setOnMetadataUpdateListener(Object rccObj, Object onMetadataUpdateObj) {
        ((RemoteControlClient) rccObj).setMetadataUpdateListener(
                (RemoteControlClient.OnMetadataUpdateListener) onMetadataUpdateObj);
    }

    static void addNewMetadata(Bundle metadata, RemoteControlClient.MetadataEditor editor) {
        if (metadata.containsKey(METADATA_KEY_RATING)) {
            editor.putObject(MediaMetadataEditor.RATING_KEY_BY_OTHERS,
                    metadata.getParcelable(METADATA_KEY_RATING));
        }
        if (metadata.containsKey(METADATA_KEY_USER_RATING)) {
            editor.putObject(MediaMetadataEditor.RATING_KEY_BY_USER,
                    metadata.getParcelable(METADATA_KEY_USER_RATING));
        }
        if (metadata.containsKey(METADATA_KEY_ART)) {
            Bitmap art = metadata.getParcelable(METADATA_KEY_ART);
            editor.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, art);
        } else if (metadata.containsKey(METADATA_KEY_ALBUM_ART)) {
            // Fall back to album art if the track art wasn't available
            Bitmap art = metadata.getParcelable(METADATA_KEY_ALBUM_ART);
            editor.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, art);
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