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
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ImageProcessor;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * A fake {@link CameraEffect} with {@link ImageProcessor}
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeImageEffect extends CameraEffect {

    public FakeImageEffect(
            @NonNull Executor processorExecutor,
            @NonNull ImageProcessor imageProcessor) {
        this(processorExecutor, imageProcessor, throwable -> {
        });
    }

    public FakeImageEffect(
            @NonNull Executor processorExecutor,
            @NonNull ImageProcessor imageProcessor,
            @NonNull Consumer<Throwable> errorListener) {
        super(IMAGE_CAPTURE, processorExecutor, imageProcessor, errorListener);
    }
}
