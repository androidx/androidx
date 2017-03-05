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
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;

/**
 * A helper class for managing a {@link android.support.v17.leanback.widget.PlaybackControlsRow}
 * and {@link PlaybackGlueHost} that implements a
 * recommended approach to handling standard playback control actions such as play/pause,
 * fast forward/rewind at progressive speed levels, and skip to next/previous. This helper class
 * is a glue layer in that manages the configuration of and interaction between the
 * leanback UI components by defining a functional interface to the media player.
 *
 * <p>You can instantiate a concrete subclass such as MediaPlayerGlue or you must
 * subclass this abstract helper.  To create a subclass you must implement all of the
 * abstract methods and the subclass must invoke {@link #onMetadataChanged()} and
 * {@link #onStateChanged()} appropriately.
 * </p>
 *
 * <p>To use an instance of the glue layer, first construct an instance.  Constructor parameters
 * inform the glue what speed levels are supported for fast forward/rewind.
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
 * additional actions by overriding {@link #createPrimaryActionsAdapter}. This helper does not
 * deal in secondary actions so those you may add separately.
 * </p>
 *
 * <p>Provide a click listener on your fragment and if an action is clicked, call
 * {@link #onActionClicked}. If you set a listener by calling {@link #setOnItemViewClickedListener},
 * your listener will be called for all unhandled actions.
 * </p>
 *
 * <p>This helper implements a key event handler. If you pass a
 * {@link PlaybackOverlayFragment}, it will configure its
 * fragment to intercept all key events.  Otherwise, you should set the glue object as key event
 * handler to the ViewHolder when bound by your row presenter; see
 * {@link RowPresenter.ViewHolder#setOnKeyListener(android.view.View.OnKeyListener)}.
 * </p>
 *
 * <p>To update the controls row progress during playback, override {@link #enableProgressUpdating}
 * to manage the lifecycle of a periodic callback to {@link #updateProgress()}.
 * {@link #getUpdatePeriod()} provides a recommended update period.
 * </p>
 * @deprecated Use {@link android.support.v17.leanback.media.PlaybackControlGlue}
 */
@Deprecated
public abstract class PlaybackControlGlue extends
        android.support.v17.leanback.media.PlaybackControlGlue {

    OnItemViewClickedListener mExternalOnItemViewClickedListener;

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param seekSpeeds Array of seek speeds for fast forward and rewind.
     */
    public PlaybackControlGlue(Context context, int[] seekSpeeds) {
        super(context, seekSpeeds, seekSpeeds);
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
        super(context, fastForwardSpeeds, rewindSpeeds);
    }

    /**
     * Constructor for the glue.
     *
     * @param context
     * @param fragment Optional; if using a {@link PlaybackOverlayFragment}, pass it in.
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
     * @param context
     * @param fragment Optional; if using a {@link PlaybackOverlayFragment}, pass it in.
     * @param fastForwardSpeeds Array of seek speeds for fast forward.
     * @param rewindSpeeds Array of seek speeds for rewind.
     */
    public PlaybackControlGlue(Context context,
                               PlaybackOverlayFragment fragment,
                               int[] fastForwardSpeeds,
                               int[] rewindSpeeds) {
        super(context, fastForwardSpeeds, rewindSpeeds);
        setHost(fragment == null ? (PlaybackGlueHost) null : new PlaybackGlueHostOld(fragment));
    }

    @Override
    protected void onAttachedToHost(PlaybackGlueHost host) {
        super.onAttachedToHost(host);
        if (host instanceof PlaybackGlueHostOld) {
            ((PlaybackGlueHostOld) host).mGlue = this;
        }
    }

    /**
     * Returns the fragment.
     */
    public PlaybackOverlayFragment getFragment() {
        if (getHost() instanceof PlaybackGlueHostOld) {
            return ((PlaybackGlueHostOld)getHost()).mFragment;
        }
        return null;
    }

    /**
     * Start playback at the given speed.
     * @deprecated use {@link #play()} instead.
     *
     * @param speed The desired playback speed.  For normal playback this will be
     *              {@link #PLAYBACK_SPEED_NORMAL}; higher positive values for fast forward,
     *              and negative values for rewind.
     */
    @Deprecated
    protected void startPlayback(int speed) {}

    /**
     * Pause playback.
     * @deprecated use {@link #pause()} instead.
     */
    @Deprecated
    protected void pausePlayback() {}

    /**
     * Skip to the next track.
     * @deprecated use {@link #next()} instead.
     */
    @Deprecated
    protected void skipToNext() {}

    /**
     * Skip to the previous track.
     * @deprecated use {@link #previous()} instead.
     */
    @Deprecated
    protected void skipToPrevious() {}

    @Override
    public final void next() {
        skipToNext();
    }

    @Override
    public final void previous() {
        skipToPrevious();
    }

    @Override
    public final void play(int speed) {
        startPlayback(speed);
    }

    @Override
    public final void pause() {
        pausePlayback();
    }

    /**
     * This method invoked when the playback controls row has changed. The adapter
     * containing this row should be notified.
     */
    protected void onRowChanged(PlaybackControlsRow row) {
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

    @Override
    protected void onCreateControlsRowAndPresenter() {
        // backward compatible, we dont create row / presenter by default.
        // User is expected to call createControlsRowAndPresenter() or setControlsRow()
        // explicitly.
    }

    /**
     * Helper method for instantiating a
     * {@link android.support.v17.leanback.widget.PlaybackControlsRow} and corresponding
     * {@link android.support.v17.leanback.widget.PlaybackControlsRowPresenter}.
     */
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        super.onCreateControlsRowAndPresenter();
        return getControlsRowPresenter();
    }

    @Override
    protected SparseArrayObjectAdapter createPrimaryActionsAdapter(
            PresenterSelector presenterSelector) {
        return super.createPrimaryActionsAdapter(presenterSelector);
    }

    /**
     * Interface allowing the application to handle input events.
     * @deprecated Use
     * {@link PlaybackGlueHost#setOnKeyInterceptListener(View.OnKeyListener)}.
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

    static final class PlaybackGlueHostOld extends PlaybackGlueHost {
        final PlaybackOverlayFragment mFragment;
        PlaybackControlGlue mGlue;
        OnActionClickedListener mActionClickedListener;

        public PlaybackGlueHostOld(PlaybackOverlayFragment fragment) {
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
        public void setOnKeyInterceptListener(final View.OnKeyListener onKeyListener) {
            mFragment.setEventHandler( new InputEventHandler() {
                @Override
                public boolean handleInputEvent(InputEvent event) {
                    if (event instanceof KeyEvent) {
                        KeyEvent keyEvent = (KeyEvent) event;
                        return onKeyListener.onKey(null, keyEvent.getKeyCode(), keyEvent);
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
