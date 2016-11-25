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

package com.android.support.room.processor

import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.preconditions.Checks
import com.android.support.room.vo.Dao
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.TypeElement

class DaoParser(val roundEnv: RoundEnvironment,
                val processingEnvironment: ProcessingEnvironment) {
    val queryParser = QueryParser(roundEnv, processingEnvironment)
    fun parse(element: TypeElement) : Dao {
        Checks.check(element.hasAnnotation(com.android.support.room.Dao::class), element,
                ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO)
        Checks.check(element.hasAnyOf(ABSTRACT) || element.kind == ElementKind.INTERFACE,
                element, ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)

        val declaredType = MoreTypes.asDeclared(element.asType())
        val allMembers = processingEnvironment.elementUtils.getAllMembers(element)
        val methods = allMembers.filter {
            it.hasAnyOf(ABSTRACT)
        }.map {
            queryParser.parse(declaredType, MoreElements.asExecutable(it))
        }
        val type = TypeName.get(declaredType)
        Checks.assertNotUnbound(type, element,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES)
        return Dao(element = element, type = type, queryMethods = methods)
    }
}