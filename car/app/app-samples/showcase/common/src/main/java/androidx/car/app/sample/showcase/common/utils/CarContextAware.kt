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

package androidx.car.app.sample.showcase.common.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.CarToast.Duration
import androidx.car.app.CarToast.LENGTH_SHORT
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.min

/** A delegate interface designed to simplify common operations requiring a [CarContext]. */
interface CarContextAware {
    /**
     * Retrieves the [CarContext] instance used by all utility functions in this interface.
     *
     * *Note:* When your class extends [androidx.car.app.Screen], the base class already provides a
     * final implementation of `getCarContext()`, automatically satisfying this requirement.
     */
    fun getCarContext(): CarContext

    /** Retrieves a localized string from the application's resources. */
    fun getString(@StringRes resId: Int): String {
        return getCarContext().getString(resId)
    }

    /** Converts a standard Android drawable resource into a [CarIcon]. */
    fun getCarIcon(@DrawableRes resId: Int): CarIcon {
        return CarIcon.Builder(IconCompat.createWithResource(getCarContext(), resId)).build()
    }

    /** Creates a [CarToast] with a default short duration, ready to be shown. */
    fun makeToast(text: String, @Duration duration: Int = LENGTH_SHORT): CarToast {
        return CarToast.makeText(getCarContext(), text, duration)
    }

    /**
     * Resolves the safe maximum list size by comparing [maxExpectedLength] against the host's
     * `CONTENT_LIMIT_TYPE_LIST`, guaranteeing a non-negative result.
     */
    fun determineListLimit(maxExpectedLength: Int): Int {
        val systemLimit =
            getCarContext()
                .getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)

        return min(maxExpectedLength, systemLimit).coerceAtLeast(0)
    }
}

/** Executes the provided [action] only if the host supports the specified minimum API level. */
inline fun CarContext.withApiGuard(atLeastApiLevel: Int, action: () -> Unit) {
    val currentLevel = getCarAppApiLevel()
    if (currentLevel >= atLeastApiLevel) action()
}
