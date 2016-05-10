/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.media;

import android.media.Rating;

class RatingCompatApi21 {
    public static Object newUnratedRating(int ratingStyle) {
        return Rating.newUnratedRating(ratingStyle);
    }

    public static Object newHeartRating(boolean hasHeart) {
        return Rating.newHeartRating(hasHeart);
    }

    public static Object newThumbRating(boolean thumbIsUp) {
        return Rating.newThumbRating(thumbIsUp);
    }

    public static Object newStarRating(int starRatingStyle, float starRating) {
        return Rating.newStarRating(starRatingStyle, starRating);
    }

    public static Object newPercentageRating(float percent) {
        return Rating.newPercentageRating(percent);
    }

    public static boolean isRated(Object ratingObj) {
        return ((Rating)ratingObj).isRated();
    }

    public static int getRatingStyle(Object ratingObj) {
        return ((Rating)ratingObj).getRatingStyle();
    }

    public static boolean hasHeart(Object ratingObj) {
        return ((Rating)ratingObj).hasHeart();
    }

    public static boolean isThumbUp(Object ratingObj) {
        return ((Rating)ratingObj).isThumbUp();
    }

    public static float getStarRating(Object ratingObj) {
        return ((Rating)ratingObj).getStarRating();
    }

    public static float getPercentRating(Object ratingObj) {
        return ((Rating)ratingObj).getPercentRating();
    }
}
