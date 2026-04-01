/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.processing.KSPLogger
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

/**
 * A service provider interface to resolve XML path to a file. An implementation of this interface
 * is discovered by AppFunctions via a [ServiceLoader].
 *
 * Used for resolving path to the XML file provided by
 * [androidx.appfunctions.compiler.AppFunctionCompilerOptions.appFunctionsXmlLocation]
 */
interface XmlFileResolver {

    /**
     * Returns an [OutputStream] for the specified file path.
     *
     * @param filePath the file path to write to
     */
    @Throws(IOException::class) fun getWriteStream(filePath: Path, logger: KSPLogger): OutputStream

    companion object {
        /**
         * Resolver to use for resolving the file location for the generated XML file.
         *
         * To support different file systems a build system can provide an implementation of this
         * interface.
         */
        val RESOLVER: XmlFileResolver by lazy {
            // Search is performed using the default ServiceLoader.load() class loader and the
            // interface's. This is because build tools will isolate annotation processor's
            // classpath and the default class loader (i.e. current thread's context class
            // loader) might miss a provided implementation.
            ServiceLoader.load(XmlFileResolver::class.java).firstOrNull()
                ?: ServiceLoader.load(
                        XmlFileResolver::class.java,
                        XmlFileResolver::class.java.classLoader,
                    )
                    .firstOrNull()
                ?: DEFAULT_RESOLVER
        }

        private val DEFAULT_RESOLVER =
            object : XmlFileResolver {
                override fun getWriteStream(filePath: Path, logger: KSPLogger): OutputStream {
                    val parent = filePath.parent
                    if (parent != null && !parent.exists()) {
                        parent.createDirectories()
                    }
                    logger.info("Writing XML file to ${filePath.toAbsolutePath()}")
                    return filePath.outputStream()
                }
            }
    }
}
