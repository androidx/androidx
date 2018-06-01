/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.fragment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;

import androidx.navigation.NavController;
import androidx.navigation.fragment.test.ImmediateNavigationActivity;
import androidx.navigation.fragment.test.R;

import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class ImmediateNavigationTest {

    @Rule
    public ActivityTestRule<ImmediateNavigationActivity> mActivityRule =
            new ActivityTestRule<>(ImmediateNavigationActivity.class, false, false);

    @Test
    public void testNavigateInOnResume() throws Throwable {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getContext(),
                ImmediateNavigationActivity.class);

        final ImmediateNavigationActivity activity = mActivityRule.launchActivity(intent);
        instrumentation.waitForIdleSync();
        NavController navController = activity.getNavController();
        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
    }
}
