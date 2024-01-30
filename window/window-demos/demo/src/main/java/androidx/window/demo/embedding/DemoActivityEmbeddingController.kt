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

package androidx.window.demo.embedding

import androidx.annotation.GuardedBy
import androidx.window.embedding.SplitAttributes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** A singleton controller to manage the global config. */
class DemoActivityEmbeddingController private constructor() {
    private val lock = ReentrantLock()

    /** Indicates that whether to expand the secondary container or not */
    internal var shouldExpandSecondaryContainer = AtomicBoolean(false)

    internal var splitAttributesCustomizationEnabled = AtomicBoolean(false)

    internal var customizedLayoutDirection: SplitAttributes.LayoutDirection
        get() {
            lock.withLock {
                return layoutDirectionLocked
            }
        }
        set(value) {
            lock.withLock {
                layoutDirectionLocked = value
            }
        }

    @GuardedBy("lock")
    private var layoutDirectionLocked = SplitAttributes.LayoutDirection.LOCALE

    internal var customizedSplitType: SplitAttributes.SplitType
        get() {
            lock.withLock {
                return splitTypeLocked
            }
        }
        set(value) {
            lock.withLock {
                splitTypeLocked = value
            }
        }

    @GuardedBy("lock")
    private var splitTypeLocked = SplitAttributes.SplitType.SPLIT_TYPE_EQUAL

    companion object {
        @Volatile
        private var globalInstance: DemoActivityEmbeddingController? = null
        private val globalLock = ReentrantLock()

        /**
         * Obtains the singleton instance of [DemoActivityEmbeddingController].
         */
        @JvmStatic
        fun getInstance(): DemoActivityEmbeddingController {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        globalInstance = DemoActivityEmbeddingController()
                    }
                }
            }
            return globalInstance!!
        }
    }
}
