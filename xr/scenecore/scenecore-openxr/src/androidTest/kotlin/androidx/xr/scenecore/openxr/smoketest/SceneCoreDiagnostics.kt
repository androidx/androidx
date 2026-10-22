/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.openxr.smoketest

import androidx.xr.arcore.Anchor
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.scenecore.openxr.INVALID_HANDLE
import androidx.xr.scenecore.openxr.SceneCoreOpenXrNative

/**
 * Diagnostics for the panel sample that need restricted or internal APIs. They are kept here so
 * that the rest of the sample only uses public Jetpack XR APIs.
 */
internal object SceneCoreDiagnostics {
    /** The runtimes that [session] picked, such as `OpenXrRuntime, OpenXrSceneRuntime`. */
    fun describeRuntimes(session: Session): String =
        session.runtimes.joinToString { it.javaClass.simpleName }.ifEmpty { "none" }

    /**
     * Whether [session] uses the ARCore fakes, whose anchors have no `XrSpace`. Compares class
     * names, since the fake runtime's class is restricted to arcore-testing.
     */
    fun usesArCoreFakes(session: Session): Boolean =
        session.runtimes.any { it.javaClass.simpleName == "FakePerceptionRuntime" }

    /** The native `XrSpace` of [anchor], or null if it has none, as with the ARCore fakes. */
    fun nativeSpaceHandle(anchor: Anchor): Long? = anchor.nativeSpaceHandle

    /**
     * Whether the scenecore-openxr native library has the entry points that attach entities to an
     * `XrSpace`, which an AnchorSpace needs. Without them, AnchorSpaces on the OpenXR SceneCore
     * runtime go to ERROR.
     */
    fun describeAttachmentNatives(): String =
        try {
            SceneCoreOpenXrNative().use { native ->
                // An uninitialized native wrapper rejects the removal, and logs an error, but only
                // once the call has reached the native library.
                native.removeTransactionSpaceAttachment(INVALID_HANDLE, INVALID_HANDLE)
            }
            "available"
        } catch (e: UnsatisfiedLinkError) {
            "missing: the scenecore-openxr prebuilt predates XrSpace attachments"
        } catch (e: LibraryNotLinkedException) {
            "unknown: ${e.libraryName} can't be loaded"
        }
}
