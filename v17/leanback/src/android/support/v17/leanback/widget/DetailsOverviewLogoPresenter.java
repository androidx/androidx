package android.support.v17.leanback.widget;

import android.support.v17.leanback.R;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;

/**
 * Presenter that responsible to create a ImageView and bind to DetailsOverviewRow. The default
 * implementation uses {@link DetailsOverviewRow#getImageDrawable()} and binds to {@link ImageView}.
 * <p>
 * Subclass may override and has its own image view. Subclass may also download image from URL
 * instead of using {@link DetailsOverviewRow#getImageDrawable()}. It's subclass's responsibility to
 * call {@link FullWidthDetailsOverviewRowPresenter#notifyOnBindLogo(FullWidthDetailsOverviewRowPresenter.ViewHolder)}
 * whenever {@link #isBoundToImage(ViewHolder, DetailsOverviewRow)} turned to true so that activity
 * transition can be started.
 */
public class DetailsOverviewLogoPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {

        protected FullWidthDetailsOverviewRowPresenter mParentPresenter;
        protected FullWidthDetailsOverviewRowPresenter.ViewHolder mParentViewHolder;

        public ViewHolder(View view) {
            super(view);
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);
        view.setLayoutParams(new ViewGroup.MarginLayoutParams(0, 0));
        return new ViewHolder(view);
    }

    /**
     * Called from {@link FullWidthDetailsOverviewRowPresenter} to setup FullWidthDetailsOverviewRowPresenter
     * and FullWidthDetailsOverviewRowPresenter.ViewHolder that hosts the logo.
     * @param viewHolder
     * @param parentViewHolder
     * @param parentPresenter
     */
    public void setContext(ViewHolder viewHolder,
            FullWidthDetailsOverviewRowPresenter.ViewHolder parentViewHolder,
            FullWidthDetailsOverviewRowPresenter parentPresenter) {
        viewHolder.mParentViewHolder = parentViewHolder;
        viewHolder.mParentPresenter = parentPresenter;
    }

    /**
     * Returns true if the logo view is bound to image. Subclass may override. The default
     * implementation returns true when {@link DetailsOverviewRow#getImageDrawable()} is not null.
     * If subclass of DetailsOverviewLogoPresenter manages its own image drawable, it should
     * override this function to report status correctly and invoke
     * {@link FullWidthDetailsOverviewRowPresenter#notifyOnBindLogo(FullWidthDetailsOverviewRowPresenter.ViewHolder)}
     * when image view is bound to the drawable.
     */
    public boolean isBoundToImage(ViewHolder viewHolder, DetailsOverviewRow row) {
        return row != null && row.getImageDrawable() != null;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        DetailsOverviewRow row = (DetailsOverviewRow) item;
        ImageView imageView = ((ImageView) viewHolder.view);
        imageView.setImageDrawable(row.getImageDrawable());
        if (isBoundToImage((ViewHolder) viewHolder, row)) {
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            lp.width = row.getImageDrawable().getIntrinsicWidth();
            lp.height = row.getImageDrawable().getIntrinsicHeight();
            imageView.setLayoutParams(lp);
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.mParentPresenter.notifyOnBindLogo(vh.mParentViewHolder);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

}
