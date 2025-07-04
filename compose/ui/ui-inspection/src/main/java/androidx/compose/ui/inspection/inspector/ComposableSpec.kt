/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

/**
 * A list of composables where drawModifier information is undesirable.
 *
 * The drawModifier information is meant to help the client to perform hit testing in the app. Some
 * composable implementations contain the ability to draw above the actual content. Avoid marking
 * these composables with drawModifier. This is not trying to be an exhaustive list, but iit is
 * meant to cover the most common cases..
 */
private val unwantedDrawComposables =
    listOf(
        // Avoid the ReusableComposeNode in the implementation of SubcomposeLayout.
        createSpec("ReusableComposeNode", "SubcomposeLayout.kt", "androidx.compose.ui.layout"),

        // Avoid the scrim in a navigation drawer.
        createSpec("Scrim", "NavigationDrawer.kt", "androidx.compose.material3"),
    )

/**
 * Return true if this [node] is among the known composables that typically draw on top of
 * composables that are more likely to be selected by the user.
 */
internal fun isUnwantedDrawComposable(node: MutableInspectorNode): Boolean {
    return unwantedDrawComposables.any { it.equalTo(node) }
}

/**
 * A specification of a Composable node from the framework that draw on top of composables that are
 * more likely to be selected by the user.
 */
private data class ComposableSpec(val name: String, val fileName: String, val packageHash: Int) {
    fun equalTo(node: MutableInspectorNode): Boolean {
        return packageHash == node.packageHash && name == node.name && fileName == node.fileName
    }
}

private fun createSpec(name: String, fileName: String, packageName: String) =
    ComposableSpec(name, fileName, packageNameHash(packageName))
