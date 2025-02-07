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

package androidx.xr.scenecore.impl.perception;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.scenecore.impl.perception.exceptions.FailedToInitializeException;
import androidx.xr.scenecore.impl.perception.exceptions.LibraryLoadingException;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * A library for handling perception on Jetpack XR.This will create and manage one or more OpenXR
 * sessions and manage calls to it.
 */
@SuppressWarnings({"BanSynchronizedMethods", "BanConcurrentHashMap"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PerceptionLibrary {
    private static final String TAG = "PerceptionLibrary";

    private static final String NATIVE_LIBRARY_NAME = "androidx.xr.runtime.openxr";
    private static final ConcurrentHashMap<Activity, Session> sActivitySessionMap =
            new ConcurrentHashMap<>();

    @SuppressWarnings("NonFinalStaticField")
    private static volatile boolean sLibraryLoaded = false;

    private Session mSession = null;

    public PerceptionLibrary() {}

    @SuppressWarnings("VisiblySynchronized")
    protected static synchronized void loadLibraryAsync(@NonNull String nativeLibraryName) {
        if (sLibraryLoaded) {
            return;
        }
        Log.i(TAG, "Loading native library: " + nativeLibraryName);
        try {
            System.loadLibrary(nativeLibraryName);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Unable to load " + nativeLibraryName);
            return;
        }
        sLibraryLoaded = true;
    }

    /**
     * Initializes a new session. It is an error to call this if there is already a session
     * associated with the activity.
     *
     * @param activity This activity to associate with the OpenXR session.
     * @param referenceSpaceType The base space type of the session.
     * @param executor This executor is used to poll the OpenXR event loop.
     * @return a new Session or null if there was an error creating the session.
     * @throws java.util.concurrent.ExecutionException if there was an error to initialize the
     *     session. The cause of the ExecutionException will be an IllegalStateException if a valid
     *     session already exists, LibraryLoadingException if the internal native library failed to
     *     load, and a FailedToInitializeException if there was a failure to initialize the session
     *     internally.
     */
    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Nullable
    public ListenableFuture<Session> initSession(
            @NonNull Activity activity,
            @PerceptionLibraryConstants.OpenXrSpaceType int referenceSpaceType,
            @NonNull ExecutorService executor) {

        ResolvableFuture<Session> future = ResolvableFuture.create();
        executor.execute(
                () -> {
                    // TODO: b/373922954 - Early out of some of these operations if the future is
                    // cancelled.
                    if (!sLibraryLoaded) {
                        loadLibraryAsync(NATIVE_LIBRARY_NAME);
                    }
                    if (!sLibraryLoaded) {
                        Log.i(TAG, "Cannot init session since the native library failed to load.");
                        future.setException(
                                new LibraryLoadingException("Native library failed to load."));
                        return;
                    }
                    Log.i(TAG, stringFromJNI());
                    if (mSession != null) {
                        future.setException(new IllegalStateException("Session already exists."));
                        return;
                    }
                    if (sActivitySessionMap.containsKey(activity)) {
                        future.setException(
                                new IllegalStateException(
                                        "Session already exists for the provided activity."));
                        return;
                    }
                    Session session = new Session(activity, referenceSpaceType, executor);
                    if (!session.initSession()) {
                        Log.e(TAG, "Failed to initialize a session.");
                        future.setException(
                                new FailedToInitializeException("Failed to initialize a session."));
                        return;
                    }

                    Log.i(TAG, "Loaded perception library.");
                    // Do another check to make sure another session wasn't created for this
                    // activity
                    // while we were initializing it.
                    if (sActivitySessionMap.putIfAbsent(activity, session) != null) {
                        future.setException(
                                new IllegalStateException(
                                        "Session already exists for the provided activity."));
                    }
                    mSession = session;
                    future.set(mSession);
                });
        return future;
    }

    /** Returns the previously created session or null. */
    @SuppressWarnings("VisiblySynchronized")
    @Nullable
    public synchronized Session getSession() {
        return mSession;
    }

    /** Returns the activity associated with the session. */
    @NonNull
    public Activity getActivity() {
        return mSession.mActivity;
    }

    /**
     * JNI function that returns a string. This is a temporary function to validate the JNI
     * implementation.
     */
    private native String stringFromJNI();
}
