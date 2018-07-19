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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * Unit tests for {@link Bundlable}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BundlableTest {
    /**
     * Serialization test value.
     */
    private static final TestBundlable TEST_VALUE = new TestBundlable()
            .setInt(123)
            .setString("TEST")
            .setEnumValue(TestBundlable.TestEnum.VALUE1)
            .setListValue(Arrays.asList(
                    new TestBundlable()
                            .setString("TEST2")
                            .setEnumValue(TestBundlable.TestEnum.VALUE2),
                    new TestBundlable()
                            .setString("TEST3")
            ))
            .setBundlableValue(
                    new TestBundlable()
                            .setString("TEST4")
            );

    /**
     * Equivalent to {@link #TEST_VALUE} after a schema change (see
     * {@link TestBundlableNewVersion}). In this new schema, {@link TestBundlable} has its
     * {@link TestBundlable#mStringValue} field deprecated, and a new
     * {@link TestBundlableNewVersion#mNewValue} non-null field was added.
     */
    private static final TestBundlableNewVersion TEST_VALUE_NEW_VERSION =
            new TestBundlableNewVersion()
                    .setInt(123)
                    .setEnumValue(TestBundlableNewVersion.TestEnum.VALUE1)
                    .setListValue(Arrays.asList(
                            new TestBundlableNewVersion()
                                    .setEnumValue(TestBundlableNewVersion.TestEnum.VALUE2),
                            new TestBundlableNewVersion()
                    ))
                    .setBundlableValue(
                            new TestBundlableNewVersion()
                    );

    /**
     * Expected value when interpreting {@link #TEST_VALUE_NEW_VERSION} using the same schema as
     * {@link TestBundlable}. Given that {@link TestBundlableNewVersion#mNewValue} doesn't exist
     * the old schema, and {@link TestBundlable#mStringValue} doesn't exist in the new schema,
     * both values are dropped during serialization/deserialization.
     */
    private static final TestBundlable TEST_VALUE_NEW_VERSION_OLD_SCHEMA = new TestBundlable()
            .setInt(123)
            .setEnumValue(TestBundlable.TestEnum.VALUE1)
            .setListValue(Arrays.asList(
                    new TestBundlable()
                            .setEnumValue(TestBundlable.TestEnum.VALUE2),
                    new TestBundlable()
            ))
            .setBundlableValue(
                    new TestBundlable()
            );

    private BundleMarshaller mBundleMarshaller = new BundleMarshaller();

    /**
     * Asserts that serializing and deserializing a {@link Bundlable} produces the same content.
     * This includes testing instances with null values in them.
     */
    @Test
    public void testSerializationDeserializationMaintainsContent() {
        TestBundlable output = new TestBundlable();

        TEST_VALUE.toBundle(mBundleMarshaller);
        output.fromBundle(mBundleMarshaller);
        assertEquals(TEST_VALUE, output);
    }

    /**
     * Asserts that serialization and deserialization works in a forward compatible way, as long as
     * they follow the versioning rules listed in {@link Bundlable}.
     */
    @Test
    public void testForwardCompatibleChangesMaintainsCommonContent() {
        TestBundlableNewVersion output = new TestBundlableNewVersion();

        TEST_VALUE.toBundle(mBundleMarshaller);
        output.fromBundle(mBundleMarshaller);
        assertEquals(TEST_VALUE_NEW_VERSION, output);
    }

    /**
     * Asserts that serialization and deserialization works in a backwards compatible way, as long
     * as they follow the versioning rules listed in {@link Bundlable}.
     */
    @Test
    public void testBackwardCompatibleChangesMaintainsCommonContent() {
        TestBundlable output = new TestBundlable();

        TEST_VALUE_NEW_VERSION.toBundle(mBundleMarshaller);
        output.fromBundle(mBundleMarshaller);
        assertEquals(TEST_VALUE_NEW_VERSION_OLD_SCHEMA, output);
    }
}
