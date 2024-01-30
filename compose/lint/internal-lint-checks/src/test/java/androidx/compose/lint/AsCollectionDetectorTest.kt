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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/* ktlint-disable max-line-length */
@RunWith(Parameterized::class)
class AsCollectionDetectorTest(
    val types: CollectionType
) : LintDetectorTest() {

    override fun getDetector(): Detector = AsCollectionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AsCollectionDetector.ISSUE)

    private val collectionTilde = "~".repeat(types.collection.length)

    @Test
    fun immutableAsImmutable() {
        lint().files(
            ScatterMapClass,
            ScatterSetClass,
            ObjectListClass,
            kotlin(
                """
                        package androidx.compose.lint

                        import androidx.collection.${types.immutable}

                        fun foo(collection: ${types.immutable}${types.params}): ${types.collection}${types.params} =
                            collection.as${types.collection}()
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:7: Error: Use method as${types.collection}() only for public API usage [AsCollectionCall]
                            collection.as${types.collection}()
                            ~~~~~~~~~~~~~$collectionTilde~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun mutableAsImmutable() {
        lint().files(
            ScatterMapClass,
            ScatterSetClass,
            ObjectListClass,
            kotlin(
                """
                        package androidx.compose.lint

                        import androidx.collection.Mutable${types.immutable}

                        fun foo(collection: Mutable${types.immutable}${types.params}): ${types.collection}${types.params} =
                            collection.as${types.collection}()
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:7: Error: Use method as${types.collection}() only for public API usage [AsCollectionCall]
                            collection.as${types.collection}()
                            ~~~~~~~~~~~~~$collectionTilde~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun mutableAsMutable() {
        lint().files(
            ScatterMapClass,
            ScatterSetClass,
            ObjectListClass,
            kotlin(
                """
                        package androidx.compose.lint

                        import androidx.collection.Mutable${types.immutable}

                        fun foo(collection: Mutable${types.immutable}${types.params}): Mutable${types.collection}${types.params} =
                            collection.asMutable${types.collection}()
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:7: Error: Use method asMutable${types.collection}() only for public API usage [AsCollectionCall]
                            collection.asMutable${types.collection}()
                            ~~~~~~~~~~~~~~~~~~~~$collectionTilde~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun nonCollectionAs() {
        lint().files(
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): ${types.collection}${types.params} =
                            WeirdCollection().as${types.collection}()

                        class WeirdCollection {
                            fun asList(): List<String>? = null
                            fun asSet(): Set<String>? = null
                            fun asMap(): Map<String, String>? = null
                        }
                        """
            )
        ).run().expectClean()
    }

    class CollectionType(
        val immutable: String,
        val collection: String,
        val params: String
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(
            CollectionType("ScatterMap", "Map", "<String, String>"),
            CollectionType("ScatterSet", "Set", "<String>"),
            CollectionType("ObjectList", "List", "<String>")
        )

        val ScatterMapClass = kotlin(
            """
            package androidx.collection
            sealed class ScatterMap<K, V> {
                fun asMap(): Map<K, V> = mapOf()
            }

            class MutableScatterMap<K, V> : ScatterMap<K, V>() {
                fun asMutableMap(): MutableMap<K, V> = mutableMapOf()
            }
            """.trimIndent()
        )

        val ScatterSetClass = kotlin(
            """
            package androidx.collection
            sealed class ScatterSet<E> {
                fun asSet(): Set<E> = setOf()
            }

            class MutableScatterSet<E> : ScatterSet<E>() {
                fun asMutableSet(): MutableSet<E> = mutableSetOf()
            }
            """.trimIndent()
        )

        val ObjectListClass = kotlin(
            """
            package androidx.collection
            sealed class ObjectList<E> {
                fun asList(): List<E> = listOf()
            }

            class MutableObjectList<E> : ObjectList<E>() {
                fun asMutableList(): MutableList<E> = mutableListOf()
            }
            """.trimIndent()
        )
    }
}
