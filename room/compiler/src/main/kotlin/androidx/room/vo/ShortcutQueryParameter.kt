/*
 * Copyright (C) 2016 The Android Open Source Project
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

import javax.lang.model.type.TypeMirror

/**
 * Parameters used in DAO methods that are annotated with Insert, Delete, Update.
 */
data class ShortcutQueryParameter(val name: String, val type: TypeMirror,
                                  val entityType: TypeMirror?, val isMultiple: Boolean) {
    /**
     * Method name in entity insertion or update adapter.
     */
    fun handleMethodName(): String {
        return if (isMultiple) {
            "handleMultiple"
        } else {
            "handle"
        }
    }
}
