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

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.DeferrableSurface;

import java.util.concurrent.Executor;

/**
 * Fake {@link SurfaceEffect} used in tests.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeSurfaceEffect implements SurfaceEffect {

    final SurfaceTexture mSurfaceTexture;
    final Surface mInputSurface;
    private final Executor mExecutor;
    private final boolean mAutoCloseSurfaceOutput;


    @Nullable
    private SurfaceRequest mSurfaceRequest;
    @Nullable
    private SurfaceOutput mSurfaceOutput;
    boolean mIsInputSurfaceReleased;
    boolean mIsOutputSurfaceRequestedToClose;

    Surface mOutputSurface;

    /**
     * Creates a {@link SurfaceEffect} that closes the {@link SurfaceOutput} automatically.
     */
    public FakeSurfaceEffect(@NonNull Executor executor) {
        this(executor, true);
    }

    /**
     * @param autoCloseSurfaceOutput if true, automatically close the {@link SurfaceOutput} once
     *                               the close request is received. Otherwise, the test needs to
     *                               get {@link #getSurfaceOutput()} and call
     *                               {@link SurfaceOutput#close()} to avoid the "Completer GCed"
     *                               error in {@link DeferrableSurface}.
     */
    FakeSurfaceEffect(@NonNull Executor executor, boolean autoCloseSurfaceOutput) {
        mSurfaceTexture = new SurfaceTexture(0);
        mInputSurface = new Surface(mSurfaceTexture);
        mExecutor = executor;
        mIsInputSurfaceReleased = false;
        mIsOutputSurfaceRequestedToClose = false;
        mAutoCloseSurfaceOutput = autoCloseSurfaceOutput;
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest request) {
        mSurfaceRequest = request;
        request.provideSurface(mInputSurface, mExecutor, result -> {
            mSurfaceTexture.release();
            mInputSurface.release();
            mIsInputSurfaceReleased = true;
        });
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        mSurfaceOutput = surfaceOutput;
        mOutputSurface = surfaceOutput.getSurface(mExecutor,
                () -> {
                    if (mAutoCloseSurfaceOutput) {
                        surfaceOutput.close();
                    }
                    mIsOutputSurfaceRequestedToClose = true;
                }
        );
    }

    @Nullable
    public SurfaceRequest getSurfaceRequest() {
        return mSurfaceRequest;
    }

    @Nullable
    public SurfaceOutput getSurfaceOutput() {
        return mSurfaceOutput;
    }

    @NonNull
    public Surface getInputSurface() {
        return mInputSurface;
    }

    @NonNull
    public Surface getOutputSurface() {
        return mOutputSurface;
    }

    public boolean isInputSurfaceReleased() {
        return mIsInputSurfaceReleased;
    }

    public boolean isOutputSurfaceRequestedToClose() {
        return mIsOutputSurfaceRequestedToClose;
    }

    /**
     * Clear up the instance to avoid the "{@link DeferrableSurface} garbage collected" error.
     */
    public void cleanUp() {
        if (mSurfaceOutput != null) {
            mSurfaceOutput.close();
        }
    }
}
