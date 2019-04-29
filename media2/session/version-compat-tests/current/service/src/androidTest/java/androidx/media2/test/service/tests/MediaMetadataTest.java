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

package androidx.media2.test.service.tests;

import static androidx.media2.common.MediaMetadata.METADATA_KEY_RATING;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;

import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaMetadata.Builder;
import androidx.media2.common.Rating;
import androidx.media2.session.HeartRating;
import androidx.media2.session.MediaUtils;
import androidx.media2.session.ThumbRating;
import androidx.media2.test.common.TestUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@MediumTest
public class MediaMetadataTest {
    @Test
    public void testBuilder() {
        final String title = "title";
        final long discNumber = 10;
        final Rating rating = new ThumbRating(true);

        Builder builder = new Builder();
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
        builder.putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, discNumber);
        builder.putRating(MediaMetadata.METADATA_KEY_USER_RATING, rating);

        MediaMetadata metadata = builder.build();
        assertEquals(title, metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
        assertEquals(discNumber, metadata.getLong(MediaMetadata.METADATA_KEY_DISC_NUMBER));
        assertEquals(rating, metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING));
    }

    @Test
    public void testSetExtra() {
        final Bundle extras = new Bundle();
        extras.putString("MediaMetadataTest", "testBuilder");

        Builder builder = new Builder();
        try {
            builder.putLong(MediaMetadata.METADATA_KEY_EXTRAS, 1);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        builder.setExtras(extras);
        MediaMetadata metadata = builder.build();
        assertTrue(TestUtils.equals(extras, metadata.getExtras()));
    }

    @Test
    public void testParcelingWithSmallBitmaps() {
        final int bitmapCount = 100;
        final List<String> keyList = new ArrayList<>(bitmapCount);
        final String bitmapKeyPrefix = "bitmap_";
        for (int i = 0; i < bitmapCount; i++) {
            keyList.add(bitmapKeyPrefix + i);
        }

        // A small bitmap about 160kB.
        final int originalWidth = 200;
        final int originalHeight = 200;
        Bitmap testBitmap = Bitmap.createBitmap(
                originalWidth, originalHeight, Bitmap.Config.ARGB_8888);

        Builder builder = new Builder();
        for (int i = 0; i < keyList.size(); i++) {
            builder.putBitmap(keyList.get(i), testBitmap);
        }

        MediaMetadata metadata = builder.build();
        ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(metadata);

        // Bitmaps will not be scaled down since they are small.
        Parcel parcel = Parcel.obtain();
        parcelImpl.writeToParcel(parcel, 0 /* flags */);
        parcel.setDataPosition(0);

        MediaMetadata metadataFromParcel =
                ParcelUtils.fromParcelable(ParcelImpl.CREATOR.createFromParcel(parcel));

        // Check the bitmap list from the metadata.
        Set<String> keySet = metadataFromParcel.keySet();
        assertTrue(keySet.containsAll(keyList));
        assertTrue(keyList.containsAll(keySet));

        for (String key : keySet) {
            Bitmap bitmap = metadataFromParcel.getBitmap(key);
            assertNotNull(bitmap);
            int newWidth = bitmap.getWidth();
            int newHeight = bitmap.getHeight();
            // The bitmaps should not have been scaled down.
            assertEquals(newWidth, originalWidth);
            assertEquals(newHeight, originalHeight);
        }
    }

    @Test
    public void testParcelingWithLargeBitmaps() {
        final int bitmapCount = 100;
        final List<String> keyList = new ArrayList<>(bitmapCount);
        final String bitmapKeyPrefix = "bitmap_";
        for (int i = 0; i < bitmapCount; i++) {
            keyList.add(bitmapKeyPrefix + i);
        }

        // A large bitmap (64MB) which exceeds the binder limit.
        final int originalWidth = 4096;
        final int originalHeight = 4096;
        Bitmap testBitmap = Bitmap.createBitmap(
                originalWidth, originalHeight, Bitmap.Config.ARGB_8888);

        Builder builder = new Builder();
        for (int i = 0; i < keyList.size(); i++) {
            builder.putBitmap(keyList.get(i), testBitmap);
        }

        MediaMetadata metadata = builder.build();
        ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(metadata);

        // Bitmaps will be scaled down when the metadata is written to parcel.
        Parcel parcel = Parcel.obtain();
        parcelImpl.writeToParcel(parcel, 0 /* flags */);
        parcel.setDataPosition(0);

        MediaMetadata metadataFromParcel =
                ParcelUtils.fromParcelable(ParcelImpl.CREATOR.createFromParcel(parcel));

        // Check the bitmap list from the metadata.
        Set<String> keySet = metadataFromParcel.keySet();
        assertTrue(keySet.containsAll(keyList));
        assertTrue(keyList.containsAll(keySet));

        for (String key : keySet) {
            Bitmap bitmap = metadataFromParcel.getBitmap(key);
            assertNotNull(bitmap);
            int newWidth = bitmap.getWidth();
            int newHeight = bitmap.getHeight();
            assertTrue("Resulting bitmap (size=" + newWidth + "x" + newHeight + ") was not "
                            + "scaled down. ",
                    newWidth < originalWidth && newHeight < originalHeight);
        }
    }

    @Test
    public void testMediaUtils_convertToMediaMetadataCompat() {
        HeartRating testRating = new HeartRating(true);
        long testState = MediaMetadata.STATUS_DOWNLOADING;
        String testCustomKey = "android.media.test";
        String testCustomValue = "customValue";
        MediaMetadata testMetadata = new Builder()
                .putRating(METADATA_KEY_RATING, testRating)
                .putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, testState)
                .putString(testCustomKey, testCustomValue)
                .build();

        MediaMetadataCompat compat = MediaUtils.convertToMediaMetadataCompat(testMetadata);
        assertEquals(3, compat.keySet().size());
        RatingCompat returnedRating = compat.getRating(MediaMetadataCompat.METADATA_KEY_RATING);
        assertEquals(RatingCompat.RATING_HEART, returnedRating.getRatingStyle());
        assertTrue(returnedRating.hasHeart());
        assertEquals(MediaDescriptionCompat.STATUS_DOWNLOADING,
                compat.getLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS));
        assertEquals(testCustomValue, compat.getString(testCustomKey));
    }

    @Test
    public void testMediaUtils_convertToMediaItem() {
        RatingCompat testRating = RatingCompat.newHeartRating(true);
        long testState = MediaDescriptionCompat.STATUS_DOWNLOADING;
        String testCustomKey = "android.media.test";
        String testCustomValue = "customValue";
        MediaMetadataCompat testMetadataCompat = new MediaMetadataCompat.Builder()
                .putRating(MediaMetadataCompat.METADATA_KEY_RATING, testRating)
                .putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, testState)
                .putString(testCustomKey, testCustomValue)
                .build();

        MediaItem item = MediaUtils.convertToMediaItem(testMetadataCompat);
        Rating returnedRating = item.getMetadata().getRating(METADATA_KEY_RATING);
        assertTrue(returnedRating instanceof HeartRating);
        assertTrue(returnedRating.isRated());
        assertEquals(testRating.hasHeart(), ((HeartRating) returnedRating).hasHeart());
        assertEquals(MediaMetadata.STATUS_DOWNLOADING,
                item.getMetadata().getLong(MediaMetadata.METADATA_KEY_DOWNLOAD_STATUS));
        assertFalse(item.getMetadata().containsKey(
                MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS));
        assertEquals(testCustomValue, item.getMetadata().getString(testCustomKey));
    }
}
