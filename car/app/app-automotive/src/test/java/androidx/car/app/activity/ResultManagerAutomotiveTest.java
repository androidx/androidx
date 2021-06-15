/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ResultManagerAutomotive}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ResultManagerAutomotiveTest {
    private final ComponentName mRendererComponent = new ComponentName(
            ApplicationProvider.getApplicationContext(), getClass().getName());
    private final Application mApplication = ApplicationProvider.getApplicationContext();
    private CarAppActivity mCarAppActivity = mock(CarAppActivity.class);
    private ResultManagerAutomotive mResultManager = new ResultManagerAutomotive();
    private CarAppViewModel mCarAppViewModel;

    @Before
    public void setUp() {
        mCarAppViewModel = new CarAppViewModel(mApplication, mRendererComponent);
    }

    @Test
    public void setResult_nullActivity_ignored() {
        mResultManager.setCarAppResult(-1, new Intent("foo"));
        verify(mCarAppActivity, never()).setResult(anyInt(), any());
    }

    @Test
    public void setResult_nonNullActivity_resultIsSet() {
        mCarAppViewModel.setActivity(mCarAppActivity);
        mResultManager.setCarAppResult(-1, new Intent("foo"));
        verify(mCarAppActivity, times(1)).setResult(anyInt(), any());
    }

    @Test
    public void getCallingComponent_nullActivity_returnsNull() {
        ComponentName componentName = mResultManager.getCallingComponent();
        assertThat(componentName).isNull();
    }

    @Test
    public void getCallingComponent_noCaller_returnsNull() {
        mCarAppViewModel.setActivity(mCarAppActivity);
        ComponentName componentName = mResultManager.getCallingComponent();
        assertThat(componentName).isNull();
    }

    @Test
    public void getCallingComponent_activityCalledForResult_returnsCallerActivity() {
        ComponentName testComponentName = new ComponentName("foo", "bar");
        when(mCarAppActivity.getCallingActivity()).thenReturn(testComponentName);
        mCarAppViewModel.setActivity(mCarAppActivity);
        ComponentName componentName = mResultManager.getCallingComponent();
        assertThat(componentName).isEqualTo(testComponentName);
    }
}
