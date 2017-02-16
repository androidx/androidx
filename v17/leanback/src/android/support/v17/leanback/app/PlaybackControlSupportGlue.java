/* This file is auto-generated from PlaybackControlGlue.java.  DO NOT MODIFY. */

package android.support.v17.leanback.app;

import android.content.Context;
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;

/**
 * @deprecated Use {@link android.support.v17.leanback.media.PlaybackControlGlue} and
 * {@link PlaybackSupportFragmentGlueHost} for {@link PlaybackSupportFragment}.
 */
@Deprecated
public abstract class PlaybackControlSupportGlue extends PlaybackControlGlue {
    /**
     * The adapter key for the first custom control on the left side
     * of the predefined primary controls.
     */
    public static final int ACTION_CUSTOM_LEFT_FIRST = PlaybackControlGlue.ACTION_CUSTOM_LEFT_FIRST;

    /**
     * The adapter key for the skip to previous control.
     */
    public static final int ACTION_SKIP_TO_PREVIOUS = PlaybackControlGlue.ACTION_SKIP_TO_PREVIOUS;

    /**
     * The adapter key for the rewind control.
     */
    public static final int ACTION_REWIND = PlaybackControlGlue.ACTION_REWIND;

    /**
     * The adapter key for the play/pause control.
     */
    public static final int ACTION_PLAY_PAUSE = PlaybackControlGlue.ACTION_PLAY_PAUSE;

    /**
     * The adapter key for the fast forward control.
     */
    public static final int ACTION_FAST_FORWARD = PlaybackControlGlue.ACTION_FAST_FORWARD;

    /**
     * The adapter key for the skip to next control.
     */
    public static final int ACTION_SKIP_TO_NEXT = PlaybackControlGlue.ACTION_SKIP_TO_NEXT;

    /**
     * The adapter key for the first custom control on the right side
     * of the predefined primary controls.
     */
    public static final int ACTION_CUSTOM_RIGHT_FIRST =
            PlaybackControlGlue.ACTION_CUSTOM_RIGHT_FIRST;

    /**
     * Invalid playback speed.
     */
    public static final int PLAYBACK_SPEED_INVALID = PlaybackControlGlue.PLAYBACK_SPEED_INVALID;

    /**
     * Speed representing playback state that is paused.
     */
    public static final int PLAYBACK_SPEED_PAUSED = PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;

    /**
     * Speed representing playback state that is playing normally.
     */
    public static final int PLAYBACK_SPEED_NORMAL = PlaybackControlGlue.PLAYBACK_SPEED_NORMAL;

    /**
     * The initial (level 0) fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L0 = PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0;

    /**
     * The level 1 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L1 = PlaybackControlGlue.PLAYBACK_SPEED_FAST_L1;

    /**
     * The level 2 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L2 = PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2;

    /**
     * The level 3 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L3 = PlaybackControlGlue.PLAYBACK_SPEED_FAST_L3;

    /**
     * The level 4 fast forward playback speed.
     * The negative of this value is for rewind at the same speed.
     */
    public static final int PLAYBACK_SPEED_FAST_L4 = PlaybackControlGlue.PLAYBACK_SPEED_FAST_L4;

    public PlaybackControlSupportGlue(Context context, int[] seekSpeeds) {
        this(context, null, seekSpeeds, seekSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context, int[] fastForwardSpeeds, int[] rewindSpeeds) {
        this(context, null, fastForwardSpeeds, rewindSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context,
            PlaybackOverlaySupportFragment fragment,
            int[] seekSpeeds) {
        this(context, fragment, seekSpeeds, seekSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context,
            PlaybackOverlaySupportFragment fragment,
            int[] fastForwardSpeeds,
            int[] rewindSpeeds) {
        super(context, fastForwardSpeeds, rewindSpeeds);
        setHost(fragment == null ? null : new PlaybackSupportGlueHostOld(fragment));
    }

    @Override
    protected void onAttachedToHost(PlaybackGlueHost host) {
        super.onAttachedToHost(host);
        if (host instanceof PlaybackSupportGlueHostOld) {
            ((PlaybackSupportGlueHostOld) host).mGlue = this;
        }
    }

    static final class PlaybackSupportGlueHostOld extends PlaybackGlueHost {
        final PlaybackOverlaySupportFragment mFragment;
        PlaybackControlSupportGlue mGlue;
        OnActionClickedListener mActionClickedListener;

        public PlaybackSupportGlueHostOld(PlaybackOverlaySupportFragment fragment) {
            mFragment = fragment;
            mFragment.setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                          RowPresenter.ViewHolder rowViewHolder, Row row) {
                    if (item instanceof Action
                            && rowViewHolder instanceof PlaybackRowPresenter.ViewHolder
                            && mActionClickedListener != null) {
                        mActionClickedListener.onActionClicked((Action) item);
                    } else if (mGlue != null && mGlue.getOnItemViewClickedListener() != null) {
                        mGlue.getOnItemViewClickedListener().onItemClicked(itemViewHolder,
                                item, rowViewHolder, row);
                    }
                }
            });
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
            mActionClickedListener = listener;
        }

        @Override
        public void setHostCallback(HostCallback callback) {
            mFragment.setHostCallback(callback);
        }

        @Override
        public void fadeOut() {
            mFragment.fadeOut();
        }

        @Override
        public void notifyPlaybackRowChanged() {
            mGlue.onRowChanged(mGlue.getControlsRow());
        }
    }
}
