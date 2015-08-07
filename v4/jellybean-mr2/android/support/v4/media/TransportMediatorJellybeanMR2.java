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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;

class TransportMediatorJellybeanMR2 {
    final Context mContext;
    final AudioManager mAudioManager;
    final View mTargetView;
    final TransportMediatorCallback mTransportCallback;
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
    final RemoteControlClient.OnGetPlaybackPositionListener mGetPlaybackPositionListener
            = new RemoteControlClient.OnGetPlaybackPositionListener() {
                @Override
                public long onGetPlaybackPosition() {
                    return mTransportCallback.getPlaybackPosition();
                }
            };
    final RemoteControlClient.OnPlaybackPositionUpdateListener mPlaybackPositionUpdateListener
            = new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                public void onPlaybackPositionUpdate(long newPositionMs) {
                    mTransportCallback.playbackPositionUpdate(newPositionMs);
                }
            };
 
    PendingIntent mPendingIntent;
    RemoteControlClient mRemoteControl;
    boolean mFocused;
    int mPlayState = 0;
    boolean mAudioFocused;

    public TransportMediatorJellybeanMR2(Context context, AudioManager audioManager,
            View view, TransportMediatorCallback transportCallback) {
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
        mRemoteControl.setOnGetPlaybackPositionListener(mGetPlaybackPositionListener);
        mRemoteControl.setPlaybackPositionUpdateListener(mPlaybackPositionUpdateListener);
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

    public void refreshState(boolean playing, long position, int transportControls) {
        if (mRemoteControl != null) {
            mRemoteControl.setPlaybackState(playing ? RemoteControlClient.PLAYSTATE_PLAYING
                    : RemoteControlClient.PLAYSTATE_STOPPED, position, playing ? 1 : 0);
            mRemoteControl.setTransportControlFlags(transportControls);
        }
    }

    public void pausePlaying() {
        if (mPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
            mPlayState = RemoteControlClient.PLAYSTATE_PAUSED;
            mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
        dropAudioFocus();
    }

    public void stopPlaying() {
        if (mPlayState != RemoteControlClient.PLAYSTATE_STOPPED) {
            mPlayState = RemoteControlClient.PLAYSTATE_STOPPED;
            mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }
        dropAudioFocus();
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
