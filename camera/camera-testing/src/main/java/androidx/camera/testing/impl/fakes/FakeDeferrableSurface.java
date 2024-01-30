/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Fake {@link DeferrableSurface} for testing.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeDeferrableSurface extends DeferrableSurface {

    @NonNull
    private final ListenableFuture<Surface> mFuture;
    @NonNull
    CallbackToFutureAdapter.Completer<Surface> mCompleter;

    public FakeDeferrableSurface(@NonNull Size size, int imageFormat) {
        super(size, imageFormat);
        mFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCompleter = completer;
                    return "FakeDeferrableSurface";
                });
    }

    @NonNull
    @Override
    protected ListenableFuture<Surface> provideSurface() {
        return mFuture;
    }

    /**
     * Set the {@link Surface} as the result of {@link #provideSurface()}.
     */
    public void setSurface(@NonNull Surface surface) {
        mCompleter.set(surface);
    }

    @Override
    public void close() {
        super.close();
        mCompleter.setCancelled();
    }
}
