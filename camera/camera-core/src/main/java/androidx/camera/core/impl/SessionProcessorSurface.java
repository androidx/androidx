/*
 * Copyright 2021 The Android Open Source Project
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

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A {@link DeferrableSurface} that is created in {@link SessionProcessor} and is added to the
 * {@link SessionConfig} for opening capture session.
 *
 * <p>It is similar as {@link ImmediateSurface} but contains output config Id that can be used to
 * query the surface the id associates with.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SessionProcessorSurface extends DeferrableSurface {
    private final Surface mSurface;
    private final int mOutputConfigId;

    public SessionProcessorSurface(@NonNull Surface surface, int outputConfigId) {
        mSurface = surface;
        mOutputConfigId = outputConfigId;
    }

    public int getOutputConfigId() {
        return mOutputConfigId;
    }

    @Override
    @NonNull
    public ListenableFuture<Surface> provideSurface() {
        return Futures.immediateFuture(mSurface);
    }
}
