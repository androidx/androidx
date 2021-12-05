/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac.kotlin

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.javac.nullability
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runJavaProcessorTest
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

@RunWith(Parameterized::class)
class KotlinMetadataElementTest(
    private val preCompiled: Boolean
) {

    @Test
    fun constructorParameters() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Subject() {
                constructor(
                    nullableString: String?,
                    nonNullBoolean: Boolean,
                    nonNullInt: Int,
                    vararg nonNullVarArgs: Int ): this()
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { processingEnv ->
            val (testClassElement, metadataElement) = getMetadataElement(
                processingEnv,
                "Subject"
            )
            val constructors = testClassElement.getConstructors()
            val noArgConstructor = constructors.first {
                it.parameters.isEmpty()
            }
            metadataElement.getConstructorMetadata(noArgConstructor).let { constructor ->
                assertThat(constructor?.isPrimary()).isTrue()
                assertThat(constructor?.parameters).isEmpty()
            }
            val fourArgConstructor = constructors.first {
                it.parameters.size == 4
            }
            metadataElement.getConstructorMetadata(fourArgConstructor).let { constructor ->
                assertThat(constructor?.isPrimary()).isFalse()
                assertThat(
                    constructor?.parameters?.map {
                        it.name to it.isNullable()
                    }
                ).containsExactly(
                    "nullableString" to true,
                    "nonNullBoolean" to false,
                    "nonNullInt" to false,
                    "nonNullVarArgs" to false
                )
            }
            assertThat(constructors.size).isEqualTo(2)
        }
    }

    @Test
    fun getParameterNames() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Subject {
                fun functionWithParams(
                    nullableString: String?,
                    nonNullBoolean: Boolean,
                    nonNullInt: Int,
                    vararg nonNullVarArgs: Int ) {
                }
                suspend fun suspendFunctionWithParams(
                    nullableString: String?,
                    nullableBoolean: Boolean?,
                    nonNullInt: Int,
                    vararg nullableVarargs : Int?) {
                }
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { processingEnv ->
            val (testClassElement, metadataElement) = getMetadataElement(
                processingEnv,
                "Subject"
            )
            testClassElement.getDeclaredMethod("functionWithParams")
                .let { metadataElement.getFunctionMetadata(it) }
                .let { functionMetadata ->
                    assertThat(
                        functionMetadata?.parameters?.map {
                            it.name to it.isNullable()
                        }
                    ).containsExactly(
                        "nullableString" to true,
                        "nonNullBoolean" to false,
                        "nonNullInt" to false,
                        "nonNullVarArgs" to false
                    )
                    assertThat(functionMetadata?.returnType?.isNullable()).isFalse()
                }
            testClassElement.getDeclaredMethod("suspendFunctionWithParams")
                .let { metadataElement.getFunctionMetadata(it) }
                .let { functionMetadata ->
                    assertThat(
                        functionMetadata?.parameters?.map {
                            it.name to it.isNullable()
                        }
                    ).containsExactly(
                        "nullableString" to true,
                        "nullableBoolean" to true,
                        "nonNullInt" to false,
                        "nullableVarargs" to false // varargs itself is still not nullable
                    )
                    assertThat(functionMetadata?.returnType?.isNullable()).isFalse()
                }
        }
    }

    @Test
    fun findPrimaryConstructorSignature() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Subject(val constructorParam: String) {
                constructor() : this("anything")
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                "Subject"
            )
            assertThat(
                testClassElement.getConstructors().map {
                    val desc = it.descriptor()
                    desc to (desc == metadataElement.findPrimaryConstructorSignature())
                }
            ).containsExactly(
                "<init>(Ljava/lang/String;)V" to true,
                "<init>()V" to false
            )
        }
    }

    @Test
    fun isSuspendFunction() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Subject(val constructorParam: String) {
                constructor() : this("anything")
                fun emptyFunction() {}
                suspend fun suspendFunction() {
                }
                fun functionWithParams(param1: String) {
                }
                fun suspendFunctionWithParams(suspendParam1: String) {
                }
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                "Subject"
            )
            assertThat(
                testClassElement.getDeclaredMethods().map {
                    it.simpleName.toString() to metadataElement.getFunctionMetadata(it)?.isSuspend()
                }
            ).containsExactly(
                "emptyFunction" to false,
                "suspendFunction" to true,
                "functionWithParams" to false,
                "suspendFunctionWithParams" to false,
                "getConstructorParam" to false // synthetic getter for constructor property
            )
        }
    }

    @Test
    fun isObject() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class KotlinClass
            interface KotlinInterface
            object AnObject
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (_, objectTypeMetadata) = getMetadataElement(invocation, "AnObject")
            assertThat(objectTypeMetadata.isObject()).isTrue()
            val (_, classTypeMetadata) = getMetadataElement(invocation, "KotlinClass")
            assertThat(classTypeMetadata.isObject()).isFalse()
            val (_, interfaceMetadata) = getMetadataElement(invocation, "KotlinInterface")
            assertThat(interfaceMetadata.isObject()).isFalse()
        }
    }

    @Test
    fun methods_genericReturnTypeNullability() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            interface Subject {
                fun nonNullList() : List<Any>
                fun nullableList() : List<Any?>
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (testDaoElement, testDaoMetadata) = getMetadataElement(
                invocation,
                "Subject"
            )
            val nonNullListMethod = testDaoElement.getDeclaredMethod("nonNullList")
            val nullableList = testDaoElement.getDeclaredMethod("nullableList")
            val nonNullMetadata = testDaoMetadata.getFunctionMetadata(nonNullListMethod)
            val nullableMetadata = testDaoMetadata.getFunctionMetadata(nullableList)
            assertThat(
                nonNullMetadata?.returnType?.typeArguments?.first()?.isNullable()
            ).isFalse()
            assertThat(
                nullableMetadata?.returnType?.typeArguments?.first()?.isNullable()
            ).isTrue()
        }
    }

    @Test
    fun properties() {
        val src = Source.kotlin(
            "Properties.kt",
            """
            class Properties {
                val nonNull:String = ""
                val nullable:String? = null
                val nullableTypeArgument:List<String?> = emptyList()
                val nonNullTypeArgument:List<Int> = emptyList()
                val multipleTypeArguments:Map<String, Any?> = emptyMap()
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (_, testMetadata) = getMetadataElement(
                invocation,
                "Properties"
            )
            testMetadata.getPropertyMetadata("nonNull").let { property ->
                assertThat(property?.name).isEqualTo("nonNull")
                assertThat(property?.typeParameters).isEmpty()
                assertThat(property?.isNullable()).isFalse()
            }

            testMetadata.getPropertyMetadata("nullable").let { property ->
                assertThat(property?.name).isEqualTo("nullable")
                assertThat(property?.typeParameters).isEmpty()
                assertThat(property?.isNullable()).isTrue()
            }

            testMetadata.getPropertyMetadata("nullableTypeArgument").let { property ->
                assertThat(property?.name).isEqualTo("nullableTypeArgument")
                assertThat(property?.isNullable()).isFalse()
                assertThat(property?.typeParameters).hasSize(1)
                assertThat(property?.typeParameters?.single()?.isNullable()).isTrue()
            }

            testMetadata.getPropertyMetadata("nonNullTypeArgument").let { property ->
                assertThat(property?.name).isEqualTo("nonNullTypeArgument")
                assertThat(property?.isNullable()).isFalse()
                assertThat(property?.typeParameters).hasSize(1)
                assertThat(property?.typeParameters?.single()?.isNullable()).isFalse()
            }

            testMetadata.getPropertyMetadata("multipleTypeArguments").let { property ->
                assertThat(property?.name).isEqualTo("multipleTypeArguments")
                assertThat(property?.isNullable()).isFalse()
                assertThat(property?.typeParameters).hasSize(2)
                assertThat(property?.typeParameters?.get(0)?.isNullable()).isFalse()
                assertThat(property?.typeParameters?.get(1)?.isNullable()).isTrue()
            }
        }
    }

    @Test
    fun accessors() {
        val src = Source.kotlin(
            "Kotlin.kt",
            """
            @JvmInline
            value class ValueClass(val value: String)
            class Subject {
                val immutableProperty: String = ""
                var mutableProperty: String = ""
                var isProperty: String? = ""
                var customSetter: String=  ""
                    get(): String = ""
                    set(myValue) { field = myValue }
                var privateSetter: String = ""
                    private set
                internal var internalProp: String? = ""
                internal var isInternalProp2: String = ""
                // these won't show up in KAPT stubs since they don't have a valid JVM name but
                // we are still testing them for consistency as they'll show up in metadata
                var valueProp: ValueClass? = null
                // these won't show up in KAPT stubs since they don't have a valid JVM name but
                // we are still testing them for consistency as they'll show up in metadata
                internal var internalValueProp: ValueClass = ValueClass("?")
            }
        """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (element, metadata) = getMetadataElement(
                invocation,
                "Subject"
            )

            fun assertSetter(
                kmFunction: KmFunction?,
                name: String,
                jvmName: String,
                paramNullable: Boolean
            ) {
                checkNotNull(kmFunction)
                assertWithMessage(kmFunction.toString()).apply {
                    that(kmFunction.name).isEqualTo(name)
                    that(kmFunction.jvmName).isEqualTo(jvmName)
                    // void is non null
                    that(kmFunction.returnType.nullability).isEqualTo(XNullability.NONNULL)
                    that(kmFunction.parameters).hasSize(1)
                    that(
                        kmFunction.parameters.single().isNullable()
                    ).isEqualTo(paramNullable)
                }
            }

            fun assertSetter(
                metadata: KotlinMetadataElement,
                method: ExecutableElement,
                name: String,
                jvmName: String,
                paramNullable: Boolean
            ) {
                val kmFunction = metadata.getFunctionMetadata(method)
                assertSetter(
                    kmFunction = kmFunction,
                    name = name,
                    jvmName = jvmName,
                    paramNullable = paramNullable
                )
                val paramName = kmFunction!!.parameters.single().name
                val javacElementName = method.parameters.single().simpleName.toString()
                assertWithMessage(
                    kmFunction.toString()
                ).that(
                    paramName
                ).isEqualTo(
                    javacElementName.sanitizeAsJavaParameterName(0)
                )
            }

            fun assertGetter(
                kmFunction: KmFunction?,
                name: String,
                jvmName: String,
                returnsNullable: Boolean
            ) {
                checkNotNull(kmFunction)
                assertWithMessage(kmFunction.toString()).apply {
                    that(kmFunction.name).isEqualTo(name)
                    that(kmFunction.jvmName).isEqualTo(jvmName)
                    that(kmFunction.returnType.isNullable()).isEqualTo(returnsNullable)
                    that(kmFunction.parameters).isEmpty()
                }
            }
            assertGetter(
                kmFunction = metadata.getFunctionMetadata(
                    element.getDeclaredMethod("getImmutableProperty")
                ),
                name = "getImmutableProperty",
                jvmName = "getImmutableProperty",
                returnsNullable = false
            )
            assertGetter(
                kmFunction = metadata.getFunctionMetadata(
                    element.getDeclaredMethod("getMutableProperty")
                ),
                name = "getMutableProperty",
                jvmName = "getMutableProperty",
                returnsNullable = false
            )
            assertSetter(
                metadata = metadata,
                method = element.getDeclaredMethod("setMutableProperty"),
                name = "setMutableProperty",
                jvmName = "setMutableProperty",
                paramNullable = false
            )
            assertSetter(
                metadata = metadata,
                method = element.getDeclaredMethod("setProperty"),
                name = "setProperty",
                jvmName = "setProperty",
                paramNullable = true
            )
            assertGetter(
                kmFunction = metadata.getFunctionMetadata(
                    element.getDeclaredMethod("isProperty")
                ),
                name = "isProperty",
                jvmName = "isProperty",
                returnsNullable = true
            )
            assertGetter(
                kmFunction = metadata.getFunctionMetadata(
                    element.getDeclaredMethod("getInternalProp\$main")
                ),
                name = "getInternalProp",
                jvmName = "getInternalProp\$main",
                returnsNullable = true
            )
            assertSetter(
                metadata = metadata,
                method = element.getDeclaredMethod("setInternalProp\$main"),
                name = "setInternalProp",
                jvmName = "setInternalProp\$main",
                paramNullable = true
            )
            assertGetter(
                kmFunction = metadata.getFunctionMetadata(
                    element.getDeclaredMethod("isInternalProp2\$main")
                ),
                name = "isInternalProp2",
                jvmName = "isInternalProp2\$main",
                returnsNullable = false
            )
            assertSetter(
                metadata = metadata,
                method = element.getDeclaredMethod("setInternalProp2\$main"),
                name = "setInternalProp2",
                jvmName = "setInternalProp2\$main",
                paramNullable = false
            )
            // read custom setter name properly
            metadata.getFunctionMetadata(
                element.getDeclaredMethod("setCustomSetter")
            ).let { kmFunction ->
                checkNotNull(kmFunction)
                assertThat(
                    kmFunction.parameters.single().name
                ).isEqualTo("myValue")
            }
            // tests value class properties. They won't show up in KAPT stubs since they don't have
            // valid java source names but we still validate them here for consistency. Maybe one
            // day we'll change Javac element to include these if we support Kotlin codegen in KAPT
            metadata.getPropertyMetadata("valueProp").let { valueProp ->
                assertGetter(
                    kmFunction = valueProp?.getter,
                    name = "getValueProp",
                    jvmName = "getValueProp-4LjoGxk",
                    returnsNullable = true
                )
                assertSetter(
                    kmFunction = valueProp?.setter,
                    name = "setValueProp",
                    jvmName = "setValueProp-d8IPsTA",
                    paramNullable = true
                )
            }
            metadata.getPropertyMetadata("internalValueProp").let { valueProp ->
                assertGetter(
                    kmFunction = valueProp?.getter,
                    name = "getInternalValueProp",
                    jvmName = "getInternalValueProp-NMFyIOA\$main",
                    returnsNullable = false
                )
                assertSetter(
                    kmFunction = valueProp?.setter,
                    name = "setInternalValueProp",
                    jvmName = "setInternalValueProp-FVOWAsA\$main",
                    paramNullable = false
                )
            }
        }
    }

    @Test
    fun internalMethodName() {
        val src = Source.kotlin(
            "Kotlin.kt",
            """
            class Subject {
                internal fun internalFun() {}
                fun normalFun() {}
                // there is no test case for functions receiving/returning value classes because
                // they are not visible through KAPT
            }
        """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (element, metadata) = getMetadataElement(
                invocation,
                "Subject"
            )
            metadata.getFunctionMetadata(
                element.getDeclaredMethod("internalFun\$main")
            ).let { functionMetadata ->
                assertThat(
                    functionMetadata?.jvmName
                ).isEqualTo("internalFun\$main")
                assertThat(
                    functionMetadata?.name
                ).isEqualTo("internalFun")
            }

            metadata.getFunctionMetadata(
                element.getDeclaredMethod("normalFun")
            ).let { functionMetadata ->
                assertThat(
                    functionMetadata?.jvmName
                ).isEqualTo("normalFun")
                assertThat(
                    functionMetadata?.name
                ).isEqualTo("normalFun")
            }
        }
    }

    @Test
    fun genericParameterNullability() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Subject(
                nonNullGenericWithNonNullParam : List<Int>,
                nonNullGenericWithNullableParam : List<Int?>,
                nullableGenericWithNullableParam : List<Int?>?,
                nullableGenericWithNonNullParam : List<Int>?
            ){
                fun foo(
                    nonNullGenericWithNonNullParam : List<Int>,
                    nonNullGenericWithNullableParam : List<Int?>,
                    nullableGenericWithNullableParam : List<Int?>?,
                    nullableGenericWithNonNullParam : List<Int>?
                ) {}
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (testDaoElement, testDaoMetadata) = getMetadataElement(
                invocation,
                "Subject"
            )
            fun assertParams(params: List<KmValueParameter>?) {
                assertThat(
                    params?.map {
                        Triple(
                            it.name,
                            it.isNullable(),
                            it.type.typeArguments.first().isNullable()
                        )
                    }
                ).containsExactly(
                    Triple("nonNullGenericWithNonNullParam", false, false),
                    Triple("nonNullGenericWithNullableParam", false, true),
                    Triple("nullableGenericWithNullableParam", true, true),
                    Triple("nullableGenericWithNonNullParam", true, false)
                )
            }
            assertParams(
                testDaoMetadata.getConstructorMetadata(
                    testDaoElement.getConstructors().single()
                )?.parameters
            )
            assertParams(
                testDaoMetadata.getFunctionMetadata(
                    testDaoElement.getDeclaredMethod("foo")
                )?.parameters
            )
        }
    }

    @Test
    fun kotlinArrayKmType() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            interface Subject {
                val nullableArrayWithNonNullComponent : Array<Int>?
                val nullableArrayWithNullableComponent : Array<Int?>?
                val nonNullArrayWithNonNullComponent : Array<Int>
                val nonNullArrayWithNullableComponent : Array<Int?>
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (_, metadata) = getMetadataElement(
                invocation,
                "Subject"
            )
            val propertyNames = listOf(
                "nullableArrayWithNonNullComponent",
                "nullableArrayWithNullableComponent",
                "nonNullArrayWithNonNullComponent",
                "nonNullArrayWithNullableComponent"
            )
            assertThat(
                propertyNames
                    .mapNotNull(metadata::getPropertyMetadata)
                    .map {
                        Triple(it.name, it.isNullable(), it.typeParameters.single().isNullable())
                    }
            ).containsExactly(
                Triple("nullableArrayWithNonNullComponent", true, false),
                Triple("nullableArrayWithNullableComponent", true, true),
                Triple("nonNullArrayWithNonNullComponent", false, false),
                Triple("nonNullArrayWithNullableComponent", false, true)
            )
        }
    }

    @Test
    fun kotlinClassMetadataToKmType() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            class Simple {
            }
            class TwoArgGeneric<Arg1, Arg2>{
            }
            // KotlinMetadata does not seem to be giving any type information for Arg2:Any?
            //  probably because it is equal to default. On the other hand though, Arg1 : Any
            //  return false for isNullable :/.
            class WithUpperBounds<Arg1 : Any, Arg2 : List<Any>?>{
            }

            abstract class WithSuperType : Map<String, Int?> {}
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (_, simple) = getMetadataElement(invocation, "Simple")
            assertThat(simple.kmType.isNullable()).isFalse()
            assertThat(simple.kmType.typeArguments).isEmpty()

            val (_, twoArgGeneric) = getMetadataElement(invocation, "TwoArgGeneric")
            assertThat(twoArgGeneric.kmType.isNullable()).isFalse()
            assertThat(twoArgGeneric.kmType.typeArguments).hasSize(2)
            assertThat(twoArgGeneric.kmType.typeArguments[0].isNullable()).isFalse()
            assertThat(twoArgGeneric.kmType.typeArguments[1].isNullable()).isFalse()

            val (_, withUpperBounds) = getMetadataElement(invocation, "WithUpperBounds")
            assertThat(withUpperBounds.kmType.typeArguments).hasSize(2)
            assertThat(withUpperBounds.kmType.typeArguments[0].extendsBound?.isNullable()).isFalse()
            assertThat(withUpperBounds.kmType.typeArguments[1].extendsBound?.isNullable()).isTrue()

            val (_, withSuperType) = getMetadataElement(invocation, "WithSuperType")
            assertThat(withSuperType.superType?.typeArguments?.get(0)?.isNullable()).isFalse()
            assertThat(withSuperType.superType?.typeArguments?.get(1)?.isNullable()).isTrue()
        }
    }

    @Test
    fun erasure() {
        val src = Source.kotlin(
            "Subject.kt",
            """
            object Subject {
                val simple : String = "foo"
                val nullableGeneric: List<Int>? = TODO()
                val nonNullGeneric: List<Int> = TODO()
            }
            """.trimIndent()
        )
        simpleRun(listOf(src)) { invocation ->
            val (_, subject) = getMetadataElement(invocation, "Subject")
            subject.getPropertyMetadata("simple")!!.type.erasure().let {
                assertThat(it.isNullable()).isFalse()
                assertThat(it.typeArguments).isEmpty()
            }
            subject.getPropertyMetadata("nullableGeneric")!!.type.erasure().let {
                assertThat(it.isNullable()).isTrue()
                assertThat(it.typeArguments).isEmpty()
            }
            subject.getPropertyMetadata("nonNullGeneric")!!.type.erasure().let {
                assertThat(it.isNullable()).isFalse()
                assertThat(it.typeArguments).isEmpty()
            }
        }
    }

    @Test
    fun withoutKotlinInClasspath() {
        if (preCompiled) {
            throw AssumptionViolatedException("this test doesn't care for precompiled code")
        }
        val libSource = Source.kotlin(
            "lib.kt",
            """
            class KotlinClass {
                val b: String = TODO()
                val a: String = TODO()
                val c: String = TODO()
                val isB:String = TODO()
                val isA:String = TODO()
                val isC:String = TODO()
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runJavaProcessorTest(
            sources = emptyList(),
            classpath = classpath
        ) { invocation ->
            val (_, metadata) =
                getMetadataElement(
                    (invocation.processingEnv as JavacProcessingEnv).delegate,
                    "KotlinClass"
                )
            assertThat(metadata).isNotNull()
        }
    }

    private fun TypeElement.getDeclaredMethods() = ElementFilter.methodsIn(enclosedElements)

    private fun TypeElement.getDeclaredMethod(name: String) = getDeclaredMethods().first {
        it.simpleName.toString() == name
    }

    private fun TypeElement.getConstructors() = ElementFilter.constructorsIn(enclosedElements)

    @Suppress("NAME_SHADOWING") // intentional
    private fun simpleRun(
        sources: List<Source> = emptyList(),
        handler: (ProcessingEnvironment) -> Unit
    ) {
        val (sources, classpath) = if (preCompiled) {
            emptyList<Source>() to compileFiles(sources)
        } else {
            sources to emptyList()
        }
        runKaptTest(sources = sources, classpath = classpath) {
            val processingEnv = it.processingEnv
            if (processingEnv !is JavacProcessingEnv) {
                throw AssumptionViolatedException("This test only works for java/kapt compilation")
            }
            handler(processingEnv.delegate)
        }
    }

    private fun getMetadataElement(processingEnv: ProcessingEnvironment, qName: String) =
        processingEnv.elementUtils.getTypeElement(qName).let {
            it to KotlinMetadataElement.createFor(it)!!
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompiled_{0}")
        fun params() = arrayOf(false, true)
    }
}