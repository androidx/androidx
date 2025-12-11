/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.uikit

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.utils.cmp_itemWithAny
import androidx.compose.ui.uikit.utils.cmp_itemWithString
import androidx.compose.ui.uikit.utils.cmp_loadAny
import androidx.compose.ui.uikit.utils.cmp_loadString
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import platform.Foundation.NSError
import platform.UIKit.UIDragItem
import platform.darwin.NSObject

/**
 * Encodes [String] to [UIDragItem] for use in drag-and-drop operations.
 *
 * @param string The string representation of a drag item.
 * @return A new [UIDragItem] instance created from the provided string.
 */
@ExperimentalComposeUiApi
fun UIDragItem.Companion.fromString(string: String): UIDragItem =
    UIDragItem.cmp_itemWithString(string)

/**
 * Encodes [NSObject] into [UIDragItem] for use in drag-and-drop operations.
 *
 * @param objectClass The [ObjCClass] representing the class of the object to be used in the drag item.
 * @param nsObject The [NSObject] instance to be associated with the created [UIDragItem]. Can be null.
 * @return A [UIDragItem] constructed using the provided [objectClass] and [nsObject]. Throws an exception if the resulting drag item is null.
 */
@OptIn(BetaInteropApi::class)
@ExperimentalComposeUiApi
fun <T: NSObject, NSItemProviderWriting> UIDragItem.Companion.fromNSObject(
    objectClass: ObjCClass,
    nsObject: T
): UIDragItem =
    requireNotNull(UIDragItem.cmp_itemWithAny(objectClass, nsObject))

/**
 * Loads a string provided by the drag-and-drop session.
 *
 * @param block Callback function that receives the loaded string and an [NSError] if applicable.
 */
@ExperimentalComposeUiApi
fun UIDragItem.loadString(block: (String?, NSError?) -> Unit) {
    itemProvider.cmp_loadString(block)
}

/**
 * Loads a specified [NSObject] class from a [UIDragItem]'s item provider, invoking the provided block
 * upon completion.
 *
 * @param objectClass The [ObjCClass] that represents the class of the object to be loaded.
 * @param block A callback function that will receive the loaded object of type [T] and an associated [NSError] if the operation fails. The loaded object may be null.
 */
@OptIn(BetaInteropApi::class)
@ExperimentalComposeUiApi
fun <T: NSObject, NSItemProviderReading> UIDragItem.loadNSObject(
    objectClass: ObjCClass,
    block: (T?, NSError?) -> Unit
) {
    itemProvider.cmp_loadAny(objectClass) { any: Any?, nsError: NSError? ->
        @Suppress("UNCHECKED_CAST")
        block(any as T?, nsError)
    }
}
