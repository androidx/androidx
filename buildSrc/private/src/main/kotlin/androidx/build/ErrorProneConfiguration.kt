/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build

import androidx.build.java.JavaCompileInputs
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

const val ERROR_PRONE_TASK = "runErrorProne"

private const val ERROR_PRONE_CONFIGURATION = "errorprone"
private val log = Logging.getLogger("ErrorProneConfiguration")

fun Project.configureErrorProneForJava() {
    val errorProneConfiguration = createErrorProneConfiguration()
    project.extensions.getByName<SourceSetContainer>("sourceSets").configureEach {
        project.configurations[it.annotationProcessorConfigurationName].extendsFrom(
            errorProneConfiguration
        )
    }
    val kmpExtension = project.multiplatformExtension
    if (kmpExtension?.targets?.any { it is KotlinJvmTarget && it.withJavaEnabled } == false) {
        // only configure error prone when Kotlin adds compileJava task
        return
    }

    log.info("Configuring error-prone for ${project.path}")
    if (kmpExtension != null) {
        val jvmJarProvider = tasks.named(kmpExtension.jvm().artifactsTaskName, Jar::class.java)
        makeKmpErrorProneTask(
            COMPILE_JAVA_TASK_NAME,
            jvmJarProvider,
            JavaCompileInputs.fromKmpJvmTarget(project)
        )
    } else {
        makeErrorProneTask(COMPILE_JAVA_TASK_NAME)
    }
}

fun Project.configureErrorProneForAndroid() {
    val androidComponents = extensions.findByType(AndroidComponentsExtension::class.java)
    androidComponents?.onVariants { variant ->
        if (variant.buildType == "release") {
            val errorProneConfiguration = createErrorProneConfiguration()
            configurations
                .getByName(variant.annotationProcessorConfiguration.name)
                .extendsFrom(errorProneConfiguration)

            log.info("Configuring error-prone for ${variant.name}'s java compile")
            makeErrorProneTask("compile${variant.name.camelCase()}JavaWithJavac") { javaCompile ->
                @Suppress("UnstableApiUsage")
                val annotationArgs = variant.javaCompilation.annotationProcessor.arguments
                javaCompile.options.compilerArgumentProviders.add(
                    CommandLineArgumentProviderAdapter(annotationArgs)
                )
            }
        }
    }
}

class CommandLineArgumentProviderAdapter(@get:Input val arguments: Provider<Map<String, String>>) :
    CommandLineArgumentProvider {
    override fun asArguments(): MutableIterable<String> {
        return mutableListOf<String>().also {
            for ((key, value) in arguments.get()) {
                it.add("-A$key=$value")
            }
        }
    }
}

private fun Project.createErrorProneConfiguration(): Configuration =
    configurations.findByName(ERROR_PRONE_CONFIGURATION)
        ?: configurations.create(ERROR_PRONE_CONFIGURATION).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            exclude(group = "com.google.errorprone", module = "javac")
            project.dependencies.add(ERROR_PRONE_CONFIGURATION, getLibraryByName("errorProne"))
        }

