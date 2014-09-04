/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import android.support.v17.leanback.app.HeadersFragment;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.view.View;
import android.view.ViewGroup;

/**
 * An abstract {@link Presenter} that renders a {@link Row}.
 *
 * <h3>Customize UI widgets</h3>
 * When a subclass of RowPresenter adds UI widgets, it should subclass
 * {@link RowPresenter.ViewHolder} and override {@link #createRowViewHolder(ViewGroup)}
 * and {@link #initializeRowViewHolder(ViewHolder)}. The subclass must use layout id
 * "row_content" for the widget that will be aligned to the title of any {@link HeadersFragment}
 * that may exist in the parent fragment. RowPresenter contains an optional and
 * replaceable {@link RowHeaderPresenter} that renders the header. You can disable
 * the default rendering or replace the Presenter with a new header presenter
 * by calling {@link #setHeaderPresenter(RowHeaderPresenter)}.
 *
 * <h3>UI events from fragments</h3>
 * RowPresenter receives calls from its parent (typically a Fragment) when:
 * <ul>
 * <li>
 * A Row is selected via {@link #setRowViewSelected(Presenter.ViewHolder, boolean)}.  The event
 * is triggered immediately when there is a row selection change before the selection
 * animation is started.
 * Subclasses of RowPresenter may override {@link #onRowViewSelected(ViewHolder, boolean)}.
 * </li>
 * <li>
 * A Row is expanded to full width via {@link #setRowViewExpanded(Presenter.ViewHolder, boolean)}.
 * The event is triggered immediately before the expand animation is started.
 * Subclasses of RowPresenter may override {@link #onRowViewExpanded(ViewHolder, boolean)}.
 * </li>
 * </ul>
 *
 * <h3>User events</h3>
 * RowPresenter provides {@link OnItemSelectedListener} and {@link OnItemClickedListener}.
 * If a subclass wants to add its own {@link View.OnFocusChangeListener} or
 * {@link View.OnClickListener}, it must do that in {@link #createRowViewHolder(ViewGroup)}
 * to be properly chained by the library.  Adding View listeners after
 * {@link #createRowViewHolder(ViewGroup)} is undefined and may result in
 * incorrect behavior by the library's listeners.
 *
 * <h3>Selection animation</h3>
 * <p>
 * When a user scrolls through rows, a fragment will initiate animation and call
 * {@link #setSelectLevel(Presenter.ViewHolder, float)} with float value between
 * 0 and 1.  By default, the RowPresenter draws a dim overlay on top of the row
 * view for views that are not selected. Subclasses may override this default effect
 * by having {@link #isUsingDefaultSelectEffect()} return false and overriding
 * {@link #onSelectLevelChanged(ViewHolder)} to apply a different selection effect.
 * </p>
 * <p>
 * Call {@link #setSelectEffectEnabled(boolean)} to enable/disable the select effect,
 * This will not only enable/disable the default dim effect but also subclasses must
 * respect this flag as well.
 * </p>
 */
public abstract class RowPresenter extends Presenter {

    static class ContainerViewHolder extends Presenter.ViewHolder {
        /**
         * wrapped row view holder
         */
        final ViewHolder mRowViewHolder;

        public ContainerViewHolder(RowContainerView containerView, ViewHolder rowViewHolder) {
            super(containerView);
            containerView.addRowView(rowViewHolder.view);
            if (rowViewHolder.mHeaderViewHolder != null) {
                containerView.addHeaderView(rowViewHolder.mHeaderViewHolder.view);
            }
            mRowViewHolder = rowViewHolder;
            mRowViewHolder.mContainerViewHolder = this;
        }
    }

    /**
     * A view holder for a {@link Row}.
     */
    public static class ViewHolder extends Presenter.ViewHolder {
        ContainerViewHolder mContainerViewHolder;
        RowHeaderPresenter.ViewHolder mHeaderViewHolder;
        Row mRow;
        boolean mSelected;
        boolean mExpanded;
        boolean mInitialzed;
        float mSelectLevel = 0f; // initially unselected
        protected final ColorOverlayDimmer mColorDimmer;

