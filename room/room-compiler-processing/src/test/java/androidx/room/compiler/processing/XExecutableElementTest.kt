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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.CONTINUATION_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.UNIT_CLASS_NAME
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.io.IOException

@RunWith(JUnit4::class)
class XExecutableElementTest {
    @Test
    fun basic() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                    public Baz(String param1) {}
                    private void foo() {}
                    public String bar(String[] param1) {
                        return "";
                    }
                }
                    """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getDeclaredMethodByJvmName("foo").let { method ->
                assertThat(method.isJavaDefault()).isFalse()
                assertThat(method.isVarArgs()).isFalse()
                assertThat(method.isOverrideableIgnoringContainer()).isFalse()
                assertThat(method.parameters).isEmpty()
                val returnType = method.returnType
                // check both as in KSP, it will show up as Unit
                assertThat(returnType.isVoid() || returnType.isKotlinUnit()).isTrue()
                assertThat(returnType.defaultValue()).isEqualTo("null")
            }
            element.getDeclaredMethodByJvmName("bar").let { method ->
                assertThat(method.isOverrideableIgnoringContainer()).isTrue()
                assertThat(method.parameters).hasSize(1)
                method.getParameter("param1").let { param ->
                    val paramType = param.type
                    check(paramType.isArray())
                    assertThat(paramType.componentType.typeName)
                        .isEqualTo(String::class.typeName())
                    assertThat(param.enclosingMethodElement).isEqualTo(method)
                }
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
            }
            element.getConstructors().single().let { ctor ->
                assertThat(ctor.parameters).hasSize(1)
                assertThat(ctor.parameters.single().enclosingMethodElement).isEqualTo(ctor)
            }
        }
    }

    @Test
    fun isVarArgs() {
        val subject = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            interface Baz {
                void method(String... inputs);
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.getMethodByJvmName("method").isVarArgs()).isTrue()
        }
    }

    @Test
    fun isVarArgs_kotlin() {
        val subject = Source.kotlin(
            "Subject.kt",
            """
            interface Subject {
                fun method(vararg inputs: String)
                suspend fun suspendMethod(vararg inputs: String);
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val element = it.processingEnv.requireTypeElement("Subject")
            assertThat(element.getMethodByJvmName("method").isVarArgs()).isTrue()
            assertThat(element.getMethodByJvmName("suspendMethod").isVarArgs()).isFalse()
        }
    }

    @Test
    fun kotlinDefaultImpl_src() {
        kotlinDefaultImpl(preCompiled = false)
    }

    @Test
    fun kotlinDefaultImpl_lib() {
        kotlinDefaultImpl(preCompiled = true)
    }

    private fun kotlinDefaultImpl(preCompiled: Boolean) {
        val subject = Source.kotlin(
            "Baz.kt",
            """
            package foo.bar

            interface Base {
                fun noDefault()
                fun withDefault(): Int {
                    return 3
                }
                fun nameMatch()
                fun nameMatch(param:Int) {}
                fun withDefaultWithParams(param1:Int, param2:String) {}
                fun withDefaultWithTypeArgs(param1: List<String>): String {
                    return param1.first()
                }
                private fun privateWithDefault(): String {
                    return ""
                }
            }

            interface Sub : Base
            """.trimIndent()
        )
        val (sources, classpath) = if (preCompiled) {
            emptyList<Source>() to compileFiles(listOf(subject))
        } else {
            listOf(subject) to emptyList<File>()
        }
        runProcessorTest(
            sources = sources,
            classpath = classpath
        ) { invocation ->
            listOf("Base", "Sub").forEach { className ->
                val element = invocation.processingEnv.requireTypeElement("foo.bar.$className")
                element.getMethodByJvmName("noDefault").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isFalse()
                }
                element.getMethodByJvmName("withDefault").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                element.getAllMethods().first {
                    it.jvmName == "nameMatch" && it.parameters.isEmpty()
                }.let { nameMatchWithoutDefault ->
                    assertThat(nameMatchWithoutDefault.hasKotlinDefaultImpl()).isFalse()
                }

                element.getAllMethods().first {
                    it.jvmName == "nameMatch" && it.parameters.size == 1
                }.let { nameMatchWithoutDefault ->
                    assertThat(nameMatchWithoutDefault.hasKotlinDefaultImpl()).isTrue()
                }

                element.getMethodByJvmName("withDefaultWithParams").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }

                element.getMethodByJvmName("withDefaultWithTypeArgs").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                // private functions in interfaces don't appear in kapt stubs
                if (invocation.isKsp && className == "Base") {
                    element.getMethodByJvmName("privateWithDefault").let { method ->
                        assertThat(method.hasKotlinDefaultImpl()).isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun kotlinDefaultImpl_typeParams_src() {
        kotlinDefaultImpl_typeParams(preCompiled = false)
    }

    @Test
    fun kotlinDefaultImpl_typeParams_lib() {
        kotlinDefaultImpl_typeParams(preCompiled = true)
    }

    private fun kotlinDefaultImpl_typeParams(preCompiled: Boolean) {
        val subject = Source.kotlin(
            "Baz.kt",
            """
            package foo.bar

            interface Base<T1, T2> {
                fun noDefault(t : T1)
                fun withDefault_noArg(): Int {
                    return 3
                }
                fun nameMatch()
                fun nameMatch(param:T1) {}
                fun withDefaultWithParams(param1:T1, param2:T2) {}
                fun withDefaultWithTypeArgs(param1: List<String>): String {
                    return param1.first()
                }
                private fun privateWithDefault(): String {
                    return ""
                }
            }

            interface Sub : Base<Int, String>

            interface Base2<T1, T2, in T3, out T4, T5 : Number> {

                fun withDefaultWithInProjectionType(param1: T3) {}

                fun withDefaultWithOutProjectionType(): T4? {
                    return null
                }

                fun withDefaultWithSubtypeArg(param: T5) { }
            }

            interface Sub2 : Base2<Int, String, Number, Number, Long>

            """.trimIndent()
        )
        val (sources, classpath) = if (preCompiled) {
            emptyList<Source>() to compileFiles(listOf(subject))
        } else {
            listOf(subject) to emptyList<File>()
        }
        runProcessorTest(
            sources = sources,
            classpath = classpath
        ) { invocation ->
            listOf("Base", "Sub").forEach { className ->
                val element = invocation.processingEnv.requireTypeElement("foo.bar.$className")
                element.getMethodByJvmName("noDefault").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isFalse()
                }
                element.getMethodByJvmName("withDefault_noArg").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                element.getAllMethods().first {
                    it.jvmName == "nameMatch" && it.parameters.isEmpty()
                }.let { nameMatchWithoutDefault ->
                    assertThat(nameMatchWithoutDefault.hasKotlinDefaultImpl()).isFalse()
                }

                element.getAllMethods().first {
                    it.jvmName == "nameMatch" && it.parameters.size == 1
                }.let { nameMatchWithoutDefault ->
                    assertThat(nameMatchWithoutDefault.hasKotlinDefaultImpl()).isTrue()
                }

                element.getMethodByJvmName("withDefaultWithParams").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }

                element.getMethodByJvmName("withDefaultWithTypeArgs").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                // private functions in interfaces don't appear in kapt stubs
                if (invocation.isKsp && className == "Base") {
                    element.getMethodByJvmName("privateWithDefault").let { method ->
                        assertThat(method.hasKotlinDefaultImpl()).isFalse()
                    }
                }
            }

            listOf("Base2", "Sub2").forEach { className ->
                val element = invocation.processingEnv.requireTypeElement("foo.bar.$className")
                element.getMethodByJvmName("withDefaultWithInProjectionType").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                element.getMethodByJvmName("withDefaultWithOutProjectionType").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
                element.getMethodByJvmName("withDefaultWithSubtypeArg").let { method ->
                    assertThat(method.hasKotlinDefaultImpl()).isTrue()
                }
            }
        }
    }

    @Test
    fun suspendMethod() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Subject {
                suspend fun noArg():Unit = TODO()
                suspend fun intReturn(): Int = TODO()
                suspend fun twoParams(param1:String, param2:Int): Pair<String, Int> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getMethodByJvmName("noArg").let { method ->
                assertThat(method.parameters).hasSize(1)
                assertThat(method.isSuspendFunction()).isTrue()
                assertThat(method.returnType.typeName).isEqualTo(TypeName.OBJECT)
                assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                method.executableType.parameterTypes.last().let { cont ->
                    assertThat(cont.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            CONTINUATION_CLASS_NAME,
                            WildcardTypeName.supertypeOf(UNIT_CLASS_NAME)
                        )
                    )
                    assertThat(cont.nullability).isEqualTo(XNullability.NONNULL)
                }
            }
            subject.getMethodByJvmName("intReturn").let { method ->
                assertThat(method.parameters).hasSize(1)
                method.parameters.last().let { cont ->
                    assertThat(cont.type.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            CONTINUATION_CLASS_NAME,
                            WildcardTypeName.supertypeOf(Integer::class.java)
                        )
                    )
                    assertThat(cont.enclosingMethodElement).isEqualTo(method)
                }
                assertThat(method.isSuspendFunction()).isTrue()
                assertThat(method.returnType.typeName).isEqualTo(TypeName.OBJECT)
                method.executableType.parameterTypes.last().let { cont ->
                    assertThat(cont.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            CONTINUATION_CLASS_NAME,
                            WildcardTypeName.supertypeOf(Integer::class.java)
                        )
                    )
                }
            }
            subject.getMethodByJvmName("twoParams").let { method ->
                assertThat(method.parameters).hasSize(3)
                assertThat(method.parameters[0].type.typeName).isEqualTo(
                    String::class.typeName()
                )
                assertThat(method.parameters[1].type.typeName).isEqualTo(
                    TypeName.INT
                )
                assertThat(method.isSuspendFunction()).isTrue()
                assertThat(method.returnType.typeName).isEqualTo(TypeName.OBJECT)
                method.executableType.parameterTypes.last().let { cont ->
                    assertThat(cont.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            CONTINUATION_CLASS_NAME,
                            WildcardTypeName.supertypeOf(
                                ParameterizedTypeName.get(
                                    Pair::class.className(),
                                    String::class.typeName(),
                                    Integer::class.typeName()
                                )
                            )
                        )
                    )
                }
            }
        }
    }

    @Test
    fun kotlinProperties() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            data class MyDataClass(val x:String, var y:String, private val z:String) {
                val prop1: String = ""
                var prop2: String = ""
                var prop3: String = TODO()
                    private set
                    get() = TODO()
                private val prop4:String = ""
                protected var prop5:String = ""
                var prop6: String
                    get // this cannot be protected, https://youtrack.jetbrains.com/issue/KT-3110
                    protected set
                protected var prop7: String
                    private set
                internal var prop8: String
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val klass = invocation.processingEnv.requireTypeElement("MyDataClass")
            val methodNames = klass.getAllMethods().map {
                it.jvmName
            }.toList()
            assertThat(methodNames).containsNoneIn(
                listOf(
                    "setX", "setProp1", "setProp3", "setZ", "setProp4", "getProp4", "setProp7"
                )
            )
            listOf("getX", "getProp1", "getProp2", "getProp3", "getProp5", "getProp6",
                "getProp8\$main").forEach {
                klass.getMethodByJvmName(it).let { method ->
                    assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                    assertThat(method.parameters).isEmpty()
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NONNULL)
                }
            }
            listOf("setY", "setProp2", "setProp8\$main").forEach {
                klass.getMethodByJvmName(it).let { method ->
                    assertThat(method.returnType.typeName).isEqualTo(TypeName.VOID)
                    assertThat(method.parameters.first().type.typeName).isEqualTo(
                        String::class.typeName()
                    )
                    assertThat(method.isPublic()).isTrue()
                    assertThat(method.parameters.first().type.nullability).isEqualTo(
                        XNullability.NONNULL
                    )
                }
            }
            listOf("getProp5", "getProp7").forEach {
                klass.getMethodByJvmName(it).let { method ->
                    assertThat(method.isProtected()).isTrue()
                    assertThat(method.isPublic()).isFalse()
                    assertThat(method.isPrivate()).isFalse()
                }
            }
            listOf("setProp5", "setProp6").forEach {
                klass.getMethodByJvmName(it).let { method ->
                    assertThat(method.isProtected()).isTrue()
                    assertThat(method.isPublic()).isFalse()
                    assertThat(method.isPrivate()).isFalse()
                }
            }
        }
    }

    @Test
    fun parametersAsMemberOf() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            open class Base<T> {
                fun foo(t:T, nullableT:T?): List<T?> = TODO()
            }
            class Subject : Base<String>()
            class NullableSubject: Base<String?>()
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(source)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("Base")
            val subject = invocation.processingEnv.requireType("Subject")
            val nullableSubject = invocation.processingEnv.requireType("NullableSubject")
            val method = base.getMethodByJvmName("foo")
            method.getParameter("t").let { param ->
                param.asMemberOf(subject).let {
                    assertThat(it.typeName).isEqualTo(String::class.typeName())
                    assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                }
                param.asMemberOf(nullableSubject).let {
                    assertThat(it.typeName).isEqualTo(String::class.typeName())
                    if (invocation.isKsp) {
                        // kapt implementation is unable to read this properly
                        assertThat(it.nullability).isEqualTo(XNullability.NULLABLE)
                    }
                }
            }
            method.getParameter("nullableT").let { param ->
                param.asMemberOf(subject).let {
                    assertThat(it.typeName).isEqualTo(String::class.typeName())
                    assertThat(it.nullability).isEqualTo(XNullability.NULLABLE)
                }
                param.asMemberOf(nullableSubject).let {
                    assertThat(it.typeName).isEqualTo(String::class.typeName())
                    assertThat(it.nullability).isEqualTo(XNullability.NULLABLE)
                }
            }
        }
    }

    @Test
    fun kotlinPropertyOverrides() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface {
                val x:Int
                var y:Int
            }
            class MyImpl : MyInterface {
                override var x: Int = 1
                override var y: Int = 1
            }
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "JavaImpl",
            """
            class JavaImpl implements MyInterface {
                public int getX() {
                    return 1;
                }
                public int getY() {
                    return 1;
                }
                public void setY(int value) {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src, javaSrc)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("MyInterface")
            val impl = invocation.processingEnv.requireTypeElement("MyImpl")
            val javaImpl = invocation.processingEnv.requireTypeElement("JavaImpl")

            fun overrides(
                owner: XTypeElement,
                ownerMethodName: String,
                base: XTypeElement,
                baseMethodName: String = ownerMethodName
            ): Boolean {
                val overrider = owner.getMethodByJvmName(ownerMethodName)
                val overridden = base.getMethodByJvmName(baseMethodName)
                return overrider.overrides(
                    overridden, owner
                )
            }
            listOf(impl, javaImpl).forEach { subject ->
                listOf("getY", "getX", "setY").forEach { methodName ->
                    assertWithMessage("${subject.className}:$methodName").that(
                        overrides(
                            owner = subject,
                            ownerMethodName = methodName,
                            base = base
                        )
                    ).isTrue()
                }

                assertWithMessage(subject.className.canonicalName()).that(
                    overrides(
                        owner = subject,
                        ownerMethodName = "getY",
                        base = base,
                        baseMethodName = "getX"
                    )
                ).isFalse()

                assertWithMessage(subject.className.canonicalName()).that(
                    overrides(
                        owner = subject,
                        ownerMethodName = "getY",
                        base = subject,
                        baseMethodName = "getX"
                    )
                ).isFalse()

                assertWithMessage(subject.className.canonicalName()).that(
                    overrides(
                        owner = base,
                        ownerMethodName = "getX",
                        base = subject,
                        baseMethodName = "getX"
                    )
                ).isFalse()

                assertWithMessage(subject.className.canonicalName()).that(
                    overrides(
                        owner = subject,
                        ownerMethodName = "setY",
                        base = base,
                        baseMethodName = "getY"
                    )
                ).isFalse()

                assertWithMessage(subject.className.canonicalName()).that(
                    overrides(
                        owner = subject,
                        ownerMethodName = "setY",
                        base = subject,
                        baseMethodName = "setY"
                    )
                ).isFalse()
            }
        }
    }

    @Test
    fun isAbstract() {
        val javaInterface = Source.java(
            "JavaInterface",
            """
            interface JavaInterface {
                void interfaceMethod();
            }
            """.trimIndent()
        )
        val javaAbstractClass = Source.java(
            "JavaAbstractClass",
            """
            abstract class JavaAbstractClass {
                abstract void abstractMethod();
                void nonAbstractMethod() {}
            }
            """.trimIndent()
        )
        val kotlinSource = Source.kotlin(
            "kotlin.kt",
            """
            interface KotlinInterface {
                fun interfaceMethod(): Unit
            }
            abstract class KotlinAbstractClass {
                abstract fun abstractMethod(): Unit
                fun nonAbstractMethod() {}
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaInterface, javaAbstractClass, kotlinSource)
        ) { invocation ->
            listOf("JavaInterface", "KotlinInterface").forEach { qName ->
                invocation.processingEnv.requireTypeElement(qName).let {
                    assertThat(it.getMethodByJvmName("interfaceMethod").isAbstract()).isTrue()
                }
            }

            listOf("JavaAbstractClass", "KotlinAbstractClass").forEach { qName ->
                invocation.processingEnv.requireTypeElement(qName).let {
                    assertThat(it.getMethodByJvmName("abstractMethod").isAbstract()).isTrue()
                    assertThat(it.getMethodByJvmName("nonAbstractMethod").isAbstract()).isFalse()
                }
            }
        }
    }

    @Test
    fun javaMethodOverridesKotlinProperty() {
        val myInterface = Source.kotlin(
            "MyInterface.kt",
            """
            interface MyInterface {
                val x:Int
                var y:Int
            }
            """.trimIndent()
        )
        val javaImpl = Source.java(
            "JavaImpl",
            """
            class JavaImpl implements MyInterface {
                public int getX() {
                    return 1;
                }
                public int getY() {
                    return 1;
                }
                public void setY(int value) {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(myInterface, javaImpl)
        ) { invocation ->
            val elm = invocation.processingEnv.requireTypeElement("JavaImpl")
            assertThat(
                elm.getMethodByJvmName("getX").returnType.typeName
            ).isEqualTo(TypeName.INT)
            assertThat(
                elm.getMethodByJvmName("getY").returnType.typeName
            ).isEqualTo(TypeName.INT)
            assertThat(
                elm.getMethodByJvmName("setY").parameters.first().type.typeName
            ).isEqualTo(TypeName.INT)
        }
    }

    @Test
    fun javaMethodOverridesKotlinProperty_generic() {
        val myInterface = Source.kotlin(
            "MyInterface.kt",
            """
            interface MyInterface<T> {
                val x:T
                var y:T
            }
            """.trimIndent()
        )
        val javaImpl = Source.java(
            "JavaImpl",
            """
            class JavaImpl implements MyInterface<Integer> {
                public Integer getX() {
                    return 1;
                }
                public Integer getY() {
                    return 1;
                }
                public void setY(Integer value) {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(myInterface, javaImpl)
        ) { invocation ->
            val elm = invocation.processingEnv.requireTypeElement("JavaImpl")
            assertThat(
                elm.getMethodByJvmName("getX").returnType.typeName
            ).isEqualTo(TypeName.INT.box())
            assertThat(
                elm.getMethodByJvmName("getY").returnType.typeName
            ).isEqualTo(TypeName.INT.box())
            assertThat(
                elm.getMethodByJvmName("setY").parameters.first().type.typeName
            ).isEqualTo(TypeName.INT.box())
        }
    }

    @Test
    fun genericToPrimitiveOverrides_methodElement() {
        genericToPrimitiveOverrides(asMemberOf = false)
    }

    @Test
    fun genericToPrimitiveOverrides_asMemberOf() {
        genericToPrimitiveOverrides(asMemberOf = true)
    }

    @Test
    fun defaultMethodParameters() {
        fun buildSource(pkg: String) = Source.kotlin(
            "Foo.kt",
            """
            package $pkg
            class Subject {
                var prop:Int = 1
                fun method1(arg:Int = 0, arg2:Int) {}
                fun method2(arg:Int, arg2:Int = 0) {}
                fun varargMethod1(x:Int = 3, vararg y:Int) {}
                fun varargMethod2(x:Int, vararg y:Int = intArrayOf(1,2,3)) {}
                suspend fun suspendMethod() {}
                @JvmOverloads
                fun jvmOverloadsMethod(
                    x:Int,
                    y:Int = 1,
                    z:String = "foo"
                ) {}
            }
            """.trimIndent()
        )

        fun XExecutableElement.defaults() = parameters.map { it.hasDefaultValue }
        runProcessorTest(
            sources = listOf(buildSource(pkg = "app")),
            classpath = compileFiles(listOf(buildSource(pkg = "lib")))
        ) { invocation ->
            listOf("app", "lib").map {
                invocation.processingEnv.requireTypeElement("$it.Subject")
            }.forEach { subject ->
                subject.getMethodByJvmName("method1").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(true, false).inOrder()
                }
                subject.getMethodByJvmName("method2").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(false, true).inOrder()
                }
                subject.getMethodByJvmName("varargMethod1").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(true, false).inOrder()
                }
                subject.getMethodByJvmName("varargMethod2").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(false, true).inOrder()
                }
                subject.getMethodByJvmName("suspendMethod").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(false)
                }
                subject.getMethodByJvmName("setProp").let { method ->
                    assertWithMessage(method.fallbackLocationText)
                        .that(method.defaults()).containsExactly(false)
                }
                val jvmOverloadedMethodCount = subject.getDeclaredMethods().count {
                    it.jvmName == "jvmOverloadsMethod"
                }
                if (invocation.isKsp) {
                    assertWithMessage(subject.fallbackLocationText)
                        .that(jvmOverloadedMethodCount).isEqualTo(1)
                    subject.getMethodByJvmName("jvmOverloadsMethod").let { method ->
                        assertWithMessage(method.fallbackLocationText)
                            .that(method.defaults())
                            .containsExactly(false, true, true).inOrder()
                    }
                } else {
                    assertWithMessage(subject.fallbackLocationText)
                        .that(jvmOverloadedMethodCount).isEqualTo(3)
                    val actuals = subject.getDeclaredMethods().filter {
                        it.jvmName == "jvmOverloadsMethod"
                    }.associateBy(
                        keySelector = { it.parameters.size },
                        valueTransform = { it.defaults() }
                    )
                    // JVM overloads is not part of the java stub or metadata, hence we cannot
                    // detect it
                    assertWithMessage(subject.fallbackLocationText)
                        .that(actuals)
                        .containsExactlyEntriesIn(
                            mapOf(
                                1 to listOf(false),
                                2 to listOf(false, false),
                                3 to listOf(false, true, true)
                            )
                        )
                }
            }
        }
    }

    @Test
    fun thrownTypes() {
        fun buildSources(pkg: String) = listOf(
            Source.java(
                "$pkg.JavaSubject",
                """
                package $pkg;
                import java.io.*;
                public class JavaSubject {
                    public JavaSubject() throws IllegalArgumentException {}

                    public void multipleThrows() throws IOException, IllegalStateException {
                    }
                }
                """.trimIndent()
            ),
            Source.kotlin(
                "KotlinSubject.kt",
                """
                package $pkg
                import java.io.*
                public class KotlinSubject {
                    @Throws(IllegalArgumentException::class)
                    constructor() {
                    }

                    @Throws(IOException::class, IllegalStateException::class)
                    fun multipleThrows() {
                    }
                }
                """.trimIndent()
            ),
            Source.kotlin(
                "AccessorThrows.kt",
                """
                package $pkg
                import java.io.*
                public class KotlinAccessors {
                    @get:Throws(IllegalArgumentException::class)
                    val getterThrows: Int = 3
                    @set:Throws(IllegalStateException::class)
                    var setterThrows: Int = 3
                    @get:Throws(IOException::class)
                    @set:Throws(IllegalStateException::class, IllegalArgumentException::class)
                    var bothThrows: Int = 3
                }
                """.trimIndent()
            )
        )
        runProcessorTest(
            sources = buildSources("app"),
            classpath = compileFiles(sources = buildSources("lib"))
        ) { invocation ->
            fun collectExceptions(subject: XTypeElement): List<Pair<String, Set<TypeName>>> {
                return (subject.getConstructors() + subject.getDeclaredMethods()).mapNotNull {
                    val throwTypes = it.thrownTypes
                    val name = if (it is XMethodElement) {
                        it.jvmName
                    } else {
                        "<init>"
                    }
                    if (throwTypes.isEmpty()) {
                        null
                    } else {
                        name to throwTypes.map { it.typeName }.toSet()
                    }
                }
            }
            listOf("app", "lib").forEach { pkg ->
                val expectedConstructor =
                    "<init>" to setOf(ClassName.get(IllegalArgumentException::class.java))
                val expectedMethod = "multipleThrows" to setOf(
                    ClassName.get(IOException::class.java),
                    ClassName.get(IllegalStateException::class.java)
                )
                invocation.processingEnv.requireTypeElement("$pkg.KotlinSubject").let { subject ->
                    assertWithMessage(subject.qualifiedName).that(
                        collectExceptions(subject)
                    ).containsExactly(
                        expectedConstructor, expectedMethod
                    )
                }
                invocation.processingEnv.requireTypeElement("$pkg.JavaSubject").let { subject ->
                    assertWithMessage(subject.qualifiedName).that(
                        collectExceptions(subject)
                    ).containsExactly(
                        expectedConstructor,
                        expectedMethod
                    )
                }
                invocation.processingEnv.requireTypeElement("$pkg.KotlinAccessors").let { subject ->
                    assertWithMessage(subject.qualifiedName).that(
                        collectExceptions(subject)
                    ).containsExactly(
                        "getGetterThrows" to setOf(
                            ClassName.get(IllegalArgumentException::class.java)
                        ),
                        "setSetterThrows" to setOf(
                            ClassName.get(IllegalStateException::class.java)
                        ),
                        "getBothThrows" to setOf(
                            ClassName.get(IOException::class.java)
                        ),
                        "setBothThrows" to setOf(
                            ClassName.get(IllegalStateException::class.java),
                            ClassName.get(IllegalArgumentException::class.java)
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun extensionFun() {
        fun buildSource(pkg: String) = Source.kotlin(
            "Foo.kt",
            """
            package $pkg
            abstract class Foo<T> {
                fun String.ext1(): String = TODO()
                fun String.ext2(inputParam: Int): String = TODO()
                fun Foo<String>.ext3(): String = TODO()
                fun Foo<T>.ext4(): String = TODO()
                fun T.ext5(): String = TODO()
                suspend fun String.ext6(): String = TODO()
                abstract fun T.ext7(): String
            }
            class FooImpl : Foo<Int>() {
                override fun Int.ext7(): String = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(buildSource(pkg = "app")),
            classpath = compileFiles(listOf(buildSource(pkg = "lib")))
        ) {
            listOf("app", "lib").forEach { pkg ->
                val element = it.processingEnv.requireTypeElement("$pkg.Foo")
                element.getDeclaredMethodByJvmName("ext1").let { method ->
                    assertThat(method.isExtensionFunction()).isTrue()
                    assertThat(method.parameters.size).isEqualTo(1)
                    assertThat(method.parameters[0].name).isEqualTo("\$this\$ext1")
                    assertThat(method.parameters[0].type.typeName)
                        .isEqualTo(String::class.typeName())
                }
                element.getDeclaredMethodByJvmName("ext2").let { method ->
                    assertThat(method.parameters.size).isEqualTo(2)
                    assertThat(method.parameters[0].name).isEqualTo("\$this\$ext2")
                    assertThat(method.parameters[0].type.typeName)
                        .isEqualTo(String::class.typeName())
                    assertThat(method.parameters[1].name).isEqualTo("inputParam")
                }
                element.getDeclaredMethodByJvmName("ext3").let { method ->
                    assertThat(method.parameters[0].type.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            ClassName.get(pkg, "Foo"),
                            String::class.typeName()
                        )
                    )
                }
                element.getDeclaredMethodByJvmName("ext4").let { method ->
                    assertThat(method.parameters[0].type.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            ClassName.get(pkg, "Foo"),
                            TypeVariableName.get("T")
                        )
                    )
                }
                element.getDeclaredMethodByJvmName("ext5").let { method ->
                    assertThat(method.parameters[0].type.typeName)
                        .isEqualTo(TypeVariableName.get("T"))
                }
                element.getDeclaredMethodByJvmName("ext6").let { method ->
                    assertThat(method.isSuspendFunction()).isTrue()
                    assertThat(method.isExtensionFunction()).isTrue()
                    assertThat(method.parameters.size).isEqualTo(2)
                    assertThat(method.parameters[0].type.typeName)
                        .isEqualTo(String::class.typeName())
                    assertThat(method.parameters[1].type.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            ClassName.get("kotlin.coroutines", "Continuation"),
                            WildcardTypeName.supertypeOf(String::class.typeName())
                        )
                    )
                }
                // Verify overridden Foo.ext7() asMemberOf FooImpl
                element.getDeclaredMethodByJvmName("ext7").let { method ->
                    assertThat(method.isAbstract()).isTrue()
                    assertThat(method.isExtensionFunction()).isTrue()
                    assertThat(method.parameters[0].type.typeName)
                        .isEqualTo(TypeVariableName.get("T"))

                    val fooImpl = it.processingEnv.requireTypeElement("$pkg.FooImpl")
                    assertThat(method.parameters[0].asMemberOf(fooImpl.type).typeName)
                        .isEqualTo(TypeName.INT.box())
                }
                // Verify non-overridden Foo.ext1() asMemberOf FooImpl
                element.getDeclaredMethodByJvmName("ext1").let { method ->
                    val fooImpl = it.processingEnv.requireTypeElement("$pkg.FooImpl")
                    assertThat(method.parameters[0].asMemberOf(fooImpl.type).typeName)
                        .isEqualTo(String::class.typeName())
                }
                // Verify non-overridden Foo.ext5() asMemberOf FooImpl
                element.getDeclaredMethodByJvmName("ext5").let { method ->
                    val fooImpl = it.processingEnv.requireTypeElement("$pkg.FooImpl")
                    assertThat(method.parameters[0].asMemberOf(fooImpl.type).typeName)
                        .isEqualTo(TypeName.INT.box())
                }
            }
        }
    }

    // see b/160258066
    private fun genericToPrimitiveOverrides(asMemberOf: Boolean) {
        val source = Source.kotlin(
            "Foo.kt",
            """
            interface Base<Key> {
                fun getKey(id: Key): Unit
                fun getKeyOverridden(id: Key): Unit
                fun returnKey(): Key
                fun returnKeyOverridden(): Key
                fun getAndReturnKey(key: Key): Key
                fun getAndReturnKeyOverridden(key: Key): Key
            }
            interface NonNullPrimitiveOverride : Base<Int> {
                override fun getKeyOverridden(id: Int): Unit
                override fun returnKeyOverridden(): Int
                override fun getAndReturnKeyOverridden(key: Int): Int
            }
            interface NullablePrimitiveOverride : Base<Int?> {
                override fun getKeyOverridden(id: Int?): Unit
                override fun returnKeyOverridden(): Int?
                override fun getAndReturnKeyOverridden(key: Int?): Int?
            }
            class Item
            interface ClassOverride : Base<Item> {
                override fun getKeyOverridden(id: Item): Unit
                override fun returnKeyOverridden(): Item
                override fun getAndReturnKeyOverridden(key: Item): Item
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(source)) { invocation ->
            val objectMethodNames = invocation.processingEnv.requireTypeElement(TypeName.OBJECT)
                .getAllNonPrivateInstanceMethods().map { it.jvmName }.toSet()

            fun XTypeElement.methodsSignature(): String {
                return getAllNonPrivateInstanceMethods()
                    .filterNot { it.jvmName in objectMethodNames }
                    .sortedBy {
                        it.jvmName
                    }.joinToString("\n") { methodElement ->
                        buildString {
                            append(methodElement.jvmName)
                            append("(")
                            val paramTypes = if (asMemberOf) {
                                methodElement.asMemberOf(this@methodsSignature.type).parameterTypes
                            } else {
                                methodElement.parameters.map { it.type }
                            }
                            val paramsSignature = paramTypes.joinToString(",") {
                                it.typeName.toString()
                            }
                            append(paramsSignature)
                            append("):")
                            val returnType = if (asMemberOf) {
                                methodElement.asMemberOf(this@methodsSignature.type).returnType
                            } else {
                                methodElement.returnType
                            }
                            append(returnType.typeName)
                        }
                    }
            }

            val nonNullOverride =
                invocation.processingEnv.requireTypeElement("NonNullPrimitiveOverride")
            assertThat(
                nonNullOverride.methodsSignature()
            ).isEqualTo(
                """
                getAndReturnKey(java.lang.Integer):java.lang.Integer
                getAndReturnKeyOverridden(java.lang.Integer):java.lang.Integer
                getAndReturnKeyOverridden(int):java.lang.Integer
                getKey(java.lang.Integer):void
                getKeyOverridden(java.lang.Integer):void
                getKeyOverridden(int):void
                returnKey():java.lang.Integer
                returnKeyOverridden():java.lang.Integer
                """.trimIndent()
            )
            val nullableOverride =
                invocation.processingEnv.requireTypeElement("NullablePrimitiveOverride")
            assertThat(
                nullableOverride.methodsSignature()
            ).isEqualTo(
                """
                getAndReturnKey(java.lang.Integer):java.lang.Integer
                getAndReturnKeyOverridden(java.lang.Integer):java.lang.Integer
                getKey(java.lang.Integer):void
                getKeyOverridden(java.lang.Integer):void
                returnKey():java.lang.Integer
                returnKeyOverridden():java.lang.Integer
                """.trimIndent()
            )
            val classOverride = invocation.processingEnv.requireTypeElement("ClassOverride")
            assertThat(
                classOverride.methodsSignature()
            ).isEqualTo(
                """
                getAndReturnKey(Item):Item
                getAndReturnKeyOverridden(Item):Item
                getKey(Item):void
                getKeyOverridden(Item):void
                returnKey():Item
                returnKeyOverridden():Item
                """.trimIndent()
            )
        }
    }

    @Test
    fun name() {
        fun buildSources(pkg: String) = listOf(
            Source.kotlin(
                "KotlinSource.kt",
                """
            package $pkg;
            @JvmInline
            value class ValueClass(val value: String)
            internal class InternalClass(val value: String)
            class KotlinSubject {
                var property: String = ""
                internal var internalProperty: String = ""
                var valueClassProperty: ValueClass = ValueClass("")
                internal var internalClassProperty = InternalClass("")
                fun normalFun() { TODO() }
                @JvmName("jvmNameForFun")
                fun jvmNameFun() { TODO() }
                internal fun internalFun() { TODO() }
                fun valueReceivingFun(param: ValueClass) { TODO() }
                fun valueReturningFun(): ValueClass { TODO() }
                internal fun internalValueReceivingFun(param: ValueClass) { TODO() }
                internal fun internalValueReturningFun(): ValueClass { TODO() }
            }
            """.trimIndent()
            )
        )

        val sources = buildSources("app")
        val classpath = compileFiles(buildSources("lib"))
        runProcessorTest(
            sources = sources,
            classpath = classpath
        ) { invocation ->
            // we use this to remove the hash added by the compiler for function names that don't
            // have valid JVM names
            // regex: match 7 characters after -
            val removeHashRegex = """(?<=-)(.{7})""".toRegex()

            fun XTypeElement.collectNameJvmNamePairs() = getDeclaredMethods().map {
                it.name to removeHashRegex.replace(it.jvmName, "HASH")
            }
            listOf("app", "lib").forEach { pkg ->
                val kotlinSubject = invocation.processingEnv
                    .requireTypeElement("$pkg.KotlinSubject")
                val validJvmProperties = listOf(
                    "getInternalClassProperty" to "getInternalClassProperty\$main",
                    "getInternalProperty" to "getInternalProperty\$main",
                    "getProperty" to "getProperty",
                    "internalFun" to "internalFun\$main",
                    "jvmNameFun" to "jvmNameForFun",
                    "normalFun" to "normalFun",
                    "setInternalClassProperty" to "setInternalClassProperty\$main",
                    "setInternalProperty" to "setInternalProperty\$main",
                    "setProperty" to "setProperty",
                )
                // these won't show up in KAPT stubs as they don't have valid jvm names
                val nonJvmProperties = listOf(
                    "getValueClassProperty" to "getValueClassProperty-HASH",
                    "internalValueReceivingFun" to "internalValueReceivingFun-HASH\$main",
                    "internalValueReturningFun" to "internalValueReturningFun-HASH\$main",
                    "setValueClassProperty" to "setValueClassProperty-HASH",
                    "valueReceivingFun" to "valueReceivingFun-HASH",
                    "valueReturningFun" to "valueReturningFun-HASH",
                )
                val expected = if (invocation.isKsp || pkg == "lib") {
                    validJvmProperties + nonJvmProperties
                } else {
                    validJvmProperties
                }
                assertWithMessage("declarations in $pkg")
                    .that(kotlinSubject.collectNameJvmNamePairs())
                    .containsExactlyElementsIn(
                        expected
                    )
            }
        }
    }
}
