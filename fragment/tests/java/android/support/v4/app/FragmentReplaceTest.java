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
package android.support.v4.app;

import android.app.Fragment;
import android.support.fragment.test.R;
import android.support.test.filters.SdkSuppress;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.app.test.FragmentTestActivity.TestFragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.KeyEvent;

/**
 * Test to prevent regressions in SupportFragmentManager fragment replace method. See b/24693644
 */
public class FragmentReplaceTest extends
        ActivityInstrumentationTestCase2<FragmentTestActivity> {
    private FragmentTestActivity mActivity;


    public FragmentReplaceTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @UiThreadTest
    public void testReplaceFragment() throws Throwable {
        mActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content, TestFragment.create(R.layout.fragment_a))
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
        assertNotNull(mActivity.findViewById(R.id.textA));
        assertNull(mActivity.findViewById(R.id.textB));
        assertNull(mActivity.findViewById(R.id.textC));


        mActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content, TestFragment.create(R.layout.fragment_b))
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
        assertNotNull(mActivity.findViewById(R.id.textA));
        assertNotNull(mActivity.findViewById(R.id.textB));
        assertNull(mActivity.findViewById(R.id.textC));

        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, TestFragment.create(R.layout.fragment_c))
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
        assertNull(mActivity.findViewById(R.id.textA));
        assertNull(mActivity.findViewById(R.id.textB));
        assertNotNull(mActivity.findViewById(R.id.textC));
    }

    @SdkSuppress(minSdkVersion = 11)
    @UiThreadTest
    public void testBackPressWithFrameworkFragment() throws Throwable {
        mActivity.getFragmentManager().beginTransaction()
                .add(R.id.content, new Fragment())
                .addToBackStack(null)
                .commit();
        mActivity.getFragmentManager().executePendingTransactions();
        assertEquals(1, mActivity.getFragmentManager().getBackStackEntryCount());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        assertEquals(0, mActivity.getFragmentManager().getBackStackEntryCount());
    }
}
