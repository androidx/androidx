/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.activity.compose.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest

val BACK_HANDLER =
    bytecodeStub(
        filename = "BackHandler.kt",
        filepath = "androidx/activity/compose",
        checksum = 0x40a94587,
        """
    package androidx.activity.compose

    import androidx.compose.runtime.Composable

    @Composable
    fun BackHandler(
        enabled: Boolean = true,
        onBack: () -> Unit) { } 
    """,
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AZcQlmZiXUpSfmVKhl5hcklmWWVKp
    l5yfW5BfnCrE65SYnO0BlM9JLfIuEWILSS0u8S7hEuXiBqrQS61IzC3ISYUJ
    KzFoMQAA3mTzZGMAAAA=
    """,
        """
    androidx/activity/compose/BackHandlerKt.class:
    H4sIAAAAAAAA/4VSXU8TQRQ9s/1eoJQiCgURBQRE2YImPFRNlITQWNBY5QGe
    ptuhTrudJbvTBl8Mf8Of4RvxwfDsjzLe2bZY4YEmnft17r3n3r2///z8BeAF
    igwrXNUDX9bPHO5q2ZX6q+P67VM/FM5b7rb2KOyJ4J1OgTHkmrzLHY+rhvO+
    1hQueWMMI0NAhqXVo0rL155UTrPbdk46iur6KnR2+1qxtHbI8PxW2MtB/LOS
    uvQ6ylquXNEdsAw6Ssu2cHYim9c8UWJYrPhBw2kKXQu4pKJcKV/zXoMDXx90
    PI9QSV8Z6mnYDPNDZKTSIlDcc8pKB5Qu3TCFUYYp94twW/38DzzgbaHNyCur
    leuLKQ15qqZIg/iPIotxG2PIMaSEMlzrDOyIYeG2lTFMDm15qS5OeMfTDNu3
    b7t8k5yhkkDShoV7DBODCvtC8zrXnLpZ7W6MToSZJ0EcW0axyH8mjUaHY9U3
    GUqX53n78ty2clZPjERi2ios5C7PC1aRbaXTFCQttjWaixfS+XievMXEXsqU
    2GLUA/kBgeF5s0PzbrRo1PiOXxcM4xWpxEGnXRPBJ7NBk+673DvkgTR235mp
    yobiuhOQPvuxdyNl1ZWhpPCbf+dAB3s9evVl/4ONVTUR2uen/QZ21e8ErtiV
    xpjp1zi8UR+btOQ4zM/CjNk6WStklci2SKbW8yMXmPgRAVbpTdJGkshgjfS7
    PQjymIxKpGDjDsWfROgU1vv4NMmn9M8YOA0P5DKYonQW9druc8jOxb99RyJe
    KqxfYLrX8hm9MbB01DuLWNQlSRXTJC1sRKDHcEi+onJmhMIxYmXMljFHL+6X
    MY8HZSzg4TFYiEdYPEYyRCLEUoh89Nohlv8C003RZnYEAAA=
    """
    )

val COMPONENT_ACTIVITY =
    bytecodeStub(
        "ComponentActivity.kt",
        "androidx/activity",
        0xd291c9ac,
        """
    package androidx.activity

    class ComponentActivity {
        fun onBackPressed() { }
    }
    """,
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AZcQlmZiXUpSfmVKhl5hcklmWWVKp
    l5yfW5BfnCrE65SYnO0BlM9JLfIuEWILSS0u8S7hEuXiBqrQS61IzC3ISYUJ
    KzFoMQAA3mTzZGMAAAA=
    """,
        """
    androidx/activity/ComponentActivity.class:
    H4sIAAAAAAAA/41RTW8TMRB9481uyralm7ZAWuCEkGgrsWnFCVClthJSqrQg
    QLnk5Oxa4Cax0dqJ2lt+C/+AExIHFHHsj0KMQ4QQXGrJb+a98fPH+Prnt+8A
    nuEh4ZE0ZWV1eZnLwuuJ9lf5iR19skYZf7RQ6iBCdiEnMh9K8yF/3b9Qha8j
    IiQvtdH+kBA92emuIEaSooY6oeY/akd43LnB/i8Iq9Ycy2LwplLOqZLQ6Ays
    H2qTnykvS+klrxGjScTXpgAxgQYsXerAWpyV+4S92TRLRVOkIptNU7EUEtGc
    TQ9Ei47jH58TpqdJFm2LVi1YDoi3w+Z/F3o68PyCE1sqwlpHG3U+HvVV9V72
    h6ysd2whh11Z6cAXYvrOjqtCvdKBbL0dG69Hqqud5uqRMdZLr61x2IfgBoUh
    +GjuF2OTWc6RwsN2v2Lpy7y8xZjMxRq2GVd+L8AtpBwbWP5j3gttCfNfY/yX
    kRZGgftzvIcHHJ+zHv5ttYeojdttrDEiC9BoYx0bPZDDJu70EDukDncdEodl
    Tn4B9lImlUcCAAA=
    """
    )

