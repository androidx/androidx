/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.layout

import android.app.Activity
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.window.R
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide all the relevant info about a [android.view.Window].
 */
public interface WindowInfoRepository {

    /**
     * A [Flow] of [WindowLayoutInfo] that contains all the available features.
     */
    public val windowLayoutInfo: Flow<WindowLayoutInfo>

    public companion object {

        private var decorator: WindowInfoRepositoryDecorator = EmptyDecorator

        /**
         * Provide an instance of [WindowInfoRepository] that is associated to the given [Activity]
         */
        @JvmName("getOrCreate")
        @JvmStatic
        public fun Activity.windowInfoRepository(): WindowInfoRepository {
            val taggedRepo = getTag(R.id.androidx_window_activity_scope) as? WindowInfoRepository
            val repo = taggedRepo ?: getOrCreateTag(R.id.androidx_window_activity_scope) {
                WindowInfoRepositoryImpl(
                    this,
                    WindowMetricsCalculatorCompat,
                    ExtensionWindowBackend.getInstance(this)
                )
            }
            return decorator.decorate(repo)
        }

        private inline fun <reified T> Activity.getTag(id: Int): T? {
            return window.decorView.getTag(id) as? T
        }

        /**
         * Checks to see if an object of type [T] is associated with the tag [id]. If it is
         * available then it is returned. Otherwise set an object crated using the [producer] and
         * return that value.
         * @return object associated with the tag.
         */
        private inline fun <reified T> Activity.getOrCreateTag(id: Int, producer: () -> T): T {
            return (window.decorView.getTag(id) as? T) ?: run {
                assert(Looper.getMainLooper() == Looper.myLooper())
                val value = producer()
                window.decorView.setTag(id, value)
                value
            }
        }

        @JvmStatic
        @RestrictTo(LIBRARY_GROUP)
        public fun overrideDecorator(overridingDecorator: WindowInfoRepositoryDecorator) {
            decorator = overridingDecorator
        }

        @JvmStatic
        @RestrictTo(LIBRARY_GROUP)
        public fun reset() {
            decorator = EmptyDecorator
        }
    }
}

@RestrictTo(LIBRARY_GROUP)
public interface WindowInfoRepositoryDecorator {
    /**
     * Returns an instance of [WindowInfoRepository] associated to the [Activity]
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun decorate(repository: WindowInfoRepository): WindowInfoRepository
}

private object EmptyDecorator : WindowInfoRepositoryDecorator {
    override fun decorate(repository: WindowInfoRepository): WindowInfoRepository {
        return repository
    }
}