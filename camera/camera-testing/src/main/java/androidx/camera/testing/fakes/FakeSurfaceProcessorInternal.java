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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.SurfaceProcessorInternal;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Fake {@link SurfaceProcessorInternal} used in tests.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeSurfaceProcessorInternal extends FakeSurfaceProcessor implements
        SurfaceProcessorInternal {

    private boolean mIsReleased;
    private int mJpegQuality = 0;

    /**
     * {@inheritDoc}
     */
    public FakeSurfaceProcessorInternal(@NonNull Executor executor) {
        this(executor, true);
    }

    /**
     * {@inheritDoc}
     */
    public FakeSurfaceProcessorInternal(@NonNull Executor executor,
            boolean autoCloseSurfaceOutput) {
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

    @Override
    @NonNull
    public ListenableFuture<Void> snapshot(int jpegQuality) {
        mJpegQuality = jpegQuality;
        return Futures.immediateFuture(null);
    }

    @IntRange(from = 0, to = 100)
    public int getJpegQuality() {
        return mJpegQuality;
    }
}
