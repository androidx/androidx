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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.saveable.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
/** Test for [RememberSaveableDetector]. */
class RememberSaveableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RememberSaveableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberSaveableDetector.RememberSaveableSaverParameter)

    private val rememberSaveableStub: TestFile =
        bytecodeStub(
            filename = "RememberSaveable.kt",
            filepath = "androidx/compose/runtime/saveable",
            checksum = 0x7556f44f,
            """
        package androidx.compose.runtime.saveable

        import androidx.compose.runtime.*

        @Composable
        fun <T : Any> rememberSaveable(
            vararg inputs: Any?,
            saver: Saver<T, out Any> = autoSaver(),
            key: String? = null,
            init: () -> T
        ): T = init()

        @Composable
        fun <T> rememberSaveable(
            vararg inputs: Any?,
            stateSaver: Saver<T, out Any>,
            key: String? = null,
            init: () -> MutableState<T>
        ): MutableState<T> = rememberSaveable(
            *inputs,
            saver = mutableStateSaver(stateSaver),
            key = key,
            init = init
        )

        interface Saver<Original, Saveable : Any>

        @Suppress("UNCHECKED_CAST")
        private fun <T> autoSaver(): Saver<T, Any> =
            (Any() as Saver<T, Any>)

        @Suppress("UNCHECKED_CAST")
        private fun <T> mutableStateSaver(inner: Saver<T, out Any>) =
            Any() as Saver<MutableState<T>, MutableState<Any?>>
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7hMudSxKVYrzixLDUxKSdVSCgoNTc1
        Nym1KBgqAtTIy8VSklpcIsTqlp/vXaLEoMUAALqJaBaNAAAA
        """,
            """
        androidx/compose/runtime/saveable/RememberSaveableKt.class:
        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
        Kg8AAA==
        """,
            """
        androidx/compose/runtime/saveable/Saver.class:
        H4sIAAAAAAAA/41PTU/CQBB92yKUilrED/gFxh4sEhMTNSZeTGowJDTxwmmB
        lSzQrekuhGN/lwfTsz/KOBW5qAc3mZm3b95k3rx/vL4BuECL4YSrcZrI8SoY
        JfFLokWQLpSRsQg0Xwo+nIsgIpBWwBgebnqpnEjF51fdKV/yYM7VJOgNp2Jk
        rqNv/R+t298Ug/eTq6DEUO/OEjOXKngUho+54aS04qVNflmRqkUCA5sRv5LF
        r01ofM7g51nNtZqWazkUXp45z80880tOnnnMdxzmWb7VtjsUxUSH4bT7z+vJ
        hLM5neDmVFY4afRFLOKhSDfs2cwwVCM5UdwsUhK5UbJIR+JeFhOt/nrDk9SS
        xHdKJYYbmShdJlfYwvrZOKJsUT3+qodoUr2kfWXSVAawQzghqiFcbBNELcQO
        dgdgGnvwBihp1DX2NRoaB5+QkX6G8gEAAA==
        """
        )

    @Test
    fun saverPassedToVarargs() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import androidx.compose.runtime.saveable.*

                class Foo
                object FooSaver : Saver<Any, Any>
                class FooSaver2 : Saver<Any, Any>
                val fooSaver3 = object : Saver<Any, Any> {}
                val fooSaver4 = FooSaver2()

                @Composable
                fun Test() {
                    val foo = rememberSaveable(FooSaver) { Foo() }
                    val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
                    val foo2 = rememberSaveable(FooSaver2()) { Foo() }
                    val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
                    val foo3 = rememberSaveable(fooSaver3) { Foo() }
                    val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
                    val foo4 = rememberSaveable(fooSaver4) { Foo() }
                    val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
                }
            """
                ),
                rememberSaveableStub,
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .run()
            .expect(
                """
src/test/Foo.kt:15: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo = rememberSaveable(FooSaver) { Foo() }
                                               ~~~~~~~~
src/test/Foo.kt:16: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
                                                           ~~~~~~~~
