/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle

import com.google.auto.common.MoreElements
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

fun Element.getPackage(): PackageElement = MoreElements.getPackage(this)

fun Element.getPackageQName() = getPackage().qualifiedName.toString()

fun ExecutableElement.name() = simpleName.toString()

fun ExecutableElement.isPackagePrivate() = !modifiers.any {
    it == Modifier.PUBLIC || it == Modifier.PROTECTED || it == Modifier.PRIVATE
}

fun ExecutableElement.isProtected() = modifiers.contains(Modifier.PROTECTED)

fun TypeElement.methods(): List<ExecutableElement> = ElementFilter.methodsIn(enclosedElements)

private const val SYNTHETIC = "__synthetic_"

fun syntheticName(method: ExecutableElement) = "$SYNTHETIC${method.simpleName}"

fun isSyntheticMethod(method: ExecutableElement) = method.name().startsWith(SYNTHETIC)