val ON_BACK_PRESSED_DISPATCHER =
    bytecodeStub(
        "OnBackPressedDispatcher.kt",
        "androidx/activity",
        0x38be529,
        """
    package androidx.activity

    class OnBackPressedDispatcher {
        fun onBackPressed() { }
    }
    """,
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AZcQlmZiXUpSfmVKhl5hcklmWWVKp
    l5yfW5BfnCrE65SYnO0BlM9JLfIuEWILSS0u8S7hEuXiBqrQS61IzC3ISYUJ
    KzFoMQAA3mTzZGMAAAA=
    """,
        """
    androidx/activity/OnBackPressedDispatcher.class:
    H4sIAAAAAAAA/41R0UobQRQ9dza7savWjbY1xva9VnCj+GSLoC2FSFqLLXnJ
    02R3qGOSWdmZBH3Lt/QP+lTogwQf+1Gld6KICEIH5tx7zp2zM/fun7+/rwDs
    4hVhQ5q8LHR+kcrM6bF2l+mxOZRZ/0uprFX5B23PpctOVVkFEZIzOZbpQJrv
    6XHvTGWuioAQvdNGu31C8Hqjs4AQUYwKqoSKO9WWsNn+71veEhaL+yVCrd0v
    3ECb9JNyMpdO8hkxHAfcAnkICdRn6UJ71uQs3+Y7p5MkFnURi2Q6icWcT0R9
    OtkRTToMr39ETI+iJGiIZsVbdog/h8Yjz9rqO+7mfZErwlJbG/V5NOyp8pvs
    DVhZbheZHHRkqT2/FeOvxajM1EftydrJyDg9VB1tNVcPjCmcdLowFtsQPCy/
    BD+AZ8dYZ5ZyJN/em1+Y+zkrrzFGM7GCBuPCzQE8Qcyxhvk786Yfjt8PjeE9
    I90aBdZnuIqXHPdY9/9wsYughactLDEi8VBrYRkrXZDFMzzvIrSILV5YRBbz
    nPwDYUKQQFkCAAA=
    """
    )

val PREDICTIVE_BACK_HANDLER =
    LintDetectorTest.bytecode(
        "libs/predictivebackhandler.jar",
        LintDetectorTest.kotlin(
                """
    package androidx.activity.compose

    import androidx.compose.runtime.Composable

    @Composable
    fun PredictiveBackHandler(
        enabled: Boolean = true,
        onBack: (progress: String) -> Unit) { }
    """
            )
            .indented(),
        0x7806fd68,
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3ApcwlmZiXUpSfmVKhl5hcklmWWVKp
    l5yfW5BfnCrEFpJaXOJdwiXKxQ0U0kutSMwtyIELKzFoMQAAfOuo51QAAAA=
    """,
        """
    androidx/activity/compose/TestKt.class:
    H4sIAAAAAAAA/4VTW08TQRT+ZnvblltZRaFcFEEtImxBDQ81JkpCbKyVCPIg
    8WHYDnXodpbsTht9MfwNX/0HvhEfDPHRH2U8s20BwYQmPZc535zznXNmf//5
    8RPAYzxiuM1VPQxk/ZPLPS07Un92vaB1GETC3RaRfqUzYAz5A97hrs9Vw32z
    dyA8Ok0wjG2Goi7NNfGCe82XlMoXIcN88X21GWhfKveg03L324owgYrcjZ61
    Ul7YYdi4EvZ0qXpWeEuHUjXK/SvvlNTlZ3Giu9XTJvrcw7bSsiXc9djne74o
    M8xVg7DhHgi9F3JJdbhSgebdmrVA19q+T6h0oEw3NnIMM+f4SaVFqLjvVpRh
    EkkvymCQpuB9FF6zd3+Th7wltJnC/WL14tTKl9tZ2BnEMEZyGEKeISOU4Vpn
    YO9pN1dNkWH6vyuYr4t93vY1w9rVq6hcpmlIpZDOwcJNhtF+htdC8zrXnOpa
    rU6CnhAzIkVsm8aw6PyTNFaJrPoKw4eTo5ncyVHOyltdNRCrcYvM4a62rcKT
    /MlRwSqxVdsmIFmJ1dl8sjDtXHNGS+lf39KDdsaxbdtJ2nYx6yQdwpZSLzOm
    yCojFnD6FM/P5kb/8HQpNVIUSCrSDPZhGDRCEUU0d01vfblJA0uuB3WKjVSl
    ErV2a0+E22YjpkTgcX+Hh9L4vcPslmwortsh2ZNvu2+uojoykhR+fva86Ju4
    GD0l9Q9saEvTGl/zw16B3FbQDj2xIY0z0cuxcyk/VmhVSZifhQmzO/IekFcm
    3yKdWXQGjjH6PQYskkzT1NKw8ZDsG10IHFyLU2SQw3WKL8XoDJZ7eJu0S/+s
    gVPzQD6LMbrO4lprPQ7DU8kvX5FKlguLxxjvliyRTIDZce1hJOKMacptk7SI
    vgEtYJV0hdKZFgq7SFQwWcEUSUxXMINbFdzG7C5YhDuY20UuQirCfAQnluTe
    jY17Ee5HKP4FMROs2egEAAA=
    """
    )
