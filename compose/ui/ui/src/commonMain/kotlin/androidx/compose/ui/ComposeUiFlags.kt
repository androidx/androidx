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
@file:JvmName("ComposeRuntimeFlags")

package androidx.compose.ui

import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * This is a collection of flags which are used to guard against regressions in some of the
 * "riskier" refactors or new feature support that is added to this module. These flags are always
 * "on" in the published artifact of this module, however these flags allow end consumers of this
 * module to toggle them "off" in case this new path is causing a regression.
 *
 * These flags are considered temporary, and there should be no expectation for these flags be
 * around for an extended period of time. If you have a regression that one of these flags fixes, it
 * is strongly encouraged for you to file a bug ASAP.
 *
 * **Usage:**
 *
 * In order to turn a feature off in a debug environment, it is recommended to set this to false in
 * as close to the initial loading of the application as possible. Changing this value after compose
 * library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              ComposeUiFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.runtime.ComposeUiFlags {
 *          public static int isRectTrackingEnabled return false
 *      }
 */
@ExperimentalComposeUiApi
object ComposeUiFlags {
    /**
     * With this flag on, during layout we will do some additional work to store the minimum
     * bounding rectangles for all Layout Nodes. This introduces some additional maintenance burden,
     * but will be used in the future to enable certain features that are not possible to do
     * efficiently at this point, as well as speed up some other areas of the system such as
     * semantics, focus, pointer input, etc. If significant performance overhead is noticed during
     * layout phases, it is possible that the addition of this tracking is the culprit.
     */
    @Suppress("MutableBareField") @JvmField var isRectTrackingEnabled: Boolean = true
}
