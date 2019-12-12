/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.view;

import static android.os.Build.VERSION.SDK_INT;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import androidx.core.graphics.Insets;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WindowInsetsCompatTest {

    @Test
    public void consumeDisplayCutout_returnsNonNullWindowInsets_pre28() {
        // There is no API create a WindowInsets instance, so we cannot test the 28 code path.
        if (SDK_INT < 28) {
            WindowInsetsCompat insets = new WindowInsetsCompat(new Object());
            assertThat(insets.consumeDisplayCutout(), notNullValue());
        }
    }

    /**
     * Only API 29+, everything works.
     */
    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void builder_api29() {
        Insets sysWindow = Insets.of(12, 34, 35, 31);
        Insets sysGesture = Insets.of(74, 26, 79, 21);
        Insets mandSysGesture = Insets.of(20, 10, 57, 1);
        Insets stable = Insets.of(3, 84, 86, 22);
        Insets tappable = Insets.of(65, 82, 32, 62);

        WindowInsetsCompat result = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(sysWindow)
                .setSystemGestureInsets(sysGesture)
                .setMandatorySystemGestureInsets(mandSysGesture)
                .setStableInsets(stable)
                .setTappableElementInsets(tappable)
                .build();

        assertEquals(sysWindow, result.getSystemWindowInsets());
        assertEquals(sysGesture, result.getSystemGestureInsets());
        assertEquals(mandSysGesture, result.getMandatorySystemGestureInsets());
        assertEquals(stable, result.getStableInsets());
        assertEquals(tappable, result.getTappableElementInsets());
    }

    /**
     * Only API 20-28, only {@code setSystemWindowInsets} works.
     */
    @Test
    @SdkSuppress(minSdkVersion = 20)
    public void builder_api20() {
        Insets sysWindow = Insets.of(12, 34, 35, 31);

        WindowInsetsCompat result = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(sysWindow)
                .build();

        assertEquals(sysWindow, result.getSystemWindowInsets());
    }

    /**
     * Only API min-19, none of the setters work. Instead we just make sure we return non-null.
     */
    @Test
    public void builder_minapi() {
        Insets sysWindow = Insets.of(12, 34, 35, 31);

        WindowInsetsCompat result = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(sysWindow)
                .build();

        assertNotNull(result);
    }
}
