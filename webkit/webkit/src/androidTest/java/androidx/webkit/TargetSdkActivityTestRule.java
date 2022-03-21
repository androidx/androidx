/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.webkit;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * This class is used to override the default targetSdkVersion value in ApplicationInfo.
 */
@SuppressWarnings("deprecation")
public class TargetSdkActivityTestRule<T extends Activity> extends
        androidx.test.rule.ActivityTestRule<T> {
    private int mTargetSdk;
    private Context mAppContext;

    public TargetSdkActivityTestRule(Class<T> activityClass, int targetSdk) {
        super(activityClass);
        mTargetSdk = targetSdk;
    }

    @Override
    protected void beforeActivityLaunched() {
        try {
            runOnUiThread(() -> {
                        mAppContext = spy(
                                InstrumentationRegistry.getInstrumentation().getTargetContext()
                                        .getApplicationContext());
                        ApplicationInfo appInfo = mAppContext.getApplicationInfo();
                        appInfo.targetSdkVersion = mTargetSdk;
                        when(mAppContext.getApplicationInfo()).thenReturn(appInfo);
                    }
            );
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
