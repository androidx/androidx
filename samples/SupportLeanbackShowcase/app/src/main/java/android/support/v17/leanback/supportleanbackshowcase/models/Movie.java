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
 */

package android.support.v17.leanback.supportleanbackshowcase.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Movie implements Serializable {

    private static final long serialVersionUID = 133742L;

    @SerializedName("title")
    private String mTitle = "";
    @SerializedName("price_hd")
    private String mPriceHd = "n/a";
    @SerializedName("price_sd")
    private String mPriceSd = "n/a";
    @SerializedName("breadcrump")
    private String mBreadcrump = "";

    public String getTitle() {
        return mTitle;
    }

    public String getBreadcrump() {
        return mBreadcrump;
    }

    public String getPriceHd() {
        return mPriceHd;
    }

    public String getPriceSd() {
        return mPriceSd;
    }

}
