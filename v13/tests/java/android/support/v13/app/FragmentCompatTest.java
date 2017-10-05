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

package android.support.v13.app;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v13.app.FragmentCompat.PermissionCompatDelegate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.HONEYCOMB)
public class FragmentCompatTest {
    @Rule
    public ActivityTestRule<FragmentCompatTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentCompatTestActivity.class);

    private Activity mActivity;
    private TestFragment mFragment;

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mFragment = attachTestFragment();
    }

    @SmallTest
    @Test
    public void testFragmentCompatDelegate() {
        FragmentCompat.PermissionCompatDelegate delegate = mock(PermissionCompatDelegate.class);

        // First test setting the delegate
        FragmentCompat.setPermissionCompatDelegate(delegate);

        FragmentCompat.requestPermissions(mFragment, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        verify(delegate).requestPermissions(same(mFragment),
                aryEq(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}), eq(42));

        // Now test clearing the delegate
        FragmentCompat.setPermissionCompatDelegate(null);

        FragmentCompat.requestPermissions(mFragment, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION}, 42);

        verifyNoMoreInteractions(delegate);
    }

    private TestFragment attachTestFragment() throws Throwable {
        final TestFragment fragment = new TestFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getFragmentManager().beginTransaction()
                        .add(fragment, null)
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
                mActivity.getFragmentManager().executePendingTransactions();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return fragment;
    }

    /**
     * Empty class to satisfy java class dependency.
     */
    public static class TestFragment extends Fragment implements
            FragmentCompat.OnRequestPermissionsResultCallback {
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                @NonNull int[] grantResults) {}
    }
}
