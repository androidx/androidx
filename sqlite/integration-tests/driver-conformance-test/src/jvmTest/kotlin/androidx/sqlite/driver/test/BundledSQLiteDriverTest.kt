/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqlite.driver.test

import androidx.kruth.assertThrows
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.Locale
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString
import kotlin.test.Test
import org.junit.AssumptionViolatedException

class BundledSQLiteDriverTest : BaseBundledConformanceTest() {

    override val driverType = TestDriverType.BUNDLED

    override fun getDatabaseFileName(): String {
        return createTempFile("test.db").also { it.toFile().deleteOnExit() }.pathString
    }

    override fun getDriver(): BundledSQLiteDriver {
        return BundledSQLiteDriver()
    }

    override fun getTestExtensionPath(): String {
        return LoadableExtension.getExtensionFileName()
    }

    @Test
    fun configurableLibraryPath() {
        val osName = System.getProperty("os.name").lowercase(Locale.US)
        val osArch = System.getProperty("os.arch").lowercase(Locale.US)
        if (!osName.contains("linux") || !osArch.contains("64")) {
            throw AssumptionViolatedException("This test is only supported on 64-bit Linux.")
        }

        // Use a temp folder and extract the native library to it, we'll configure the driver
        // to load the JNI from it instead of from the JAR.
        val tempFolder = Files.createTempDirectory("libTestDir")
        val tempFile = tempFolder.resolve("libsqliteJni.so")
        BundledSQLiteDriver::class
            .java
            .classLoader
            .getResourceAsStream("natives/linux_x64/libsqliteJni.so")
            .use { Files.copy(it!!, tempFile) }

        System.setProperty("androidx.sqlite.driver.bundled.path", tempFolder.pathString)
        try {
            val driver = BundledSQLiteDriver()
            val connection = driver.open(":memory:")
            connection.close()
        } finally {
            System.clearProperty("androidx.sqlite.driver.bundled.path")
        }
    }

    @Test
    fun configurableLibraryBadPath() {
        val tempFolder = Files.createTempDirectory("libTestDir")
        System.setProperty("androidx.sqlite.driver.bundled.path", tempFolder.pathString)
        try {
            // Create a separate class loader so that the actual runtime class loader is not
            // affected by the initialization error.
            val classpath =
                System.getProperty("java.class.path").split(File.pathSeparator).map {
                    URI("file://$it").toURL()
                }
            val tempClassLoader = URLClassLoader(classpath.toTypedArray(), null)
            assertThrows<ExceptionInInitializerError> {
                    // Attempt and fail to load the class that loads the native library due to the
                    // configured path not having the file.
                    Class.forName(
                        "androidx.sqlite.driver.bundled.BundledSQLiteDriver\$NativeLibraryObject",
                        /* initialize = */ true,
                        tempClassLoader,
                    )
                }
                .hasCauseThat()
                .hasMessageThat()
                .apply {
                    contains("Cannot find a suitable SQLite binary")
                    contains("(androidx.sqlite.driver.bundled.path = ${tempFolder.pathString})")
                }
        } finally {
            System.clearProperty("androidx.sqlite.driver.bundled.path")
        }
    }
}
