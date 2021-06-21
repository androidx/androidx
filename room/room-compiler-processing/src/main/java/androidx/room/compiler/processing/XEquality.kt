/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

/**
 * Helper interface to enforce implementing equality in wrappers so that we don't by mistake
 * create wrappers that do not properly handle equality.
 *
 * Enforcement is done in JavacType and JavacElement
 */
internal interface XEquality {
    /**
     * The list of items that should participate in equality checks.
     */
    val equalityItems: Array<out Any?>

    companion object {
        fun hashCode(elements: Array<out Any?>): Int {
            return elements.contentHashCode()
        }

        fun equals(first: Any?, second: Any?): Boolean {
            if (first !is XEquality || second !is XEquality) {
                return false
            }
            return equals(first.equalityItems, second.equalityItems)
        }

        fun equals(first: Array<out Any?>, second: Array<out Any?>): Boolean {
            // TODO there is probably a better way to do this
            if (first.size != second.size) {
                return false
            }
            repeat(first.size) {
                if (first[it] != second[it]) {
                    return false
                }
            }
            return true
        }
    }
}
