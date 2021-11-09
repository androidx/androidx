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

/** Describes a place category. */
public class PlaceCategory {
    private final String mDisplayName;
    private final String mCategory;

    /** Create an instance of {@link PlaceCategory}. */
    @NonNull
    public static PlaceCategory create(@NonNull String displayName, @NonNull String category) {
        return new PlaceCategory(displayName, category);
    }

    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }

    @NonNull
    public String getCategory() {
        return mCategory;
    }

    private PlaceCategory(String displayName, String category) {
        mDisplayName = displayName;
        mCategory = category;
    }
}
