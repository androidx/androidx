/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle.state;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.graphics.Point;
import android.support.test.filters.MediumTest;
import android.support.v4.app.Fragment;

import com.android.support.lifecycle.testapp.MainActivity;
import com.android.support.lifecycle.testapp.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@MediumTest
@RunWith(Parameterized.class)
public class SavedStateProviderTest extends BaseStateProviderTest<SavedStateProvider> {

    public SavedStateProviderTest() {
        super(true);
    }

    @Test
    public void testIntSavedState() throws Throwable {
        testRecreation(
                provider -> provider.intStateValue("xuint", 261),
                provider -> assertThat(provider.intStateValue("xuint", 239).get(), is(261))
        );
    }

    @Test
    public void testEmptyRecreation() throws Throwable {
        testRecreation(
                provider -> provider.intStateValue("xuint", 261),
                provider -> { },
                provider -> assertThat(provider.intStateValue("xuint", 239).get(), is(261))
        );
    }

    @Test
    public void testParcelableSavedState() throws Throwable {
        testRecreation(
                provider -> provider.<Point>stateValue("object").set(new Point(261, 141)),
                provider -> assertThat(provider.stateValue("object").get(),
                        is(new Point(261, 141)))
        );
    }

    @Test
    public void testSameKey() throws Throwable {
        String key = "shared_key";
        testRecreation(
                provider -> {
                    provider.<Point>stateValue(key).set(new Point(261, 141));
                    provider.intStateValue(key, 10);
                },
                provider -> {
                    assertThat(provider.intStateValue(key).get(), is(10));
                    assertThat(provider.<Point>stateValue(key).get(), is((Point) null));
                }
        );
    }

    @Override
    protected SavedStateProvider getStateProvider(MainActivity activity) {
        if (mTestVariant == TestVariant.ACTIVITY) {
            return StateProviders.savedStateProvider(activity);
        }
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment);
        return StateProviders.savedStateProvider(fragment);
    }
}
