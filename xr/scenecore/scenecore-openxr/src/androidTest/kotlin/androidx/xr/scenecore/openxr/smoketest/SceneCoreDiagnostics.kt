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
}
