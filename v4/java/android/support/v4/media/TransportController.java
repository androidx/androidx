/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.view.KeyEventCompat;
import android.view.KeyEvent;
import android.view.View;

/**
 * Helper for implementing a media transport control (with play, pause, skip, and
 * other media actions).  Takes care of both key events and advanced features
 * like {@link android.media.RemoteControlClient}.
 */
public class TransportController {
    final Context mContext;
    final Callbacks mCallbacks;
    final AudioManager mAudioManager;
    final View mView;
    final Object mDispatcherState;
    final TransportControllerJellybeanMR2 mController;
    final TransportControllerJellybeanMR2.TransportCallback mTransportKeyCallback
            = new TransportControllerJellybeanMR2.TransportCallback() {
        @Override
        public void handleKey(KeyEvent key) {
            key.dispatch(mKeyEventCallback);
        }
        @Override
        public void handleAudioFocusChange(int focusChange) {
            mCallbacks.onAudioFocusChange(TransportController.this, focusChange);
        }
    };

    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_PLAY KeyEvent.KEYCODE_MEDIA_PLAY} */
    public static final int KEYCODE_MEDIA_PLAY = 126;
    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_PAUSE KeyEvent.KEYCODE_MEDIA_PAUSE} */
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_RECORD KeyEvent.KEYCODE_MEDIA_RECORD} */
    public static final int KEYCODE_MEDIA_RECORD = 130;

    static boolean isMediaKey(int keyCode) {
        switch (keyCode) {
            case KEYCODE_MEDIA_PLAY:
            case KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
                return true;
            }
        }
        return false;
    }

    final KeyEvent.Callback mKeyEventCallback = new KeyEvent.Callback() {
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return isMediaKey(keyCode) ? mCallbacks.onMediaButtonDown(keyCode, event) : false;
        }

        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return isMediaKey(keyCode) ? mCallbacks.onMediaButtonUp(keyCode, event) : false;
        }

        @Override
        public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
            return false;
        }
    };

    /**
     * Class through which you receive information about media transport actions.
     * These may either come from key events dispatched directly to your UI, or
     * events sent over a media button event receiver that this class keeps active
     * while your window is in focus.
     */
    public static class Callbacks {
        /**
         * Report that a media button has been pressed.  This is like
         * {@link KeyEvent.Callback#onKeyDown(int, android.view.KeyEvent)} but
         * will only deliver media keys.
         * @param keyCode The code of the media key.
         * @param event The full key event.
         * @return Indicate whether the key has been consumed.  The default
         * implementation always returns true.  This only matters for keys
         * being dispatched here from
         * {@link TransportController#dispatchKeyEvent(android.view.KeyEvent)
         * TransportController.dispatchKeyEvent}, and determines whether the key
         * continues on to its default key handling (which for media keys means
         * being delivered to the current media remote control, which should
         * be us).
         */
        public boolean onMediaButtonDown(int keyCode, KeyEvent event) {
            return true;
        }

        /**
         * Report that a media button has been pressed.  This is like
         * {@link KeyEvent.Callback#onKeyUp(int, android.view.KeyEvent)} but
         * will only deliver media keys.
         * @param keyCode The code of the media key.
         * @param event The full key event.
         * @return Indicate whether the key has been consumed.  The default
         * implementation always returns true.  This only matters for keys
         * being dispatched here from
         * {@link TransportController#dispatchKeyEvent(android.view.KeyEvent)
         * TransportController.dispatchKeyEvent}, and determines whether the key
         * continues on to its default key handling (which for media keys means
         * being delivered to the current media remote control, which should
         * be us).
         */
        public boolean onMediaButtonUp(int keyCode, KeyEvent event) {
            return true;
        }

        /**
         * Report that audio focus has changed on the app.  This only happens if
         * you have indicated you have started playing with
         * {@link TransportController#startPlaying TransportController.startPlaying},
         * which takes audio focus for you.
         * @param focusChange The type of focus change, as per
         * {@link android.media.AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
         * OnAudioFocusChangeListener.onAudioFocusChange}.  The default implementation will
         * deliver a {@link KeyEvent#KEYCODE_MEDIA_STOP}
         * when receiving {@link android.media.AudioManager#AUDIOFOCUS_LOSS},
         * {@link KeyEvent#KEYCODE_MEDIA_PAUSE}
         * when receiving {@link android.media.AudioManager#AUDIOFOCUS_LOSS_TRANSIENT}
         * or {@link android.media.AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK}, and
         * {@link KeyEvent#KEYCODE_MEDIA_PLAY}
         * when receiving {@link android.media.AudioManager#AUDIOFOCUS_GAIN}.
         */
        public void onAudioFocusChange(TransportController transport, int focusChange) {
            transport.handleAudioFocusChange(focusChange);
        }
    }

    public TransportController(Activity activity, Callbacks callbacks) {
        this(activity, null, callbacks);
    }

    public TransportController(View view, Callbacks callbacks) {
        this(null, view, callbacks);
    }

    private TransportController(Activity activity, View view, Callbacks callbacks) {
        mContext = activity != null ? activity : view.getContext();
        mCallbacks = callbacks;
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mView = activity != null ? activity.getWindow().getDecorView() : view;
        mDispatcherState = KeyEventCompat.getKeyDispatcherState(mView);
        if (Build.VERSION.SDK_INT >= 18 || Build.VERSION.CODENAME.equals("JellyBeanMR2")) {
            mController = new TransportControllerJellybeanMR2(mContext, mAudioManager,
                    mView, mTransportKeyCallback);
        } else {
            mController = null;
        }
    }

    /**
     * Return the {@link android.media.RemoteControlClient} associated with this transport.
     * This returns a generic Object since the RemoteControlClient is not availble before
     * {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH}.  Further, this class
     * will not use RemoteControlClient in its implementation until
     * {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2}.  You should always check for
     * null here and not do anything with the RemoteControlClient if none is given; this
     * way you don't need to worry about the current platform API version.
     */
    public Object getRemoteControlClient() {
        return mController != null ? mController.getRemoteControlClient() : null;
    }

    /**
     * Must call from {@link Activity#dispatchKeyEvent Activity.dispatchKeyEvent} to give
     * the transport an opportunity to intercept media keys.  Any such keys will show up
     * in {@link Callbacks}.
     * @param event
     * @return
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        return KeyEventCompat.dispatch(event, mKeyEventCallback, mDispatcherState, this);
    }

    /**
     * Move the controller into the playing state.  This updates the remote control
     * client to indicate it is playing, and takes audio focus for the app.
     */
    public void startPlaying() {
        if (mController != null) {
            mController.startPlaying();
        }
    }

    /**
     * Move the controller into the paused state.  This updates the remote control
     * client to indicate it is paused, but keeps audio focus.
     */
    public void pausePlaying() {
        if (mController != null) {
            mController.pausePlaying();
        }
    }

    /**
     * Move the controller into the stopped state.  This updates the remote control
     * client to indicate it is stopped, and removes audio focus from the app.
     */
    public void stopPlaying() {
        if (mController != null) {
            mController.stopPlaying();
        }
    }

    /**
     * Optionally call when no longer using the TransportController.  Its resources
     * will also be automatically cleaned up when your activity/view is detached from
     * its window, so you don't normally need to call this explicitly.
     */
    public void destroy() {
        mController.destroy();
    }

    void handleAudioFocusChange(int focusChange) {
        if (mController != null) {
            mController.handleAudioFocusChange(focusChange);
        }
    }
}
