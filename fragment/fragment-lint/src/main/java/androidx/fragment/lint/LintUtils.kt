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
