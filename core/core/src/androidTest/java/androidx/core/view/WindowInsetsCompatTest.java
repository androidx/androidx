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
import static org.junit.Assert.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
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
}
