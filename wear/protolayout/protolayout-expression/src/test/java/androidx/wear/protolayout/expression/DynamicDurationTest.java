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

import static androidx.wear.protolayout.expression.DynamicBuilders.dynamicDurationFromProto;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
public final class DynamicDurationTest {
    private static final String STATE_KEY = "state-key";

    @Test
    public void stateEntryValueDuration() {
        DynamicDuration stateDuration = DynamicDuration.from(new AppDataKey<>(STATE_KEY));

        assertThat(stateDuration.toDynamicDurationProto().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void serializing_deserializing_withFingerprint() {
        DynamicDuration from = DynamicDuration.withSecondsPrecision(Duration.ZERO);
        NodeFingerprint fingerprint = from.getFingerprint().toProto();

        DynamicProto.DynamicDuration to = from.toDynamicDurationProto(true);
        assertThat(to.getFingerprint()).isEqualTo(fingerprint);

        DynamicDuration back = dynamicDurationFromProto(to);
        assertThat(back.getFingerprint().toProto()).isEqualTo(fingerprint);
    }

    @Test
    public void toByteArray_fromByteArray_withFingerprint() {
        DynamicDuration from = DynamicDuration.from(new AppDataKey<>(STATE_KEY));
        byte[] buffer = from.toDynamicDurationByteArray();
        DynamicProto.DynamicDuration toProto =
                DynamicDuration.fromByteArray(buffer).toDynamicDurationProto(true);

        assertThat(toProto.getFingerprint()).isEqualTo(from.getFingerprint().toProto());
    }
}
