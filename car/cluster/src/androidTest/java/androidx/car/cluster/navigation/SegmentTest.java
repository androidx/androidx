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

package androidx.car.cluster.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link Segment} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SegmentTest {
    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        assertEquals(new Segment(), new Segment(""));
        assertEquals(new Segment("foo"), new Segment("foo"));
        assertNotEquals(new Segment("foo"), new Segment("bar"));
        assertEquals("", new Segment().getName());
        assertEquals(new Segment().hashCode(), new Segment("").hashCode());
        assertEquals(new Segment("foo").hashCode(), new Segment("foo").hashCode());
    }

    /**
     * Test null on {@link Segment} constructor
     */
    @Test(expected = NullPointerException.class)
    public void nullability() {
        new Segment(null);
    }
}
