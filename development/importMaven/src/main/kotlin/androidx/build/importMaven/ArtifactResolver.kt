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

package androidx.build.importMaven

import androidx.build.importMaven.ArtifactResolver.resolveArtifacts
import androidx.build.importMaven.KmpConfig.SUPPORTED_KONAN_TARGETS
import java.net.URI
import org.apache.logging.log4j.kotlin.logger
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.internal.artifacts.verification.exceptions.DependencyVerificationException
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Provides functionality to resolve and download artifacts.
 * see: [resolveArtifacts]
 * see: [LocalMavenRepoDownloader]
 * see: [MavenRepositoryProxy]
 */
internal object ArtifactResolver {
    internal val jetbrainsRepositories = listOf(
        "https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/",
        "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev",
        "https://maven.pkg.jetbrains.space/public/p/compose/dev",
        "https://maven.pkg.jetbrains.space/kotlin/p/dokka/test"
    )

    internal val gradlePluginPortalRepo = "https://plugins.gradle.org/m2/"

    internal fun createAndroidXRepo(
        buildId: Int
    ) = "https://androidx.dev/snapshots/builds/$buildId/artifacts/repository"

    internal fun createMetalavaRepo(
        buildId: Int
    ) = "https://androidx.dev/metalava/builds/$buildId/artifacts/repo/m2repository"

    /**
     * Resolves given set of [artifacts].
     *
     * @param artifacts List of artifacts to resolve.
     * @param additionalRepositories List of repositories in addition to mavenCentral and google
     * @param localRepositories List of local repositories. If an artifact is found here, it won't
     *        be downloaded.
     * @param explicitlyFetchInheritedDependencies If set to true, each discovered dependency will
     *        be fetched again. For instance:
     *        artifact1:v1
     *          artifact2:v2
     *            artifact3:v1
     *          artifact3:v3
     *       If this flag is `false`, we'll only fetch artifact1:v1, artifact2:v2, artifact3:v3.
     *       If this flag is `true`, we'll fetch `artifact3:v1` as well (because artifact2:v2
     *       declares a dependency on it even though it is overridden by the dependency of
     *       artifact1:v1
     * @param downloadObserver An observer that will be notified each time a file is downloaded from
     *        a remote repository.
     */
    fun resolveArtifacts(
        artifacts: List<String>,
        additionalRepositories: List<String> = emptyList(),
        localRepositories: List<String> = emptyList(),
        explicitlyFetchInheritedDependencies: Boolean = false,
        downloadObserver: DownloadObserver?,
    ): ArtifactsResolutionResult {
        return SingleUseArtifactResolver(
            project = ProjectService.createProject(),
            artifacts = artifacts,
            additionalPriorityRepositories = additionalRepositories,
            localRepositories = localRepositories,
            explicitlyFetchInheritedDependencies = explicitlyFetchInheritedDependencies,
            downloadObserver = downloadObserver
        ).resolveArtifacts()
    }

    /**
     * see docs for [ArtifactResolver.resolveArtifacts]
     */
    private class SingleUseArtifactResolver(
        private val project: Project,
        private val artifacts: List<String>,
        private val additionalPriorityRepositories: List<String>,
        private val localRepositories: List<String>,
        private val explicitlyFetchInheritedDependencies: Boolean,
        private val downloadObserver: DownloadObserver?,
    ) {
        private val logger = logger("ArtifactResolver")
        fun resolveArtifacts(): ArtifactsResolutionResult {
            logger.info {
                """--------------------------------------------------------------------------------
Resolving artifacts:
${artifacts.joinToString(separator = "\n - ", prefix = " - ")}
Local repositories:
${localRepositories.joinToString(separator = "\n - ", prefix = " - ")}
High priority repositories:
${
    if (additionalPriorityRepositories.isEmpty())
        " - None"
    else
        additionalPriorityRepositories.joinToString(separator = "\n - ", prefix = " - ")
}
--------------------------------------------------------------------------------"""
            }
            return withProxyServer(
                downloadObserver = downloadObserver
            ) {
                logger.trace {
                    "Initialized proxy servers"
                }
                var dependenciesPassedVerification = true

                project.dependencies.apply {
                    components.all(CustomMetadataRules::class.java)
                    attributesSchema.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)
                        .compatibilityRules.add(JarAndAarAreCompatible::class.java)
                }
                val completedComponentIds = mutableSetOf<String>()
                val pendingComponentIds = mutableSetOf<String>().also {
                    it.addAll(artifacts)
                }
                val allResolvedArtifacts = mutableSetOf<ResolvedArtifactResult>()
                do {
                    val dependencies = pendingComponentIds.map {
                        project.dependencies.create(it)
                    }
                    val resolvedArtifacts = createConfigurationsAndResolve(dependencies)
                    if (!resolvedArtifacts.dependenciesPassedVerification) {
                        dependenciesPassedVerification = false
                    }
                    allResolvedArtifacts.addAll(resolvedArtifacts.artifacts)
                    completedComponentIds.addAll(pendingComponentIds)
                    pendingComponentIds.clear()
                    val newComponentIds = resolvedArtifacts.artifacts.mapNotNull {
                        (it.id.componentIdentifier as? ModuleComponentIdentifier)?.toString()
                    }.filter {
                        !completedComponentIds.contains(it) && pendingComponentIds.add(it)
                    }
                    logger.trace {
                        "New component ids:\n${newComponentIds.joinToString("\n")}"
                    }
                    pendingComponentIds.addAll(newComponentIds)
                } while (explicitlyFetchInheritedDependencies && pendingComponentIds.isNotEmpty())
                ArtifactsResolutionResult(
                    allResolvedArtifacts.toList(),
                    dependenciesPassedVerification
                )
            }.also { result ->
                val artifacts = result.artifacts
                logger.trace {
                    "Resolved files: ${artifacts.size}"
                }
                check(artifacts.isNotEmpty()) {
                    "Didn't resolve any artifacts from $artifacts. Try --verbose for more " +
                      "information"
                }
                artifacts.forEach { artifact ->
                    logger.trace {
                        artifact.id.toString()
                    }
                }
            }
        }

