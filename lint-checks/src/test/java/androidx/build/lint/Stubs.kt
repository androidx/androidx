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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestFiles.java

class Stubs {

    companion object {

        /** [TestFile] containing Keep.java from the annotation library. */
        val Keep =
            TestFiles.java(
                """
package androidx.annotation;

public @interface Keep {
}
            """
            )

        val RunWith =
            TestFiles.kotlin(
                """
package org.junit.runner

annotation class RunWith(val value: KClass<*>)
            """
            )

        val JUnit4Runner =
            TestFiles.kotlin(
                """
package org.junit.runners

class JUnit4
            """
            )

        val ParameterizedRunner =
            TestFiles.kotlin(
                """
package org.junit.runners

class Parameterized
            """
            )

        val TestParameterInjector =
            TestFiles.kotlin(
                    """
                    package com.google.testing.junit.testparameterinjector

                    class TestParameterInjector
                    """
                )
                .indented()

        val AndroidJUnit4Runner =
            TestFiles.kotlin(
                """
package androidx.test.ext.junit.runners

class AndroidJUnit4
            """
            )

        val TestSizeAnnotations =
            TestFiles.kotlin(
                """
package androidx.test.filters

annotation class SmallTest
annotation class MediumTest
annotation class LargeTest
            """
            )

        val TestAnnotation =
            TestFiles.kotlin(
                """
package org.junit

annotation class Test
            """
            )
        val RuleAnnotation =
            TestFiles.kotlin(
                """
package org.junit

annotation class Rule
            """
            )

        val AndroidXScreenshotTestRule =
            TestFiles.kotlin(
                    """
                    package androidx.test.screenshot

                    class AndroidXScreenshotTestRule
                    """
                )
                .indented()

        val SdkSuppressAnnotation =
            java(
                    """
        package androidx.test.filters;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        public @interface SdkSuppress {
            int minSdkVersion() default 0;
            int maxSdkVersion() default Integer.MAX_VALUE;
            String codename() default "";
        }
        """
                )
                .indented()

        val AndroidBuild =
            java(
                    """
            package android.os;

            public final class Build {
                public static final class VERSION_CODES {
                    public static final int TIRAMISU = 33;
                    public static final int VANILLA_ICE_CREAM = 35;
                }
                public static final class VERSION {
                    public static final int SDK_INT = 35;
                }
            }
            """
                )
                .indented()

        /**
         * [TestFile] containing OptIn.kt from the Kotlin standard library.
         *
         * This is a workaround for the Kotlin standard library used by the Lint test harness not
         * including the Experimental annotation by default.
         */
        val OptIn =
            TestFiles.kotlin(
                """
package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.reflect.KClass

@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class RequiresOptIn(
    val message: String = "",
    val level: Level = Level.ERROR
) {
    public enum class Level {
        WARNING,
        ERROR,
    }
}

@Target(
    CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
)
@Retention(SOURCE)
@SinceKotlin("1.3")
@RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public annotation class OptIn(
    vararg val markerClass: KClass<out Annotation>
)
            """
            )

        /** [TestFile] containing ChecksSdkIntAtLeast.java from the annotation library. */
        val ChecksSdkIntAtLeast =
            TestFiles.java(
                """
package androidx.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({METHOD, FIELD})
public @interface ChecksSdkIntAtLeast {
    int api() default -1;
    String codename() default "";
    int parameter() default -1;
    int lambda() default -1;
}
            """
            )

        val RequiresApi =
            TestFiles.java(
                """
package androidx.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
public @interface RequiresApi {
    @IntRange(from = 1)
    int value() default 1;
    @IntRange(from = 1)
    int api() default 1;
}
            """
            )

        val IntRange =
            TestFiles.java(
                """
package androidx.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE})
public @interface IntRange {
    long from() default Long.MIN_VALUE;
    long to() default Long.MAX_VALUE;
}
            """
            )

        val RestrictTo =
            TestFiles.java(
                """
package androidx.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target({ANNOTATION_TYPE, TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
public @interface RestrictTo {
    Scope[] value();
    enum Scope {
        LIBRARY,
        LIBRARY_GROUP,
        LIBRARY_GROUP_PREFIX,
        /** @deprecated Use {@link #LIBRARY_GROUP_PREFIX} instead */
        @Deprecated
        GROUP_ID,
        TESTS,
        SUBCLASSES,
    }
}
            """
            )

        val JetBrainsAnnotations =
            TestFiles.kotlin(
                """
package org.jetbrains.annotations

annotation class NotNull
annotation class Nullable
            """
            )

        val IgnoreAnnotation =
            TestFiles.kotlin(
                """
package org.junit

annotation class Ignore
            """
            )

        val DoNotInline =
            TestFiles.java(
                """
package androidx.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({METHOD})
public @interface DoNotInline {
}
            """
            )

        val DeprecatedSinceApi =
            TestFiles.kotlin(
                """
package androidx.annotation

import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS, CONSTRUCTOR)
public annotation class DeprecatedSinceApi(
    val api: Int,
    val message: String = ""
)
            """
            )

        val JvmDefaultWithCompatibility =
            TestFiles.kotlin(
                """
package kotlin.jvm

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JvmDefaultWithCompatibility
            """
                    .trimIndent()
            )

        /** [TestFile] containing OptIn.kt from the AndroidX experimental annotation library. */
        val JetpackOptIn: TestFile =
            LintDetectorTest.kotlin(
                """
package androidx.annotation

import kotlin.annotation.Retention
import kotlin.annotation.Target
import kotlin.reflect.KClass

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FILE,
    AnnotationTarget.TYPEALIAS
)
annotation class OptIn(
    @get:Suppress("ArrayReturn")
    vararg val markerClass: KClass<out Annotation>
)
    """
                    .trimIndent()
            )

        /**
         * [TestFile] containing RequiresOptIn.kt from the AndroidX experimental annotation library.
         */
        val JetpackRequiresOptIn: TestFile =
            LintDetectorTest.kotlin(
                """
package androidx.annotation

import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class RequiresOptIn(
    val level: Level = Level.ERROR
) {
    enum class Level {
        WARNING,
        ERROR
    }
}
    """
                    .trimIndent()
            )

        /** [TestFile] containing VisibleForTesting.kt from the AndroidX annotation library. */
        val VisibleForTesting: TestFile =
            LintDetectorTest.kotlin(
                """
package androidx.annotation

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
public annotation class VisibleForTesting(
    @ProductionVisibility val otherwise: Int = PRIVATE
) {
    public companion object {
        public const val PRIVATE: Int = 2
        public const val PACKAGE_PRIVATE: Int = 3
        public const val PROTECTED: Int = 4
        public const val NONE: Int = 5
    }
}
            """
                    .trimIndent()
            )

        /** Contains only a few of the isAtLeastX implementations from BuildCompat for testing */
        val BuildCompat: TestFile =
            LintDetectorTest.java(
                """
package androidx.core.os;

import android.os.Build;
import android.os.Build.VERSION;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresOptIn;
import androidx.annotation.RestrictTo;

import java.util.Locale;

public class BuildCompat {
    private BuildCompat() {}

    @RestrictTo(RestrictTo.Scope.TESTS)
    protected static boolean isAtLeastPreReleaseCodename(@NonNull String codename, @NonNull String buildCodename) {
        if ("REL".equals(buildCodename)) {
            return false;
        }
        final String buildCodenameUpper = buildCodename.toUpperCase(Locale.ROOT);
        final String codenameUpper = codename.toUpperCase(Locale.ROOT);
        return buildCodenameUpper.compareTo(codenameUpper) >= 0;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    @Deprecated
    public static boolean isAtLeastN() {
        return VERSION.SDK_INT >= 24;
    }

    @PrereleaseSdkCheck
    @ChecksSdkIntAtLeast(api = 32, codename = "Sv2")
    @Deprecated
    public static boolean isAtLeastSv2() {
        return VERSION.SDK_INT >= 32 || (VERSION.SDK_INT >= 31 && isAtLeastPreReleaseCodename("Sv2", VERSION.CODENAME));
    }

    @PrereleaseSdkCheck
    @ChecksSdkIntAtLeast(codename = "UpsideDownCake")
    public static boolean isAtLeastU() {
        return VERSION.SDK_INT >= 33 && isAtLeastPreReleaseCodename("UpsideDownCake", VERSION.CODENAME);
    }

    @RequiresOptIn
    public @interface PrereleaseSdkCheck { }
}
        """
                    .trimIndent()
            )

        val FlaggedApi: TestFile =
            TestFiles.java(
                    """
package android.annotation; // HIDE-FROM-DOCUMENTATION

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface FlaggedApi {
    String value();
}
      """
                )
                .indented()

        val ChecksAconfigFlag: TestFile =
            TestFiles.kotlin(
                    """
package androidx.annotation

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD
)
public annotation class ChecksAconfigFlag (
    val flag: String
)
        """
                )
                .indented()

        val RequiresAconfigFlag: TestFile =
            TestFiles.kotlin(
                    """
package androidx.annotation

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE,
)
public annotation class RequiresAconfigFlag (
    val value: String
)
        """
                )
                .indented()

        val Flags: TestFile =
            TestFiles.kotlin(
                    """
package androidx.core.flagging

public class Flags {
    public companion object {
        @JvmOverloads
        @JvmStatic
        public fun getBooleanFlagValue(
            packageName: String,
            flagName: String,
            defaultValue: Boolean = false,
        ): Boolean {
            return defaultValue
        }
    }
}
        """
                )
                .indented()

        val EspressoUiController =
            TestFiles.java(
                    """
package androidx.test.espresso;

public interface UiController {
    void loopMainThreadForAtLeast(long millisDelay);
}
            """
                )
                .indented()
    }
}
