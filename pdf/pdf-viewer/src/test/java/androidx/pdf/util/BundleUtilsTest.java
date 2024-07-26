/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

/** Tests for {@link BundleUtils}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class BundleUtilsTest {

    private static final String KEY_1 = "Key1";
    private static final String KEY_2 = "Key2";
    private static final String VAL_1 = "Val1";
    private static final String VAL_2 = "Val2";

    @Test
    public void testGetMapFromBundle_whenBundleNull_returnsNull() {
        assertThat(BundleUtils.getMapFrom(null)).isNull();
    }

    @Test
    public void testGetMapFromBundle_whenBundleEmpty_returnsEmptyMap() {
        Bundle bundle = new Bundle();
        Map<String, String> map = BundleUtils.getMapFrom(bundle);
        assertThat(map).isNotNull();
        assertThat(map).isEmpty();
    }

    @Test
    public void testGetMapFromBundle_whenBundleHasValues_returnsCorrectMap() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_1, VAL_1);
        bundle.putString(KEY_2, VAL_2);
        Map<String, String> map = BundleUtils.getMapFrom(bundle);
        assertThat(map.get(KEY_1)).isEqualTo(VAL_1);
        assertThat(map.get(KEY_2)).isEqualTo(VAL_2);
    }

    @Test
    public void testGetMapFromBundle_whenBundleHasANullValue_returnsCorrectMap() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_1, VAL_1);
        bundle.putString(KEY_2, null);
        Map<String, String> map = BundleUtils.getMapFrom(bundle);
        assertThat(map.get(KEY_1)).isEqualTo(VAL_1);
        assertThat(map.get(KEY_2)).isNull();
    }

    @Test
    public void testGetMapFromBundle_whenBundleHasANonStringValue_returnsCorrectMap() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_1, VAL_1);
        bundle.putFloat(KEY_2, 42F);
        Map<String, String> map = BundleUtils.getMapFrom(bundle);
        assertThat(map.get(KEY_1)).isEqualTo(VAL_1);
        assertThat(map.get(KEY_2)).isNull();
    }

    @Test
    public void testBundleEquals_whenBothNull_returnsTrue() {
        assertThat(BundleUtils.bundleEquals(null, null)).isTrue();
    }

    @Test
    public void testBundleEquals_whenEitherNull_returnsFalse() {
        Bundle b1 = new Bundle();
        assertThat(BundleUtils.bundleEquals(null, b1)).isFalse();
        assertThat(BundleUtils.bundleEquals(b1, null)).isFalse();
    }

    @Test
    public void testBundleEquals_whenBothEmpty_returnsTrue() {
        Bundle b1 = new Bundle();
        Bundle b2 = new Bundle();
        assertThat(BundleUtils.bundleEquals(b1, b2)).isTrue();
    }

    @Test
    public void testBundleEquals_whenDifferentLengths_returnsFalse() {
        Bundle b1 = new Bundle();
        Bundle b2 = new Bundle();
        b1.putString(KEY_1, VAL_1);
        b1.putFloat(KEY_2, 42F);
        b2.putString(KEY_1, VAL_1);
        assertThat(BundleUtils.bundleEquals(b1, b2)).isFalse();
    }

    @Test
    public void testBundleEquals_whenDifferentOrder_returnsTrue() {
        Bundle b1 = new Bundle();
        Bundle b2 = new Bundle();
        b1.putString(KEY_1, VAL_1);
        b1.putFloat(KEY_2, 42F);
        b2.putFloat(KEY_2, 42F);
        b2.putString(KEY_1, VAL_1);
        assertThat(BundleUtils.bundleEquals(b1, b2)).isTrue();
    }

    @Test
    public void testBundleEquals_whenDifferentObjectType_returnsFalse() {
        Bundle b1 = new Bundle();
        Bundle b2 = new Bundle();
        b1.putString(KEY_1, VAL_1);
        b1.putFloat(KEY_2, 42F);
        b2.putString(KEY_2, "42F");
        b2.putString(KEY_1, VAL_1);
        assertThat(BundleUtils.bundleEquals(b1, b2)).isFalse();
    }
}
