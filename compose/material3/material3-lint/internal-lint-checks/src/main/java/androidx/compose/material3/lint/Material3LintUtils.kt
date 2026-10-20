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

package androidx.compose.material3.lint

import com.android.tools.lint.detector.api.JavaContext

private const val Material3Package = "androidx.compose.material3"
private const val WearMaterial3Package = "androidx.wear.compose.material3"

/**
 * Returns `true` if [this] context is a non-test, non-sample source file in
 * `androidx.compose.material3` or `androidx.wear.compose.material3` (or one of their subpackages).
 */
internal fun JavaContext.isMaterial3Source(): Boolean {
    if (isTestSource) return false
    val packageName = uastFile?.packageName ?: return false
    if (packageName.contains("samples")) return false
    return isInPackage(packageName, Material3Package) ||
        isInPackage(packageName, WearMaterial3Package)
}

/**
 * Returns `true` if [this] context is in `androidx.wear.compose.material3` (or one of its
 * subpackages).
 */
internal fun JavaContext.isWearMaterial3Source(): Boolean {
    val packageName = uastFile?.packageName ?: return false
    return isInPackage(packageName, WearMaterial3Package)
}

private fun isInPackage(packageName: String, prefix: String): Boolean {
    return packageName == prefix || packageName.startsWith("$prefix.")
}
