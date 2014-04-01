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

import android.support.v17.leanback.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An abstract {@link Presenter} for rendering a detailed description of an
 * item. Typically this Presenter will be used in a DetailsOveriewRowPresenter.
 *
 * <p>Subclasses will override {@link #onBindDescription} to implement the data
 * binding for this Presenter.
 */
public abstract class AbstractDetailsDescriptionPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final TextView mBody;

        public ViewHolder(View view) {
            super(view);
            mTitle = (TextView) view.findViewById(R.id.lb_details_description_title);
            mSubtitle = (TextView) view.findViewById(R.id.lb_details_description_subtitle);
            mBody = (TextView) view.findViewById(R.id.lb_details_description_body);
        }

        public TextView getTitle() {
            return mTitle;
        }

        public TextView getSubtitle() {
            return mSubtitle;
        }

        public TextView getBody() {
            return mBody;
        }
    }

    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.lb_details_description, parent, false);
        return new ViewHolder(v);
    }

    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        DetailsOverviewRow row = (DetailsOverviewRow) item;
        onBindDescription(vh, row.getItem());
    }

    /**
     * Binds the data from the item referenced in the DetailsOverviewRow to the
     * ViewHolder.
     *
     * @param vh The ViewHolder for this details description view.
     * @param row The item from the DetailsOverviewRow being presented.
     */
    protected abstract void onBindDescription(ViewHolder vh, Object item);

    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}
}
