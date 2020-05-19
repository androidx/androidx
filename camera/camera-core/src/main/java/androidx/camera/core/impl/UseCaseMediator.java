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

package androidx.camera.core.impl;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.camera.core.internal.CameraUseCaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of {@link UseCase}.
 *
 * <p>The set of {@link UseCase} instances have synchronized interactions with a single {@link
 * CameraUseCaseAdapter}.
 */
public final class UseCaseMediator {
    private static final String TAG = "UseCaseMediator";

    // The lock for accessing the list of UseCases.
    private final Object mUseCasesLock = new Object();

    @GuardedBy("mUseCasesLock")
    private final List<UseCase> mUseCases = new ArrayList<>();

    private volatile boolean mIsActive = false;

    private final CameraUseCaseAdapter mCameraUseCaseAdapter;

    public UseCaseMediator(@NonNull CameraUseCaseAdapter cameraUseCaseAdaptor) {
        mCameraUseCaseAdapter = cameraUseCaseAdaptor;
    }

    /** Starts all the use cases so that they are brought into an online state. */
    public void start() {
        synchronized (mUseCasesLock) {
            mCameraUseCaseAdapter.getCameraInternal().attachUseCases(mUseCases);
            mIsActive = true;
        }
    }

    /** Stops all the use cases so that they are brought into an offline state. */
    public void stop() {
        synchronized (mUseCasesLock) {
            mCameraUseCaseAdapter.getCameraInternal().detachUseCases(mUseCases);
            mIsActive = false;
        }
    }

    /**
     * Adds the {@link UseCase} to the mediator.
     *
     * @return true if the use case is added, or false if the use case already exists in the
     * mediator.
     */
    public boolean addUseCase(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.add(useCase);
        }
    }

    /** Returns true if the {@link UseCase} is contained in the mediator. */
    public boolean contains(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.contains(useCase);
        }
    }

    /**
     * Removes the {@link UseCase} from the mediator.
     *
     * @return Returns true if the use case is removed. Otherwise returns false (if the use case did
     * not exist in the mediator).
     */
    public boolean removeUseCase(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.remove(useCase);
        }
    }

    /**
     * Called when lifecycle ends. Destroys all use cases in this mediator.
     */
    public void destroy() {
        List<UseCase> useCasesToClear = new ArrayList<>();
        synchronized (mUseCasesLock) {
            useCasesToClear.addAll(mUseCases);
            mUseCases.clear();
        }

        Log.d(TAG, "Destroying use cases: " + useCasesToClear);
        mCameraUseCaseAdapter.detachUseCases(useCasesToClear);
    }

    /**
     * Returns the collection of all the use cases currently contained by
     * the{@link UseCaseMediator}.
     */
    @NonNull
    public List<UseCase> getUseCases() {
        synchronized (mUseCasesLock) {
            return Collections.unmodifiableList(mUseCases);
        }
    }

    /**
     * Returns the {@link CameraUseCaseAdapter} that is associated with the mediator.
     */
    @NonNull
    public CameraUseCaseAdapter getCameraUseCaseAdaptor() {
        return mCameraUseCaseAdapter;
    }

    public boolean isActive() {
        return mIsActive;
    }
}
