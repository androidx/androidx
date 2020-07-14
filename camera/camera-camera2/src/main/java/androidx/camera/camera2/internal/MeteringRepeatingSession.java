/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

/**
 * A SessionConfig to act a Metering repeating use case.
 *
 * <p> When ImageCapture only to do the action of takePicture, the MeteringRepeating is
 * created in Camera2 layer to make Camera2 have the repeating surface to metering the auto 3A or
 * wait for 3A converged.
 *
 */
class MeteringRepeatingSession {
    private static final String TAG = "MeteringRepeating";
    private DeferrableSurface mDeferrableSurface;
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @NonNull
    private final SessionConfig mSessionConfig;

    /** Creates a new instance of a {@link MeteringRepeatingSession}. */
    MeteringRepeatingSession() {
        MeteringRepeatingConfig configWithDefaults = new MeteringRepeatingConfig();

        // Create the metering DeferrableSurface
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(0, 0);
        Surface surface = new Surface(surfaceTexture);

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(configWithDefaults);
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        mDeferrableSurface = new ImmediateSurface(surface);

        Futures.addCallback(mDeferrableSurface.getTerminationFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                surface.release();
                surfaceTexture.release();
            }

            @Override
            public void onFailure(Throwable t) {
                throw new IllegalStateException("Future should never "
                        + "fail. Did it get completed by GC?", t);
            }
        }, CameraXExecutors.directExecutor());

        builder.addSurface(mDeferrableSurface);

        mSessionConfig = builder.build();
    }

    @NonNull
    SessionConfig getSessionConfig() {
        return mSessionConfig;
    }

    @NonNull
    String getName() {
        return "MeteringRepeating";
    }

    /**
     * The {@link MeteringRepeatingSession} should only be used once and afterwards should be
     * cleared.
     */
    void clear() {
        if (DEBUG) {
            Log.d(TAG, "MeteringRepeating clear!");
        }
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = null;
    }

    /**
     * A minimal config that contains a {@link SessionConfig.OptionUnpacker} in order to unpack
     * the camera2 related options.
     */
    private static class MeteringRepeatingConfig implements UseCaseConfig<UseCase> {
        @NonNull
        private final Config mConfig;
        MeteringRepeatingConfig() {
            MutableOptionsBundle mutableOptionsBundle = MutableOptionsBundle.create();
            mutableOptionsBundle.insertOption(UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER,
                    new Camera2SessionOptionUnpacker());
            mConfig = mutableOptionsBundle;
        }

        @NonNull
        @Override
        public Config getConfig() {
            return mConfig;
        }
    }
}


