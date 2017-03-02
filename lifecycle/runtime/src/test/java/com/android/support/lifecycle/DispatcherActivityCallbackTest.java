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

package com.android.support.lifecycle;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
@SmallTest
public class DispatcherActivityCallbackTest {
    @Test
    public void onCreateFrameworkActivity() {
        FragmentLifecycleDispatcher.DispatcherActivityCallback callback =
                new FragmentLifecycleDispatcher.DispatcherActivityCallback();
        Activity activity = mock(Activity.class);
        callback.onActivityCreated(activity, mock(Bundle.class));
        Mockito.verifyNoMoreInteractions(activity);
        // assert no crash
    }

    @Test
    public void onCreateFragmentActivity() {
        FragmentLifecycleDispatcher.DispatcherActivityCallback callback =
                new FragmentLifecycleDispatcher.DispatcherActivityCallback();
        FragmentActivity activity = mock(FragmentActivity.class);
        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);

        callback.onActivityCreated(activity, mock(Bundle.class));
        verify(activity).getSupportFragmentManager();
        verify(fragmentManager).registerFragmentLifecycleCallbacks(
                any(FragmentManager.FragmentLifecycleCallbacks.class), eq(true));
    }
}
