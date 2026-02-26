/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.solver.types

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.ext.decapitalize
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.CustomDaoReturnTypeConverter
import androidx.room3.writer.TypeWriter
import java.util.Locale

/** Wraps a type converter specified by the developer and forwards calls to it. */
class DaoReturnTypeConverterWrapper(
    val customDaoReturnTypeConverter: CustomDaoReturnTypeConverter
) : DaoReturnTypeConverter(to = customDaoReturnTypeConverter.to) {
    override val isSuspend = customDaoReturnTypeConverter.function.isSuspendFunction()
    override val requiredFunctionParamTypes =
        customDaoReturnTypeConverter.requiredFunctionParamTypes
    override val executeAndReturnLambda = customDaoReturnTypeConverter.executeAndReturnLambda
    private val converterClassName = customDaoReturnTypeConverter.className

    override fun buildStatement(returnTypeArgName: XTypeName, scope: CodeGenScope): XCodeBlock {
        val converterFunctionName = customDaoReturnTypeConverter.getFunctionName(scope.language)
        return if (customDaoReturnTypeConverter.isEnclosingClassKotlinObject) {
            when (scope.language) {
                CodeLanguage.JAVA ->
                    XCodeBlock.of(
                        "%T.INSTANCE.%L",
                        converterClassName,
                        customDaoReturnTypeConverter.getFunctionName(scope.language),
                    )
                CodeLanguage.KOTLIN ->
                    XCodeBlock.of("%T.%L", converterClassName, converterFunctionName)
            }
        } else if (customDaoReturnTypeConverter.isStatic) {
            XCodeBlock.of("%T.%L(%L)", converterClassName, converterFunctionName, returnTypeArgName)
        } else {
            // TODO(b/460563469): Implement Provided DAO Return Type Converter handling, this
            //  codepath is currently unused.
            XCodeBlock.of("%N.%L", daoReturnTypeConverter(scope), converterFunctionName)
        }
    }

    private fun daoReturnTypeConverter(scope: CodeGenScope): XPropertySpec {
        val baseName = converterClassName.simpleNames.last().decapitalize(Locale.US)
        val propertySpec =
            object : TypeWriter.SharedPropertySpec(baseName, converterClassName) {
                override fun getUniqueKey(): String {
                    return "converter_${converterClassName}"
                }

                override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {
                    builder.initializer(XCodeBlock.ofNewInstance(converterClassName))
                }
            }
        return scope.writer.getOrCreateProperty(propertySpec)
    }
}
