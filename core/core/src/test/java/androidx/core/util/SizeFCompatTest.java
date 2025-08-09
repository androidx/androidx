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

package androidx.core.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.util.SizeF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = 23)
public class SizeFCompatTest {

    @Test
    public void constructor_validSize() {
        SizeFCompat size = new SizeFCompat(100.4f, 150.2f);

        assertThat(size.getWidth()).isEqualTo(100.4f);
        assertThat(size.getHeight()).isEqualTo(150.2f);
    }

    @Test
    public void constructor_invalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new SizeFCompat(Float.NaN, 100f));
        assertThrows(IllegalArgumentException.class, () -> new SizeFCompat(12.5f, Float.NaN));
    }

    @Test
    public void equals() {
        assertThat(new SizeFCompat(100.4f, 150.2f)).isEqualTo(new SizeFCompat(100.4f, 150.2f));
        assertThat(new SizeFCompat(100.4f, 150.2f)).isNotEqualTo(new SizeFCompat(10.4f, 150.2f));
        assertThat(new SizeFCompat(100.4f, 150.2f)).isNotEqualTo(new SizeFCompat(100.4f, 15.2f));
    }

    @Test
    public void hashCode_sameForEqualSizes() {
        assertThat(new SizeFCompat(100.4f, 150.2f).hashCode())
                .isEqualTo(new SizeFCompat(100.4f, 150.2f).hashCode());
        assertThat(new SizeFCompat(100.4f, 150.2f).hashCode())
                .isNotEqualTo(new SizeFCompat(10.4f, 150.2f).hashCode());
        assertThat(new SizeFCompat(100.4f, 150.2f).hashCode())
                .isNotEqualTo(new SizeFCompat(100.4f, 15.2f).hashCode());
    }

    @Test
    public void toString_readable() {
        assertThat(new SizeFCompat(10.2f, 20.4f).toString()).isEqualTo("10.2x20.4");
    }

    @Config(sdk = 23)
    @Test
    public void toSizeF() {
        assertThat(new SizeFCompat(10.2f, 20.4f).toSizeF()).isEqualTo(new SizeF(10.2f, 20.4f));
    }

    @Config(sdk = 23)
    @Test
    public void toSizeFCompat() {
        assertThat(SizeFCompat.toSizeFCompat(new SizeF(11.2f, 21.4f)))
                .isEqualTo(new SizeFCompat(11.2f, 21.4f));
    }
}
