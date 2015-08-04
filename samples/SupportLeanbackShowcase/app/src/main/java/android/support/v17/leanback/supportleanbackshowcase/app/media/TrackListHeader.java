/*
 * Copyright (C) 2015 The Android Open Source Project
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
 *
 */

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TrackListHeader extends Row {

    public static class Presenter extends RowPresenter {

        private final Context mContext;

        public Presenter(Context context) {
            mContext = context;
            setHeaderPresenter(null);
        }

        @Override public boolean isUsingDefaultSelectEffect() {
            return false;
        }

        @Override protected ViewHolder createRowViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.row_track_list_header, parent, false);
            view.setFocusable(false);
            view.setFocusableInTouchMode(false);
            return new ViewHolder(view);
        }
    }


}
