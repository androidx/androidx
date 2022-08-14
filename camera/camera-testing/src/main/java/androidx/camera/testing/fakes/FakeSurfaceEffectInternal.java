/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.processing.SurfaceEffectInternal;

import java.util.concurrent.Executor;

/**
 * Fake {@link SurfaceEffectInternal} used in tests.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeSurfaceEffectInternal extends FakeSurfaceEffect implements SurfaceEffectInternal {

    private boolean mIsReleased;

    /**
     * {@inheritDoc}
     */
    public FakeSurfaceEffectInternal(@NonNull Executor executor) {
        this(executor, true);
    }

    /**
     * {@inheritDoc}
     */
    public FakeSurfaceEffectInternal(@NonNull Executor executor, boolean autoCloseSurfaceOutput) {
        super(executor, autoCloseSurfaceOutput);
        mIsReleased = false;
    }

    public boolean isReleased() {
        return mIsReleased;
    }

    @Override
    public void release() {
        mIsReleased = true;
    }
}
