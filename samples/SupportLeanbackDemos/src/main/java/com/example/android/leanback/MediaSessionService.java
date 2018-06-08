/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The service to play music. It also contains the media session.
 */
public class MediaSessionService extends Service {


    public static final String CANNOT_SET_DATA_SOURCE = "Cannot set data source";
    private static final float NORMAL_SPEED = 1.0f;

    /**
     * When media player is prepared, our service can send notification to UI side through this
     * callback. So UI will have chance to prepare/ pre-processing the UI status.
     */
    interface MediaPlayerListener {
        void onPrepared();
    }

    /**
     * This LocalBinder class contains the getService() method which will return the service object.
     */
    public class LocalBinder extends Binder {
        MediaSessionService getService() {
            return MediaSessionService.this;
        }
    }

    /**
     * Constant used in this class.
     */
    private static final String MUSIC_PLAYER_SESSION_TOKEN = "MusicPlayer Session token";
    private static final int MEDIA_ACTION_NO_REPEAT = 0;
    private static final int MEDIA_ACTION_REPEAT_ONE = 1;
    private static final int MEDIA_ACTION_REPEAT_ALL = 2;
    public static final String MEDIA_PLAYER_ERROR_MESSAGE = "Media player error message";
    public static final String PLAYER_NOT_INITIALIZED = "Media player not initialized";
    public static final String PLAYER_IS_PLAYING = "Media player is playing";
    public static final String PLAYER_SET_DATA_SOURCE_ERROR =
            "Media player set new data source error";
    private static final boolean DEBUG = false;
    private static final String TAG = "MusicPlaybackService";
    private static final int FOCUS_CHANGE = 0;

    // This handler can control media player through audio's status.
    private class MediaPlayerAudioHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FOCUS_CHANGE:
                    switch (msg.arg1) {
                        // pause media item when audio focus is lost
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (isPlaying()) {
                                audioFocusLossHandler();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (isPlaying()) {
                                audioLossFocusTransientHandler();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (isPlaying()) {
                                audioLossFocusTransientCanDuckHanlder();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (!isPlaying()) {
                                audioFocusGainHandler();
                            }
                            break;
                    }
            }
        }
    }

    // The callbacks' collection which can be notified by this service.
    private List<MediaPlayerListener> mCallbacks = new ArrayList<>();

    // audio manager obtained from system to gain audio focus
    private AudioManager mAudioManager;

    // record user defined repeat mode.
    private int mRepeatState = MEDIA_ACTION_NO_REPEAT;

    // record user defined shuffle mode.
    private int mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;

    private MediaPlayer mPlayer;
    private MediaSessionCompat mMediaSession;

    // set -1 as invalid media item for playing.
    private int mCurrentIndex = -1;
    // media item in media playlist.
    private MusicItem mCurrentMediaItem;
    // media player's current progress.
    private int mCurrentPosition;
    // Buffered Position which will be updated inside of OnBufferingUpdateListener
    private long mBufferedProgress;
    List<MusicItem> mMediaItemList = new ArrayList<>();
    private boolean mInitialized;

    // fast forward/ rewind speed factors and indexes
    private float[] mFastForwardSpeedFactors;
    private float[] mRewindSpeedFactors;
    private int mFastForwardSpeedFactorIndex = 0;
    private int mRewindSpeedFactorIndex = 0;

    // Flags to indicate if current state is fast forwarding/ rewinding.
    private boolean mIsFastForwarding;
    private boolean mIsRewinding;

    // handle audio related event.
    private Handler mMediaPlayerHandler = new MediaPlayerAudioHandler();

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume and continue playing.
    private static final float REDUCED_VOLUME = 0.1f;
    // The volume we set the media player when we have audio focus.
    private static final float FULL_VOLUME = 1.0f;

    // Record position when current rewind action begins.
    private long mRewindStartPosition;
    // Record the time stamp when current rewind action is ended.
    private long mRewindEndTime;
    // Record the time stamp when current rewind action is started.
    private long mRewindStartTime;
    // Flag to represent the beginning of rewind operation.
    private boolean mIsRewindBegin;

