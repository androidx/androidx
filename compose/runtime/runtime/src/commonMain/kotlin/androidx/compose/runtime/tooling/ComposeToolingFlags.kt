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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.internal.trace
import kotlin.jvm.JvmField

/**
 * This is a collection of flags which are used to enable certain tooling features across Compose
 * libraries. The main difference from [androidx.compose.runtime.ComposeRuntimeFlags] is that these
 * flags are considered more permanent and are usually disabled unless the tooling feature is
 * required for investigation or other purposes.
 *
 * **Usage:**
 *
 * In order to turn a feature on in a debug environment, it is recommended to set the associated
 * flag to true in as close to the initial loading of the application as possible. Changing this
 * value after compose library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              ComposeToolingFlags.SomeFeatureEnabled = true
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.tooling.ComposeToolingFlags {
 *          public static int SomeFeatureEnabled return true
 *      }
 */
@Suppress("FeatureFlagSetup") // These are not normal, temporary feature flags, so exempt them
@ComposeToolingApi
public object ComposeToolingFlags {

    /**
     * Enables verbose tracing blocks in coroutines launched from @Composable context, measure /
     * layout and other Compose phases. These tracing blocks are intended to accurately measure each
     * phase in macrobenchmarks through Perfetto trace metrics.
     *
     * The verbose trace blocks might have a negative impact on performance and thus should be
     * disabled by default.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    public var isVerboseTracingEnabled: Boolean = false
}

internal inline fun <T> verboseTrace(name: String, block: () -> T): T =
    @OptIn(ComposeToolingApi::class)
    if (ComposeToolingFlags.isVerboseTracingEnabled) {
        trace(name, block)
    } else {
        block()
    }
