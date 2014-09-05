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

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.KeyEvent;

class MediaControllerCompatApi21 {
    public static Object fromToken(Context context, Object sessionToken) {
        return new MediaController(context, (MediaSession.Token) sessionToken);
    }

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static void registerCallback(Object controllerObj, Object callbackObj, Handler handler) {
        ((MediaController) controllerObj).registerCallback(
                (MediaController.Callback)callbackObj, handler);
    }

    public static void unregisterCallback(Object controllerObj, Object callbackObj) {
        ((MediaController) controllerObj)
                .unregisterCallback((MediaController.Callback) callbackObj);
    }

    public static Object getTransportControls(Object controllerObj) {
        return ((MediaController)controllerObj).getTransportControls();
    }

    public static Object getPlaybackState(Object controllerObj) {
        return ((MediaController)controllerObj).getPlaybackState();
    }

    public static Object getMetadata(Object controllerObj) {
        return ((MediaController)controllerObj).getMetadata();
    }

    public static int getRatingType(Object controllerObj) {
        return ((MediaController)controllerObj).getRatingType();
    }

    public static Object getPlaybackInfo(Object controllerObj) {
        return ((MediaController)controllerObj).getPlaybackInfo();
    }

    public static boolean dispatchMediaButtonEvent(Object controllerObj, KeyEvent event) {
        return ((MediaController)controllerObj).dispatchMediaButtonEvent(event);
    }

    public static void sendCommand(Object controllerObj,
            String command, Bundle params, ResultReceiver cb) {
        ((MediaController)controllerObj).sendCommand(command, params, cb);
    }

    public static class TransportControls {
        public static void play(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).play();
        }

        public static void pause(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).pause();
        }

        public static void stop(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).stop();
        }

        public static void seekTo(Object controlsObj, long pos) {
            ((MediaController.TransportControls)controlsObj).seekTo(pos);
        }

        public static void fastForward(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).fastForward();
        }

        public static void rewind(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).rewind();
        }

        public static void skipToNext(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).skipToNext();
        }

        public static void skipToPrevious(Object controlsObj) {
            ((MediaController.TransportControls)controlsObj).skipToPrevious();
        }

        public static void setRating(Object controlsObj, Object ratingObj) {
            ((MediaController.TransportControls)controlsObj).setRating((Rating)ratingObj);
        }
    }

    public static class PlaybackInfo {
        public static int getPlaybackType(Object volumeInfoObj) {
            return ((MediaController.PlaybackInfo)volumeInfoObj).getPlaybackType();
        }

        public static AudioAttributes getAudioAttributes(Object volumeInfoObj) {
            return ((MediaController.PlaybackInfo) volumeInfoObj).getAudioAttributes();
        }

        public static int getLegacyAudioStream(Object volumeInfoObj) {
            AudioAttributes attrs = getAudioAttributes(volumeInfoObj);
            return toLegacyStreamType(attrs);
        }

        public static int getVolumeControl(Object volumeInfoObj) {
            return ((MediaController.PlaybackInfo)volumeInfoObj).getVolumeControl();
        }

        public static int getMaxVolume(Object volumeInfoObj) {
            return ((MediaController.PlaybackInfo)volumeInfoObj).getMaxVolume();
        }

        public static int getCurrentVolume(Object volumeInfoObj) {
            return ((MediaController.PlaybackInfo)volumeInfoObj).getCurrentVolume();
        }

        // This is copied from AudioAttributes.toLegacyStreamType. TODO This
        // either needs to be kept in sync with that one or toLegacyStreamType
        // needs to be made public so it can be used by the support lib.
        private static final int FLAG_SCO = 0x1 << 2;
        private static final int STREAM_BLUETOOTH_SCO = 6;
        private static final int STREAM_SYSTEM_ENFORCED = 7;
        private static int toLegacyStreamType(AudioAttributes aa) {
            // flags to stream type mapping
            if ((aa.getFlags() & AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    == AudioAttributes.FLAG_AUDIBILITY_ENFORCED) {
                return STREAM_SYSTEM_ENFORCED;
            }
            if ((aa.getFlags() & FLAG_SCO) == FLAG_SCO) {
                return STREAM_BLUETOOTH_SCO;
            }

            // usage to stream type mapping
            switch (aa.getUsage()) {
                case AudioAttributes.USAGE_MEDIA:
                case AudioAttributes.USAGE_GAME:
                case AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY:
                case AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                    return AudioManager.STREAM_MUSIC;
                case AudioAttributes.USAGE_ASSISTANCE_SONIFICATION:
                    return AudioManager.STREAM_SYSTEM;
                case AudioAttributes.USAGE_VOICE_COMMUNICATION:
                    return AudioManager.STREAM_VOICE_CALL;
                case AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING:
                    return AudioManager.STREAM_DTMF;
                case AudioAttributes.USAGE_ALARM:
                    return AudioManager.STREAM_ALARM;
                case AudioAttributes.USAGE_NOTIFICATION_RINGTONE:
                    return AudioManager.STREAM_RING;
                case AudioAttributes.USAGE_NOTIFICATION:
                case AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                case AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                case AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                case AudioAttributes.USAGE_NOTIFICATION_EVENT:
                    return AudioManager.STREAM_NOTIFICATION;
                case AudioAttributes.USAGE_UNKNOWN:
                default:
                    return AudioManager.STREAM_MUSIC;
            }
        }
    }

    public static interface Callback {
        public void onSessionDestroyed();
        public void onSessionEvent(String event, Bundle extras);
        public void onPlaybackStateChanged(Object stateObj);
        public void onMetadataChanged(Object metadataObj);
    }

    static class CallbackProxy<T extends Callback> extends MediaController.Callback {
        protected final T mCallback;

        public CallbackProxy(T callback) {
            mCallback = callback;
        }

        @Override
        public void onSessionDestroyed() {
            mCallback.onSessionDestroyed();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            mCallback.onSessionEvent(event, extras);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mCallback.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mCallback.onMetadataChanged(metadata);
        }
    }
}