    // A runnable object which will delay the execution of mPlayer.stop()
    private Runnable mDelayedStopRunnable = new Runnable() {
        @Override
        public void run() {
            mPlayer.stop();
            mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_STOPPED).build());
        }
    };

    // Listener for audio focus.
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new
            AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (DEBUG) {
                        Log.d(TAG, "onAudioFocusChange. focusChange=" + focusChange);
                    }
                    mMediaPlayerHandler.obtainMessage(FOCUS_CHANGE, focusChange, 0).sendToTarget();
                }
            };

    private final IBinder mBinder = new LocalBinder();

    /**
     * The public API to gain media session instance from service.
     *
     * @return Media Session Instance.
     */
    public MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // This service can be created for multiple times, the objects will only be created when
        // it is null
        if (mMediaSession == null) {
            mMediaSession = new MediaSessionCompat(this, MUSIC_PLAYER_SESSION_TOKEN);
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mMediaSession.setCallback(new MediaSessionCallback());
        }

        if (mAudioManager == null) {
            // Create audio manager through system service
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        // initialize the player (including activate media session, request audio focus and
        // set up the listener to listen to player's state)
        initializePlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        mMediaPlayerHandler.removeCallbacksAndMessages(null);
        if (mPlayer != null) {
            // stop and release the media player since it's no longer in use
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
    }

    /**
     * After binding to this service, other component can set Media Item List and prepare
     * the first item in the list through this function.
     *
     * @param mediaItemList A list of media item to play.
     * @param isQueue       When this parameter is true, that meas new items should be appended to
     *                      original media item list.
     *                      If this parameter is false, the original playlist will be cleared and
     *                      replaced with a new media item list.
     */
    public void setMediaList(List<MusicItem> mediaItemList, boolean isQueue) {
        if (!isQueue) {
            mMediaItemList.clear();
        }
        mMediaItemList.addAll(mediaItemList);

        /**
         * Points to the first media item in play list.
         */
        mCurrentIndex = 0;
        mCurrentMediaItem = mMediaItemList.get(0);

        try {
            mPlayer.setDataSource(this.getApplicationContext(),
                    mCurrentMediaItem.getMediaSourceUri(getApplicationContext()));
            // Prepare the player asynchronously, use onPrepared listener as signal.
            mPlayer.prepareAsync();
        } catch (IOException e) {
            PlaybackStateCompat.Builder ret = createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_ERROR);
            ret.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                    PLAYER_SET_DATA_SOURCE_ERROR);
        }
    }

    /**
     * Set Fast Forward Speeds for this media session service.
     *
     * @param fastForwardSpeeds The array contains all fast forward speeds.
     */
    public void setFastForwardSpeedFactors(int[] fastForwardSpeeds) {
        mFastForwardSpeedFactors = new float[fastForwardSpeeds.length + 1];

        // Put normal speed factor at the beginning of the array
        mFastForwardSpeedFactors[0] = 1.0f;

        for (int index = 1; index < mFastForwardSpeedFactors.length; ++index) {
            mFastForwardSpeedFactors[index] = fastForwardSpeeds[index - 1];
        }
    }

    /**
     * Set Rewind Speeds for this media session service.
     *
     * @param rewindSpeeds The array contains all rewind speeds.
     */
    public void setRewindSpeedFactors(int[] rewindSpeeds) {
        mRewindSpeedFactors = new float[rewindSpeeds.length];
        for (int index = 0; index < mRewindSpeedFactors.length; ++index) {
            mRewindSpeedFactors[index] = -rewindSpeeds[index];
        }
    }

    /**
     * Prepare the first item in the list. And setup the listener for media player.
     */
    private void initializePlayer() {
        // This service can be created for multiple times, the objects will only be created when
        // it is null
        if (mPlayer != null) {
            return;
        }
        mPlayer = new MediaPlayer();

        // Set playback state to none to create a valid playback state. So controls row can get
        // information about the supported actions.
        mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                PlaybackStateCompat.STATE_NONE).build());
        // Activate media session
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }

        // Set up listener and audio stream type for underlying music player.
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // set up listener when the player is prepared.
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mInitialized = true;
                // Every time when the player is prepared (when new data source is set),
                // all listeners will be notified to toggle the UI to "pause" status.
                notifyUiWhenPlayerIsPrepared();

                // When media player is prepared, the callback functions will be executed to update
                // the meta data and playback state.
                onMediaSessionMetaDataChanged();
                mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                        PlaybackStateCompat.STATE_PAUSED).build());
            }
        });

        // set up listener for player's error.
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                if (DEBUG) {
                    PlaybackStateCompat.Builder builder = createPlaybackStateBuilder(
                            PlaybackStateCompat.STATE_ERROR);
                    builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                            MEDIA_PLAYER_ERROR_MESSAGE);
                    mMediaSession.setPlaybackState(builder.build());
                }
                return true;
            }
        });

        // set up listener to respond the event when current music item is finished
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            /**
             * Expected Interaction Behavior:
             * 1. If current media item's playing speed not equal to normal speed.
             *
             *    A. MEDIA_ACTION_REPEAT_ALL
             *       a. If current media item is the last one. The first music item in the list will
             *          be prepared, but it won't play until user press play button.
             *
             *          When user press the play button, the speed will be reset to normal (1.0f)
             *          no matter what the previous media item's playing speed is.
             *
             *       b. If current media item isn't the last one, next media item will be prepared,
             *          but it won't play.
             *
             *          When user press the play button, the speed will be reset to normal (1.0f)
             *          no matter what the previous media item's playing speed is.
             *
             *    B. MEDIA_ACTION_REPEAT_ONE
             *       Different with previous scenario, current item will go back to the start point
             *       again and play automatically. (The reason to enable auto play here is for
             *       testing purpose and to make sure our designed API is flexible enough to support
             *       different situations.)
             *
             *       No matter what the previous media item's playing speed is, in this situation
             *       current media item will be replayed in normal speed.
             *
             *    C. MEDIA_ACTION_REPEAT_NONE
             *       a. If current media is the last one. The service will be closed, no music item
             *          will be prepared to play. From the UI perspective, the progress bar will not
             *          be reset to the starting point.
             *
             *       b. If current media item isn't the last one, next media item will be prepared,
             *          but it won't play.
             *
             *          When user press the play button, the speed will be reset to normal (1.0f)
             *          no matter what the previous media item's playing speed is.
             *
             * @param mp Object of MediaPlayer。
             */
            @Override
            public void onCompletion(MediaPlayer mp) {

                // When current media item finishes playing, always reset rewind/ fastforward state
                mFastForwardSpeedFactorIndex = 0;
                mRewindSpeedFactorIndex = 0;
                // Set player's playback speed back to normal
                mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(
                        mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex]));
                // Pause the player, and update the status accordingly.
                mPlayer.pause();
                mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                        PlaybackStateCompat.STATE_PAUSED).build());

                if (mRepeatState == MEDIA_ACTION_REPEAT_ALL
                        && mCurrentIndex == mMediaItemList.size() - 1) {
                    // if the repeat mode is enabled but the shuffle mode is not enabled,
                    // will go back to the first music item to play
                    if (mShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
                        mCurrentIndex = 0;
                    } else {
                        // Or will choose a music item from playing list randomly.
                        mCurrentIndex = generateMediaItemIndex();
                    }
                    mCurrentMediaItem = mMediaItemList.get(mCurrentIndex);
                    // The ui will also be changed from playing state to pause state through
                    // setDataSource() operation
                    setDataSource();
                } else if (mRepeatState == MEDIA_ACTION_REPEAT_ONE) {
                    // Play current music item again.
                    // The ui will stay to be "playing" status for the reason that there is no
                    // setDataSource() function call.
                    mPlayer.start();
                    mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                            PlaybackStateCompat.STATE_PLAYING).build());
                } else if (mCurrentIndex < mMediaItemList.size() - 1) {
                    if (mShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
                        mCurrentIndex++;
                    } else {
                        mCurrentIndex = generateMediaItemIndex();
                    }
                    mCurrentMediaItem = mMediaItemList.get(mCurrentIndex);
                    // The ui will also be changed from playing state to pause state through
                    // setDataSource() operation
                    setDataSource();
                } else {
                    // close the service when the playlist is finished
                    // The PlaybackState will be updated to STATE_STOPPED. And onPlayComplete
                    // callback will be called by attached glue.
                    mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                            PlaybackStateCompat.STATE_STOPPED).build());
                    stopSelf();
                }
            }
        });

        final MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener =
                new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        mBufferedProgress = getDuration() * percent / 100;
                        PlaybackStateCompat.Builder builder = createPlaybackStateBuilder(
                                PlaybackStateCompat.STATE_BUFFERING);
                        builder.setBufferedPosition(mBufferedProgress);
                        mMediaSession.setPlaybackState(builder.build());
                    }
                };
        mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
    }


    /**
     * Public API to register listener for this service.
     *
     * @param listener The listener which will keep tracking current service's status
     */
    public void registerCallback(MediaPlayerListener listener) {
        mCallbacks.add(listener);
    }

    /**
     * Instead of shuffling the who music list, we will generate a media item index randomly
     * and return it as the index for next media item to play.
     *
     * @return The index of next media item to play.
     */
    private int generateMediaItemIndex() {
        return new Random().nextInt(mMediaItemList.size());
    }

    /**
     * When player is prepared, service will send notification to UI through calling the callback's
     * method
     */
    private void notifyUiWhenPlayerIsPrepared() {
        for (MediaPlayerListener callback : mCallbacks) {
            callback.onPrepared();
        }
    }

    /**
     * Set up media session callback to associate with player's operation.
     */
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
        public void onSkipToNext() {
            next();
        }

        @Override
        public void onSkipToPrevious() {
            previous();
        }

        @Override
        public void onStop() {
            stop();
        }

        @Override
        public void onSeekTo(long pos) {
            // media player's seekTo method can only take integer as the parameter
            // so the data type need to be casted as int
            seekTo((int) pos);
        }

        @Override
        public void onFastForward() {
            fastForward();
        }

        @Override
        public void onRewind() {
            rewind();
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            setRepeatState(repeatMode);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            setShuffleMode(shuffleMode);
        }
    }

    /**
     * Set new data source and prepare the music player asynchronously.
     */
    private void setDataSource() {
        reset();
        try {
            mPlayer.setDataSource(this.getApplicationContext(),
                    mCurrentMediaItem.getMediaSourceUri(getApplicationContext()));
            mPlayer.prepareAsync();
        } catch (IOException e) {
            PlaybackStateCompat.Builder builder = createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_ERROR);
            builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                    CANNOT_SET_DATA_SOURCE);
            mMediaSession.setPlaybackState(builder.build());
        }
    }

    /**
     * This function will return a playback state builder based on playbackState and current
     * media position.
     *
     * @param playState current playback state.
     * @return Object of PlaybackStateBuilder.
     */
    private PlaybackStateCompat.Builder createPlaybackStateBuilder(int playState) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        long currentPosition = getCurrentPosition();
        float playbackSpeed = NORMAL_SPEED;
        if (mIsFastForwarding) {
            playbackSpeed = mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex];
            // After setting the playback speed, reset mIsFastForwarding flag.
            mIsFastForwarding = false;
        } else if (mIsRewinding) {
            playbackSpeed = mRewindSpeedFactors[mRewindSpeedFactorIndex];
            // After setting the playback speed, reset mIsRewinding flag.
            mIsRewinding = false;
        }
        playbackStateBuilder.setState(playState, currentPosition, playbackSpeed
        ).setActions(
                getPlaybackStateActions()
        );
        return playbackStateBuilder;
    }

    /**
     * Return supported actions related to current playback state.
     * Currently the return value from this function is a constant.
     * For demonstration purpose, the customized fast forward action and customized rewind action
     * are supported in our case.
     *
     * @return playback state actions.
     */
    private long getPlaybackStateActions() {
        long res = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                | PlaybackStateCompat.ACTION_SET_REPEAT_MODE;
        return res;
    }

    /**
     * Callback function when media session's meta data is changed.
     * When this function is returned, the callback function onMetaDataChanged will be
     * executed to address the new playback state.
     */
    private void onMediaSessionMetaDataChanged() {
        if (mCurrentMediaItem == null) {
            throw new IllegalArgumentException(
                    "mCurrentMediaItem is null in onMediaSessionMetaDataChanged!");
        }
        MediaMetadataCompat.Builder metaDataBuilder = new MediaMetadataCompat.Builder();

        if (mCurrentMediaItem.getMediaTitle() != null) {
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                    mCurrentMediaItem.getMediaTitle());
        }

        if (mCurrentMediaItem.getMediaDescription() != null) {
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    mCurrentMediaItem.getMediaDescription());
        }

        if (mCurrentMediaItem.getMediaAlbumArtResId(getApplicationContext()) != 0) {
            Bitmap albumArtBitmap = BitmapFactory.decodeResource(getResources(),
                    mCurrentMediaItem.getMediaAlbumArtResId(getApplicationContext()));
            metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap);
        }

        // duration information will be fetched from player.
        metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

        mMediaSession.setMetadata(metaDataBuilder.build());
    }

    // Reset player. will be executed when new data source is assigned.
    private void reset() {
        if (mPlayer != null) {
            mPlayer.reset();
            mInitialized = false;
        }
    }

    // Control the player to play the music item.
    private void play() {
        // Only when player is not null (meaning the player has been created), the player is
        // prepared (using the mInitialized as the flag to represent it,
        // this boolean variable will only be assigned to true inside of the onPrepared callback)
        // and the media item is not currently playing (!isPlaying()), then the player can be
        // started.

        // If the player has not been prepared, but this function is fired, it is an error state
        // from the app side
        if (!mInitialized) {
            PlaybackStateCompat.Builder builder = createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_ERROR);
            builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                    PLAYER_NOT_INITIALIZED);
            mMediaSession.setPlaybackState(builder.build());

            // If the player has is playing, and this function is fired again, it is an error state
            // from the app side
        }  else {
            // Request audio focus only when needed
            if (mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return;
            }

            if (mPlayer.getPlaybackParams().getSpeed() != NORMAL_SPEED) {
                // Reset to normal speed and play
                resetSpeedAndPlay();
            } else {
                // Continue play.
                mPlayer.start();
                mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                        PlaybackStateCompat.STATE_PLAYING).build());
            }
        }

    }

    // Control the player to pause current music item.
    private void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            // abandon audio focus immediately when the music item is paused.
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);

            mPlayer.pause();
            // Update playbackState.
            mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_PAUSED).build());
        }
    }

    // Control the player to stop.
    private void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            // Update playbackState.
            mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_STOPPED).build());
        }
    }


    /**
     * Control the player to play next music item.
     * Expected Interaction Behavior:
     * No matter current media item is playing or not, when use hit next button, next item will be
     * prepared but won't play unless user hit play button
     *
     * Also no matter current media item is fast forwarding or rewinding. Next music item will
     * be played in normal speed.
     */
    private void next() {
        if (mMediaItemList.isEmpty()) {
            return;
        }
        mCurrentIndex = (mCurrentIndex + 1) % mMediaItemList.size();
        mCurrentMediaItem = mMediaItemList.get(mCurrentIndex);

        // Reset FastForward/ Rewind state to normal state
        mFastForwardSpeedFactorIndex = 0;
        mRewindSpeedFactorIndex = 0;
        // Set player's playback speed back to normal
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(
                mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex]));
        // Pause the player and update the play state.
        // The ui will also be changed from "playing" state to "pause" state.
        mPlayer.pause();
        mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                PlaybackStateCompat.STATE_PAUSED).build());
        // set new data source to play based on mCurrentIndex and prepare the player.
        // The ui will also be changed from "playing" state to "pause" state through setDataSource()
        // operation
        setDataSource();
    }

    /**
     * Control the player to play next music item.
     * Expected Interaction Behavior:
     * No matter current media item is playing or not, when use hit previous button, previous item
     * will be prepared but won't play unless user hit play button
     *
     * Also no matter current media item is fast forwarding or rewinding. Previous music item will
     * be played in normal speed.
     */
    private void previous() {
        if (mMediaItemList.isEmpty()) {
            return;
        }
        mCurrentIndex = (mCurrentIndex - 1 + mMediaItemList.size()) % mMediaItemList.size();
        mCurrentMediaItem = mMediaItemList.get(mCurrentIndex);

        // Reset FastForward/ Rewind state to normal state
        mFastForwardSpeedFactorIndex = 0;
        mRewindSpeedFactorIndex = 0;
        // Set player's playback speed back to normal
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(
                mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex]));
        // Pause the player and update the play state.
        // The ui will also be changed from "playing" state to "pause" state.
        mPlayer.pause();
        // Update playbackState.
        mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                PlaybackStateCompat.STATE_PAUSED).build());
        // set new data source to play based on mCurrentIndex and prepare the player.
        // The ui will also be changed from "playing" state to "pause" state through setDataSource()
        // operation
        setDataSource();
    }

    // Get is playing information from underlying player.
    private boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    // Play media item in a fast forward speed.
    private void fastForward() {
        // To support fast forward action, the mRewindSpeedFactors must be provided through
        // setFastForwardSpeedFactors() method;
        if (mFastForwardSpeedFactors == null) {
            if (DEBUG) {
                Log.d(TAG, "FastForwardSpeedFactors are not set");
            }
            return;
        }

        // Toggle the flag to indicate fast forward status.
        mIsFastForwarding = true;

        // The first element in mFastForwardSpeedFactors is used to represent the normal speed.
        // Will always be incremented by 1 firstly before setting the speed.
        mFastForwardSpeedFactorIndex += 1;
        if (mFastForwardSpeedFactorIndex > mFastForwardSpeedFactors.length - 1) {
            mFastForwardSpeedFactorIndex = mFastForwardSpeedFactors.length - 1;
        }

        // In our customized fast forward operation, the media player will not be paused,
        // But the player's speed will be changed accordingly.
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(
                mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex]));
        // Update playback state, mIsFastForwarding will be reset to false inside of it.
        mMediaSession.setPlaybackState(
                createPlaybackStateBuilder(PlaybackStateCompat.STATE_FAST_FORWARDING).build());
    }


    // Play media item in a rewind speed.
    // Android media player doesn't support negative speed. So for customized rewind operation,
    // the player will be paused internally, but the pause state will not be published. So from
    // the UI perspective, the player is still in playing status.
    // Every time when the rewind speed is changed, the position will be computed through previous
    // rewind speed then media player will seek to that position for seamless playing.
    private void rewind() {
        // To support rewind action, the mRewindSpeedFactors must be provided through
        // setRewindSpeedFactors() method;
        if (mRewindSpeedFactors == null) {
            if (DEBUG) {
                Log.d(TAG, "RewindSpeedFactors are not set");
            }
            return;
        }

        // Perform rewind operation using different speed.
        if (mIsRewindBegin) {
            // record end time stamp for previous rewind operation.
            mRewindEndTime = SystemClock.elapsedRealtime();
            long position = mRewindStartPosition
                    + (long) mRewindSpeedFactors[mRewindSpeedFactorIndex - 1] * (
                    mRewindEndTime - mRewindStartTime);
            if (DEBUG) {
                Log.e(TAG, "Last Rewind Operation Position" + position);
            }
            mPlayer.seekTo((int) position);

            // Set new start status
            mRewindStartPosition = position;
            mRewindStartTime = mRewindEndTime;
            // It is still in rewind state, so mIsRewindBegin remains to be true.
        }

        // Perform rewind operation using the first speed set.
        if (!mIsRewindBegin) {
            mRewindStartPosition = getCurrentPosition();
            Log.e("REWIND_BEGIN", "REWIND BEGIN PLACE " + mRewindStartPosition);
            mIsRewindBegin = true;
            mRewindStartTime = SystemClock.elapsedRealtime();
        }

        // Toggle the flag to indicate rewind status.
        mIsRewinding = true;

        // Pause the player but won't update the UI status.
        mPlayer.pause();

        // Update playback state, mIsRewinding will be reset to false inside of it.
        mMediaSession.setPlaybackState(
                createPlaybackStateBuilder(PlaybackStateCompat.STATE_REWINDING).build());

        mRewindSpeedFactorIndex += 1;
        if (mRewindSpeedFactorIndex > mRewindSpeedFactors.length - 1) {
            mRewindSpeedFactorIndex = mRewindSpeedFactors.length - 1;
        }
    }

    // Reset the playing speed to normal.
    // From PlaybackBannerGlue's key dispatching mechanism. If the player is currently in rewinding
    // or fast forwarding status, moving from the rewinding/ FastForwarindg button will trigger
    // the fastForwarding/ rewinding ending event.
    // When customized fast forwarding or rewinding actions are supported, this function will be
    // called.
    // If we are in rewind mode, this function will compute the new position through rewinding
    // speed and compare the start/ end rewinding time stamp.
    private void resetSpeedAndPlay() {

        if (mIsRewindBegin) {
            mIsRewindBegin = false;
            mRewindEndTime = SystemClock.elapsedRealtime();

            long position = mRewindStartPosition
                    + (long) mRewindSpeedFactors[mRewindSpeedFactorIndex ] * (
                    mRewindEndTime - mRewindStartTime);

            // Seek to the computed position for seamless playing.
            mPlayer.seekTo((int) position);
        }

        // Reset the state to normal state.
        mFastForwardSpeedFactorIndex = 0;
        mRewindSpeedFactorIndex = 0;
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(
                mFastForwardSpeedFactors[mFastForwardSpeedFactorIndex]));

        // Update the playback status from rewinding/ fast forwardindg to STATE_PLAYING.
        // Which indicates current media item is played in the normal speed.
        mMediaSession.setPlaybackState(
                createPlaybackStateBuilder(PlaybackStateCompat.STATE_PLAYING).build());
    }

    // Get current playing progress from media player.
    private int getCurrentPosition() {
        if (mInitialized && mPlayer != null) {
            // Always record current position for seekTo operation.
            mCurrentPosition = mPlayer.getCurrentPosition();
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    // get music duration from underlying music player
    private int getDuration() {
        return (mInitialized && mPlayer != null) ? mPlayer.getDuration() : 0;
    }

    // seek to specific position through underlying music player.
    private void seekTo(int newPosition) {
        if (mPlayer != null) {
            mPlayer.seekTo(newPosition);
        }
    }

    // set shuffle mode through passed parameter.
    private void setShuffleMode(int shuffleMode) {
        mShuffleMode = shuffleMode;
    }

    // set shuffle mode through passed parameter.
    public void setRepeatState(int repeatState) {
        mRepeatState = repeatState;
    }

    private void audioFocusLossHandler() {
        // Permanent loss of audio focus
        // Pause playback immediately
        mPlayer.pause();
        // Wait 30 seconds before stopping playback
        mMediaPlayerHandler.postDelayed(mDelayedStopRunnable, 30);
        // Update playback state.
        mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                PlaybackStateCompat.STATE_PAUSED).build());
        // Will record current player progress when losing the audio focus.
        mCurrentPosition = getCurrentPosition();
    }

    private void audioLossFocusTransientHandler() {
        // In this case, we already have lost the audio focus, and we cannot duck.
        // So the player will be paused immediately, but different with the previous state, there is
        // no need to stop the player.
        mPlayer.pause();
        // update playback state
        mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                PlaybackStateCompat.STATE_PAUSED).build());
        // Will record current player progress when lossing the audio focus.
        mCurrentPosition = getCurrentPosition();
    }

    private void audioLossFocusTransientCanDuckHanlder() {
        // In this case, we have lots the audio focus, but since we can duck
        // the music item can continue to play but the volume will be reduced
        mPlayer.setVolume(REDUCED_VOLUME, REDUCED_VOLUME);
    }

    private void audioFocusGainHandler() {
        // In this case the app has been granted audio focus again
        // Firstly, raise volume to normal
        mPlayer.setVolume(FULL_VOLUME, FULL_VOLUME);

        // If the recorded position is the same as current position
        // Start the player directly
        if (mCurrentPosition == mPlayer.getCurrentPosition()) {
            mPlayer.start();
            mMediaSession.setPlaybackState(createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_PLAYING).build());
            // If the recorded position is not equal to current position
            // The player will seek to the last recorded position firstly to continue playing the
            // last music item
        } else {
            mPlayer.seekTo(mCurrentPosition);
            PlaybackStateCompat.Builder builder = createPlaybackStateBuilder(
                    PlaybackStateCompat.STATE_BUFFERING);
            builder.setBufferedPosition(mBufferedProgress);
            mMediaSession.setPlaybackState(builder.build());
        }
    }
}
