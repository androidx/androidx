/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.appcompat.widget;

import android.app.Instrumentation;
import android.os.Build;

import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/**
 * This class is for testing RTL-related functionality of {@link AppCompatSpinner}
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class AppCompatSpinnerRtlTest
        extends AppCompatBaseViewTest<AppCompatSpinnerRtlActivity, AppCompatSpinner> {
    private Instrumentation mInstrumentation;

    public AppCompatSpinnerRtlTest() {
        super(AppCompatSpinnerRtlActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        // Spinner has default background set on it
        return true;
    }

    @Override
    public void setUp() {
        super.setUp();
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @FlakyTest
    @Test
    public void testHorizontalOffsetRtl() {
        AppCompatSpinnerTest.checkOffsetIsCorrect(mInstrumentation, mContainer, 200, false, true);
    }
}
