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
@file:JvmName("ActionHelper")

package androidx.car.app.sample.showcase.common.utils

import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.OnClickListener

/**
 * Constructs an [Action] using a clean, declarative Kotlin syntax.
 * * Automatically ignores `null` parameters, bypassing the need to use [Action.Builder] directly.
 */
@JvmOverloads
fun createAction(
    title: String? = null,
    icon: CarIcon? = null,
    flags: Int? = null,
    clickListener: OnClickListener? = null,
): Action {
    val builder = Action.Builder()
    title?.let { builder.setTitle(it) }
    icon?.let { builder.setIcon(it) }
    flags?.let { builder.setFlags(it) }
    clickListener?.let { builder.setOnClickListener(it) }
    return builder.build()
}
