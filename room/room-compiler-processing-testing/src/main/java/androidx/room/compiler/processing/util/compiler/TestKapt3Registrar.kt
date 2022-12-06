/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import java.io.File
import javax.annotation.processing.Processor
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.base.kapt3.logString
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.kapt3.AbstractKapt3Extension
import org.jetbrains.kotlin.kapt3.KaptAnonymousTypeTransformer
import org.jetbrains.kotlin.kapt3.base.LoadedProcessors
import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt3.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension

/**
 * Registers the KAPT component for the kotlin compilation.
 *
 * mostly taken from
 * https://github.com/JetBrains/kotlin/blob/master/plugins/kapt3/kapt3-compiler/src/
 *  org/jetbrains/kotlin/kapt3/Kapt3Plugin.kt
 */
@Suppress("DEPRECATION") // TODO: Migrate ComponentRegistrar to CompilerPluginRegistrar
@OptIn(ExperimentalCompilerApi::class)
internal class TestKapt3Registrar(
    val processors: List<Processor>,
    val baseOptions: KaptOptions.Builder,
    val messageCollector: MessageCollector
) : @Suppress("DEPRECATION") org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        doOpenInternalPackagesIfRequired()
        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val optionsBuilder = baseOptions.apply {
            projectBaseDir = project.basePath?.let(::File)
            compileClasspath.addAll(
                contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file }
            )
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir =
                classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        }

        val logger = MessageCollectorBackedKaptLogger(
            isVerbose = optionsBuilder.flags.contains(KaptFlag.VERBOSE),
            isInfoAsWarnings = optionsBuilder.flags.contains(KaptFlag.INFO_AS_WARNINGS),
            messageCollector = messageCollector
        )

        val options = optionsBuilder.build()

        options.sourcesOutputDir.mkdirs()

        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        val kapt3AnalysisCompletedHandlerExtension = object : AbstractKapt3Extension(
            options = options,
            logger = logger,
            compilerConfiguration = configuration
        ) {
            override fun loadProcessors(): LoadedProcessors {
                return LoadedProcessors(
                    processors = processors.map {
                        IncrementalProcessor(
                            processor = it,
                            kind = DeclaredProcType.NON_INCREMENTAL,
                            logger = logger
                        )
                    },
                    classLoader = TestKapt3Registrar::class.java.classLoader
                )
            }
        }

        AnalysisHandlerExtension.registerExtension(project, kapt3AnalysisCompletedHandlerExtension)
        StorageComponentContainerContributor.registerExtension(
            project,
            KaptComponentContributor(kapt3AnalysisCompletedHandlerExtension)
        )
    }

    class KaptComponentContributor(private val analysisExtension: PartialAnalysisHandlerExtension) :
        StorageComponentContainerContributor {
        override fun registerModuleComponents(
            container: StorageComponentContainer,
            platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor
        ) {
            if (!platform.isJvm()) return
            container.useInstance(KaptAnonymousTypeTransformer(analysisExtension))
        }
    }
}