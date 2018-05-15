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

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;

import androidx.navigation.fragment.test.EmbeddedXmlActivity;

import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class EmbeddedXmlTest {

    @Rule
    public ActivityTestRule<EmbeddedXmlActivity> mActivityRule =
            new ActivityTestRule<>(EmbeddedXmlActivity.class, false, false);

    @Test
    public void testRecreate() throws Throwable {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getContext(),
                EmbeddedXmlActivity.class);

        final EmbeddedXmlActivity activity = mActivityRule.launchActivity(intent);
        instrumentation.waitForIdleSync();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
    }
}
