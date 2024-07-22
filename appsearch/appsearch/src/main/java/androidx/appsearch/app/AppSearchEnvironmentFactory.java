/*
 * Copyright 2024 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * This is a factory class for implementations needed based on the environment.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchEnvironmentFactory {
    private static volatile AppSearchEnvironment sAppSearchEnvironment;

    /** Returns the singleton instance of {@link AppSearchEnvironment}. */
    @NonNull
    public static AppSearchEnvironment getEnvironmentInstance() {
        AppSearchEnvironment localRef = sAppSearchEnvironment;
        if (localRef == null) {
            synchronized (AppSearchEnvironmentFactory.class) {
                localRef = sAppSearchEnvironment;
                if (localRef == null) {
                    sAppSearchEnvironment = localRef =
                            new JetpackAppSearchEnvironment();
                }
            }
        }
        return localRef;
    }

    /** Sets an instance of {@link AppSearchEnvironment}. for testing.*/
    @VisibleForTesting
    public static void setEnvironmentInstanceForTest(
            @NonNull AppSearchEnvironment appSearchEnvironment) {
        synchronized (AppSearchEnvironmentFactory.class) {
            sAppSearchEnvironment = appSearchEnvironment;
        }
    }

    private AppSearchEnvironmentFactory() {
    }
}
