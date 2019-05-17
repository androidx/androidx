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

package androidx.camera.testing.fakes;

import android.util.Size;

import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseConfig;

import java.util.Map;

/**
 * A fake {@link UseCase}.
 */
public class FakeUseCase extends UseCase {
    private volatile boolean mIsCleared = false;

    /**
     * Creates a new instance of a {@link FakeUseCase} with a given configuration.
     */
    public FakeUseCase(FakeUseCaseConfig config) {
        super(config);
    }

    /**
     * Creates a new instance of a {@link FakeUseCase} with a default configuration.
     */
    public FakeUseCase() {
        this(new FakeUseCaseConfig.Builder().build());
    }

    @Override
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(LensFacing lensFacing) {
        return new FakeUseCaseConfig.Builder()
                .setLensFacing(lensFacing)
                .setSessionOptionUnpacker(new SessionConfig.OptionUnpacker() {
                    @Override
                    public void unpack(UseCaseConfig<?> useCaseConfig,
                            SessionConfig.Builder sessionConfigBuilder) {
                    }
                });
    }

    @Override
    public void clear() {
        super.clear();
        mIsCleared = true;
    }

    @Override
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        return suggestedResolutionMap;
    }

    /**
     * Returns true if {@link #clear()} has been called previously.
     */
    public boolean isCleared() {
        return mIsCleared;
    }
}
