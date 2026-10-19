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

import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo.MathInfo;
import android.view.accessibility.AccessibilityNodeInfo.StructuredDataInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.MathInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Tests for how {@link StructuredDataInfoCompat} interacts with the platform {@link
 * StructuredDataInfo} classes on the first SDK that has them. Behavior that's the same on all SDKs
 * is tested in {@link StructuredDataInfoCompatTest}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.CINNAMON_BUN)
@DoNotInstrument
public final class StructuredDataInfoCompatApi37Test {
    private static final String INTENT = MathInfoCompat.MATH_ATTRIBUTE_INTENT;
    private static final String ARG = MathInfoCompat.MATH_ATTRIBUTE_ARG;

    @Test
    public void setStructuredDataInfo_mathInfo_storesInfoInPlatformField() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        MathInfoCompat fraction = new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION);
        fraction.putAttribute(INTENT, "divide($a, $b)");

        node.setStructuredDataInfo(fraction);

        StructuredDataInfo platformInfo = node.unwrap().getStructuredDataInfo();
        assertThat(platformInfo).isInstanceOf(MathInfo.class);
        assertThat(platformInfo.getTag()).isEqualTo(MathInfo.MATH_TAG_FRACTION);
        assertThat(platformInfo.getAttributes()).containsExactly(INTENT, "divide($a, $b)");
        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_TYPE_KEY)).isFalse();
        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_TAG_KEY)).isFalse();
        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_ATTRIBUTES_KEY)).isFalse();
    }

    @Test
    public void getStructuredDataInfo_mathInfoSetWithPlatformApi_returnsMathInfoCompat() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        MathInfo mathInfo = new MathInfo(MathInfo.MATH_TAG_IDENTIFIER);
        mathInfo.putAttribute(MathInfo.MATH_ATTRIBUTE_INTENT, "velocity");
        node.unwrap().setStructuredDataInfo(mathInfo);

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();

        assertThat(retrieved).isInstanceOf(MathInfoCompat.class);
        assertThat(retrieved.getTag()).isEqualTo(MathInfoCompat.MATH_TAG_IDENTIFIER);
        assertThat(retrieved.getAttributes()).containsExactly(INTENT, "velocity");
    }

    @Test
    public void mutatingMathInfoCompatWrappingPlatformObject_updatesPlatformObject() {
        MathInfo mathInfo = new MathInfo(MathInfo.MATH_TAG_IDENTIFIER);
        MathInfoCompat mathInfoCompat = new MathInfoCompat(mathInfo);

        mathInfoCompat.putAttribute(INTENT, "velocity");
        mathInfoCompat.putAttribute(ARG, "1");
        mathInfoCompat.removeAttribute(ARG);

        assertThat(mathInfoCompat.getTag()).isEqualTo(MathInfoCompat.MATH_TAG_IDENTIFIER);
        assertThat(mathInfo.getAttributes()).containsExactly(INTENT, "velocity");
    }

    @Test
    public void getStructuredDataInfo_platformFieldTakesPrecedenceOverExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putMathExtras(node.getExtras(), MathInfoCompat.MATH_TAG_NUMBER);
        node.unwrap().setStructuredDataInfo(new MathInfo(MathInfo.MATH_TAG_IDENTIFIER));

        assertThat(node.getStructuredDataInfo().getTag())
                .isEqualTo(MathInfoCompat.MATH_TAG_IDENTIFIER);
    }

    @Test
    public void setStructuredDataInfo_mathInfo_removesInfoStoredInExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putMathExtras(node.getExtras(), MathInfoCompat.MATH_TAG_NUMBER);

        node.setStructuredDataInfo(new MathInfoCompat(MathInfoCompat.MATH_TAG_IDENTIFIER));
        node.unwrap().setStructuredDataInfo(null);

        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_TYPE_KEY)).isFalse();
        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void setStructuredDataInfo_null_clearsPlatformFieldAndExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        putMathExtras(node.getExtras(), MathInfoCompat.MATH_TAG_NUMBER);
        node.unwrap().setStructuredDataInfo(new MathInfo(MathInfo.MATH_TAG_IDENTIFIER));

        node.setStructuredDataInfo(null);

        assertThat(node.unwrap().getStructuredDataInfo()).isNull();
        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_TYPE_KEY)).isFalse();
        assertThat(node.getStructuredDataInfo()).isNull();
    }

    @Test
    public void setStructuredDataInfo_infoWithoutPlatformObject_storesInfoInExtras() {
        // Instances read from extras don't wrap a platform object, as is the case on all SDKs for
        // types whose platform class isn't available.
        AccessibilityNodeInfoCompat source = AccessibilityNodeInfoCompat.obtain();
        putMathExtras(source.getExtras(), MathInfoCompat.MATH_TAG_NUMBER);
        StructuredDataInfoCompat info = source.getStructuredDataInfo();
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.unwrap().setStructuredDataInfo(new MathInfo(MathInfo.MATH_TAG_IDENTIFIER));

        node.setStructuredDataInfo(info);

        assertThat(node.unwrap().getStructuredDataInfo()).isNull();
        assertThat(node.getExtras().getString(STRUCTURED_DATA_TYPE_KEY))
                .isEqualTo(MathInfoCompat.EXTRAS_TYPE);
        assertThat(node.getStructuredDataInfo()).isEqualTo(info);
    }

    @Test
    public void equals_instancesWithAndWithoutPlatformObjectAndSameData_areEqual() {
        MathInfo mathInfo = new MathInfo(MathInfo.MATH_TAG_FRACTION);
        mathInfo.putAttribute(INTENT, "divide($a, $b)");
        MathInfoCompat withPlatformObject = new MathInfoCompat(mathInfo);
        Bundle attributes = new Bundle();
        attributes.putString(INTENT, "divide($a, $b)");
        MathInfoCompat withoutPlatformObject =
                new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION, attributes);

        assertThat(withPlatformObject).isEqualTo(withoutPlatformObject);
        assertThat(withoutPlatformObject).isEqualTo(withPlatformObject);
        assertThat(withPlatformObject.hashCode()).isEqualTo(withoutPlatformObject.hashCode());
    }

    private static void putMathExtras(Bundle extras, String tag) {
        extras.putString(STRUCTURED_DATA_TYPE_KEY, MathInfoCompat.EXTRAS_TYPE);
        extras.putString(STRUCTURED_DATA_TAG_KEY, tag);
        extras.putBundle(STRUCTURED_DATA_ATTRIBUTES_KEY, new Bundle());
    }
}
