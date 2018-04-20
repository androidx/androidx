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

package androidx.media;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import android.os.Build;
import android.os.Bundle;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.MediaMetadata2.Builder;

import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaMetadata2Test {
    @Test
    public void testBuilder() {
        final Bundle extras = new Bundle();
        extras.putString("MediaMetadata2Test", "testBuilder");
        final String title = "title";
        final long discNumber = 10;
        final Rating2 rating = Rating2.newThumbRating(true);

        Builder builder = new Builder();
        builder.setExtras(extras);
        builder.putString(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, title);
        builder.putLong(MediaMetadata2.METADATA_KEY_DISC_NUMBER, discNumber);
        builder.putRating(MediaMetadata2.METADATA_KEY_USER_RATING, rating);

        MediaMetadata2 metadata = builder.build();
        assertTrue(TestUtils.equals(extras, metadata.getExtras()));
        assertEquals(title, metadata.getString(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE));
        assertEquals(discNumber, metadata.getLong(MediaMetadata2.METADATA_KEY_DISC_NUMBER));
        assertEquals(rating, metadata.getRating(MediaMetadata2.METADATA_KEY_USER_RATING));
    }
}
