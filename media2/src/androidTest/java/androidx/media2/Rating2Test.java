/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.os.Parcel;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelImpl;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link Rating2} and its subclasses.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class Rating2Test extends MediaTestBase {
    @Test
    public void testUnratedHeartRating2() {
        HeartRating2 rating2 = new HeartRating2();
        assertEquals(false, rating2.isRated());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedHeartRating2() {
        final boolean hasHeart = true;
        HeartRating2 rating2 = new HeartRating2(hasHeart);
        assertEquals(true, rating2.isRated());
        assertEquals(hasHeart, rating2.hasHeart());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedPercentageRating2() {
        PercentageRating2 rating2 = new PercentageRating2();
        assertEquals(false, rating2.isRated());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedPercentageRating2() {
        double delta = 0.000001;
        float percentage = 20.5f;
        PercentageRating2 rating2 = new PercentageRating2(percentage);
        assertEquals(true, rating2.isRated());
        assertEquals(percentage, rating2.getPercentRating(), delta);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedThumbRating2() {
        ThumbRating2 rating2 = new ThumbRating2();
        assertEquals(false, rating2.isRated());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedThumbRating2() {
        boolean isThumbUp = true;
        ThumbRating2 rating2 = new ThumbRating2(isThumbUp);
        assertEquals(true, rating2.isRated());
        assertEquals(isThumbUp, rating2.isThumbUp());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testUnratedStarRating2() {
        int maxStars = 5;
        StarRating2 rating2 = new StarRating2(maxStars);
        assertEquals(false, rating2.isRated());
        assertEquals(maxStars, rating2.getMaxStars());
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    @Test
    public void testRatedStarRating2() {
        double delta = 0.000001;
        int maxStars = 5;
        float starRating = 3.1f;
        StarRating2 rating2 = new StarRating2(maxStars, starRating);
        assertEquals(true, rating2.isRated());
        assertEquals(maxStars, rating2.getMaxStars());
        assertEquals(starRating, rating2.getStarRating(), delta);
        assertEquals(rating2, writeToParcelAndCreateRating2(rating2));
    }

    private Rating2 writeToParcelAndCreateRating2(Rating2 rating2) {
        ParcelImpl parcelImpl = MediaUtils2.toParcelable(rating2);
        Parcel parcel = Parcel.obtain();
        parcelImpl.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ParcelImpl newParcelImpl = ParcelImpl.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return MediaUtils2.fromParcelable(newParcelImpl);
    }
}