        /**
         * Creates configurations with the given list of dependencies and resolves them.
         */
        private fun createConfigurationsAndResolve(
            dependencies: List<Dependency>
        ): ArtifactsResolutionResult {
            val configurations = dependencies.flatMap { dep ->
                buildList {
                    addAll(createApiConfigurations(dep))
                    addAll(createRuntimeConfigurations(dep))
                    addAll(createGradlePluginConfigurations(dep))
                    addAll(createKmpConfigurations(dep))
                }
            }
            val resolutionList = configurations.map { configuration ->
                resolveArtifacts(configuration, disableVerificationOnFailure = true)
            }
            val artifacts = resolutionList.flatMap { resolution ->
                resolution.artifacts
            }
            val dependenciesPassedVerification = resolutionList.map { resolution ->
                resolution.dependenciesPassedVerification
            }.all { it == true }
            return ArtifactsResolutionResult(artifacts, dependenciesPassedVerification)
        }

        /**
         * Resolves the given configuration.
         * @param configuration The configuration to resolve
         * @param disableVerificationOnFailure If set, this method will try to re-resolve the
         *        configuration without dependency verification. This might be necessary if an
         *        artifact is signed but the key is not registered in any of the public key servers.
         */
        private fun resolveArtifacts(
            configuration: Configuration,
            disableVerificationOnFailure: Boolean
        ): ArtifactsResolutionResult {
            return try {
                val artifacts = configuration.incoming.artifactView {
                    // We need to be lenient because we are requesting files that might not exist.
                    // For example source.jar or .asc.
                    it.lenient(true)
                }.artifacts.artifacts.toList()
                ArtifactsResolutionResult(artifacts.toList(), dependenciesPassedVerification = true)
            } catch (verificationException: DependencyVerificationException) {
                if (disableVerificationOnFailure) {
                    val copy = configuration.copyRecursive().also {
                        it.resolutionStrategy.disableDependencyVerification()
                    }
                    logger.warn {
                        """
Failed key verification for public servers, will retry without verification.
${verificationException.message?.prependIndent("    ")}
                        """
                    }
                    val artifacts = resolveArtifacts(copy, disableVerificationOnFailure = false)
                    return ArtifactsResolutionResult(
                        artifacts.artifacts,
                        dependenciesPassedVerification = false
                    )
                } else {
                    throw verificationException
                }
            }
        }

        /**
         * Creates proxy servers for remote repositories, adds them to the project and invokes
         * the block. Once the block is complete, all proxy servers will be closed.
         */
        private fun <T> withProxyServer(
            downloadObserver: DownloadObserver? = null,
            block: () -> T
        ): T {
            val repoUrls = additionalPriorityRepositories + listOf(
                RepositoryHandler.GOOGLE_URL,
                RepositoryHandler.MAVEN_CENTRAL_URL,
                gradlePluginPortalRepo
            )
            return MavenRepositoryProxy.startAll(
                repositoryUrls = repoUrls,
                downloadObserver = downloadObserver
            ) { repoUris ->
                project.repositories.clear()
                // add local repositories first, they are not tracked
                localRepositories.map { localRepo ->
                    project.repositories.maven {
                        it.url = URI(localRepo)
                    }
                }
                repoUris.map { mavenUri ->
                    project.repositories.maven {
                        it.url = mavenUri
                        it.isAllowInsecureProtocol = true
                    }
                }
                block()
            }
        }

        private fun createConfiguration(
            vararg dependencies: Dependency,
            configure: Configuration.() -> Unit
        ): Configuration {
            val configuration = project.configurations.detachedConfiguration(*dependencies)
            configuration.configure()
            return configuration
        }

