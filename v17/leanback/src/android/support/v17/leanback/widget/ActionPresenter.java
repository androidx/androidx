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

class ActionPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView tv = (TextView) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.lb_action, parent, false);
        return new ViewHolder(tv);
    }

    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Action action = (Action) item;
        ViewHolder vh = (ViewHolder) viewHolder;
        ((TextView) vh.view).setText(action.getLabel());
    }

    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}
}
