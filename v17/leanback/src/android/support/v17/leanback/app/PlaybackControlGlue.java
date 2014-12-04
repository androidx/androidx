package android.support.v17.leanback.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
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


/**
 * A helper class for managing a {@link android.support.v17.leanback.widget.PlaybackControlsRow} and
 * {@link PlaybackOverlayFragment} that implements a recommended approach to handling standard
 * playback control actions such as play/pause, fast forward/rewind at progressive speed levels,
 * and skip to next/previous.  This helper class is a glue layer in that it manages the
 * configuration of and interaction between the leanback UI components by defining a functional
 * interface to the media player.
 *
 * <p>You can instantiate a concrete subclass such as {@link MediaControllerGlue} or you must
 * subclass this abstract helper.  To create a subclass you must implement all of the
 * abstract methods and the subclass must invoke {@link #onMetadataChanged()} and
 * {@link #onStateChanged()} appropriately.
 * </p>
 *
 * <p>To use an instance of the glue layer, first construct an instance.  Constructor parameters
 * inform the glue what speed levels are supported for fast forward/rewind.  If you have your own
 * controls row you must pass it to {@link #setControlsRow}.  The row will be updated by the glue
 * layer based on the media metadata and playback state.  Alternatively, you may call
 * {@link #createControlsRowAndPresenter()} which will set a controls row and return
 * a row presenter you can use to present the row.
 * </p>
 *
 * <p>The helper sets a {@link android.support.v17.leanback.widget.SparseArrayObjectAdapter}
 * on the controls row as the primary actions adapter, and adds actions to it.  You can provide
 * additional actions by overriding {@link #createPrimaryActionsAdapter}.  This helper does not
 * deal in secondary actions so those you may add separately.
 * </p>
 *
 * <p>The helper sets an {@link android.support.v17.leanback.widget.OnItemViewClickedListener}
 * on the fragment.  To receive callbacks on clicks for elements unknown to the helper, pass
 * a listener to {@link #setOnItemViewClickedListener}.
 * </p>
 *
 * <p>To update the controls row progress during playback, override {@link #enableProgressUpdating}
 * to manage the lifecycle of a periodic callback to {@link #updateProgress()}.
 * {@link #getUpdatePeriod()} provides a recommended update period.
 * </p>
 *
 */
