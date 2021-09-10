/*
 * Copyright 2021 The Android Open Source Project
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

package android.support.mediacompat.client;

import static android.support.mediacompat.testlib.util.TestUtil.assertBundleEquals;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;

/** Tests for {@link MediaDescriptionCompat}. */
@SmallTest
public class MediaDescriptionCompatTest {

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void roundTripViaFrameworkObject_returnsEqualMediaUriAndExtras() {
        Uri mediaUri = Uri.parse("androidx://media/uri");
        MediaDescriptionCompat originalDescription = new MediaDescriptionCompat.Builder()
                .setMediaUri(mediaUri)
                .setExtras(createExtras())
                .build();

        MediaDescriptionCompat restoredDescription = MediaDescriptionCompat.fromMediaDescription(
                originalDescription.getMediaDescription());

        assertEquals(mediaUri, restoredDescription.getMediaUri());
        assertBundleEquals(createExtras(), restoredDescription.getExtras());
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void getMediaDescription_withMediaUri_doesNotTouchExtras() {
        MediaDescriptionCompat originalDescription = new MediaDescriptionCompat.Builder()
                .setMediaUri(Uri.EMPTY)
                .setExtras(createExtras())
                .build();
        originalDescription.getMediaDescription();
        assertBundleEquals(createExtras(), originalDescription.getExtras());
    }

    private static Bundle createExtras() {
        Bundle extras = new Bundle();
        extras.putString("key1", "value1");
        extras.putString("key2", "value2");
        return extras;
    }
}
