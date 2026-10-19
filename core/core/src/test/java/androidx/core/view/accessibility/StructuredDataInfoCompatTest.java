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

package androidx.core.view.accessibility;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_ATTRIBUTES_KEY;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_TAG_KEY;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_TYPE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.MathInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Map;

/**
 * Tests for {@link StructuredDataInfoCompat} and {@link MathInfoCompat}.
 *
 * <p>Unless a test's config says otherwise, it runs on the library's minimum SDK, on the newest SDK
 * without the platform {@code MathInfo} class, where structured data is stored in a node's extras,
 * and on the first SDK with it, where it's stored in the platform field. Each test expects the same
 * behavior on all of them.
 *
 * <p>This class must not reference platform classes that are missing from older SDKs, since it's
 * loaded on them. Tests of interactions with the platform classes are in {@link
 * StructuredDataInfoCompatApi37Test}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = {
            Build.VERSION_CODES.N,
            Build.VERSION_CODES.BAKLAVA,
            Build.VERSION_CODES.CINNAMON_BUN
        })
@DoNotInstrument
public final class StructuredDataInfoCompatTest {
    private static final String INTENT = MathInfoCompat.MATH_ATTRIBUTE_INTENT;
    private static final String ARG = MathInfoCompat.MATH_ATTRIBUTE_ARG;

    @Test
    public void getStructuredDataInfo_unset_returnsNull() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();

        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void setStructuredDataInfo_roundTripsMathInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        MathInfoCompat fraction = createFraction();

        node.setStructuredDataInfo(fraction);

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();
        assertThat(retrieved).isInstanceOf(MathInfoCompat.class);
        assertThat(retrieved.getTag()).isEqualTo(MathInfoCompat.MATH_TAG_FRACTION);
        assertThat(retrieved.getAttributes()).containsExactly(INTENT, "divide($a, $b)", ARG, "1");
        assertThat(retrieved).isEqualTo(fraction);
    }

    @Test
    public void setStructuredDataInfo_null_clearsInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        node.setStructuredDataInfo(null);

        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void setStructuredDataInfo_mathInfoWrappingNull_clearsInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        node.setStructuredDataInfo(new MathInfoCompat((AccessibilityNodeInfo.MathInfo) null));

        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void setStructuredDataInfo_replacesPreviousInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        node.setStructuredDataInfo(new MathInfoCompat(MathInfoCompat.MATH_TAG_SQUARE_ROOT));

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();
        assertThat(retrieved.getTag()).isEqualTo(MathInfoCompat.MATH_TAG_SQUARE_ROOT);
        assertThat(retrieved.getAttributes()).isEmpty();
    }

    @Test
    public void parceling_preservesInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        StructuredDataInfoCompat retrieved = parcelAndUnparcel(node).getStructuredDataInfo();

        assertThat(retrieved).isInstanceOf(MathInfoCompat.class);
        assertThat(retrieved).isEqualTo(createFraction());
    }

    @Test
    public void obtainingCopy_preservesInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        AccessibilityNodeInfoCompat copy = AccessibilityNodeInfoCompat.obtain(node);

        assertThat(copy.getStructuredDataInfo()).isEqualTo(createFraction());
    }

    @Test
    public void getStructuredDataInfo_unknownTypeInExtras_returnsNull() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putStructuredDataExtras(node.getExtras(), "type-from-newer-library", "tag", new Bundle());

        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void getStructuredDataInfo_extrasWithoutTag_returnsNull() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putStructuredDataExtras(node.getExtras(), MathInfoCompat.EXTRAS_TYPE, null, new Bundle());

        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void getStructuredDataInfo_extrasWithoutAttributes_returnsInfoWithoutAttributes() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putStructuredDataExtras(
                node.getExtras(), MathInfoCompat.EXTRAS_TYPE, MathInfoCompat.MATH_TAG_ROW, null);

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();

        assertThat(retrieved).isEqualTo(new MathInfoCompat(MathInfoCompat.MATH_TAG_ROW));
    }

    @Test
    public void getStructuredDataInfo_ignoresNonStringAttributesInExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        Bundle attributes = new Bundle();
        attributes.putString(INTENT, "divide($a, $b)");
        attributes.putInt(ARG, 1);
        putStructuredDataExtras(
                node.getExtras(),
                MathInfoCompat.EXTRAS_TYPE,
                MathInfoCompat.MATH_TAG_FRACTION,
                attributes);

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();

        assertThat(retrieved.getAttributes()).containsExactly(INTENT, "divide($a, $b)");
        assertThat(retrieved.getAttribute(ARG)).isNull();
    }

    @Test
    public void setStructuredDataInfo_preservesUnrelatedExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.getExtras().putString("unrelated.key", "value");

        node.setStructuredDataInfo(createFraction());
        node.setStructuredDataInfo(null);

        assertThat(node.getExtras().getString("unrelated.key")).isEqualTo("value");
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.N, Build.VERSION_CODES.BAKLAVA})
    public void setStructuredDataInfo_belowApi37_storesInfoInExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();

        node.setStructuredDataInfo(createFraction());

        Bundle extras = node.getExtras();
        assertThat(extras.getString(STRUCTURED_DATA_TYPE_KEY)).isEqualTo("math");
        assertThat(extras.getString(STRUCTURED_DATA_TAG_KEY))
                .isEqualTo(MathInfoCompat.MATH_TAG_FRACTION);
        Bundle attributes = extras.getBundle(STRUCTURED_DATA_ATTRIBUTES_KEY);
        assertThat(attributes.keySet()).containsExactly(INTENT, ARG);
        assertThat(attributes.getString(INTENT)).isEqualTo("divide($a, $b)");
        assertThat(attributes.getString(ARG)).isEqualTo("1");
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.N, Build.VERSION_CODES.BAKLAVA})
    public void setStructuredDataInfo_null_belowApi37_removesInfoFromExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createFraction());

        node.setStructuredDataInfo(null);

        Bundle extras = node.getExtras();
        assertThat(extras.containsKey(STRUCTURED_DATA_TYPE_KEY)).isFalse();
        assertThat(extras.containsKey(STRUCTURED_DATA_TAG_KEY)).isFalse();
        assertThat(extras.containsKey(STRUCTURED_DATA_ATTRIBUTES_KEY)).isFalse();
    }

    @Test
    public void equalsAndHashCode_compareTagAndAttributes() {
        MathInfoCompat fraction = createFraction();
        MathInfoCompat differentAttributes = createFraction();
        differentAttributes.putAttribute(INTENT, "ratio($a, $b)");
        MathInfoCompat differentTag = new MathInfoCompat(MathInfoCompat.MATH_TAG_SQUARE_ROOT);
        differentTag.putAttribute(INTENT, "divide($a, $b)");
        differentTag.putAttribute(ARG, "1");

        assertThat(fraction).isEqualTo(fraction);
        assertThat(fraction).isEqualTo(createFraction());
        assertThat(fraction.hashCode()).isEqualTo(createFraction().hashCode());
        assertThat(fraction).isNotEqualTo(differentAttributes);
        assertThat(fraction).isNotEqualTo(differentTag);
        assertThat(fraction).isNotEqualTo(null);
        assertThat(fraction).isNotEqualTo(MathInfoCompat.MATH_TAG_FRACTION);
    }

    @Test
    public void equals_differentSubclassesWithSameData_areNotEqual() {
        MathInfoCompat mathInfo = new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION);
        StructuredDataInfoCompat otherType =
                new StructuredDataInfoCompat(
                        null, "other", MathInfoCompat.MATH_TAG_FRACTION, null) {};

        assertThat(otherType.getTag()).isEqualTo(mathInfo.getTag());
        assertThat(otherType.getAttributes()).isEqualTo(mathInfo.getAttributes());
        assertThat(mathInfo).isNotEqualTo(otherType);
        assertThat(otherType).isNotEqualTo(mathInfo);
    }

    @Test
    public void getAttributes_isUnmodifiable() {
        Map<String, String> attributes = createFraction().getAttributes();

        assertThrows(UnsupportedOperationException.class, () -> attributes.put(INTENT, "value"));
    }

    @Test
    public void removeAttribute_removesOnlyThatAttribute() {
        MathInfoCompat fraction = createFraction();

        fraction.removeAttribute(ARG);

        assertThat(fraction.getAttributes()).containsExactly(INTENT, "divide($a, $b)");
    }

    private static MathInfoCompat createFraction() {
        MathInfoCompat fraction = new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION);
        fraction.putAttribute(INTENT, "divide($a, $b)");
        fraction.putAttribute(ARG, "1");
        return fraction;
    }

    private static void putStructuredDataExtras(
            Bundle extras, String type, String tag, Bundle attributes) {
        extras.putString(STRUCTURED_DATA_TYPE_KEY, type);
        extras.putString(STRUCTURED_DATA_TAG_KEY, tag);
        extras.putBundle(STRUCTURED_DATA_ATTRIBUTES_KEY, attributes);
    }

    /**
     * Returns a copy of {@code node} made by parceling it, as when it's sent to an accessibility
     * service. On older SDKs, this recycles {@code node}, so it mustn't be used afterwards.
     */
    private static AccessibilityNodeInfoCompat parcelAndUnparcel(AccessibilityNodeInfoCompat node) {
        Parcel parcel = Parcel.obtain();
        try {
            node.unwrap().writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return AccessibilityNodeInfoCompat.wrap(
                    AccessibilityNodeInfo.CREATOR.createFromParcel(parcel));
        } finally {
            parcel.recycle();
        }
    }
}
