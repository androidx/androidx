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

package androidx.car.navigation.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link BundleMarshaller}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BundleMarshallerTest {
    private final BundleMarshaller mBundleMarshaller = new BundleMarshaller();

    /**
     * A random test value with a mix of primitives, null and non-null data.
     */
    private static final TestBundlable TEST_VALUE = new TestBundlable()
            .setInt(1)
            .setString("TEST")
            .setBundlableValue(new TestBundlable()
                    .setEnumValue(TestBundlable.TestEnum.VALUE1));

    /**
     * Tests that null values are serialized as expected.
     */
    @Test
    public void serialization_nullCase() {
        new TestBundlable().toBundle(mBundleMarshaller);

        Bundle data = mBundleMarshaller.getBundle();
        assertTrue(data.containsKey("intValue"));
        assertEquals(0, data.getInt("intValue"));
        assertNull(data.getString("stringValue"));
        assertNull(data.getString("enumValue"));
        assertTrue(data.getBoolean("bundlableValue._isNull"));
        assertEquals(-1, data.getInt("bundlableListValue._size"));
    }

    /**
     * Tests that nested {@link Bundlable}s are serialized as expected.
     */
    @Test
    public void serialization_nestedBundlable() {
        String stringValue = "TEST";
        int intValue = 1;

        new TestBundlable()
                .setBundlableValue(new TestBundlable()
                        .setInt(intValue)
                        .setString(stringValue))
                .toBundle(mBundleMarshaller);

        Bundle data = mBundleMarshaller.getBundle();
        assertTrue(data.containsKey("bundlableValue._isNull"));
        assertFalse(data.getBoolean("bundlableValue._isNull"));
        assertEquals(intValue, data.getInt("bundlableValue.intValue"));
        assertEquals(stringValue, data.getString("bundlableValue.stringValue"));
    }

    /**
     * Tests the correct serialization of a list of size 0.
     */
    @Test
    public void listSerialization_listOfSize0() {
        TestBundlable value = new TestBundlable().setListValue(new ArrayList<>());
        value.toBundle(mBundleMarshaller);
        Bundle data = mBundleMarshaller.getBundle();
        assertEquals(0, data.getInt("bundlableListValue._size"));
    }

    /**
     * Tests the correct serialization of a list of size n.
     */
    @Test
    public void listSerialization_listOfSizeN() {
        TestBundlable value = new TestBundlable().setListValue(Arrays.asList(
                new TestBundlable().setInt(1),
                new TestBundlable().setInt(2),
                new TestBundlable().setInt(3)));
        Bundle data = mBundleMarshaller.getBundle();
        value.toBundle(mBundleMarshaller);
        assertEquals(3, data.getInt("bundlableListValue._size"));
        assertEquals(1, data.getInt("bundlableListValue.0.intValue"));
        assertEquals(2, data.getInt("bundlableListValue.1.intValue"));
        assertEquals(3, data.getInt("bundlableListValue.2.intValue"));
    }

    @Test
    public void listSerialization_removingElementsInPlace() {
        // Serialize and deserialize a list of a certain size
        List<TestBundlable> mutableList = new ArrayList<>(Arrays.asList(
                new TestBundlable().setInt(1),
                new TestBundlable().setInt(2),
                new TestBundlable().setInt(3)));
        TestBundlable out = new TestBundlable().setListValue(mutableList);
        TestBundlable in = new TestBundlable();
        out.toBundle(mBundleMarshaller);
        in.fromBundle(mBundleMarshaller);
        assertEquals(out, in);

        // Remove some elements and check that they are correctly removed during deserialization
        mutableList.remove(0);
        out.toBundle(mBundleMarshaller);
        in.fromBundle(mBundleMarshaller);
        assertEquals(out, in);
    }

    /**
     * Tests that {@link BundleMarshaller#getDelta()} returns the same value as
     * {@link BundleMarshaller#getBundle()} during the initial serialization.
     */
    @Test
    public void deltaSerialization_equalsFullDataIfNotReset() {
        TEST_VALUE.toBundle(mBundleMarshaller);
        Bundle data = mBundleMarshaller.getBundle();
        Bundle delta = mBundleMarshaller.getDelta();
        assertBundlesEqual(data, delta);
    }

    /**
     * Tests that {@link BundleMarshaller#getDelta()} is empty if no data is modified between
     * serializations.
     */
    @Test
    public void deltaSerialization_emptyIfNoDataIsModified() {
        TEST_VALUE.toBundle(mBundleMarshaller);
        mBundleMarshaller.resetDelta();
        TEST_VALUE.toBundle(mBundleMarshaller);
        Bundle delta = mBundleMarshaller.getDelta();
        assertEquals(0, delta.size());
    }

    /**
     * Tests that {@link BundleMarshaller#getDelta()} returns only the data that has been modified
     * between two serializations.
     */
    @Test
    public void deltaSerialization_onlyContainsModifiedData() {
        // Serialize some base data
        TestBundlable testValue = new TestBundlable()
                .setInt(1)
                .setString("TEST")
                .setBundlableValue(new TestBundlable()
                        .setEnumValue(TestBundlable.TestEnum.VALUE1));
        testValue.toBundle(mBundleMarshaller);

        // Reset change tracking and re-serialize after making some changes.
        mBundleMarshaller.resetDelta();
        testValue.setInt(2).setString(null);
        testValue.mBundableValue.setEnumValue(TestBundlable.TestEnum.VALUE2);
        testValue.toBundle(mBundleMarshaller);

        Bundle expectedDelta = new Bundle();
        expectedDelta.putInt("intValue", testValue.mIntValue);
        expectedDelta.putString("stringValue", testValue.mStringValue);
        expectedDelta.putString("bundlableValue.enumValue",
                testValue.mBundableValue.mEnumValue.name());

        Bundle delta = mBundleMarshaller.getDelta();
        assertBundlesEqual(expectedDelta, delta);
    }

    /**
     * Asserts that the provided {@link Bundle}s are equal. It throws {@link AssertionError}
     * otherwise.
     */
    private void assertBundlesEqual(Bundle expected, Bundle actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail(String.format("Expected %s value but found %s",
                    expected != null ? "non-null" : "null",
                    actual != null ? "non-null" : "null"));
        }
        if (!expected.keySet().equals(actual.keySet())) {
            fail(String.format("Expected keys: %s, but found keys: %s",
                    expected.keySet().stream().sorted().collect(Collectors.joining(",")),
                    actual.keySet().stream().sorted().collect(Collectors.joining(","))));
        }
        for (String key : expected.keySet()) {
            assertEquals(String.format("Expected '%s' at key '%s' but found '%s",
                            expected.get(key), key, actual.get(key)),
                    expected.get(key),
                    actual.get(key));
        }
    }
}
