package android.support.v17.leanback.app;

import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;

/**
 * {@link PlaybackGlue.PlaybackGlueHost} implementation
 * the interaction between this class and {@link PlaybackFragment}.
 */
public final class PlaybackFragmentGlueHost extends PlaybackGlue.PlaybackGlueHost {

    private final PlaybackFragment mFragment;

    public PlaybackFragmentGlueHost(PlaybackFragment fragment) {
        this.mFragment = fragment;
    }

    @Override
    public void setFadingEnabled(boolean enable) {
        mFragment.setFadingEnabled(enable);
    }

    @Override
    public void setOnKeyInterceptListener(View.OnKeyListener onKeyListener) {
        mFragment.setOnKeyInterceptListener(onKeyListener);
    }

    @Override
    public void setOnActionClickedListener(final OnActionClickedListener listener) {
        mFragment.setOnPlaybackItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Action) {
                    listener.onActionClicked((Action)item);
                }
            }
        });
    }
}
