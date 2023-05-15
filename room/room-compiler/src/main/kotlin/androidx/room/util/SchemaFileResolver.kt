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

import java.io.File
import java.nio.file.Path
import java.util.ServiceLoader

/**
 * A service provider interface to resolve a schema path to a file. An implementation of this
 * interfaces is discovered by Room via a [ServiceLoader].
 */
interface SchemaFileResolver {

    /**
     * Resolves the given path to a file. The path will be a either a sibling of Room's schema
     * location or the folder itself as provided via the annotation processor options
     * 'room.schemaLocation' or 'roomSchemaInput.
     */
    fun getFile(path: Path): File

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
            override fun getFile(path: Path) = path.toFile()
        }
    }
}