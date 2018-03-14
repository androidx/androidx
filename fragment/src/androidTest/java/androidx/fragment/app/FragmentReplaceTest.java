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
package androidx.fragment.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.app.test.FragmentTestActivity.TestFragment;
import androidx.fragment.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to prevent regressions in SupportFragmentManager fragment replace method. See b/24693644
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class FragmentReplaceTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private Instrumentation mInstrumentation;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testReplaceFragment() throws Throwable {
        final FragmentActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        fm.beginTransaction()
                .add(R.id.content, TestFragment.create(R.layout.fragment_a))
                .addToBackStack(null)
                .commit();
        executePendingTransactions(fm);
        assertNotNull(activity.findViewById(R.id.textA));
        assertNull(activity.findViewById(R.id.textB));
        assertNull(activity.findViewById(R.id.textC));


        fm.beginTransaction()
                .add(R.id.content, TestFragment.create(R.layout.fragment_b))
                .addToBackStack(null)
                .commit();
        executePendingTransactions(fm);
        assertNotNull(activity.findViewById(R.id.textA));
        assertNotNull(activity.findViewById(R.id.textB));
        assertNull(activity.findViewById(R.id.textC));

        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, TestFragment.create(R.layout.fragment_c))
                .addToBackStack(null)
                .commit();
        executePendingTransactions(fm);
        assertNull(activity.findViewById(R.id.textA));
        assertNull(activity.findViewById(R.id.textB));
        assertNotNull(activity.findViewById(R.id.textC));
    }

    private void executePendingTransactions(final FragmentManager fm) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.executePendingTransactions();
            }
        });
    }

    @Test
    public void testBackPressWithFrameworkFragment() throws Throwable {
        final Activity activity = mActivityRule.getActivity();

        activity.getFragmentManager().beginTransaction()
                .add(R.id.content, new Fragment())
                .addToBackStack(null)
                .commit();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getFragmentManager().executePendingTransactions();
            }
        });
        assertEquals(1, activity.getFragmentManager().getBackStackEntryCount());

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        assertEquals(0, activity.getFragmentManager().getBackStackEntryCount());
    }
}
