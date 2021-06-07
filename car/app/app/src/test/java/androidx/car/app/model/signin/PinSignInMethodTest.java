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

package androidx.car.app.model.signin;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PinSignInMethod}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PinSignInMethodTest {
    @Test
    public void create_defaultValues() {
        PinSignInMethod signIn = new PinSignInMethod.Builder("ABC").build();
        assertThat(signIn.getPinCode().toString()).isEqualTo("ABC");
    }

    @Test
    public void create_checkPinLimits() {
        // Zero
        assertThrows(IllegalArgumentException.class, () -> new PinSignInMethod.Builder(""));

        // Over max of 12
        assertThrows(IllegalArgumentException.class,
                () -> new PinSignInMethod.Builder("123456123456x"));

        // Just at max
        PinSignInMethod signIn = new PinSignInMethod.Builder("123456123456").build();
        assertThat(signIn.getPinCode().toString().length() == 12);
    }

    @Test
    public void equals() {
        String pin = "ABC";
        PinSignInMethod signIn = new PinSignInMethod.Builder(pin).build();
        assertThat(signIn).isEqualTo(new PinSignInMethod.Builder(pin).build());
    }

    @Test
    public void notEquals_differentPin() {
        PinSignInMethod signIn = new PinSignInMethod.Builder("ABC").build();
        assertThat(signIn).isNotEqualTo(new PinSignInMethod.Builder("DEF").build());
    }
}
