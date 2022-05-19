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

package androidx.camera.core.processing;

import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * A data class represents a {@link Node} input that is based on {@link Surface}s.
 */
@AutoValue
public abstract class SurfaceIn {

    /**
     * Instruction on how the buffer should be edited by the {@link Node}, one for each output.
     *
     * TODO(b/234180399): consider switching to com.google.common.collect.ImmutableList.
     */
    @SuppressWarnings("AutoValueImmutableFields")
    @NonNull
    public abstract List<SurfaceOption> getOutputOptions();

    /**
     * The input surface to read the frame from.
     */
    @NonNull
    public abstract SettableSurface getSurface();

    /**
     * Creates an instance of {@link SurfaceIn}.
     */
    @NonNull
    public static SurfaceIn create(@NonNull List<SurfaceOption> surfaceOptions,
            @NonNull SettableSurface surface) {
        return new AutoValue_SurfaceIn(surfaceOptions, surface);
    }
}
