/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class FingerprintTest {
    private static final int SELF_TYPE_VALUE = 1234;
    private static final int FIELD_1 = 1;
    private static final int VALUE_HASH1 = 101;
    private static final int FIELD_2 = 2;
    private static final int VALUE_HASH2 = 202;
    private static final int FIELD_3 = 3;
    private static final int VALUE_HASH3 = 301;
    private static final int FIELD_4 = 4;
    private static final int VALUE_HASH4 = 401;

    @Test
    public void addChildNode() {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint childFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        childFingerPrint.recordPropertyUpdate(FIELD_1, VALUE_HASH1);

        parentFingerPrint.addChildNode(childFingerPrint);

        assertThat(parentFingerPrint.childNodes()).containsExactly(childFingerPrint);
    }

    @Test
    public void toProto_fromProto() {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint childFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        childFingerPrint.recordPropertyUpdate(FIELD_1, VALUE_HASH1);
        parentFingerPrint.addChildNode(childFingerPrint);

        NodeFingerprint proto = parentFingerPrint.toProto();
        Fingerprint fingerprint = new Fingerprint(proto);
        assertThat(fingerprint.selfTypeValue()).isEqualTo(SELF_TYPE_VALUE);
        assertThat(fingerprint.selfPropsValue()).isEqualTo(0);

        Fingerprint child = fingerprint.childNodes().get(0);
        assertThat(child.selfTypeValue()).isEqualTo(SELF_TYPE_VALUE);
        assertThat(child.selfPropsValue()).isEqualTo(31 * FIELD_1 + VALUE_HASH1);
    }

    @Test
    public void childNodeOrderMatters() {
        Fingerprint root1 = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint root2 = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint parent12 = createParentFor(FIELD_1, VALUE_HASH1, FIELD_2, VALUE_HASH2);
        Fingerprint parent34 = createParentFor(FIELD_3, VALUE_HASH3, FIELD_4, VALUE_HASH4);
        Fingerprint parent13 = createParentFor(FIELD_1, VALUE_HASH1, FIELD_3, VALUE_HASH3);
        Fingerprint parent24 = createParentFor(FIELD_2, VALUE_HASH2, FIELD_4, VALUE_HASH4);

        root1.addChildNode(parent12);
        root1.addChildNode(parent34);
        root2.addChildNode(parent13);
        root2.addChildNode(parent24);

        assertThat(root1.childNodesValue()).isNotEqualTo(root2.childNodesValue());
    }

    private Fingerprint createParentFor(
            int fieldId1, int fieldValue1, int fieldId2, int fieldValue2) {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint child1FingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        child1FingerPrint.recordPropertyUpdate(fieldId1, fieldValue1);
        parentFingerPrint.addChildNode(child1FingerPrint);
        Fingerprint child2FingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        child2FingerPrint.recordPropertyUpdate(fieldId2, fieldValue2);
        parentFingerPrint.addChildNode(child2FingerPrint);
        return parentFingerPrint;
    }
}
