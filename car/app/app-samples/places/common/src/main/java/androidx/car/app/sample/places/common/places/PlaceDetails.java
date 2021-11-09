/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.sample.places.common.places;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/** Wraps a {@link PlaceInfo} and adds additional details to it. */
public class PlaceDetails {
    private final PlaceInfo mPlace;
    @Nullable
    private final String mPhoneNumber;
    private final double mRatings;
    private final List<String> mPhotoUrls;
    private final String mIconUrl;

    public PlaceDetails(
            @NonNull PlaceInfo place,
            @Nullable String phoneNumber,
            double ratings,
            @NonNull List<String> photos,
            @NonNull String icon) {
        mPlace = place;
        mPhoneNumber = phoneNumber;
        mRatings = ratings;
        mPhotoUrls = photos;
        mIconUrl = icon;
    }

    @NonNull
    public PlaceInfo getPlace() {
        return mPlace;
    }

    @Nullable
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public double getRatings() {
        return mRatings;
    }

    @NonNull
    public List<String> getPhotoUrls() {
        return mPhotoUrls;
    }

    @NonNull
    public String getIconUrl() {
        return mIconUrl;
    }
}
