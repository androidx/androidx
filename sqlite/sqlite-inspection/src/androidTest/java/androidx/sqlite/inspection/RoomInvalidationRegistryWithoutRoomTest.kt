/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection

import androidx.inspection.InspectorEnvironment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test just checks that we have reasonable defaults (e.g. no crash) if Room is not available
 * in the classpath.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RoomInvalidationRegistryWithoutRoomTest {
    @Test
    fun noOpTest() {
        // this does not really assert anything, we just want to make sure it does not crash and
        // never makes a call to the environment if Room is not available.
        val env = object : InspectorEnvironment {
            override fun registerEntryHook(
                originClass: Class<*>,
                originMethod: String,
                entryHook: InspectorEnvironment.EntryHook
            ) {
                throw AssertionError("should never call environment")
            }

            override fun <T : Any?> findInstances(clazz: Class<T>): MutableList<T> {
                throw AssertionError("should never call environment")
            }

            override fun <T : Any?> registerExitHook(
                originClass: Class<*>,
                originMethod: String,
                exitHook: InspectorEnvironment.ExitHook<T>
            ) {
                throw AssertionError("should never call environment")
            }
        }
        val tracker = RoomInvalidationRegistry(env)
        tracker.triggerInvalidations()
        tracker.invalidateCache()
        tracker.triggerInvalidations()
    }
}
