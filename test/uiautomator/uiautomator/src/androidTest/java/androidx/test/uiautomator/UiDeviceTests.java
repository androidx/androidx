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

package androidx.test.uiautomator;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.mockito.Mockito;

public class UiDeviceTests {

    @Test
    public void testGetUiAutomation_withDefaultFlags() {
        // Get the default flags
        final int defaultFlags = Configurator.getInstance().getUiAutomationFlags();

        // Setup mocks
        Instrumentation mockInstrumentation = Mockito.mock(Instrumentation.class);

        // Can't mock UiAutomation, so return a real instance
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        doReturn(automation).when(mockInstrumentation).getUiAutomation(anyInt());
        doReturn(automation).when(mockInstrumentation).getUiAutomation();

        // Get a UiDevice instance to test and use it to obtain a UiAutomation instance
        UiDevice device = Mockito.spy(new UiDevice(mockInstrumentation));
        UiAutomation uiAutomation = device.getUiAutomation();

        // Verify that the UiAutomation instance was obtained with the right arguments
        if (UiDevice.API_LEVEL_ACTUAL > Build.VERSION_CODES.M) {
            // On N+, we should obtain a UiAutomation instance using the default flags
            Mockito.verify(mockInstrumentation, atLeastOnce()).getUiAutomation(eq(defaultFlags));
        } else {
            // Prior to N, flags should be ignored
            Mockito.verify(mockInstrumentation, atLeastOnce()).getUiAutomation();
        }
    }

    @Test
    public void testGetUiAutomation_withCustomFlags() {
        // Remember the default flags
        final int defaultFlags = Configurator.getInstance().getUiAutomationFlags();

        // Set some custom flags
        final int flags = 5;
        Configurator.getInstance().setUiAutomationFlags(flags);

        // Setup mocks
        Instrumentation mockInstrumentation = Mockito.mock(Instrumentation.class);

        // Can't mock UiAutomation, so return a real instance
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        doReturn(automation).when(mockInstrumentation).getUiAutomation(anyInt());
        doReturn(automation).when(mockInstrumentation).getUiAutomation();

        // Get a UiDevice instance to test and use it to obtain a UiAutomation instance
        UiDevice device = Mockito.spy(new UiDevice(mockInstrumentation));
        UiAutomation uiAutomation = device.getUiAutomation();

        // Verify that the UiAutomation instance was obtained with the right arguments
        if (UiDevice.API_LEVEL_ACTUAL > Build.VERSION_CODES.M) {
            // On N+, we should obtain a UiAutomation instance using the custom flags
            Mockito.verify(mockInstrumentation, atLeastOnce()).getUiAutomation(eq(flags));
        } else {
            // Prior to N, flags should be ignored
            Mockito.verify(mockInstrumentation, atLeastOnce()).getUiAutomation();
        }

        // Reset the flags
        Configurator.getInstance().setUiAutomationFlags(defaultFlags);
    }
}
