/* This file is auto-generated from PlaybackControlGlue.java.  DO NOT MODIFY. */

package android.support.v17.leanback.app;

import android.content.Context;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;

/**
 * @deprecated Use {@link PlaybackControlGlue} and {@link PlaybackSupportFragmentGlueHost} for
 * {@link PlaybackSupportFragment}.
 */
@Deprecated
public abstract class PlaybackControlSupportGlue extends PlaybackControlGlue {

    public PlaybackControlSupportGlue(Context context, int[] seekSpeeds) {
        super(context, seekSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context, int[] fastForwardSpeeds, int[] rewindSpeeds) {
        super(context, fastForwardSpeeds, rewindSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context,
            PlaybackOverlaySupportFragment fragment,
            int[] seekSpeeds) {
        super(context,
                fragment == null ? null: new PlaybackSupportGlueHostOld(fragment),
                seekSpeeds,
                seekSpeeds);
    }

    public PlaybackControlSupportGlue(
            Context context,
            PlaybackOverlaySupportFragment fragment,
            int[] fastForwardSpeeds,
            int[] rewindSpeeds) {
        super(context,
                fragment == null ? null: new PlaybackSupportGlueHostOld(fragment),
                fastForwardSpeeds,
                rewindSpeeds);
    }

    @Override
    public void setHost(PlaybackGlueHost host) {
        super.setHost(host);
        if (host instanceof PlaybackSupportGlueHostOld) {
            ((PlaybackSupportGlueHostOld) host).mGlue = this;
        }
    }

    static final class PlaybackSupportGlueHostOld extends PlaybackGlueHost {
        final PlaybackOverlaySupportFragment mFragment;
        PlaybackControlGlue mGlue;

        public PlaybackSupportGlueHostOld(PlaybackOverlaySupportFragment fragment) {
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
