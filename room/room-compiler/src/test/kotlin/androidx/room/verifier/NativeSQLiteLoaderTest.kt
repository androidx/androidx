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

package androidx.room.verifier

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

@RunWith(JUnit4::class)
class NativeSQLiteLoaderTest {

    @Test
    fun multipleClassLoader() {
        // Get current classpath
        val stringUrls = System.getProperty("java.class.path")!!
            .split(System.getProperty("path.separator")!!).toTypedArray()
        // Find classes under test.
        val targetDirName = "room-compiler/build/classes/kotlin/main"
        val classesDirPath = stringUrls.first { it.contains(targetDirName) }
        // Create a JAR file out the classes and resources
        val jarFile = File.createTempFile("jar-for-test-", ".jar")
        createJar(classesDirPath, jarFile)
        val jarUrl = URL("file://${jarFile.absolutePath}")
        // Find Kotlin stdlibs (will need them to load class under test)
        val kotlinStdbLibUrls = stringUrls
            .filter { it.contains("kotlin-stdlib") && it.endsWith(".jar") }
            .map { URL("file://$it") }
        // Also find sqlite-jdbc since it is a hard dep of NativeSQLiteLoader
        val sqliteJdbcJarUrl = stringUrls
            .filter { it.contains("sqlite-jdbc") && it.endsWith(".jar") }
            .map { URL("file://$it") }
        // Spawn a few threads and have them all in parallel load the native lib
        val completedThreads = AtomicInteger(0)
        val numOfThreads = 8
        val pool = Executors.newFixedThreadPool(numOfThreads)
        val loadedClasses = arrayOfNulls<Class<*>>(numOfThreads)
        for (i in 1..numOfThreads) {
            pool.execute {
                try {
                    Thread.sleep((i * 10).toLong())
                    // Create an isolated class loader, it should load *different* instances
                    // of NativeSQLiteLoader.class
                    val classLoader = URLClassLoader(
                        (kotlinStdbLibUrls + sqliteJdbcJarUrl + jarUrl).toTypedArray(),
                        ClassLoader.getSystemClassLoader().parent
                    )
                    val clazz =
                        classLoader.loadClass("androidx.room.verifier.NativeSQLiteLoader")
                    clazz.getDeclaredMethod("load").invoke(null)
                    classLoader.close()
                    loadedClasses[i - 1] = clazz
                } catch (e: Throwable) {
                    e.printStackTrace()
                    fail(e.message)
                }
                completedThreads.incrementAndGet()
            }
        }
        // Verify all threads completed
        pool.shutdown()
        pool.awaitTermination(3, TimeUnit.SECONDS)
        assertThat(completedThreads.get()).isEqualTo(numOfThreads)
        // Verify all loaded classes are different from each other
        loadedClasses.forEachIndexed { i, clazz1 ->
            loadedClasses.forEachIndexed { j, clazz2 ->
                if (i == j) {
                    assertThat(clazz1).isEqualTo(clazz2)
                } else {
                    assertThat(clazz1).isNotEqualTo(clazz2)
                }
            }
        }
    }

    private fun createJar(inputDir: String, outputFile: File) {
        JarOutputStream(outputFile.outputStream()).use {
            addJarEntry(File(inputDir), inputDir, it)
        }
    }

    private fun addJarEntry(source: File, changeDir: String, target: JarOutputStream) {
        if (source.isDirectory) {
            var name = source.path.replace("\\", "/")
            if (name.isNotEmpty()) {
                if (!name.endsWith("/")) {
                    name += "/"
                }
                val entry = JarEntry(name.substring(changeDir.length + 1))
                entry.time = source.lastModified()
                target.putNextEntry(entry)
                target.closeEntry()
            }
            source.listFiles()!!.forEach { nestedFile ->
                addJarEntry(nestedFile, changeDir, target)
            }
        } else if (source.isFile) {
            val entry = JarEntry(
                source.path.replace("\\", "/").substring(changeDir.length + 1)
            )
            entry.time = source.lastModified()
            target.putNextEntry(entry)
            source.inputStream().use { inputStream ->
                inputStream.copyTo(target)
            }
            target.closeEntry()
        }
    }
}