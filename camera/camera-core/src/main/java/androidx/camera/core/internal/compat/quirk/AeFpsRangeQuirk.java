/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.internal.compat.quirk;

import android.util.Range;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.StreamSpec;

/**
 * A Quirk interface denotes devices have specific issue and can be avoided by specific AE FPS
 * range setting.
 */
public interface AeFpsRangeQuirk extends Quirk {

    /**
     * Returns the target AE FPS range to avoid the issue.
     */
    @NonNull
    default Range<Integer> getTargetAeFpsRange() {
        return StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
    }
}
