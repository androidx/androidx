/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.compiler.ext

import androidx.contentaccess.compiler.utils.ErrorIndicator
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

fun Messager.reportError(error: String, element: Element) {
    this.printMessage(
        Diagnostic.Kind.ERROR, error, element)
}

fun Messager.reportError(error: String, element: Element, errorIndicator: ErrorIndicator) {
    errorIndicator.indicateError()
    this.printMessage(
        Diagnostic.Kind.ERROR, error, element)
}