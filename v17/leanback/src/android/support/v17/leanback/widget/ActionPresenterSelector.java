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
import android.widget.ImageView;
import android.widget.TextView;

class ActionPresenterSelector extends PresenterSelector {

    private Presenter mOneLineActionPresenter = new OneLineActionPresenter();
    private Presenter mTwoLineActionPresenter = new TwoLineActionPresenter();

    @Override
    public Presenter getPresenter(Object item) {
        Action action = (Action) item;
        if (TextUtils.isEmpty(action.getLabel2())) {
            return mOneLineActionPresenter;
        } else {
            return mTwoLineActionPresenter;
        }
    }

    static class OneLineActionPresenter extends Presenter {
        public static class ViewHolder extends Presenter.ViewHolder {
            TextView mLabel;

            public ViewHolder(View view) {
                super(view);
                mLabel = (TextView) view.findViewById(R.id.lb_action_text);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_action_1_line, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            Action action = (Action) item;
            ViewHolder vh = (ViewHolder) viewHolder;

            vh.mLabel.setText(action.getLabel1());
        }

        @Override
        public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}
    }

    static class TwoLineActionPresenter extends Presenter {
        public static class ViewHolder extends Presenter.ViewHolder {
            ImageView mIconView;
            TextView mLabel;

            public ViewHolder(View view) {
                super(view);
                mIconView = (ImageView) view.findViewById(R.id.lb_action_icon);
                mLabel = (TextView) view.findViewById(R.id.lb_action_text_line);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_action_2_lines, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            Action action = (Action) item;
            ViewHolder vh = (ViewHolder) viewHolder;

            int horizontalPadding = vh.view.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.lb_action_1_line_padding_left);
            if (action.getIcon() != null) {
                vh.view.setPadding(0, 0, horizontalPadding, 0);
                vh.mIconView.setVisibility(View.VISIBLE);
                // TODO: scale this?
                vh.mIconView.setImageDrawable(action.getIcon());
            } else {
                vh.view.setPadding(horizontalPadding, 0, horizontalPadding, 0);
                vh.mIconView.setVisibility(View.GONE);
            }
            CharSequence line1 = action.getLabel1();
            CharSequence line2 = action.getLabel2();
            if (TextUtils.isEmpty(line1)) {
                vh.mLabel.setText(line2);
            } else if (TextUtils.isEmpty(line2)) {
                vh.mLabel.setText(line1);
            } else {
                vh.mLabel.setText(line1 + "\n" + line2);
            }
        }

        @Override
        public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.mIconView.setVisibility(View.GONE);
            vh.view.setPadding(0, 0, 0, 0);
        }
    }
}
