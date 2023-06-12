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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class FingerprintTest {
    private static final int SELF_TYPE_VALUE = 1234;
    private static final int FIELD_1 = 1;
    private static final int VALUE_HASH1 = 10;

    private static final int DISCARDED_VALUE = -1;

    @Test
    public void addChildNode() {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint childFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        childFingerPrint.recordPropertyUpdate(FIELD_1, VALUE_HASH1);

        parentFingerPrint.addChildNode(childFingerPrint);

        assertThat(parentFingerPrint.childNodes()).containsExactly(childFingerPrint);
    }

    @Test
    public void discard_clearsSelfFingerprint() {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint childFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        childFingerPrint.recordPropertyUpdate(FIELD_1, VALUE_HASH1);
        parentFingerPrint.addChildNode(childFingerPrint);

        parentFingerPrint.discardValues(/* includeChildren= */ false);

        assertThat(parentFingerPrint.selfPropsValue()).isEqualTo(DISCARDED_VALUE);
        assertThat(parentFingerPrint.childNodes()).containsExactly(childFingerPrint);
        assertThat(parentFingerPrint.childNodesValue()).isNotEqualTo(DISCARDED_VALUE);
    }

    @Test
    public void discard_includeChildren_clearsSelfAndChildrenFingerprint() {
        Fingerprint parentFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        Fingerprint childFingerPrint = new Fingerprint(SELF_TYPE_VALUE);
        childFingerPrint.recordPropertyUpdate(FIELD_1, VALUE_HASH1);
        parentFingerPrint.addChildNode(childFingerPrint);

        parentFingerPrint.discardValues(/* includeChildren= */ true);

        assertThat(parentFingerPrint.selfPropsValue()).isEqualTo(DISCARDED_VALUE);
        assertThat(parentFingerPrint.childNodes()).isEmpty();
        assertThat(parentFingerPrint.childNodesValue()).isEqualTo(DISCARDED_VALUE);
    }
}