src/test/Foo.kt:17: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo2 = rememberSaveable(FooSaver2()) { Foo() }
                                                ~~~~~~~~~~~
src/test/Foo.kt:18: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~~~
src/test/Foo.kt:19: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo3 = rememberSaveable(fooSaver3) { Foo() }
                                                ~~~~~~~~~
src/test/Foo.kt:20: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~
src/test/Foo.kt:21: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo4 = rememberSaveable(fooSaver4) { Foo() }
                                                ~~~~~~~~~
src/test/Foo.kt:22: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~
8 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/Foo.kt line 15: Change to `saver = FooSaver`:
@@ -15 +15
-                     val foo = rememberSaveable(FooSaver) { Foo() }
+                     val foo = rememberSaveable(saver = FooSaver) { Foo() }
Fix for src/test/Foo.kt line 16: Change to `stateSaver = FooSaver`:
@@ -16 +16
-                     val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
+                     val mutableStateFoo = rememberSaveable(stateSaver = FooSaver) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 17: Change to `saver = FooSaver2()`:
@@ -17 +17
-                     val foo2 = rememberSaveable(FooSaver2()) { Foo() }
+                     val foo2 = rememberSaveable(saver = FooSaver2()) { Foo() }
Fix for src/test/Foo.kt line 18: Change to `stateSaver = FooSaver2()`:
@@ -18 +18
-                     val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
+                     val mutableStateFoo2 = rememberSaveable(stateSaver = FooSaver2()) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 19: Change to `saver = fooSaver3`:
@@ -19 +19
-                     val foo3 = rememberSaveable(fooSaver3) { Foo() }
+                     val foo3 = rememberSaveable(saver = fooSaver3) { Foo() }
Fix for src/test/Foo.kt line 20: Change to `stateSaver = fooSaver3`:
@@ -20 +20
-                     val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
+                     val mutableStateFoo3 = rememberSaveable(stateSaver = fooSaver3) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 21: Change to `saver = fooSaver4`:
@@ -21 +21
-                     val foo4 = rememberSaveable(fooSaver4) { Foo() }
+                     val foo4 = rememberSaveable(saver = fooSaver4) { Foo() }
Fix for src/test/Foo.kt line 22: Change to `stateSaver = fooSaver4`:
@@ -22 +22
-                     val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
+                     val mutableStateFoo4 = rememberSaveable(stateSaver = fooSaver4) { mutableStateOf(Foo()) }
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import androidx.compose.runtime.saveable.*

                class Foo
                object FooSaver : Saver<Any, Any>
                class FooSaver2 : Saver<Any, Any>
                val fooSaver3 = object : Saver<Any, Any> {}
                val fooSaver4 = FooSaver2()

                @Composable
                fun Test() {
                    val foo = rememberSaveable(saver = FooSaver) { Foo() }
                    val mutableStateFoo = rememberSaveable(stateSaver = FooSaver) {
                        mutableStateOf(Foo())
                    }
                    val foo2 = rememberSaveable(saver = FooSaver2()) { Foo() }
                    val mutableStateFoo2 = rememberSaveable(stateSaver = FooSaver2()) {
                        mutableStateOf(Foo())
                    }
                    val foo3 = rememberSaveable(saver = fooSaver3) { Foo() }
                    val mutableStateFoo3 = rememberSaveable(stateSaver = fooSaver3) {
                        mutableStateOf(Foo())
                    }
                    val foo4 = rememberSaveable(saver = fooSaver4) { Foo() }
                    val mutableStateFoo4 = rememberSaveable(stateSaver = fooSaver4) {
                        mutableStateOf(Foo())
                    }

                    val fooVarargs = rememberSaveable(Any(), FooSaver, Any()) { Foo() }
                    val mutableStateFooVarargs = rememberSaveable(Any(), FooSaver, Any()) {
                        mutableStateOf(Foo())
                    }
                }
            """
                ),
                rememberSaveableStub,
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .run()
            .expectClean()
    }
}