public abstract class PlaybackControlGlue {
    /**
     * The adapter key for the first custom control on the right side
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

    private final PlaybackOverlayFragment mFragment;
    private final Context mContext;
    private final int[] mFastForwardSpeeds;
    private final int[] mRewindSpeeds;
    private PlaybackControlsRow mControlsRow;
    private SparseArrayObjectAdapter mPrimaryActionsAdapter;
    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;
    private OnItemViewClickedListener mExternalOnItemViewClickedListener;
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

    private final OnItemViewClickedListener mOnItemViewClickedListener =
            new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder viewHolder, Object object,
                                  RowPresenter.ViewHolder viewHolder2, Row row) {
            if (DEBUG) Log.v(TAG, "onItemClicked " + object);
            boolean handled = false;
            if (object instanceof Action) {
                handled = handleActionClicked((Action) object);
            }
            if (!handled && mExternalOnItemViewClickedListener != null) {
                mExternalOnItemViewClickedListener.onItemClicked(viewHolder, object,
                        viewHolder2, row);
            }
        }
    };

    private final PlaybackOverlayFragment.InputEventHandler mInputEventHandler =
            new PlaybackOverlayFragment.InputEventHandler() {
        @Override
        public boolean handleInputEvent(InputEvent event) {
            boolean result = false;
            if (event instanceof KeyEvent &&
                    ((KeyEvent) event).getAction() == KeyEvent.ACTION_DOWN) {
                int keyCode = ((KeyEvent) event).getKeyCode();
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_BACK:
                        if (mPlaybackSpeed >= PLAYBACK_SPEED_FAST_L0 ||
                                mPlaybackSpeed <= -PLAYBACK_SPEED_FAST_L0) {
                            mPlaybackSpeed = PLAYBACK_SPEED_NORMAL;
                            startPlayback(mPlaybackSpeed);
                            updatePlaybackStatusAfterUserAction();
                            result = (keyCode == KeyEvent.KEYCODE_BACK);
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (mPlayPauseAction != null) {
                            handleActionClicked(mPlayPauseAction);
                            result = true;
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        if (mFastForwardAction != null) {
                            handleActionClicked(mFastForwardAction);
                            result = true;
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        if (mRewindAction != null) {
                            handleActionClicked(mRewindAction);
                            result = true;
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        if (mSkipPreviousAction != null) {
                            handleActionClicked(mSkipPreviousAction);
                            result = true;
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        if (mSkipNextAction != null) {
                            handleActionClicked(mSkipNextAction);
                            result = true;
                        }
                        break;
                }
            }
            return result;
        }
    };

    /**
     * Constructor for the glue.
     *
     * <p>The {@link PlaybackOverlayFragment} must be passed in.
     * A {@link OnItemViewClickedListener} and {@link PlaybackOverlayFragment.InputEventHandler}
     * will be set on the fragment.
     * </p>
     *
     * @param context
     * @param fragment
     * @param seekSpeeds Array of seek speeds for fast forward and rewind.
     */
    public PlaybackControlGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] seekSpeeds) {
        this(context, fragment, seekSpeeds, seekSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * <p>The {@link PlaybackOverlayFragment} must be passed in.
     * A {@link OnItemViewClickedListener} and {@link PlaybackOverlayFragment.InputEventHandler}
     * will be set on the fragment.
     * </p>
     *
     * @param context
     * @param fragment
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     */
    public PlaybackControlGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        mContext = context;
        mFragment = fragment;
        if (mFragment.getOnItemViewClickedListener() != null) {
            throw new IllegalStateException("Fragment OnItemViewClickedListener already present");
        }
        mFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);
        if (mFragment.getInputEventHandler() != null) {
            throw new IllegalStateException("Fragment InputEventListener already present");
        }
        mFragment.setInputEventHandler(mInputEventHandler);
        if (fastForwardSpeeds.length == 0 || fastForwardSpeeds.length > NUMBER_OF_SEEK_SPEEDS) {
            throw new IllegalStateException("invalid fastForwardSpeeds array size");
        }
        mFastForwardSpeeds = fastForwardSpeeds;
        if (rewindSpeeds.length == 0 || rewindSpeeds.length > NUMBER_OF_SEEK_SPEEDS) {
            throw new IllegalStateException("invalid rewindSpeeds array size");
        }
        mRewindSpeeds = rewindSpeeds;
    }

    /**
     * Helper method for instantiating a
     * {@link android.support.v17.leanback.widget.PlaybackControlsRow} and corresponding
     * {@link android.support.v17.leanback.widget.PlaybackControlsRowPresenter}.
     */
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRow controlsRow = new PlaybackControlsRow(this);
        setControlsRow(controlsRow);

        return new PlaybackControlsRowPresenter(new AbstractDetailsDescriptionPresenter() {
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
        });
    }

    /**
     * Returns the fragment.
     */
    public PlaybackOverlayFragment getFragment() {
        return mFragment;
    }

    /**
     * Returns the context.
     */
    public Context getContext() {
        return mContext;
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
        if (!mFadeWhenPlaying) {
            mFragment.setFadingEnabled(false);
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
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mExternalOnItemViewClickedListener = listener;
    }

    /**
     * Returns the {@link OnItemViewClickedListener}.
     */
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

    private boolean handleActionClicked(Action action) {
        boolean handled = false;
        if (action == mPlayPauseAction) {
            if (mPlaybackSpeed != PLAYBACK_SPEED_NORMAL) {
                mPlaybackSpeed = PLAYBACK_SPEED_NORMAL;
                startPlayback(mPlaybackSpeed);
            } else {
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
                    case PLAYBACK_SPEED_NORMAL:
                    case PLAYBACK_SPEED_PAUSED:
                        mPlaybackSpeed = PLAYBACK_SPEED_FAST_L0;
                        break;
                    case PLAYBACK_SPEED_FAST_L0:
                    case PLAYBACK_SPEED_FAST_L1:
                    case PLAYBACK_SPEED_FAST_L2:
                    case PLAYBACK_SPEED_FAST_L3:
                        mPlaybackSpeed++;
                        break;
                }
                startPlayback(mPlaybackSpeed);
                updatePlaybackStatusAfterUserAction();
            }
            handled = true;
        } else if (action == mRewindAction) {
            if (mPlaybackSpeed > -getMaxRewindSpeedId()) {
                switch (mPlaybackSpeed) {
                    case PLAYBACK_SPEED_NORMAL:
                    case PLAYBACK_SPEED_PAUSED:
                        mPlaybackSpeed = -PLAYBACK_SPEED_FAST_L0;
                        break;
                    case -PLAYBACK_SPEED_FAST_L0:
                    case -PLAYBACK_SPEED_FAST_L1:
                    case -PLAYBACK_SPEED_FAST_L2:
                    case -PLAYBACK_SPEED_FAST_L3:
                        mPlaybackSpeed--;
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
                mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(mContext);
            }
            mPrimaryActionsAdapter.set(ACTION_SKIP_TO_PREVIOUS, mSkipPreviousAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_SKIP_TO_PREVIOUS);
            mSkipPreviousAction = null;
        }
        if ((actions & ACTION_REWIND) != 0) {
            if (mRewindAction == null) {
                mRewindAction = new PlaybackControlsRow.RewindAction(mContext,
                        mRewindSpeeds.length);
            }
            mPrimaryActionsAdapter.set(ACTION_REWIND, mRewindAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_REWIND);
            mRewindAction = null;
        }
        if ((actions & ACTION_PLAY_PAUSE) != 0) {
            if (mPlayPauseAction == null) {
                mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(mContext);
            }
            mPrimaryActionsAdapter.set(ACTION_PLAY_PAUSE, mPlayPauseAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_PLAY_PAUSE);
            mPlayPauseAction = null;
        }
        if ((actions & ACTION_FAST_FORWARD) != 0) {
            if (mFastForwardAction == null) {
                mFastForwardAction = new PlaybackControlsRow.FastForwardAction(mContext,
                        mFastForwardSpeeds.length);
            }
            mPrimaryActionsAdapter.set(ACTION_FAST_FORWARD, mFastForwardAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_FAST_FORWARD);
            mFastForwardAction = null;
        }
        if ((actions & ACTION_SKIP_TO_NEXT) != 0) {
            if (mSkipNextAction == null) {
                mSkipNextAction = new PlaybackControlsRow.SkipNextAction(mContext);
            }
            mPrimaryActionsAdapter.set(ACTION_SKIP_TO_NEXT, mSkipNextAction);
        } else {
            mPrimaryActionsAdapter.clear(ACTION_SKIP_TO_NEXT);
            mSkipNextAction = null;
        }

        if (mFastForwardAction != null) {
            int index = 0;
            if (playbackSpeed >= PLAYBACK_SPEED_FAST_L0) {
                index = playbackSpeed - PLAYBACK_SPEED_FAST_L0;
                if (playbackSpeed < getMaxForwardSpeedId()) {
                    index++;
                }
            }
            if (mFastForwardAction.getIndex() != index) {
                mFastForwardAction.setIndex(index);
                notifyItemChanged(mPrimaryActionsAdapter, mFastForwardAction);
            }
        }
        if (mRewindAction != null) {
            int index = 0;
            if (playbackSpeed <= -PLAYBACK_SPEED_FAST_L0) {
                index = -playbackSpeed - PLAYBACK_SPEED_FAST_L0;
                if (-playbackSpeed < getMaxRewindSpeedId()) {
                    index++;
                }
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

        if (mFadeWhenPlaying) {
            mFragment.setFadingEnabled(playbackSpeed == PLAYBACK_SPEED_NORMAL);
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
}
