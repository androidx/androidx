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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;

public class TransportControllerJellybeanMR2 {
    final Context mContext;
    final AudioManager mAudioManager;
    final View mTargetView;
    final TransportCallback mTransportCallback;
    final String mReceiverAction;
    final IntentFilter mReceiverFilter;
    final Intent mIntent;
    final ViewTreeObserver.OnWindowAttachListener mWindowAttachListener =
            new ViewTreeObserver.OnWindowAttachListener() {
                @Override
                public void onWindowAttached() {
                    windowAttached();
                }
                @Override
                public void onWindowDetached() {
                    windowDetached();
                }
            };
    final ViewTreeObserver.OnWindowFocusChangeListener mWindowFocusListener =
            new ViewTreeObserver.OnWindowFocusChangeListener() {
                @Override
                public void onWindowFocusChanged(boolean hasFocus) {
                    if (hasFocus) gainFocus();
                    else loseFocus();
                }
            };
    final BroadcastReceiver mMediaButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                mTransportCallback.handleKey(event);
            } catch (ClassCastException e) {
                Log.w("TransportController", e);
            }
        }
    };
    AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            mTransportCallback.handleAudioFocusChange(focusChange);
        }
    };

    PendingIntent mPendingIntent;
    RemoteControlClient mRemoteControl;
    boolean mFocused;
    int mPlayState = 0;
    boolean mAudioFocused;

    public interface TransportCallback {
        public void handleKey(KeyEvent key);
        public void handleAudioFocusChange(int focusChange);
    }

    public TransportControllerJellybeanMR2(Context context, AudioManager audioManager,
            View view, TransportCallback transportCallback) {
        mContext = context;
        mAudioManager = audioManager;
        mTargetView = view;
        mTransportCallback = transportCallback;
        mReceiverAction = context.getPackageName() + ":transport:" + System.identityHashCode(this);
        mIntent = new Intent(mReceiverAction);
        mIntent.setPackage(context.getPackageName());
        mReceiverFilter = new IntentFilter();
        mReceiverFilter.addAction(mReceiverAction);
        mTargetView.getViewTreeObserver().addOnWindowAttachListener(mWindowAttachListener);
        mTargetView.getViewTreeObserver().addOnWindowFocusChangeListener(mWindowFocusListener);
    }

    public Object getRemoteControlClient() {
        return mRemoteControl;
    }

    public void destroy() {
        windowDetached();
        mTargetView.getViewTreeObserver().removeOnWindowAttachListener(mWindowAttachListener);
        mTargetView.getViewTreeObserver().removeOnWindowFocusChangeListener(mWindowFocusListener);
    }

    void windowAttached() {
        mContext.registerReceiver(mMediaButtonReceiver, mReceiverFilter);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mRemoteControl = new RemoteControlClient(mPendingIntent);
    }

    void gainFocus() {
        if (!mFocused) {
            mFocused = true;
            mAudioManager.registerMediaButtonEventReceiver(mPendingIntent);
            mAudioManager.registerRemoteControlClient(mRemoteControl);
            if (mPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
                takeAudioFocus();
            }
        }
    }

    void takeAudioFocus() {
        if (!mAudioFocused) {
            mAudioFocused = true;
            mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public void startPlaying() {
        if (mPlayState != RemoteControlClient.PLAYSTATE_PLAYING) {
            mPlayState = RemoteControlClient.PLAYSTATE_PLAYING;
            mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
        if (mFocused) {
            takeAudioFocus();
        }
    }

    public void pausePlaying() {
        if (mPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
            mPlayState = RemoteControlClient.PLAYSTATE_PAUSED;
            mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    public void stopPlaying() {
        if (mPlayState != RemoteControlClient.PLAYSTATE_STOPPED) {
            mPlayState = RemoteControlClient.PLAYSTATE_STOPPED;
            mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }
        dropAudioFocus();
    }

    void handleAudioFocusChange(int focusChange) {
        int keyCode = 0;
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
                break;
        }
        if (keyCode != 0) {
            final long now = SystemClock.uptimeMillis();
            mTransportCallback.handleKey(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));
            mTransportCallback.handleKey(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));
        }
    }

    void dropAudioFocus() {
        if (mAudioFocused) {
            mAudioFocused = false;
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }

    void loseFocus() {
        dropAudioFocus();
        if (mFocused) {
            mFocused = false;
            mAudioManager.unregisterRemoteControlClient(mRemoteControl);
            mAudioManager.unregisterMediaButtonEventReceiver(mPendingIntent);
        }
    }

    void windowDetached() {
        loseFocus();
        if (mPendingIntent != null) {
            mContext.unregisterReceiver(mMediaButtonReceiver);
            mPendingIntent.cancel();
            mPendingIntent = null;
            mRemoteControl = null;
        }
    }
}
