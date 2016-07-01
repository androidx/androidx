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

package android.support.v17.leanback.supportleanbackshowcase.models;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This is a generic example of a custom data object, containing info we might want to keep with
 * each card on the home screen
 */
public class Card {

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("extraText") private String mExtraText = "";
    @SerializedName("imageUrl") private String mImageUrl;
    @SerializedName("footerColor") private String mFooterColor = null;
    @SerializedName("selectedColor") private String mSelectedColor = null;
    @SerializedName("localImageResource") private String mLocalImageResource = null;
    @SerializedName("footerIconLocalImageResource") private String mFooterResource = null;
    @SerializedName("type") private Card.Type mType;
    @SerializedName("id") private int mId;
    @SerializedName("width") private int mWidth;
    @SerializedName("height") private int mHeight;

    public String getTitle() {
        return mTitle;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getId() {
        return mId;
    }

    public Card.Type getType() {
        return mType;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getExtraText() {
        return mExtraText;
    }

    public int getFooterColor() {
        if (mFooterColor == null) return -1;
        return Color.parseColor(mFooterColor);
    }

    public int getSelectedColor() {
        if (mSelectedColor == null) return -1;
        return Color.parseColor(mSelectedColor);
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public URI getImageURI() {
        if (getImageUrl() == null) return null;
        try {
            return new URI(getImageUrl());
        } catch (URISyntaxException e) {
            Log.d("URI exception: ", getImageUrl());
            return null;
        }
    }

    public int getLocalImageResourceId(Context context) {
        return context.getResources().getIdentifier(getLocalImageResourceName(), "drawable",
                                                    context.getPackageName());
    }

    public String getLocalImageResourceName() {
        return mLocalImageResource;
    }

    public String getFooterLocalImageResourceName() {
        return mFooterResource;
    }

    public enum Type {

        MOVIE_COMPLETE,
        MOVIE,
        MOVIE_BASE,
        ICON,
        SQUARE_BIG,
        SINGLE_LINE,
        GAME,
        SQUARE_SMALL,
        DEFAULT,
        SIDE_INFO,
        SIDE_INFO_TEST_1,
        TEXT,
        CHARACTER,
        GRID_SQUARE

    }

}
