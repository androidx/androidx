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

package android.support.v17.leanback.media;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.PlaybackSeekDataProvider;
import android.support.v17.leanback.widget.PlaybackSeekUi;
import android.support.v17.leanback.widget.PlaybackTransportRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A helper class for managing a {@link PlaybackControlsRow} being displayed in
 * {@link PlaybackGlueHost}, it supports standard playback control actions play/pause, and
 * skip next/previous. This helper class is a glue layer in that manages interaction between the
 * leanback UI components {@link PlaybackControlsRow} {@link PlaybackTransportRowPresenter}
 * and a functional {@link PlayerAdapter} which represents the underlying
 * media player.
 *
 * <p>App must pass a {@link PlayerAdapter} in constructor for a specific
 * implementation e.g. a {@link MediaPlayerAdapter}.
 * </p>
 *
 * <p>The glue has two actions bar: primary actions bar and secondary actions bar. App
 * can provide additional actions by overriding {@link #onCreatePrimaryActions} and / or
 * {@link #onCreateSecondaryActions} and respond to actions by override
 * {@link #onActionClicked(Action)}.
 * </p>
 *
 * <p> It's also subclass's responsibility to implement the "repeat mode" in
 * {@link #onPlayCompleted()}.
 * </p>
 *
 * <p>
 * Apps calls {@link #setSeekProvider(PlaybackSeekDataProvider)} to provide seek data. If the
 * {@link PlaybackGlueHost} is instance of {@link PlaybackSeekUi}, the provider will be passed to
 * PlaybackGlueHost to render thumb bitmaps.
 * </p>
 * Sample Code:
 * <pre><code>
 * public class MyVideoFragment extends VideoFragment {
 *     &#64;Override
 *     public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         final PlaybackTransportControlGlue<MediaPlayerAdapter> playerGlue =
 *                 new PlaybackTransportControlGlue(getActivity(),
 *                         new MediaPlayerAdapter(getActivity()));
 *         playerGlue.setHost(new VideoFragmentGlueHost(this));
 *         playerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
 *             &#64;Override
 *             public void onPreparedStateChanged(PlaybackGlue glue) {
 *                 if (glue.isPrepared()) {
 *                     playerGlue.setSeekProvider(new MySeekProvider());
 *                     playerGlue.play();
 *                 }
 *             }
 *         });
 *         playerGlue.setSubtitle("Leanback artist");
 *         playerGlue.setTitle("Leanback team at work");
 *         String uriPath = "android.resource://com.example.android.leanback/raw/video";
 *         playerGlue.getPlayerAdapter().setDataSource(Uri.parse(uriPath));
 *     }
 * }
 * </code></pre>
 * @param <T> Type of {@link PlayerAdapter} passed in constructor.
 */
public class PlaybackTransportControlGlue<T extends PlayerAdapter> extends PlaybackGlue
        implements OnActionClickedListener, View.OnKeyListener {

    static final String TAG = "PlaybackTransportGlue";
    static final boolean DEBUG = false;

    static final int MSG_UPDATE_PLAYBACK_STATE = 100;
    private static final int UPDATE_PLAYBACK_STATE_DELAY_MS = 2000;

    final T mPlayerAdapter;
    PlaybackControlsRow mControlsRow;
    PlaybackRowPresenter mControlsRowPresenter;
    PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    boolean mIsPlaying = true;
    boolean mFadeWhenPlaying = true;

    CharSequence mSubtitle;
    CharSequence mTitle;
    Drawable mCover;

    PlaybackSeekDataProvider mSeekProvider;
    boolean mSeekEnabled;
    PlaybackGlueHost.PlayerCallback mPlayerCallback;
    boolean mBuffering = false;
    int mVideoWidth = 0;
    int mVideoHeight = 0;
    boolean mErrorSet = false;
    int mErrorCode;
    String mErrorMessage;

    static class UpdatePlaybackStateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_PLAYBACK_STATE) {
                PlaybackTransportControlGlue glue =
                        ((WeakReference<PlaybackTransportControlGlue>) msg.obj).get();
                if (glue != null) {
                    glue.updatePlaybackState();
                }
            }
        }
    }

    static final Handler sHandler = new UpdatePlaybackStateHandler();

    final WeakReference<PlaybackTransportControlGlue> mGlueWeakReference =  new WeakReference(this);

    final PlayerAdapter.Callback mAdapterCallback = new PlayerAdapter
            .Callback() {

        @Override
        public void onPlayStateChanged(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onPlayStateChanged");
            PlaybackTransportControlGlue.this.onPlayStateChanged();
        }

        @Override
        public void onCurrentPositionChanged(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onCurrentPositionChanged");
            PlaybackTransportControlGlue.this.onUpdateProgress();
        }

        @Override
        public void onBufferedPositionChanged(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onBufferedPositionChanged");
            PlaybackTransportControlGlue.this.onUpdateBufferedProgress();
        }

        @Override
        public void onDurationChanged(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onDurationChanged");
            PlaybackTransportControlGlue.this.onUpdateDuration();
        }

        @Override
        public void onPlayCompleted(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onPlayCompleted");
            PlaybackTransportControlGlue.this.onPlayCompleted();
        }

        @Override
        public void onPreparedStateChanged(PlayerAdapter wrapper) {
            if (DEBUG) Log.v(TAG, "onPreparedStateChanged");
            PlaybackTransportControlGlue.this.onPreparedStateChanged();
        }

        @Override
        public void onVideoSizeChanged(PlayerAdapter wrapper, int width, int height) {
            mVideoWidth = width;
            mVideoHeight = height;
            if (mPlayerCallback != null) {
                mPlayerCallback.onVideoSizeChanged(width, height);
            }
        }

        @Override
        public void onError(PlayerAdapter wrapper, int errorCode, String errorMessage) {
            mErrorSet = true;
            mErrorCode = errorCode;
            mErrorMessage = errorMessage;
            if (mPlayerCallback != null) {
                mPlayerCallback.onError(errorCode, errorMessage);
            }
        }

        @Override
        public void onBufferingStateChanged(PlayerAdapter wrapper, boolean start) {
            mBuffering = start;
            if (mPlayerCallback != null) {
                mPlayerCallback.onBufferingStateChanged(start);
            }
        }
    };

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param impl Implementation to underlying media player.
     */
    public PlaybackTransportControlGlue(Context context, T impl) {
        super(context);
        mPlayerAdapter = impl;
        mPlayerAdapter.setCallback(mAdapterCallback);
    }

    public final T getPlayerAdapter() {
        return mPlayerAdapter;
    }

    @Override
    protected void onAttachedToHost(PlaybackGlueHost host) {
        super.onAttachedToHost(host);
        host.setOnKeyInterceptListener(this);
        host.setOnActionClickedListener(this);
        onCreateDefaultControlsRow();
        onCreateDefaultRowPresenter();
        host.setPlaybackRowPresenter(getPlaybackRowPresenter());
        host.setPlaybackRow(getControlsRow());
        if (host instanceof PlaybackSeekUi) {
            ((PlaybackSeekUi) host).setPlaybackSeekUiClient(mPlaybackSeekUiClient);
        }
        mPlayerCallback = host.getPlayerCallback();
        onAttachHostCallback();
        mPlayerAdapter.onAttachedToHost(host);
    }

    void onAttachHostCallback() {
        if (mPlayerCallback != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                mPlayerCallback.onVideoSizeChanged(mVideoWidth, mVideoHeight);
            }
            if (mErrorSet) {
                mPlayerCallback.onError(mErrorCode, mErrorMessage);
            }
            mPlayerCallback.onBufferingStateChanged(mBuffering);
        }
    }

    void onDetachHostCallback() {
        mErrorSet = false;
        mErrorCode = 0;
        mErrorMessage = null;
        if (mPlayerCallback != null) {
            mPlayerCallback.onBufferingStateChanged(false);
        }
    }

    @Override
    protected void onHostStart() {
        mPlayerAdapter.setProgressUpdatingEnabled(true);
    }

    @Override
    protected void onHostStop() {
        mPlayerAdapter.setProgressUpdatingEnabled(false);
    }

    @Override
    protected void onDetachedFromHost() {
        if (getHost() instanceof PlaybackSeekUi) {
            ((PlaybackSeekUi) getHost()).setPlaybackSeekUiClient(null);
        }
        onDetachHostCallback();
        mPlayerCallback = null;
        mPlayerAdapter.onDetachedFromHost();
        mPlayerAdapter.setProgressUpdatingEnabled(false);
        super.onDetachedFromHost();
    }

    void onCreateDefaultControlsRow() {
        if (mControlsRow == null) {
            PlaybackControlsRow controlsRow = new PlaybackControlsRow(this);
            setControlsRow(controlsRow);
        }
    }

    void onCreateDefaultRowPresenter() {
        if (mControlsRowPresenter == null) {
            final AbstractDetailsDescriptionPresenter detailsPresenter =
                    new AbstractDetailsDescriptionPresenter() {
                        @Override
                        protected void onBindDescription(ViewHolder
                                viewHolder, Object obj) {
                            PlaybackTransportControlGlue glue = (PlaybackTransportControlGlue) obj;
                            viewHolder.getTitle().setText(glue.getTitle());
                            viewHolder.getSubtitle().setText(glue.getSubtitle());
                        }
                    };

            PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
                @Override
                protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                    super.onBindRowViewHolder(vh, item);
                    vh.setOnKeyListener(PlaybackTransportControlGlue.this);
                }
                @Override
                protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                    super.onUnbindRowViewHolder(vh);
                    vh.setOnKeyListener(null);
                }
            };
            rowPresenter.setDescriptionPresenter(detailsPresenter);
            setPlaybackRowPresenter(rowPresenter);
        }
    }

    /**
     * Sets the controls to auto hide after a timeout when media is playing.
     * @param enable True to enable auto hide after a timeout when media is playing.
     * @see PlaybackGlueHost#setControlsOverlayAutoHideEnabled(boolean)
     */
    public void setControlsOverlayAutoHideEnabled(boolean enable) {
        mFadeWhenPlaying = enable;
        if (!mFadeWhenPlaying && getHost() != null) {
            getHost().setControlsOverlayAutoHideEnabled(false);
        }
    }

    /**
     * Returns true if the controls auto hides after a timeout when media is playing.
     * @return true if the controls auto hides after a timeout when media is playing.
     * @see PlaybackGlueHost#isControlsOverlayAutoHideEnabled()
     */
    public boolean isControlsOverlayAutoHideEnabled() {
        return mFadeWhenPlaying;
    }

    /**
     * Sets the controls row to be managed by the glue layer. If
     * {@link PlaybackControlsRow#getPrimaryActionsAdapter()} is not provided, a default
     * {@link ArrayObjectAdapter} will be created and initialized in
     * {@link #onCreatePrimaryActions(ArrayObjectAdapter)}. If
     * {@link PlaybackControlsRow#getSecondaryActionsAdapter()} is not provided, a default
     * {@link ArrayObjectAdapter} will be created and initialized in
     * {@link #onCreateSecondaryActions(ArrayObjectAdapter)}.
     * The primary actions and playback state related aspects of the row
     * are updated by the glue.
     */
    public void setControlsRow(PlaybackControlsRow controlsRow) {
        mControlsRow = controlsRow;
        mControlsRow.setCurrentPosition(-1);
        mControlsRow.setDuration(-1);
        mControlsRow.setBufferedPosition(-1);
        if (mControlsRow.getPrimaryActionsAdapter() == null) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(
                    new ControlButtonPresenterSelector());
            onCreatePrimaryActions(adapter);
            mControlsRow.setPrimaryActionsAdapter(adapter);
        }
        // Add secondary actions
        if (mControlsRow.getSecondaryActionsAdapter() == null) {
            ArrayObjectAdapter secondaryActions = new ArrayObjectAdapter(
                    new ControlButtonPresenterSelector());
            onCreateSecondaryActions(secondaryActions);
            getControlsRow().setSecondaryActionsAdapter(secondaryActions);
        }
        updateControlsRow();
    }

    /**
     * Sets the controls row Presenter to be managed by the glue layer.
     */
    public void setPlaybackRowPresenter(PlaybackRowPresenter presenter) {
        mControlsRowPresenter = presenter;
    }

    /**
     * Returns the playback controls row managed by the glue layer.
     */
    public PlaybackControlsRow getControlsRow() {
        return mControlsRow;
    }

    /**
     * Returns the playback controls row Presenter managed by the glue layer.
     */
    public PlaybackRowPresenter getPlaybackRowPresenter() {
        return mControlsRowPresenter;
    }

    /**
     * Handles action clicks.  A subclass may override this add support for additional actions.
     */
    @Override
    public void onActionClicked(Action action) {
        dispatchAction(action, null);
    }

    /**
     * Handles key events and returns true if handled.  A subclass may override this to provide
     * additional support.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                return false;
        }
        final ObjectAdapter primaryActionsAdapter = mControlsRow.getPrimaryActionsAdapter();
        Action action = mControlsRow.getActionForKeyCode(primaryActionsAdapter, keyCode);
        if (action == null) {
            action = mControlsRow.getActionForKeyCode(mControlsRow.getSecondaryActionsAdapter(),
                    keyCode);
        }

        if (action != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                dispatchAction(action, event);
            }
            return true;
        }
        return false;
    }

    /**
     * Called when the given action is invoked, either by click or keyevent.
     */
    boolean dispatchAction(Action action, KeyEvent keyEvent) {
        boolean handled = false;
        if (action instanceof PlaybackControlsRow.PlayPauseAction) {
            boolean canPlay = keyEvent == null
                    || keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY;
            boolean canPause = keyEvent == null
                    || keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE;
            //            PLAY_PAUSE    PLAY      PAUSE
            // playing    paused                  paused
            // paused     playing       playing
            // ff/rw      playing       playing   paused
            if (canPause
                    && (canPlay ? mIsPlaying :
                        !mIsPlaying)) {
                mIsPlaying = false;
                pause();
            } else if (canPlay && !mIsPlaying) {
                mIsPlaying = true;
                play();
            }
            updatePlaybackStatusAfterUserAction();
            handled = true;
        } else if (action instanceof PlaybackControlsRow.SkipNextAction) {
            next();
            handled = true;
        } else if (action instanceof PlaybackControlsRow.SkipPreviousAction) {
            previous();
            handled = true;
        }
        return handled;
    }

    private void updateControlsRow() {
        onMetadataChanged();
        sHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE, mGlueWeakReference);
        updatePlaybackState();
    }

    private void updatePlaybackStatusAfterUserAction() {
        updatePlaybackState(mIsPlaying);
        // Sync playback state after a delay
        sHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE, mGlueWeakReference);
        sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_UPDATE_PLAYBACK_STATE,
                mGlueWeakReference), UPDATE_PLAYBACK_STATE_DELAY_MS);
    }

    @Override
    public final boolean isPlaying() {
        return mPlayerAdapter.isPlaying();
    }

    @Override
    public final void play() {
        mPlayerAdapter.play();
    }

    @Override
    public void pause() {
        mPlayerAdapter.pause();
    }

    private void updatePlaybackState(boolean isPlaying) {
        if (mControlsRow == null) {
            return;
        }

        if (!isPlaying) {
            onUpdateProgress();
            mPlayerAdapter.setProgressUpdatingEnabled(mPlaybackSeekUiClient.mIsSeek);
        } else {
            mPlayerAdapter.setProgressUpdatingEnabled(true);
        }

        if (mFadeWhenPlaying && getHost() != null) {
            getHost().setControlsOverlayAutoHideEnabled(isPlaying);
        }

        if (mPlayPauseAction != null) {
            int index = !isPlaying
                    ? PlaybackControlsRow.PlayPauseAction.INDEX_PLAY
                    : PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE;
            if (mPlayPauseAction.getIndex() != index) {
                mPlayPauseAction.setIndex(index);
                notifyItemChanged((ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter(),
                        mPlayPauseAction);
            }
        }
    }

    private static void notifyItemChanged(ArrayObjectAdapter adapter, Object object) {
        int index = adapter.indexOf(object);
        if (index >= 0) {
            adapter.notifyArrayItemRangeChanged(index, 1);
        }
    }

    /**
     * May be overridden to add primary actions to the adapter. Default implementation add
     * {@link PlaybackControlsRow.PlayPauseAction}.
     *
     * @param primaryActionsAdapter The adapter to add primary {@link Action}s.
     */
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        primaryActionsAdapter.add(mPlayPauseAction =
                new PlaybackControlsRow.PlayPauseAction(getContext()));
    }

    /**
     * May be overridden to add secondary actions to the adapter.
     *
     * @param secondaryActionsAdapter The adapter you need to add the {@link Action}s to.
     */
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
    }

    void onUpdateProgress() {
        if (mControlsRow != null && !mPlaybackSeekUiClient.mIsSeek) {
            mControlsRow.setCurrentPosition(mPlayerAdapter.isPrepared()
                    ? mPlayerAdapter.getCurrentPosition() : -1);
        }
    }

    void onUpdateBufferedProgress() {
        if (mControlsRow != null) {
            mControlsRow.setBufferedPosition(mPlayerAdapter.getBufferedPosition());
        }
    }

    void onUpdateDuration() {
        if (mControlsRow != null) {
            mControlsRow.setDuration(
                    mPlayerAdapter.isPrepared() ? mPlayerAdapter.getDuration() : -1);
        }
    }

    /**
     * @return The duration of the media item in milliseconds.
     */
    public final long getDuration() {
        return mPlayerAdapter.getDuration();
    }

    /**
     * @return The current position of the media item in milliseconds.
     */
    public final long getCurrentPosition() {
        return mPlayerAdapter.getCurrentPosition();
    }

    /**
     * @return The current buffered position of the media item in milliseconds.
     */
    public final long getBufferedPosition() {
        return mPlayerAdapter.getBufferedPosition();
    }

    @Override
    public final boolean isPrepared() {
        return mPlayerAdapter.isPrepared();
    }

    /**
     * Event when ready state for play changes.
     */
    @CallSuper
    protected void onPreparedStateChanged() {
        onUpdateDuration();
        List<PlayerCallback> callbacks = getPlayerCallbacks();
        if (callbacks != null) {
            for (int i = 0, size = callbacks.size(); i < size; i++) {
                callbacks.get(i).onPreparedStateChanged(this);
            }
        }
    }

    /**
     * Sets the drawable representing cover image. The drawable will be rendered by default
     * description presenter in
     * {@link PlaybackTransportRowPresenter#setDescriptionPresenter(Presenter)}.
     * @param cover The drawable representing cover image.
     */
    public void setArt(Drawable cover) {
        if (mCover == cover) {
            return;
        }
        this.mCover = cover;
        mControlsRow.setImageDrawable(mCover);
        if (getHost() != null) {
            getHost().notifyPlaybackRowChanged();
        }
    }

    /**
     * @return The drawable representing cover image.
     */
    public Drawable getArt() {
        return mCover;
    }

    /**
     * Sets the media subtitle. The subtitle will be rendered by default description presenter
     * {@link PlaybackTransportRowPresenter#setDescriptionPresenter(Presenter)}.
     * @param subtitle Subtitle to set.
     */
    public void setSubtitle(CharSequence subtitle) {
        if (subtitle == null ? mSubtitle == null : subtitle.equals(mSubtitle)) {
            return;
        }
        mSubtitle = subtitle;
        if (getHost() != null) {
            getHost().notifyPlaybackRowChanged();
        }
    }

    /**
     * Return The media subtitle.
     */
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /**
     * Sets the media title. The title will be rendered by default description presenter
     * {@link PlaybackTransportRowPresenter#setDescriptionPresenter(Presenter)}.
     */
    public void setTitle(CharSequence title) {
        if (title == null ? mTitle == null : title.equals(mTitle)) {
            return;
        }
        mTitle = title;
        if (getHost() != null) {
            getHost().notifyPlaybackRowChanged();
        }
    }

    /**
     * Returns the title of the media item.
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Event when metadata changed
     */
    void onMetadataChanged() {
        if (mControlsRow == null) {
            return;
        }

        if (DEBUG) Log.v(TAG, "updateRowMetadata");

        mControlsRow.setImageDrawable(getArt());
        mControlsRow.setDuration(mPlayerAdapter.getDuration());
        mControlsRow.setCurrentPosition(mPlayerAdapter.getCurrentPosition());

        if (getHost() != null) {
            getHost().notifyPlaybackRowChanged();
        }
    }

    void updatePlaybackState() {
        mIsPlaying = mPlayerAdapter.isPlaying();
        updatePlaybackState(mIsPlaying);
    }

    /**
     * Event when play state changed.
     */
    @CallSuper
    protected void onPlayStateChanged() {
        if (sHandler.hasMessages(MSG_UPDATE_PLAYBACK_STATE, mGlueWeakReference)) {
            sHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE, mGlueWeakReference);
            if (mPlayerAdapter.isPlaying() != mIsPlaying) {
                if (DEBUG) Log.v(TAG, "Status expectation mismatch, delaying update");
                sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_UPDATE_PLAYBACK_STATE,
                        mGlueWeakReference), UPDATE_PLAYBACK_STATE_DELAY_MS);
            } else {
                if (DEBUG) Log.v(TAG, "Update state matches expectation");
                updatePlaybackState();
            }
        } else {
            updatePlaybackState();
        }
        List<PlayerCallback> callbacks = getPlayerCallbacks();
        if (callbacks != null) {
            for (int i = 0, size = callbacks.size(); i < size; i++) {
                callbacks.get(i).onPlayStateChanged(this);
            }
        }
    }

    /**
     * Event when play finishes, subclass may handling repeat mode here.
     */
    @CallSuper
    protected void onPlayCompleted() {
        List<PlayerCallback> callbacks = getPlayerCallbacks();
        if (callbacks != null) {
            for (int i = 0, size = callbacks.size(); i < size; i++) {
                callbacks.get(i).onPlayCompleted(this);
            }
        }
    }

    final SeekUiClient mPlaybackSeekUiClient = new SeekUiClient();

    class SeekUiClient extends PlaybackSeekUi.Client {
        boolean mPausedBeforeSeek;
        long mPositionBeforeSeek;
        long mLastUserPosition;
        boolean mIsSeek;

        @Override
        public PlaybackSeekDataProvider getPlaybackSeekDataProvider() {
            return mSeekProvider;
        }

        @Override
        public boolean isSeekEnabled() {
            return mSeekProvider != null || mSeekEnabled;
        }

        @Override
        public void onSeekStarted() {
            mIsSeek = true;
            mPausedBeforeSeek = !isPlaying();
            mPlayerAdapter.setProgressUpdatingEnabled(true);
            // if we seek thumbnails, we don't need save original position because current
            // position is not changed during seeking.
            // otherwise we will call seekTo() and may need to restore the original position.
            mPositionBeforeSeek = mSeekProvider == null ? mPlayerAdapter.getCurrentPosition() : -1;
            mLastUserPosition = -1;
            pause();
        }

        @Override
        public void onSeekPositionChanged(long pos) {
            if (mSeekProvider == null) {
                mPlayerAdapter.seekTo(pos);
            } else {
                mLastUserPosition = pos;
            }
            if (mControlsRow != null) {
                mControlsRow.setCurrentPosition(pos);
            }
        }

        @Override
        public void onSeekFinished(boolean cancelled) {
            if (!cancelled) {
                if (mLastUserPosition > 0) {
                    seekTo(mLastUserPosition);
                }
            } else {
                if (mPositionBeforeSeek >= 0) {
                    seekTo(mPositionBeforeSeek);
                }
            }
            mIsSeek = false;
            if (!mPausedBeforeSeek) {
                play();
            } else {
                mPlayerAdapter.setProgressUpdatingEnabled(false);
                // we neeed update UI since PlaybackControlRow still saves previous position.
                onUpdateProgress();
            }
        }
    };

    /**
     * Set seek data provider used during user seeking.
     * @param seekProvider Seek data provider used during user seeking.
     */
    public final void setSeekProvider(PlaybackSeekDataProvider seekProvider) {
        mSeekProvider = seekProvider;
    }

    /**
     * Get seek data provider used during user seeking.
     * @return Seek data provider used during user seeking.
     */
    public final PlaybackSeekDataProvider getSeekProvider() {
        return mSeekProvider;
    }

    /**
     * Enable or disable seek when {@link #getSeekProvider()} is null. When true,
     * {@link PlayerAdapter#seekTo(long)} will be called during user seeking.
     *
     * @param seekEnabled True to enable seek, false otherwise
     */
    public final void setSeekEnabled(boolean seekEnabled) {
        mSeekEnabled = seekEnabled;
    }

    /**
     * @return True if seek is enabled without {@link PlaybackSeekDataProvider}, false otherwise.
     */
    public final boolean isSeekEnabled() {
        return mSeekEnabled;
    }

    /**
     * Seek media to a new position.
     * @param position New position.
     */
    public final void seekTo(long position) {
        mPlayerAdapter.seekTo(position);
    }

}
