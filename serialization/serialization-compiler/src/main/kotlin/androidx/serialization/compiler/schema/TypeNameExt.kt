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

package androidx.serialization.compiler.schema

import androidx.serialization.schema.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.SimpleElementVisitor6

internal fun TypeElement.toTypeName(): TypeName {
    return this.accept(TypeNameVisitor, mutableListOf())
}

private object TypeNameVisitor : SimpleElementVisitor6<TypeName, MutableList<String>>() {
    override fun visitType(e: TypeElement, p: MutableList<String>): TypeName {
        p += e.simpleName.toString()
        return e.enclosingElement.accept(this, p)
    }

    override fun visitPackage(e: PackageElement, p: MutableList<String>): TypeName {
        p.reverse()

        return if (e.isUnnamed) {
            TypeName(null, p)
        } else {
            TypeName(e.qualifiedName.toString(), p)
        }
    }

    override fun visitUnknown(e: Element?, p: MutableList<String>?): TypeName {
        throw IllegalArgumentException("Unexpected class nesting structure: $e")
    }
}
