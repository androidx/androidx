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

package androidx.appsearch.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An interface which exposes environment specific methods for AppSearch.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchEnvironment {

    /** Returns the directory to initialize appsearch based on the environment. */
    @NonNull File getAppSearchDir(@NonNull Context context, @Nullable UserHandle userHandle);

    /** Returns the correct context for the user based on the environment. */
    @NonNull Context createContextAsUser(@NonNull Context context, @NonNull UserHandle userHandle);

    /** Returns an ExecutorService based on given parameters. */
    @NonNull ExecutorService createExecutorService(
            int corePoolSize,
            int maxConcurrency,
            long keepAliveTime,
            @NonNull TimeUnit unit,
            @NonNull BlockingQueue<Runnable> workQueue,
            int priority);

    /** Returns an ExecutorService with a single thread. */
    @NonNull ExecutorService createSingleThreadExecutor();

    /** Creates and returns an Executor with cached thread pools. */
    @NonNull ExecutorService createCachedThreadPoolExecutor();

    /**
     * Returns a cache directory for creating temporary files like in case of migrating documents.
     */
    @Nullable File getCacheDir(@NonNull Context context);

    /** Returns if we can log INFO level logs. */
    boolean isInfoLoggingEnabled();

    /**
     * The different environments that AppSearch code might be built in.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            JETPACK_ENVIRONMENT,
            FRAMEWORK_ENVIRONMENT,
            PLAY_SERVICES_ENVIRONMENT,
    })
    @interface EnvironmentType {
    }

    /** This code is being built in the Jetpack Environment */
    int JETPACK_ENVIRONMENT = 1;

    /** This code is being built in the Android Framework Environment */
    int FRAMEWORK_ENVIRONMENT = 2;

    /** This code is being built in the internal environment for Play Services code. */
    int PLAY_SERVICES_ENVIRONMENT = 3;

    /** Returns the {@code EnvironmentType} for this environment. */
    @EnvironmentType int getEnvironment();

    /** Returns a file with the given name under the given parent path. */
    @NonNull File getFile(@NonNull File fileParentPath, @NonNull String fileName);

    /**
     * Populates the {@link PackageInfo#signatures} field for P- devices.
     *
     * <p>This is a GMSCore-specific fallback used after retrieving packages on P- devices. It
     * ensures that a certificate history is still available for verification.
     *
     * @throws PackageManager.NameNotFoundException If the package is not found.
     */
    void populateSignatures(@NonNull Context context, @NonNull PackageInfo packageInfo)
            throws PackageManager.NameNotFoundException;
}
