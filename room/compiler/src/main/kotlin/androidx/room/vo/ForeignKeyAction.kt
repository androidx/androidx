/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.ForeignKey

/**
 * Compiler representation of ForeignKey#Action.
 */
enum class ForeignKeyAction(val annotationValue: Int, val sqlName: String) {
    NO_ACTION(ForeignKey.NO_ACTION, "NO ACTION"),
    RESTRICT(ForeignKey.RESTRICT, "RESTRICT"),
    SET_NULL(ForeignKey.SET_NULL, "SET NULL"),
    SET_DEFAULT(ForeignKey.SET_DEFAULT, "SET DEFAULT"),
    CASCADE(ForeignKey.CASCADE, "CASCADE");
    companion object {
        private val mapping by lazy {
            ForeignKeyAction.values().associateBy { it.annotationValue }
        }
        fun fromAnnotationValue(value: Int?) = mapping[value]
    }
}
