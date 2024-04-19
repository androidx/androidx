/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.lint

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.ApiConstraint
import com.android.tools.lint.detector.api.VersionChecks.Companion.getTargetApiAnnotation
import org.jetbrains.uast.UElement

/**
 * Returns true if the element or one of its containing elements has an annotation that satisfies
 * the [atLeast] constraint.
 */
internal fun hasAtLeastTargetApiAnnotation(
    evaluator: JavaEvaluator,
    element: UElement?,
    atLeast: ApiConstraint
): Boolean {
    var curr = element ?: return false
    while (true) {
        val (outer, target) = getTargetApiAnnotation(evaluator, curr)
        if (target != null && target.isAtLeast(atLeast)) {
            return true
        }
        curr = outer?.uastParent?.uastParent ?: return false
    }
}
