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

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.setupLanguageVersionSettings
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test
import java.net.URLClassLoader

class RunComposableTests : AbstractCodegenTest() {

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.setupLanguageVersionSettings(K2JVMCompilerArguments().apply {
            // enabling multiPlatform to use expect/actual declarations
            multiPlatform = true
        })
    }

    @Test // Bug report: https://github.com/JetBrains/compose-jb/issues/1407
    fun testDefaultValuesFromExpectComposableFunctions() = ensureSetup {
        runCompose(
            testFunBody = """
                ExpectComposable { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable("anotherValue") { value ->
                    results["anotherValue"] = value
                }
                results["returnDefaultValue"] = ExpectComposableWithReturn()
                results["returnAnotherValue"] = ExpectComposableWithReturn("returnAnotherValue")
            """.trimIndent(),
            files = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun ExpectComposable(
                        value: String = "defaultValue",
                        content: @Composable (v: String) -> Unit
                    )
                    @Composable
                    expect fun ExpectComposableWithReturn(
                        value: String = "returnDefaultValue"
                    ): String
                """.trimIndent(),
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(value)
                    }
                    @Composable
                    actual fun ExpectComposableWithReturn(
                        value: String
                    ): String = value
                """.trimIndent()
            )
        ) { results ->
            assertEquals("defaultValue", results["defaultValue"])
            assertEquals("anotherValue", results["anotherValue"])
            assertEquals("returnDefaultValue", results["returnDefaultValue"])
            assertEquals("returnAnotherValue", results["returnAnotherValue"])
        }
    }

    // This method was partially borrowed/copy-pasted from RobolectricComposeTester
    // where some of the code was commented out. Those commented out parts are needed here.
    private fun runCompose(
        @Language("kotlin")
        mainImports: String = "",
        @Language("kotlin")
        testFunBody: String,
        files: Map<String, String>, // name to source code
        accessResults: (results: HashMap<*, *>) -> Unit
    ) {
        val className = "TestFCS_${uniqueNumber++}"

        val allSources = files + ("Main.kt" to """
            import androidx.compose.runtime.*
            $mainImports

            class $className {
                val results = hashMapOf<String, Any>()

                @Composable
                fun test() {
                    $testFunBody
                }
            }

        """.trimIndent())

        val compiledClasses = classLoader(allSources)
        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val testMethod = instanceClass.getMethod(
            "test",
            *emptyArray(),
            Composer::class.java,
            Int::class.java
        )
        val getResultsMethod = instanceClass.getMethod("getResults")

        val setContentMethod = Composition::class.java.methods.first { it.name == "setContent" }
        setContentMethod.isAccessible = true

        val realComposable: (Composer, Int) -> Unit = { composer, _ ->
            testMethod.invoke(instanceOfClass, *emptyArray(), composer, 1)
        }

        val composition = Composition(UnitApplier(), createRecomposer())
        setContentMethod.invoke(composition, realComposable)

        val results = getResultsMethod.invoke(instanceOfClass) as HashMap<*, *>
        accessResults(results)
    }

    private class UnitApplier : Applier<Unit> {
        override val current: Unit
            get() = Unit

        override fun down(node: Unit) {}
        override fun up() {}
        override fun insertTopDown(index: Int, instance: Unit) {}
        override fun insertBottomUp(index: Int, instance: Unit) {}
        override fun remove(index: Int, count: Int) {}
        override fun move(from: Int, to: Int, count: Int) {}
        override fun clear() {}
    }

    private object SixtyFpsMonotonicFrameClock : MonotonicFrameClock {
        private const val fps = 60

        override suspend fun <R> withFrameNanos(
            onFrame: (Long) -> R
        ): R {
            delay(1000L / fps)
            return onFrame(System.nanoTime())
        }
    }

    private fun createRecomposer(): Recomposer {
        val mainScope = CoroutineScope(
            NonCancellable + Dispatchers.Main + SixtyFpsMonotonicFrameClock
        )

        return Recomposer(mainScope.coroutineContext).also {
            mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                it.runRecomposeAndApplyChanges()
            }
        }
    }
}