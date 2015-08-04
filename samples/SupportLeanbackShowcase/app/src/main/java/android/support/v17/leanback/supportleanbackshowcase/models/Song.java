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

package android.support.v17.leanback.supportleanbackshowcase.models;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

public class Song extends Row {

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("text") private String mText = "";
    @SerializedName("image") private String mImage = null;
    @SerializedName("file") private String mFile = null;
    @SerializedName("duration") private String mDuration = null;
    @SerializedName("number") private int mNumber = 0;


    public String getDuration() {
        return mDuration;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getText() {
        return mText;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getFileResource(Context context) {
        return context.getResources()
                      .getIdentifier(mFile, "raw", context.getPackageName());
    }

    public int getImageResource(Context context) {
        return context.getResources()
                      .getIdentifier(mImage, "drawable", context.getPackageName());
    }

    public interface OnSongRowClickListener {

        void onSongRowClicked(Song song);

    }

    public static class Presenter extends RowPresenter implements View.OnClickListener {

        private final Context mContext;
        private OnSongRowClickListener mClickListener;


        public Presenter(Context context) {
            mContext = context;
            setHeaderPresenter(null);
        }

        public void setOnClickListener(OnSongRowClickListener listener) {
            mClickListener = listener;
        }

        @Override protected ViewHolder createRowViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.row_song, parent, false);
            view.findViewById(R.id.rowContainer).setOnClickListener(this);
            return new ViewHolder(view);
        }

        @Override public boolean isUsingDefaultSelectEffect() {
            return false;
        }

        @Override protected void onBindRowViewHolder(ViewHolder vh, Object item) {
            super.onBindRowViewHolder(vh, item);
            Song song = (Song) item;
            ((TextView) vh.view.findViewById(R.id.trackNumber)).setText("" + song.getNumber());
            ((TextView) vh.view.findViewById(R.id.trackDuration)).setText(song.getDuration());
            String text = song.getTitle() + " / " + song.getDescription();
            ((TextView) vh.view.findViewById(R.id.trackName)).setText(text);
            vh.view.findViewById(R.id.rowContainer).setTag(item);
        }

        @Override public void onClick(View v) {
            if (mClickListener == null) return;
            mClickListener.onSongRowClicked((Song) v.getTag());
        }
    }
}
