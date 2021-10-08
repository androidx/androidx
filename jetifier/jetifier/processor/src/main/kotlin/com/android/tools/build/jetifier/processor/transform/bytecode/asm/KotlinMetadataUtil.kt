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

package com.android.tools.build.jetifier.processor.transform.bytecode.asm

/**
 * If the given string [signature] is in format `(ILFoo;LBar;)LResult;` it maps
 * referenced types via [mapDeclaration]. If the given string doesn't follow this pattern,
 * `null` is returned.
 *
 * Such strings occur in kotlin's [Metadata] annotation for property getters and setters and
 * data classes.
 */
internal fun rewriteIfMethodSignature(
    signature: String,
    mapDeclaration: (String) -> String
): String? {
    val mapType = { declaration: String ->
        val type = if (isArrayDeclaration(declaration)) declaration.trim('[') else declaration
        val mapped = if (isTypeDeclaration(type)) {
            "L${mapDeclaration(type.substring(1, type.length - 1))};"
        } else {
            type
        }
        "${"[".repeat(declaration.length - type.length)}$mapped"
    }
    // trying to match strings in the format `(ILFoo;LBar;)LResult;`
    if (!signature.startsWith('(')) return null
    val index = signature.indexOf(')')
    if (index == -1) return null
    val params = splitParameters(signature.substring(1, index)).joinToString("") {
        mapType(it)
    }
    val returnType = signature.substring(index + 1)
    return "($params)${mapType(returnType)}"
}

private fun splitParameters(parameters: String): List<String> {
    val result = mutableListOf<String>()
    val currentParam = StringBuilder(parameters.length)
    var inClassName = false
    for (c in parameters) {
        currentParam.append(c)
        inClassName = if (inClassName) c != ';' else c == 'L'
        // add a parameter if we're no longer in class and not in array start
        if (!inClassName && c != '[') {
            result.add(currentParam.toString())
            currentParam.clear()
        }
    }
    return result
}

private fun isTypeDeclaration(string: String) = string.startsWith("L") && string.endsWith(";")
private fun isArrayDeclaration(string: String) = string.startsWith("[")