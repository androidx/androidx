/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.core.app.GrammaticalInflectionManagerCompat.GRAMMATICAL_GENDER_MASCULINE;

import static org.junit.Assert.assertEquals;

import android.support.v4.BaseInstrumentationTestCase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GrammaticalInflectionManagerCompatTest extends
        BaseInstrumentationTestCase<GrammaticalInfectionActivity> {

    public GrammaticalInflectionManagerCompatTest() {
        super(GrammaticalInfectionActivity.class);
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void testSetGrammaticalGender() throws InterruptedException {
        GrammaticalInflectionManagerCompat.setRequestedApplicationGrammaticalGender(
                mActivityTestRule.getActivity(),
                GRAMMATICAL_GENDER_MASCULINE
        );

        mActivityTestRule.getActivity().await();

        assertEquals(GRAMMATICAL_GENDER_MASCULINE,
                GrammaticalInflectionManagerCompat.getApplicationGrammaticalGender(
                        mActivityTestRule.getActivity()));
    }
}
