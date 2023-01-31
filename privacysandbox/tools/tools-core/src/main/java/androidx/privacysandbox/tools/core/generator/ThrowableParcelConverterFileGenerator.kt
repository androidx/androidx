/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.generator.AidlGenerator.Companion.parcelableStackFrameName
import androidx.privacysandbox.tools.core.generator.AidlGenerator.Companion.throwableParcelName
import androidx.privacysandbox.tools.core.generator.SpecNames.stackTraceElementClass
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec

class ThrowableParcelConverterFileGenerator(private val basePackageName: String) {
    companion object {
        const val converterName = "${throwableParcelName}Converter"
        fun toThrowableParcelNameSpec(packageName: String) = MemberName(ClassName(
            packageName,
            converterName
        ), "toThrowableParcel")
        fun fromThrowableParcelNameSpec(packageName: String) = MemberName(ClassName(
            packageName,
            converterName
        ), "fromThrowableParcel")
    }

    private val throwableParcelNameSpec = ClassName(basePackageName, throwableParcelName)
    private val parcelableStackFrameNameSpec = ClassName(basePackageName, parcelableStackFrameName)
    private val toThrowableParcelNameSpec = toThrowableParcelNameSpec(basePackageName)
    private val fromThrowableParcelNameSpec = fromThrowableParcelNameSpec(basePackageName)

    fun generate(convertToParcel: Boolean = false, convertFromParcel: Boolean = false) =
        FileSpec.builder(
            basePackageName,
            converterName
        ).build {
            addCommonSettings()
            addType(generateConverter(convertToParcel, convertFromParcel))
        }

    private fun generateConverter(convertToParcel: Boolean, convertFromParcel: Boolean) =
        TypeSpec.objectBuilder(ClassName(basePackageName, converterName)).build {
            if (convertToParcel) addFunction(generateToThrowableParcel())
            if (convertFromParcel) addFunction(generateFromThrowableParcel())
        }

    private fun generateToThrowableParcel() =
        FunSpec.builder(toThrowableParcelNameSpec.simpleName).build {
            addParameter("throwable", Throwable::class)
            returns(throwableParcelNameSpec)
            addCode {
                add(
                    """
                    val parcel = %T()
                    parcel.exceptionClass = throwable::class.qualifiedName
                    parcel.errorMessage = throwable.message
                    parcel.stackTrace = throwable.stackTrace.map {
                        val stackFrame = %T()
                        stackFrame.declaringClass = it.className
                        stackFrame.methodName = it.methodName
                        stackFrame.fileName = it.fileName
                        stackFrame.lineNumber = it.lineNumber
                        stackFrame
                    }.toTypedArray()
                    throwable.cause?.let {
                        parcel.cause = arrayOf(${toThrowableParcelNameSpec.simpleName}(it))
                    }
                    parcel.suppressedExceptions =
                        throwable.suppressedExceptions.map {
                            ${toThrowableParcelNameSpec.simpleName}(it)
                        }.toTypedArray()
                    return parcel
                """.trimIndent(),
                    throwableParcelNameSpec,
                    parcelableStackFrameNameSpec,
                )
            }
        }

    private fun generateFromThrowableParcel() =
        FunSpec.builder(fromThrowableParcelNameSpec.simpleName).build {
            addParameter("throwableParcel", throwableParcelNameSpec)
            returns(Throwable::class)
            addCode {
                add(
                    """
                    val exceptionClass = throwableParcel.exceptionClass
                    val errorMessage = throwableParcel.errorMessage
                    val stackTrace = throwableParcel.stackTrace
                    val exception = PrivacySandboxException(
                        "[${'$'}exceptionClass] ${'$'}errorMessage",
                        throwableParcel.cause?.firstOrNull()?.let {
                            ${fromThrowableParcelNameSpec.simpleName}(it)
                        }
                    )
                    for (suppressed in throwableParcel.suppressedExceptions) {
                        exception.addSuppressed(${fromThrowableParcelNameSpec.simpleName}(suppressed))
                    }
                    exception.stackTrace =
                        stackTrace.map {
                            %T(
                                it.declaringClass,
                                it.methodName,
                                it.fileName,
                                it.lineNumber
                            )
                        }.toTypedArray()
                    return exception
                """.trimIndent(),
                    stackTraceElementClass,
                )
            }
        }
}