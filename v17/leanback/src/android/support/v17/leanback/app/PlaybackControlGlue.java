/*
 * Copyright (C) 2016 The Android Open Source Project
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
 */
package android.support.v17.leanback.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;

/**
 * A helper class for managing a {@link android.support.v17.leanback.widget.PlaybackControlsRow}
 * and {@link android.support.v17.leanback.app.PlaybackGlue.PlaybackGlueHost} that implements a
 * recommended approach to handling standard playback control actions such as play/pause,
 * fast forward/rewind at progressive speed levels, and skip to next/previous. This helper class
 * is a glue layer in that it manages the configuration of and interaction between the
 * leanback UI components by defining a functional interface to the media player.
 *
 * <p>You can instantiate a concrete subclass such as {@link MediaControllerGlue} or you must
 * subclass this abstract helper.  To create a subclass you must implement all of the
 * abstract methods and the subclass must invoke {@link #onMetadataChanged()} and
 * {@link #onStateChanged()} appropriately.
 * </p>
 *
 * <p>To use an instance of the glue layer, first construct an instance.  Constructor parameters
 * inform the glue what speed levels are supported for fast forward/rewind. Providing a
 * {@link android.support.v17.leanback.app.PlaybackGlue.PlaybackGlueHost} is optional.
 * </p>
 *
 * <p>If you have your own controls row you must pass it to {@link #setControlsRow}.
 * The row will be updated by the glue layer based on the media metadata and playback state.
 * Alternatively, you may call {@link #createControlsRowAndPresenter()} which will set a controls
 * row and return a row presenter you can use to present the row.
 * </p>
 *
 * <p>The helper sets a {@link android.support.v17.leanback.widget.SparseArrayObjectAdapter}
 * on the controls row as the primary actions adapter, and adds actions to it. You can provide
 * additional actions by overriding {@link #createPrimaryActionsAdapter}.  This helper does not
 * deal in secondary actions so those you may add separately.
 * </p>
 *
 * <p>Provide a click listener on your fragment and if an action is clicked, call
 * {@link #onActionClicked}.  There is no need to call {@link #setOnItemViewClickedListener}
 * but if you do a click listener will be installed on the fragment and recognized action clicks
 * will be handled.  Your listener will be called only for unhandled actions.
 * </p>
 *
 * <p>This helper implements a key event handler. If you pass a
 * {@link android.support.v17.leanback.app.PlaybackGlue.PlaybackGlueHost}, it will configure it's
 * fragment to intercept all key events.  Otherwise, you should set the glue object as key event
 * handler to the ViewHolder when bound by your row presenter; see
 * {@link RowPresenter.ViewHolder#setOnKeyListener(android.view.View.OnKeyListener)}.
 * </p>
 *
 * <p>To update the controls row progress during playback, override {@link #enableProgressUpdating}
 * to manage the lifecycle of a periodic callback to {@link #updateProgress()}.
 * {@link #getUpdatePeriod()} provides a recommended update period.
 * </p>
 *
 */
