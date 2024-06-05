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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoboxingStateValuePropertyDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = AutoboxingStateValuePropertyDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AutoboxingStateValuePropertyDetector.AutoboxingStateValueProperty)

    @Test
    fun testReadAutoboxingPropertyAsVariableAssignment() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.mutableIntStateOf

                    fun valueAssignment() {
                        val state = mutableIntStateOf(4)
                        val value = state.value
                    }
                """
                        .trimIndent()
                ),
                AutoboxingStateValuePropertyStub,
                MinimalSnapshotStateStub
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/lint/test/test.kt:7: Warning: Reading value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    val value = state.value
                      ~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 7: Replace with `intValue`:
@@ -7 +7
-     val value = state.value
+     val value = state.intValue
            """
            )
    }

    @Test
    fun testTrivialAssignAutoboxingProperty() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.mutableIntStateOf

                    fun valueAssignment() {
                        val state = mutableIntStateOf(0)
                        state.value = 42
                    }
                """
                        .trimIndent()
                ),
                AutoboxingStateValuePropertyStub,
                MinimalSnapshotStateStub
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/lint/test/test.kt:7: Warning: Assigning value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    state.value = 42
          ~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 7: Replace with `intValue`:
@@ -7 +7
-     state.value = 42
+     state.intValue = 42
            """
            )
    }

    companion object {
        private val AutoboxingStateValuePropertyStub =
            bytecodeStub(
                filename = "AutoboxingStateValueProperty.kt",
                filepath = "androidx/compose/runtime/snapshots",
                checksum = 0xd8b7ebd3,
                source =
                    """
                package androidx.compose.runtime.snapshots

                @Retention(AnnotationRetention.BINARY)
                @Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
                annotation class AutoboxingStateValueProperty(
                    val preferredPropertyName: String
                )
            """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7h0uFSwqVYLyczr0SvJLW4RIgtBEh6
            lygxaDEAAHx2CidzAAAA
            """,
                """
            androidx/compose/runtime/snapshots/AutoboxingStateValueProperty.class:
            H4sIAAAAAAAA/6VSz28SURD+3vKzqLCtrVJqpdZK68XFxpsmhioqCS1kd9Ok
            4WAe8MQtyz7cfSC9cfP/8M/wYEiP/lHG2WKBA6kHLzPfzsw3883s+/X7x08A
            L3DA8Jp7bV867ZHRkr2+DIThDzzl9IQReLwffJYqMEoDJZty5HgdS3ElTrk7
            EHVf9oWvLhJgDPo5H3LD5V7HqDXPRUslEGHIz6Pc8yRRHekZpRlMIMaw0ffF
            J+H7on3d8YT3BMP6wdPqnG8pn6a/ZNiudqVyHW+xoymU8EJE+dgwFMewv6Ru
            PnmRET+qnJTMM4bcEorN/Y5QVJXmriu/ivY0EDDs3ThgxsvUzVq9bNpnH62y
            bZfNxcj7v5Gd6tJDLYos/KOkLl2ndRHu/6ZasqzwTksJM127y/NlV/Soo33R
            F+Fpjsv2h9pbhtXrZY+F4m2uOCW13jBCj4iFZiU0YGBdio+c8KtIqP2c4dVk
            rKe0rJbS9K3UZEwuTS55+U3LTsaHWpEdJdfiupbTihEzM/WX3+PxXDSp6dGw
            xyFDqfqfj5Tkkrr8TSXPuoohZcmB3xLvHJee0KY5nXHqBE7TFfOfGxRIF6LU
            MR5uTSoTSBLSULiyT7BP/gtiWKGalMAt3MYdgukGNIEM9NCsYm2avYt1bISw
            ASZwD/eRJepmA5EKchVsVfAA2wTxsII8dqgqwCPsNhAL8DjAXoDElU3+AfvU
            hgfXAwAA
            """
            )

        private val MinimalSnapshotStateStub: TestFile =
            bytecodeStub(
                filename = "SnapshotState.kt",
                filepath = "androidx/compose/runtime",
                checksum = 0x992154cd,
                source =
                    """
            package androidx.compose.runtime

            import androidx.compose.runtime.snapshots.AutoboxingStateValueProperty

            fun mutableIntStateOf(initialValue: Int): MutableIntState =
                throw UnsupportedOperationException("Stub!")

            interface State<T> {
                val value: T
            }

            interface MutableState<T> : State<T> {
                override var value: T
            }

            interface IntState : State<Int> {
                @get:AutoboxingStateValueProperty("intValue")
                override val value: Int
                    get() = intValue

                val intValue: Int
            }

            interface MutableIntState : IntState, MutableState<Int> {
                @get:AutoboxingStateValueProperty("intValue")
                @set:AutoboxingStateValueProperty("intValue")
                override var value: Int
                    @Suppress("AutoBoxing") get() = intValue
                    set(value) { intValue = value }

                override var intValue: Int
            }
            """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7h0uFSwqVYLyczr0SvJLW4RIgtBEh6
            lygxaDEAAHx2CidzAAAA
            """,
                """
            androidx/compose/runtime/IntState＄DefaultImpls.class:
            H4sIAAAAAAAA/4VTy27TQBQ9E7d1mhjSlrYQCoXSAElBNUgskLqKipAsmbQi
            VTasJskkncSeicbjKPwVS2DBB/BRiGs3pUBBWcx9nLlz7sv+/uPrNwCv8Jzh
            kKu+0bI/83s6nuhE+CZVVsbCD5RtW25F7Y0Y8DSyQTyJEheMYW3Ep9yPuBr6
            J92R6FkXDkNxKGyHR6lgeF0PF9IeNcIrGgLFUJgjhub/XyaKT5JzbRO/mVrd
            1TOphjlVnvXU6Ikw9iNxbE2MGAhjRP8SbPGYyipKdVnhfqjN0B8J2zVcqsTn
            SmmikprslratNIqIaG9hFy7KDGXqPPhF7dQbgYcbuFmGhwrD+rU2XawzuNMs
            /GTAsF0P/jULD7ewWcIGthiWa/ZcJlnZi+dKGcOxtpFU/jtheZ9bTlghnjq0
            c5aJ1UyAgY0zg1ZXmMnMesHg/b5s2nR7PvOc+nBsGZaOdZ+6rIRSiVYad4U5
            492IkI1Q93jU4UZm/hzceX9RXqCmMpEENa8GzVD7+/aUG9qUFeaPMC9QSpjj
            iCeJILfU1qnpibcyS1CdU3Su0eMlCljCRaurWMYKHNTy755aJ106+II1hs/Y
            /kReAY9JruQ3m3hC0ruIwm3cIf2UToWi6A9AMSHCEgF1Om7GljkOGnk8zZPe
            PkIVBznvPp6R3iW8SjF3P8AJsBPgXoD72A3wAA8D7P0EkuUIJ5QDAAA=
            """,
                """
            androidx/compose/runtime/IntState.class:
            H4sIAAAAAAAA/4WS304TQRTGv9n+2xaEpVKFogiCCjcsEi9MMCaNBrNJraRN
            kISraTvUhe1MMzPb1DuewgfwwofwwpBe+lDGsy1VomIvZuacM+f7zZmZ8/3H
            128AnmGLYZ3LtlZhe+C3VLenjPB1LG3YFX4gbcNyK3JgDG+qZ7zP/YjLjv+u
            eSZadr96o3Ike3FNQSjREXr/5T6D9ycohzTD6v9hOWQZ3I6wRzyKBUNpa/sf
            eIbKzUUZyXvmg7LGr8RWNdUglJ0Re4Q81KontP1IjFJPi1OhtWhPgjXepTPd
            UE6O36gq3fHPhG1qHkrjcykVoUJFdk3ZWhxFBJqheoNfmtTWdsCwUD1XNgql
            /1ZY3uaWU57T7afoO1gy5ZMJDOyc4oMw8XbJaj9lOLy88ArOkjMenltw3Kx7
            unR5secW00Vn19ll9aKXKpN1PPySHn7OZstpN+1l6oteNok+f388/DSJ5zw3
            4e4ll5naAlTk2pTvphQqO9MfX3ZnKnLztTjlcWSDbi8yOTxgmL0eoT5pXH3Y
            KH3n3DKs1MeQQPZDEzYjUfn97qQPpBT6VcSNEeTmG2FHchtrKqfQULFuiYMw
            Imf5inL0F4N6zEGGrpGjZ3eoK13kydsgb552CmTPGMziFpKWnaMgGdikOdla
            o5R1LONRIkYKj0frQzyh9YD2PUIvnCAVoBjgdoBFlMjEnQB3sXQCZkhbPkHe
            YMXgnsF9OspgzmDeYNXANcj/BKBShOC5AwAA
            """,
                """
            androidx/compose/runtime/MutableIntState＄DefaultImpls.class:
            H4sIAAAAAAAA/5VT205TQRRd0wKl7dECCooIqFRtq3K84BMmhmBMJimFiOmL
            T9N2KAOnM82cOQ3+lb6JD36AH2Xcc1ougib4MLPnsrLWXrP3/Pz1/QeANawx
            vBa6Y43qHIVt0+ubWIY20U71ZLiVONGKJNdu1wkny+/knkgix3v9KM6BMUwd
            iIEII6G74XbrQLZdDlmGya50TRElkuFtpX5V9vVq/YyNDmVX2nWGjX8TxFr0
            433j4nAjcaZljpTuplSp+I41fWndZ+KY7Vu5J62VnZPDhuhRdpNKnyS6Uje2
            Gx5I17JC6TgUWhuiUobWDeMaSRQRUeWqZnIoMhTpHfipQrZS5QGu4XoRAUoM
            05fc5jDNkBt4+PYew1yF/+1JAtzAzQJmMMswXnb7KmaoXf2VyXV8Wp7V/ygP
            rzbJUnze0hhl2AywgLve0yLlMxheME7+6ofGRUqHW9KJjnCCpDO9QZbajvkp
            7ycQ9tAvqG0yR8qvnjME5xuNumx3VOg0kdVDR8qbpkM6pbrSspH0WtJ+9Mky
            zNRNW0RNYZXfjw4XPgw9cT1QsaKjjbPqMpQv3u4IS+3hpP0DFnCtpd2MRBxL
            2hZ2TWLb8r3yAvMjiuYlerxABmPeKcU8xjGBLJ6mX4+sUyzUjjHF8A1zX1LM
            M5on0ptFrNIcDFG4hdsUQxolQtHvozoSYcG/GI2cZ/ObPOZxZ6TxhpAZipO1
            hWMsZfD1gsJSqjA3xIwU/GoZ9+jeaxUpnmp5nXxmpEMlI28+M6ocZVkj3Zcp
            /xO8SrkZ7hPmwSdkOVY4yhwP8YjjMSoc1d+/bB9ggQQAAA==
            """,
                """
            androidx/compose/runtime/MutableIntState.class:
            H4sIAAAAAAAA/41T3U4TQRg9sy3d7VJxKaAF/ONPWlS2NpqYYIxEY7JJqaZN
            gISrKR3qwna27sw2eMdT+ABe+BBeGMKlD2X8trRAVISLmfl+zpzzzcw3P399
            /wHgGVYZily2otBvHbq7YacbKuFGsdR+R7gbsebNQHhSNzTXwgRj+FTd5z3u
            Bly23ffNfbGr16qXEgx3/gcy0OjDXl7gpq2iLaK1V2sMzp+SJtIMc1fKmsgw
            LF1L2oTFYLWF3uRBLBimiqV/FMOwfvlJlORd9THUyl2PddgMD33Z7nP3KT9E
            YVdE+jNxTHUjsSeiSLSGwRrvkKbly6H8QjWM2u6+0M2I+1K5XMqQqPyQ7Fqo
            a3EQEJGlzupNF73SJsMoncA7Y0kVSx7F1MXYePUg1IEv3Q2heYtrTjxGp5ei
            bmDJlE0mMLADih/6iVcmq/WUQR4fTdpGwTgfjmUbVtbaKxwfVYwyq1j5dN4o
            p8pGveCkZ8jaPvk2dvI1k5tJWyNOZj5tmY5VX3SySa6SOV1fbL3e2j75MsTZ
            zijhco6VqFYYVq7snbMuS+7t6l5kWL5eOxKSLmKkd3pxz69bxuJbscfjQHud
            bqBMLDHkLkaonRuDTunDVw80w2z9lMuTPV/5RLZ+/uC035NSRG8CrpQgN9vw
            25LrOKKq7EYYR7vinR+QMz1g2fyLg/6BgREaJrL0omn6PjZG6XQl8m5SPEf2
            DYUxcpK/5WB8kExAwyQl8pjoAyYJQAZWaLYp/ZA4ljGNR+QbSOFxfy3iCa11
            yk+R+q0dpDzc9lDwCDlDJmY93MHdHTCFe7i/k6g8UJhTmFdYIEWFvMKEwqTC
            uMKigq2on+H8BhJaxEnABAAA
            """,
                """
            androidx/compose/runtime/MutableState.class:
            H4sIAAAAAAAA/4VR0WoTURA9c3eT3aQxbmOradRaBTHxwa3FBzGlIKIYSBCa
            EIQ83SZr3GZzt+TeDX3cb/HBj/BBlj76UeJsKkUM1Zc7c+aeOTOc+fHz23cA
            L/CQ8FiqySIOJ+f+OJ6fxTrwF4ky4Tzwe4mRJ1HQN9IEDojQOxy86p7KpfQj
            qab+h5PTYGzaR+ul7rWaK7HDwaB91CZ4fzc6sAm7/252UCS408AMZZQEhK1m
            a30BQqHZ4inM1FfM7eY6sTUkFJvMzJPN7iw2Uaj8XmDkRBrJ/WK+tNgqyp9S
            /oBAM66fhzna52zynPA2S6tlURflLF0F4RbcT/UsfWq7WerRgVuza+I97Yvj
            umc1xMss/XjxtXrxpVhp2K7tFR7ZbtFzcrEDwpPr/fvzJrwdDQh7/3E7N2N5
            6YDXV/JMf47N6uPZzBBK/XCqpEkW/F3ux8liHLwLIwY7x5ciw1CHPPG1UjE3
            hbHS7L9AgX1w2ADBF3NRYrSbI5QZb6ByhW/A+p1ZeLCK97HH8Q0zqqxycwSr
            A6+DzQ5quMUptjrYxu0RSOMO6iM+IXY0Ghp3Ne7pHJY0NjQqvwDJFOU0xwIA
            AA==
            """,
                """
            androidx/compose/runtime/SnapshotStateKt.class:
            H4sIAAAAAAAA/5WSXWsTQRSG39l8r7HdRqtN6lc/tKmg2xbBi4ogorC4TaDR
            QMnVJDvGSTYzy+5syGX+laCgufZHiTObQCjihTfvOfOeZ87MnN1fv7/9APAC
            RwRNKoJY8mDmDuQkkglz41QoPmFuR9Ao+SJVR1HFPqgSCIEzolPqhlQM3XZ/
            xAbazRFsTVJF+yHzxBJufyZ41vSO/X/2vri+4ZzgwJfx0B0x1Y8pF4lLhZC6
            xKXOW1K10jDU1NH6/E8iSaNIxooF7YjFGfpuNmCRSUooExQ6Ku3vlWETFF9x
            wdVrgu2mv27RUTEXw/PjbhVV3LRRwQZB1ZCchl0apoyAePp9/liqkAv3gika
            UEX1TazJNKdnSIxUjECzY5NYujjjJjvRWXBKsL+YV+3F3LZ2rCw45caGs5g3
            rBNyVnQsHXOGPCN4+j8j0yc61z7S87EiyL+Vgb73ps8Fa6WTPos/mn0ENV8O
            zLNibtYrc/dy2d4TU55wbb1Zz53A7sg0HrD33KD1Fdr9C8QpLOSxHEIdBRT1
            ei/7w3LaAezvqFzVbnzF5k8zIOxrLWaVEg60VpcUHGzpeLiqljL2caaP8ETH
            l9qt6f63esh5uO1h28Md3PWwg7qHBnZ7IAnu4X4P+QSFBA8SOAke/gF9don0
            8QIAAA==
            """,
                """
            androidx/compose/runtime/State.class:
            H4sIAAAAAAAA/31Qy0rDQBQ9k6RpGl+pz1pFXLYuTBUX4gvcCIWKYIsIXY3t
            WMemE+lMi8t8iws/woUEl36UeFNdqbi5955z77mv94+XVwB7WGfY4Ko7jGX3
            MezEg4dYi3A4UkYORNg03Ig8GEPlqHXQuOdjHkZc9cKLm3vRMYcnvymG4CeX
            h8Pg9YS54tFIMCxWqn/pcpVqq0W+2OjHJpIqPBeGd7nhxFmDsU3rsswUMgMG
            1if+UWaoRlF3h+EgTWZ9q2T5aeJbQWY827stpcmW46VJwHatmnU5H9hlaz9N
            rt+enbcn1y07nhPksg67DJuN/59Bu7AWy8bnxl/XBE3FH/RdbCb57b5hKDRl
            T3EzGlLab8ajYUecyYjA6uVXryup5U0kTpWKSSRjpV2ajxwml9G3XNDXsUrI
            ggf7O7JRnvgS1sgfU0WBNH4bdh1TdUzXMYNZCjFXR4BiG0xjHgttuBqLGksa
            yxorOoP5T7cJbagBAgAA
            """
            )
    }
}
