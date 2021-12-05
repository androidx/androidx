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

package androidx.room.compiler.processing.util

import javax.lang.model.SourceVersion

/**
 * Kotlin might generate names that are not valid in Java source code (but valid in binary).
 * This helper method is used to sanitize them for method parameters.
 */
internal fun String?.sanitizeAsJavaParameterName(
    argIndex: Int
): String = if (this != null && SourceVersion.isName(this)) {
    this
} else {
    "p$argIndex"
}