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

package androidx.privacysandbox.tools.apicompiler.generator

import androidx.privacysandbox.tools.core.generator.SpecNames.bundleClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.viewClass
import androidx.privacysandbox.tools.core.generator.addCode
import androidx.privacysandbox.tools.core.generator.addControlFlow
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.stubDelegateNameSpec
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier

/** SDK provider generator that uses Android API 33 to communicate with the Privacy Sandbox. */
internal class Api33SdkProviderGenerator(parsedApi: ParsedApi) :
    AbstractSdkProviderGenerator(parsedApi) {
    companion object {
        private val sandboxedSdkClass = ClassName("android.app.sdksandbox", "SandboxedSdk")
    }

    override val superclassName = ClassName("android.app.sdksandbox", "SandboxedSdkProvider")

    override fun generateOnLoadSdkFunction() =
        FunSpec.builder("onLoadSdk").build {
            addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            addParameter("params", bundleClass)
            returns(sandboxedSdkClass)

            addStatement("val ctx = %N", contextPropertyName)
            addCode {
                addControlFlow("if (ctx == null)") {
                    addStatement(
                        "throw IllegalStateException(\"Context must not be null. " +
                            "Do you need to call attachContext()?\")"
                    )
                }
            }
            addStatement("val sdk = ${createServiceFunctionName(api.getOnlyService())}(ctx)")
            addStatement(
                "return %T(%T(sdk, ctx))",
                sandboxedSdkClass,
                api.getOnlyService().stubDelegateNameSpec(),
            )
        }

    override fun generateGetViewFunction(): FunSpec {
        return FunSpec.builder("getView").build {
            addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            addParameter("windowContext", contextClass)
            addParameter("params", bundleClass)
            addParameter("width", Int::class)
            addParameter("height", Int::class)
            returns(viewClass)
            addStatement(
                "throw UnsupportedOperationException(%S)",
                "This SDK doesn't support explicit SurfaceView requests.",
            )
        }
    }
}
