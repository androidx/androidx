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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiCoreDynamicEvaluatorTest {

    private lateinit var evaluator: A2uiCoreDynamicEvaluator
    private lateinit var basePath: A2uiDataPath
    private lateinit var mockAddFunction: A2uiFunction
    private lateinit var mockCatalog: A2uiCoreCatalog
    private lateinit var mockContext: A2uiExecutionContext

    @Before
    fun setUp() {
        basePath = A2uiDataPath(BASE_PATH)

        mockContext =
            object : A2uiExecutionContext {
                override fun evaluatePayload(dataPath: A2uiDataPath, payload: Any?): Any? {
                    return null
                }

                override fun resolveValue(path: A2uiDataPath): Any? {
                    return "$RESOLVED_PATH_PREFIX ${path.path}"
                }

                override fun executeFunction(name: String, args: Map<String, Any>): Any? {
                    val catalogFunction = mockCatalog.functions[name]
                    if (catalogFunction != null) {
                        return catalogFunction.execute(args, this)
                    }
                    return null
                }

                override fun <T : Any> getOrCreateFunctionScopedCache(
                    functionDefinition: A2uiFunctionDefinition,
                    factory: () -> T,
                ): T {
                    return factory()
                }
            }

        mockAddFunction =
            object : A2uiFunction {
                override val definition: A2uiFunctionDefinition =
                    object : A2uiFunctionDefinition {
                        override val name = FUNC_ADD
                        override val description: String = ""
                        override val argumentSchema: A2uiSchema = A2uiObjectSchema.INSTANCE
                        override val returnType: A2uiFunctionReturnType =
                            A2uiFunctionReturnType.NUMBER
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any? {
                    val a = args[ARG_A] as? Int ?: return null
                    val b = args[ARG_B] as? Int ?: return null
                    return a + b
                }
            }

        val mockConcatFunction =
            object : A2uiFunction {
                override val definition: A2uiFunctionDefinition =
                    object : A2uiFunctionDefinition {
                        override val name = FUNC_CONCAT
                        override val description: String = ""
                        override val argumentSchema: A2uiSchema = A2uiObjectSchema.INSTANCE
                        override val returnType: A2uiFunctionReturnType =
                            A2uiFunctionReturnType.STRING
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any? {
                    return "${args[ARG_A]}-${args[ARG_B]}"
                }
            }

        mockCatalog =
            object : A2uiCoreCatalog {
                override val id: String = "test_catalog"
                override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                    A2uiCoreComponentDefinitionCollection()
                override val functions: A2uiFunctionCollection =
                    A2uiFunctionCollection(listOf(mockAddFunction, mockConcatFunction))
                override val themeSchema: A2uiSchema? = null
            }

        evaluator = A2uiCoreDynamicEvaluatorImpl
    }

    @Test
    fun evaluate_literalPrimitiveValue_returnsLiteralString() {
        val payload = "hello"
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(payload)
    }

    @Test
    fun evaluate_literalListValue_returnsLiteralList() {
        val listPayload = listOf(1, 2, 3)
        val result = evaluator.evaluate(basePath, listPayload, mockContext)
        assertThat(result).isEqualTo(listPayload)
    }

    @Test
    fun evaluate_literalMapValue_returnsLiteralMap() {
        val payload = mapOf("other" to "value")
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(payload)
    }

    @Test
    fun evaluate_pathPayloadWithRelativePath_resolvesCorrectly() {
        val payload = mapOf(KEY_PATH to RELATIVE_PATH)
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo("$RESOLVED_PATH_PREFIX ${BASE_PATH}/${RELATIVE_PATH}")
    }

    @Test
    fun evaluate_pathPayloadWithAbsolutePath_resolvesCorrectly() {
        val payload = mapOf(KEY_PATH to ABSOLUTE_PATH)
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo("$RESOLVED_PATH_PREFIX $ABSOLUTE_PATH")
    }

    @Test
    fun evaluate_pathPayloadWithMissingValue_returnsUnresolved() {
        val missingResolverContext =
            object : A2uiExecutionContext {
                override fun evaluatePayload(dataPath: A2uiDataPath, payload: Any?): Any? = null

                override fun resolveValue(path: A2uiDataPath): Any? = null

                override fun executeFunction(name: String, args: Map<String, Any>): Any? = null

                override fun <T : Any> getOrCreateFunctionScopedCache(
                    functionDefinition: A2uiFunctionDefinition,
                    factory: () -> T,
                ): T {
                    return factory()
                }
            }
        val payload = mapOf(KEY_PATH to RELATIVE_PATH)

        val result = evaluator.evaluate(basePath, payload, missingResolverContext)
        assertThat(result).isNull()
    }

    @Test
    fun evaluate_callPayloadWithUnregisteredFunction_returnsUnresolved() {
        val payload = mapOf(KEY_CALL to "someFunction")

        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isNull()
    }

    @Test
    fun evaluate_callPayloadWithRegisteredFunction_executesFunction() {
        val payload = mapOf(KEY_CALL to FUNC_ADD, KEY_ARGS to mapOf(ARG_A to 5, ARG_B to 15))
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(20)
    }

    @Test
    fun evaluate_callPayloadWithA2uiFunctionCallSchemaKeys_executesFunction() {
        val payload =
            mapOf(
                KEY_CALL to FUNC_ADD,
                KEY_ARGS to mapOf(ARG_A to 5, ARG_B to 15),
                KEY_CALLABLE_FROM to "clientOnly",
                KEY_RETURN_TYPE to "number",
            )
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(20)
    }

    @Test
    fun evaluate_callPayloadWithUnrecognizedKey_returnsLiteralMap() {
        val payload =
            mapOf(
                KEY_CALL to FUNC_ADD,
                KEY_ARGS to mapOf(ARG_A to 5, ARG_B to 15),
                "unrecognized" to "value",
            )
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(payload)
    }

    @Test
    fun evaluate_callPayloadWithInvalidArgsType_returnsLiteralMap() {
        val payload = mapOf(KEY_CALL to FUNC_ADD, KEY_ARGS to "not_a_map")
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo(payload)
    }

    @Test
    fun evaluate_callPayloadWithDynamicArguments_resolvesArguments() {
        val payload =
            mapOf(
                KEY_CALL to FUNC_CONCAT,
                KEY_ARGS to
                    mapOf(ARG_A to mapOf(KEY_PATH to "/arg1"), ARG_B to mapOf(KEY_PATH to "/arg2")),
            )
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isEqualTo("$RESOLVED_PATH_PREFIX /arg1-$RESOLVED_PATH_PREFIX /arg2")
    }

    @Test
    fun evaluate_callPayloadWithDeepNesting_evaluateCorrectly() {
        var nestedPayload: Any = 0
        repeat(100_000) {
            nestedPayload =
                mapOf(KEY_CALL to FUNC_ADD, KEY_ARGS to mapOf(ARG_A to nestedPayload, ARG_B to 1))
        }

        val result = evaluator.evaluate(basePath, nestedPayload, mockContext)
        assertThat(result).isEqualTo(100_000)
    }

    @Test
    fun evaluate_nestedMapPayload_resolvesCorrectly() {
        val payload = mapOf("user" to mapOf("name" to mapOf(KEY_PATH to "/username")))
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result)
            .isEqualTo(mapOf("user" to mapOf("name" to "$RESOLVED_PATH_PREFIX /username")))
    }

    @Test
    fun evaluate_nestedListPayload_resolvesCorrectly() {
        val payload = listOf(mapOf(KEY_PATH to "/item1"), mapOf(KEY_PATH to "/item2"))
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result)
            .isEqualTo(listOf("$RESOLVED_PATH_PREFIX /item1", "$RESOLVED_PATH_PREFIX /item2"))
    }

    @Test
    fun evaluate_mixedNestedPayload_resolvesCorrectly() {
        val payload =
            mapOf(
                "user" to
                    mapOf(
                        "scores" to
                            listOf(
                                mapOf(KEY_PATH to "/score1"),
                                mapOf(
                                    KEY_CALL to FUNC_ADD,
                                    KEY_ARGS to mapOf(ARG_A to 10, ARG_B to 5),
                                ),
                            )
                    )
            )
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result)
            .isEqualTo(
                mapOf("user" to mapOf("scores" to listOf("$RESOLVED_PATH_PREFIX /score1", 15)))
            )
    }

    @Test
    fun evaluate_staticTree_preservesReferentialIdentity() {
        val payload =
            mapOf(
                "user" to mapOf("name" to "Alice", "roles" to listOf("admin", "user")),
                "settings" to mapOf("theme" to "dark"),
            )
        val result = evaluator.evaluate(basePath, payload, mockContext)
        assertThat(result).isSameInstanceAs(payload)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun evaluate_mixedTree_preservesReferentialIdentityForStaticBranches() {
        // Only "name" is dynamic here. "roles" and "settings" are static.
        val roles = listOf("admin", "user")
        val settings = mapOf("theme" to "dark")
        val user = mapOf("name" to mapOf(KEY_PATH to "/username"), "roles" to roles)
        val payload = mapOf("user" to user, "settings" to settings)

        val result = evaluator.evaluate(basePath, payload, mockContext) as Map<*, *>

        // The root map and "user" map should be new instances because "name" changed referentially
        assertThat(result).isNotSameInstanceAs(payload)
        val resultUser = result["user"] as Map<*, *>
        assertThat(resultUser).isNotSameInstanceAs(user)

        // Sibling/descendant static subtrees must preserve their original reference perfectly!
        assertThat(resultUser["roles"]).isSameInstanceAs(roles)
        assertThat(result["settings"]).isSameInstanceAs(settings)
    }

    private companion object {
        private const val RESOLVED_PATH_PREFIX = "resolved"
        private const val BASE_PATH = "/base"
        private const val RELATIVE_PATH = "relative"

        private const val ABSOLUTE_PATH = "/absolute"

        private const val FUNC_ADD = "add"
        private const val FUNC_CONCAT = "concat"
        private const val ARG_A = "a"
        private const val ARG_B = "b"

        private const val KEY_PATH = "path"
        private const val KEY_CALL = "call"
        private const val KEY_ARGS = "args"
        private const val KEY_CALLABLE_FROM = "callableFrom"
        private const val KEY_RETURN_TYPE = "returnType"
    }
}
