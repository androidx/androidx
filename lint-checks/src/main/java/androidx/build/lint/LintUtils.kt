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

package androidx.build.lint

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/*
 * See ag/25264517. This was previously a public extension function that is now private.
 * It's currently used by [BanRestrictToTestsScope] and [BanVisibleForTestingParams].
 */
internal fun PsiElement.getFqName(): String? =
    when (val element = namedUnwrappedElement) {
        is PsiMember ->
            element.getName()?.let { name ->
                val prefix = element.containingClass?.qualifiedName
                (if (prefix != null) "$prefix.$name" else name)
            }

        is KtNamedDeclaration -> element.fqName.toString()
        else -> null
    }
