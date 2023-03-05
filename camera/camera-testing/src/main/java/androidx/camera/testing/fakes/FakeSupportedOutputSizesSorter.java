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

package androidx.camera.testing.fakes;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.SupportedOutputSizesSorter;

import java.util.List;
import java.util.Map;

/**
 * A fake implementation of {@link SupportedOutputSizesSorter} for testing.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeSupportedOutputSizesSorter extends SupportedOutputSizesSorter {

    @NonNull
    Map<UseCaseConfig<?>, List<Size>> mSupportedOutputSizes;

    /**
     * Creates a new instance of {@link FakeSupportedOutputSizesSorter}.
     *
     * @param supportedOutputSizes the supported output sizes for each use case config.
     */
    public FakeSupportedOutputSizesSorter(
            @NonNull Map<UseCaseConfig<?>, List<Size>> supportedOutputSizes) {
        super(new FakeCameraInfoInternal());
        mSupportedOutputSizes = supportedOutputSizes;
    }

    @Override
    @NonNull
    public List<Size> getSortedSupportedOutputSizes(@NonNull UseCaseConfig<?> useCaseConfig) {
        return requireNonNull(mSupportedOutputSizes.get(useCaseConfig));
    }
}
