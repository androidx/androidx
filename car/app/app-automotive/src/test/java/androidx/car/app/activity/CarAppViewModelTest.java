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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarAppViewModel} */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarAppViewModelTest {
    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName(
            ApplicationProvider.getApplicationContext(), "Class1");
    private static final int TEST_DISPLAY_ID = 123;
    private static final Intent TEST_INTENT = new Intent("TestAction");

    private final Application mApplication = ApplicationProvider.getApplicationContext();
    private CarAppViewModel mCarAppViewModel;
    private ICarAppActivity mICarAppActivity;

    @Before
    public void setUp() {
        mCarAppViewModel = new CarAppViewModel(mApplication, TEST_COMPONENT_NAME);
        mICarAppActivity = mock(ICarAppActivity.class);
    }

    @Test
    public void testSetup() {
        assertThat(mCarAppViewModel.getServiceConnectionManager()).isNotNull();
        assertThat(mCarAppViewModel.getServiceConnectionManager().getServiceComponentName())
                .isEqualTo(TEST_COMPONENT_NAME);
        assertThat(mCarAppViewModel.getServiceDispatcher()).isNotNull();
    }

    @Test
    public void testBind_serviceConnectionManager_invoke() {
        ServiceConnectionManager serviceConnectionManager = mock(ServiceConnectionManager.class);
        mCarAppViewModel.setServiceConnectionManager(serviceConnectionManager);
        mCarAppViewModel.bind(TEST_INTENT, mICarAppActivity, TEST_DISPLAY_ID);

        verify(serviceConnectionManager).bind(TEST_INTENT, mICarAppActivity, TEST_DISPLAY_ID);
    }

    @Test
    public void testUnbind_serviceConnectionManager_invoke() {
        ServiceConnectionManager serviceConnectionManager = mock(ServiceConnectionManager.class);
        mCarAppViewModel.setServiceConnectionManager(serviceConnectionManager);
        mCarAppViewModel.unbind();

        verify(serviceConnectionManager).unbind();
    }

    @Test
    public void testErrorHandler_capturesErrorEvent() {
        ErrorHandler.ErrorType errorType = ErrorHandler.ErrorType.HOST_ERROR;
        Throwable exception = new IllegalStateException();

        ErrorHandler errorHandler =
                mCarAppViewModel.getServiceConnectionManager().getErrorHandler();
        errorHandler.onError(errorType, exception);

        CarAppViewModel.ErrorEvent errorEvent = mCarAppViewModel.getErrorEvent().getValue();
        assertThat(errorEvent).isNotNull();
        assertThat(errorEvent.getErrorType()).isEqualTo(errorType);
        assertThat(errorEvent.getException()).isEqualTo(exception);
    }
}
