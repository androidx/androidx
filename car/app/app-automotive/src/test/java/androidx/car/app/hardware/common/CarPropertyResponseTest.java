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

package androidx.car.app.hardware.common;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarPropertyResponseTest {
    private static final int PROPERTY_ID = 123;

    @Test
    public void build_returnsUnknownResponseIfNothingSetBesidesPropertyId() {
        assertThat(CarPropertyResponse.builder().setPropertyId(PROPERTY_ID).setStatus(
                CarValue.STATUS_UNKNOWN).build()).isEqualTo(
                CarPropertyResponse.builder().setPropertyId(PROPERTY_ID).setStatus(
                        CarValue.STATUS_UNKNOWN).setValue(null).setTimestampMillis(0).setCarZones(
                        ImmutableList.of(CarZone.CAR_ZONE_GLOBAL)).build());
    }

    @Test
    public void build_requiresNonNullValueIfStatusIsSuccess() {
        assertThrows(IllegalStateException.class, () -> CarPropertyResponse.builder().setPropertyId(
                PROPERTY_ID).setStatus(CarValue.STATUS_SUCCESS).build());
    }

    @Test
    public void build_requiresNullValueIfStatusIsUnknown() {
        assertThrows(IllegalStateException.class, () -> CarPropertyResponse.builder().setPropertyId(
                PROPERTY_ID).setStatus(CarValue.STATUS_UNKNOWN).setValue(4).build());
    }

    @Test
    public void build_requiresNullValueIfStatusIsUnimplemented() {
        assertThrows(IllegalStateException.class, () -> CarPropertyResponse.builder().setPropertyId(
                PROPERTY_ID).setStatus(CarValue.STATUS_UNIMPLEMENTED).setValue(4).build());
    }

    @Test
    public void build_requiresNullValueIfStatusIsUnavailable() {
        assertThrows(IllegalStateException.class, () -> CarPropertyResponse.builder().setPropertyId(
                PROPERTY_ID).setStatus(CarValue.STATUS_UNAVAILABLE).setValue(4).build());
    }
}
