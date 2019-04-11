/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Size;

import androidx.camera.testing.fakes.FakeUseCase;

import java.util.Map;

/**
 * A second fake {@link UseCase}.
 *
 * <p>This is used to complement the {@link FakeUseCase} for testing instances where a use case of
 * different type is created.
 */
class FakeOtherUseCase extends UseCase {
    private volatile boolean mIsCleared = false;

    /** Creates a new instance of a {@link FakeOtherUseCase} with a given configuration. */
    FakeOtherUseCase(FakeOtherUseCaseConfig config) {
        super(config);
    }

    /** Creates a new instance of a {@link FakeOtherUseCase} with a default configuration. */
    FakeOtherUseCase() {
        this(new FakeOtherUseCaseConfig.Builder().build());
    }

    @Override
    public void clear() {
        super.clear();
        mIsCleared = true;
    }

    @Override
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(CameraX.LensFacing lensFacing) {
        return new FakeOtherUseCaseConfig.Builder().setLensFacing(
                lensFacing == null ? CameraX.LensFacing.BACK : lensFacing);
    }

    @Override
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        return suggestedResolutionMap;
    }

    /** Returns true if {@link #clear()} has been called previously. */
    public boolean isCleared() {
        return mIsCleared;
    }
}
