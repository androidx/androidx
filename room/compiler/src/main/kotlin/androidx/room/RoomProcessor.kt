/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room

import androidx.room.processor.Context
import androidx.room.processor.DatabaseProcessor
import androidx.room.processor.MissingTypeException
import androidx.room.processor.ProcessorErrors
import androidx.room.util.SimpleJavaVersion
import androidx.room.vo.DaoMethod
import androidx.room.vo.Warning
import androidx.room.writer.DaoWriter
import androidx.room.writer.DatabaseWriter
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element

/**
 * Annotation processor option to tell Gradle that Room is an isolating annotation processor.
 */
private const val ISOLATING_ANNOTATION_PROCESSORS_INDICATOR =
    "org.gradle.annotation.processing.isolating"

/**
 * The annotation processor for Room.
 */
class RoomProcessor : BasicAnnotationProcessor() {

    /** Helper variable to avoid reporting the warning twice. */
    private var jdkVersionHasBugReported = false

    override fun initSteps(): MutableIterable<ProcessingStep>? {
        return mutableListOf(DatabaseProcessingStep(processingEnv))
    }

    override fun getSupportedOptions(): MutableSet<String> {
        val supportedOptions = Context.ARG_OPTIONS.toMutableSet()

        if (Context.BooleanProcessorOptions.INCREMENTAL.getValue(processingEnv)) {
            if (methodParametersVisibleInClassFiles()) {
                // Room can be incremental
                supportedOptions.add(ISOLATING_ANNOTATION_PROCESSORS_INDICATOR)
            } else {
                if (!jdkVersionHasBugReported) {
                    Context(processingEnv).logger.w(
                        Warning.JDK_VERSION_HAS_BUG, ProcessorErrors.JDK_VERSION_HAS_BUG
                    )
                    jdkVersionHasBugReported = true
                }
            }
        }

        return supportedOptions
    }

    /**
     * Returns `true` if the method parameters in class files can be accessed by Room.
     *
     * Context: Room requires access to the real parameter names of constructors (see
     * PojoProcessor.getParamNames). Room uses the ExecutableElement.getParemters() API on the
     * constructor element to do this.
     *
     * When Room is not yet incremental, the above API is working as expected. However, if we make
     * Room incremental, during an incremental compile Gradle may want to pass class files instead
     * source files to annotation processors (to avoid recompiling the source files that haven't
     * changed). Due to JDK bug https://bugs.openjdk.java.net/browse/JDK-8007720, the class files
     * may lose the real parameter names of constructors, which would break Room.
     *
     * The above JDK bug was fixed in JDK 11. The fix was also cherry-picked back into the
     * embedded JDK that was shipped with Android Studio 3.5+.
     *
     * Therefore, for Room to be incremental, we need to check whether the JDK being used has the
     * fix: Either it is JDK 11+ or it is an embedded JDK that has the cherry-picked fix (version
     * 1.8.0_202-release-1483-b39-5509098 or higher).
     */
    private fun methodParametersVisibleInClassFiles(): Boolean {
        val currentJavaVersion = SimpleJavaVersion.getCurrentVersion() ?: return false

        if (currentJavaVersion >= SimpleJavaVersion.VERSION_11_0_0) {
            return true
        }

        val isEmbeddedJdk =
            System.getProperty("java.vendor")?.contains("JetBrains", ignoreCase = true)
                ?: false
        // We are interested in 3 ranges of Android Studio (AS) versions:
        //    1. AS 3.5.0-alpha09 and lower use JDK 1.8.0_152 or lower.
        //    2. AS 3.5.0-alpha10 up to 3.5.0-beta01 use JDK 1.8.0_202-release-1483-b39-5396753.
        //    3. AS 3.5.0-beta02 and higher use JDK 1.8.0_202-release-1483-b39-5509098 or higher,
        //       which have the cherry-picked JDK fix.
        // Therefore, if the JDK version is 1.8.0_202, we need to filter out those in range #2.
        return if (isEmbeddedJdk && (currentJavaVersion > SimpleJavaVersion.VERSION_1_8_0_202)) {
            true
        } else if (isEmbeddedJdk && (currentJavaVersion == SimpleJavaVersion.VERSION_1_8_0_202)) {
            System.getProperty("java.runtime.version")
                ?.let { it != "1.8.0_202-release-1483-b39-5396753" }
                ?: false
        } else {
            false
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    class DatabaseProcessingStep(val processingEnv: ProcessingEnvironment) : ProcessingStep {
        override fun process(
            elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
        ): MutableSet<Element> {
            val context = Context(processingEnv)
            val rejectedElements = mutableSetOf<Element>()
            val databases = elementsByAnnotation[Database::class.java]
                    ?.mapNotNull {
                        try {
                            DatabaseProcessor(context, MoreElements.asType(it)).process()
                        } catch (ex: MissingTypeException) {
                            // Abandon processing this database class since it needed a type element
                            // that is missing. It is possible that the type will be generated by a
                            // further annotation processing round, so we will try again by adding
                            // this class element to a deferred set.
                            rejectedElements.add(it)
                            null
                        }
                    }
            val daoMethodsMap = databases?.flatMap { db -> db.daoMethods.map { it to db } }?.toMap()
            daoMethodsMap?.let {
                prepareDaosForWriting(databases, it.keys.toList())
                it.forEach { (daoMethod, db) ->
                    DaoWriter(daoMethod.dao, db.element, context.processingEnv)
                        .write(context.processingEnv)
                }
            }

            databases?.forEach { db ->
                DatabaseWriter(db).write(context.processingEnv)
                if (db.exportSchema) {
                    val schemaOutFolder = context.schemaOutFolder
                    if (schemaOutFolder == null) {
                        context.logger.w(Warning.MISSING_SCHEMA_LOCATION, db.element,
                                ProcessorErrors.MISSING_SCHEMA_EXPORT_DIRECTORY)
                    } else {
                        if (!schemaOutFolder.exists()) {
                            schemaOutFolder.mkdirs()
                        }
                        val qName = db.element.qualifiedName.toString()
                        val dbSchemaFolder = File(schemaOutFolder, qName)
                        if (!dbSchemaFolder.exists()) {
                            dbSchemaFolder.mkdirs()
                        }
                        db.exportSchema(File(dbSchemaFolder, "${db.version}.json"))
                    }
                }
            }
            return rejectedElements
        }

        override fun annotations(): MutableSet<out Class<out Annotation>> {
            return mutableSetOf(Database::class.java)
        }

        /**
         * Traverses all dao methods and assigns them suffix if they are used in multiple databases.
         */
        private fun prepareDaosForWriting(
            databases: List<androidx.room.vo.Database>,
            daoMethods: List<DaoMethod>
        ) {
            daoMethods.groupBy { it.dao.typeName }
                    // if used only in 1 database, nothing to do.
                    .filter { entry -> entry.value.size > 1 }
                    .forEach { entry ->
                        entry.value.groupBy { daoMethod ->
                            // first suffix guess: Database's simple name
                            val db = databases.first { db -> db.daoMethods.contains(daoMethod) }
                            db.typeName.simpleName()
                        }.forEach { (dbName, methods) ->
                            if (methods.size == 1) {
                                // good, db names do not clash, use db name as suffix
                                methods.first().dao.setSuffix(dbName)
                            } else {
                                // ok looks like a dao is used in 2 different databases both of
                                // which have the same name. enumerate.
                                methods.forEachIndexed { index, method ->
                                    method.dao.setSuffix("${dbName}_$index")
                                }
                            }
                        }
                    }
        }
    }
}
