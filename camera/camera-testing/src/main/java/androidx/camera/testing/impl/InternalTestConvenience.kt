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

package androidx.camera.testing.impl

import android.app.Activity
import androidx.camera.core.Logger
import androidx.test.core.app.ActivityScenario

/**
 * Utility object to hold the convenience functions for internal testing.
 *
 * This should never be publicly available directly.
 */
public object InternalTestConvenience {
    public const val LOG_TAG: String = "InternalTestConvenience"

    /**
     * Executes the given [block] function on this [ActivityScenario] resource and finally closes it
     * without throwing in some cases for the convenience of camera tests.
     *
     * [ActivityScenario.close] may throw an exception in some cases due to bugs usually unrelated
     * to a camera test. This function suppresses the exceptions in such cases for the convenience
     * of tests where issues related to clearing up resources aren't related.
     *
     * @param block a function to process this resource.
     * @return the result of [block] function invoked on this resource.
     */
    public inline fun <A : Activity, R> ActivityScenario<A>.useInCameraTest(
        block: (ActivityScenario<A>) -> R,
    ): R {
        try {
            return block(this)
        } finally {
            try {
                close()
            } catch (e: Throwable) {
                if (AndroidUtil.isEmulator(28)) {
                    Logger.w(LOG_TAG, "Suppressed exception from ActivityScenario.close()", e)
                } else {
                    // rethrow in case it's not a known issue
                    throw e
                }
            }
        }
    }
}
