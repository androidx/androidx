/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.app;

import static org.junit.Assert.assertEquals;

import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.os.LocaleListCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * TestCase to verify the working of {@link androidx.core.app.LocaleManagerCompat.getSystemLocales}.
 */
public class GetSystemLocalesTest extends BaseInstrumentationTestCase<GetSystemLocalesActivity> {

    LocaleListCompat mExpectedSystemLocales = null;

    public GetSystemLocalesTest() {
        super(GetSystemLocalesActivity.class);
    }


    @Before
    public void setup() {
        // Since the application has no custom locales set, the initial locales should be same as
        // the system locales.
        mExpectedSystemLocales =
                LocaleManagerCompat.getConfigurationLocales(mActivityTestRule.getActivity()
                        .getResources().getConfiguration());
    }

    @Test
    public void testGetSystemLocales_noAppLocalesSet_systemLocalesSameAsInitLocales() {
        // verify that the system locales are the same as the initial locales.
        assertEquals(
                mExpectedSystemLocales,
                LocaleManagerCompat.getSystemLocales(mActivityTestRule.getActivity())
        );
    }
}
