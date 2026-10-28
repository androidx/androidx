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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanRestrictToDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = BanRestrictToDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(BanRestrictToDetector.ISSUE)

    @Test
    fun restrictTo_reportsError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo
                    import androidx.annotation.RestrictTo.Scope

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    class RestrictedClass {
                        @RestrictTo(Scope.LIBRARY)
                        fun restrictedFunction() {}

                        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
                        val restrictedProperty: Int = 0

                        @get:RestrictTo(RestrictTo.Scope.LIBRARY)
                        @set:RestrictTo(RestrictTo.Scope.LIBRARY)
                        var restrictedAccessors: Int = 0
                    }

                    @androidx.annotation.RestrictTo(RestrictTo.Scope.LIBRARY)
                    fun restrictedTopLevelFunction() {}
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/RestrictedClass.kt:7: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedClass.kt:9: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedClass.kt:12: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedClass.kt:15: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @get:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedClass.kt:16: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @set:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedClass.kt:20: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @androidx.annotation.RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
6 errors, 0 warnings
                """
            )
    }

    @Test
    fun restrictTo_fileTarget_reportsError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    @file:RestrictTo(RestrictTo.Scope.LIBRARY)

                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    class ClassInRestrictedFile

                    fun functionInRestrictedFile() {}
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/ClassInRestrictedFile.kt:2: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @file:RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
                """
            )
    }

    @Test
    fun restrictTo_constructors_reportsError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    class PrimaryConstructor @RestrictTo(RestrictTo.Scope.LIBRARY) constructor()

                    class SecondaryConstructor {
                        @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(value: Int)
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/PrimaryConstructor.kt:6: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    class PrimaryConstructor @RestrictTo(RestrictTo.Scope.LIBRARY) constructor()
                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/PrimaryConstructor.kt:9: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(value: Int)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    @Test
    fun restrictTo_fieldTarget_reportsError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    class FieldTarget {
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY)
                        val fieldProperty: Int = 0

                        @JvmField
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY)
                        var jvmFieldProperty: Int = 0
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/FieldTarget.kt:7: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/FieldTarget.kt:11: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    /**
     * Primary-constructor properties are modeled by UAST as a parameter, a field and accessors, so
     * each annotation must be reported exactly once (no duplicates, no misses).
     */
    @Test
    fun restrictTo_primaryConstructorProperties_reportsErrorOnce() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    class PrimaryConstructorProperties(
                        @get:RestrictTo(RestrictTo.Scope.LIBRARY) val getterTarget: Int,
                        @get:RestrictTo(RestrictTo.Scope.LIBRARY)
                        @set:RestrictTo(RestrictTo.Scope.LIBRARY)
                        var accessorTargets: Int,
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY) val fieldTarget: Int,
                        @RestrictTo(RestrictTo.Scope.LIBRARY) val noUseSiteTarget: Int,
                    )
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/PrimaryConstructorProperties.kt:7: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @get:RestrictTo(RestrictTo.Scope.LIBRARY) val getterTarget: Int,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/PrimaryConstructorProperties.kt:8: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @get:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/PrimaryConstructorProperties.kt:9: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @set:RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/PrimaryConstructorProperties.kt:11: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @field:RestrictTo(RestrictTo.Scope.LIBRARY) val fieldTarget: Int,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/PrimaryConstructorProperties.kt:12: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY) val noUseSiteTarget: Int,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
                """
            )
    }

    @Test
    fun restrictTo_objectsInterfacesAndInnerClasses_reportsError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    object RestrictedObject

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    interface RestrictedInterface {
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        fun restrictedInterfaceFunction()
                    }

                    class Outer {
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        inner class RestrictedInner

                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        class RestrictedNested

                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        companion object
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/RestrictedObject.kt:6: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedObject.kt:9: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedObject.kt:11: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedObject.kt:16: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedObject.kt:19: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/RestrictedObject.kt:22: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                        @RestrictTo(RestrictTo.Scope.LIBRARY)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
6 errors, 0 warnings
                """
            )
    }

    @Test
    fun noRestrictTo_doesNotReportError() {
        lint()
            .files(
                RestrictToStub,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    // Only referencing the annotation class without applying it is fine.
                    val scope = RestrictTo.Scope.LIBRARY

                    internal class InternalClass {
                        @Deprecated("Use something else")
                        fun deprecatedFunction() {}
                    }
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.lint.test.other

                    // A different annotation that happens to share the simple name.
                    annotation class RestrictTo

                    @RestrictTo
                    class NotTheAndroidXAnnotation
                    """
                ),
            )
            .run()
            .expectClean()
    }

    /**
     * The AndroidX API guidelines require typedef annotation classes to be annotated with
     * `@RestrictTo`, so these must not be reported.
     */
    @Test
    fun restrictTo_typedefAnnotationClasses_doesNotReportError() {
        lint()
            .files(
                RestrictToStub,
                TypedefStubs,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.IntDef
                    import androidx.annotation.LongDef
                    import androidx.annotation.RestrictTo
                    import androidx.annotation.StringDef

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    @Retention(AnnotationRetention.SOURCE)
                    @IntDef(0, 1)
                    annotation class IntMode

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    @Retention(AnnotationRetention.SOURCE)
                    @LongDef(0L, 1L)
                    annotation class LongMode

                    // Annotation order should not matter.
                    @Retention(AnnotationRetention.SOURCE)
                    @StringDef("a", "b")
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    annotation class StringMode
                    """
                ),
            )
            .run()
            .expectClean()
    }

    /**
     * Only typedef annotation classes are exempt: other annotation classes, and annotation classes
     * using a non-AndroidX annotation that shares a typedef's simple name, are still reported.
     */
    @Test
    fun restrictTo_nonTypedefAnnotationClasses_reportsError() {
        lint()
            .files(
                RestrictToStub,
                TypedefStubs,
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import androidx.annotation.RestrictTo

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    @Retention(AnnotationRetention.SOURCE)
                    annotation class NotATypedef

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    @IntDef(0, 1)
                    annotation class LookalikeIntDef

                    @Retention(AnnotationRetention.SOURCE)
                    annotation class IntDef(vararg val value: Int)
                    """
                ),
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/NotATypedef.kt:6: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/NotATypedef.kt:10: Error: @RestrictTo should not be used in core Compose libraries [BanRestrictTo]
                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    companion object {
        private val TypedefStubs: TestFile =
            bytecode(
                "libs/typedefs.jar",
                kotlin(
                        """
                package androidx.annotation

                @Retention(AnnotationRetention.SOURCE)
                @Target(AnnotationTarget.ANNOTATION_CLASS)
                public annotation class IntDef(
                    vararg val value: Int = [],
                    val flag: Boolean = false,
                    val open: Boolean = false,
                )

                @Retention(AnnotationRetention.SOURCE)
                @Target(AnnotationTarget.ANNOTATION_CLASS)
                public annotation class LongDef(
                    vararg val value: Long = [],
                    val flag: Boolean = false,
                    val open: Boolean = false,
                )

                @Retention(AnnotationRetention.SOURCE)
                @Target(AnnotationTarget.ANNOTATION_CLASS)
                public annotation class StringDef(
                    vararg val value: String = [],
                    val open: Boolean = false,
                )
                """
                    )
                    .indented(),
                0x18cfcfb1,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmIWBihQYtBiAAAPGQqpGAAAAA==
                """,
                """
                androidx/annotation/IntDef.class:
                H4sIAAAAAAAA/4WSz08TQRTHvzPbdrdLaZdFFAoIIvLDg63EAwmngpisqZS0
                1UR6MAMdyMKyS5gtwo2biSf/Bv8MD4bgzf/Af8b4phW6JlWTnZk3n/dr5733
                /eeXrwCe4TFDUYTt08hvn5dEGEaxiP0oLHlh/Fzum2AMzqE4E6VAhAel2u6h
                3ItNGAwzfZpwq9yKJtIM6TMRdCRDamm55dGxH4gDBmNpeceg7CASnciQYbp6
                FMWBHyZD1WUsQy2tMSwO0PdTJS0zjdrr+sYmvWqAS1OcHsiYrPIiCKL3st0D
                imH+nwlu/ZzK1latWWl6ta13G9VKo8EwWx1YiOQ/LfzHZDsK/L2LNV2GgYa3
                6ecG6zcDeUyRmhcnkowKiX9svt2mUozcPO6VjEVbxIKs+PGZbgHXW4qBHWmB
                ms3PfS2VSWo/Zfhxdbli83Fuc2fSvrqkY4yW1ROHbG4ZPS0tjZKHdf2Bj5M7
                L7P1FdexuMOLvGzM8XJqnVs5N08kTSSznrJM5+qyC8wEqBd6HtefM5liipQW
                ka7Hb5J2skTMBDGd7LePLG/Zrm1xN2PZ5aHVHBHXGqbweaK5l5oWVh39wBWG
                qerfp5/KRCOa7V2eHMVUyf5MEBKdgJjdiDqne/KFH9CgT9Q71NJj+cZX/m4g
                ++ZqgRIiRfEyVF6DSm7CotuivrW6PcjCvgE7GCKQ+wOQ+5LuGBawTOcnpDFM
                alMijwIcEkdaYBIuRnGnpxgjxV0tdhX3MI4J8iq2YHiY9DDlUdRpD/cx42EW
                D8hKYQ4PWxhRmFd4pGB2d/qy3T2nYCnYCsMKeYWCgqO0sasw+gujVe4+UwQA
                AA==
                """,
                """
                androidx/annotation/LongDef.class:
                H4sIAAAAAAAA/4WSz08TQRTHvzPbdrdLaZeiCEUEEfnhwVbigYRTQUyWVEra
                aiI9mIEOzcKyS5gtwq03E0/+Df4ZHgzBm/+B/4zxTSt0TRpMdmfefN73vbf7
                3vz8/e07gJd4xjAtgtZZ6LUuiiIIwkhEXhgUK2HQfiUPTTAG50ici6Ivgnax
                un8kDyITBsPsgMbiyremiSRD8lz4HcmQWF5pbtN26Is2g7G8smdQeRAJT2XA
                MFM5DiPfC+KpajKSgbbWGZaG+Ael4spUvfq2trnFUBgS0hBnbRmRKit8P/wo
                W32gGBbuLHAb55R3dqqNcsOt7nzYrJTrdYa5ytBGxL9p8T+S3dD3Di7XdRuG
                Cm/Lzw/3b/nyhDI1Lk8liXKxb2y836VWjN383BsZiZaIBKn4ybkeAddLgoEd
                a4OGzS88bZXIar1g+HXVXbX5JLe5M21fdWmboDfdN0dsbhl9L70axTfr+hOf
                pHBeYhurecfiDi/wkjHPS4kNbmXyWSJJIqmNhGU6V90eMGOglutHXH9NpQoJ
                clpEehF/SdJJEzFjxHTSPz6zrGXnbYvnU5ZdGlnLEMlbo5Q+SzSzrWluzdE/
                uKpbfsf1pz7RHU27QUSH58cRtXJwKQiJjk/MroedswP52vPppk/VOjTTE/nO
                U96+LwdytUgVkaB8KeqvQT03YdFpSZ+avSGkYd+APYwQyPwDKHxZjwyLWKH9
                C5IYJbcpkUUODpljTTCJPMZxr++4T44JbfYcDzCJKYoqNGG4mHbx0KWsMy4e
                YdbFHB6TSmEeT5oYU1hQeKpg9lZ60r01o2Ap2AqjClmFnIKjtDivMP4Hsl6o
                /FUEAAA=
                """,
                """
                androidx/annotation/StringDef.class:
                H4sIAAAAAAAA/4VSXU8TQRQ9M/3Y7VK221WkFBFE5MMHW4kPJDwVxGRNpaSt
                JtIHM9CRLCy7pLNFeOPNxMQ/4c/wwRBMfPBHGe+0QmvSYLIzc+fccz/2zP31
                +9t3AM+xwjAjwnYn8ttnJRGGUSxiPwpLjbjjhwcv5AcDjME5FKeiFIjwoFTb
                O5T7sYEEw+wAHYqs3JgGUgypUxF0JcPE8kqrOgjo519nSEYnMmRILK/sJqgj
                UD/VoygO/HA4aV3GMtQWRSyN8A+KDjPTjdqb+uYWQ3FESFN0DmRMLFsEQfRR
                tvuAYli4tcBNnFPZ3q41K02vtv1+s1ppNBjmqiMlGe5p8T+UnSjw98/XtQwj
                iTfl50f7twJ5TJma5yeSSLmhHpvvdkiK/PXPvZaxaItYEIsfn2rtud6SDOxI
                G/Ts/MzXVpms9jOGn5cXqxYvcIs709blBR15WnbfHLO4yfteWhoaPsyrT7xA
                4bzMNp64BZM7vGi5aZO5vJwsJ+Zp3+Bm1rXJkyrycnojaRrO5UV98l/i1dd0
                upgkklHP9Yl/kZRj/vjMbDPjWiYnvlW21sYIcXtJxwnNvtKovZbTf7NK01u9
                de5JFprFjBfGdHl6FJNygxkgSHQDwqxG1O3sy5d+QCM+Ve/SEx7Lt77y9wI5
                oKtFqokk5UtrmUliAybdFumWaPU0z8C6BnYxRuwlzcRjLNP5BSlkyW1IjMNG
                jkynBSaRh4s7fcddckxos+e4h0kUKGqqhYSHoodpj7Le9zCDBx5mMUcshYeY
                b8FReKSwoGD0dvoyvd1UsBSyCuMKtkJOaWZewf0DwzryUTwEAAA=
                """,
            )

        private val RestrictToStub: TestFile =
            bytecode(
                "libs/restrictto.jar",
                kotlin(
                        """
                package androidx.annotation

                import androidx.annotation.RestrictTo.Scope
                import java.lang.annotation.ElementType

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
                @Suppress("DEPRECATED_JAVA_ANNOTATION", "SupportAnnotationUsage")
                @java.lang.annotation.Target(
                    ElementType.ANNOTATION_TYPE,
                    ElementType.TYPE,
                    ElementType.METHOD,
                    ElementType.CONSTRUCTOR,
                    ElementType.FIELD,
                    ElementType.PACKAGE,
                )
                public annotation class RestrictTo(vararg val value: Scope) {
                    public enum class Scope {
                        LIBRARY,
                        LIBRARY_GROUP,
                        LIBRARY_GROUP_PREFIX,
                        @Deprecated("Use LIBRARY_GROUP_PREFIX instead.") GROUP_ID,
                        TESTS,
                        SUBCLASSES,
                    }
                }
                """
                    )
                    .indented(),
                0x2aa494eb,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijg0uWSSsxLKcrPTKnQS87PLcgvTtXL
                ycwr0StJLS4R4vfMK0ktykvMcc5JLC72LlFi0GIAAJU1DNpHAAAA
                """,
                """
                androidx/annotation/RestrictTo＄Scope.class:
                H4sIAAAAAAAA/5VU31MTVxT+7iZkN+uqMVKKiAiaKglgIqgFSakBot0SgWaB
                SmlLl2SNC5tdJ7thfLQvfes/0b9Aw0xl2mmH4bF/VKfn3g0iigx9uHv3nPOd
                H/c7595//v3jbwB3UGdImW614dnVF1nTdb3ADGzPzZYtP2jYlWDJSxkV77kl
                gzGMlzbNbTPrmG4tW3Sb9XzpNK6TU5MM5456yogyxPK2awdTDF2D7wQ2yNmt
                TerpFYbIYHpFQwyyig7EGaLBM9tnuHG6vAxqyqJkKdesWwzJD5MwnA0RXqNq
                u6bDwHSqa9t0mhblSQ+m106bSk6tFErLRYPh5il9NFxEZxwSPmFIHJa2sLFp
                VQIZnzJ0VBzPpco7B9Ol9wHkfgk9KrpxWUYn5RdFLzxluH8Mm+nTHuPuu84z
                jun7k8dFO9pNqqUP/bxHA1S0KIS4r1lB0SUHzmQvnWDLCxzbzXK+feHWtlLW
                2RPM/2PIrpe8Ri27aQUbDdN2/XfwfnbeC+abjkMoJVWcXyrrvFU9Hy9KwyDS
                vDsZ3tuDiZBL+nS5UF7VMAKNW2/RCLV164/KC8uLGnKh5Tb17YhlfbFcfKg/
                0TAWAu5QKaFBn9VwL1R+TgQuFY0lQ8NEqLlPVBrL0zOlgmEUSZ0P1V+Qd77i
                iBukYEQlVVxBTsGYgnsKJhTkNQwjyQ2PGC6svd8zGTTqPR87/xxN4BzDGetQ
                xTAy+EGUE9qq4THmVZSwwHDxADZrPW9YFTOwqvzO1C3fN2s0LAPLvtV/HFv9
                1MfAMqvE84WDGI+twKyagUkRpPp2hN4xiX8Y/4Cu8BbpX9hcytFflRrxaO+l
                pkrdkiolEqqkKMr+L/s/S917L0eVZDQp5fZe5th0XJH2f4tFFSkR2ZSUKK0O
                WjFaMi2FVpyHG2U8Sd/JQynjJ2qkmEwakEP9ra2A2nlIAsOlctMN7Lq1Yvv2
                hmMVDkeWnrsZr0r+50u2a8036xtWY8kkDH/KvIrprJgNm8ttZdywa64ZNBv0
                f7kdVXe3j4mr6a5rNcTt5m1VDa/ZqFgPbcd6cJvGpYOYUx5c4iMO0J5r72Pt
                nUaVUFXONixaZ4iPb2iVfRhknmjD8nxP9PBXTux0nwgcI1d60On7lKQiIuCN
                kjNDvW+gvua9RE3AiGb04hl9+0MI5dFEThlncU7YZZxHgjxs4acgjgtIkoWH
                viYQgNpC1+/o3cOVV2I8DqLHcJ3wfbjaxt+hCNxDTUqZXVzbg/TqSDUcz6vR
                QhRJ52iPI4XPqL6notowY7SFoaO5IoQOa7yBzbbupiBl+G2944Ti/sOJ2F+Q
                5jKRFrJGhmKNGpmOFu4amVgL40ZGbmHSyCgtTBmZVyKfgi9phVF+RVTozD8h
                rSYfRN6gsIOsEKajQhgVwkyHEO4KYTYmhHEhFGUhTArhoSKEqV18tYMuYnIP
                X+9icQdDr98esIeOA1yhpvTR/1U6WD+ROYACtSCKLUEWXRyY2KCpCGmIwBF7
                RQyQTvYlGrvlNUR0rOj4VscTrNKO73Ss4fs1MB8/4Mc1qD40H+s+Yj5kHyM+
                cj7GfNzzMeEj/x92M2+u2AgAAA==
                """,
                """
                androidx/annotation/RestrictTo.class:
                H4sIAAAAAAAA/41US0/bQBD+1klsN7ySlGcoz1IefZBAH4f2UhMSajXEkWOq
                ohyQSVbIYOwqdijcuPV/9Ef00EOFOPZHVZ0llATJoly+mf3m253Z2bF///n5
                C8ArvGGYtr1my3eapznb8/zQDh3fy5k8CFtOI7R8BYwhdWif2DnX9g5yxv4h
                b4QKYgwzXbZnq3bjKkgwJE5st80ZVpZX6uW7Uy3UGv4X/o5hoXzkh67j9cq2
                20G4wTf9RvuYeyFvkmwqQmbykMLkUXwpIt6trlcpb+gVzdxlyEZssezWAQ9J
                NWi7rv+VNztEEF1nN8HNvpRWqRiWZulGZa9Q1mo16sq1VUs7lYIIMAxVTaNa
                NK3dva2iZRXNXqZ2zfQVjErNMncKlkGrREkvljcZ4iW9XBTtiHyPmzLmo+NF
                l4uOWmdXrR/qqdXardKx8Y6Rt4vWB4OyKVWt8FHbImou+sBbbzQbrelt/uJ/
                JFXfdRpnJEz/a/c2D+2mHdrESccnMZpkSQATAAZ2RPypI1Z58pprDJ8vzkeT
                0riUlFKTyYtzMunOUpXUy2/S+MX5upRnb5m8MZzJpKRsMiOrLCPl4/nYPKE5
                dpu7/C7L2bgqpRLi/HVxzbtHmyqFuOk9P4DElUMDdh+9Im450GVXj0KGft3z
                eKvg2kHAaVKTNb/davCS49KpE2abGnvMPzmBs+/y7sQGi3QdxKlSWTQyTo8N
                lbwYXhImiV+jeO79BP05qOdYx2uyP0j14DqucPShHwPkDtahcgwhJSAtICPg
                oYBhASMCRgWMYVzsrUPmmEBWwKSARwJGBIwKmMI0pZnppJlFGnNIYL6OmI7H
                OhZ0PMEiWSzpWMZKHSzAUzyrQwnwPMCLQDiEqwHUALm/2RJJMwQFAAA=
                """,
            )
    }
}
