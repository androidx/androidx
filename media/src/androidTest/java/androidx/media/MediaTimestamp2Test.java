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

import android.media.MediaTimestamp;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link MediaTimestamp2}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaTimestamp2Test extends MediaTestBase {

    @Test
    public void testConstructor() {
        MediaTimestamp2 timestamp = new MediaTimestamp2(123, 456, 0.25f);
        assertEquals(123, timestamp.getAnchorMediaTimeUs());
        assertEquals(456, timestamp.getAnchorSytemNanoTime());
        assertEquals(0.25f, timestamp.getMediaClockRate());
    }

    @Test
    public void testVoidConstructor() {
        MediaTimestamp2 timestamp = new MediaTimestamp2();
        assertEquals(0, timestamp.getAnchorMediaTimeUs());
        assertEquals(0, timestamp.getAnchorSytemNanoTime());
        assertEquals(1.0f, timestamp.getMediaClockRate());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testTimestampUnknown() {
        assertEquals(MediaTimestamp.TIMESTAMP_UNKNOWN.getAnchorMediaTimeUs(),
                MediaTimestamp2.TIMESTAMP_UNKNOWN.getAnchorMediaTimeUs());
        assertEquals(MediaTimestamp.TIMESTAMP_UNKNOWN.getAnchorSytemNanoTime(),
                MediaTimestamp2.TIMESTAMP_UNKNOWN.getAnchorSytemNanoTime());
        assertEquals(MediaTimestamp.TIMESTAMP_UNKNOWN.getMediaClockRate(),
                MediaTimestamp2.TIMESTAMP_UNKNOWN.getMediaClockRate());
    }
}