// Given an existing JavaCompile task, reconfigures the task to use the ErrorProne compiler plugin
private fun JavaCompile.configureWithErrorProne() {
    options.isFork = true
    options.forkOptions.jvmArgs!!.addAll(
        listOf(
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
        )
    )
    val compilerArgs = this.options.compilerArgs
    compilerArgs +=
        listOf(
            // Tell error-prone that we are running it on android compatible libraries
            "-XDandroidCompatible=true",
            "-XDcompilePolicy=simple", // Workaround for b/36098770
            listOf(
                    "-Xplugin:ErrorProne",

                    // Ignore intermediate build output, generated files, and external sources. Also
                    // sources
                    // imported from Android Studio and IntelliJ which are used in the lint-checks
                    // project.
                    "-XepExcludedPaths:.*/(build/generated|build/errorProne|external|" +
                        "compileTransaction/compile-output|" +
                        "lint-checks/src/main/java/androidx/com)/.*",

                    // Consider re-enabling the following checks. Disabled as part of
                    // error-prone upgrade
                    "-Xep:InlineMeSuggester:OFF",
                    "-Xep:NarrowCalculation:OFF",
                    "-Xep:LongDoubleConversion:OFF",
                    "-Xep:UnicodeEscape:OFF",
                    "-Xep:JavaUtilDate:OFF",
                    "-Xep:UnrecognisedJavadocTag:OFF",
                    "-Xep:ObjectEqualsForPrimitives:OFF",
                    "-Xep:DoNotCallSuggester:OFF",
                    "-Xep:EqualsNull:OFF",
                    "-Xep:MalformedInlineTag:OFF",
                    "-Xep:MissingSuperCall:OFF",
                    "-Xep:ToStringReturnsNull:OFF",
                    "-Xep:ReturnValueIgnored:OFF",
                    "-Xep:MissingImplementsComparable:OFF",
                    "-Xep:EmptyTopLevelDeclaration:OFF",
                    "-Xep:InvalidThrowsLink:OFF",
                    "-Xep:StaticAssignmentOfThrowable:OFF",
                    "-Xep:DoNotClaimAnnotations:OFF",
                    "-Xep:AlreadyChecked:OFF",
                    "-Xep:StringSplitter:OFF",
                    "-Xep:NonApiType:OFF",
                    "-Xep:StringCaseLocaleUsage:OFF",
                    "-Xep:LabelledBreakTarget:OFF",
                    "-Xep:Finalize:OFF",
                    "-Xep:AddressSelection:OFF",
                    "-Xep:StringCharset:OFF",
                    "-Xep:EnumOrdinal:OFF",
                    "-Xep:ClassInitializationDeadlock:OFF",
                    "-Xep:VoidUsed:OFF",

                    // We allow inter library RestrictTo usage.
                    "-Xep:RestrictTo:OFF",

                    // Disable the following checks.
                    "-Xep:UnescapedEntity:OFF",
                    "-Xep:MissingSummary:OFF",
                    "-Xep:StaticAssignmentInConstructor:OFF",
                    "-Xep:InvalidLink:OFF",
                    "-Xep:InvalidInlineTag:OFF",
                    "-Xep:EmptyBlockTag:OFF",
                    "-Xep:EmptyCatch:OFF",
                    "-Xep:JdkObsolete:OFF",
                    "-Xep:PublicConstructorForAbstractClass:OFF",
                    "-Xep:MutablePublicArray:OFF",
                    "-Xep:NonCanonicalType:OFF",
                    "-Xep:ModifyCollectionInEnhancedForLoop:OFF",
                    "-Xep:InheritDoc:OFF",
                    "-Xep:InvalidParam:OFF",
                    "-Xep:InlineFormatString:OFF",
                    "-Xep:InvalidBlockTag:OFF",
                    "-Xep:ProtectedMembersInFinalClass:OFF",
                    "-Xep:SameNameButDifferent:OFF",
                    "-Xep:AnnotateFormatMethod:OFF",
                    "-Xep:ReturnFromVoid:OFF",
                    "-Xep:AlmostJavadoc:OFF",
                    "-Xep:InjectScopeAnnotationOnInterfaceOrAbstractClass:OFF",
                    "-Xep:InvalidThrows:OFF",

                    // Disable checks which are already enforced by lint.
                    "-Xep:PrivateConstructorForUtilityClass:OFF",

                    // Enforce the following checks.
                    "-Xep:JavaTimeDefaultTimeZone:ERROR",
                    "-Xep:ParameterNotNullable:ERROR",
                    "-Xep:MissingOverride:ERROR",
                    "-Xep:EqualsHashCode:ERROR",
                    "-Xep:NarrowingCompoundAssignment:ERROR",
                    "-Xep:ClassNewInstance:ERROR",
                    "-Xep:ClassCanBeStatic:ERROR",
                    "-Xep:SynchronizeOnNonFinalField:ERROR",
                    "-Xep:OperatorPrecedence:ERROR",
                    "-Xep:IntLongMath:ERROR",
                    "-Xep:MissingFail:ERROR",
                    "-Xep:JavaLangClash:ERROR",
                    "-Xep:TypeParameterUnusedInFormals:ERROR",
                    // "-Xep:StringSplitter:ERROR", // disabled with upgrade to 2.14.0
                    "-Xep:ReferenceEquality:ERROR",
                    "-Xep:AssertionFailureIgnored:ERROR",
                    "-Xep:UnnecessaryParentheses:ERROR",
                    "-Xep:EqualsGetClass:ERROR",
                    "-Xep:UnusedVariable:ERROR",
                    "-Xep:UnusedMethod:ERROR",
                    "-Xep:UndefinedEquals:ERROR",
                    "-Xep:ThreadLocalUsage:ERROR",
                    "-Xep:FutureReturnValueIgnored:ERROR",
                    "-Xep:ArgumentSelectionDefectChecker:ERROR",
                    "-Xep:HidingField:ERROR",
                    "-Xep:UnsynchronizedOverridesSynchronized:ERROR",
                    "-Xep:Finally:ERROR",
                    "-Xep:ThreadPriorityCheck:ERROR",
                    "-Xep:AutoValueFinalMethods:ERROR",
                    "-Xep:ImmutableEnumChecker:ERROR",
                    "-Xep:UnsafeReflectiveConstructionCast:ERROR",
                    "-Xep:LockNotBeforeTry:ERROR",
                    "-Xep:DoubleCheckedLocking:ERROR",
                    "-Xep:InconsistentCapitalization:ERROR",
                    "-Xep:ModifiedButNotUsed:ERROR",
                    "-Xep:AmbiguousMethodReference:ERROR",
                    "-Xep:EqualsIncompatibleType:ERROR",
                    "-Xep:ParameterName:ERROR",
                    "-Xep:RxReturnValueIgnored:ERROR",
                    "-Xep:BadImport:ERROR",
                    "-Xep:MissingCasesInEnumSwitch:ERROR",
                    "-Xep:ObjectToString:ERROR",
                    "-Xep:CatchAndPrintStackTrace:ERROR",
                    "-Xep:MixedMutabilityReturnType:ERROR",

                    // Enforce checks related to nullness annotation usage
                    "-Xep:NullablePrimitiveArray:ERROR",
                    "-Xep:MultipleNullnessAnnotations:ERROR",
                    "-Xep:NullablePrimitive:ERROR",
                    "-Xep:NullableVoid:ERROR",
                    "-Xep:NullableWildcard:ERROR",
                    "-Xep:NullableTypeParameter:ERROR",
                    "-Xep:NullableConstructor:ERROR",

                    // Nullaway
                    "-XepIgnoreUnknownCheckNames", // https://github.com/uber/NullAway/issues/25
                    "-Xep:NullAway:ERROR",
                    "-XepOpt:NullAway:AnnotatedPackages=android.arch,android.support,androidx"
                )
                .joinToString(" ")
        )
}

