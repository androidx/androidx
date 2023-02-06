/*
 * Copyright 2022 The Android Open Source Project
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** A singleton controller to manage the global config. */
class DemoActivityEmbeddingController private constructor() {

    private val lock = Object()

    @GuardedBy("lock")
    private var _animationBackgroundColor = SplitAttributes.BackgroundColor.DEFAULT

    /** Animation background color to use when the animation requires a background. */
    var animationBackgroundColor: SplitAttributes.BackgroundColor
        get() = synchronized(lock) {
            _animationBackgroundColor
        }
        set(value) = synchronized(lock) {
            _animationBackgroundColor = value
        }

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