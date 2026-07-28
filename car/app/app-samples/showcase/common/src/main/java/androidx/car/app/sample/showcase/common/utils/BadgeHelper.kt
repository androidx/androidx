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

import androidx.car.app.model.Badge
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon

/**
 * Constructs a [Badge] using a clean, declarative Kotlin syntax. Automatically handles API
 * constraints and ignores irrelevant parameters.
 */
fun createBadge(
    hasDot: Boolean = false,
    dotColor: CarColor? = null,
    icon: CarIcon? = null,
    iconBackgroundColor: CarColor? = null,
): Badge {
    val builder = Badge.Builder()

    val shouldEnableDot = hasDot || icon == null

    if (shouldEnableDot) {
        builder.setHasDot(true)
        builder.setDotColor(dotColor ?: CarColor.RED)
    }

    icon?.let {
        builder.setIcon(it)
        iconBackgroundColor?.let { color -> builder.setIconBackgroundColor(color) }
    }

    return builder.build()
}
