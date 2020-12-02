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

package androidx.window;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class VersionTest {

    @Test
    public void testParse() {
        // Test valid version string
        Version v = Version.parse("1.2.3-test");
        assertNotNull(v);
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getPatch());
        assertEquals("test", v.getDescription());

        // Test invalid version string
        v = Version.parse("invalid");
        assertNull(v);
    }

    @Test
    public void testCompareTo() {
        // Test diff in major
        Version v1 = Version.parse("2.2.3-test");
        Version v2 = Version.parse("1.2.3-test");
        assertThat(v1.compareTo(v2)).isGreaterThan(0);
        assertThat(v2.compareTo(v1)).isLessThan(0);

        // Test diff in minor
        v1 = Version.parse("1.2.3-test");
        v2 = Version.parse("1.1.3-test");
        assertThat(v1.compareTo(v2)).isGreaterThan(0);
        assertThat(v2.compareTo(v1)).isLessThan(0);

        // Test diff in patch
        v1 = Version.parse("1.2.3-test");
        v2 = Version.parse("1.2.0-test");
        assertThat(v1.compareTo(v2)).isGreaterThan(0);
        assertThat(v2.compareTo(v1)).isLessThan(0);

        // Test equals. Description is not included in the check.
        v1 = Version.parse("1.2.3-t1");
        v2 = Version.parse("1.2.3-t2");
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    public void testStatics() {
        assertEquals(Version.parse("0.1.0-1"), Version.VERSION_0_1);
        assertEquals(Version.parse("1.0.0-2"), Version.VERSION_1_0);
        assertEquals(Version.parse("0.0.0-3"), Version.UNKNOWN);
        assertEquals(Version.CURRENT, Version.VERSION_1_0);
    }
}
