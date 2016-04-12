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
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.BaseOnItemViewSelectedListener;
import android.support.v17.leanback.widget.MultiActionsProvider;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Song implements MultiActionsProvider {

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("text") private String mText = "";
    @SerializedName("image") private String mImage = null;
    @SerializedName("file") private String mFile = null;
    @SerializedName("duration") private String mDuration = null;
    @SerializedName("number") private int mNumber = 0;
    @SerializedName("favorite") private boolean mFavorite = false;

    private MultiAction[] mMediaRowActions;


    public void setMediaRowActions(MultiAction[] mediaRowActions) {
        mMediaRowActions = mediaRowActions;
    }

    public MultiAction[] getMediaRowActions() {
        return mMediaRowActions;
    }

    public String getDuration() {
        return mDuration;
    }

    public void setDuration(String duration) {
        mDuration = duration;
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

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite;
    }

    public int getFileResource(Context context) {
        return context.getResources()
                      .getIdentifier(mFile, "raw", context.getPackageName());
    }

    public int getImageResource(Context context) {
        return context.getResources()
                      .getIdentifier(mImage, "drawable", context.getPackageName());
    }

    @Override
    public MultiAction[] getActions() {
        return mMediaRowActions;
    }

}