public abstract class PlaybackControlGlue extends PlaybackGlue
        implements OnActionClickedListener, View.OnKeyListener {
    /**
     * The adapter key for the first custom control on the left side
     * of the predefined primary controls.
     */
    public static final int ACTION_CUSTOM_LEFT_FIRST = 0x1;

    /**
     * The adapter key for the skip to previous control.
     */
    public static final int ACTION_SKIP_TO_PREVIOUS = 0x10;

    /**
     * The adapter key for the rewind control.
     */
    public static final int ACTION_REWIND = 0x20;

    /**
     * The adapter key for the play/pause control.
     */
    public static final int ACTION_PLAY_PAUSE = 0x40;

    /**
     * The adapter key for the fast forward control.
     */
    public static final int ACTION_FAST_FORWARD = 0x80;

    /**
     * The adapter key for the skip to next control.
     */
    public static final int ACTION_SKIP_TO_NEXT = 0x100;

    /**
     * The adapter key for the first custom control on the right side
     * of the predefined primary controls.
     */
    public static final int ACTION_CUSTOM_RIGHT_FIRST = 0x1000;

    /**
     * Invalid playback speed.
     */
    public static final int PLAYBACK_SPEED_INVALID = -1;

    /**
     * Speed representing playback state that is paused.
     */
    public static final int PLAYBACK_SPEED_PAUSED = 0;

    /**
     * Speed representing playback state that is playing normally.
     */
    public static final int PLAYBACK_SPEED_NORMAL = 1;

    /**
     * The initial (level 0) fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L0 = 10;

    /**
     * The level 1 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L1 = 11;

    /**
     * The level 2 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L2 = 12;

    /**
     * The level 3 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L3 = 13;

    /**
     * The level 4 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L4 = 14;

    private static final String TAG = "PlaybackControlGlue";
    private static final boolean DEBUG = false;

    private static final int MSG_UPDATE_PLAYBACK_STATE = 100;
    private static final int UPDATE_PLAYBACK_STATE_DELAY_MS = 2000;
    private static final int NUMBER_OF_SEEK_SPEEDS = PLAYBACK_SPEED_FAST_L4 -
            PLAYBACK_SPEED_FAST_L0 + 1;

    private final int[] mFastForwardSpeeds;
    private final int[] mRewindSpeeds;
    private PlaybackControlsRow mControlsRow;
    private SparseArrayObjectAdapter mPrimaryActionsAdapter;
    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;
    OnItemViewClickedListener mExternalOnItemViewClickedListener;
    private int mPlaybackSpeed = PLAYBACK_SPEED_NORMAL;
    private boolean mFadeWhenPlaying = true;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_PLAYBACK_STATE) {
                updatePlaybackState();
            }
        }
    };

    /**
     * Interface allowing the application to handle input events.
     * @deprecated Use
     * {@link PlaybackGlue.PlaybackGlueHost#setOnKeyInterceptListener(View.OnKeyListener)}.
     */
    @Deprecated
    public interface InputEventHandler {
        /**
         * Called when an {@link InputEvent} is received.
         *
         * @return If the event should be consumed, return true. To allow the event to
         * continue on to the next handler, return false.
         */
        boolean handleInputEvent(InputEvent event);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param seekSpeeds Array of seek speeds for fast forward and rewind.
     */
    public PlaybackControlGlue(Context context, int[] seekSpeeds) {
        this(context, (PlaybackGlueHost) null, seekSpeeds, seekSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     */
    public PlaybackControlGlue(Context context,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        this(context, (PlaybackGlueHost) null, fastForwardSpeeds, rewindSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param fragment Optional; if using a {@link PlaybackOverlayFragment}, pass it in.
     * @param seekSpeeds Array of seek speeds for fast forward and rewind.
     * @deprecated Use
     * {@link #PlaybackControlGlue(Context, PlaybackGlue.PlaybackGlueHost, int[], int[])}.
     */
    @Deprecated
    public PlaybackControlGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] seekSpeeds) {
        this(context, fragment, seekSpeeds, seekSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param fragment Optional; if using a {@link PlaybackOverlayFragment}, pass it in.
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     * @deprecated Use
     * {@link #PlaybackControlGlue(Context, PlaybackGlue.PlaybackGlueHost, int[], int[])}.
     */
    @Deprecated
    public PlaybackControlGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        this(context, fragment == null ? (PlaybackGlueHost) null:
                new PlaybackGlueHostOld(fragment), fastForwardSpeeds, rewindSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param host Optional; if using a {@link PlaybackGlue.PlaybackGlueHost}, pass it in.
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     */
    public PlaybackControlGlue(Context context,
                               PlaybackGlueHost host,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        super(context);
        setHost(host);
        if (fastForwardSpeeds.length == 0 || fastForwardSpeeds.length > NUMBER_OF_SEEK_SPEEDS) {
            throw new IllegalStateException("invalid fastForwardSpeeds array size");
        }
        mFastForwardSpeeds = fastForwardSpeeds;
        if (rewindSpeeds.length == 0 || rewindSpeeds.length > NUMBER_OF_SEEK_SPEEDS) {
            throw new IllegalStateException("invalid rewindSpeeds array size");
        }
        mRewindSpeeds = rewindSpeeds;
    }

    @Override
    public void setHost(PlaybackGlueHost host) {
        super.setHost(host);
        if (mPlaybackGlueHost != null) {
            if (mPlaybackGlueHost instanceof PlaybackGlueHostOld) {
                ((PlaybackGlueHostOld) mPlaybackGlueHost).mGlue = this;
            }
            mPlaybackGlueHost.setOnKeyInterceptListener(this);
            mPlaybackGlueHost.setOnActionClickedListener(this);
        }
    }


    /**
     * Helper method for instantiating a {@link PlaybackControlsRow} and corresponding
     * {@link PlaybackControlsRowPresenter}.
     */
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRow controlsRow = new PlaybackControlsRow(this);
        setControlsRow(controlsRow);

        final AbstractDetailsDescriptionPresenter detailsPresenter =
                new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder
                                                     viewHolder, Object object) {
                PlaybackControlGlue glue = (PlaybackControlGlue) object;
                if (glue.hasValidMedia()) {
                    viewHolder.getTitle().setText(glue.getMediaTitle());
                    viewHolder.getSubtitle().setText(glue.getMediaSubtitle());
                } else {
                    viewHolder.getTitle().setText("");
                    viewHolder.getSubtitle().setText("");
                }
            }
        };

        return new PlaybackControlsRowPresenter(detailsPresenter) {
            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(PlaybackControlGlue.this);
            }
            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
    }

    /**
     * Returns the fragment.
     * @deprecated The glue is no longer associated with a fragment, use {@link #getHost()}.
     */
    @Deprecated
    public PlaybackOverlayFragment getFragment() {
        if (mPlaybackGlueHost instanceof PlaybackGlueHostOld) {
            return ((PlaybackGlueHostOld)mPlaybackGlueHost).mFragment;
        }
        return null;
    }

    /**
     * Returns the fast forward speeds.
     */
    public int[] getFastForwardSpeeds() {
        return mFastForwardSpeeds;
    }

    /**
     * Returns the rewind speeds.
     */
    public int[] getRewindSpeeds() {
        return mRewindSpeeds;
    }

    /**
     * Sets the controls to fade after a timeout when media is playing.
     */
    public void setFadingEnabled(boolean enable) {
        mFadeWhenPlaying = enable;
        if (!mFadeWhenPlaying && mPlaybackGlueHost != null) {
            mPlaybackGlueHost.setFadingEnabled(false);
        }
    }

    /**
     * Returns true if controls are set to fade when media is playing.
     */
    public boolean isFadingEnabled() {
        return mFadeWhenPlaying;
    }

    /**
     * Set the {@link OnItemViewClickedListener} to be called if the click event
     * is not handled internally.
     * @param listener
     * @deprecated Don't call this. Instead use the listener on the fragment yourself.
     */
    @Deprecated
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mExternalOnItemViewClickedListener = listener;
    }

    /**
     * Returns the {@link OnItemViewClickedListener}.
     * @deprecated Don't call this. Instead use the listener on the fragment yourself.
     */
    @Deprecated
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mExternalOnItemViewClickedListener;
    }

    /**
     * Sets the controls row to be managed by the glue layer.
     * The primary actions and playback state related aspects of the row
     * are updated by the glue.
     */
    public void setControlsRow(PlaybackControlsRow controlsRow) {
        mControlsRow = controlsRow;
        mPrimaryActionsAdapter = createPrimaryActionsAdapter(
                new ControlButtonPresenterSelector());
        mControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        updateControlsRow();
    }

    /**
     * Returns the playback controls row managed by the glue layer.
     */
    public PlaybackControlsRow getControlsRow() {
        return mControlsRow;
    }

    /**
     * Override this to start/stop a runnable to call {@link #updateProgress} at
     * an interval such as {@link #getUpdatePeriod}.
     */
    public void enableProgressUpdating(boolean enable) {
    }

    /**
     * Returns the time period in milliseconds that should be used
     * to update the progress.  See {@link #updateProgress()}.
     */
    public int getUpdatePeriod() {
        // TODO: calculate a better update period based on total duration and screen size
        return 500;
    }

    /**
     * Updates the progress bar based on the current media playback position.
     */
    public void updateProgress() {
        int position = getCurrentPosition();
        if (DEBUG) Log.v(TAG, "updateProgress " + position);
        mControlsRow.setCurrentTime(position);
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
                boolean abortSeek = mPlaybackSpeed >= PLAYBACK_SPEED_FAST_L0 ||
                        mPlaybackSpeed <= -PLAYBACK_SPEED_FAST_L0;
                if (abortSeek) {
                    mPlaybackSpeed = PLAYBACK_SPEED_NORMAL;
                    startPlayback(mPlaybackSpeed);
                    updatePlaybackStatusAfterUserAction();
                    return keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE;
                }
                return false;
        }
        Action action = mControlsRow.getActionForKeyCode(mPrimaryActionsAdapter, keyCode);

        if (action != null) {
            if (action == mPrimaryActionsAdapter.lookup(ACTION_PLAY_PAUSE) ||
                    action == mPrimaryActionsAdapter.lookup(ACTION_REWIND) ||
                    action == mPrimaryActionsAdapter.lookup(ACTION_FAST_FORWARD) ||
                    action == mPrimaryActionsAdapter.lookup(ACTION_SKIP_TO_PREVIOUS) ||
                    action == mPrimaryActionsAdapter.lookup(ACTION_SKIP_TO_NEXT)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    dispatchAction(action, event);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the given action is invoked, either by click or keyevent.
     */
    private boolean dispatchAction(Action action, KeyEvent keyEvent) {
        boolean handled = false;
        if (action == mPlayPauseAction) {
            boolean canPlay = keyEvent == null ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY;
            boolean canPause = keyEvent == null ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE;
            if (mPlaybackSpeed != PLAYBACK_SPEED_NORMAL) {
                if (canPlay) {
                    mPlaybackSpeed = PLAYBACK_SPEED_NORMAL;
                    startPlayback(mPlaybackSpeed);
                }
            } else if (canPause) {
                mPlaybackSpeed = PLAYBACK_SPEED_PAUSED;
                pausePlayback();
            }
            updatePlaybackStatusAfterUserAction();
            handled = true;
        } else if (action == mSkipNextAction) {
            skipToNext();
            handled = true;
        } else if (action == mSkipPreviousAction) {
            skipToPrevious();
            handled = true;
        } else if (action == mFastForwardAction) {
            if (mPlaybackSpeed < getMaxForwardSpeedId()) {
                switch (mPlaybackSpeed) {
                    case PLAYBACK_SPEED_FAST_L0:
                    case PLAYBACK_SPEED_FAST_L1:
                    case PLAYBACK_SPEED_FAST_L2:
                    case PLAYBACK_SPEED_FAST_L3:
                        mPlaybackSpeed++;
                        break;
                    default:
                        mPlaybackSpeed = PLAYBACK_SPEED_FAST_L0;
                        break;
                }
                startPlayback(mPlaybackSpeed);
                updatePlaybackStatusAfterUserAction();
            }
            handled = true;
        } else if (action == mRewindAction) {
            if (mPlaybackSpeed > -getMaxRewindSpeedId()) {
                switch (mPlaybackSpeed) {
                    case -PLAYBACK_SPEED_FAST_L0:
                    case -PLAYBACK_SPEED_FAST_L1:
                    case -PLAYBACK_SPEED_FAST_L2:
                    case -PLAYBACK_SPEED_FAST_L3:
                        mPlaybackSpeed--;
                        break;
                    default:
                        mPlaybackSpeed = -PLAYBACK_SPEED_FAST_L0;
                        break;
                }
                startPlayback(mPlaybackSpeed);
                updatePlaybackStatusAfterUserAction();
            }
            handled = true;
        }
        return handled;
    }

    private int getMaxForwardSpeedId() {
        return PLAYBACK_SPEED_FAST_L0 + (mFastForwardSpeeds.length - 1);
    }

    private int getMaxRewindSpeedId() {
        return PLAYBACK_SPEED_FAST_L0 + (mRewindSpeeds.length - 1);
    }

    private void updateControlsRow() {
        updateRowMetadata();
        mHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE);
        updatePlaybackState();
    }

    private void updatePlaybackStatusAfterUserAction() {
        updatePlaybackState(mPlaybackSpeed);
        // Sync playback state after a delay
        mHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PLAYBACK_STATE,
                UPDATE_PLAYBACK_STATE_DELAY_MS);
    }

    private void updateRowMetadata() {
        if (mControlsRow == null) {
            return;
        }

        if (DEBUG) Log.v(TAG, "updateRowMetadata hasValidMedia " + hasValidMedia());

        if (!hasValidMedia()) {
            mControlsRow.setImageDrawable(null);
            mControlsRow.setTotalTime(0);
            mControlsRow.setCurrentTime(0);
        } else {
            mControlsRow.setImageDrawable(getMediaArt());
            mControlsRow.setTotalTime(getMediaDuration());
            mControlsRow.setCurrentTime(getCurrentPosition());
        }

        onRowChanged(mControlsRow);
    }

    private void updatePlaybackState() {
        if (hasValidMedia()) {
            mPlaybackSpeed = getCurrentSpeedId();
            updatePlaybackState(mPlaybackSpeed);
        }
    }

    private void updatePlaybackState(int playbackSpeed) {
        if (mControlsRow == null) {
            return;
        }

        final long actions = getSupportedActions();
        if ((actions & ACTION_SKIP_TO_PREVIOUS) != 0) {
            if (mSkipPreviousAction == null) {
                mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getContext());
            }
            mPrimaryActionsAdapter.set(ACTION_SKIP_TO_PREVIOUS, mSkipPreviousAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_SKIP_TO_PREVIOUS);
            mSkipPreviousAction = null;
        }
        if ((actions & ACTION_REWIND) != 0) {
            if (mRewindAction == null) {
                mRewindAction = new PlaybackControlsRow.RewindAction(
                        getContext(),
                        mRewindSpeeds.length);
            }
            mPrimaryActionsAdapter.set(ACTION_REWIND, mRewindAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_REWIND);
            mRewindAction = null;
        }
        if ((actions & ACTION_PLAY_PAUSE) != 0) {
            if (mPlayPauseAction == null) {
                mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(getContext());
            }
            mPrimaryActionsAdapter.set(ACTION_PLAY_PAUSE, mPlayPauseAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_PLAY_PAUSE);
            mPlayPauseAction = null;
        }
        if ((actions & ACTION_FAST_FORWARD) != 0) {
            if (mFastForwardAction == null) {
                mFastForwardAction = new PlaybackControlsRow.FastForwardAction(
                        getContext(),
                        mFastForwardSpeeds.length);
            }
            mPrimaryActionsAdapter.set(ACTION_FAST_FORWARD, mFastForwardAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_FAST_FORWARD);
            mFastForwardAction = null;
        }
        if ((actions & ACTION_SKIP_TO_NEXT) != 0) {
            if (mSkipNextAction == null) {
                mSkipNextAction = new PlaybackControlsRow.SkipNextAction(getContext());
            }
            mPrimaryActionsAdapter.set(ACTION_SKIP_TO_NEXT, mSkipNextAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_SKIP_TO_NEXT);
            mSkipNextAction = null;
        }

        if (mFastForwardAction != null) {
            int index = 0;
            if (playbackSpeed >= PLAYBACK_SPEED_FAST_L0) {
                index = playbackSpeed - PLAYBACK_SPEED_FAST_L0 + 1;
            }
            if (mFastForwardAction.getIndex() != index) {
                mFastForwardAction.setIndex(index);
                notifyItemChanged(mPrimaryActionsAdapter, mFastForwardAction);
            }
        }
        if (mRewindAction != null) {
            int index = 0;
            if (playbackSpeed <= -PLAYBACK_SPEED_FAST_L0) {
                index = -playbackSpeed - PLAYBACK_SPEED_FAST_L0 + 1;
            }
            if (mRewindAction.getIndex() != index) {
                mRewindAction.setIndex(index);
                notifyItemChanged(mPrimaryActionsAdapter, mRewindAction);
            }
        }

        if (playbackSpeed == PLAYBACK_SPEED_PAUSED) {
            updateProgress();
            enableProgressUpdating(false);
        } else {
            enableProgressUpdating(true);
        }

        if (mFadeWhenPlaying && mPlaybackGlueHost != null) {
            mPlaybackGlueHost.setFadingEnabled(playbackSpeed == PLAYBACK_SPEED_NORMAL);
        }

        if (mPlayPauseAction != null) {
            int index = playbackSpeed == PLAYBACK_SPEED_PAUSED ?
                    PlaybackControlsRow.PlayPauseAction.PLAY :
                    PlaybackControlsRow.PlayPauseAction.PAUSE;
            if (mPlayPauseAction.getIndex() != index) {
                mPlayPauseAction.setIndex(index);
                notifyItemChanged(mPrimaryActionsAdapter, mPlayPauseAction);
            }
        }
    }

    private static void notifyItemChanged(SparseArrayObjectAdapter adapter, Object object) {
        int index = adapter.indexOf(object);
        if (index >= 0) {
            adapter.notifyArrayItemRangeChanged(index, 1);
        }
    }

    private static String getSpeedString(int speed) {
        switch (speed) {
            case PLAYBACK_SPEED_INVALID:
                return "PLAYBACK_SPEED_INVALID";
            case PLAYBACK_SPEED_PAUSED:
                return "PLAYBACK_SPEED_PAUSED";
            case PLAYBACK_SPEED_NORMAL:
                return "PLAYBACK_SPEED_NORMAL";
            case PLAYBACK_SPEED_FAST_L0:
                return "PLAYBACK_SPEED_FAST_L0";
            case PLAYBACK_SPEED_FAST_L1:
                return "PLAYBACK_SPEED_FAST_L1";
            case PLAYBACK_SPEED_FAST_L2:
                return "PLAYBACK_SPEED_FAST_L2";
            case PLAYBACK_SPEED_FAST_L3:
                return "PLAYBACK_SPEED_FAST_L3";
            case PLAYBACK_SPEED_FAST_L4:
                return "PLAYBACK_SPEED_FAST_L4";
            case -PLAYBACK_SPEED_FAST_L0:
                return "-PLAYBACK_SPEED_FAST_L0";
            case -PLAYBACK_SPEED_FAST_L1:
                return "-PLAYBACK_SPEED_FAST_L1";
            case -PLAYBACK_SPEED_FAST_L2:
                return "-PLAYBACK_SPEED_FAST_L2";
            case -PLAYBACK_SPEED_FAST_L3:
                return "-PLAYBACK_SPEED_FAST_L3";
            case -PLAYBACK_SPEED_FAST_L4:
                return "-PLAYBACK_SPEED_FAST_L4";
        }
        return null;
    }

    /**
     * Returns true if there is a valid media item.
     */
    public abstract boolean hasValidMedia();

    /**
     * Returns true if media is currently playing.
     */
    public abstract boolean isMediaPlaying();

    /**
     * Returns the title of the media item.
     */
    public abstract CharSequence getMediaTitle();

    /**
     * Returns the subtitle of the media item.
     */
    public abstract CharSequence getMediaSubtitle();

    /**
     * Returns the duration of the media item in milliseconds.
     */
    public abstract int getMediaDuration();

    /**
     * Returns a bitmap of the art for the media item.
     */
    public abstract Drawable getMediaArt();

    /**
     * Returns a bitmask of actions supported by the media player.
     */
    public abstract long getSupportedActions();

    /**
     * Returns the current playback speed.  When playing normally,
     * {@link #PLAYBACK_SPEED_NORMAL} should be returned.
     */
    public abstract int getCurrentSpeedId();

    /**
     * Returns the current position of the media item in milliseconds.
     */
    public abstract int getCurrentPosition();

    /**
     * Start playback at the given speed.
     * @param speed The desired playback speed.  For normal playback this will be
     *              {@link #PLAYBACK_SPEED_NORMAL}; higher positive values for fast forward,
     *              and negative values for rewind.
     */
    protected abstract void startPlayback(int speed);

    /**
     * Pause playback.
     */
    protected abstract void pausePlayback();

    /**
     * Skip to the next track.
     */
    protected abstract void skipToNext();

    /**
     * Skip to the previous track.
     */
    protected abstract void skipToPrevious();

    /**
     * Invoked when the playback controls row has changed.  The adapter containing this row
     * should be notified.
     */
    protected abstract void onRowChanged(PlaybackControlsRow row);

    /**
     * Creates the primary action adapter.  May be overridden to add additional primary
     * actions to the adapter.
     */
    protected SparseArrayObjectAdapter createPrimaryActionsAdapter(
            PresenterSelector presenterSelector) {
        return new SparseArrayObjectAdapter(presenterSelector);
    }

    /**
     * Must be called appropriately by a subclass when the playback state has changed.
     * It updates the playback state displayed on the media player.
     */
    protected void onStateChanged() {
        if (DEBUG) Log.v(TAG, "onStateChanged");
        // If a pending control button update is present, delay
        // the update until the state settles.
        if (!hasValidMedia()) {
            return;
        }
        if (mHandler.hasMessages(MSG_UPDATE_PLAYBACK_STATE)) {
            mHandler.removeMessages(MSG_UPDATE_PLAYBACK_STATE);
            if (getCurrentSpeedId() != mPlaybackSpeed) {
                if (DEBUG) Log.v(TAG, "Status expectation mismatch, delaying update");
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PLAYBACK_STATE,
                        UPDATE_PLAYBACK_STATE_DELAY_MS);
            } else {
                if (DEBUG) Log.v(TAG, "Update state matches expectation");
                updatePlaybackState();
            }
        } else {
            updatePlaybackState();
        }
    }

    /**
     * Must be called appropriately by a subclass when the metadata state has changed.
     */
    protected void onMetadataChanged() {
        if (DEBUG) Log.v(TAG, "onMetadataChanged");
        updateRowMetadata();
    }

    static final class PlaybackGlueHostOld extends PlaybackGlueHost {
        final PlaybackOverlayFragment mFragment;
        PlaybackControlGlue mGlue;

        public PlaybackGlueHostOld(PlaybackOverlayFragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void setFadingEnabled(boolean enable) {
            mFragment.setFadingEnabled(enable);
        }

        @Override
        public void setOnKeyInterceptListener(final View.OnKeyListener onKeyListenerr) {
            mFragment.setEventHandler( new InputEventHandler() {
                @Override
                public boolean handleInputEvent(InputEvent event) {
                    if (event instanceof KeyEvent) {
                        KeyEvent keyEvent = (KeyEvent) event;
                        return onKeyListenerr.onKey(null, keyEvent.getKeyCode(), keyEvent);
                    }
                    return false;
                }
            });
        }

        @Override
        public void setOnActionClickedListener(final OnActionClickedListener listener) {
            mFragment.setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                          RowPresenter.ViewHolder rowViewHolder, Row row) {
                    if (item instanceof Action) {
                        listener.onActionClicked((Action)item);
                        if (mGlue.mExternalOnItemViewClickedListener != null) {
                            mGlue.mExternalOnItemViewClickedListener.onItemClicked(itemViewHolder,
                                    item, rowViewHolder, row);
                        }
                    }
                }
            });
        }
    }
}
