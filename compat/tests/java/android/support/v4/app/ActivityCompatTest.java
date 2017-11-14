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

package android.support.v4.app;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.app.Activity;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.app.ActivityCompat.PermissionCompatDelegate;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActivityCompatTest extends BaseInstrumentationTestCase<TestSupportActivity> {

    public ActivityCompatTest() {
        super(TestSupportActivity.class);
    }

    @SdkSuppress(minSdkVersion = 24)
    @SmallTest
    @Test
    public void testPermissionDelegate() {
        Activity activity = mActivityTestRule.getActivity();
        ActivityCompat.PermissionCompatDelegate delegate = mock(PermissionCompatDelegate.class);

        // First test setting the delegate
        ActivityCompat.setPermissionCompatDelegate(delegate);

        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        verify(delegate).requestPermissions(same(activity), aryEq(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}), eq(42));

        // Test clearing the delegate
        ActivityCompat.setPermissionCompatDelegate(null);

        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        verifyNoMoreInteractions(delegate);
    }
}
