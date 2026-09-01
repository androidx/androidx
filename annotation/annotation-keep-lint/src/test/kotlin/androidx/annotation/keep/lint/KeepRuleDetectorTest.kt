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

package androidx.annotation.keep.lint

import com.android.tools.lint.checks.AnnotationDetector
import com.android.tools.lint.checks.ObjectAnimatorDetector
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class KeepRuleDetectorTest {

    @Test
    fun testDocumentationExample() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg
            fun simpleCall(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s)
            }
            """
                    )
                    .indented(),
                // Test both Java and Kotlin, since they have different quickfix syntax
                java(
                        """
            package test.pkg;

            import java.lang.reflect.Method;

            public class JavaUsage {
                public void simpleCall(Object p, String s) throws Exception {
                    Class.forName("androidx.api.Printer")
                            .getDeclaredMethod("print", String.class)
                            .invoke(p, s);
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsage.java:9: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        .invoke(p, s);
                         ~~~~~~
        src/test/pkg/test.kt:5: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                .invoke(p, s)
                 ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsage.java line 9: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -5,0 +7,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = {String.class}
        +    )
        Autofix for src/test/pkg/test.kt line 5: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2,6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.api.Printer",
        +    methodName = "print",
        +    parameterTypes = [String::class]
        +)
        """
            )
    }

    @Test
    fun testAlreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypes = [java.lang.String::class]
            )
            fun alreadyAnnotated(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // OK, already annotated
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.pkg;

            import androidx.annotation.keep.UsesReflectionToAccessMethod;

            public class AlreadyAnnotated {
                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "print", parameterTypes = {String.class})
                public void simpleCall(Object p, String s) throws Exception {
                    Class.forName("androidx.api.Printer")
                            .getDeclaredMethod("print", String.class)
                            .invoke(p, s); // OK, already annotated
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAlreadyAnnotatedOuterClass() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypes = [java.lang.String::class]
            )
            class MyClass {
              fun alreadyAnnotated(p: Any, s: String) {
                  Class.forName("androidx.api.Printer")
                      .getDeclaredMethod("print", String::class.java)
                      .invoke(p, s) // OK, already annotated on outer class
              }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAnnotationClassNotAvailable() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            fun test(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // OK - keep annotations not on the classpath
            }
            """
                    )
                    .indented()
                // Deliberately not including *usesReflectionStubs here
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAnnotationMatching() {
        // Verifies a number of scenarios to make sure that we match up reflection usages with
        // annotations:
        //  (1) Make sure the parameter types match, if specified
        //  (2) Make sure that wildcard names match
        //  (3) Make sure that constructor and method=<init> matches
        //  (4) Test a mixture of strings in source and constants in annotation
        //      and vice versa
        //  (5) Test the special handling where not specifying parameters should
        //      match all; make sure that isn't confused as meaning an empty list
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer2",
                methodName = "print",
                parameterTypes = [String::class, Boolean::class]
            )
            fun wrongClassName(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // ERROR 1: wrong class name
            }

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypes = [String::class, Boolean::class]
            )
            fun wrongParameterTypes(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // ERROR 2: parameter list mismatch
            }

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypeNames = ["java.lang.String"]
            )
            fun matchTypeString(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // OK 1
            }

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "*",
            )
            fun wildcardMatch(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // OK 2: Should be matched by *
            }

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "<init>",
            )
            fun constructorMatch(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredConstructor(String::class.java)
                    .newInstance(p, s) // OK 3
            }

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypes = []
            )
            fun matchNoArgs(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // ERROR 3: Shouldn't match empty parameter list
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:13: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                .invoke(p, s) // ERROR 1: wrong class name
                 ~~~~~~
        src/test/pkg/test.kt:24: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                .invoke(p, s) // ERROR 2: parameter list mismatch
                 ~~~~~~
        src/test/pkg/test.kt:66: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                .invoke(p, s) // ERROR 3: Shouldn't match empty parameter list
                 ~~~~~~
        0 errors, 3 warnings
        """
            )
    }

    @Test
    fun testConstructorExplicit() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            @file:Suppress("unused", "NewApi")

            package com.example.keeptest
            import test.pkg.api.Foo
            import android.content.Context
            import java.time.Clock
            // Adapted from GcmWorker in Jetpack
            fun tryCreateFoo(context: Context, clock: Clock): Foo? {
                val klass = Class.forName("com.example.FooImpl")
                return try {
                    klass.getConstructor(Context::class.java, Clock::class.java).newInstance(context, clock)
                            as Foo
                } catch (_: Throwable) {
                    null
                }
            }

            fun tryCreateFoo(): Foo? {
                val klass = Class.forName("com.example.FooImpl")
                return try {
                    klass.getDeclaredConstructor().newInstance() as Foo
                } catch (_: Throwable) {
                    null
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/test.kt:11: Warning: This method calls com.example.FooImpl.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                klass.getConstructor(Context::class.java, Clock::class.java).newInstance(context, clock)
                                                                             ~~~~~~~~~~~
        src/com/example/keeptest/test.kt:21: Warning: This method calls com.example.FooImpl.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                klass.getDeclaredConstructor().newInstance() as Foo
                                               ~~~~~~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/com/example/keeptest/test.kt line 11: Annotate with @UsesReflectionToConstruct:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -7,0 +9,4 @@
        +@UsesReflectionToConstruct(
        +    className = "com.example.FooImpl",
        +    parameterTypes = [Context::class, Clock::class]
        +)
        Autofix for src/com/example/keeptest/test.kt line 21: Annotate with @UsesReflectionToConstruct:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -17,0 +19,4 @@
        +@UsesReflectionToConstruct(
        +    className = "com.example.FooImpl",
        +    parameterTypes = []
        +)
        """
            )
    }

    @Test
    fun testConstructorAlreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            @file:Suppress("unused")

            package com.example.keeptest
            import test.pkg.api.Foo
            import android.annotation.SuppressLint
            import android.content.Context
            import androidx.annotation.keep.UsesReflectionToConstruct
            import java.time.Clock

            // Adapted from GcmWorker in Jetpack
            @SuppressLint("NewApi")
            @UsesReflectionToConstruct(
                className = "com.example.FooImpl",
                parameterTypes = [Context::class, Clock::class]
            )
            fun tryCreateFoo(context: Context, clock: Clock): Foo? {
                val klass = Class.forName("com.example.FooImpl")
                return try {
                    klass.getConstructor(Context::class.java, Clock::class.java).newInstance(context, clock)
                            as Foo
                } catch (_: Throwable) {
                    null
                }
            }

            @UsesReflectionToConstruct(className = "com.example.FooImpl", parameterTypes = [])
            fun tryCreateFoo(): Foo? {
                val klass = Class.forName("com.example.FooImpl")
                return try {
                    klass.getDeclaredConstructor().newInstance() as Foo
                } catch (_: Throwable) {
                    null
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testConstructorSubclass() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package com.example.keeptest;

            import test.pkg.api.Bar;
            import java.lang.reflect.Constructor;
            import java.lang.reflect.InvocationTargetException;

            public class JavaTest {
                public static Bar tryCreateBarSubclass(String name) {
                    try {
                        Class<?> klass = Class.forName(name).asSubclass(Bar.class);
                        Constructor<?> constructor = klass.getDeclaredConstructor();
                        return (Bar) constructor.newInstance(); // ERROR 1
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                             InvocationTargetException | InstantiationException e) {
                        return null;
                    }
                }

                public static Class<? extends Bar> getBarSubclass(String name) throws ClassNotFoundException{
                      return Class.forName(name).asSubclass(Bar.class); // ERROR 2
                }

                public static Bar tryCreateBarSubclassSplit(String name) {
                    try {
                        Constructor<? extends Bar> constructor = getBarSubclass(name).getDeclaredConstructor();
                        return constructor.newInstance(); // ERROR 3
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                             InvocationTargetException | InstantiationException e) {
                        return null;
                    }
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        "src/test/pkg/KotlinTest.kt",
                        """
            @file:Suppress("unused")

            package test.pkg

            import test.pkg.api.Bar

            // this generates a keep for the class as well as all subclasses
            // in the future, we may want to specify an arg to specify to keep subclasses only
            fun tryCreateBarSubclass(name: String): Bar? {
                val klass = Class.forName(name).asSubclass(Bar::class.java)
                return try {
                    klass.getDeclaredConstructor().newInstance() as Bar // ERROR 4
                } catch (_: Throwable) {
                    null
                }
            }

            fun getBarSubclass(name: String): Class<out Bar> {
                return Class.forName(name).asSubclass(Bar::class.java) // ERROR 5
            }

            // this is a harder case, since the asSubclass is in a sub-method
            fun tryCreateBarSubclassSplit(name: String): Bar? {
                return try {
                    getBarSubclass(name).getDeclaredConstructor().newInstance() as Bar // ERROR 6
                } catch (_: Throwable) {
                    null
                }
            }

            fun getBarSubclass2(name: String): Class<out Bar> {
                @Suppress("UNCHECKED_CAST")
                return Class.forName(name) as Class<Bar> // ERROR 7
            }
            """,
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/JavaTest.java:12: Warning: This method calls test.pkg.api.Bar.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                    return (Bar) constructor.newInstance(); // ERROR 1
                                             ~~~~~~~~~~~
        src/com/example/keeptest/JavaTest.java:20: Warning: This method calls test.pkg.api.Bar reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                  return Class.forName(name).asSubclass(Bar.class); // ERROR 2
                               ~~~~~~~
        src/com/example/keeptest/JavaTest.java:26: Warning: This method calls test.pkg.api.Bar.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                    return constructor.newInstance(); // ERROR 3
                                       ~~~~~~~~~~~
        src/test/pkg/KotlinTest.kt:12: Warning: This method calls test.pkg.api.Bar.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                klass.getDeclaredConstructor().newInstance() as Bar // ERROR 4
                                               ~~~~~~~~~~~
        src/test/pkg/KotlinTest.kt:19: Warning: This method calls test.pkg.api.Bar reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            return Class.forName(name).asSubclass(Bar::class.java) // ERROR 5
                         ~~~~~~~
        src/test/pkg/KotlinTest.kt:25: Warning: This method calls test.pkg.api.Bar.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                getBarSubclass(name).getDeclaredConstructor().newInstance() as Bar // ERROR 6
                                                              ~~~~~~~~~~~
        src/test/pkg/KotlinTest.kt:33: Warning: This method calls test.pkg.api.Bar reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            return Class.forName(name) as Class<Bar> // ERROR 7
                         ~~~~~~~
        0 errors, 7 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/com/example/keeptest/JavaTest.java line 12: Annotate with @UsesReflectionToConstruct:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct;
        @@ -7,0 +9,4 @@
        +    @UsesReflectionToConstruct(
        +        classConstant = Bar.class,
        +        parameterTypes = {}
        +    )
        Autofix for src/com/example/keeptest/JavaTest.java line 26: Annotate with @UsesReflectionToConstruct:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct;
        @@ -22,0 +24,4 @@
        +    @UsesReflectionToConstruct(
        +        classConstant = Bar.class,
        +        parameterTypes = {}
        +    )
        Autofix for src/test/pkg/KotlinTest.kt line 12: Annotate with @UsesReflectionToConstruct:
        @@ -4,0 +5 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -8,0 +10,4 @@
        +@UsesReflectionToConstruct(
        +    classConstant = Bar::class,
        +    parameterTypes = []
        +)
        Autofix for src/test/pkg/KotlinTest.kt line 25: Annotate with @UsesReflectionToConstruct:
        @@ -4,0 +5 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -22,0 +24,4 @@
        +@UsesReflectionToConstruct(
        +    classConstant = Bar::class,
        +    parameterTypes = []
        +)
        """
            )
    }

    @Test
    fun testReflectionInnerFunction() {
        // Adapted from WorkManager's use of reflection
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
          package android.content
          class Context {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          class WorkerParameters {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.work.WorkerParameters
          abstract class ListenableWorker(private val context: Context, private val parameters: WorkerParameters) {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.work.WorkerParameters
          import androidx.work.ListenableWorker
          abstract class WorkerFactory {
            abstract fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker?
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.work.WorkerParameters
          import androidx.work.ListenableWorker
          import androidx.work.WorkerFactory
          import java.lang.reflect.Constructor

          class ReflectiveFactory: WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker? {
              fun createWorkerBlock(): ListenableWorker {
                val klass = Class.forName(name).asSubclass(ListenableWorker::class.java)
                val constructor = klass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
                val instance = constructor.newInstance(context, parameters)
                return instance
              }

              return createWorkerBlock()
            }
          }
        """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/androidx/work/ReflectiveFactory.kt:13: Warning: This method calls androidx.work.ListenableWorker.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
              val instance = constructor.newInstance(context, parameters)
                                         ~~~~~~~~~~~
        0 errors, 1 warning
      """
            )
            .expectFixDiffs(
                """
                Autofix for src/androidx/work/ReflectiveFactory.kt line 13: Annotate with @UsesReflectionToConstruct:
                @@ -3,0 +4 @@
                +import androidx.annotation.keep.UsesReflectionToConstruct
                @@ -8,0 +10,4 @@
                +  @UsesReflectionToConstruct(
                +      classConstant = ListenableWorker::class,
                +      parameterTypes = [Context::class, WorkerParameters::class]
                +  )
                """
                    .trimIndent()
            )
    }

    @Test
    fun testReflectionAnnotatedInnerFunction() {
        // Adapted from WorkManager's use of reflection
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
          package android.content
          class Context {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          class WorkerParameters {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.work.WorkerParameters
          abstract class ListenableWorker(private val context: Context, private val parameters: WorkerParameters) {
            // Stub
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.work.WorkerParameters
          import androidx.work.ListenableWorker
          abstract class WorkerFactory {
            abstract fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker?
          }
        """
                    )
                    .indented(),
                kotlin(
                        """
          package androidx.work
          import android.content.Context
          import androidx.annotation.keep.UsesReflectionToConstruct
          import androidx.work.WorkerParameters
          import androidx.work.ListenableWorker
          import androidx.work.WorkerFactory
          import java.lang.reflect.Constructor

          class ReflectiveFactory: WorkerFactory() {
            @UsesReflectionToConstruct(
              classConstant = ListenableWorker::class,
              parameterTypes = [Context::class, WorkerParameters::class]
            )
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker? {
              fun createWorkerBlock(): ListenableWorker {
                val klass = Class.forName(name).asSubclass(ListenableWorker::class.java)
                val constructor = klass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
                val instance = constructor.newInstance(context, parameters)
                return instance
              }

              return createWorkerBlock()
            }
          }
        """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testField1() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package com.example.keeptest

            import test.pkg.api.Foo

            fun fieldStatic(): Foo? {
                val foo = runCatching {
                    val classLoader = Foo::class.java.classLoader!!
                    val className = "com.example.SomeClass"
                    val fieldName = "foo"
                    val fieldRef = classLoader.loadClass(className).getField(fieldName)
                    fieldRef.get(null) as? Foo
                }
                return foo.getOrNull()
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/test.kt:11: Warning: This method references com.example.SomeClass.foo reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                fieldRef.get(null) as? Foo
                         ~~~
        0 errors, 1 warning
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/com/example/keeptest/test.kt line 11: Annotate with @UsesReflectionToAccessField:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField
        @@ -4,0 +6,5 @@
        +@UsesReflectionToAccessField(
        +    className = "com.example.SomeClass",
        +    fieldName = "foo",
        +    fieldType = Foo::class
        +)
        """
            )
    }

    @Test
    fun testField1_alreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package com.example.keeptest

            import androidx.annotation.keep.UsesReflectionToAccessField
            import test.pkg.api.Foo

            @UsesReflectionToAccessField(
               className = "com.example.SomeClass",
               fieldName = "*",
               fieldType = Foo::class
            )
            fun fieldStatic(): Foo? {
                val foo = runCatching {
                    val classLoader = Foo::class.java.classLoader!!
                    val className = "com.example.SomeClass"
                    val fieldName = "foo"
                    val fieldRef = classLoader.loadClass(className).getField(fieldName)
                    fieldRef.get(null) as? Foo
                }
                return foo.getOrNull()
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testField2() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        // Example from ErrorCode in health:connect:connect-client
                        """
            package test.pkg

            import java.lang.reflect.Field

            annotation class ErrorCode {
                companion object {
                    const val PROVIDER_NOT_INSTALLED = 1
                    const val PROVIDER_NOT_ENABLED = 2
                    const val INTERNAL_ERROR = 3
                }
            }

            @ErrorCode
            fun multiMethod(errorCode: Int): Int {
                return ErrorCode::class
                    .java
                    .declaredFields
                    .filter { it.type.isAssignableFrom(Int::class.java) }
                    .map { field: Field ->
                        try {
                            return@map field[null] as Int
                        } catch (e: IllegalAccessException) {
                            return@map ErrorCode.INTERNAL_ERROR
                        }
                    }
                    .firstOrNull { value: Int -> value == errorCode } ?: ErrorCode.INTERNAL_ERROR
            }

            fun getFields() {}
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/ErrorCode.kt:17: Warning: This method references test.pkg.ErrorCode.* reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                .declaredFields
                 ~~~~~~~~~~~~~~
        0 errors, 1 warning
        """
            )
            // No fix since we don't know specific method
            .expectFixDiffs("")
    }

    @Test
    fun testFieldAlreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessField
            import java.lang.reflect.Field

            annotation class ErrorCode {
                companion object {
                    const val PROVIDER_NOT_INSTALLED = 1
                    const val PROVIDER_NOT_ENABLED = 2
                    const val INTERNAL_ERROR = 3
                }
            }

            @UsesReflectionToAccessField(
                classConstant = ErrorCode::class,
                fieldName = "*"
            )
            @ErrorCode
            fun multiMethod(errorCode: Int): Int {
                return ErrorCode::class
                    .java
                    .declaredFields
                    .filter { it.type.isAssignableFrom(Int::class.java) }
                    .map { field: Field ->
                        try {
                            return@map field[null] as Int
                        } catch (e: IllegalAccessException) {
                            return@map ErrorCode.INTERNAL_ERROR
                        }
                    }
                    .firstOrNull { value: Int -> value == errorCode } ?: ErrorCode.INTERNAL_ERROR
            }

            fun getFields() {}
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testField4() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        // From androidx'
                        // core/core/src/main/java/androidx/core/app/NotificationManagerCompat.java
                        """
            package com.example.keeptest;

            import android.content.Context;

            import android.app.AppOpsManager;
            import android.content.pm.ApplicationInfo;

            import java.lang.reflect.Field;
            import java.lang.reflect.Method;

            public class AppOpsTest {
                private Context mContext;
                private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
                private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
                public boolean areNotificationsEnabled() throws Exception {
                    AppOpsManager appOps =
                            (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
                    ApplicationInfo appInfo = mContext.getApplicationInfo();
                    String pkg = mContext.getApplicationContext().getPackageName();
                    int uid = appInfo.uid;
                    Class<?> appOpsClass = Class.forName(AppOpsManager.class.getName());
                    Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE,
                            Integer.TYPE, String.class);
                    Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                    int value = (int) opPostNotificationValue.get(Integer.class);
                    return ((int) checkOpNoThrowMethod.invoke(appOps, value, uid, pkg)
                            == AppOpsManager.MODE_ALLOWED);
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/AppOpsTest.java:25: Warning: This method references OP_POST_NOTIFICATION reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                int value = (int) opPostNotificationValue.get(Integer.class);
                                                          ~~~
        src/com/example/keeptest/AppOpsTest.java:26: Warning: This method calls checkOpNoThrow() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                return ((int) checkOpNoThrowMethod.invoke(appOps, value, uid, pkg)
                                                   ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for src/com/example/keeptest/AppOpsTest.java line 25: Annotate with @UsesReflectionToAccessField:
        @@ -7,0 +8 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField;
        @@ -14,0 +16,5 @@
        +    @UsesReflectionToAccessField(
        +        className = "[TODO]|",
        +        fieldName = "OP_POST_NOTIFICATION",
        +        fieldType = int.class
        +    )
        Fix for src/com/example/keeptest/AppOpsTest.java line 26: Annotate with @UsesReflectionToAccessMethod:
        @@ -7,0 +8 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -14,0 +16,6 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "[TODO]|",
        +        methodName = "checkOpNoThrow",
        +        parameterTypes = {int.class, int.class, String.class},
        +        returnType = int.class
        +    )
        """
            )
    }

    @Test
    fun testInvalid() {
        lint()
            .files(
                kotlin(
                        """
            import androidx.annotation.keep.UsesReflectionToConstruct
            import androidx.annotation.keep.UsesReflectionToAccessField
            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToConstruct() // ERROR 1
            @UsesReflectionToAccessMethod(methodName = "method") // ERROR 2
            @UsesReflectionToAccessField(fieldName = "field") // ERROR 3
            fun missingClass() {}

            @UsesReflectionToConstruct(classConstant = Foo::class, className = "Foo", ) // ERROR 4
            @UsesReflectionToAccessMethod(classConstant = Foo::class, className = "Foo", methodName = "method") // ERROR 5
            @UsesReflectionToAccessField(classConstant = Foo::class, className = "Foo", fieldName = "field") // ERROR 6
            fun dupeClass() {}

            @UsesReflectionToConstruct(classConstant = Foo::class, parameterTypes = [], parameterTypeNames = []) // ERROR 7
            @UsesReflectionToAccessMethod(classConstant = Foo::class, methodName = "method", parameterTypes = [], parameterTypeNames = []) // ERROR 8
            fun dupeParams() {}

            @UsesReflectionToAccessField(classConstant = Foo::class, fieldName = "field", fieldType = Foo::class, fieldTypeName = "Foo") // ERROR 9
            fun dupeFieldClass() {}

            @UsesReflectionToConstruct(className = "TODO") // ERROR 10

            class Foo
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .issues(AnnotationDetector.ANNOTATION_USAGE)
            .run()
            .expect(
                """
        src/Foo.kt:5: Error: @UsesReflectionToConstruct must specify either a classConstant or a className attribute [SupportAnnotationUsage]
        @UsesReflectionToConstruct() // ERROR 1
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/Foo.kt:6: Error: @UsesReflectionToAccessMethod must specify either a classConstant or a className attribute [SupportAnnotationUsage]
        @UsesReflectionToAccessMethod(methodName = "method") // ERROR 2
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/Foo.kt:7: Error: @UsesReflectionToAccessField must specify either a classConstant or a className attribute [SupportAnnotationUsage]
        @UsesReflectionToAccessField(fieldName = "field") // ERROR 3
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/Foo.kt:10: Error: Specify only one of classConstant or className [SupportAnnotationUsage]
        @UsesReflectionToConstruct(classConstant = Foo::class, className = "Foo", ) // ERROR 4
                                                   ~~~~~~~~~~
        src/Foo.kt:11: Error: Specify only one of classConstant or className [SupportAnnotationUsage]
        @UsesReflectionToAccessMethod(classConstant = Foo::class, className = "Foo", methodName = "method") // ERROR 5
                                                      ~~~~~~~~~~
        src/Foo.kt:12: Error: Specify only one of classConstant or className [SupportAnnotationUsage]
        @UsesReflectionToAccessField(classConstant = Foo::class, className = "Foo", fieldName = "field") // ERROR 6
                                                     ~~~~~~~~~~
        src/Foo.kt:15: Error: Specify only one of parameterTypes or parameterTypeNames [SupportAnnotationUsage]
        @UsesReflectionToConstruct(classConstant = Foo::class, parameterTypes = [], parameterTypeNames = []) // ERROR 7
                                                                                ~~
        src/Foo.kt:16: Error: Specify only one of parameterTypes or parameterTypeNames [SupportAnnotationUsage]
        @UsesReflectionToAccessMethod(classConstant = Foo::class, methodName = "method", parameterTypes = [], parameterTypeNames = []) // ERROR 8
                                                                                                          ~~
        src/Foo.kt:19: Error: Specify only one of fieldType or fieldTypeName [SupportAnnotationUsage]
        @UsesReflectionToAccessField(classConstant = Foo::class, fieldName = "field", fieldType = Foo::class, fieldTypeName = "Foo") // ERROR 9
                                                                                                  ~~~~~~~~~~
        src/Foo.kt:22: Error: Specify a real className [SupportAnnotationUsage]
        @UsesReflectionToConstruct(className = "TODO") // ERROR 10
                                               ~~~~~~
        10 errors
        """
            )
    }

    @Test
    fun testMethod1() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package com.example.keeptest

            import test.pkg.api.Foo

            fun callStatic(): Foo? {
                val foo = runCatching {
                    val classLoader =
                        Foo::class.java.classLoader!! // this is an arbitrary way to get a classloader
                    val className = "com.example.SomeClass"
                    val methodName = "getFoo"
                    val methodRef = classLoader.loadClass(className).getMethod(methodName)
                    methodRef.invoke(null) as? Foo
                }
                return foo.getOrNull()
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/test.kt:12: Warning: This method calls com.example.SomeClass.getFoo() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                methodRef.invoke(null) as? Foo
                          ~~~~~~
        0 errors, 1 warning
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/com/example/keeptest/test.kt line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -4,0 +6,6 @@
        +@UsesReflectionToAccessMethod(
        +    className = "com.example.SomeClass",
        +    methodName = "getFoo",
        +    parameterTypes = [],
        +    returnType = Foo::class
        +)
        """
            )
    }

    @Test
    fun testMethod2() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package com.example.keeptest

            import test.pkg.api.Bar
            import test.pkg.api.Foo

            fun callStaticMulti(): Foo? {
                val foo = runCatching {
                    val classLoader =
                        Foo::class.java.classLoader!! // this is an arbitrary way to get a classloader
                    val className = "com.example.SomeClass"
                    val methodName = "getFoo"
                    val clazz = classLoader.loadClass(className)
                    if (clazz.getMethod("getBar").invoke(null) as? Bar == null) {
                        return null
                    }

                    clazz.getMethod(methodName).invoke(null) as? Foo
                }
                return foo.getOrNull()
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/test.kt:13: Warning: This method calls com.example.SomeClass.getBar() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                if (clazz.getMethod("getBar").invoke(null) as? Bar == null) {
                                              ~~~~~~
        src/com/example/keeptest/test.kt:17: Warning: This method calls com.example.SomeClass.getFoo() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                clazz.getMethod(methodName).invoke(null) as? Foo
                                            ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/com/example/keeptest/test.kt line 13: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -5,0 +7,6 @@
        +@UsesReflectionToAccessMethod(
        +    className = "com.example.SomeClass",
        +    methodName = "getBar",
        +    parameterTypes = [],
        +    returnType = Bar::class
        +)
        Autofix for src/com/example/keeptest/test.kt line 17: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -5,0 +7,6 @@
        +@UsesReflectionToAccessMethod(
        +    className = "com.example.SomeClass",
        +    methodName = "getFoo",
        +    parameterTypes = [],
        +    returnType = Foo::class
        +)
        """
            )
    }

    @Test
    fun testMethod3() {
        // Several tricky things here; the reflected type is as type parameter
        // and the method signature is accessed via varargs dereference
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package com.example.keeptest

            import android.util.ArrayMap
            import java.lang.reflect.Method
            import kotlin.reflect.KClass

            // Copied from navigation-common, several complexities:
            // - extracted method signature
            // - method return type defined in object type's superclass
            // - method map used to define type

            open class NavArgs

            class SavedState

            internal val methodSignature = arrayOf(SavedState::class.java)
            internal val methodMap = ArrayMap<KClass<out NavArgs>, Method>()

            class NavArgsLazy<Args : NavArgs>(
                private val navArgsClass: KClass<Args>,
                private val argumentProducer: () -> SavedState
            ) : Lazy<Args> {
                private var cached: Args? = null

                override val value: Args
                    get() {
                        var args = cached
                        if (args == null) {
                            val arguments = argumentProducer()
                            val method: Method =
                                methodMap[navArgsClass]
                                    ?: navArgsClass.java.getMethod("fromBundle", *methodSignature).also { method
                                        ->
                                        // Save a reference to the method
                                        methodMap[navArgsClass] = method
                                    }

                            @Suppress("UNCHECKED_CAST")
                            args = method.invoke(null, arguments) as Args
                            cached = args
                        }
                        return args
                    }

                override fun isInitialized(): Boolean = cached != null
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/com/example/keeptest/NavArgs.kt:39: Warning: This method calls fromBundle() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        args = method.invoke(null, arguments) as Args
                                      ~~~~~~
        0 errors, 1 warning
        """
            )
            .verifyFixes()
            .window(1)
            .expectFixDiffs(
                """
        Fix for src/com/example/keeptest/NavArgs.kt line 39: Annotate with @UsesReflectionToAccessMethod:
        @@ -3,2 +3,3 @@
         import android.util.ArrayMap
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
         import java.lang.reflect.Method
        @@ -25,2 +26,8 @@
             override val value: Args
        +        @UsesReflectionToAccessMethod(
        +            className = [TODO()]|,
        +            methodName = "fromBundle",
        +            parameterTypes = [SavedState::class],
        +            returnType = NavArgs::class
        +        )
                 get() {
        """
            )
    }

    @Test
    fun testMethodNegative() {
        // Already annotated; this is testMethod1, testMethod2 and testMethod3 combined
        // along with explanations
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import test.pkg.api.Bar
            import test.pkg.api.Foo
            import android.util.ArrayMap
            import java.lang.reflect.Method
            import kotlin.reflect.KClass
            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "com.example.SomeClass",
                methodName = "getFoo",
                returnType = Foo::class
            )
            fun callStatic(): Foo? {
                val foo = runCatching {
                    val classLoader =
                        Foo::class.java.classLoader!! // this is an arbitrary way to get a classloader
                    val className = "com.example.SomeClass"
                    val methodName = "getFoo"

                    val methodRef = classLoader.loadClass(className).getMethod(methodName)
                    methodRef.invoke(null) as? Foo
                }
                return foo.getOrNull()
            }

            @UsesReflectionToAccessMethod(
                className = "com.example.SomeClass",
                methodName = "getFoo",
                returnType = Foo::class
            )
            @UsesReflectionToAccessMethod(
                className = "com.example.SomeClass",
                methodName = "getBar",
                returnType = Bar::class
            )
            fun callStaticMulti(): Foo? {
                val foo = runCatching {
                    val classLoader =
                        Foo::class.java.classLoader!! // this is an arbitrary way to get a classloader
                    val className = "com.example.SomeClass"
                    val methodName = "getFoo"
                    val clazz = classLoader.loadClass(className)
                    if (clazz.getMethod("getBar").invoke(null) as? Bar == null) {
                        return null
                    }

                    clazz.getMethod(methodName).invoke(null) as? Foo
                }
                return foo.getOrNull()
            }

            // Copied from navigation-common, several complexities:
            // - extracted method signature
            // - method return type defined in object type's superclass
            // - method map used to define type

            open class NavArgs

            class SavedState

            internal val methodSignature = arrayOf(SavedState::class.java)
            internal val methodMap = ArrayMap<KClass<out NavArgs>, Method>()

            class NavArgsLazy<Args : NavArgs>(
                private val navArgsClass: KClass<Args>,
                private val argumentProducer: () -> SavedState
            ) : Lazy<Args> {
                private var cached: Args? = null

                override val value: Args
                    @UsesReflectionToAccessMethod(
                        classConstant = NavArgs::class,
                        methodName = "fromBundle",
                        parameterTypes = [SavedState::class]
                    )
                    get() {
                        var args = cached
                        if (args == null) {
                            val arguments = argumentProducer()
                            val method: Method =
                                methodMap[navArgsClass]
                                    ?: navArgsClass.java.getMethod("fromBundle", *methodSignature).also { method
                                        ->
                                        // Save a reference to the method
                                        methodMap[navArgsClass] = method
                                    }

                            @Suppress("UNCHECKED_CAST")
                            args = method.invoke(null, arguments) as Args
                            cached = args
                        }
                        return args
                    }

                override fun isInitialized(): Boolean = cached != null
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg.api
            interface Foo
            open class Bar
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAddFirstAnnotationInKotlin() {
        // Checks the quickfix to make sure we're correctly inserting the right annotations
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            fun simpleCall(p: Any, s: String) {
                Class.forName("androidx.api.Printer")
                    .getDeclaredMethod("print", String::class.java)
                    .invoke(p, s) // ERROR 1
            }

            fun simpleCallWithLoadClass(p: Any, s: String) {
                p.javaClass.classLoader?.loadClass("androidx.api.Printer")
                    ?.getDeclaredMethod("print", String::class.java)
                    ?.invoke(p, s) // ERROR 2
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test.kt:4: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                .invoke(p, s) // ERROR 1
                 ~~~~~~
        src/test.kt:10: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                ?.invoke(p, s) // ERROR 2
                  ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test.kt line 4: Annotate with @UsesReflectionToAccessMethod:
        @@ -0,0 +1,6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.api.Printer",
        +    methodName = "print",
        +    parameterTypes = [String::class]
        +)
        Autofix for src/test.kt line 10: Annotate with @UsesReflectionToAccessMethod:
        @@ -0,0 +1 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -6,0 +8,5 @@
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.api.Printer",
        +    methodName = "print",
        +    parameterTypes = [String::class]
        +)
        """
            )
    }

    @Test
    fun testSecondKeepTargetInKotlin() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              import androidx.annotation.keep.UsesReflectionToAccessMethod

              @UsesReflectionToAccessMethod(
                  className = "androidx.api.Printer",
                  methodName = "print",
                  parameterTypes = [String::class]
              )
              fun simpleCall(p: Any, s: String) {
                  val cls = Class.forName("androidx.api.Printer")

                  // Already annotated above
                  cls.getDeclaredMethod("print", String::class.java).invoke(p, s)

                  // Not annotated
                  cls.getDeclaredMethod("close").invoke(p)
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
          src/test/pkg/test.kt:17: Warning: This method calls androidx.api.Printer.close() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
              cls.getDeclaredMethod("close").invoke(p)
                                             ~~~~~~
          0 errors, 1 warnings
          """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 17: Annotate with @UsesReflectionToAccessMethod:
        @@ -4,0 +5,5 @@
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.api.Printer",
        +    methodName = "close",
        +    parameterTypes = []
        +)
          """
            )
    }

    @Test
    fun testAddFirstAnnotationInJava() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              public class JavaUsage {
                  public void simpleCall(Object p, String s) throws Exception {
                      Class<?> cls = Class.forName("androidx.api.Printer");
                      cls.getDeclaredMethod("print", String.class).invoke(p, s);
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
          src/test/pkg/JavaUsage.java:6: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                  cls.getDeclaredMethod("print", String.class).invoke(p, s);
                                                               ~~~~~~
          0 errors, 1 warnings
          """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsage.java line 6: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -3,0 +5,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = {String.class}
        +    )
          """
            )
    }

    @Test
    fun testSecondKeepTargetInJava() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import androidx.annotation.keep.UsesReflectionToAccessMethod;

            public class JavaUsage {
                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "close", parameterTypes = {})
                public void simpleCall(Object p, String s) throws Exception {
                    Class<?> cls = Class.forName("androidx.api.Printer");
                    cls.getDeclaredMethod("print", String.class).invoke(p, s);
                    cls.getDeclaredMethod("close").invoke(p);
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsage.java:9: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                cls.getDeclaredMethod("print", String.class).invoke(p, s);
                                                             ~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsage.java line 9: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = {String.class}
        +    )
        """
            )
    }

    @Test
    @Suppress("LiftReturnOrAssignment")
    fun testPrimitiveIntCast() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        // Example reduced from
                        // androidx/privacysandbox/sdkruntime/client/loader/VersionHandshake.kt
                        """
            package test.pkg

            import java.lang.reflect.InvocationTargetException

            fun perform(classLoader: ClassLoader?): Int {
                val versionsClass =
                    Class.forName("androidx.privacysandbox.sdkruntime.core.Versions", false, classLoader)
                val handShakeMethod = versionsClass.getMethod("handShake", Int::class.javaPrimitiveType)
                try {
                    return handShakeMethod.invoke(null, 0) as Int
                } catch (_: InvocationTargetException) {
                    return -1
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:10: Warning: This method calls androidx.privacysandbox.sdkruntime.core.Versions.handShake() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                return handShakeMethod.invoke(null, 0) as Int
                                       ~~~~~~
        0 errors, 1 warning
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 10: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -4,0 +6,6 @@
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.privacysandbox.sdkruntime.core.Versions",
        +    methodName = "handShake",
        +    parameterTypes = [Int::class],
        +    returnType = Int::class
        +)
        """
            )
    }

    @Test
    fun testInlineConstants() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              public class JavaUsage {
                  private final static String CLASS_NAME = test.pkg.JavaUsage.Inner.CLASS_NAME;
                  public void simpleCall(Object p, String s) throws Exception {
                      Class<?> cls = Class.forName(CLASS_NAME);
                      final String methodName = "print";
                      cls.getDeclaredMethod(methodName, String.class).invoke(p, s);
                  }

                  public static class Inner {
                      public static final String CLASS_NAME = "androidx.api.Printer";
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsage.java:8: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                cls.getDeclaredMethod(methodName, String.class).invoke(p, s);
                                                                ~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsage.java line 8: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -4,0 +6,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = {String.class}
        +    )
        """
            )
    }

    @Test
    fun testConstructor() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              fun callConstructor(): Any? {
                  return try {
                      val impl = Class.forName("androidx.transition.FragmentTransitionSupport")
                      impl.getDeclaredConstructor().newInstance()
                  } catch (ignored: Exception) {
                      null
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:6: Warning: This method calls androidx.transition.FragmentTransitionSupport.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                impl.getDeclaredConstructor().newInstance()
                                              ~~~~~~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 6: Annotate with @UsesReflectionToConstruct:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -2,0 +4,4 @@
        +@UsesReflectionToConstruct(
        +    className = "androidx.transition.FragmentTransitionSupport",
        +    parameterTypes = []
        +)
        """
            )
    }

    @Test
    fun testKotlinInvokeSyntax() {
        // using method(args) instead of method.invoke(args)
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              // From compose/ui/ui-inspection/src/main/java/androidx/compose/ui/inspection/ComposeLayoutInspector.kt
              fun invokeSyntax(instance: Any) {
                  // This is needed to get composables from dialogs etc.
                  val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                  val field = wrapper.getDeclaredMethod("getOwner")
                  field(instance)
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:8: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.getOwner() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            field(instance)
            ~~~~~
        0 errors, 1 warnings
        """
            )
    }

    @Test
    @Suppress("JavaReflectionInvocation")
    fun testParameterTypes() {
        // Test some corner cases for parameter types handling -- arrays,
        // primitives, Java primitive wrappers
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import androidx.annotation.keep.UsesReflectionToAccessMethod;
            import java.util.List;

            public class JavaTest {
                // Printer.print(Integer first, int second, List<String>, String[], String[][])
                public void simpleCall(Object p, String s) throws Exception {
                    Class.forName("androidx.api.Printer")
                            .getDeclaredMethod("print", Integer.class, Integer.TYPE, List.class, String[].class, String[][].class)
                            .invoke(p, s); // ERROR 1
                }

                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "print", parameterTypes = {Integer.class, int.class, List.class, String[].class, String[][].class})
                public void alreadyAnnotated(Object p, String s) throws Exception {
                    Class.forName("androidx.api.Printer")
                            .getDeclaredMethod("print", Integer.class, Integer.TYPE, List.class, String[].class, String[][].class)
                            .invoke(p, s); // OK 1
                }

                // Check that we're treating Integer.TYPE and int.class as the same
                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "print", parameterTypes = {Integer.class, Integer.TYPE, List.class, String[].class, String[][].class})
                public void alreadyAnnotated2(Object p, String s) throws Exception {
                    Class.forName("androidx.api.Printer")
                            .getDeclaredMethod("print", Integer.class, int.class, List.class, String[].class, String[][].class)
                            .invoke(p, s); // OK 2
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            class KotlinTest {
                // Printer.print(Integer first, int second, List<String>, String[], String[][])
                @Throws(Exception::class)
                fun simpleCall(p: Any?, s: String?) {
                    Class.forName("androidx.api.Printer")
                        .getDeclaredMethod(
                            "print",
                            Integer::class.java,
                            Integer.TYPE,
                            MutableList::class.java,
                            Array<String>::class.java,
                            Array<Array<String>>::class.java,
                        )
                        .invoke(p, s) // ERROR 2
                }

                @UsesReflectionToAccessMethod(
                    className = "androidx.api.Printer",
                    methodName = "print",
                    parameterTypes = [Integer::class, Int::class, List::class, Array<String>::class, Array<Array<String>>::class]
                )
                @Throws(Exception::class)
                fun alreadyAnnotated(p: Any?, s: String?) {
                    Class.forName("androidx.api.Printer")
                        .getDeclaredMethod(
                            "print",
                            Integer::class.java,
                            Integer.TYPE,
                            List::class.java,
                            Array<String>::class.java,
                            Array<Array<String>>::class.java,
                        )
                        .invoke(p, s) // OK 3
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:11: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        .invoke(p, s); // ERROR 1
                         ~~~~~~
        src/test/pkg/KotlinTest.kt:18: Warning: This method calls androidx.api.Printer.print() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                    .invoke(p, s) // ERROR 2
                     ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaTest.java line 11: Annotate with @UsesReflectionToAccessMethod:
        @@ -7,0 +8,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = {Integer.class, int.class, List.class, String[].class, String[][].class}
        +    )
        Autofix for src/test/pkg/KotlinTest.kt line 18: Annotate with @UsesReflectionToAccessMethod:
        @@ -3,0 +4 @@
        +import java.util.List
        @@ -6,0 +8,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "print",
        +        parameterTypes = [Integer::class, Int::class, List::class, Array<String>::class, Array<Array<String>>::class]
        +    )
        """
            )
    }

    @Test
    fun testMethodByFilterWithInvocation() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            fun accessMethodWithoutInvocation(instance: Any, content: String) {
                val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                val method = wrapper.declaredMethods.firstOrNull { it.name == "setContent" } ?: return  // ERROR 1
            }

            // Check various other filter methods
            fun accessMethodWithoutInvocation2(instance: Any, content: String) {
                val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                val method2 = wrapper.declaredMethods.find { it.name == "setContent2" } ?: return         // ERROR 2
                val method3 = wrapper.declaredMethods.findLast { it.name == "setContent3" } ?: return     // ERROR 3
                val method4 = wrapper.declaredMethods.first { it.name == "setContent4" } ?: return        // ERROR 4
                val method5 = wrapper.declaredMethods.single { it.name == "setContent5" } ?: return       // ERROR 5
                val method6 = wrapper.declaredMethods.singleOrNull { it.name == "setContent6" } ?: return // ERROR 6
            }

            fun callMethodByFilter(instance: Any, content: String) {
                val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                val method = wrapper.methods.firstOrNull { it.name == "setContent" } ?: return
                method.invoke(instance, content) // ERROR 7
            }

            fun getFieldByFilter(instance: Any, content: String) {
                val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                val method = wrapper.fields.firstOrNull { it.name == "myField" } ?: return
                method.get(instance) // ERROR 8
            }

            fun callConstructorByFilter(instance: Any, content: String) {
                val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                val method = wrapper.constructors.firstOrNull { it.parameters.size == 1 } ?: return
                method.newInstance(instance, content) // ERROR 9
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:5: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method = wrapper.declaredMethods.firstOrNull { it.name == "setContent" } ?: return  // ERROR 1
                                                               ~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:11: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent2() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method2 = wrapper.declaredMethods.find { it.name == "setContent2" } ?: return         // ERROR 2
                                                         ~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:12: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent3() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method3 = wrapper.declaredMethods.findLast { it.name == "setContent3" } ?: return     // ERROR 3
                                                             ~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:13: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent4() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method4 = wrapper.declaredMethods.first { it.name == "setContent4" } ?: return        // ERROR 4
                                                          ~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:14: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent5() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method5 = wrapper.declaredMethods.single { it.name == "setContent5" } ?: return       // ERROR 5
                                                           ~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:15: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent6() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method6 = wrapper.declaredMethods.singleOrNull { it.name == "setContent6" } ?: return // ERROR 6
                                                                 ~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/test.kt:21: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            method.invoke(instance, content) // ERROR 7
                   ~~~~~~
        src/test/pkg/test.kt:26: Warning: This method references androidx.compose.ui.platform.WrappedComposition.* reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
            val method = wrapper.fields.firstOrNull { it.name == "myField" } ?: return
                                 ~~~~~~
        src/test/pkg/test.kt:32: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
            val method = wrapper.constructors.firstOrNull { it.parameters.size == 1 } ?: return
                                 ~~~~~~~~~~~~
        0 errors, 9 warnings
        """
            )
    }

    @Test
    fun testNoArgsInFix() {
        // If we access method/field without invoking, then we don't know the signature so make
        // sure the annotation leaves it out
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              fun accessMethodWithoutInvocation(instance: Any, content: String) {
                  val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                  val method = wrapper.declaredMethods.firstOrNull { it.name == "setContent" } ?: return  // ERROR 1
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:5: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.setContent() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val method = wrapper.declaredMethods.firstOrNull { it.name == "setContent" } ?: return  // ERROR 1
                                                               ~~~~~~~~~~~~~~~~~~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 5: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -2,0 +4,4 @@
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.compose.ui.platform.WrappedComposition",
        +    methodName = "setContent"
        +)
        """
            )
    }

    @Test
    fun testInvokeFirstConstructor() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              fun callMethodByFilter(instance: Any) {
                  val wrapper = Class.forName("androidx.compose.ui.platform.WrappedComposition")
                  val instanceA = wrapper.constructors.first().newInstance()
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:5: Warning: This method calls androidx.compose.ui.platform.WrappedComposition.<init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
            val instanceA = wrapper.constructors.first().newInstance()
                                    ~~~~~~~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 5: Annotate with @UsesReflectionToConstruct:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -2,0 +4,3 @@
        +@UsesReflectionToConstruct(
        +    className = "androidx.compose.ui.platform.WrappedComposition"
        +)
        """
            )
    }

    @Test
    fun testCompanionObjectAccess() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              import android.content.Context

              // From compose/ui/ui-inspection/src/main/java/androidx/compose/ui/inspection/ComposeLayoutInspector.kt
              private fun hotReload2(context: Context) {
                  val hotReload = Class.forName("androidx.compose.runtime.HotReloader")
                  val companion = hotReload.getField("Companion").get(null)
                  val save = companion.javaClass.getDeclaredMethod("saveStateAndDispose", Any::class.java)
                  val load = companion.javaClass.getDeclaredMethod("loadStateAndCompose", Any::class.java)
                  save.isAccessible = true
                  load.isAccessible = true
                  // Add a context parameter even though it is not currently used.
                  // (It was required in earlier versions of the Compose runtime.)
                  val state = save(companion, context)
                  load(companion, state)
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:8: Warning: This method references androidx.compose.runtime.HotReloader.Companion reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
            val companion = hotReload.getField("Companion").get(null)
                                                            ~~~
        src/test/pkg/test.kt:15: Warning: This method calls saveStateAndDispose() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            val state = save(companion, context)
                        ~~~~
        src/test/pkg/test.kt:16: Warning: This method calls loadStateAndCompose() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            load(companion, state)
            ~~~~
        0 errors, 3 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 8: Annotate with @UsesReflectionToAccessField:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField
        @@ -5,0 +7,4 @@
        +@UsesReflectionToAccessField(
        +    className = "androidx.compose.runtime.HotReloader",
        +    fieldName = "Companion"
        +)
        Fix for src/test/pkg/test.kt line 15: Annotate with @UsesReflectionToAccessMethod:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -5,0 +7,5 @@
        +@UsesReflectionToAccessMethod(
        +    className = [TODO()]|,
        +    methodName = "saveStateAndDispose",
        +    parameterTypes = [Object::class]
        +)
        Fix for src/test/pkg/test.kt line 16: Annotate with @UsesReflectionToAccessMethod:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -5,0 +7,5 @@
        +@UsesReflectionToAccessMethod(
        +    className = [TODO()]|,
        +    methodName = "loadStateAndCompose",
        +    parameterTypes = [Object::class]
        +)
        """
            )
    }

    @Test
    fun testDynamicClassJava() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              import android.content.Context;
              import java.lang.reflect.Method;

              public class JavaUsages2 {
                  public void hotReload(Context context) throws Exception {
                      Class<?> hotReload = Class.forName("androidx.compose.runtime.HotReloader");
                      Object companion = hotReload.getField("Companion").get(null);
                      Method save = companion.getClass().getDeclaredMethod("saveStateAndDispose", Object.class);
                      save.setAccessible(true);
                      save.invoke(companion, context);
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsages2.java:9: Warning: This method references androidx.compose.runtime.HotReloader.Companion reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                Object companion = hotReload.getField("Companion").get(null);
                                                                   ~~~
        src/test/pkg/JavaUsages2.java:12: Warning: This method calls saveStateAndDispose() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                save.invoke(companion, context);
                     ~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsages2.java line 9: Annotate with @UsesReflectionToAccessField:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField;
        @@ -6,0 +8,4 @@
        +    @UsesReflectionToAccessField(
        +        className = "androidx.compose.runtime.HotReloader",
        +        fieldName = "Companion"
        +    )
        Fix for src/test/pkg/JavaUsages2.java line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -6,0 +8,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "[TODO]|",
        +        methodName = "saveStateAndDispose",
        +        parameterTypes = {Object.class}
        +    )
        """
            )
    }

    @Test
    fun testReflectionOnFieldInitializerKotlin() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg
            // From compose/runtime/runtime/src/nonEmulatorJvmTest/kotlin/androidx/compose/runtime/reflect/ComposableMethodTest.kt
            class ComposableMethodTest {
                private val clazz = Class.forName("androidx.compose.runtime.reflect.ComposableMethodTestKt")
                private val wrapperClazz = Class.forName("androidx.compose.runtime.reflect.ComposablesWrapper")
                private val composable = clazz.declaredMethods.find { it.name == "composableFunction" }!!
                private val composableMethod =
                    wrapperClazz.declaredMethods.find { it.name == "composableMethod" }!!
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                // NOTE: Here we're also flagging the fields that look up a class. Here
                // we know it's a private field that is ONLY used for method lookup, so I
                // shouldn't need to annotate these to keep the whole class. But we don't
                // currently analyze the whole class to look for this. Should we?
                """
        src/test/pkg/ComposableMethodTest.kt:4: Warning: This code calls androidx.compose.runtime.reflect.ComposableMethodTestKt reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            private val clazz = Class.forName("androidx.compose.runtime.reflect.ComposableMethodTestKt")
                                      ~~~~~~~
        src/test/pkg/ComposableMethodTest.kt:5: Warning: This code calls androidx.compose.runtime.reflect.ComposablesWrapper reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            private val wrapperClazz = Class.forName("androidx.compose.runtime.reflect.ComposablesWrapper")
                                             ~~~~~~~
        src/test/pkg/ComposableMethodTest.kt:6: Warning: This code calls androidx.compose.runtime.reflect.ComposableMethodTestKt.composableFunction() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            private val composable = clazz.declaredMethods.find { it.name == "composableFunction" }!!
                                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/pkg/ComposableMethodTest.kt:8: Warning: This code calls androidx.compose.runtime.reflect.ComposablesWrapper.composableMethod() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                wrapperClazz.declaredMethods.find { it.name == "composableMethod" }!!
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        0 errors, 4 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/ComposableMethodTest.kt line 6: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -5,0 +7,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.compose.runtime.reflect.ComposableMethodTestKt",
        +        methodName = "composableFunction"
        +    )
        Autofix for src/test/pkg/ComposableMethodTest.kt line 8: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -6,0 +8,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.compose.runtime.reflect.ComposablesWrapper",
        +        methodName = "composableMethod"
        +    )
        """
            )
    }

    @Test
    fun testReflectionOnFieldInitializer_AlreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            class ComposableMethodTest {
                @Suppress("ReflectionAnnotation")
                private val wrapperClazz = Class.forName("androidx.compose.runtime.reflect.ComposablesWrapper")
                @UsesReflectionToAccessMethod(
                    className = "androidx.compose.runtime.reflect.ComposablesWrapper",
                    methodName = "composableMethod",
                )
                private val composableMethod =
                    wrapperClazz.declaredMethods.find { it.name == "composableMethod" }!!
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAccessClass() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              fun accessClass(): Class<*> {
                  return Class.forName("test.pkg.Something")
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test.kt:2: Warning: This method calls test.pkg.Something reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            return Class.forName("test.pkg.Something")
                         ~~~~~~~
        0 errors, 1 warning
        """
            )
            // No fix since we don't know specific method
            .expectFixDiffs("")
    }

    @Test
    fun testAccessClassAlreadyAnnotated() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "test.pkg.Something",
                methodName = "*"
            )
            fun accessClass(): Class<*> {
                return Class.forName("test.pkg.Something")
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAccessWithoutCall() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            package test.pkg

            @Suppress("PrivateApi", "DiscouragedPrivateApi")
            fun callReflectively(): Boolean {
                try {
                    val webViewFactoryClass = Class.forName("androidx.webkit.WebViewFactory")
                    val providerInstanceField =
                        webViewFactoryClass.getDeclaredField("sProviderInstance")
                    providerInstanceField.isAccessible = true
                    return providerInstanceField[null] != null
                } catch (e: Exception) {
                    return false
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/test.kt:8: Warning: This method references androidx.webkit.WebViewFactory.sProviderInstance reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                    webViewFactoryClass.getDeclaredField("sProviderInstance")
                                        ~~~~~~~~~~~~~~~~
        0 errors, 1 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 8: Annotate with @UsesReflectionToAccessField:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField
        @@ -2,0 +4,4 @@
        +@UsesReflectionToAccessField(
        +    className = "androidx.webkit.WebViewFactory",
        +    fieldName = "sProviderInstance"
        +)
        """
            )
    }

    @Test
    fun testMissingAnnotation() {
        // One method already annotated, but the other one is not
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import androidx.annotation.keep.UsesReflectionToAccessMethod;

            public class JavaUsage {
                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "print", parameterTypes = {String.class})
                public void simpleCall(Object p, String s) throws Exception {
                    Class<?> cls = Class.forName("androidx.api.Printer");
                    // Already annotated above
                    cls.getDeclaredMethod("print", String.class).invoke(p, s);
                    // Not annotated
                    cls.getDeclaredMethod("close").invoke(p);
                }

                @UsesReflectionToAccessMethod(className = "androidx.api.Printer", methodName = "print", parameterTypes = {String.class})
                public void simpleCall2(Object p, String s) throws Exception {
                    Class<?> cls = Class.forName("androidx.api.Printer");
                    // Already annotated above
                    cls.getDeclaredMethod("print", String.class).invoke(p, s);
                    // Not annotated
                    cls.getDeclaredMethod("close").invoke(p);
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg

            import androidx.annotation.keep.UsesReflectionToAccessMethod

            @UsesReflectionToAccessMethod(
                className = "androidx.api.Printer",
                methodName = "print",
                parameterTypes = [String::class]
            )
            fun simpleCall(p: Any, s: String) {
                val cls = Class.forName("androidx.api.Printer")

                // Already annotated above
                cls.getDeclaredMethod("print", String::class.java).invoke(p, s)

                // Not annotated
                cls.getDeclaredMethod("close").invoke(p)
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsage.java:12: Warning: This method calls androidx.api.Printer.close() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                cls.getDeclaredMethod("close").invoke(p);
                                               ~~~~~~
        src/test/pkg/JavaUsage.java:21: Warning: This method calls androidx.api.Printer.close() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                cls.getDeclaredMethod("close").invoke(p);
                                               ~~~~~~
        src/test/pkg/test.kt:17: Warning: This method calls androidx.api.Printer.close() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
            cls.getDeclaredMethod("close").invoke(p)
                                           ~~~~~~
        0 errors, 3 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/JavaUsage.java line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "close",
        +        parameterTypes = {}
        +    )
        Autofix for src/test/pkg/JavaUsage.java line 21: Annotate with @UsesReflectionToAccessMethod:
        @@ -14,0 +15,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "androidx.api.Printer",
        +        methodName = "close",
        +        parameterTypes = {}
        +    )
        Autofix for src/test/pkg/test.kt line 17: Annotate with @UsesReflectionToAccessMethod:
        @@ -4,0 +5,5 @@
        +@UsesReflectionToAccessMethod(
        +    className = "androidx.api.Printer",
        +    methodName = "close",
        +    parameterTypes = []
        +)
        """
            )
    }

    @Test
    fun testField3() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg
              fun accessField(o: Any) {
                  Class.forName("androidx.api.Printer")
                      .getDeclaredField("SPOOL_SIZE")
                      .get(o)
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
          src/test/pkg/test.kt:5: Warning: This method references androidx.api.Printer.SPOOL_SIZE reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                  .get(o)
                   ~~~
          0 errors, 1 warnings
          """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/test.kt line 5: Annotate with @UsesReflectionToAccessField:
        @@ -1,0 +2,5 @@
        +import androidx.annotation.keep.UsesReflectionToAccessField
        +@UsesReflectionToAccessField(
        +    className = "androidx.api.Printer",
        +    fieldName = "SPOOL_SIZE"
        +)
          """
            )
    }

    @Test
    fun testPlatform() {
        // When accessing platform APIs we don't want @UsesReflection annotations
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              @Suppress("PrivateApi", "DiscouragedPrivateApi")
              fun callsPlatformReflectively(): Boolean {
                  // No warning here since we don't shrink the platform
                  try {
                      val webViewFactoryClass = Class.forName("android.webkit.WebViewFactory") // OK
                      val providerInstanceField =
                          webViewFactoryClass.getDeclaredField("sProviderInstance") // OK
                      providerInstanceField.isAccessible = true
                      return providerInstanceField[null] != null
                  } catch (e: Exception) {
                      return false
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testInit() {
        // Make sure we can attach annotations outside normal methods
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import java.lang.reflect.Field;

            public class JavaUsages {
                static {
                    for (Field field : MyClass.class.getDeclaredFields()) { // ERROR 1
                        System.out.println(field.getName());
                    }
                }

                Field[] fields = MyClass.class.getDeclaredFields(); // ERROR 2
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.pkg;
            public class MyClass {}
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaUsages.java:7: Warning: This code references test.pkg.MyClass.* reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                for (Field field : MyClass.class.getDeclaredFields()) { // ERROR 1
                                                 ~~~~~~~~~~~~~~~~~
        src/test/pkg/JavaUsages.java:12: Warning: This code references test.pkg.MyClass.* reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
            Field[] fields = MyClass.class.getDeclaredFields(); // ERROR 2
                                           ~~~~~~~~~~~~~~~~~
        0 errors, 2 warnings
        """
            )
            // No fix since we don't know specific method
            .expectFixDiffs("")
    }

    @Test
    fun testJavaFieldPrinter() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              import java.lang.reflect.Field;

              public class MyFieldValuePrinter {

                  /** @noinspection ClassEscapesDefinedScope*/
                  public void printFieldValues(PrintableFieldInterface objectWithFields) throws Exception {
                      for (Field field : objectWithFields.getClass().getDeclaredFields()) {
                          System.out.println(field.getName() + " = " + field.get(objectWithFields));
                      }
                  }

                  class PrintableFieldInterface {
                      public static final int CONSTANT = 42;
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/MyFieldValuePrinter.java:9: Warning: This method references * reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                for (Field field : objectWithFields.getClass().getDeclaredFields()) {
                                                               ~~~~~~~~~~~~~~~~~
        0 errors, 1 warning
        """
            )
            // No fix since we don't know specific method
            .expectFixDiffs("")
    }

    @Test
    fun testClassInSamePackage() {
        // Make sure our import cleanup machinery for the command line doesn't optimize out the
        // package prefix in the keep target annotation if it's the same as the package
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            public class MyHiddenMethodCaller {
                public void callHiddenMethod(BaseClass base) throws Exception {
                    Class.forName("test.pkg.MyHiddenMethodCaller.BaseClass").getDeclaredMethod("hiddenMethod").invoke(base);
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
          src/test/pkg/MyHiddenMethodCaller.java:5: Warning: This method calls test.pkg.MyHiddenMethodCaller.BaseClass.hiddenMethod() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                  Class.forName("test.pkg.MyHiddenMethodCaller.BaseClass").getDeclaredMethod("hiddenMethod").invoke(base);
                                                                                                             ~~~~~~
          0 errors, 1 warnings
          """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/MyHiddenMethodCaller.java line 5: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -3,0 +5,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.MyHiddenMethodCaller.BaseClass",
        +        methodName = "hiddenMethod",
        +        parameterTypes = {}
        +    )
          """
            )
    }

    @Test
    fun testSuppressWithDotClass() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              public class MyHiddenMethodCaller {
                  public void callHiddenMethod(BaseClass base) throws Exception {
                      BaseClass.class.getDeclaredMethod("hiddenMethod").invoke(base);
                  }

                  public static class BaseClass {
                      public void hiddenMethod() {}
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
          src/test/pkg/MyHiddenMethodCaller.java:5: Warning: This method calls test.pkg.MyHiddenMethodCaller.BaseClass.hiddenMethod() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                  BaseClass.class.getDeclaredMethod("hiddenMethod").invoke(base);
                                                                    ~~~~~~
          0 errors, 1 warnings
          """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/MyHiddenMethodCaller.java line 5: Annotate with @UsesReflectionToAccessMethod:
        @@ -1,0 +2 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -3,0 +5,5 @@
        +    @UsesReflectionToAccessMethod(
        +        classConstant = MyHiddenMethodCaller.BaseClass.class,
        +        methodName = "hiddenMethod",
        +        parameterTypes = {}
        +    )
        """
            )
    }

    @Test
    fun testAlreadyDeclaredWithClassConstantJava() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
              package test.pkg;

              import androidx.annotation.keep.UsesReflectionToAccessMethod;

              public class MyHiddenMethodCaller {
                  @UsesReflectionToAccessMethod(classConstant = BaseClass.class, methodName = "hiddenMethod", parameterTypes = {})
                  public void callHiddenMethod(BaseClass base) throws Exception {
                      base.getClass().getDeclaredMethod("hiddenMethod").invoke(base);
                  }

                  public static class BaseClass {
                      public void hiddenMethod() {
                      }
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAlreadyDeclaredWithClassConstantKotlin() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
              package test.pkg

              import androidx.annotation.keep.UsesReflectionToAccessMethod

              class MyHiddenMethodCaller {
                  @UsesReflectionToAccessMethod(
                      classConstant = BaseClass::class,
                      methodName = "hiddenMethod",
                      parameterTypes = []
                  )
                  fun callHiddenMethod(base: BaseClass) {
                      base.javaClass.getDeclaredMethod("hiddenMethod").invoke(base)
                  }

                  @UsesReflectionToAccessMethod(
                      classConstant = BaseClass::class,
                      methodName = "hiddenMethod",
                      parameterTypes = []
                  )
                  fun callHiddenMethodWithFullyQualifiedClassConstant(base: BaseClass) {
                      base.javaClass.getDeclaredMethod("hiddenMethod").invoke(base)
                  }

                  class BaseClass {
                      fun hiddenMethod() {}
                  }
              }
              """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expectClean()
    }

    @Test
    @Suppress("unchecked")
    fun testParametersPassedIn() {
        // Make sure that if the parameter is a class array, we don't conclude the
        // parameter type is a class array
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import android.content.Context;

            import java.lang.reflect.Constructor;

            public class JavaParameterTypes {
                private Context mContext;
                private <T> T newInstance(String className, Class<?>[] constructorSignature,
                                          Object[] arguments)  throws Exception {
                    Class<?> clazz = Class.forName(className, false, mContext.getClassLoader());
                    Constructor<?> constructor = clazz.getConstructor(constructorSignature);
                    constructor.setAccessible(true);
                    return (T) constructor.newInstance(arguments);
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg

            import android.content.Context
            import java.lang.reflect.Constructor

            class KotlinFieldType {
                private val mContext: Context? = null

                @Throws(Exception::class)
                private fun <T> newInstance(
                    className: String, constructorSignature: Array<Class<*>?>,
                    arguments: Array<Any?>
                ): T? {
                    val clazz = Class.forName(className, false, mContext.getClassLoader())
                    val constructor: Constructor<*> = clazz.getConstructor(*constructorSignature)
                    constructor.setAccessible(true)
                    return constructor.newInstance(*arguments) as T
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaParameterTypes.java:14: Warning: This method calls <init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                return (T) constructor.newInstance(arguments);
                                       ~~~~~~~~~~~
        src/test/pkg/KotlinFieldType.kt:17: Warning: This method calls <init>() reflectively, so it should be annotated with @UsesReflectionToConstruct(...) [ReflectionAnnotation]
                return constructor.newInstance(*arguments) as T
                                   ~~~~~~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for src/test/pkg/JavaParameterTypes.java line 14: Annotate with @UsesReflectionToConstruct:
        @@ -4,0 +5 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct;
        @@ -8,0 +10,3 @@
        +    @UsesReflectionToConstruct(
        +        className = "[TODO]|"
        +    )
        Fix for src/test/pkg/KotlinFieldType.kt line 17: Annotate with @UsesReflectionToConstruct:
        @@ -3,0 +4 @@
        +import androidx.annotation.keep.UsesReflectionToConstruct
        @@ -8,0 +10,3 @@
        +    @UsesReflectionToConstruct(
        +        className = [TODO()]|
        +    )
        """
            )
    }

    @Test
    fun testFieldType() {
        // Make sure we don't assume the class is the type of the holder object
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        // Example from
                        // appcompat/appcompat/src/main/java/androidx/appcompat/view/menu/MenuItemWrapperICS.java
                        """
            package test.pkg;

            import java.lang.reflect.Method;

            public class JavaFieldType {
                private SupportMenuItem mWrappedObject = null;
                private Method mSetExclusiveCheckableMethod;
                public void setExclusiveCheckable(boolean checkable) {
                    try {
                        if (mSetExclusiveCheckableMethod == null) {
                            mSetExclusiveCheckableMethod = mWrappedObject.getClass()
                                    .getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE);
                        }
                        mSetExclusiveCheckableMethod.invoke(mWrappedObject, checkable);
                    } catch (Exception ignore) {
                    }
                }
            }
            """
                    )
                    .indented(),
                kotlin(
                        """
            package test.pkg

            import java.lang.Boolean
            import java.lang.reflect.Method

            class KotlinFieldType {
                private val mWrappedObject: SupportMenuItem? = null
                private var mSetExclusiveCheckableMethod: Method? = null
                fun setExclusiveCheckable(checkable: Boolean) {
                    try {
                        if (mSetExclusiveCheckableMethod == null) {
                            mSetExclusiveCheckableMethod = mWrappedObject!!.javaClass
                                .getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE)
                        }
                        mSetExclusiveCheckableMethod!!.invoke(mWrappedObject, checkable)
                    } catch (ignore: Exception) {
                    }
                }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package test.pkg;
            class SupportMenuItem {
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaFieldType.java:12: Warning: This method calls setExclusiveCheckable() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                                .getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE);
                                 ~~~~~~~~~~~~~~~~~
        src/test/pkg/KotlinFieldType.kt:13: Warning: This method calls setExclusiveCheckable() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                            .getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE)
                             ~~~~~~~~~~~~~~~~~
        0 errors, 2 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for src/test/pkg/JavaFieldType.java line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -7,0 +9,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "[TODO]|",
        +        methodName = "setExclusiveCheckable",
        +        parameterTypes = {}
        +    )
        Fix for src/test/pkg/KotlinFieldType.kt line 13: Annotate with @UsesReflectionToAccessMethod:
        @@ -2,0 +3 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod
        @@ -8,0 +10,5 @@
        +    @UsesReflectionToAccessMethod(
        +        className = [TODO()]|,
        +        methodName = "setExclusiveCheckable",
        +        parameterTypes = []
        +    )
        """
            )
    }

    @Test
    fun testKotlinReflect() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                kotlin(
                        """
            @file:Suppress("unused", "UnusedVariable")

            package test.pkg

            import android.content.Context
            import androidx.annotation.keep.UsesReflectionToAccessField
            import androidx.annotation.keep.UsesReflectionToAccessMethod
            import kotlin.reflect.KCallable
            import kotlin.reflect.KClass
            import kotlin.reflect.full.declaredFunctions
            import kotlin.reflect.full.declaredMemberProperties
            import kotlin.reflect.full.declaredMembers

            class KotlinReflect {
                fun test() {
                    val field = Context::class.members.find { it.name == "BIND_NOT_FOREGROUND" } // ERROR 1
                }

                fun test2() {
                    val field =
                        Context::class.declaredMembers.find { it.name == "BIND_NOT_FOREGROUND" } // ERROR 2
                }

                fun test3() {
                    val method: KCallable<*>? =
                        Context::class.members.find { // ERROR 3
                            it.name == "getColor" &&
                                    it.isFinal && it.parameters.size == 1 && it.parameters[0].type == Int
                        }
                    method?.call(5)
                }

                fun test4() {
                    val method: KCallable<*>? =
                        Context::class.declaredFunctions.find { // ERROR 4
                            it.name == "getColor"
                                    && it.isFinal
                                    && it.parameters.size == 1
                                    && it.parameters[0].type == Int
                        }
                    method?.call(5)
                }

                private fun test5() {
                    loadConstantsFromEnclosedClasses(AbsoluteAlignment::class.java)
                }

                private fun loadConstantsFromEnclosedClasses(javaClass: Class<*>) {
                    loadConstantsFromObjectInstance(javaClass.kotlin)
                    javaClass.declaredClasses.forEach { loadConstantsFromEnclosedClasses(it) }
                }

                private fun loadConstantsFromObjectInstance(kClass: KClass<*>) {
                    try {
                        val instance = kClass.objectInstance ?: return
                        kClass.declaredMemberProperties // ERROR 5
                            .asSequence()
                            .filter { it.isFinal && !it.isLateinit }
                    } catch (_: Throwable) {
                    }
                }

                @UsesReflectionToAccessField(
                    className = "my.Class",
                    fieldName = "*"
                )
                private fun loadConstantsFromObjectInstance2(kClass: KClass<*>) {
                    try {
                        val instance = kClass.objectInstance ?: return
                        kClass.declaredMemberProperties // OK 1
                            .asSequence()
                            .filter { it.isFinal && !it.isLateinit }
                    } catch (_: Throwable) {
                    }
                }

                @UsesReflectionToAccessMethod(
                    className = "my.Class",
                    methodName = "getColor"
                )
                fun test3_alreadyAnnotated() {
                    val method: KCallable<*>? =
                        Context::class.members.find { // OK 2
                            it.name == "getColor" &&
                                    it.isFinal && it.parameters.size == 1 && it.parameters[0].type == Int
                        }
                    method?.call(5)
                }

                fun testUnrelatedMethods() {
                    this.members.find { it == "BIND_NOT_FOREGROUND" } // OK 3
                    this.declaredMembers.find { it == "BIND_NOT_FOREGROUND" } // OK 4
                }

                private val members: List<String> = emptyList()
                private val declaredMembers: List<String> = emptyList()
            }

            class AbsoluteAlignment
            """
                    )
                    .indented(),
                // Stubs for reflect
                kotlin(
                        """
            /* HIDE-FROM-DOCUMENTATION */
            @file:JvmName("KClasses")
            package kotlin.reflect.full

            import kotlin.reflect.KCallable
            import kotlin.reflect.KClass
            import kotlin.reflect.KFunction
            import kotlin.reflect.KProperty1

            val KClass<*>.declaredMembers: Collection<KCallable<*>> get() = emptyList()
            val KClass<*>.functions: Collection<KFunction<*>> get() = emptyList()
            val KClass<*>.declaredMemberFunctions: Collection<KFunction<*>> get() = emptyList()
            val KClass<*>.declaredFunctions: Collection<KFunction<*>> get() = emptyList()
            val <T : Any> KClass<T>.memberProperties: Collection<KProperty1<T, *>> get() = emptyList()
            val <T : Any> KClass<T>.declaredMemberProperties: Collection<KProperty1<T, *>> get() = emptyList()
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .run()
            .expect(
                """
        src/test/pkg/KotlinReflect.kt:16: Warning: This method calls code reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                val field = Context::class.members.find { it.name == "BIND_NOT_FOREGROUND" } // ERROR 1
                                           ~~~~~~~
        src/test/pkg/KotlinReflect.kt:21: Warning: This method calls code reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                    Context::class.declaredMembers.find { it.name == "BIND_NOT_FOREGROUND" } // ERROR 2
                                   ~~~~~~~~~~~~~~~
        src/test/pkg/KotlinReflect.kt:26: Warning: This method calls code reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                    Context::class.members.find { // ERROR 3
                                   ~~~~~~~
        src/test/pkg/KotlinReflect.kt:35: Warning: This method calls code reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                    Context::class.declaredFunctions.find { // ERROR 4
                                   ~~~~~~~~~~~~~~~~~
        src/test/pkg/KotlinReflect.kt:56: Warning: This method calls code reflectively, so it should be annotated with @UsesReflectionToAccessField(...) [ReflectionAnnotation]
                    kClass.declaredMemberProperties // ERROR 5
                           ~~~~~~~~~~~~~~~~~~~~~~~~
        0 errors, 5 warnings
        """
            )
            // No fix since we don't know specific method
            .expectFixDiffs("")
    }

    @Test
    fun testObjectAnimators() {
        lint()
            .issues(KeepRuleDetector.ISSUE)
            .files(
                java(
                        """
            package test.pkg;

            import android.animation.ObjectAnimator;
            import android.animation.PropertyValuesHolder;
            import android.widget.Button;

            @SuppressWarnings("unused")
            public class AnimatorTest {

                public void testObjectAnimator1(Button button) {
                    Object myObject = new MyObject();
                    ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, "prop1", 0, 1, 2, 5);
                    animator1.setDuration(10);
                    animator1.start();
                }

                public void testPropertyHolder() {
                    Object myObject = new MyObject();

                    PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("prop1", 50);
                    PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("prop2", 100f);
                    ObjectAnimator.ofPropertyValuesHolder(myObject, p1, p2).start();
                    ObjectAnimator.ofPropertyValuesHolder(myObject,
                            PropertyValuesHolder.ofInt("prop1", 50),
                            PropertyValuesHolder.ofFloat("prop2", 100f)).start();
                }

                static class MyObject {
                    public int getProp1() { }
                    public void setProp1(int x) { }
                    private void setProp2(float x) { }
                }
            }
            """
                    )
                    .indented(),
                *usesReflectionStubs,
            )
            .issues(ObjectAnimatorDetector.MISSING_KEEP, KeepRuleDetector.ISSUE)
            .run()
            .expect(
                """
        src/test/pkg/AnimatorTest.java:12: Warning: This method calls test.pkg.AnimatorTest.MyObject.getProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, "prop1", 0, 1, 2, 5);
                                                                          ~~~~~~~
        src/test/pkg/AnimatorTest.java:12: Warning: This method calls test.pkg.AnimatorTest.MyObject.setProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, "prop1", 0, 1, 2, 5);
                                                                          ~~~~~~~
        src/test/pkg/AnimatorTest.java:20: Warning: This method calls test.pkg.AnimatorTest.MyObject.getProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("prop1", 50);
                                                                     ~~~~~~~
        src/test/pkg/AnimatorTest.java:20: Warning: This method calls test.pkg.AnimatorTest.MyObject.setProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("prop1", 50);
                                                                     ~~~~~~~
        src/test/pkg/AnimatorTest.java:21: Warning: This method calls test.pkg.AnimatorTest.MyObject.getProp2() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("prop2", 100f);
                                                                       ~~~~~~~
        src/test/pkg/AnimatorTest.java:21: Warning: This method calls test.pkg.AnimatorTest.MyObject.setProp2() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("prop2", 100f);
                                                                       ~~~~~~~
        src/test/pkg/AnimatorTest.java:24: Warning: This method calls test.pkg.AnimatorTest.MyObject.getProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        PropertyValuesHolder.ofInt("prop1", 50),
                                                   ~~~~~~~
        src/test/pkg/AnimatorTest.java:24: Warning: This method calls test.pkg.AnimatorTest.MyObject.setProp1() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        PropertyValuesHolder.ofInt("prop1", 50),
                                                   ~~~~~~~
        src/test/pkg/AnimatorTest.java:25: Warning: This method calls test.pkg.AnimatorTest.MyObject.getProp2() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        PropertyValuesHolder.ofFloat("prop2", 100f)).start();
                                                     ~~~~~~~
        src/test/pkg/AnimatorTest.java:25: Warning: This method calls test.pkg.AnimatorTest.MyObject.setProp2() reflectively, so it should be annotated with @UsesReflectionToAccessMethod(...) [ReflectionAnnotation]
                        PropertyValuesHolder.ofFloat("prop2", 100f)).start();
                                                     ~~~~~~~
        0 errors, 10 warnings
        """
            )
            .expectFixDiffs(
                """
        Autofix for src/test/pkg/AnimatorTest.java line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -9,0 +11,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "getProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 12: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -9,0 +11,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "setProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 20: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "getProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 20: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "setProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 21: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "getProp2"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 21: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "setProp2"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 24: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "getProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 24: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "setProp1"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 25: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "getProp2"
        +    )
        Autofix for src/test/pkg/AnimatorTest.java line 25: Annotate with @UsesReflectionToAccessMethod:
        @@ -5,0 +6 @@
        +import androidx.annotation.keep.UsesReflectionToAccessMethod;
        @@ -16,0 +18,4 @@
        +    @UsesReflectionToAccessMethod(
        +        className = "test.pkg.AnimatorTest.MyObject",
        +        methodName = "setProp2"
        +    )
        """
            )
    }
}

val usesReflectionStubs: Array<TestFile> =
    arrayOf(
        kotlin(
                """
        package androidx.annotation.keep
        import kotlin.reflect.KClass
        @Retention(AnnotationRetention.BINARY)
        @Repeatable
        @Target(
            AnnotationTarget.CLASS,
            AnnotationTarget.FIELD,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.CONSTRUCTOR,
        )
        annotation class UsesReflectionToConstruct(
            val classConstant: KClass<*> = Unspecified::class,
            val className: String = "",
            val parameterTypes: Array<KClass<*>> = [Unspecified::class],
            val parameterTypeNames: Array<String> = [""],
        )
        """
            )
            .indented(),
        kotlin(
                """
        package androidx.annotation.keep
        import kotlin.reflect.KClass
        @Retention(AnnotationRetention.BINARY)
        @Repeatable
        @Target(
            AnnotationTarget.CLASS,
            AnnotationTarget.FIELD,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.CONSTRUCTOR,
        )
        annotation class UsesReflectionToAccessField(
            val classConstant: KClass<*> = Unspecified::class,
            val className: String = "",
            val fieldName: String,
            val fieldType: KClass<*> = Unspecified::class,
            val fieldTypeName: String = "",
        )
        """
            )
            .indented(),
        kotlin(
                """
        package androidx.annotation.keep
        import kotlin.reflect.KClass
        @Retention(AnnotationRetention.BINARY)
        @Repeatable
        @Target(
            AnnotationTarget.CLASS,
            AnnotationTarget.FIELD,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.CONSTRUCTOR,
        )
        annotation class UsesReflectionToAccessMethod(
            val classConstant: KClass<*> = Unspecified::class,
            val className: String = "",
            val methodName: String,
            val parameterTypes: Array<KClass<*>> = [Unspecified::class],
            val parameterTypeNames: Array<String> = [""],
            val returnType: KClass<*> = Unspecified::class,
            val returnTypeName: String = "",
        )
        """
            )
            .indented(),
        kotlin(
                """
        package androidx.annotation.keep
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.CLASS,
            AnnotationTarget.FIELD,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.CONSTRUCTOR,
        )
        annotation class UnconditionallyKeep(
            val shouldPreserveName: Boolean = true
        )
        """
            )
            .indented(),
        kotlin(
                """
        package androidx.annotation.keep
        class Unspecified
        """
            )
            .indented(),
    )
