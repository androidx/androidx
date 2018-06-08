/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.leanback;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.leanback.media.PlaybackBaseControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

class PlaybackTransportControlGlueSample<T extends PlayerAdapter> extends
        androidx.leanback.media.PlaybackTransportControlGlue<T> {


    // In this glue, we don't support fast forward/ rewind/ repeat/ shuffle action
    private static final float NORMAL_SPEED = 1.0f;

    // for debugging purpose
    private static final Boolean DEBUG = false;
    private static final String TAG = "PlaybackTransportControlGlue";

    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    private PlaybackControlsRow.PictureInPictureAction mPipAction;
    private PlaybackControlsRow.ClosedCaptioningAction mClosedCaptioningAction;
    private MediaSessionCompat mMediaSessionCompat;

    PlaybackTransportControlGlueSample(Context context, T impl) {
        super(context, impl);
        mClosedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(context);
        mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsUpAction.INDEX_OUTLINE);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(context);
        mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsDownAction.INDEX_OUTLINE);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(context);
        mPipAction = new PlaybackControlsRow.PictureInPictureAction(context);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        adapter.add(mThumbsUpAction);
        adapter.add(mThumbsDownAction);
        if (android.os.Build.VERSION.SDK_INT > 23) {
            adapter.add(mPipAction);
        }
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
        adapter.add(mRepeatAction);
        adapter.add(mClosedCaptioningAction);
    }

    @Override
    public void onActionClicked(Action action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }
        super.onActionClicked(action);
    }

    @Override
    protected void onUpdateBufferedProgress() {
        super.onUpdateBufferedProgress();

        // if the media session is not connected, don't update playback state information
        if (mMediaSessionCompat == null) {
            return;
        }

        mMediaSessionCompat.setPlaybackState(createPlaybackStateBasedOnAdapterState());
    }

    @Override
    protected void onUpdateProgress() {
        super.onUpdateProgress();

        // if the media session is not connected, don't update playback state information
        if (mMediaSessionCompat == null) {
            return;
        }

        mMediaSessionCompat.setPlaybackState(createPlaybackStateBasedOnAdapterState());
    }


    @Override
    protected void onUpdateDuration() {
        super.onUpdateDuration();
        onMediaSessionMetaDataChanged();
    }

    // when meta data is changed, the metadata for media session will also be updated
    @Override
    protected void onMetadataChanged() {
        super.onMetadataChanged();
        onMediaSessionMetaDataChanged();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            Action action = getControlsRow().getActionForKeyCode(keyEvent.getKeyCode());
            if (shouldDispatchAction(action)) {
                dispatchAction(action);
                return true;
            }
        }
        return super.onKey(view, keyCode, keyEvent);
    }

    /**
     * Public api to connect media session to this glue
     */
    public void connectToMediaSession(MediaSessionCompat mediaSessionCompat) {
        mMediaSessionCompat = mediaSessionCompat;
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setActive(true);
        mMediaSessionCompat.setCallback(new MediaSessionCallback());
        onMediaSessionMetaDataChanged();
    }

    /**
     * Public api to disconnect media session from this glue
     */
    public void disconnectToMediaSession() {
        if (DEBUG) {
            Log.e(TAG, "disconnectToMediaSession: Media session disconnected");
        }
        mMediaSessionCompat.setActive(false);
        mMediaSessionCompat.release();
    }

    private boolean shouldDispatchAction(Action action) {
        return action == mRepeatAction || action == mThumbsUpAction || action == mThumbsDownAction;
    }

    private void dispatchAction(Action action) {
        Toast.makeText(getContext(), action.toString(), Toast.LENGTH_SHORT).show();
        PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
        multiAction.nextIndex();
        notifyActionChanged(multiAction);
    }

    private void notifyActionChanged(PlaybackControlsRow.MultiAction action) {
        int index = -1;
        if (getPrimaryActionsAdapter() != null) {
            index = getPrimaryActionsAdapter().indexOf(action);
        }
        if (index >= 0) {
            getPrimaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
        } else {
            if (getSecondaryActionsAdapter() != null) {
                index = getSecondaryActionsAdapter().indexOf(action);
                if (index >= 0) {
                    getSecondaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
                }
            }
        }
    }

    private ArrayObjectAdapter getPrimaryActionsAdapter() {
        if (getControlsRow() == null) {
            return null;
        }
        return (ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    private ArrayObjectAdapter getSecondaryActionsAdapter() {
        if (getControlsRow() == null) {
            return null;
        }
        return (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
    }

    Handler mHandler = new Handler();

    @Override
    protected void onPlayCompleted() {
        super.onPlayCompleted();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRepeatAction.getIndex() != PlaybackControlsRow.RepeatAction.INDEX_NONE) {
                    play();
                }
            }
        });
    }

    public void setMode(int mode) {
        mRepeatAction.setIndex(mode);
        if (getPrimaryActionsAdapter() == null) {
            return;
        }
        notifyActionChanged(mRepeatAction);
    }

    /**
     * Callback function when media session's meta data is changed.
     * When this function is returned, the callback function onMetaDataChanged will be
     * executed to address the new playback state.
     */
    private void onMediaSessionMetaDataChanged() {

        /**
         * Only update the media session's meta data when the media session is connected
         */
        if (mMediaSessionCompat == null) {
            return;
        }

        MediaMetadataCompat.Builder metaDataBuilder = new MediaMetadataCompat.Builder();

        // update media title
        if (getTitle() != null) {
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                    getTitle().toString());
        }

        if (getSubtitle() != null) {
            // update media subtitle
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    getSubtitle().toString());
        }

        if (getArt() != null) {
            // update media art bitmap
            Drawable artDrawable = getArt();
            metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    Bitmap.createBitmap(
                            artDrawable.getIntrinsicWidth(), artDrawable.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888));
        }

        metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

        mMediaSessionCompat.setMetadata(metaDataBuilder.build());
    }

    @Override
    public void play() {
        super.play();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    protected void onPlayStateChanged() {
        super.onPlayStateChanged();

        // return when the media session compat is null
        if (mMediaSessionCompat == null) {
            return;
        }

        mMediaSessionCompat.setPlaybackState(createPlaybackStateBasedOnAdapterState());
    }

    @Override
    protected void onPreparedStateChanged() {
        super.onPreparedStateChanged();

        // return when the media session compat is null
        if (mMediaSessionCompat == null) {
            return;
        }

        mMediaSessionCompat.setPlaybackState(createPlaybackStateBasedOnAdapterState());
    }

    // associate media session event with player action
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            play();
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onSeekTo(long pos) {
            seekTo(pos);
        }
    }

    /**
     * Get supported actions from player adapter then translate it into playback state compat
     * related actions
     */
    private long getPlaybackStateActions() {
        long supportedActions = 0L;
        long actionsFromPlayerAdapter = getPlayerAdapter().getSupportedActions();
        if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_SKIP_TO_PREVIOUS) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_SKIP_TO_NEXT) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_REWIND) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_REWIND;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_FAST_FORWARD) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_FAST_FORWARD;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_PLAY_PAUSE) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_PLAY_PAUSE;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_REPEAT) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_SET_REPEAT_MODE;
        } else if ((actionsFromPlayerAdapter & PlaybackBaseControlGlue.ACTION_SHUFFLE) != 0) {
            supportedActions |= PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE;
        }
        return supportedActions;
    }

    /**
     * Helper function to create a playback state based on current adapter's state.
     *
     * @return playback state compat builder
     */
    private PlaybackStateCompat createPlaybackStateBasedOnAdapterState() {

        PlaybackStateCompat.Builder playbackStateCompatBuilder = new PlaybackStateCompat.Builder();
        long currentPosition = getCurrentPosition();
        long bufferedPosition = getBufferedPosition();

        // In this glue we only support normal speed
        float playbackSpeed = NORMAL_SPEED;

        // Translate player adapter's state to play back state compat
        // If player adapter is not prepared
        // ==> STATE_STOPPED
        //     (Launcher can only visualize the media session under playing state,
        //     it makes more sense to map this state to PlaybackStateCompat.STATE_STOPPED)
        // If player adapter is prepared
        //     If player is playing
        //     ==> STATE_PLAYING
        //     If player is not playing
        //     ==> STATE_PAUSED
        if (!getPlayerAdapter().isPrepared()) {
            playbackStateCompatBuilder
                    .setState(PlaybackStateCompat.STATE_STOPPED, currentPosition, playbackSpeed)
                    .setActions(getPlaybackStateActions());
        } else if (getPlayerAdapter().isPlaying()) {
            playbackStateCompatBuilder
                    .setState(PlaybackStateCompat.STATE_PLAYING, currentPosition, playbackSpeed)
                    .setActions(getPlaybackStateActions());
        } else {
            playbackStateCompatBuilder
                    .setState(PlaybackStateCompat.STATE_PAUSED, currentPosition, playbackSpeed)
                    .setActions(getPlaybackStateActions());
        }

        // always fill buffered position
        return playbackStateCompatBuilder.setBufferedPosition(bufferedPosition).build();
    }
}