        /**
         * Constructor for ViewHolder.
         *
         * @param view The View bound to the Row.
         */
        public ViewHolder(View view) {
            super(view);
            mColorDimmer = ColorOverlayDimmer.createDefault(view.getContext());
        }

        /**
         * Returns the Row bound to the View in this ViewHolder.
         */
        public final Row getRow() {
            return mRow;
        }

        /**
         * Returns whether the Row is in its expanded state.
         *
         * @return true if the Row is expanded, false otherwise.
         */
        public final boolean isExpanded() {
            return mExpanded;
        }

        /**
         * Returns whether the Row is selected.
         *
         * @return true if the Row is selected, false otherwise.
         */
        public final boolean isSelected() {
            return mSelected;
        }

        /**
         * Returns the current selection level of the Row.
         */
        public final float getSelectLevel() {
            return mSelectLevel;
        }

        /**
         * Returns the view holder for the Row header for this Row.
         */
        public final RowHeaderPresenter.ViewHolder getHeaderViewHolder() {
            return mHeaderViewHolder;
        }
    }

    private RowHeaderPresenter mHeaderPresenter = new RowHeaderPresenter();
    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;

    boolean mSelectEffectEnabled = true;

    @Override
    public final Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder vh = createRowViewHolder(parent);
        vh.mInitialzed = false;
        Presenter.ViewHolder result;
        if (needsRowContainerView()) {
            RowContainerView containerView = new RowContainerView(parent.getContext());
            if (mHeaderPresenter != null) {
                vh.mHeaderViewHolder = (RowHeaderPresenter.ViewHolder)
                        mHeaderPresenter.onCreateViewHolder((ViewGroup) vh.view);
            }
            result = new ContainerViewHolder(containerView, vh);
        } else {
            result = vh;
        }
        initializeRowViewHolder(vh);
        if (!vh.mInitialzed) {
            throw new RuntimeException("super.initializeRowViewHolder() must be called");
        }
        return result;
    }

    /**
     * Called to create a ViewHolder object for a Row. Subclasses will override
     * this method to return a different concrete ViewHolder object. 
     *
     * @param parent The parent View for the Row's view holder.
     * @return A ViewHolder for the Row's View.
     */
    protected abstract ViewHolder createRowViewHolder(ViewGroup parent);

    /**
     * Called after a {@link RowPresenter.ViewHolder} is created for a Row.
     * Subclasses may override this method and start by calling
     * super.initializeRowViewHolder(ViewHolder).
     *
     * @param vh The ViewHolder to initialize for the Row.
     */
    protected void initializeRowViewHolder(ViewHolder vh) {
        vh.mInitialzed = true;
    }

    /**
     * Set the Presenter used for rendering the header. Can be null to disable
     * header rendering. The method must be called before creating any Row Views.
     */
    public final void setHeaderPresenter(RowHeaderPresenter headerPresenter) {
        mHeaderPresenter = headerPresenter;
    }

    /**
     * Get the Presenter used for rendering the header, or null if none has been
     * set.
     */
    public final RowHeaderPresenter getHeaderPresenter() {
        return mHeaderPresenter;
    }

    /**
     * Get the {@link RowPresenter.ViewHolder} from the given Presenter
     * ViewHolder.
     */
    public final ViewHolder getRowViewHolder(Presenter.ViewHolder holder) {
        if (holder instanceof ContainerViewHolder) {
            return ((ContainerViewHolder) holder).mRowViewHolder;
        } else {
            return (ViewHolder) holder;
        }
    }

    /**
     * Set the expanded state of a Row view.
     *
     * @param holder The Row ViewHolder to set expanded state on.
     * @param expanded True if the Row is expanded, false otherwise.
     */
    public final void setRowViewExpanded(Presenter.ViewHolder holder, boolean expanded) {
        ViewHolder rowViewHolder = getRowViewHolder(holder);
        rowViewHolder.mExpanded = expanded;
        onRowViewExpanded(rowViewHolder, expanded);
    }

    /**
     * Set the selected state of a Row view.
     *
     * @param holder The Row ViewHolder to set expanded state on.
     * @param selected True if the Row is expanded, false otherwise.
     */
    public final void setRowViewSelected(Presenter.ViewHolder holder, boolean selected) {
        ViewHolder rowViewHolder = getRowViewHolder(holder);
        rowViewHolder.mSelected = selected;
        onRowViewSelected(rowViewHolder, selected);
    }

    /**
     * Subclass may override this to respond to expanded state changes of a Row.
     * The default implementation will hide/show the header view. Subclasses may
     * make visual changes to the Row View but must not create animation on the
     * Row view.
     */
    protected void onRowViewExpanded(ViewHolder vh, boolean expanded) {
        updateHeaderViewVisibility(vh);
        vh.view.setActivated(expanded);
    }

    /**
     * Subclass may override this to respond to selected state changes of a Row.
     * Subclass may make visual changes to Row view but must not create
     * animation on the Row view.
     */
    protected void onRowViewSelected(ViewHolder vh, boolean selected) {
        if (selected) {
            if (mOnItemViewSelectedListener != null) {
                mOnItemViewSelectedListener.onItemSelected(null, null, vh, vh.getRow());
            }
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(null, vh.getRow());
            }
        }
        updateHeaderViewVisibility(vh);
    }

    private void updateHeaderViewVisibility(ViewHolder vh) {
        if (mHeaderPresenter != null && vh.mHeaderViewHolder != null) {
            RowContainerView containerView = ((RowContainerView) vh.mContainerViewHolder.view);
            containerView.showHeader(vh.isExpanded());
        }
    }

    /**
     * Set the current select level to a value between 0 (unselected) and 1 (selected).
     * Subclasses may override {@link #onSelectLevelChanged(ViewHolder)} to
     * respond to changes in the selected level.
     */
    public final void setSelectLevel(Presenter.ViewHolder vh, float level) {
        ViewHolder rowViewHolder = getRowViewHolder(vh);
        rowViewHolder.mSelectLevel = level;
        onSelectLevelChanged(rowViewHolder);
    }

    /**
     * Get the current select level. The value will be between 0 (unselected) 
     * and 1 (selected).
     */
    public final float getSelectLevel(Presenter.ViewHolder vh) {
        return getRowViewHolder(vh).mSelectLevel;
    }

    /**
     * Callback when select level is changed. The default implementation applies
     * the select level to {@link RowHeaderPresenter#setSelectLevel(RowHeaderPresenter.ViewHolder, float)}
     * when {@link #getSelectEffectEnabled()} is true. Subclasses may override
     * this function and implement a different select effect. In this case, you
     * should also override {@link #isUsingDefaultSelectEffect()} to disable
     * the default dimming effect applied by the library.
     */
    protected void onSelectLevelChanged(ViewHolder vh) {
        if (getSelectEffectEnabled()) {
            vh.mColorDimmer.setActiveLevel(vh.mSelectLevel);
            if (vh.mHeaderViewHolder != null) {
                mHeaderPresenter.setSelectLevel(vh.mHeaderViewHolder, vh.mSelectLevel);
            }
            if (isUsingDefaultSelectEffect()) {
                ((RowContainerView) vh.mContainerViewHolder.view).setForegroundColor(
                        vh.mColorDimmer.getPaint().getColor());
            }
        }
    }

    /**
     * Enables or disables the row selection effect.
     * This will not only affect the default dim effect, but subclasses must
     * respect this flag as well.
     */
    public final void setSelectEffectEnabled(boolean applyDimOnSelect) {
        mSelectEffectEnabled = applyDimOnSelect;
    }

    /**
     * Returns true if the row selection effect is enabled.
     * This value not only determines whether the default dim implementation is
     * used, but subclasses must also respect this flag.
     */
    public final boolean getSelectEffectEnabled() {
        return mSelectEffectEnabled;
    }

    /**
     * Return whether this RowPresenter is using the default dimming effect
     * provided by the library.  Subclasses may(most likely) return false and
     * override {@link #onSelectLevelChanged(ViewHolder)}.
     */
    public boolean isUsingDefaultSelectEffect() {
        return true;
    }

    final boolean needsDefaultSelectEffect() {
        return isUsingDefaultSelectEffect() && getSelectEffectEnabled();
    }

    final boolean needsRowContainerView() {
        return mHeaderPresenter != null || needsDefaultSelectEffect();
    }

    /**
     * Return true if the Row view can draw outside its bounds.
     */
    public boolean canDrawOutOfBounds() {
        return false;
    }

    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        onBindRowViewHolder(getRowViewHolder(viewHolder), item);
    }

    protected void onBindRowViewHolder(ViewHolder vh, Object item) {
        vh.mRow = (Row) item;
        if (vh.mHeaderViewHolder != null) {
            mHeaderPresenter.onBindViewHolder(vh.mHeaderViewHolder, item);
        }
    }

    @Override
    public final void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        onUnbindRowViewHolder(getRowViewHolder(viewHolder));
    }

    protected void onUnbindRowViewHolder(ViewHolder vh) {
        if (vh.mHeaderViewHolder != null) {
            mHeaderPresenter.onUnbindViewHolder(vh.mHeaderViewHolder);
        }
        vh.mRow = null;
    }

    @Override
    public final void onViewAttachedToWindow(Presenter.ViewHolder holder) {
        onRowViewAttachedToWindow(getRowViewHolder(holder));
    }

    protected void onRowViewAttachedToWindow(ViewHolder vh) {
        if (vh.mHeaderViewHolder != null) {
            mHeaderPresenter.onViewAttachedToWindow(vh.mHeaderViewHolder);
        }
    }

    @Override
    public final void onViewDetachedFromWindow(Presenter.ViewHolder holder) {
        onRowViewDetachedFromWindow(getRowViewHolder(holder));
    }

    protected void onRowViewDetachedFromWindow(ViewHolder vh) {
        if (vh.mHeaderViewHolder != null) {
            mHeaderPresenter.onViewDetachedFromWindow(vh.mHeaderViewHolder);
        }
        cancelAnimationsRecursive(vh.view);
    }

    /**
     * Set the listener for item or row selection. A RowPresenter fires a row
     * selection event with a null item. Subclasses (e.g. {@link ListRowPresenter})
     * can fire a selection event with the selected item.
     */
    public final void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /**
     * Get the listener for item or row selection.
     */
    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Set the listener for item click events. A RowPresenter does not use this
     * listener, but a subclass may fire an item click event if it has the concept
     * of an item. The {@link OnItemClickedListener} will override any
     * {@link View.OnClickListener} that an item's Presenter sets during
     * {@link Presenter#onCreateViewHolder(ViewGroup)}. So in general, you
     * should choose to use an OnItemClickedListener or a {@link
     * View.OnClickListener}, but not both.
     */
    public final void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
    }

    /**
     * Get the listener for item click events.
     */
    public final OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Set listener for item or row selection.  RowPresenter fires row selection
     * event with null item, subclass of RowPresenter e.g. {@link ListRowPresenter} can
     * fire a selection event with selected item.
     */
    public final void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mOnItemViewSelectedListener = listener;
    }

    /**
     * Get listener for item or row selection.
     */
    public final OnItemViewSelectedListener getOnItemViewSelectedListener() {
        return mOnItemViewSelectedListener;
    }

    /**
     * Set listener for item click event.  RowPresenter does nothing but subclass of
     * RowPresenter may fire item click event if it does have a concept of item.
     * OnItemViewClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general,  developer should choose one of the listeners but not both.
     */
    public final void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
    }

    /**
     * Set listener for item click event.
     */
    public final OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    /**
     * Freeze/Unfreeze the row, typically used when transition starts/ends.
     */
    public void freeze(ViewHolder holder, boolean freeze) {
    }

}
