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

import java.util.ArrayList;

/**
 * Helper for implementing a media transport control (with play, pause, skip, and
 * other media actions).  Takes care of both key events and advanced features
 * like {@link android.media.RemoteControlClient}.  This class is intended to
 * serve as an intermediary between transport controls (whether they be on-screen
 * controls, hardware buttons, remote controls) and the actual player.  The player
 * is represented by a single {@link TransportPerformer} that must be supplied to
 * this class.  On-screen controls that want to control and show the state of the
 * player should do this through calls to the {@link TransportController} interface.
 *
 * <p>Here is a simple but fairly complete sample of a video player that is built
 * around this class.  Note that the MediaController class used here is not the one
 * included in the standard Android framework, but a custom implementation.  Real
 * applications often implement their own transport controls, or you can copy the
 * implementation here out of Support4Demos.</p>
 *
 * {@sample development/samples/Support4Demos/src/com/example/android/supportv4/media/TransportControllerActivity.java
 *      complete}
 */
public class TransportMediator extends TransportController {
    final Context mContext;
    final TransportPerformer mCallbacks;
    final AudioManager mAudioManager;
    final View mView;
    final Object mDispatcherState;
    final TransportMediatorJellybeanMR2 mController;
    final ArrayList<TransportStateListener> mListeners
            = new ArrayList<TransportStateListener>();
    final TransportMediatorCallback mTransportKeyCallback
            = new TransportMediatorCallback() {
        @Override
        public void handleKey(KeyEvent key) {
            key.dispatch(mKeyEventCallback);
        }
        @Override
        public void handleAudioFocusChange(int focusChange) {
            mCallbacks.onAudioFocusChange(focusChange);
        }

        @Override
        public long getPlaybackPosition() {
            return mCallbacks.onGetCurrentPosition();
        }

        @Override
        public void playbackPositionUpdate(long newPositionMs) {
            mCallbacks.onSeekTo(newPositionMs);
        }
    };

    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_PLAY KeyEvent.KEYCODE_MEDIA_PLAY} */
    public static final int KEYCODE_MEDIA_PLAY = 126;
    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_PAUSE KeyEvent.KEYCODE_MEDIA_PAUSE} */
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    /** Synonym for {@link KeyEvent#KEYCODE_MEDIA_RECORD KeyEvent.KEYCODE_MEDIA_RECORD} */
    public static final int KEYCODE_MEDIA_RECORD = 130;

    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PREVIOUS
     * RemoveControlClient.FLAG_KEY_MEDIA_PREVIOUS */
    public final static int FLAG_KEY_MEDIA_PREVIOUS = 1 << 0;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_REWIND
     * RemoveControlClient.FLAG_KEY_MEDIA_REWIND */
    public final static int FLAG_KEY_MEDIA_REWIND = 1 << 1;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY
     * RemoveControlClient.FLAG_KEY_MEDIA_PLAY */
    public final static int FLAG_KEY_MEDIA_PLAY = 1 << 2;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY_PAUSE
     * RemoveControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE */
    public final static int FLAG_KEY_MEDIA_PLAY_PAUSE = 1 << 3;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PAUSE
     * RemoveControlClient.FLAG_KEY_MEDIA_PAUSE */
    public final static int FLAG_KEY_MEDIA_PAUSE = 1 << 4;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_STOP
     * RemoveControlClient.FLAG_KEY_MEDIA_STOP */
    public final static int FLAG_KEY_MEDIA_STOP = 1 << 5;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_FAST_FORWARD
     * RemoveControlClient.FLAG_KEY_MEDIA_FAST_FORWARD */
    public final static int FLAG_KEY_MEDIA_FAST_FORWARD = 1 << 6;
    /** Synonym for {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_NEXT
     * RemoveControlClient.FLAG_KEY_MEDIA_NEXT */
    public final static int FLAG_KEY_MEDIA_NEXT = 1 << 7;

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

    public TransportMediator(Activity activity, TransportPerformer callbacks) {
        this(activity, null, callbacks);
    }

    public TransportMediator(View view, TransportPerformer callbacks) {
        this(null, view, callbacks);
    }

    private TransportMediator(Activity activity, View view, TransportPerformer callbacks) {
        mContext = activity != null ? activity : view.getContext();
        mCallbacks = callbacks;
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mView = activity != null ? activity.getWindow().getDecorView() : view;
        mDispatcherState = KeyEventCompat.getKeyDispatcherState(mView);
        if (Build.VERSION.SDK_INT >= 18) { // JellyBean MR2
            mController = new TransportMediatorJellybeanMR2(mContext, mAudioManager,
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
     *
     * <p>Note that this class takes possession of the
     * {@link android.media.RemoteControlClient.OnGetPlaybackPositionListener} and
     * {@link android.media.RemoteControlClient.OnPlaybackPositionUpdateListener} callbacks;
     * you will interact with these through
     * {@link TransportPerformer#onGetCurrentPosition() TransportPerformer.onGetCurrentPosition} and
     * {@link TransportPerformer#onSeekTo TransportPerformer.onSeekTo}, respectively.</p>
     */
    public Object getRemoteControlClient() {
        return mController != null ? mController.getRemoteControlClient() : null;
    }

    /**
     * Must call from {@link Activity#dispatchKeyEvent Activity.dispatchKeyEvent} to give
     * the transport an opportunity to intercept media keys.  Any such keys will show up
     * in {@link TransportPerformer}.
     * @param event
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        return KeyEventCompat.dispatch(event, mKeyEventCallback, mDispatcherState, this);
    }

    public void registerStateListener(TransportStateListener listener) {
        mListeners.add(listener);
    }

    public void unregisterStateListener(TransportStateListener listener) {
        mListeners.remove(listener);
    }

    private TransportStateListener[] getListeners() {
        if (mListeners.size() <= 0) {
            return null;
        }
        TransportStateListener listeners[] = new TransportStateListener[mListeners.size()];
        mListeners.toArray(listeners);
        return listeners;
    }

    private void reportPlayingChanged() {
        TransportStateListener[] listeners = getListeners();
        if (listeners != null) {
            for (TransportStateListener listener : listeners) {
                listener.onPlayingChanged(this);
            }
        }
    }

    private void reportTransportControlsChanged() {
        TransportStateListener[] listeners = getListeners();
        if (listeners != null) {
            for (TransportStateListener listener : listeners) {
                listener.onTransportControlsChanged(this);
            }
        }
    }

    private void pushControllerState() {
        if (mController != null) {
            mController.refreshState(mCallbacks.onIsPlaying(),
                    mCallbacks.onGetCurrentPosition(),
                    mCallbacks.onGetTransportControlFlags());
        }
    }

    public void refreshState() {
        pushControllerState();
        reportPlayingChanged();
        reportTransportControlsChanged();
    }

    /**
     * Move the controller into the playing state.  This updates the remote control
     * client to indicate it is playing, and takes audio focus for the app.
     */
    @Override
    public void startPlaying() {
        if (mController != null) {
            mController.startPlaying();
        }
        mCallbacks.onStart();
        pushControllerState();
        reportPlayingChanged();
    }

    /**
     * Move the controller into the paused state.  This updates the remote control
     * client to indicate it is paused, but keeps audio focus.
     */
    @Override
    public void pausePlaying() {
        if (mController != null) {
            mController.pausePlaying();
        }
        mCallbacks.onPause();
        pushControllerState();
        reportPlayingChanged();
    }

    /**
     * Move the controller into the stopped state.  This updates the remote control
     * client to indicate it is stopped, and removes audio focus from the app.
     */
    @Override
    public void stopPlaying() {
        if (mController != null) {
            mController.stopPlaying();
        }
        mCallbacks.onStop();
        pushControllerState();
        reportPlayingChanged();
    }

    @Override
    public long getDuration() {
        return mCallbacks.onGetDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mCallbacks.onGetCurrentPosition();
    }

    @Override
    public void seekTo(long pos) {
        mCallbacks.onSeekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mCallbacks.onIsPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mCallbacks.onGetBufferPercentage();
    }

    /**
     * Retrieves the flags for the media transport control buttons that this transport supports.
     * Result is a combination of the following flags:
     *      {@link #FLAG_KEY_MEDIA_PREVIOUS},
     *      {@link #FLAG_KEY_MEDIA_REWIND},
     *      {@link #FLAG_KEY_MEDIA_PLAY},
     *      {@link #FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link #FLAG_KEY_MEDIA_PAUSE},
     *      {@link #FLAG_KEY_MEDIA_STOP},
     *      {@link #FLAG_KEY_MEDIA_FAST_FORWARD},
     *      {@link #FLAG_KEY_MEDIA_NEXT}
     */
    public int getTransportControlFlags() {
        return mCallbacks.onGetTransportControlFlags();
    }

    /**
     * Optionally call when no longer using the TransportController.  Its resources
     * will also be automatically cleaned up when your activity/view is detached from
     * its window, so you don't normally need to call this explicitly.
     */
    public void destroy() {
        mController.destroy();
    }
}
