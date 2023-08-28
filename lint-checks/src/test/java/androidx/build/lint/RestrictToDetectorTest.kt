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

package androidx.build.lint

import androidx.build.lint.RestrictToDetector.Companion.sameLibraryGroupPrefix
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.mavenLibrary
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Project
import java.io.File

class RestrictToDetectorTest : AbstractCheckTest() {
    override fun getDetector(): Detector = RestrictToDetector()

    override fun getIssues(): List<Issue> = listOf(RestrictToDetector.RESTRICTED)

    fun testRestrictToSubClass() {
        val expected =
            """
            src/test/pkg/RestrictToSubclassTest.java:26: Error: Class1.onSomething can only be called from subclasses [RestrictedApiAndroidX]
                        cls.onSomething();         // ERROR: Not from subclass
                            ~~~~~~~~~~~
            src/test/pkg/RestrictToSubclassTest.java:27: Error: Class1.counter can only be accessed from subclasses [RestrictedApiAndroidX]
                        int counter = cls.counter; // ERROR: Not from subclass
                                          ~~~~~~~
            2 errors, 0 warnings
            """

        lint()
            .files(
                java(
                    """
                    package test.pkg;

                    import androidx.annotation.RestrictTo;

                    @SuppressWarnings("ClassNameDiffersFromFileName")
                    public class RestrictToSubclassTest {
                        public static class Class1 {
                            @RestrictTo(RestrictTo.Scope.SUBCLASSES)
                            public void onSomething() {
                            }

                            @RestrictTo(RestrictTo.Scope.SUBCLASSES)
                            public int counter;
                        }

                        public static class SubClass extends Class1 {
                            public void test1() {
                                onSomething(); // OK: Call from subclass
                                int counter = cls.counter; // OK: Reference from subclass
                            }
                        }

                        @SuppressWarnings("MethodMayBeStatic")
                        public static class NotSubClass {
                            public void test2(Class1 cls) {
                                cls.onSomething();         // ERROR: Not from subclass
                                int counter = cls.counter; // ERROR: Not from subclass
                            }
                        }
                    }
                    """
                )
                    .indented(),
                SUPPORT_ANNOTATIONS_JAR
            )
            .run()
            .expect(expected)
    }

