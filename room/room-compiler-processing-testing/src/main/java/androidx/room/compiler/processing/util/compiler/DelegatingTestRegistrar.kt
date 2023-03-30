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

import androidx.room.compiler.processing.util.compiler.DelegatingTestRegistrar.Companion.runCompilation
import java.net.URI
import java.nio.file.Paths
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.util.ServiceLoaderLite

/**
 * A component registrar for Kotlin Compiler that delegates to a list of thread local delegates.
 *
 * see [runCompilation] for usages.
 */
@Suppress("DEPRECATION") // TODO: Migrate ComponentRegistrar to CompilerPluginRegistrar
@OptIn(ExperimentalCompilerApi::class)
internal class DelegatingTestRegistrar :
    @Suppress("DEPRECATION") org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        delegates.get()?.let {
            it.forEach {
                it.registerProjectComponents(project, configuration)
            }
        }
    }

    companion object {
        private const val REGISTRAR_CLASSPATH =
            "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"

        private val resourcePathForSelfClassLoader by lazy {
            this::class.java.classLoader.getResources(REGISTRAR_CLASSPATH)
                .asSequence()
                .mapNotNull { url ->
                    val uri = URI.create(url.toString().removeSuffix("/$REGISTRAR_CLASSPATH"))
                    when (uri.scheme) {
                        "jar" -> Paths.get(URI.create(uri.schemeSpecificPart.removeSuffix("!")))
                        "file" -> Paths.get(uri)
                        else -> return@mapNotNull null
                    }.toAbsolutePath()
                }
                .find { resourcesPath ->
                    ServiceLoaderLite.findImplementations(
                        @Suppress("DEPRECATION")
                        org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar::class.java,
                        listOf(resourcesPath.toFile())
                    ).any { implementation ->
                        implementation == DelegatingTestRegistrar::class.java.name
                    }
                }?.toString()
                ?: throw AssertionError(
                    """
                    Could not find the ComponentRegistrar class loader that should load
                    ${DelegatingTestRegistrar::class.qualifiedName}
                    """.trimIndent()
                )
        }
        @Suppress("DEPRECATION")
        private val delegates =
            ThreadLocal<List<org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar>>()
        fun runCompilation(
            compiler: K2JVMCompiler,
            messageCollector: MessageCollector,
            arguments: K2JVMCompilerArguments,
            @Suppress("DEPRECATION")
            pluginRegistrars: List<org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar>
        ): ExitCode {
            try {
                arguments.addDelegatingTestRegistrar()
                delegates.set(pluginRegistrars)
                return compiler.exec(
                    messageCollector = messageCollector,
                    services = Services.EMPTY,
                    arguments = arguments
                )
            } finally {
                delegates.remove()
            }
        }

        private fun K2JVMCompilerArguments.addDelegatingTestRegistrar() {
            pluginClasspaths = (pluginClasspaths ?: arrayOf()) + resourcePathForSelfClassLoader
        }
    }
}