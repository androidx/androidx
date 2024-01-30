/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler

import androidx.room.compiler.processing.util.compiler.DelegatingTestRegistrar.runCompilation
import java.net.URI
import kotlin.io.path.absolute
import kotlin.io.path.toPath
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.util.ServiceLoaderLite

/**
 * A utility object for setting up Kotlin Compiler plugins that delegate to a list of thread local
 * plugins.
 *
 * see [runCompilation] for usages.
 */
@OptIn(ExperimentalCompilerApi::class)
object DelegatingTestRegistrar {

    @Suppress("DEPRECATION")
    private val k1Delegates =
        ThreadLocal<List<org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar>>()

    private val k2Delegates = ThreadLocal<List<CompilerPluginRegistrar>>()

    class K1Registrar :
        @Suppress("DEPRECATION") org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
        override fun registerProjectComponents(
            project: MockProject,
            configuration: CompilerConfiguration
        ) {
            k1Delegates.get()?.forEach { it.registerProjectComponents(project, configuration) }
        }

        // FirKotlinToJvmBytecodeCompiler throws an error when it sees an incompatible plugin.
        override val supportsK2: Boolean
            get() = true
    }

    class K2Registrar : CompilerPluginRegistrar() {
        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
            k2Delegates.get()?.forEach { with(it) { registerExtensions(configuration) } }
        }

        override val supportsK2: Boolean
            get() = true
    }

    private const val K1_SERVICES_REGISTRAR_PATH =
        "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"

    private const val K2_SERVICES_REGISTRAR_PATH =
        "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"

    private val k1ResourcePathForSelfClassLoader by lazy {
        getResourcePathForClassLoader(K1_SERVICES_REGISTRAR_PATH)
    }

    private val k2ResourcePathForSelfClassLoader by lazy {
        getResourcePathForClassLoader(K2_SERVICES_REGISTRAR_PATH)
    }

    private fun getResourcePathForClassLoader(servicesRegistrarPath: String): String {
        val registrarClassToLoad =
            when (servicesRegistrarPath) {
                K1_SERVICES_REGISTRAR_PATH ->
                    @Suppress("DEPRECATION")
                    org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar::class
                K2_SERVICES_REGISTRAR_PATH -> CompilerPluginRegistrar::class
                else -> error("Unknown services registrar path: $servicesRegistrarPath")
            }
        val expectedRegistrarClass =
            when (servicesRegistrarPath) {
                K1_SERVICES_REGISTRAR_PATH -> K1Registrar::class
                K2_SERVICES_REGISTRAR_PATH -> K2Registrar::class
                else -> error("Unknown services registrar path: $servicesRegistrarPath")
            }
        val classpath =
            this::class
                .java
                .classLoader
                .getResources(servicesRegistrarPath)
                .asSequence()
                .mapNotNull { url ->
                    val uri = URI.create(url.toString().removeSuffix("/$servicesRegistrarPath"))
                    when (uri.scheme) {
                        "jar" -> URI.create(uri.schemeSpecificPart.removeSuffix("!")).toPath()
                        "file" -> uri.toPath()
                        else -> return@mapNotNull null
                    }.absolute()
                }
                .find { resourcesPath ->
                    ServiceLoaderLite.findImplementations(
                            registrarClassToLoad.java,
                            listOf(resourcesPath.toFile())
                        )
                        .any { implementation ->
                            implementation == expectedRegistrarClass.java.name
                        }
                }
        if (classpath == null) {
            throw AssertionError(
                """
                Could not find the $registrarClassToLoad class loader that should load
                $expectedRegistrarClass
                """
                    .trimIndent()
            )
        }
        return classpath.toString()
    }

    internal fun runCompilation(
        compiler: K2JVMCompiler,
        messageCollector: MessageCollector,
        arguments: K2JVMCompilerArguments,
        registrars: PluginRegistrarArguments
    ): ExitCode {
        try {
            k1Delegates.set(registrars.k1Registrars)
            k2Delegates.set(registrars.k2Registrars)
            arguments.addDelegatingTestRegistrars()
            return compiler.exec(
                messageCollector = messageCollector,
                services = Services.EMPTY,
                arguments = arguments
            )
        } finally {
            k1Delegates.remove()
            k2Delegates.remove()
        }
    }

    private fun K2JVMCompilerArguments.addDelegatingTestRegistrars() {
        pluginClasspaths =
            buildList {
                    pluginClasspaths?.let { addAll(it) }
                    add(k1ResourcePathForSelfClassLoader)
                    add(k2ResourcePathForSelfClassLoader)
                }
                .toTypedArray()
    }
}
