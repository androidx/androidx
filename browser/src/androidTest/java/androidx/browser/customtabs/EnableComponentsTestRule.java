/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.customtabs;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Custom Tabs and Trusted Web Activity functionality require Activities and Services to be
 * found in the manifest. We want to make it explicit which Activities/Services are required for
 * each test and not let components for one test interfere with another.
 */
public class EnableComponentsTestRule extends TestWatcher {
    private final List<Class<?>> mComponents;

    /**
     * Creates this TestRule which will enable the given components and disable them after the
     * tests.
     */
    public EnableComponentsTestRule(Class<?>... components) {
        // TODO(peconn): Figure out some generic bounds that allows a list of Classes that are
        // either Services or Actvities.
        mComponents = new ArrayList<>(Arrays.asList(components));
    }

    @Override
    protected void starting(Description description) {
        setEnabled(true);
    }

    @Override
    protected void finished(Description description) {
        setEnabled(false);
    }

    /**
     * Manually disables an already enabled component.
     */
    public void manuallyDisable(Class<?> clazz) {
        setComponentEnabled(clazz, false);
    }

    /**
     * Manually enables a component. Will be disabled when test finishes.
     */
    public void manuallyEnable(Class<?> clazz) {
        setComponentEnabled(clazz, true);
        mComponents.add(clazz);
    }

    private void setEnabled(boolean enabled) {
        for (Class<?> component : mComponents) {
            setComponentEnabled(component, enabled);
        }
    }

    private static void setComponentEnabled(Class<?> clazz, boolean enabled) {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        ComponentName name = new ComponentName(context, clazz);

        int newState = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        int flags = PackageManager.DONT_KILL_APP;

        if (pm.getComponentEnabledSetting(name) != newState) {
            pm.setComponentEnabledSetting(name, newState, flags);
        }
    }
}
