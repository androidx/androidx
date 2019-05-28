/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;

/**
 * Tests for {@link EnumWrapper} serialization
 */
public class EnumWrapperTest {
    public enum CONSUMER_ENUM {
        FOO,
        BAR
    }

    public enum PRODUCER_ENUM {
        FOO,
        BAR,
        MOO
    }

    public static final EnumWrapper<CONSUMER_ENUM> TEST_WRAPPER = new EnumWrapper<>(
            CONSUMER_ENUM.FOO
    );
    public static final EnumWrapper<CONSUMER_ENUM> TEST_WRAPPER_2 = new EnumWrapper<>(
            CONSUMER_ENUM.BAR
    );

    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        EnumWrapper<CONSUMER_ENUM> expected = new EnumWrapper<>(
                CONSUMER_ENUM.FOO
        );

        assertEquals(expected, TEST_WRAPPER);
        assertEquals(expected.hashCode(), TEST_WRAPPER.hashCode());
        assertNotEquals(expected, TEST_WRAPPER_2);
        assertNotEquals(expected.hashCode(), TEST_WRAPPER_2.hashCode());


        assertEquals(EnumWrapper.getValue(new EnumWrapper<>(), CONSUMER_ENUM.FOO),
                CONSUMER_ENUM.FOO);
        assertEquals(EnumWrapper.getValue(expected, CONSUMER_ENUM.BAR), CONSUMER_ENUM.FOO);
        assertNotEquals(EnumWrapper.getValue(new EnumWrapper<>(), CONSUMER_ENUM.FOO),
                CONSUMER_ENUM.BAR);
        assertNotEquals(EnumWrapper.getValue(expected, CONSUMER_ENUM.BAR), CONSUMER_ENUM.BAR);

        assertEquals(expected, EnumWrapper.of(CONSUMER_ENUM.FOO));
        assertNotEquals(expected, EnumWrapper.of(CONSUMER_ENUM.FOO, CONSUMER_ENUM.BAR));
    }

    /**
     * If a value provided by the producer is not known by the data consumer (e.g.: a new value
     * was introduced, but the consumer still is using a older version of the API),
     * test the returned first "fallback" value that is known to the consumer.
     */
    @Test
    public void fallback() {
        EnumWrapper<PRODUCER_ENUM> prodWrapper = new EnumWrapper<>(
                PRODUCER_ENUM.MOO, PRODUCER_ENUM.FOO, PRODUCER_ENUM.BAR
        );

        Parcelable out = deserialize(serialize(ParcelUtils.toParcelable(prodWrapper)));
        EnumWrapper<CONSUMER_ENUM> consWrapper = ParcelUtils.fromParcelable(out);

        assertEquals(EnumWrapper.getValue(consWrapper, CONSUMER_ENUM.BAR), CONSUMER_ENUM.FOO);
    }

    /**
     * Test null on {@link EnumWrapper} constructor
     */
    @Test(expected = NullPointerException.class)
    public void nullability_constructor() {
        new EnumWrapper<>(null);
    }

    /**
     * Test null on {@link EnumWrapper#of}
     */
    @Test(expected = NullPointerException.class)
    public void nullability_of() {
        EnumWrapper.of(null);
    }

    private Parcel serialize(Parcelable wrapper) {
        Bundle in = new Bundle();
        Parcel parcel = Parcel.obtain();
        in.putParcelable("KEY", wrapper);
        in.writeToParcel(parcel, 0);
        return parcel;
    }

    private Parcelable deserialize(Parcel parcel) {
        Bundle out = new Bundle();
        parcel.setDataPosition(0);
        out.setClassLoader(NavigationState.class.getClassLoader());
        out.readFromParcel(parcel);
        return out.getParcelable("KEY");
    }
}
