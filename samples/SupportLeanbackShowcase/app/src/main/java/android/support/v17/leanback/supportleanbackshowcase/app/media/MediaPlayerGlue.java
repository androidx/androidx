/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.io.IOException;

/**
 * This glue extends the {@link PlaybackControlGlue} with a {@link MediaPlayer} synchronization. It
 * supports 7 actions: <ul> <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction}</li>
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction}</li> <li>{@link
 * android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction}</li> <li>{@link
 * android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction}</li> <li>{@link
 * android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction}</li> <li>{@link
 * android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction}</li> <li>{@link
 * android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction}</li> </ul>
 * <p/>
 */
public abstract class MediaPlayerGlue extends PlaybackControlGlue implements
        OnItemViewSelectedListener {

    public static final int FAST_FORWARD_REWIND_STEP = 10 * 1000; // in milliseconds
    public static final int FAST_FORWARD_REWIND_REPEAT_DELAY = 200; // in milliseconds
    private static final String TAG = "MediaPlayerGlue";
    protected final PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    protected final PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private final Context mContext;
    private final MediaPlayer mPlayer = new MediaPlayer();
    private final PlaybackControlsRow.RepeatAction mRepeatAction;
    private final PlaybackControlsRow.ShuffleAction mShuffleAction;
    private PlaybackControlsRow mControlsRow;
    private Runnable mRunnable;
    private Handler mHandler = new Handler();
    private boolean mInitialized = false; // true when the MediaPlayer is prepared/initialized
    private OnMediaFileFinishedPlayingListener mMediaFileFinishedPlayingListener;
    private Action mSelectedAction; // the action which is currently selected by the user
    private long mLastKeyDownEvent = 0L; // timestamp when the last DPAD_CENTER KEY_DOWN occurred
    private MetaData mMetaData;
    private Uri mMediaSourceUri = null;
    private String mMediaSourcePath = null;

    public MediaPlayerGlue(Context context, PlaybackOverlayFragment fragment) {
        super(context, fragment, new int[]{1});
        mContext = context;

        // Instantiate secondary actions
        mShuffleAction = new PlaybackControlsRow.ShuffleAction(mContext);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(mContext);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(mContext);
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(mContext);
        mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);
        mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);

        // Register selected listener such that we know what action the user currently has focused.
        fragment.setOnItemViewSelectedListener(this);
    }

    /**
     * Will reset the {@link MediaPlayer} and the glue such that a new file can be played. You are
     * not required to call this method before playing the first file. However you have to call it
     * before playing a second one.
     */
    void reset() {
        mInitialized = false;
        mPlayer.reset();
    }

    public void setOnMediaFileFinishedPlayingListener(OnMediaFileFinishedPlayingListener listener) {
        mMediaFileFinishedPlayingListener = listener;
    }

    /**
     * Override this method in case you need to add different secondary actions.
     *
     * @param secondaryActionsAdapter The adapter you need to add the {@link Action}s to.
     */
    protected void addSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        secondaryActionsAdapter.add(mShuffleAction);
        secondaryActionsAdapter.add(mRepeatAction);
        secondaryActionsAdapter.add(mThumbsDownAction);
        secondaryActionsAdapter.add(mThumbsUpAction);
    }

    /**
     * @see MediaPlayer#setDisplay(SurfaceHolder)
     */
    public void setDisplay(SurfaceHolder surfaceHolder) {
        mPlayer.setDisplay(surfaceHolder);
    }

    /**
     * Use this method to setup the {@link PlaybackControlsRowPresenter}. It'll be called
     * <u>after</u> the {@link PlaybackControlsRowPresenter} has been created and the primary and
     * secondary actions have been added.
     *
     * @param presenter The PlaybackControlsRowPresenter used to display the controls.
     */
    public void setupControlsRowPresenter(PlaybackControlsRowPresenter presenter) {
        // TODO: hahnr@ move into resources
        presenter.setProgressColor(getContext().getResources().getColor(
                R.color.player_progress_color));
        presenter.setBackgroundColor(getContext().getResources().getColor(
                R.color.player_background_color));
    }

    @Override public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRowPresenter presenter = super.createControlsRowAndPresenter();
        mControlsRow = getControlsRow();

        // Add secondary actions and change the control row color.
        ArrayObjectAdapter secondaryActions = new ArrayObjectAdapter(
                new ControlButtonPresenterSelector());
        mControlsRow.setSecondaryActionsAdapter(secondaryActions);
        addSecondaryActions(secondaryActions);
        setupControlsRowPresenter(presenter);
        return presenter;
    }

    @Override public void enableProgressUpdating(final boolean enabled) {
        if (!enabled) {
            if (mRunnable != null) mHandler.removeCallbacks(mRunnable);
            return;
        }
        mRunnable = new Runnable() {
            @Override public void run() {
                updateProgress();
                Log.d(TAG, "enableProgressUpdating(boolean)");
                mHandler.postDelayed(this, getUpdatePeriod());
            }
        };
        mHandler.postDelayed(mRunnable, getUpdatePeriod());
    }

    @Override public void onActionClicked(Action action) {
        // If either 'Shuffle' or 'Repeat' has been clicked we need to make sure the acitons index
        // is incremented and the UI updated such that we can display the new state.
        super.onActionClicked(action);
        if (action instanceof PlaybackControlsRow.ShuffleAction) {
            mShuffleAction.nextIndex();
        } else if (action instanceof PlaybackControlsRow.RepeatAction) {
            mRepeatAction.nextIndex();
        } else if (action instanceof PlaybackControlsRow.ThumbsUpAction) {
            if (mThumbsUpAction.getIndex() == PlaybackControlsRow.ThumbsAction.SOLID) {
                mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);
            } else {
                mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsAction.SOLID);
                mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);
            }
        } else if (action instanceof PlaybackControlsRow.ThumbsDownAction) {
            if (mThumbsDownAction.getIndex() == PlaybackControlsRow.ThumbsAction.SOLID) {
                mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);
            } else {
                mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsAction.SOLID);
                mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsAction.OUTLINE);
            }
        }
        onMetadataChanged();
    }

    @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
        // This method is overridden in order to make implement fast forwarding and rewinding when
        // the user keeps the corresponding action pressed.
        // We only consume DPAD_CENTER Action_DOWN events on the Fast-Forward and Rewind action and
        // only if it has not been pressed in the last X milliseconds.
        boolean consume = mSelectedAction instanceof PlaybackControlsRow.RewindAction;
        consume = consume || mSelectedAction instanceof PlaybackControlsRow.FastForwardAction;
        consume = consume && mInitialized;
        consume = consume && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER;
        consume = consume && event.getAction() == KeyEvent.ACTION_DOWN;
        consume = consume && System
                .currentTimeMillis() - mLastKeyDownEvent > FAST_FORWARD_REWIND_REPEAT_DELAY;
        if (consume) {
            mLastKeyDownEvent = System.currentTimeMillis();
            int newPosition = getCurrentPosition() + FAST_FORWARD_REWIND_STEP;
            if (mSelectedAction instanceof PlaybackControlsRow.RewindAction) {
                newPosition = getCurrentPosition() - FAST_FORWARD_REWIND_STEP;
            }
            // Make sure the new calculated duration is in the range 0 >= X >= MediaDuration
            if (newPosition < 0) newPosition = 0;
            if (newPosition > getMediaDuration()) newPosition = getMediaDuration();
            seekTo(newPosition);
            return true;
        }
        return super.onKey(v, keyCode, event);
    }

    @Override public boolean hasValidMedia() {
        return mMetaData != null;
    }

    @Override public boolean isMediaPlaying() {
        return mPlayer.isPlaying();
    }

    @Override public CharSequence getMediaTitle() {
        return hasValidMedia() ? mMetaData.getTitle() : "N/a";
    }

    @Override public CharSequence getMediaSubtitle() {
        return hasValidMedia() ? mMetaData.getArtist() : "N/a";
    }

    @Override public int getMediaDuration() {
        return mInitialized ? mPlayer.getDuration() : 0;
    }

    @Override public Drawable getMediaArt() {
        return hasValidMedia() ? mMetaData.getCover() : null;
    }

    @Override public long getSupportedActions() {
        return PlaybackControlGlue.ACTION_PLAY_PAUSE | PlaybackControlGlue.ACTION_FAST_FORWARD | PlaybackControlGlue.ACTION_REWIND;
    }

    @Override public int getCurrentSpeedId() {
        // 0 = Pause, 1 = Normal Playback Speed
        return mPlayer.isPlaying() ? 1 : 0;
    }

    @Override public int getCurrentPosition() {
        return mInitialized ? mPlayer.getCurrentPosition() : 0;
    }

    @Override protected void startPlayback(int speed) throws IllegalStateException {
        mPlayer.start();
    }

    @Override protected void pausePlayback() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    @Override protected void skipToNext() {
        // Not supported.
    }

    @Override protected void skipToPrevious() {
        // Not supported.
    }

    /**
     * Called whenever the user presses fast-forward/rewind or when the user keeps the corresponding
     * action pressed.
     *
     * @param newPosition The new position of the media track in milliseconds.
     */
    protected void seekTo(int newPosition) {
        mPlayer.seekTo(newPosition);
    }

    /**
     * Sets the media source of the player witha given URI.
     * @see MediaPlayer#setDataSource(String)
     * @return Returns <code>true</code> if uri represents a new media; <code>false</code>
     * otherwise.
     */
    public boolean setMediaSource(Uri uri) {
        if (mMediaSourceUri != null && mMediaSourceUri.equals(uri)) {
            return false;
        }
        mMediaSourceUri = uri;
        return true;
    }

    /**
     * Sets the media source of the player with a String path URL.
     * @see MediaPlayer#setDataSource(String)
     * @return Returns <code>true</code> if path represents a new media; <code>false</code>
     * otherwise.
     */
    public boolean setMediaSource(String path) {
        if (mMediaSourcePath != null && mMediaSourcePath.equals(mMediaSourcePath)) {
            return false;
        }
        mMediaSourcePath = path;
        return true;
    }

    public void prepareMediaForPlaying() {
        reset();
        try {
            if (mMediaSourceUri != null) mPlayer.setDataSource(getContext(), mMediaSourceUri);
            else mPlayer.setDataSource(mMediaSourcePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override public void onPrepared(MediaPlayer mp) {
                mInitialized = true;
                mPlayer.start();
                onMetadataChanged();
                onStateChanged();
                updateProgress();
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override public void onCompletion(MediaPlayer mp) {
                if (mInitialized && mMediaFileFinishedPlayingListener != null)
                    mMediaFileFinishedPlayingListener.onMediaFileFinishedPlaying(mMetaData);
            }
        });
        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mControlsRow.setBufferedProgress((int) (mp.getDuration() * (percent / 100f)));
            }
        });
        mPlayer.prepareAsync();
        onStateChanged();
    }

    /**
     * Call to <code>startPlayback(1)</code>.
     *
     * @throws IllegalStateException See {@link MediaPlayer} for further information about it's
     * different states when setting a data source and preparing it to be played.
     */
    public void startPlayback() throws IllegalStateException {
        startPlayback(1);
    }

    /**
     * @return Returns <code>true</code> iff 'Shuffle' is <code>ON</code>.
     */
    public boolean useShuffle() {
        return mShuffleAction.getIndex() == PlaybackControlsRow.ShuffleAction.ON;
    }

    /**
     * @return Returns <code>true</code> iff 'Repeat-One' is <code>ON</code>.
     */
    public boolean repeatOne() {
        return mRepeatAction.getIndex() == PlaybackControlsRow.RepeatAction.ONE;
    }

    /**
     * @return Returns <code>true</code> iff 'Repeat-All' is <code>ON</code>.
     */
    public boolean repeatAll() {
        return mRepeatAction.getIndex() == PlaybackControlsRow.RepeatAction.ALL;
    }

    public void setMetaData(MetaData metaData) {
        mMetaData = metaData;
        onMetadataChanged();
    }

    /**
     * This is a listener implementation for the {@link OnItemViewSelectedListener} of the {@link
     * PlaybackOverlayFragment}. This implementation is required in order to detect KEY_DOWN events
     * on the {@link android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction} and
     * {@link android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction}. Thus you should
     * <u>NOT</u> set another {@link OnItemViewSelectedListener} on your {@link
     * PlaybackOverlayFragment}. Instead, override this method and call its super (this)
     * implementation.
     *
     * @see OnItemViewSelectedListener#onItemSelected(Presenter.ViewHolder, Object,
     * RowPresenter.ViewHolder, Row)
     */
    @Override public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                         RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof Action) {
            mSelectedAction = (Action) item;
        } else {
            mSelectedAction = null;
        }
    }

    /**
     * A listener which will be called whenever a track is finished playing.
     */
    public interface OnMediaFileFinishedPlayingListener {

        /**
         * Called when a track is finished playing.
         *
         * @param metaData The track's {@link MetaData} which just finished playing.
         */
        void onMediaFileFinishedPlaying(MetaData metaData);

    }

    /**
     * Holds the meta data such as track title, artist and cover art. It'll be used by the {@link
     * MediaPlayerGlue}.
     */
    public static class MetaData {

        private String mTitle;
        private String mArtist;
        private Drawable mCover;

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            this.mTitle = title;
        }

        public String getArtist() {
            return mArtist;
        }

        public void setArtist(String artist) {
            this.mArtist = artist;
        }

        public Drawable getCover() {
            return mCover;
        }

        public void setCover(Drawable cover) {
            this.mCover = cover;
        }

    }

}
