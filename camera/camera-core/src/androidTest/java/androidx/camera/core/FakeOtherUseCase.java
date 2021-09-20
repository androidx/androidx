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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.testing.fakes.FakeUseCase;

/**
 * A second fake {@link UseCase}.
 *
 * <p>This is used to complement the {@link FakeUseCase} for testing instances where a use case of
 * different type is created.
 */
@RequiresApi(21)
public class FakeOtherUseCase extends UseCase {
    private volatile boolean mIsDetached = false;

    /** Creates a new instance of a {@link FakeOtherUseCase} with a given configuration. */
    public FakeOtherUseCase(FakeOtherUseCaseConfig config) {
        super(config);
    }

    /** Creates a new instance of a {@link FakeOtherUseCase} with a default configuration. */
    FakeOtherUseCase() {
        this(new FakeOtherUseCaseConfig.Builder().getUseCaseConfig());
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mIsDetached = true;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    @NonNull
    protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
        return suggestedResolution;
    }

    /** Returns true if {@link #onDetached()} has been called previously. */
    public boolean isDetached() {
        return mIsDetached;
    }
}
