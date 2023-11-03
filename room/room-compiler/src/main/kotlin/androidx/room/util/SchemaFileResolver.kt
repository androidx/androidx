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

package androidx.room.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * A service provider interface to resolve a schema path to a file. An implementation of this
 * interfaces is discovered by Room via a [ServiceLoader].
 */
interface SchemaFileResolver {

    /**
     * Requests an input stream to be opened for a given path. The function implementation might
     * return `null` if such path does not exist.
     *
     * The path will be a either a sibling of Room's schema location or the folder itself as
     * provided via the annotation processor options 'room.schemaLocation' or 'roomSchemaInput.
     */
    @Throws(IOException::class)
    fun readPath(path: Path): InputStream?

    /**
     * Requests an input stream to be opened for a given path.
     *
     * The path will be a either a sibling of Room's schema location or the folder itself as
     * provided via the annotation processor options 'room.schemaLocation' or 'roomSchemaOutput.
     */
    @Throws(IOException::class)
    fun writePath(path: Path): OutputStream

    companion object {
        val RESOLVER: SchemaFileResolver by lazy {
            // Search is performed using the default ServiceLoader.load() class loader and the
            // interface's. This is because build tools will isolate annotation processor's
            // classpath and the default class loader (i.e. current thread's context class
            // loader) might miss a provided implementation.
            ServiceLoader.load(
                SchemaFileResolver::class.java,
            ).firstOrNull() ?: ServiceLoader.load(
                SchemaFileResolver::class.java,
                SchemaFileResolver::class.java.classLoader
            ).firstOrNull() ?: DEFAULT_RESOLVER
        }

        private val DEFAULT_RESOLVER = object : SchemaFileResolver {

            override fun readPath(path: Path): InputStream? {
                return if (path.exists()) path.inputStream() else null
            }

            override fun writePath(path: Path): OutputStream {
                val parent = path.parent
                if (parent != null && !parent.exists()) {
                    parent.createDirectories()
                }
                return path.outputStream()
            }
        }
    }
}
