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

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * Tests {@link TimedMetaData}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TimedMetaDataTest extends MediaTestBase {

    @Test
    public void testConstructor() {
        byte[] meta = new byte[] { 0x42, 0x4f };
        TimedMetaData timedMeta = new TimedMetaData(123, meta);
        assertEquals(123, timedMeta.getTimestamp());
        assertTrue(Arrays.equals(meta, timedMeta.getMetaData()));
    }
}
