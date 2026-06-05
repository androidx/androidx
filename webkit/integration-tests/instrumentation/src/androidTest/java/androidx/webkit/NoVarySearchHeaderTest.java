/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NoVarySearchHeaderTest {

    @Test
    public void testNeverVaryHeader() {
        NoVarySearchHeader header = NoVarySearchHeader.neverVaryHeader();
        assertFalse(header.varyOnKeyOrder);
        assertTrue(header.ignoreDifferencesInParameters);
        assertTrue(header.ignoredQueryParameters.isEmpty());
        assertTrue(header.consideredQueryParameters.isEmpty());
    }

    @Test
    public void testAlwaysVaryHeader() {
        NoVarySearchHeader header = NoVarySearchHeader.alwaysVaryHeader();
        assertTrue(header.varyOnKeyOrder);
        assertFalse(header.ignoreDifferencesInParameters);
        assertTrue(header.ignoredQueryParameters.isEmpty());
        assertTrue(header.consideredQueryParameters.isEmpty());
    }

    @Test
    public void testNeverVaryExcept() {
        List<String> considered = List.of("param1", "param2");
        NoVarySearchHeader header = NoVarySearchHeader
                .neverVaryExcept(/*varyOnOrdering=*/ true, considered);
        assertTrue(header.varyOnKeyOrder);
        assertTrue(header.ignoreDifferencesInParameters);
        assertTrue(header.ignoredQueryParameters.isEmpty());
        assertEquals(considered, header.consideredQueryParameters);
    }

    @Test
    public void testVaryExcept() {
        List<String> ignored = List.of("param1", "param2");
        NoVarySearchHeader header = NoVarySearchHeader
                .varyExcept(/*varyOnOrdering=*/ false, ignored);
        assertFalse(header.varyOnKeyOrder);
        assertFalse(header.ignoreDifferencesInParameters);
        assertEquals(ignored, header.ignoredQueryParameters);
        assertTrue(header.consideredQueryParameters.isEmpty());
    }

    @Test
    public void testImmutabilityNeverVaryExcept() {
        List<String> considered = new ArrayList<>();
        considered.add("param1");
        NoVarySearchHeader header = NoVarySearchHeader
                .neverVaryExcept(/*varyOnOrdering=*/ true, considered);

        // Verify defensive copying
        considered.add("param2");
        assertEquals(1, header.consideredQueryParameters.size());
        assertEquals("param1", header.consideredQueryParameters.get(0));

        // Verify immutability of returned list
        try {
            header.consideredQueryParameters.add("param3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // Expected
        }
    }

    @Test
    public void testImmutabilityVaryExcept() {
        List<String> ignored = new ArrayList<>();
        ignored.add("param1");
        NoVarySearchHeader header = NoVarySearchHeader
                .varyExcept(/*varyOnOrdering=*/ false, ignored);

        // Verify defensive copying
        ignored.add("param2");
        assertEquals(1, header.ignoredQueryParameters.size());
        assertEquals("param1", header.ignoredQueryParameters.get(0));

        // Verify immutability of returned list
        try {
            header.ignoredQueryParameters.add("param3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // Expected
        }
    }
}
