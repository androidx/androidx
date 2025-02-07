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

package androidx.xr.runtime.openxr

import android.os.IBinder
import androidx.xr.runtime.internal.Anchor

/** Wraps the minimum necessary information to export an anchor to another Jetpack XR module. */
public interface ExportableAnchor : Anchor {
    /* nativePointer to the [XrSpace] instance that backs this anchor */
    public val nativePointer: Long

    /* anchorToken is a Binder reference of the anchor, it can be used to import the anchor by an
     * OpenXR session. */
    public val anchorToken: IBinder
}
