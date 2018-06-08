/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.tvprovider.media.tv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.media.tv.TvContentRating;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class TvContractUtilsTest {

    @Test
    public void testStringToContentRatings_nullInput() {
        assertArrayEquals(TvContractUtils.EMPTY, TvContractUtils.stringToContentRatings(null));
    }

    @Test
    public void testStringToContentRatings_emptyInput() {
        assertArrayEquals(TvContractUtils.EMPTY, TvContractUtils.stringToContentRatings(""));
    }

    @Test
    public void testStringToContentRatings_singleRating() {
        TvContentRating[] ratings = new TvContentRating[1];
        ratings[0] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D",
                "US_TV_L",
                "US_TV_S",
                "US_TV_V");
        assertArrayEquals(ratings, TvContractUtils.stringToContentRatings(
                "com.android.tv/US_TV/US_TV_PG/US_TV_D/US_TV_L/US_TV_S/US_TV_V"));
    }

    @Test
    public void testStringToContentRatings_multipleRatings() {
        TvContentRating[] ratings = new TvContentRating[3];
        ratings[0] = TvContentRating.createRating(
                "com.android.tv",
                "US_MV",
                "US_MV_NC17");
        ratings[1] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_Y7");
        ratings[2] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D",
                "US_TV_L",
                "US_TV_S",
                "US_TV_V");
        assertArrayEquals(ratings, TvContractUtils.stringToContentRatings(
                "com.android.tv/US_MV/US_MV_NC17,"
                        + "com.android.tv/US_TV/US_TV_Y7,"
                        + "com.android.tv/US_TV/US_TV_PG/US_TV_D/US_TV_L/US_TV_S/US_TV_V"));
    }

    @Test
    public void testStringToContentRatings_allRatingsInvalid() {
        assertArrayEquals(TvContractUtils.EMPTY, TvContractUtils.stringToContentRatings(
                "com.android.tv/US_MV," // Invalid
                        + "com.android.tv")); // Invalid
    }

    @Test
    public void testStringToContentRatings_someRatingsInvalid() {
        TvContentRating[] ratings = new TvContentRating[1];
        ratings[0] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D",
                "US_TV_L",
                "US_TV_S",
                "US_TV_V");
        assertArrayEquals(ratings, TvContractUtils.stringToContentRatings(
                "com.android.tv/US_MV," // Invalid
                        + "com.android.tv/US_TV/US_TV_PG/US_TV_D/US_TV_L/US_TV_S/US_TV_V," // Valid
                        + "com.android.tv")); // Invalid
    }

    @Test
    public void testContentRatingsToString_nullInput() {
        assertEquals(null, TvContractUtils.contentRatingsToString(null));
    }

    @Test
    public void testContentRatingsToString_emptyInput() {
        assertEquals(null, TvContractUtils.contentRatingsToString(new TvContentRating[0]));
    }

    @Test
    public void testContentRatingsToString_singleRating() {
        TvContentRating[] ratings = new TvContentRating[1];
        ratings[0] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D",
                "US_TV_L",
                "US_TV_S",
                "US_TV_V");
        assertEquals("com.android.tv/US_TV/US_TV_PG/US_TV_D/US_TV_L/US_TV_S/US_TV_V",
                TvContractUtils.contentRatingsToString(ratings));
    }

    @Test
    public void testContentRatingsToString_multipleRatings() {
        TvContentRating[] ratings = new TvContentRating[3];
        ratings[0] = TvContentRating.createRating(
                "com.android.tv",
                "US_MV",
                "US_MV_NC17");
        ratings[1] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D",
                "US_TV_L",
                "US_TV_S",
                "US_TV_V");
        ratings[2] = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_Y7");
        String ratingString = "com.android.tv/US_MV/US_MV_NC17,"
                + "com.android.tv/US_TV/US_TV_PG/US_TV_D/US_TV_L/US_TV_S/US_TV_V,"
                + "com.android.tv/US_TV/US_TV_Y7";
        assertEquals(ratingString, TvContractUtils.contentRatingsToString(ratings));
    }
}
