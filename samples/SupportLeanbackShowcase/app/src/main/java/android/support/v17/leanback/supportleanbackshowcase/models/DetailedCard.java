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
import android.support.v17.leanback.supportleanbackshowcase.models.Card;

import com.google.gson.annotations.SerializedName;

public class DetailedCard {

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("text") private String mText = "";
    @SerializedName("localImageResource") private String mLocalImageResource = null;
    @SerializedName("price") private String mPrice = null;
    @SerializedName("characters") private Card[] mCharacters = null;
    @SerializedName("recommended") private Card[] mRecommended = null;
    @SerializedName("year") private int mYear = 0;


    public String getPrice() {
        return mPrice;
    }

    public int getYear() {
        return mYear;
    }

    public String getLocalImageResource() {
        return mLocalImageResource;
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

    public Card[] getCharacters() {
        return mCharacters;
    }

    public Card[] getRecommended() {
        return mRecommended;
    }

    public int getLocalImageResourceId(Context context) {
        return context.getResources()
                      .getIdentifier(getLocalImageResource(), "drawable", context.getPackageName());
    }
}
