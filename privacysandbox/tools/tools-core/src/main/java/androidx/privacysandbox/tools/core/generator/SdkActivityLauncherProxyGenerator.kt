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

import androidx.privacysandbox.tools.core.generator.SpecNames.iBinderClass
import androidx.privacysandbox.tools.core.generator.SpecNames.resumeMethod
import androidx.privacysandbox.tools.core.generator.SpecNames.resumeWithExceptionMethod
import androidx.privacysandbox.tools.core.generator.SpecNames.suspendCancellableCoroutineMethod
import androidx.privacysandbox.tools.core.model.Types
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Used in client-side code generation to ensure that `SdkActivityLauncher`s are not repeatedly
 * repackaged into binders.
 */
class SdkActivityLauncherProxyGenerator(private val basePackageName: String) {
    companion object {
        const val proxyClassName = "SdkActivityLauncherProxy"
        const val converterClassName = "SdkActivityLauncherConverter"
        val iSdkActivityLauncher = ClassName(
            "androidx.privacysandbox.ui.core",
            "ISdkActivityLauncher"
        )
    }

    fun generate(): FileSpec {
        val classSpec = TypeSpec.classBuilder(proxyClassName).build {
            addSuperinterface(Types.sdkActivityLauncher.poetClassName())
            addModifiers(KModifier.PUBLIC)
            primaryConstructor(
                listOf(
                    PropertySpec.builder("remote", iSdkActivityLauncher).build(),
                    PropertySpec.builder(
                        "launcherInfo",
                        SpecNames.bundleClass,
                    ).build(),
                ),
            )
            addFunction(launchSdkActivityFunSpec())
        }

        return FileSpec.builder(basePackageName, proxyClassName).build {
            addCommonSettings()
            addType(classSpec)
            addType(converterObjectSpec())
        }
    }

    private fun launchSdkActivityFunSpec() = FunSpec.builder("launchSdkActivity").build {
        val transactionCallbackName = ClassName(
            "androidx.privacysandbox.ui.core",
            "ISdkActivityLauncherCallback",
            "Stub"
        )
        val tokenParameterName = "sdkActivityHandlerToken"

        addModifiers(KModifier.PUBLIC)
        addModifiers(KModifier.OVERRIDE)
        addModifiers(KModifier.SUSPEND)
        addParameter(tokenParameterName, iBinderClass)
        returns(Boolean::class)
        addCode {
            addControlFlow("return %M", suspendCancellableCoroutineMethod) {
                addStatement("remote.launchSdkActivity(")
                indent()
                addStatement("%L,", tokenParameterName)
                addControlFlow("object: %T()", transactionCallbackName) {
                    addControlFlow(
                        "override fun onLaunchAccepted(%L: %T?)",
                        tokenParameterName,
                        iBinderClass,
                    ) {
                        addStatement("it.%M(true)", resumeMethod)
                    }
                    addControlFlow(
                        "override fun onLaunchRejected(%L: %T?)",
                        tokenParameterName,
                        iBinderClass,
                    ) {
                        addStatement("it.%M(true)", resumeMethod)
                    }
                    addControlFlow("override fun onLaunchError(message: String?)") {
                        addStatement("it.%M(RuntimeException(message))", resumeWithExceptionMethod)
                    }
                }
                unindent()
                addStatement(")")
            }
        }
    }

    private fun converterObjectSpec() =
        TypeSpec.objectBuilder("SdkActivityLauncherConverter").build {
            val proxyPoetClassName = ClassName(basePackageName, proxyClassName)

            addFunction(FunSpec.builder("getLocalOrProxyLauncher").build {
                addParameter("launcherInfo", SpecNames.bundleClass)
                returns(Types.sdkActivityLauncher.poetTypeName())
                addStatement(
                    """val remote = launcherInfo.getBinder("sdkActivityLauncherBinderKey")"""
                )
                addStatement(
                    """requireNotNull(remote) { "Invalid SdkActivityLauncher info bundle." }"""
                )
                addStatement(
                    "val binder = %T.Stub.asInterface(remote)",
                    iSdkActivityLauncher,
                )
                addStatement("return SdkActivityLauncherProxy(binder, launcherInfo)")
            })

            addFunction(FunSpec.builder("toBinder").build {
                addParameter("launcher", Types.sdkActivityLauncher.poetClassName())
                returns(SpecNames.bundleClass)
                addCode {
                    addControlFlow("if (launcher is %T)", proxyPoetClassName) {
                        addStatement("return launcher.launcherInfo")
                    }
                }
                addStatement(
                    "return launcher.%M()",
                    MemberName(
                        "androidx.privacysandbox.ui.client",
                        "toLauncherInfo"
                    )
                )
            })
        }
}
