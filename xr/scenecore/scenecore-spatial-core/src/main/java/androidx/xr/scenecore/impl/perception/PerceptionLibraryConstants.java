/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.impl.perception;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Class to manage constants for the PerceptionLibrary. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class PerceptionLibraryConstants {

    /** View OpenXR Reference Space Type. */
    public static final int OPEN_XR_SPACE_TYPE_VIEW = 1;

    /** Local OpenXR Reference Space Type. */
    public static final int OPEN_XR_SPACE_TYPE_LOCAL = 2;

    /** Stage OpenXR Reference Space Type. */
    public static final int OPEN_XR_SPACE_TYPE_STAGE = 3;

    /** Local Floor OpenXR Reference Space Type. */
    public static final int OPEN_XR_SPACE_TYPE_LOCAL_FLOOR = 1000426000;

    /** Unbounded OpenXR Reference Space Type. */
    public static final int OPEN_XR_SPACE_TYPE_UNBOUNDED = 1000467000;

    /** IntDef for OpenXR Reference Space Types. */
    @IntDef({
        OPEN_XR_SPACE_TYPE_VIEW,
        OPEN_XR_SPACE_TYPE_LOCAL,
        OPEN_XR_SPACE_TYPE_STAGE,
        OPEN_XR_SPACE_TYPE_LOCAL_FLOOR,
        OPEN_XR_SPACE_TYPE_UNBOUNDED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface OpenXrSpaceType {}

    private PerceptionLibraryConstants() {}
}
