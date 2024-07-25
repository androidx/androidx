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

import com.google.devtools.ksp.AbstractKotlinSymbolProcessingExtension
import com.google.devtools.ksp.KspCliOption
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import java.io.File
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.com.intellij.core.CoreApplicationEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeAdapter
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

/** Registers the KSP component for the kotlin compilation. */
@Suppress("DEPRECATION") // TODO: Migrate ComponentRegistrar to CompilerPluginRegistrar
@OptIn(ExperimentalCompilerApi::class)
internal class TestKspRegistrar(
    val kspWorkingDir: File,
    val baseOptions: KspOptions.Builder,
    val processorProviders: List<SymbolProcessorProvider>,
    val messageCollector: MessageCollector
) : @Suppress("DEPRECATION") org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        baseOptions.apply {
            projectBaseDir = project.basePath?.let { File(it) } ?: kspWorkingDir
            incremental = false
            incrementalLog = false
            languageVersionSettings = configuration.languageVersionSettings
            // NOT supported yet, hence we set a default
            classOutputDir =
                classOutputDir
                    ?: kspWorkingDir.resolve(KspCliOption.CLASS_OUTPUT_DIR_OPTION.optionName)
            // NOT supported yet, hence we set a default
            resourceOutputDir =
                resourceOutputDir
                    ?: kspWorkingDir.resolve(KspCliOption.RESOURCE_OUTPUT_DIR_OPTION.optionName)
            cachesDir =
                cachesDir ?: kspWorkingDir.resolve(KspCliOption.CACHES_DIR_OPTION.optionName)
            kspOutputDir =
                kspOutputDir ?: kspWorkingDir.resolve(KspCliOption.KSP_OUTPUT_DIR_OPTION.optionName)
            val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
            compileClasspath.addAll(
                contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file }
            )
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
        }
        val logger =
            MessageCollectorBasedKSPLogger(
                messageCollector = messageCollector,
                wrappedMessageCollector = messageCollector,
                allWarningsAsErrors = baseOptions.allWarningsAsErrors
            )
        val options = baseOptions.build()
        AnalysisHandlerExtension.registerExtension(
            project,
            TestKspExtension(
                options = options,
                processorProviders = processorProviders,
                logger = logger
            )
        )
        // Placeholder extension point; Required by dropPsiCaches().
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            PsiTreeChangeListener.EP.name,
            PsiTreeChangeAdapter::class.java
        )
    }

    private class TestKspExtension(
        options: KspOptions,
        processorProviders: List<SymbolProcessorProvider>,
        logger: KSPLogger
    ) :
        AbstractKotlinSymbolProcessingExtension(
            options = options,
            logger = logger,
            testMode = false
        ) {
        private val loadedProviders = processorProviders

        override fun loadProviders() = loadedProviders
    }
}
