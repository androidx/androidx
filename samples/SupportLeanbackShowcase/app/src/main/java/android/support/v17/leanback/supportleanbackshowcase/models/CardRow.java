/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This class represents a row of cards. In a real world application you might want to store more
 * data than in this example.
 */
public class CardRow {

    // Used to determine whether the row shall use shadows when displaying its cards or not.
    @SerializedName("shadow") private boolean mShadow = true;
    @SerializedName("title") private String mTitle;
    @SerializedName("cards") private List<Card> mCards;

    public String getTitle() {
        return mTitle;
    }

    public boolean useShadow() {
        return mShadow;
    }

    public List<Card> getCards() {
        return mCards;
    }

}
