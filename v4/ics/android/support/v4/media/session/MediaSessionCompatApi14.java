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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Bundle;
import android.os.ResultReceiver;

public class MediaSessionCompatApi14 {
    /***** RemoteControlClient States, we only need none as the others were public *******/
    final static int RCC_PLAYSTATE_NONE = 0;

    /***** MediaSession States *******/
    final static int STATE_NONE = 0;
    final static int STATE_STOPPED = 1;
    final static int STATE_PAUSED = 2;
    final static int STATE_PLAYING = 3;
    final static int STATE_FAST_FORWARDING = 4;
    final static int STATE_REWINDING = 5;
    final static int STATE_BUFFERING = 6;
    final static int STATE_ERROR = 7;
    final static int STATE_CONNECTING = 8;
    final static int STATE_SKIPPING_TO_PREVIOUS = 9;
    final static int STATE_SKIPPING_TO_NEXT = 10;

    /***** MediaMetadata keys ********/
    private static final String METADATA_KEY_TITLE = "android.media.metadata.TITLE";
    private static final String METADATA_KEY_ARTIST = "android.media.metadata.ARTIST";
    private static final String METADATA_KEY_DURATION = "android.media.metadata.DURATION";
    private static final String METADATA_KEY_ALBUM = "android.media.metadata.ALBUM";
    private static final String METADATA_KEY_AUTHOR = "android.media.metadata.AUTHOR";
    private static final String METADATA_KEY_WRITER = "android.media.metadata.WRITER";
    private static final String METADATA_KEY_COMPOSER = "android.media.metadata.COMPOSER";
    private static final String METADATA_KEY_COMPILATION = "android.media.metadata.COMPILATION";
    private static final String METADATA_KEY_DATE = "android.media.metadata.DATE";
    private static final String METADATA_KEY_YEAR = "android.media.metadata.YEAR";
    private static final String METADATA_KEY_GENRE = "android.media.metadata.GENRE";
    private static final String METADATA_KEY_TRACK_NUMBER = "android.media.metadata.TRACK_NUMBER";
    private static final String METADATA_KEY_NUM_TRACKS = "android.media.metadata.NUM_TRACKS";
    private static final String METADATA_KEY_DISC_NUMBER = "android.media.metadata.DISC_NUMBER";
    private static final String METADATA_KEY_ALBUM_ARTIST = "android.media.metadata.ALBUM_ARTIST";

    public static Object createRemoteControlClient(PendingIntent mbIntent) {
        return new RemoteControlClient(mbIntent);
    }

    public static void setState(Object rccObj, int state) {
        ((RemoteControlClient) rccObj).setPlaybackState(getRccStateFromState(state));
    }

    public static void setMetadata(Object rccObj, Bundle metadata) {
        RemoteControlClient.MetadataEditor editor = ((RemoteControlClient) rccObj).editMetadata(
                true);
        buildOldMetadata(metadata, editor);
        editor.apply();
    }

    public static void registerRemoteControlClient(Context context, Object rccObj) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.registerRemoteControlClient((RemoteControlClient) rccObj);
    }

    public static void unregisterRemoteControlClient(Context context, Object rccObj) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.unregisterRemoteControlClient((RemoteControlClient) rccObj);
    }

    static int getRccStateFromState(int state) {
        switch (state) {
            case STATE_CONNECTING:
            case STATE_BUFFERING:
                return RemoteControlClient.PLAYSTATE_BUFFERING;
            case STATE_ERROR:
                return RemoteControlClient.PLAYSTATE_ERROR;
            case STATE_FAST_FORWARDING:
                return RemoteControlClient.PLAYSTATE_FAST_FORWARDING;
            case STATE_NONE:
                return RCC_PLAYSTATE_NONE;
            case STATE_PAUSED:
                return RemoteControlClient.PLAYSTATE_PAUSED;
            case STATE_PLAYING:
                return RemoteControlClient.PLAYSTATE_PLAYING;
            case STATE_REWINDING:
                return RemoteControlClient.PLAYSTATE_REWINDING;
            case STATE_SKIPPING_TO_PREVIOUS:
                return RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS;
            case STATE_SKIPPING_TO_NEXT:
                return RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS;
            case STATE_STOPPED:
                return RemoteControlClient.PLAYSTATE_STOPPED;
            default:
                return -1;
        }
    }

    static void buildOldMetadata(Bundle metadata, RemoteControlClient.MetadataEditor editor) {
        if (metadata.containsKey(METADATA_KEY_ALBUM)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                    metadata.getString(METADATA_KEY_ALBUM));
        }
        if (metadata.containsKey(METADATA_KEY_ALBUM_ARTIST)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                    metadata.getString(METADATA_KEY_ALBUM_ARTIST));
        }
        if (metadata.containsKey(METADATA_KEY_ARTIST)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                    metadata.getString(METADATA_KEY_ARTIST));
        }
        if (metadata.containsKey(METADATA_KEY_AUTHOR)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_AUTHOR,
                    metadata.getString(METADATA_KEY_AUTHOR));
        }
        if (metadata.containsKey(METADATA_KEY_COMPILATION)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_COMPILATION,
                    metadata.getString(METADATA_KEY_COMPILATION));
        }
        if (metadata.containsKey(METADATA_KEY_COMPOSER)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_COMPOSER,
                    metadata.getString(METADATA_KEY_COMPOSER));
        }
        if (metadata.containsKey(METADATA_KEY_DATE)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_DATE,
                    metadata.getString(METADATA_KEY_DATE));
        }
        if (metadata.containsKey(METADATA_KEY_DISC_NUMBER)) {
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER,
                    metadata.getLong(METADATA_KEY_DISC_NUMBER));
        }
        if (metadata.containsKey(METADATA_KEY_DURATION)) {
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                    metadata.getLong(METADATA_KEY_DURATION));
        }
        if (metadata.containsKey(METADATA_KEY_GENRE)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_GENRE,
                    metadata.getString(METADATA_KEY_GENRE));
        }
        if (metadata.containsKey(METADATA_KEY_NUM_TRACKS)) {
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS,
                    metadata.getLong(METADATA_KEY_NUM_TRACKS));
        }
        if (metadata.containsKey(METADATA_KEY_TITLE)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                    metadata.getString(METADATA_KEY_TITLE));
        }
        if (metadata.containsKey(METADATA_KEY_TRACK_NUMBER)) {
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                    metadata.getLong(METADATA_KEY_TRACK_NUMBER));
        }
        if (metadata.containsKey(METADATA_KEY_WRITER)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_WRITER,
                    metadata.getString(METADATA_KEY_WRITER));
        }
        if (metadata.containsKey(METADATA_KEY_YEAR)) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_YEAR,
                    metadata.getString(METADATA_KEY_YEAR));
        }
    }

    public static interface Callback {
        public void onCommand(String command, Bundle extras, ResultReceiver cb);

        public boolean onMediaButtonEvent(Intent mediaButtonIntent);

        public void onPlay();

        public void onPause();

        public void onSkipToNext();

        public void onSkipToPrevious();

        public void onFastForward();

        public void onRewind();

        public void onStop();

        public void onSeekTo(long pos);

        public void onSetRating(Object ratingObj);
    }
}