    fun testRestrictToGroupId() {
        val project =
            project()
                .files(
                    java(
                        """
                package test.pkg;
                import library.pkg.internal.InternalClass;
                import library.pkg.Library;
                import library.pkg.PrivateClass;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class TestLibrary {
                    public void test() {
                        Library.method(); // OK
                        Library.privateMethod(); // ERROR
                        PrivateClass.method(); // ERROR
                        InternalClass.method(); // ERROR
                    }

                    @Override
                    public method() {
                        super.method(); // ERROR
                    }
                }
                """
                    )
                        .indented(),
                    java(
                        "src/test/java/test/pkg/UnitTestLibrary.java",
                        """
                package test.pkg;
                import library.pkg.PrivateClass;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class UnitTestLibrary {
                    public void test() {
                        PrivateClass.method(); // Not enforced in tests
                    }
                }
                """
                    )
                        .indented(),
                    library,
                    gradle(
                        """
                apply plugin: 'com.android.application'

                dependencies {
                    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'
                }
                """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
        lint()
            .projects(project)
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
            src/main/java/test/pkg/TestLibrary.java:10: Error: Library.privateMethod can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
                    Library.privateMethod(); // ERROR
                            ~~~~~~~~~~~~~
            src/main/java/test/pkg/TestLibrary.java:11: Error: PrivateClass.method can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
                    PrivateClass.method(); // ERROR
                                 ~~~~~~
            src/main/java/test/pkg/TestLibrary.java:12: Error: InternalClass.method can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
                    InternalClass.method(); // ERROR
                                  ~~~~~~
            3 errors, 0 warnings
            """
            )
    }

    fun testRestrictToLibrary() {
        // 120087311: Enforce RestrictTo(LIBRARY) when the API is defined in another project
        val library =
            project()
                .files(
                    java(
                        """
                package com.example.mylibrary;

                import androidx.annotation.RestrictTo;

                public class LibraryCode {
                    // No restriction: any access is fine.
                    public static int FIELD1;

                    // Scoped to same library: accessing from
                    // lib is okay, from app is not.
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    public static int FIELD2;

                    // Scoped to same library group: whether accessing
                    // from app is okay depends on whether they are in
                    // the same library group (=groupId). In this test
                    // project we don't know what they are so there's
                    // no warning generated.
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public static int FIELD3;

                    public static void method1() {
                    }

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    public static void method2() {
                    }

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public static void method3() {
                    }
                }
                """
                    )
                        .indented(),
                    java(
                        """
                package test.pkg;

                import com.example.mylibrary.LibraryCode;

                // Access within the same library -- all OK
                public class LibraryCode2 {
                    public void method() {
                        LibraryCode.method1(); // OK
                        LibraryCode.method2(); // OK
                        LibraryCode.method3(); // OK
                        int f1 =  LibraryCode.FIELD1; // OK
                        int f2 =  LibraryCode.FIELD2; // OK
                        int f3 =  LibraryCode.FIELD3; // OK
                    }
                }
                """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
                .name("lib")
                .type(ProjectDescription.Type.LIBRARY)

        val app =
            project()
                .files(
                    kotlin(
                        """
                package com.example.myapplication

                import com.example.mylibrary.LibraryCode

                fun test() {
                    LibraryCode.method1()
                    LibraryCode.method2()
                    LibraryCode.method3()
                    val f1 = LibraryCode.FIELD1
                    val f2 = LibraryCode.FIELD2
                    val f3 = LibraryCode.FIELD3
                }
                """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
                .dependsOn(library)
                .name("app")

        lint()
            .projects(library, app)
            .run()
            .expect(
                """
            src/com/example/myapplication/test.kt:7: Error: LibraryCode.method2 can only be called from within the same library (lib) [RestrictedApiAndroidX]
                LibraryCode.method2()
                            ~~~~~~~
            src/com/example/myapplication/test.kt:10: Error: LibraryCode.FIELD2 can only be accessed from within the same library (lib) [RestrictedApiAndroidX]
                val f2 = LibraryCode.FIELD2
                                     ~~~~~~
            2 errors, 0 warnings
            """
            )
    }

    fun testHierarchy() {
        val project =
            project()
                .files(
                    java(
                        """
                package test.pkg;
                import library.pkg.PrivateClass;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class TestLibrary1 extends PrivateClass {
                    @Override
                    public void method() {
                        super.method(); // ERROR
                    }
                }
                """
                    )
                        .indented(),
                    java(
                        """
                package test.pkg;
                import library.pkg.PrivateClass;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class TestLibrary2 extends PrivateClass {
                }
                """
                    )
                        .indented(),
                    java(
                        """
                package test.pkg;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class Inheriting1 extends TestLibrary1 {
                    public void test() {
                        method(); // OK -- overridden without annotation
                    }
                }
                """
                    )
                        .indented(),
                    java(
                        """
                package test.pkg;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class Inheriting2 extends TestLibrary2 {
                    public void test() {
                        method(); // ERROR - not overridden, pointing into library
                    }
                }
                """
                    )
                        .indented(),
                    library,
                    gradle(
                        """
                apply plugin: 'com.android.application'

                dependencies {
                    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'
                }
                """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
        lint()
            .projects(project)
            .run()
            .expect(
                """
            src/main/java/test/pkg/Inheriting2.java:6: Error: PrivateClass.method can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
                    method(); // ERROR - not overridden, pointing into library
                    ~~~~~~
            src/main/java/test/pkg/TestLibrary1.java:5: Error: PrivateClass can only be accessed from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
            public class TestLibrary1 extends PrivateClass {
                                              ~~~~~~~~~~~~
            src/main/java/test/pkg/TestLibrary1.java:8: Error: PrivateClass.method can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
                    super.method(); // ERROR
                          ~~~~~~
            src/main/java/test/pkg/TestLibrary2.java:5: Error: PrivateClass can only be accessed from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
            public class TestLibrary2 extends PrivateClass {
                                              ~~~~~~~~~~~~
            4 errors, 0 warnings
            """
            )
    }

    fun testRestrictedInheritedAnnotation() {
        // Regression test for http://b.android.com/230387
        // Ensure that when we perform the @RestrictTo check, we don't incorrectly
        // inherit annotations from the base classes of AppCompatActivity and treat
        // those as @RestrictTo on the whole AppCompatActivity class itself.
        lint()
            .files(
                /*
                Compiled version of these two classes:
                    package test.pkg;
                    import androidx.annotation.RestrictTo;
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public class RestrictedParent {
                    }
                and
                    package test.pkg;
                    public class Parent extends RestrictedParent {
                        public void myMethod() {
                        }
                    }
                 */
                base64gzip(
                    "libs/exploded-aar/my.group.id/mylib/25.0.0-SNAPSHOT/jars/classes.jar",
                    "" +
                        "H4sIAAAAAAAAAAvwZmYRYeDg4GB4VzvRkwEJcDKwMPi6hjjqevq56f87xcDA" +
                        "zBDgzc4BkmKCKgnAqVkEiOGafR39PN1cg0P0fN0++5457eOtq3eR11tX69yZ" +
                        "85uDDK4YP3hapOflq+Ppe7F0FQtnxAvJI9JSUi/Flj5boia2XCujYuk0C1HV" +
                        "tGei2iKvRV8+zf5U9LGIEeyWNZtvhngBbfJCcYspmlvkgbgktbhEvyA7XT8I" +
                        "yCjKTC5JTQlILErNK9FLzkksLp4aGOvN5Chi+/j6tMxZqal2rK7xV+y+RLio" +
                        "iRyatGmWgO2RHdY3blgp7978b/28JrlfjH9XvMh66Cxwg6fY/tze73Mknz3+" +
                        "/Fb2gOaqSJXAbRvyEpsVi/WmmojznPzbrOe8al3twYCCJULbP25QP8T3nrVl" +
                        "iszbjwtOO1uerD8wpXKSoPNVQyWjby925u8WablkfCj/Y4BG8bEJua8tvhzZ" +
                        "OsdnSr35HJ4fM4RbpbWV2xctPGY0ySUu2Es6b0mYyobnBU/bo36VifS7WZmY" +
                        "zZ+aPknWN+mlIX9S4kKnxNuXlSedMZ0ilGj7IFCl43WF3bq5L00Mn809NjW6" +
                        "+L18/p1nsdrtIpd4ptrLnwmYs+cE345Xt8/ec6g4dkjs8EX7EMmy56+OmQl9" +
                        "mT75aMblsyfSNDYvt5xgV8NavVCBsTsnjSttg4PZ97sNrikn1TeavD2l6L/P" +
                        "Y2uqVSu7QWPomoUuGdMmKJltLIr8yQSKpPpfEa8iGBkYfJjwRZIociQhR01q" +
                        "n7//IQeBo/cv1AesjsiX2cmp9u1B4OOjLcGmbpzfl949oFRytszwY3Kl0cMD" +
                        "7B+cJZetzex5l3hvj/nn0+euf8/jf8BVyMGuzviL0Y/zX6/WlL2qFs8XSx7c" +
                        "e3mnypfg0BPtb9P0zoacuT5nzlIr4dczDVZ9sl+YPX2VypGVU5f6xsWLnVxs" +
                        "sGnD9ZZ3z/7G3Vp6jvPh5nuzfPxCWmVMpadrf1RT2vHhx2Z7k8QLav53JKZG" +
                        "zjQ35rn48PPq64yhNuHzYw95rbn3Q/hLYD/zujpZqxdFvbNYvwhs+qSpWxNY" +
                        "/Yd9b7zC1oSQfFl5cErewhTw/BEwCIIYQYHEyCTCgJqvYDkOlClRAUoWRdeK" +
                        "nEFEULTZ4sigyCaA4gg59uRRTDhJOFuhG4bsS1EUw/KYcER/gDcrG0gBCxDy" +
                        "ArVNZgbxABAMMsu2BAAA"
                ),
                java(
                    "" +
                        "package test.pkg;\n" +
                        "\n" +
                        "public class Cls extends Parent {\n" +
                        "    @Override\n" +
                        "    public void myMethod() {\n" +
                        "        super.myMethod();\n" +
                        "    }\n" +
                        "}\n"
                ),
                gradle(
                    "" +
                        "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'\n" +
                        "}"
                ),
                SUPPORT_ANNOTATIONS_JAR
            )
            .run()
            .expectClean()
    }

    fun testRestrictedClassOrInterfaceUsage() {
        lint()
            .files(
                kotlin(
                    """
                package test.pkg

                class MyClass : RestrictedClass()
                """
                ),
                java(
                    """
                package test.pkg;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class MyJavaClass extends RestrictedClass implements RestrictedInterface {
                }
                """
                )
                    .indented(),
                java(
                    "src/androidTest/java/test/pkg/MyTestJavaClass.java",
                    """
                  package test.pkg;

                  @SuppressWarnings("ClassNameDiffersFromFileName")
                  public class MyTestJavaClass extends RestrictedClass {
                  }
                  """
                )
                    .indented(),
                kotlin(
                    """
                package test.pkg

                import androidx.annotation.RestrictTo

                @RestrictTo(RestrictTo.Scope.TESTS)
                open class RestrictedClass
                """
                )
                    .indented(),
                kotlin(
                    """
                package test.pkg

                import androidx.annotation.RestrictTo

                @RestrictTo(RestrictTo.Scope.TESTS)
                interface RestrictedInterface
                """
                )
                    .indented(),
                gradle(
                    """
                android {
                    lintOptions {
                        checkTestSources true
                    }
                }
                """
                )
                    .indented(),
                SUPPORT_ANNOTATIONS_JAR
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
            src/main/kotlin/test/pkg/MyClass.kt:4: Error: RestrictedClass can only be called from tests [RestrictedApiAndroidX]
                            class MyClass : RestrictedClass()
                                            ~~~~~~~~~~~~~~~
            src/main/java/test/pkg/MyJavaClass.java:4: Error: RestrictedClass can only be accessed from tests [RestrictedApiAndroidX]
            public class MyJavaClass extends RestrictedClass implements RestrictedInterface {
                                             ~~~~~~~~~~~~~~~
            src/main/java/test/pkg/MyJavaClass.java:4: Error: RestrictedInterface can only be accessed from tests [RestrictedApiAndroidX]
            public class MyJavaClass extends RestrictedClass implements RestrictedInterface {
                                                                        ~~~~~~~~~~~~~~~~~~~
            3 errors, 0 warnings
            """
            )
    }

    fun test123545341() {
        // Regression test for
        // 123545341: RestrictTo(TESTS) doesn't allow same class to use methods
        // (Note that that test asks for the following not to be an error, but this is
        // deliberate and we're testing the enforcement here)
        lint()
            .files(
                java(
                    """
                package test.pkg;

                import androidx.annotation.RestrictTo;
                import static androidx.annotation.RestrictTo.Scope.TESTS;

                class Outer {
                    private Inner innerInstance;

                    @RestrictTo(TESTS)
                    class Inner {
                        public void method() {
                        }
                    }

                    private void outerMethod() {
                        // This is marked as invalid
                        innerInstance.method();
                    }
                }
                """
                ),
                SUPPORT_ANNOTATIONS_JAR
            )
            .run()
            .expect(
                """
            src/test/pkg/Outer.java:18: Error: Inner.method can only be called from tests [RestrictedApiAndroidX]
                                    innerInstance.method();
                                                  ~~~~~~
            1 errors, 0 warnings
            """
            )
    }

    fun test169255669() {
        // Regression test for 169255669: ClassCastException in RestrictToDetector.
        @Suppress("ConvertSecondaryConstructorToPrimary")
        lint()
            .files(
                kotlin(
                    """
                package test.pkg

                import androidx.annotation.RestrictTo

                class Foo {
                    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
                    constructor()
                }

                @RestrictTo(RestrictTo.Scope.SUBCLASSES)
                val foo = Foo()
                """
                )
                    .indented(),
                SUPPORT_ANNOTATIONS_JAR
            )
            .run()
            .expect(
                """
            src/test/pkg/Foo.kt:11: Error: Foo can only be called from subclasses [RestrictedApiAndroidX]
            val foo = Foo()
                      ~~~
            1 errors, 0 warnings
            """
            )
    }

    fun test169610406() {
        // 169610406: Strange warning from RestrictToDetector for Kotlin property
        //            initialized by constructor call
        lint()
            .files(
                kotlin(
                    """
                package test.pkg

                import androidx.annotation.RestrictTo
                import androidx.annotation.RestrictTo.Scope.SUBCLASSES

                class Foo
                open class Bar {
                    // No use site target specified; Kotlin will take this to refer to the
                    // field only; lint will also interpret this as applying to getters and setters
                    @RestrictTo(SUBCLASSES)
                    val foo1: Foo = Foo()

                    // Field explicitly requested; lint only enforce this on field references, not getters/setters
                    @field:RestrictTo(SUBCLASSES)
                    val foo2: Foo = Foo()

                    // Setter only; don't enforce on getter
                    @set:RestrictTo(SUBCLASSES)
                    var foo3: Foo? = Foo()

                    // Getter only, don't enforce on setter
                    @get:RestrictTo(SUBCLASSES)
                    var foo4: Foo? = Foo()
                }
              """
                )
                    .indented(),
                kotlin(
                    """
                package test.pkg
                class Sub : Bar() {
                    fun test() {
                        val test = foo1 // OK 1
                        println(foo1)   // OK 2
                        println(foo2)   // OK 3
                        println(foo3)   // OK 4
                        println(foo5)   // OK 5
                    }
                }
                class NotSub(private val bar: Bar) {
                    fun test() {
                        val test = bar.foo1  // WARN 1
                        println(bar.foo1)    // WARN 2
                        val test2 = bar.foo2 // OK 6
                        println(bar.foo2)    // OK 7
                        val test3 = bar.foo3 // OK 8
                        println(bar.foo3)    // OK 9
                        bar.foo3 = null      // WARN 3
                        println(bar.foo4)    // WARN 4
                        bar.foo4 = null      // OK 10
                    }
                }
                """
                )
                    .indented(),
                SUPPORT_ANNOTATIONS_JAR
            )
            .run()
            .expect(
                """
            src/test/pkg/Sub.kt:13: Error: Bar.getFoo1 can only be called from subclasses [RestrictedApiAndroidX]
                    val test = bar.foo1  // WARN 1
                                   ~~~~
            src/test/pkg/Sub.kt:14: Error: Bar.getFoo1 can only be called from subclasses [RestrictedApiAndroidX]
                    println(bar.foo1)    // WARN 2
                                ~~~~
            src/test/pkg/Sub.kt:19: Error: Bar.setFoo3 can only be called from subclasses [RestrictedApiAndroidX]
                    bar.foo3 = null      // WARN 3
                        ~~~~
            src/test/pkg/Sub.kt:20: Error: Bar.getFoo4 can only be called from subclasses [RestrictedApiAndroidX]
                    println(bar.foo4)    // WARN 4
                                ~~~~
            4 errors, 0 warnings
            """
            )
    }

    companion object {
        val library: TestFile =
            mavenLibrary(
                "my.group.id:mylib:25.0.0-SNAPSHOT",
                stubSources =
                listOf(
                    java(
                        """
                        package library.pkg;

                        import androidx.annotation.RestrictTo;

                        public class Library {
                            public static void method() {
                            }

                            @RestrictTo(RestrictTo.Scope.GROUP_ID)
                            public static void privateMethod() {
                            }
                        }
                        """
                    )
                        .indented(),
                    java(
                        """
                        package library.pkg;

                        import androidx.annotation.RestrictTo;

                        @RestrictTo(RestrictTo.Scope.GROUP_ID)
                        public class PrivateClass {
                            public static void method() {
                            }
                        }
                        """
                    )
                        .indented(),
                    java(
                        """
                        package library.pkg.internal;

                        public class InternalClass {
                            public static void method() {
                            }
                        }
                        """
                    )
                        .indented(),
                    java(
                        """
                        @RestrictTo(RestrictTo.Scope.GROUP_ID)
                        package library.pkg.internal;

                        import androidx.annotation.RestrictTo;
                        """
                    )
                        .indented()
                ),
                compileOnly = listOf(SUPPORT_ANNOTATIONS_JAR)
            )
    }

    fun testRestrictToLibraryViaGradleModel() {
        val library =
            project()
                .files(
                    java(
                        """
                package com.example.mylibrary;

                import androidx.annotation.RestrictTo;

                public class LibraryCode {
                    // No restriction: any access is fine.
                    public static int FIELD1;

                    // Scoped to same library: accessing from
                    // lib is okay, from app is not.
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    public static int FIELD2;

                    // Scoped to same library group: whether accessing
                    // from app is okay depends on whether they are in
                    // the same library group (=groupId). In this test
                    // project we don't know what they are so there's
                    // no warning generated.
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public static int FIELD3;

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
                    public static int FIELD4;

                    public static void method1() {
                    }

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    public static void method2() {
                    }

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public static void method3() {
                    }

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
                    public static void method4() {
                    }
                }
                """
                    )
                        .indented(),
                    java(
                        """
                package test.pkg;

                import com.example.mylibrary.LibraryCode;

                // Access within the same library -- all OK
                public class LibraryCode2 {
                    public void method() {
                        LibraryCode.method1(); // OK
                        LibraryCode.method2(); // OK
                        LibraryCode.method3(); // OK
                        LibraryCode.method4(); // OK
                        int f1 =  LibraryCode.FIELD1; // OK
                        int f2 =  LibraryCode.FIELD2; // OK
                        int f3 =  LibraryCode.FIELD3; // OK
                        int f4 =  LibraryCode.FIELD4; // OK
                    }
                }
                """
                    )
                        .indented(),
                    gradle(
                        """
                    apply plugin: 'com.android.library'
                    group=test.pkg.library
                    """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
                .name("lib1")

        // Add library3 to test case when group doesn't contain any dots.
        val library3 =
            project()
                .files(
                    java(
                        """
                package com.example.dotless;

                import androidx.annotation.RestrictTo;

                public class DotlessCode {
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
                    public static void method() {
                    }
                }
                """
                    )
                        .indented(),
                    gradle(
                        """
                    apply plugin: 'com.android.library'
                    group=dotless
                    """
                    )
                        .indented(),
                    SUPPORT_ANNOTATIONS_JAR
                )
                .name("lib3")

        val library2 =
            project()
                .files(
                    kotlin(
                        """
                package com.example.myapplication

                import com.example.mylibrary.LibraryCode
                import com.example.dotless.DotlessCode

                fun test() {
                    LibraryCode.method1() // OK
                    LibraryCode.method2() // ERROR
                    LibraryCode.method3() // ERROR
                    LibraryCode.method4() // ERROR
                    val f1 = LibraryCode.FIELD1 // OK
                    val f2 = LibraryCode.FIELD2 // ERROR
                    val f3 = LibraryCode.FIELD3 // ERROR
                    val f4 = LibraryCode.FIELD4 // ERROR
                    DotlessCode.method() // ERROR
                }
                """
                    )
                        .indented(),
                    gradle(
                        """
                    apply plugin: 'com.android.library'
                    group=other.app
                    """
                    )
                        .indented()
                )
                .name("lib2")
                .dependsOn(library)
                .dependsOn(library3)

        // Make sure projects are placed correctly on disk: to do this, record
        // project locations with a special client, then after the lint run make
        // sure the locations are correct.
        library2.under(library)
        library3.under(library)
        var libDir1: File? = null
        var libDir2: File? = null
        var libDir3: File? = null
        val factory: () -> com.android.tools.lint.checks.infrastructure.TestLintClient = {
            object : com.android.tools.lint.checks.infrastructure.TestLintClient() {
                override fun registerProject(dir: File, project: Project) {
                    if (project.name == "lib1") {
                        libDir1 = dir
                    } else if (project.name == "lib2") {
                        libDir2 = dir
                    } else if (project.name == "lib3") {
                        libDir3 = dir
                    }
                    super.registerProject(dir, project)
                }
            }
        }
        assertEquals("LIBRARY:lib1", library.toString())

        lint()
            .projects(library, library2, library3)
            .reportFrom(library2)
            .clientFactory(factory)
            .run()
            .expect(
                """
                src/main/kotlin/com/example/myapplication/test.kt:8: Error: LibraryCode.method2 can only be called from within the same library (test.pkg.library:test_project-lib1) [RestrictedApiAndroidX]
                    LibraryCode.method2() // ERROR
                                ~~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:9: Error: LibraryCode.method3 can only be called from within the same library group (referenced groupId=test.pkg.library from groupId=other.app) [RestrictedApiAndroidX]
                    LibraryCode.method3() // ERROR
                                ~~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:10: Error: LibraryCode.method4 can only be called from within the same library group prefix (referenced groupId=test.pkg.library with prefix test.pkg from groupId=other.app) [RestrictedApiAndroidX]
                    LibraryCode.method4() // ERROR
                                ~~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:12: Error: LibraryCode.FIELD2 can only be accessed from within the same library (test.pkg.library:test_project-lib1) [RestrictedApiAndroidX]
                    val f2 = LibraryCode.FIELD2 // ERROR
                                         ~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:13: Error: LibraryCode.FIELD3 can only be accessed from within the same library group (referenced groupId=test.pkg.library from groupId=other.app) [RestrictedApiAndroidX]
                    val f3 = LibraryCode.FIELD3 // ERROR
                                         ~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:14: Error: LibraryCode.FIELD4 can only be accessed from within the same library group prefix (referenced groupId=test.pkg.library with prefix test.pkg from groupId=other.app) [RestrictedApiAndroidX]
                    val f4 = LibraryCode.FIELD4 // ERROR
                                         ~~~~~~
                src/main/kotlin/com/example/myapplication/test.kt:15: Error: DotlessCode.method can only be called from within the same library group prefix (referenced groupId=dotless with prefix "" from groupId=other.app) [RestrictedApiAndroidX]
                    DotlessCode.method() // ERROR
                                ~~~~~~
                7 errors, 0 warnings
                """
            )

        // Make sure project directories are laid out correctly
        assertTrue(libDir2!!.parentFile.path == libDir1!!.path)
        assertTrue(libDir3!!.parentFile.path == libDir1!!.path)
    }

    fun testLibraryGroupPrefixMatches() {
        assertTrue(sameLibraryGroupPrefix("foo", "foo"))
        assertFalse(sameLibraryGroupPrefix("foo", "bar"))
        assertTrue(sameLibraryGroupPrefix("foo.bar", "foo.bar"))
        assertFalse(sameLibraryGroupPrefix("foo.bar", "bar"))
        assertFalse(sameLibraryGroupPrefix("foo.bar", "foo"))

        assertTrue(sameLibraryGroupPrefix("foo.bar", "foo.baz"))
        assertTrue(sameLibraryGroupPrefix("com.foo.bar", "com.foo.baz"))
        assertFalse(sameLibraryGroupPrefix("com.foo.bar", "com.bar.qux"))

        // Implementation for AndroidX differs from the standard RestrictToDetector, since we
        // treat LIBRARY_GROUP_PREFIX as anything in the androidx.* package. See b/297047524.
        assertTrue(sameLibraryGroupPrefix("androidx.foo.foo", "androidx.bar.bar"))
    }

    fun test278573413() {
        // Regression test for b/278573413.
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import library.pkg.PrivateKotlinClass

          class Test : PrivateKotlinClass {
            override fun method() {}
          }
          """
                )
                    .indented(),
                mavenLibrary(
                    "my.group.id:myklib:25.0.0-SNAPSHOT",
                    stubSources =
                    listOf(
                        kotlin(
                            """
              package library.pkg

              import androidx.annotation.RestrictTo

              @RestrictTo(RestrictTo.Scope.GROUP_ID)
              open class PrivateKotlinClass {
                  open fun method() {}
              }
              """
                        )
                            .indented(),
                    ),
                    compileOnly = listOf(SUPPORT_ANNOTATIONS_JAR)
                ),
                gradle(
                    """
                apply plugin: 'com.android.application'

                dependencies {
                    compile 'my.group.id:myklib:25.0.0-SNAPSHOT'
                }
                """
                )
                    .indented(),
                SUPPORT_ANNOTATIONS_JAR
            )
            .allowKotlinClassStubs(true)
            .run()
            .expect(
                """
        src/main/kotlin/test/pkg/Test.kt:5: Error: PrivateKotlinClass can only be accessed from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
        class Test : PrivateKotlinClass {
                     ~~~~~~~~~~~~~~~~~~
        src/main/kotlin/test/pkg/Test.kt:6: Error: PrivateKotlinClass.method can only be called from within the same library group (referenced groupId=my.group.id from groupId=<unknown>) [RestrictedApiAndroidX]
          override fun method() {}
                       ~~~~~~
        2 errors, 0 warnings
        """
            )
    }
}
