/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.test.android.AndroidSemanticsTreeInteraction

/**
 * Extension methods that provide the entry point for the testing APIs.
 */

/**
 * Finds a component identified by the given tag.
 *
 * For usage patterns see [SemanticsTreeInteraction]
 */
fun findByTag(testTag: String): SemanticsTreeInteraction {
    return findByCondition { node ->
        node.data.testTag == testTag
    }
}

/**
 * Finds a component by the given text.
 *
 * For usage patterns see [SemanticsTreeInteraction]
 */
fun findByText(text: String, ignoreCase: Boolean = false): SemanticsTreeInteraction {
    return findByCondition { node ->
        node.data.label.equals(text, ignoreCase)
    }
}

/**
 * Finds a component that matches the given condition
 *
 * For usage patterns see [SemanticsTreeInteraction]
 */
fun findByCondition(
    selector: (SemanticsTreeNode) -> Boolean
): SemanticsTreeInteraction {
    return semanticsTreeInteractionFactory().addSelector(selector)
}
