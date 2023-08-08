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

package androidx.fragment.lint

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * Checks if the [PsiType] is a subclass of class with canonical name [superName].
 *
 * @param context The context of the lint request.
 * @param superName The canonical name to check that the [PsiType] is a subclass of.
 * @param strict Whether [superName] is inclusive.
 */
internal fun PsiType?.extends(
    context: JavaContext,
    superName: String,
    strict: Boolean = false
): Boolean = context.evaluator.extendsClass(PsiTypesUtil.getPsiClass(this), superName, strict)

/**
 * Walks up the uastParent hierarchy from this element.
 */
internal fun UElement.walkUp(): Sequence<UElement> = generateSequence(uastParent) { it.uastParent }

/**
 * This is useful if you're in a nested call expression and want to find the nearest parent while ignoring this call.
 *
 * For example, if you have the following two cases of a `foo()` expression:
 * - `checkNotNull(fragment.foo())`
 * - `checkNotNull(foo())` // if foo() is a local function
 *
 * Calling this from `foo()` in both cases will drop you at the outer `checkNotNull()` expression.
 */
val UElement.nearestNonQualifiedReferenceParent: UElement?
    get() = walkUp().first {
        it !is UQualifiedReferenceExpression
    }

/**
 * @see [fullyQualifiedNearestParentOrNull]
 */
internal fun UElement.fullyQualifiedNearestParent(includeSelf: Boolean = true): UElement {
    return fullyQualifiedNearestParentOrNull(includeSelf)!!
}

/**
 * Given an element, returns the nearest fully qualified parent.
 *
 * Examples where [this] is a `UCallExpression` representing `bar()`:
 * - `Foo.bar()` -> `Foo.bar()`
 * - `bar()` -> `bar()`
 *
 * @param includeSelf Whether or not to include [this] element in the checks.
 */
internal fun UElement.fullyQualifiedNearestParentOrNull(includeSelf: Boolean = true): UElement? {
    val node = if (includeSelf) this else uastParent ?: return null
    return if (node is UQualifiedReferenceExpression) {
        node.uastParent
    } else {
        node
    }
}
