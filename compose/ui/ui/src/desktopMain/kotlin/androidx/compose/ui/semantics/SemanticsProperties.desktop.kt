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

package androidx.compose.ui.semantics

import androidx.compose.ui.ExperimentalComposeUiApi
import javax.accessibility.AccessibleRole


/**
 * Extra semantics properties specific to the desktop.
 */
internal object DesktopSemanticsProperties {

    /** @see SemanticsPropertyReceiver.awtRole */
    val AwtRole = AccessibilityKey<AccessibleRole>("AwtRole") { parentValue, _ -> parentValue }

}

/**
 * Specifies directly the [AccessibleRole] reported to AWT for the element.
 *
 * This should only be used for roles that are not supported by Compose's [Role].
 * Note that this overrides the role specified by [SemanticsPropertyReceiver.role], if any.
 *
 * @see SemanticsPropertyReceiver.role
 */
@ExperimentalComposeUiApi
var SemanticsPropertyReceiver.awtRole by DesktopSemanticsProperties.AwtRole