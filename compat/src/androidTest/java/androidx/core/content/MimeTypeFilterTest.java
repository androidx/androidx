/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link MimeTypeFilter}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class MimeTypeFilterTest {
    @Test
    public void matchesBasic() throws Exception {
        assertTrue(MimeTypeFilter.matches("image/jpeg", "*/*"));
        assertTrue(MimeTypeFilter.matches("image/jpeg", "image/*"));
        assertTrue(MimeTypeFilter.matches("image/jpeg", "image/jpeg"));
        assertTrue(MimeTypeFilter.matches("image/jpeg", "*/jpeg"));

        // These matchers are case *sensitive*.
        assertFalse(MimeTypeFilter.matches("ImAgE/JpEg", "iMaGe/*"));
        assertFalse(MimeTypeFilter.matches("IMAGE/JPEG", "image/jpeg"));
        assertFalse(MimeTypeFilter.matches("image/jpeg", "IMAGE/JPEG"));

        assertFalse(MimeTypeFilter.matches("image/jpeg", "image/png"));
        assertFalse(MimeTypeFilter.matches("image/jpeg", "video/jpeg"));

        assertFalse(MimeTypeFilter.matches((String) null, "*/*"));
        assertFalse(MimeTypeFilter.matches((String) null, "image/"));
        assertFalse(MimeTypeFilter.matches((String) null, "image/jpeg"));

        // Null and invalid MIME types.
        assertFalse(MimeTypeFilter.matches((String) null, "*/*"));
        assertFalse(MimeTypeFilter.matches("", "*/*"));
        assertFalse(MimeTypeFilter.matches("image/", "*/*"));
        assertFalse(MimeTypeFilter.matches("*/", "*/*"));
    }

    @Test
    public void matchesManyFilters() throws Exception {
        assertEquals("*/*", MimeTypeFilter.matches("image/jpeg", new String[] {"*/*"}));
        assertEquals("image/*", MimeTypeFilter.matches("image/jpeg", new String[] {"image/*"}));
        assertEquals("image/jpeg", MimeTypeFilter.matches(
                "image/jpeg", new String[] {"image/jpeg"}));

        assertEquals("*/*", MimeTypeFilter.matches(
                "image/jpeg", new String[] {"not/matching", "*/*"}));
        assertEquals("image/*", MimeTypeFilter.matches(
                "image/jpeg", new String[] {"image/*", "image/jpeg"}));
        assertEquals("image/jpeg", MimeTypeFilter.matches(
                "image/jpeg", new String[] {"image/jpeg", "image/png"}));

        assertNull(MimeTypeFilter.matches(
                "ImAgE/JpEg", new String[] {"iMaGe/*", "image/*"}));
        assertEquals("*/jpeg", MimeTypeFilter.matches(
                "image/jpeg", new String[] {"*/png", "*/jpeg"}));

        assertNull(MimeTypeFilter.matches("image/jpeg", new String[] {}));

        assertNull(MimeTypeFilter.matches("image/jpeg", new String[] {"image/png", "video/jpeg"}));
        assertNull(MimeTypeFilter.matches("image/jpeg", new String[] {"video/jpeg", "image/png"}));

        assertNull(MimeTypeFilter.matches(null, new String[] {"*/*"}));
        assertNull(MimeTypeFilter.matches(null, new String[] {"image/"}));
        assertNull(MimeTypeFilter.matches(null, new String[] {"image/jpeg"}));

        // Null and invalid MIME types.
        assertNull(MimeTypeFilter.matches((String) null, new String[] { "*/*" }));
        assertNull(MimeTypeFilter.matches("", new String[] { "*/*" }));
        assertNull(MimeTypeFilter.matches("image/", new String[] { "*/*" }));
        assertNull(MimeTypeFilter.matches("*/", new String[] { "*/*" }));
    }

    @Test
    public void matchesManyMimeTypes() throws Exception {
        assertArrayEquals(new String[] {"image/jpeg", "image/png"},
                MimeTypeFilter.matchesMany(new String[] {"image/jpeg", "image/png"}, "image/*"));
        assertArrayEquals(new String[] {"image/png"},
                MimeTypeFilter.matchesMany(new String[] {"image/jpeg", "image/png"}, "image/png"));
        assertArrayEquals(new String[] {},
                MimeTypeFilter.matchesMany(new String[] {"image/jpeg", "image/png"}, "*/JpEg"));

        assertArrayEquals(new String[] {},
                MimeTypeFilter.matchesMany(new String[] {"*/", "image/"}, "*/*"));
        assertArrayEquals(new String[] {},
                MimeTypeFilter.matchesMany(new String[] {}, "*/*"));
    }

    @Test
    public void illegalFilters() throws Exception {
        try {
            MimeTypeFilter.matches("image/jpeg", "");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", "*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", "*/");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", "/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", "*/*/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches(new String[] { "image/jpeg" }, "");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches(new String[] { "image/jpeg" }, "*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches(new String[] { "image/jpeg" }, "*/");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches(new String[] { "image/jpeg" }, "/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches(new String[] { "image/jpeg" }, "*/*/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", new String[] { "" });
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", new String[] { "*" });
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", new String[] { "*/" });
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", new String[] { "/*" });
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matches("image/jpeg", new String[] { "*/*/*" });
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matchesMany(new String[] { "image/jpeg" }, "");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matchesMany(new String[] { "image/jpeg" }, "*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matchesMany(new String[] { "image/jpeg" }, "*/");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matchesMany(new String[] { "image/jpeg" }, "/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        try {
            MimeTypeFilter.matchesMany(new String[] { "image/jpeg" }, "*/*/*");
            fail("Illegal filter, should throw.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }
}
