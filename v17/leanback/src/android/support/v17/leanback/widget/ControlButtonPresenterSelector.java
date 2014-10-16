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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

/**
 * ControlButtonPresenterSelector displays primary and secondary
 * controls for a {@link PlaybackControlsRow}.
 *
 * Binds to items of type {@link Action}.
 */
public class ControlButtonPresenterSelector extends PresenterSelector {

    private final Presenter mPrimaryPresenter =
            new ControlButtonPresenter(R.layout.lb_control_button_primary);
    private final Presenter mSecondaryPresenter =
            new ControlButtonPresenter(R.layout.lb_control_button_secondary);

    /**
     * Returns the presenter for primary controls.
     */
    public Presenter getPrimaryPresenter() {
        return mPrimaryPresenter;
    }

    /**
     * Returns the presenter for secondary controls.
     */
    public Presenter getSecondaryPresenter() {
        return mSecondaryPresenter;
    }

    /**
     * Always returns the presenter for primary controls.
     */
    public Presenter getPresenter(Object item) {
        return mPrimaryPresenter;
    }

    static class ActionViewHolder extends Presenter.ViewHolder {
        ImageView mIcon;
        View mFocusableView;

        public ActionViewHolder(View view) {
            super(view);
            mIcon = (ImageView) view.findViewById(R.id.icon);
            mFocusableView = view.findViewById(R.id.button);
        }
    }

    static class ControlButtonPresenter extends Presenter {
        private int mLayoutResourceId;

        ControlButtonPresenter(int layoutResourceId) {
            mLayoutResourceId = layoutResourceId;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResourceId, parent, false);
            return new ActionViewHolder(v);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            Action action = (Action) item;
            ActionViewHolder vh = (ActionViewHolder) viewHolder;
            vh.mIcon.setImageDrawable(action.getIcon());
            CharSequence contentDescription = !TextUtils.isEmpty(action.getLabel1()) ?
                action.getLabel1() : action.getLabel2();
            if (!TextUtils.equals(vh.mFocusableView.getContentDescription(), contentDescription)) {
                vh.mFocusableView.setContentDescription(contentDescription);
                vh.mFocusableView.sendAccessibilityEvent(
                        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            }
        }

        @Override
        public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
            ActionViewHolder vh = (ActionViewHolder) viewHolder;
            vh.mIcon.setImageDrawable(null);
            vh.mFocusableView.setContentDescription(null);
        }

        @Override
        public void setOnClickListener(Presenter.ViewHolder viewHolder,
                View.OnClickListener listener) {
            ((ActionViewHolder) viewHolder).mFocusableView.setOnClickListener(listener);
        }
    }
}
