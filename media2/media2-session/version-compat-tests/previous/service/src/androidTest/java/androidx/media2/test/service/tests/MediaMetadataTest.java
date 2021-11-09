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
import static androidx.media2.common.MediaMetadata.METADATA_KEY_USER_RATING;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.Color;
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
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class MediaMetadataTest {
    @Test
    public void builder() {
        final String title = "title";
        final long discNumber = 10;
        final Rating rating = new ThumbRating(true);

        Builder builder = new Builder();
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
        builder.putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, discNumber);
        builder.putRating(METADATA_KEY_USER_RATING, rating);

        MediaMetadata metadata = builder.build();
        assertEquals(title, metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
        assertEquals(discNumber, metadata.getLong(MediaMetadata.METADATA_KEY_DISC_NUMBER));
        assertEquals(rating, metadata.getRating(METADATA_KEY_USER_RATING));
    }

    @Test
    public void setExtra() {
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
    public void parcelling_withSmallBitmap_bitmapPreservedAfterParcelled() {
        // A small bitmap (160kB) that doesn't need to be scaled down.
        final int testBitmapSize = 200;
        Bitmap testBitmap = Bitmap.createBitmap(
                testBitmapSize, testBitmapSize, Bitmap.Config.ARGB_8888);
        testBitmap.setPixel(2, 2, Color.GREEN);
        String testKey = MediaMetadata.METADATA_KEY_ALBUM_ART;
        MediaMetadata metadata = new Builder().putBitmap(testKey, testBitmap).build();

        Parcel parcel = Parcel.obtain();
        try {
            // Test twice to ensure internal cache works correctly.
            for (int i = 0; i < 2; i++) {
                ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(metadata);
                parcelImpl.writeToParcel(parcel, 0 /* flags */);
                parcel.setDataPosition(0);

                MediaMetadata metadataFromParcel =
                        ParcelUtils.fromParcelable(ParcelImpl.CREATOR.createFromParcel(parcel));
                assertEquals(testBitmap, metadata.getBitmap(testKey));
                assertEquals(testBitmapSize, testBitmap.getHeight());
                assertEquals(testBitmapSize, testBitmap.getWidth());
            }
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void parcelling_withLargeBitmap_bitmapPreservedAfterParcelled() {
        // A large bitmap (4MB) which exceeds the binder limit. Scaling down would happen.
        final int testBitmapSize = 1024;
        Bitmap testBitmap = Bitmap.createBitmap(
                testBitmapSize, testBitmapSize, Bitmap.Config.ARGB_8888);
        testBitmap.setPixel(2, 2, Color.GREEN);
        String testKey = MediaMetadata.METADATA_KEY_ALBUM_ART;
        MediaMetadata metadata = new Builder().putBitmap(testKey, testBitmap).build();

        Parcel parcel = Parcel.obtain();
        try {
            // Test twice to ensure internal cache works correctly.
            for (int i = 0; i < 2; i++) {
                ParcelImpl parcelImpl = (ParcelImpl) ParcelUtils.toParcelable(metadata);
                parcelImpl.writeToParcel(parcel, 0 /* flags */);
                parcel.setDataPosition(0);

                MediaMetadata metadataFromParcel =
                        ParcelUtils.fromParcelable(ParcelImpl.CREATOR.createFromParcel(parcel));
                assertEquals(testBitmap, metadata.getBitmap(testKey));
                assertEquals(testBitmapSize, testBitmap.getHeight());
                assertEquals(testBitmapSize, testBitmap.getWidth());
            }
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void parceling_withSmallBitmaps() {
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
        try {
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
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void parceling_withLargeBitmaps() {
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
        try {
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
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void mediaUtils_convertToMediaMetadataCompat() {
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
    public void mediaUtils_convertToMediaItem_withoutUserRating() {
        RatingCompat testRating = RatingCompat.newHeartRating(true);
        long testState = MediaDescriptionCompat.STATUS_DOWNLOADING;
        String testCustomKey = "android.media.test";
        String testCustomValue = "customValue";
        MediaMetadataCompat testMetadataCompat = new MediaMetadataCompat.Builder()
                .putRating(MediaMetadataCompat.METADATA_KEY_RATING, testRating)
                .putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, testState)
                .putString(testCustomKey, testCustomValue)
                .build();

        MediaItem item = MediaUtils.convertToMediaItem(
                testMetadataCompat, RatingCompat.RATING_HEART);
        Rating returnedRating = item.getMetadata().getRating(METADATA_KEY_RATING);
        assertTrue(returnedRating instanceof HeartRating);
        assertTrue(returnedRating.isRated());
        assertEquals(testRating.hasHeart(), ((HeartRating) returnedRating).hasHeart());
        Rating returnedUserRating = item.getMetadata().getRating(METADATA_KEY_USER_RATING);
        assertTrue(returnedUserRating instanceof HeartRating);
        assertFalse(returnedUserRating.isRated());
        assertEquals(MediaMetadata.STATUS_DOWNLOADING,
                item.getMetadata().getLong(MediaMetadata.METADATA_KEY_DOWNLOAD_STATUS));
        assertFalse(item.getMetadata().containsKey(
                MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS));
        assertEquals(testCustomValue, item.getMetadata().getString(testCustomKey));
    }

    @Test
    public void mediaUtils_convertToMediaItem_withUserRating() {
        RatingCompat testRating = RatingCompat.newHeartRating(true);
        MediaMetadataCompat testMetadataCompat = new MediaMetadataCompat.Builder()
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING, testRating)
                .build();

        MediaItem item = MediaUtils.convertToMediaItem(
                testMetadataCompat, RatingCompat.RATING_HEART);
        Rating returnedUserRating = item.getMetadata().getRating(METADATA_KEY_USER_RATING);
        assertTrue(returnedUserRating instanceof HeartRating);
        assertTrue(returnedUserRating.isRated());
        assertTrue(((HeartRating) returnedUserRating).hasHeart());
    }
}
