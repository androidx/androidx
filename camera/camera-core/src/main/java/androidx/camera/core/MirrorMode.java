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

package androidx.camera.core;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The mirror mode.
 *
 * <p>Constants describing image mirroring transforms.
 */
public class MirrorMode {
    /** No mirror effect will be applied. */
    public static final int MIRROR_MODE_OFF = 0;

    /** The mirror effect is always applied. */
    public static final int MIRROR_MODE_ON = 1;

    /**
     * The mirror effect is applied only when the lens facing of the associated camera is
     * {@link CameraSelector#LENS_FACING_FRONT}.
     */
    public static final int MIRROR_MODE_ON_FRONT_ONLY = 2;

    /** The mirror mode is not specified by the user **/
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int MIRROR_MODE_UNSPECIFIED = -1;

    private MirrorMode() {
    }

    /**
     */
    @IntDef({MIRROR_MODE_OFF, MIRROR_MODE_ON, MIRROR_MODE_ON_FRONT_ONLY, MIRROR_MODE_UNSPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface Mirror {
    }
}
