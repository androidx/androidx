/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.LocusId;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LocusIdCompatTest {

    private static final String ID = "Chat_A_B";

    @Test
    public void testConstructor_defaultValues() {
        final LocusIdCompat locusId = new LocusIdCompat(ID);
        assertEquals(ID, locusId.getId());
    }

    @Test
    public void testEquals_defaultValues() {
        final LocusIdCompat locusIdA = new LocusIdCompat(ID);
        final LocusIdCompat locusIdB = new LocusIdCompat(ID);
        final LocusIdCompat locusIdC = new LocusIdCompat("random");
        assertEquals(locusIdA, locusIdA);
        assertEquals(locusIdB, locusIdA);
        assertNotEquals(null, locusIdA);
        assertNotEquals(locusIdC, locusIdA);
    }

    @Test
    public void testLocusId_conversions() {
        if (Build.VERSION.SDK_INT >= 29) {
            final LocusIdCompat src = new LocusIdCompat(ID);
            final LocusId cnv = src.toLocusId();
            final LocusIdCompat res = LocusIdCompat.toLocusIdCompat(cnv);
            assertEquals(src, res);
        }
    }
}

