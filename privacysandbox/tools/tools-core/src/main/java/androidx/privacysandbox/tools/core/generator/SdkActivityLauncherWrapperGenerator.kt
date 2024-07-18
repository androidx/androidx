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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.model.Types
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Used in server-side code generation to ensure that `SdkActivityLauncher`s are not repeatedly
 * repackaged into binders.
 */
class SdkActivityLauncherWrapperGenerator(private val basePackageName: String) {
    companion object {
        const val className = "SdkActivityLauncherAndBinderWrapper"
    }

    fun generate(): FileSpec {
        val classSpec = TypeSpec.classBuilder(className).build {
            addSuperinterface(Types.sdkActivityLauncher.poetClassName(), CodeBlock.of("delegate"))
            addModifiers(KModifier.PUBLIC)
            primaryConstructor(
                listOf(
                    PropertySpec.builder(
                        "delegate",
                        Types.sdkActivityLauncher.poetTypeName(),
                    ).addModifiers(KModifier.PRIVATE).build(),
                    PropertySpec.builder(
                        "launcherInfo",
                        SpecNames.bundleClass,
                    ).build(),
                ),
                KModifier.PRIVATE,
            )

            addFunction(fromLauncherInfo())
            addType(companionObject())
        }

        return FileSpec.builder(basePackageName, className).build {
            addCommonSettings()
            addType(classSpec)
        }
    }

    private fun fromLauncherInfo() = FunSpec.constructorBuilder()
        .addParameter("launcherInfo", SpecNames.bundleClass)
        .callThisConstructor(
            CodeBlock.of(
                "%T.fromLauncherInfo(launcherInfo)",
                ClassName(
                    "androidx.privacysandbox.ui.provider",
                    "SdkActivityLauncherFactory"
                ),
            ),
            CodeBlock.of("launcherInfo"),
        ).build()

    private fun companionObject() = TypeSpec.companionObjectBuilder().addFunction(
        FunSpec.builder("getLauncherInfo").build {
            addParameter("launcher", Types.sdkActivityLauncher.poetClassName())
            returns(SpecNames.bundleClass)
            addCode {
                addControlFlow("if (launcher is %N)", className) {
                    addStatement("return launcher.launcherInfo")
                }
                addStatement(
                    "throw·IllegalStateException(%S)",
                    "Invalid SdkActivityLauncher instance cannot be bundled. " +
                        "SdkActivityLaunchers may only be created by apps."
                )
            }
        }
    ).build()
}
