/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.semantics

object SemanticsPropertiesAndroid {
    /** @see SemanticsPropertyReceiver.testTagsAsResourceId */
    val TestTagsAsResourceId =
        SemanticsPropertyKey<Boolean>(
            name = "TestTagsAsResourceId",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.accessibilityClassName */
    val AccessibilityClassName =
        AccessibilityKey<String>("AccessibilityClassName") { parentValue, _ -> parentValue }
}

/**
 * Creates a [SemanticsPropertyKey] that allows declaring Android-specific semantics properties
 * (key/value pairs set inside semantics blocks in a type-safe way) that are made available as
 * accessibility extras provided to accessibility services via
 * [android.view.accessibility.AccessibilityNodeInfo.getExtras].
 *
 * Each key has one particular statically defined value type T, where T is required to be either
 * [Serializable] (which including boxed primitive types) or [android.os.Parcelable].
 *
 * If the same property is set multiple times, the last value set in the outer modifier wins.
 *
 * @param name The name of the property, which should be the same as the constant from which it is
 *   accessed.
 * @param accessibilityExtraKey The key used to store the value in the extras [android.os.Bundle].
 * @param mergePolicy The merge policy to use when merging descendant semantics.
 */
fun <T> SemanticsPropertyKey(
    name: String,
    accessibilityExtraKey: String,
    mergePolicy: (T?, T) -> T? = { parentValue, _ -> parentValue },
) =
    SemanticsPropertyKey(
        name = name,
        isImportantForAccessibility = false,
        accessibilityExtraKey = accessibilityExtraKey,
        mergePolicy = mergePolicy,
    )

/**
 * Configuration toggle to map testTags to resource-id.
 *
 * This provides a way of filling in AccessibilityNodeInfo.viewIdResourceName, which in the View
 * System is populated based on the resource string in the XML.
 *
 * testTags are also provided in AccessibilityNodeInfo.extras under key
 * "androidx.compose.ui.semantics.testTag". However, when using UIAutomator or on Android 7 and
 * below, extras are not available, so a more backwards-compatible way of making testTags available
 * to accessibility-tree-based integration tests is sometimes needed. resource-id was the most
 * natural property to repurpose for this.
 *
 * This property applies to a semantics subtree. For example, if it's set to true on the root
 * semantics node of the app (and no child nodes set it back to false), then every testTag will be
 * mapped.
 */
var SemanticsPropertyReceiver.testTagsAsResourceId by
    SemanticsPropertiesAndroid.TestTagsAsResourceId

/*
 * Semantics that maps directly to the [android.view.accessibility.AccessibilityNodeInfo.className].
 * Accessibility services will use this to describe the element or do customizations.
 *
 * Developers will rarely set it directly. In most cases the [android.view.accessibility.AccessibilityNodeInfo.className] is derived from other semantics automatically. This property can be useful for niche use cases where deriving the classname is
 * not possible or a special customisation is required.
 *
 * Note that when you assign this semantics, it will override a AccessibilityNodeInfo's classname
 * that would otherwise be determined automatically through the presence of other semantics properties.
 *
 * The value of this property is not an arbitrary string. It should correspond to the type or a
 * constant defined on Android platform. For example, to assign a button classname you would use
 * "android.widget.Button".
 */
var SemanticsPropertyReceiver.accessibilityClassName by
    SemanticsPropertiesAndroid.AccessibilityClassName
