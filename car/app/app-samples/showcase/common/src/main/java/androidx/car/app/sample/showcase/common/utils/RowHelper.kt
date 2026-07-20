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

import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Toggle
import kotlin.collections.forEach

/**
 * Constructs a [Row] using a clean, declarative Kotlin syntax. Automatically ignores `null`
 * parameters, bypassing the need to use [Row.Builder] directly.
 */
fun createRow(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    image: CarIcon? = null,
    firstLine: CharSequence? = null,
    firstLineCarText: CarText? = null,
    secondLine: CharSequence? = null,
    secondLineCarText: CarText? = null,
    actions: List<Action>? = null,
    toggle: Toggle? = null,
    isBrowsable: Boolean? = null,
    clickListener: OnClickListener? = null,
): Row {
    val builder = Row.Builder()

    titleCarText?.let { builder.setTitle(it) } ?: builder.setTitle(title ?: "")
    image?.let { builder.setImage(it) }
    clickListener?.let { builder.setOnClickListener(it) }

    firstLineCarText?.let { builder.addText(it) } ?: firstLine?.let { builder.addText(it) }
    secondLineCarText?.let { builder.addText(it) } ?: secondLine?.let { builder.addText(it) }

    actions?.forEach { builder.addAction(it) }
    toggle?.let { builder.setToggle(it) }

    if (isBrowsable == true) builder.setBrowsable(true)

    return builder.build()
}