/**
 * Given a [JavaCompile] task, creates a task that runs the ErrorProne compiler with the same
 * settings, including any kotlin source provided by [jvmCompileInputs].
 *
 * Note: Since ErrorProne only understands Java files which may be dependent on Kotlin source, using
 * this method to register ErrorProne task causes it to be dependent on jvmJar task.
 *
 * @param jvmCompileInputs [JavaCompileInputs] that specifies jvm source including Kotlin sources.
 */
private fun Project.makeKmpErrorProneTask(
    compileTaskName: String,
    jvmJarTaskProvider: TaskProvider<Jar>,
    jvmCompileInputs: JavaCompileInputs
) {
    makeErrorProneTask(compileTaskName) { errorProneTask ->
        // ErrorProne doesn't understand Kotlin source, so first let kotlinCompile finish, then
        // take the resulting jar and add it to the classpath.
        val jvmJarTask = jvmJarTaskProvider.get()
        val jvmJarFileCollection = files(provider { jvmJarTask.archiveFile.get().asFile })
        errorProneTask.dependsOn(jvmJarTaskProvider.name)
        errorProneTask.classpath = jvmCompileInputs.dependencyClasspath.plus(jvmJarFileCollection)
        errorProneTask.source =
            jvmCompileInputs.sourcePaths
                // flatMap src dirs into src files so we can read the extensions.
                .asFileTree
                // ErrorProne normally skips non-java source, but we need to explicitly filter for
                // it
                // since non-empty list with no java source will throw an exception.
                .filter { it.extension.equals("java", ignoreCase = true) }
                .asFileTree
    }
}

/**
 * Given a [JavaCompile] task, creates a task that runs the ErrorProne compiler with the same
 * settings.
 *
 * @param onConfigure optional callback which lazily evaluates on task configuration. Use this to do
 *   any additional configuration such as overriding default settings.
 */
private fun Project.makeErrorProneTask(
    compileTaskName: String,
    onConfigure: (errorProneTask: JavaCompile) -> Unit = {}
) = afterEvaluate {
    val errorProneTaskProvider =
        maybeRegister<JavaCompile>(
            name = ERROR_PRONE_TASK,
            onConfigure = {
                val compileTask =
                    tasks.withType(JavaCompile::class.java).named(compileTaskName).get()
                it.classpath = compileTask.classpath
                it.source = compileTask.source
                it.destinationDirectory.set(layout.buildDirectory.dir("errorProne"))
                it.options.compilerArgs = compileTask.options.compilerArgs.toMutableList()
                it.options.annotationProcessorPath = compileTask.options.annotationProcessorPath
                it.options.bootstrapClasspath = compileTask.options.bootstrapClasspath
                it.sourceCompatibility = compileTask.sourceCompatibility
                it.targetCompatibility = compileTask.targetCompatibility
                it.configureWithErrorProne()
                it.dependsOn(compileTask.dependsOn)

                onConfigure(it)
            },
            onRegister = { errorProneProvider -> project.addToCheckTask(errorProneProvider) }
        )
    addToBuildOnServer(errorProneTaskProvider)
}
