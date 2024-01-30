/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.filters.SdkSuppress;

import org.junit.runners.Suite;
import org.junit.runners.model.RunnerBuilder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * TestRunner based on {@link org.junit.experimental.runners.Enclosed} to run tests in inner classes
 *
 * <p>Ignore all the test classes when the android API level of the DUT is not in the range of
 * target SDK.
 * <p>Abstract inner classes will be ignored.
 * <p>SDK annotation in inner is 1st priority, annotation in outer class is 2nd priority.
 */
public class ConstraintEnclosedTestRunner extends Suite {

    public ConstraintEnclosedTestRunner(@NonNull Class<?> klass, @NonNull RunnerBuilder builder)
            throws Throwable {
        super(builder, klass, filterClasses(klass));
    }

    @NonNull
    private static Class<?>[] filterClasses(@NonNull Class<?> klass) {
        List<Class<?>> filteredList = new ArrayList<>();

        boolean outerSdkCheckResult = checkTestTargetSdk(klass);
        for (final Class<?> clazz : klass.getClasses()) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                if (containsSdkTarget(clazz)) {
                    // Use the SDK target from the inner classes if exists.
                    if (checkTestTargetSdk(clazz)) {
                        filteredList.add(clazz);
                    }
                } else if (outerSdkCheckResult) {
                    // If the inner class doesn't have SDK annotation, check the SDK annotation in
                    // the outer class.
                    filteredList.add(clazz);
                }
            }
        }
        return filteredList.toArray(new Class<?>[filteredList.size()]);
    }

    private static boolean containsSdkTarget(@NonNull Class<?> klass) {
        return klass.getAnnotation(SdkSuppress.class) != null;
    }

    private static boolean checkTestTargetSdk(@NonNull Class<?> klass) {
        int maxSdk = Integer.MAX_VALUE;
        int minSdk = 1;
        SdkSuppress sdkSuppressAnnotation = klass.getAnnotation(SdkSuppress.class);
        if (sdkSuppressAnnotation != null) {
            maxSdk = sdkSuppressAnnotation.maxSdkVersion();
            minSdk = sdkSuppressAnnotation.minSdkVersion();
        }

        return Build.VERSION.SDK_INT >= minSdk && Build.VERSION.SDK_INT <= maxSdk;
    }
}
