/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isTypeElement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

data class Dao(
    val element: XTypeElement,
    val type: XType,
    val queryMethods: List<QueryMethod>,
    val rawQueryMethods: List<RawQueryMethod>,
    val insertionMethods: List<InsertionMethod>,
    val deletionMethods: List<DeletionMethod>,
    val updateMethods: List<UpdateMethod>,
    val upsertionMethods: List<UpsertionMethod>,
    val transactionMethods: List<TransactionMethod>,
    val delegatingMethods: List<KotlinBoxedPrimitiveMethodDelegate>,
    val kotlinDefaultMethodDelegates: List<KotlinDefaultMethodDelegate>,
    val constructorParamType: TypeName?
) {
    // parsed dao might have a suffix if it is used in multiple databases.
    private var suffix: String? = null

    fun setSuffix(newSuffix: String) {
        if (this.suffix != null) {
            throw IllegalStateException("cannot set suffix twice")
        }
        this.suffix = if (newSuffix == "") "" else "_$newSuffix"
    }

    val typeName: ClassName by lazy { element.className }

    val deleteOrUpdateShortcutMethods: List<DeleteOrUpdateShortcutMethod> by lazy {
        deletionMethods + updateMethods
    }

    private val implClassName by lazy {
        if (suffix == null) {
            suffix = ""
        }
        val path = arrayListOf<String>()
        var enclosing = element.enclosingTypeElement
        while (enclosing?.isTypeElement() == true) {
            path.add(enclosing!!.name)
            enclosing = enclosing!!.enclosingTypeElement
        }
        path.reversed().joinToString("_") + "${typeName.simpleName()}${suffix}_Impl"
    }

    val implTypeName: ClassName by lazy {
        ClassName.get(typeName.packageName(), implClassName)
    }
}
