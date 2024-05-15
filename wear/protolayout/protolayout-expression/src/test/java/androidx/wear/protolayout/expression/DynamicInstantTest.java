/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.wear.protolayout.expression.DynamicBuilders.dynamicInstantFromProto;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicInstantTest {
    private static final String STATE_KEY = "state-key";

    @Test
    public void stateEntryValueInstant() {
        DynamicInstant stateInstant = DynamicInstant.from(new AppDataKey<>(STATE_KEY));

        assertThat(stateInstant.toDynamicInstantProto().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void serializing_deserializing_withFingerprint() {
        DynamicInstant from = DynamicInstant.platformTimeWithSecondsPrecision();
        NodeFingerprint fingerprint = from.getFingerprint().toProto();

        DynamicProto.DynamicInstant to = from.toDynamicInstantProto(true);
        assertThat(to.getFingerprint()).isEqualTo(fingerprint);

        DynamicInstant back = dynamicInstantFromProto(to);
        assertThat(back.getFingerprint().toProto()).isEqualTo(fingerprint);
    }

    @Test
    public void toByteArray_fromByteArray_withFingerprint() {
        DynamicInstant from = DynamicInstant.from(new AppDataKey<>(STATE_KEY));
        byte[] buffer = from.toDynamicInstantByteArray();
        DynamicProto.DynamicInstant toProto =
                DynamicInstant.fromByteArray(buffer).toDynamicInstantProto(true);

        assertThat(toProto.getFingerprint()).isEqualTo(from.getFingerprint().toProto());
    }
}
