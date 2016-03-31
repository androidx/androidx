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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.v17.leanback.R;
import android.view.ContextThemeWrapper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Abstract {@link Presenter} class for rendering media items in a playlist format.
 * Media items in the playlist are arranged as a vertical list with each row holding each media's
 * metadata which is provided by the user of this class.
 * <p>
 *     Subclasses must override {@link #onBindMediaViewHolder} to implement their media item model
 *     data binding to each row view
 * </p>
 * <p>
 *     {@link AbstractMediaListHeaderPresenter} can be used in conjunction with this presenter in
 *     order to display a playlist with a header view..
 * </p>
 */
public abstract class AbstractMediaItemPresenter extends RowPresenter {


    private int mBackgroundColor = Color.TRANSPARENT;
    private boolean mBackgroundColorSet;
    private final Context mContext;

    /**
     * Constructor used for creating an abstract media item presenter of a given theme.
     * @param context The context the user of this presenter is running in.
     * @param mThemeResId The resource id of the desired theme used for styling of this presenter.
     */
    public AbstractMediaItemPresenter(Context context, int mThemeResId) {
        mContext = new ContextThemeWrapper(context.getApplicationContext(), mThemeResId);
        setHeaderPresenter(null);
    }

    /**
     * Constructor used for creating an abstract media item presenter.
     * The styling for this presenter is extracted from Context of parent in
     * {@link #createRowViewHolder(ViewGroup)}.
     */
    public AbstractMediaItemPresenter() {
        mContext = null;
        setHeaderPresenter(null);
    }

    /**
     * The ViewHolder for the {@link AbstractMediaItemPresenter}. It references different views
     * that place different meta-data corresponding to a media item.
     */
    public static class ViewHolder extends RowPresenter.ViewHolder {

        private final View mContainerView;
        private final TextView mTrackNumberView;
        private final TextView mTrackNameView;
        private final TextView mTrackDurationView;

        View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getOnItemViewClickedListener() != null) {
                    getOnItemViewClickedListener().onItemClicked(ViewHolder.this,
                            ViewHolder.this.getRowObject(), ViewHolder.this,
                            ViewHolder.this.getRowObject());
                }
            }
        };

        public ViewHolder(View view) {
            super(view);
            mContainerView  = view.findViewById(R.id.rowContainer);
            mContainerView.setOnClickListener(mOnClickListener);
            mTrackNumberView = (TextView) view.findViewById(R.id.trackNumber);
            mTrackNameView = (TextView) view.findViewById(R.id.trackName);
            mTrackDurationView = (TextView) view.findViewById(R.id.trackDuration);
        }

        /**
         * @return The TextView responsible for rendering the track number
         */
        public TextView getTrackNumberView() {
            return mTrackNumberView;
        }

        /**
         * @return The TextView responsible for rendering the track name
         */
        public TextView getTrackNameView() {
            return mTrackNameView;
        }

        /**
         * @return The TextView responsible for rendering the track duration
         */
        public TextView getTrackDurationView() {
            return mTrackDurationView;
        }
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        Context context = mContext != null ? mContext : parent.getContext();
        View view = LayoutInflater.from(context).
                inflate(R.layout.lb_row_media_item, parent, false);
        ViewHolder vh = new ViewHolder(view);
        if (mBackgroundColorSet) {
            vh.mContainerView.setBackgroundColor(mBackgroundColor);
        }
        return new ViewHolder(view);
    }

    @Override
    public boolean isUsingDefaultSelectEffect() {
        return false;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);
        onBindMediaViewHolder((ViewHolder) vh, item);
    }

    /**
     * Sets the background color for the row views within the playlist.
     * If this is not set, a default color, defaultBrandColor, from theme is used.
     * This defaultBrandColor defaults to android:attr/colorPrimary on v21, if it's specified.
     * @param color The ARGB color used to set as the media list background color.
     */
    public void setBackgroundColor(int color) {
        mBackgroundColorSet = true;
        mBackgroundColor = color;
    }

    /**
     * Binds the media data model provided by the user to the {@link ViewHolder} provided by the
     * {@link AbstractMediaItemPresenter}.
     * The subclasses of this presenter can access and bind individual views for TrackNumber,
     * TrackName, and TrackDuration, by calling {@link ViewHolder#getTrackNumberView},
     * {@link ViewHolder#getTrackNameView}, and {@link ViewHolder#getTrackDurationView}, on the
     * {@link ViewHolder} provided as the argument {@code vh} by this presenter.
     *
     * @param vh The ViewHolder for this {@link AbstractMediaItemPresenter}.
     * @param item The media item data object being presented.
     */
    protected abstract void onBindMediaViewHolder(ViewHolder vh, Object item);

}