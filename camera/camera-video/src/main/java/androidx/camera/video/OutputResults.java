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

package androidx.camera.video;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * Class to provide the information of the output.
 */
@AutoValue
public abstract class OutputResults {

    @NonNull
    static OutputResults of(@NonNull Uri outputUri) {
        Preconditions.checkNotNull(outputUri, "OutputUri cannot be null.");
        return new AutoValue_OutputResults(outputUri);
    }

    /**
     * Gets the {@link Uri} of the output.
     *
     * <p>Returns the actual {@link Uri} of the output destination if the
     * {@link OutputOptions} is implemented by {@link MediaStoreOutputOptions}, otherwise
     * returns {@link Uri#EMPTY}.
     */
    @NonNull
    public abstract Uri getOutputUri();
}
