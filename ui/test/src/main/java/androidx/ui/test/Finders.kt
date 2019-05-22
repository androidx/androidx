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

import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * Extension methods that provide the entry point for the testing APIs.
 */

/**
 * Finds a component identified by the given tag.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun findByTag(testTag: String): SemanticsNodeInteraction {
    return find {
        this.testTag == testTag
    }
}

/**
 * Finds a component by the given text.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun findByText(text: String, ignoreCase: Boolean = false): SemanticsNodeInteraction {
    return find {
        label.equals(text, ignoreCase)
    }
}

/**
 * Finds a component that matches the given condition.
 * This tries to match exactly one element and throws [AssertionError] if more than one is matched.
 *
 * For usage patterns see [SemanticsNodeInteraction]
 */
fun find(
    selector: SemanticsConfiguration.() -> Boolean
): SemanticsNodeInteraction {
    return semanticsTreeInteractionFactory(selector)
        .findOne()
}

fun findAll(
    selector: SemanticsConfiguration.() -> Boolean
): List<SemanticsNodeInteraction> {
    return semanticsTreeInteractionFactory(selector)
        .findAllMatching()
}
