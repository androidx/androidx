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

import android.support.v4.app.Fragment;

import com.android.support.lifecycle.testapp.MainActivity;
import com.android.support.lifecycle.testapp.R;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RetainedStateProviderTest extends BaseStateProviderTest<RetainedStateProvider> {

    public RetainedStateProviderTest() {
        super(false);
    }

    @Test
    public void testIntState() throws Throwable {
        testRecreation(
                provider -> provider.intStateValue("int", 261),
                provider -> assertThat(provider.intStateValue("int", 239).get(), is(261))
        );
    }

    @Test
    public void testStateValue() throws Throwable {
        final Object o = new Object();
        testRecreation(
                provider -> provider.stateValue("object").set(o),
                provider -> assertThat(provider.stateValue("object").get(), is(o))
        );
    }

    @Override
    protected RetainedStateProvider getStateProvider(MainActivity activity) {
        if (mTestVariant == TestVariant.ACTIVITY) {
            return StateProviders.retainedStateProvider(activity);
        }
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment);
        return StateProviders.retainedStateProvider(fragment);
    }
}
