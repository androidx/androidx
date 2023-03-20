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

package androidx.compose.compiler.plugins.kotlin.facade

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class FirAnalysisResult(
    val moduleCompilerAnalyzedOutput: ModuleCompilerAnalyzedOutput,
    override val files: List<KtFile>,
    val reporter: BaseDiagnosticsCollector
): AnalysisResult {
    override val diagnostics: List<AnalysisResult.Diagnostic>
        get() = reporter.diagnostics.map {
            AnalysisResult.Diagnostic(it.factoryName, it.textRanges)
        }
}

private class FirFrontendResult(
    val firResult: Fir2IrActualizedResult,
    val generatorExtensions: JvmGeneratorExtensions,
)

class K2CompilerFacade(environment: KotlinCoreEnvironment) : KotlinCompilerFacade(environment) {
    init {
        PsiElementFinder.EP.getPoint(environment.project)
            .unregisterExtension(JavaElementFinder::class.java)
    }

    private val project: Project
        get() = environment.project

    private val configuration: CompilerConfiguration
        get() = environment.configuration

    override fun analyze(files: List<SourceFile>): FirAnalysisResult {
        val ktFiles = files.map { it.toKtFile(project) }

        val session = createSessionForTests(
            sourceScope = GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })
                .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project)),
            librariesScope = ProjectScope.getLibrariesScope(project),
            moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME, "main"),
            getPackagePartProvider = environment::createPackagePartProvider
        )
        val reporter = DiagnosticReporterFactory.createReporter()
        val analysis = buildResolveAndCheckFir(session, ktFiles, reporter)
        return FirAnalysisResult(analysis, ktFiles, reporter)
    }

    private fun frontend(files: List<SourceFile>): FirFrontendResult {
        val analysisResult = analyze(files)

        FirDiagnosticsCompilerResultsReporter.throwFirstErrorAsException(
            analysisResult.reporter,
            MessageRenderer.PLAIN_FULL_PATHS
        )

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)

        val fir2IrResult = FirResult(listOf(
            analysisResult.moduleCompilerAnalyzedOutput
        )).convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            Fir2IrConfiguration(
                configuration.languageVersionSettings,
                configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES),
                object : EvaluatedConstTracker() {
                    private val storage =
                        ConcurrentHashMap<String, ConcurrentHashMap<Pair<Int, Int>, ConstantValue<*>>>()

                    override fun save(
                        start: Int,
                        end: Int,
                        file: String,
                        constant: ConstantValue<*>
                    ) {
                        storage
                            .getOrPut(file) { ConcurrentHashMap() }
                            .let { it[start to end] = constant }
                    }

                    override fun load(start: Int, end: Int, file: String): ConstantValue<*>? {
                        return storage[file]?.get(start to end)
                    }

                    override fun load(file: String): Map<Pair<Int, Int>, ConstantValue<*>>? {
                        return storage[file]
                    }
                }
            ),
            IrGenerationExtension.getInstances(project),
            analysisResult.reporter
        )

        return FirFrontendResult(fir2IrResult, fir2IrExtensions)
    }

    override fun compileToIr(files: List<SourceFile>): IrModuleFragment =
        frontend(files).firResult.irModuleFragment

    override fun compile(files: List<SourceFile>): GenerationState {
        val frontendResult = frontend(files)
        val irModuleFragment = frontendResult.firResult.irModuleFragment
        val components = frontendResult.firResult.components

        val generationState = GenerationState.Builder(
            project,
            ClassBuilderFactories.TEST,
            irModuleFragment.descriptor,
            NoScopeRecordCliBindingTrace().bindingContext,
            configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).build()

        generationState.beforeCompile()
        val codegenFactory = JvmIrCodegenFactory(
            configuration,
            configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        )
        codegenFactory.generateModuleInFrontendIRMode(
            generationState, irModuleFragment, components.symbolTable, components.irProviders,
            frontendResult.generatorExtensions,
            FirJvmBackendExtension(components, frontendResult.firResult.irActualizedResult),
            frontendResult.firResult.pluginContext
        ) {}
        generationState.factory.done()
        return generationState
    }

    private fun createSessionForTests(
        sourceScope: GlobalSearchScope,
        librariesScope: GlobalSearchScope,
        moduleName: String,
        getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ): FirSession {
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            JvmPlatformAnalyzerServices,
            externalSessionProvider = null,
            VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                getPackagePartProvider
            ),
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            PsiBasedProjectFileSearchScope(sourceScope),
            PsiBasedProjectFileSearchScope(librariesScope),
            lookupTracker = null,
            enumWhenTracker = null,
            incrementalCompilationContext = null,
            extensionRegistrars = FirExtensionRegistrar.getInstances(project),
            needRegisterJavaElementFinder = true,
        )
    }
}
