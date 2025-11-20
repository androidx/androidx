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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;

import androidx.core.app.ActivityCompat.PermissionCompatDelegate;
import androidx.core.test.R;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActivityCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    public static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public ActivityCompatTest() {
        super(TestActivity.class);
    }

    private Activity getActivity() {
        return mActivityTestRule.getActivity();
    }

    @Test
    public void testPermissionDelegate() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                PermissionCompatDelegate delegate =
                        mock(PermissionCompatDelegate.class);

                // First test setting the delegate
                ActivityCompat.setPermissionCompatDelegate(delegate);

                ActivityCompat.requestPermissions(activity, LOCATION_PERMISSIONS, 42);
                //noinspection ConstantConditions
                verify(delegate).requestPermissions(
                        same(activity), aryEq(LOCATION_PERMISSIONS), eq(42));

                // Test clearing the delegate
                ActivityCompat.setPermissionCompatDelegate(null);

                ActivityCompat.requestPermissions(activity, LOCATION_PERMISSIONS, 42);
                verifyNoMoreInteractions(delegate);
            });
        }
    }

    @Test
    public void testPermissionNull() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                String[] permissions = new String[]{null};

                try {
                    ActivityCompat.requestPermissions(activity, permissions, 42);
                } catch (IllegalArgumentException e) {
                    assertThat(e).hasMessageThat().contains("Permission request for "
                            + "permissions " + Arrays.toString(permissions) + " must not "
                            + "contain null or empty values");
                }
            });
        }
    }

    @Test
    public void testPermissionEmpty() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                String[] permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, ""
                };

                try {
                    ActivityCompat.requestPermissions(activity, permissions, 42);
                } catch (IllegalArgumentException e) {
                    assertThat(e).hasMessageThat().contains("Permission request for "
                            + "permissions " + Arrays.toString(permissions) + " must not "
                            + "contain null or empty values");
                }
            });
        }
    }

    @Test
    public void testRequireViewByIdFound() {
        View view = getActivity().findViewById(R.id.view);
        assertSame(view, ActivityCompat.requireViewById(getActivity(), R.id.view));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdMissing() {
        // container isn't present inside activity
        ActivityCompat.requireViewById(getActivity(), R.id.container);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdInvalid() {
        // NO_ID is always invalid
        ActivityCompat.requireViewById(getActivity(), View.NO_ID);
    }

    @Test
    public void testShouldShowRequestPermissionRationaleForPostNotifications() throws Throwable {
        if (Build.VERSION.SDK_INT < 33) {
            // permission doesn't exist yet, so should return false
            assertFalse(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.POST_NOTIFICATIONS));
        }
    }

    @Test
    public void testOnSharedElementsReady() {
        AtomicInteger counter = new AtomicInteger();
        SharedElementCallback callback = new SharedElementCallback() {
        };
        android.app.SharedElementCallback.OnSharedElementsReadyListener listener =
                counter::incrementAndGet;

        // Ensure that the method wrapper works as intended.
        listener.onSharedElementsReady();
        assertEquals(1, counter.get());

        // Ensure that the callback wrapper calls the method wrapper.
        android.app.SharedElementCallback wrapper =
                new ActivityCompat.SharedElementCallback21Impl(callback);
        wrapper.onSharedElementsArrived(null, null, listener);
        assertEquals(2, counter.get());
    }
}
