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

package androidx.app.slice.compat;

import static android.content.Context.MODE_PRIVATE;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertArrayEquals;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.app.slice.SliceSpec;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CompatPinnedListTest {

    private final Context mContext = InstrumentationRegistry.getContext();
    private CompatPinnedList mCompatPinnedList;
    private List<SliceSpec> mSpecs;

    private static final SliceSpec[] FIRST_SPECS = new SliceSpec[]{
            new SliceSpec("spec1", 3),
            new SliceSpec("spec2", 3),
            new SliceSpec("spec3", 2),
            new SliceSpec("spec4", 1),
    };

    private static final SliceSpec[] SECOND_SPECS = new SliceSpec[]{
            new SliceSpec("spec2", 1),
            new SliceSpec("spec3", 2),
            new SliceSpec("spec4", 3),
            new SliceSpec("spec5", 4),
    };

    @Before
    public void setup() {
        mCompatPinnedList = new CompatPinnedList(mContext, "test_file");
        mSpecs = Collections.emptyList();
    }

    @After
    public void tearDown() {
        mContext.getSharedPreferences("test_file", MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void testAddFirstPin() {
        assertTrue(mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg",
                mSpecs));
    }

    @Test
    public void testAddSecondPin() {
        assertTrue(mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg",
                mSpecs));
        assertFalse(mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg2",
                mSpecs));
    }

    @Test
    public void testAddMultipleUris() {
        assertTrue(mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg",
                mSpecs));
        assertTrue(mCompatPinnedList.addPin(Uri.parse("content://something/something2"), "my_pkg",
                mSpecs));
    }

    @Test
    public void testRemovePin() {
        mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg", mSpecs);
        mCompatPinnedList.addPin(Uri.parse("content://something/something"), "my_pkg2", mSpecs);
        assertFalse(mCompatPinnedList.removePin(Uri.parse("content://something/something"),
                    "my_pkg"));
        assertTrue(mCompatPinnedList.removePin(Uri.parse("content://something/something"),
                    "my_pkg2"));
    }

    @Test
    public void testMergeSpecs() {
        Uri uri = Uri.parse("content://something/something");

        assertEquals(Collections.emptyList(), mCompatPinnedList.getSpecs(uri));

        mCompatPinnedList.addPin(uri, "my_pkg", Arrays.asList(FIRST_SPECS));
        assertArrayEquals(FIRST_SPECS, mCompatPinnedList.getSpecs(uri).toArray(new SliceSpec[0]));

        mCompatPinnedList.addPin(uri, "my_pkg2", Arrays.asList(SECOND_SPECS));
        assertArrayEquals(new SliceSpec[]{
                // spec1 is gone because it's not in the second set.
                new SliceSpec("spec2", 1), // spec2 is 1 because it's smaller in the second set.
                new SliceSpec("spec3", 2), // spec3 is the same in both sets
                new SliceSpec("spec4", 1), // spec4 is 1 because it's smaller in the first set.
                // spec5 is gone because it's not in the first set.
        }, mCompatPinnedList.getSpecs(uri).toArray(new SliceSpec[0]));

    }
}
