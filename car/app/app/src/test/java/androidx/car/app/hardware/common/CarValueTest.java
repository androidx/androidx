/*
 * Copyright 2021 The Android Open Source Project
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

/** Tests for {@link VehicleException}. */

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarValueTest {

    @Test
    public void createInstance() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);

        assertThat(value).isEqualTo(carValue.getValue());
        assertThat(timeStampMillis).isEqualTo(carValue.getTimestampMillis());
        assertThat(status).isEqualTo(carValue.getStatus());
    }

    @Test
    public void equals() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, timeStampMillis, status)).isEqualTo(carValue);
    }

    @Test
    public void notEquals_differentValue() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>("other", timeStampMillis, status))
                .isNotEqualTo(carValue);
    }

    @Test
    public void notEquals_differentTimestamp() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, 20, status)).isNotEqualTo(carValue);
    }

    @Test
    public void notEquals_differentStatus() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, timeStampMillis,
                CarValue.STATUS_UNAVAILABLE)).isNotEqualTo(carValue);
    }

}
