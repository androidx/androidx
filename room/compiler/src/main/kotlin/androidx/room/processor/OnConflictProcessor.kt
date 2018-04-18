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

package androidx.room.processor

import androidx.room.OnConflictStrategy
import com.google.auto.common.AnnotationMirrors
import javax.lang.model.element.AnnotationMirror

/**
 * Processes on conflict fields in annotations
 */
object OnConflictProcessor {
    val INVALID_ON_CONFLICT = -1

    @OnConflictStrategy
    fun extractFrom(annotation: AnnotationMirror?, fieldName: String = "onConflict"): Int {
        return if (annotation == null) {
            INVALID_ON_CONFLICT
        } else {
            try {
                val onConflictValue = AnnotationMirrors
                        .getAnnotationValue(annotation, fieldName)
                        .value
                onConflictValue.toString().toInt()
            } catch (ex: NumberFormatException) {
                INVALID_ON_CONFLICT
            }
        }
    }

    fun onConflictText(@OnConflictStrategy onConflict: Int): String {
        return when (onConflict) {
            OnConflictStrategy.REPLACE -> "REPLACE"
            OnConflictStrategy.ABORT -> "ABORT"
            OnConflictStrategy.FAIL -> "FAIL"
            OnConflictStrategy.IGNORE -> "IGNORE"
            OnConflictStrategy.ROLLBACK -> "ROLLBACK"
            else -> "BAD_CONFLICT_CONSTRAINT"
        }
    }
}
