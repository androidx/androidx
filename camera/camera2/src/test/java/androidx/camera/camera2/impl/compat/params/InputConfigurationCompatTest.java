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

package androidx.camera.camera2.impl.compat.params;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.ImageFormat;
import android.hardware.camera2.params.InputConfiguration;
import android.os.Build;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class InputConfigurationCompatTest {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;
    private static final int FORMAT = ImageFormat.YUV_420_888;

    @Test
    public void canCreateInputConfigurationCompat() {
        InputConfigurationCompat compat = new InputConfigurationCompat(WIDTH, HEIGHT, FORMAT);

        assertThat(compat.getWidth()).isEqualTo(WIDTH);
        assertThat(compat.getHeight()).isEqualTo(HEIGHT);
        assertThat(compat.getFormat()).isEqualTo(FORMAT);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void canWrapInputConfiguration() {
        InputConfiguration inputConfig = new InputConfiguration(WIDTH, HEIGHT, FORMAT);
        InputConfigurationCompat compat = InputConfigurationCompat.wrap(inputConfig);

        assertThat(compat).isNotNull();
        assertThat(compat.unwrap()).isSameInstanceAs(inputConfig);
    }

    @Test
    public void canCompare() {
        InputConfigurationCompat compat1 = new InputConfigurationCompat(WIDTH, HEIGHT, FORMAT);
        InputConfigurationCompat compat2 = new InputConfigurationCompat(WIDTH, HEIGHT, FORMAT);

        // Swap width and height
        // noinspection SuspiciousNameCombination
        InputConfigurationCompat compat3 = new InputConfigurationCompat(HEIGHT, WIDTH, FORMAT);

        assertThat(compat1).isEqualTo(compat1);
        assertThat(compat1).isEqualTo(compat2);
        assertThat(compat1).isNotEqualTo(compat3);

        assertThat(compat1.hashCode()).isEqualTo(compat1.hashCode());
        assertThat(compat1.hashCode()).isEqualTo(compat2.hashCode());
        assertThat(compat1.hashCode()).isNotEqualTo(compat3.hashCode());
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void baseImplHashCodeMatchesFramework() {
        InputConfigurationCompat.InputConfigurationCompatBaseImpl baseImpl =
                new InputConfigurationCompat.InputConfigurationCompatBaseImpl(WIDTH, HEIGHT,
                        FORMAT);

        InputConfiguration config = new InputConfiguration(WIDTH, HEIGHT, FORMAT);

        assertThat(baseImpl.hashCode()).isEqualTo(config.hashCode());
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void baseImplToStringMatchesFramework() {
        InputConfigurationCompat.InputConfigurationCompatBaseImpl baseImpl =
                new InputConfigurationCompat.InputConfigurationCompatBaseImpl(WIDTH, HEIGHT,
                        FORMAT);

        InputConfiguration config = new InputConfiguration(WIDTH, HEIGHT, FORMAT);

        assertThat(baseImpl.toString()).isEqualTo(config.toString());
    }
}
