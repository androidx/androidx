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
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.util.Log;
import android.os.SystemClock;

class MediaSessionCompatApi18 {
    private static final String TAG = "MediaSessionCompatApi18";

    /***** PlaybackState actions *****/
    private static final long ACTION_SEEK_TO = 1 << 8;

    private static boolean sIsMbrPendingIntentSupported = true;

    public static Object createPlaybackPositionUpdateListener(Callback callback) {
        return new OnPlaybackPositionUpdateListener<Callback>(callback);
    }

    public static void registerMediaButtonEventReceiver(Context context, PendingIntent pi,
            ComponentName cn) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Some Android implementations are not able to register a media button event receiver
        // using a PendingIntent but need a ComponentName instead. These will raise a
        // NullPointerException.
        if (sIsMbrPendingIntentSupported) {
            try {
                am.registerMediaButtonEventReceiver(pi);
            } catch (NullPointerException e) {
                Log.w(TAG, "Unable to register media button event receiver with "
                        + "PendingIntent, falling back to ComponentName.");
                sIsMbrPendingIntentSupported = false;
            }
        }

        if (!sIsMbrPendingIntentSupported) {
          am.registerMediaButtonEventReceiver(cn);
        }
    }

    public static void unregisterMediaButtonEventReceiver(Context context, PendingIntent pi,
            ComponentName cn) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (sIsMbrPendingIntentSupported) {
            am.unregisterMediaButtonEventReceiver(pi);
        } else {
            am.unregisterMediaButtonEventReceiver(cn);
        }
    }

    public static void setState(Object rccObj, int state, long position, float speed,
            long updateTime) {
        long currTime = SystemClock.elapsedRealtime();
        if (state == MediaSessionCompatApi14.STATE_PLAYING && position > 0) {
            long diff = 0;
            if (updateTime > 0) {
                diff = currTime - updateTime;
                if (speed > 0 && speed != 1f) {
                    diff *= speed;
                }
            }
            position += diff;
        }
        state = MediaSessionCompatApi14.getRccStateFromState(state);
        ((RemoteControlClient) rccObj).setPlaybackState(state, position, speed);
    }

    public static void setTransportControlFlags(Object rccObj, long actions) {
        ((RemoteControlClient) rccObj).setTransportControlFlags(
                getRccTransportControlFlagsFromActions(actions));
    }

    public static void setOnPlaybackPositionUpdateListener(Object rccObj,
            Object onPositionUpdateObj) {
        ((RemoteControlClient) rccObj).setPlaybackPositionUpdateListener(
                (RemoteControlClient.OnPlaybackPositionUpdateListener) onPositionUpdateObj);
    }

    static int getRccTransportControlFlagsFromActions(long actions) {
        int transportControlFlags =
                MediaSessionCompatApi14.getRccTransportControlFlagsFromActions(actions);
        if ((actions & ACTION_SEEK_TO) != 0) {
            transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;
        }
        return transportControlFlags;
    }

    static class OnPlaybackPositionUpdateListener<T extends Callback>
            implements RemoteControlClient.OnPlaybackPositionUpdateListener {
        protected final T mCallback;

        public OnPlaybackPositionUpdateListener(T callback) {
            mCallback = callback;
        }

        @Override
        public void onPlaybackPositionUpdate(long newPositionMs) {
            mCallback.onSeekTo(newPositionMs);
        }
    }

    interface Callback {
        public void onSeekTo(long pos);
    }
}
