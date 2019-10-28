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

package androidx.lifecycle;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DispatcherActivityCallbackTest {
    @Test
    public void onCreateFrameworkActivity() {
        LifecycleDispatcher.DispatcherActivityCallback callback =
                new LifecycleDispatcher.DispatcherActivityCallback();
        Activity activity = mock(Activity.class);
        checkReportFragment(callback, activity);
    }

    @SuppressLint("CommitTransaction")
    private void checkReportFragment(LifecycleDispatcher.DispatcherActivityCallback callback,
            Activity activity) {
        android.app.FragmentManager fm = mock(android.app.FragmentManager.class);
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(activity.getFragmentManager()).thenReturn(fm);
        when(fm.beginTransaction()).thenReturn(transaction);
        when(transaction.add(any(Fragment.class), anyString())).thenReturn(transaction);
        callback.onActivityCreated(activity, mock(Bundle.class));
        verify(activity).getFragmentManager();
        verify(fm).beginTransaction();
        verify(transaction).add(any(ReportFragment.class), anyString());
        verify(transaction).commit();
    }
}