        /**
         * Creates a configuration that has the same attributes as java runtime configuration
         */
        private fun createRuntimeConfigurations(
            vararg dependencies: Dependency
        ): List<Configuration> {
            return listOf(
                LibraryElements.JAR to TargetJvmEnvironment.STANDARD_JVM,
                LibraryElements.JAR to TargetJvmEnvironment.ANDROID,
                "aar" to TargetJvmEnvironment.ANDROID,
            ).map { (libraryElement, jvmEnvironment) ->
                createConfiguration(*dependencies) {
                    attributes.apply {
                        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, libraryElement)
                        attribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_RUNTIME)
                        attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                        attribute(
                            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                            jvmEnvironment
                        )
                    }
                }
            }
        }

        @Suppress("UnstableApiUsage")
        private fun createGradlePluginConfigurations(
            vararg dependencies: Dependency
        ): List<Configuration> {
            return listOf(
                GradleVersion.current().baseVersion,
                GradleVersion.current()
            ).map { version ->
                // taken from DefaultScriptHandler in gradle
                createConfiguration(*dependencies) {
                    attributes.apply {
                        attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                        attribute(
                            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                            TargetJvmEnvironment.STANDARD_JVM
                        )

                        attribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_API)

                        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, LibraryElements.JAR)

                        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                        attribute(
                            GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                            version.version
                        )
                    }
                }
            }
        }

        /**
         * Creates a configuration that has the same attributes as java api configuration
         */
        private fun createApiConfigurations(
            vararg dependencies: Dependency
        ): List<Configuration> {
            return listOf(
                LibraryElements.JAR,
                "aar"
            ).map { libraryElement ->
                createConfiguration(*dependencies) {
                    attributes.apply {
                        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, libraryElement)
                        attribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_API)
                        attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                    }
                }
            }
        }

        /**
         * Creates configuration that resembles the ones created by KMP.
         * Note that, the configurations built by KMP depends on flags etc so to account for all of
         * them, we create all variations with different attribute values.
         */
        private fun createKmpConfigurations(
            vararg dependencies: Dependency,
        ): List<Configuration> {
            val konanTargetConfigurations = SUPPORTED_KONAN_TARGETS.flatMap { konanTarget ->
                KOTlIN_USAGES.map { kotlinUsage ->
                    createKonanTargetConfiguration(
                        dependencies = dependencies,
                        konanTarget = konanTarget,
                        kotlinUsage = kotlinUsage
                    )
                }
            }
            // jvm and android configurations
            val jvmAndAndroid = KOTlIN_USAGES.flatMap { kotlinUsage ->
                listOf(
                    "jvm",
                    TargetJvmEnvironment.ANDROID
                ).map { targetJvm ->
                    createConfiguration(*dependencies) {
                        attributes.apply {
                            attribute(Usage.USAGE_ATTRIBUTE, kotlinUsage)
                            attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                            attribute(
                                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                                targetJvm
                            )
                        }
                    }
                }
            }

            val wasmJs = KOTlIN_USAGES.map { kotlinUsage ->
                createConfiguration(*dependencies) {
                    attributes.apply {
                        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
                        attribute(Usage.USAGE_ATTRIBUTE, kotlinUsage)
                        attribute(
                            KotlinWasmTargetAttribute.wasmTargetAttribute,
                            KotlinWasmTargetAttribute.js
                        )
                        attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, "non-jvm")
                    }
                }
            }

            val js =
                KOTlIN_USAGES.map { kotlinUsage ->
                    createConfiguration(*dependencies) {
                        attributes.apply {
                            attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                            attribute(
                                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                                "non-jvm"
                            )
                            attribute(Usage.USAGE_ATTRIBUTE, kotlinUsage)
                            attribute(
                                KotlinJsCompilerAttribute.jsCompilerAttribute,
                                KotlinJsCompilerAttribute.ir
                            )
                            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
                        }
                    }
                }

            val commonArtifacts = KOTlIN_USAGES.map { kotlinUsage ->
                createConfiguration(*dependencies) {
                    attributes.apply {
                        attribute(Usage.USAGE_ATTRIBUTE, kotlinUsage)
                        attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
                    }
                }
            }
            return jvmAndAndroid + wasmJs + js + konanTargetConfigurations + commonArtifacts
        }

        private fun createKonanTargetConfiguration(
            vararg dependencies: Dependency,
            konanTarget: KonanTarget,
            kotlinUsage: String
        ): Configuration {
            return createConfiguration(*dependencies) {
                attributes.apply {
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                    attribute(Usage.USAGE_ATTRIBUTE, kotlinUsage)
                    attribute(KotlinNativeTarget.konanTargetAttribute, konanTarget.name)
                    attribute(Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, "non-jvm")
                }
            }
        }

        private fun <T : Named> AttributeContainer.attribute(
            key: Attribute<T>,
            value: String
        ) = attribute(
            key, project.objects.named(
                key.type,
                value
            )
        )

        companion object {
            /**
             * Kotlin usage attributes that we want to pull.
             */
            private val KOTlIN_USAGES = listOf(
                KotlinUsages.KOTLIN_API,
                KotlinUsages.KOTLIN_METADATA,
                KotlinUsages.KOTLIN_CINTEROP,
                KotlinUsages.KOTLIN_RUNTIME,
                KotlinUsages.KOTLIN_SOURCES
            )
        }
    }
}
