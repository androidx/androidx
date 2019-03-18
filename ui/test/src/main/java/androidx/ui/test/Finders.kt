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

/**
 * Finds a component identified by the given tag. There has to be only exactly one component
 * satisfying the condition.
 *
 * @throws AssertionError if exactly one component not found
 */
fun UiTestRunner.findByTag(testTag: String): SemanticsTreeQuery {
    return findByCondition { node ->
        node.data.testTag == testTag
    }
}

fun UiTestRunner.findByCondition(
    selector: (SemanticsTreeNode) -> Boolean
): SemanticsTreeQuery {
    return SemanticsTreeQuery(this, selector)
}