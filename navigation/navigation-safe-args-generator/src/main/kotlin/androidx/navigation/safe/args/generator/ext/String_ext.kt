/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.ext

import java.util.Locale

fun String.toCamelCase(): String {
    val split = this.split("_")
    if (split.size == 0) return ""
    if (split.size == 1) return split[0].capitalize(Locale.US)
    return split.joinToCamelCase()
}

fun String.toCamelCaseAsVar(): String {
    val split = this.split("_")
    if (split.size == 0) return ""
    if (split.size == 1) return split[0]
    return split.joinToCamelCaseAsVar()
}

// Gets class name parts (package name, simple name, inner names) out of a canonical name such
// as a.b.OuterClass$InnerClass, useful for then building a javapoet or kotlinpoet ClassName.
fun String.toClassNameParts(): Triple<String, String, Array<String>> {
    val packageName = substringBeforeLast('.', "")
    val (simpleName, innerNames) = substringAfterLast('.').let {
        val simpleName = it.substringBefore("$")
        val innerNames = it.substringAfter("$", "").let { innerName ->
            if (innerName.isNotEmpty()) {
                innerName.split("$")
            } else {
                emptyList()
            }
        }
        simpleName to innerNames
    }
    return Triple(packageName, simpleName, innerNames.toTypedArray())
}

fun String.capitalize(locale: Locale): String = if (isNotEmpty() && this[0].isLowerCase()) {
    substring(0, 1).uppercase(locale) + substring(1)
} else {
    this
}
