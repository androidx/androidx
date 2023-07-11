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

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class AutoboxingStateValuePropertyDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = AutoboxingStateValuePropertyDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AutoboxingStateValuePropertyDetector.AutoboxingStateValueProperty)

    @Test
    fun testReadAutoboxingPropertyAsVariableAssignment() {
        lint().files(
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.mutableIntStateOf

                    fun valueAssignment() {
                        val state = mutableIntStateOf(4)
                        val value = state.value
                    }
                """.trimIndent()
            ),
            AutoboxingStateValuePropertyStub,
            MinimalSnapshotStateStub
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:7: Warning: Reading value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    val value = state.value
                      ~~~~~
0 errors, 1 warnings
            """
        ).expectFixDiffs(
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
        lint().files(
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.mutableIntStateOf

                    fun valueAssignment() {
                        val state = mutableIntStateOf(0)
                        state.value = 42
                    }
                """.trimIndent()
            ),
            AutoboxingStateValuePropertyStub,
            MinimalSnapshotStateStub
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:7: Warning: Assigning value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    state.value = 42
          ~~~~~
0 errors, 1 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 7: Replace with `intValue`:
@@ -7 +7
-     state.value = 42
+     state.intValue = 42
            """
        )
    }

    companion object {
        private val AutoboxingStateValuePropertyStub = bytecodeStub(
            filename = "AutoboxingStateValueProperty.kt",
            filepath = "androidx/compose/runtime/snapshots",
            checksum = 0x2c564988,
            source = """
                package androidx.compose.runtime.snapshots

                @Retention(AnnotationRetention.BINARY)
                @Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
                annotation class AutoboxingStateValueProperty(
                    val preferredPropertyName: String
                )
            """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7h0uFSwqVYLyczr0SvJLW4RIgtBEh6
                lygxaDEAAJG2tCtzAAAA
            """,
            """
                androidx/compose/runtime/snapshots/AutoboxingStateValueProperty.class:
                H4sIAAAAAAAA/6VSTXMSQRB9s3ytqGETEyXESBIjiRcXU960yiKKShUJ1O5W
                qlIcrIEdccOyg7sDwo2b/8Of4cGicvRHWc4EAxyoePDS/ba7X/fr3vn1+8dP
                AC9wSPCaBm7IPXdotni3xyNmhv1AeF1mRgHtRZ+5iMxSX/AmH3pB2xZUsDPq
                91k95D0WilEKhMC4oANq+jRom7XmBWuJFGIE+XmUBgGXVI8HZmkGU0gQbPRC
                9omFIXOvO57SLiNYP3xanfNtEcrpLwm2qx0ufC9Y7GgxwQKFZD4xUOIIDpbU
                zScvMpLHldOSdU6QW0JxaNhmQlatUN/nX5k7DUQE+zcOmPEydatWL1vO+Ue7
                7DhlazHy/m9kp7r0UIsiC/8oqXPfa43U/m+qJdtWd1pKmOnaW54v+6wrOzqj
                HlOnOSk7H2pvCVavlz1hgrpUUJnUuoOYfEREGV0ZEJCOjA899VWUyH1O8Goy
                NtJaVktrxlZ6MpZuRTr98puWnYyPtCI51teShpbTijErM/WX35PJXFzXjLjq
                cURQqv7nI5Vypbr8TSXPOoIgbfN+2GLvPF8+oU1rOuPMi7ymz+Y/NypIXYjL
                jkm1tVSZgi6RhsKVfYID6b8ggVuyJs1wG3dwV8KVBjSGDAxlVrE2zd7DOjYU
                bIAw3McDZCV1s4FYBbkKtip4iG0J8aiCPHZkVYRd7DWQiPA4wn6E1JXV/wB4
                ImM31wMAAA==
            """
        )

        private val MinimalSnapshotStateStub: TestFile = bytecodeStub(
            filename = "SnapshotState.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0x575bf828,
            source = """
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
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7h0uFSwqVYLyczr0SvJLW4RIgtBEh6
                lygxaDEAAJG2tCtzAAAA
            """,
            """
                androidx/compose/runtime/IntState＄DefaultImpls.class:
                H4sIAAAAAAAA/4VTy27TQBQ9E7d1SAxpS1sIhUJpgKSgGiQWSF1FRUiWTFoR
                lA2rSTJJJ7FnovE4Cn/FEljwAXwU4tpJKVBQFnMfZ+6c+7K///j6DcBLPGM4
                4qpvtOzP/J6OJzoRvkmVlbHwA2XblltRey0GPI1sEE+ixAVjWB/xKfcjrob+
                aXcketaFw1AcCtvhUSoYXtXDpbTHjfCShkAxFOaYofn/l4nik+Rc28RvplZ3
                9UyqYU6VZz0zeiKM/Ugc2xMjBsIY0b8AWzymsopSXVR4EGoz9EfCdg2XKvG5
                UpqopCa7pW0rjSIi2l/ahYsyQ5k6D35RO/VG4OE6bpThocKwcaVNFxsM7jQL
                Px0w7NSDf83Cw01slbCJbYbVmj2XSVb28rlSxnCsbSSV/1ZY3ueWE1aIpw7t
                nGWimAkwsHFm0OoKM5lZzxm835dNm24vZp5TH40tw8qJ7lOXlVAq0UrjrjDv
                eTciZDPUPR51uJGZvwB3383LC9RUJpKg5uWgGWp/355xQ5uywvwR5gVKCXMS
                8SQR5JbaOjU98UZmCaoLis4VerxAASuYt3oNq1iDg1r+3VPrpEuHX7DO8Bk7
                n8gr4BHJtfxmC49JevMo3MJt0k/oVCiK/gAUEyIsEVCn42ZsmeOgkcfTPOnt
                Q1RxmPMe4CnpPcKrFHPnA5wAuwHuBriHvQD38SDA/k8Blj2alAMAAA==
            """,
            """
                androidx/compose/runtime/IntState.class:
                H4sIAAAAAAAA/4WS304TQRTGv9n+2xaEpYJCUQRBLTdsJV6YYEyIBrNJraRN
                kISraTutW7YzzcxsU+94Ch/ACx/CC0O49KGMZ1tQomIvZuacM+f7zZmZ8/3H
                128AnqHMsMFlW6uwPfJbqj9QRvg6ljbsCz+QtmG5FTkwhjfVHh9yP+Ky679r
                9kTL7lVvVI5lL64pCCW6Qu+93GPw/gTlkGZY+z8shyyD2xX2iEexYFgqb/8D
                z7B/c1FG8oH5oKzx92OrmmoUyu6YPUYeajUQ2n4kxtJAi47QWrSvgjXepzPd
                UF4dv1lVuuv3hG1qHkrjcykVoUJFdk3ZWhxFBJqheoNfmlR5O2BYqJ4qG4XS
                fyssb3PLKc/pD1P0HSyZ3GQCAzul+ChMvApZ7acMh+dnXsFZdibDcwuOm3U7
                y+dnu24xXXQqToXVi16qRNbxxZf0xedstpR2016mvuhlk+jz98cXn67iOc9N
                uLvJZaa2ABW5PuW7KYXKzgwnl92Zitx6LTo8jmzQH0QmhwcMs9cj1CeNyw8b
                p++cWobV+gQSyGFowmYk9n+/O+kDKYV+FXFjBLn5RtiV3Maayik0VKxb4iCM
                yFm5pBz9xaAec5Cha+To2R3qShd58jbJm6edAtkzBrO4haRl5yhIBrZoTrbW
                KWUDK3iUiJHC4/H6EE9oPaB9j9ALJ0gFKAa4HWARS2TiToC7WD4BM6QtnSBv
                sGpwz+A+HWUwZzBvsGbgGuR/AkgwZTK5AwAA
            """,
            """
                androidx/compose/runtime/MutableIntState＄DefaultImpls.class:
                H4sIAAAAAAAA/5VT205TQRRd0wKF9igXBUUEVKq2VXu84BMmpsGYTFIKEdMX
                n6btUAZOZ5o5cxr8K30TH/wAP8q457RcBE3wYe4ra+21956fv77/ALCOdYbX
                QnesUZ2jsG16fRPL0CbaqZ4MtxInWpHk2u064WTxndwTSeR4rx/FOTCGmQMx
                EGEkdDfcbh3ItsshyzDZla4pokQyvC3Vr8q+Ua6fsdGl7Eq7wVD7N0GsRT/e
                Ny4Oa4kzLXOkdDelSsV3rOlL6z4Tx3zfyj1preycXDZEj6KbVPok0LW6sd3w
                QLqWFUrHodDaEJUytG8Y10iiiIhKVzWTQ4GhQHngpwrZUpkHuIbrBQSYZpi9
                5DaHWYbcwMO39xgWSvxvKQlwAzfzmMM8w3jR7auYoXL1LJPr+LQ81f8oDy83
                yVJ83tIYRdgMsIS73tMyxTMYPjBO/uqHxkVKh1vSiY5wgqQzvUGW2o75adJP
                IOyh31DbZI6U3z1nCM43GnXZ7qjQaSDVQ0fKm6ZDOtN1pWUj6bWk/eiDZZir
                m7aImsIqfx5dLn0YeuJ6oGJFV7Wz6jIUL77uCEvt4aT9AxZwraXdjEQcSzrm
                d01i2/K98gKLI4rmJXq8QAZj3imtUxjHBLJ4mn49sk5rvnKMGYZvWPiSYp7R
                PJG+LKNKczBE4RZu0xrSmCYU/T6qIxHmfcZo5DybP0xhEXdGGm8ImfGpriwd
                YyWDrxcUVlKFhSFmpOB3q7hH716rQOuplteZyox0qGTkzUdGlaMoK6T7MuV/
                glcpN8N9wjz4hCzHGkeR4yEecTxGiaP8GxKjsqaBBAAA
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
                bmDJZCUTGNgBxQ/9xCuT1XrKII+PJm2jYJwPx7INK2vtFY6PKkaZVax8Om+U
                U2WjXnDSM2Rtn3wbO/mayc2krREnM5+2TMeqLzrZJFfJnK4vtl5vbZ98GeJs
                Z5RwOcdKVCsMK1f2zlmXJfd2dS8yLF+vHQlJFzHSO72459ctY/Gt2ONxoL1O
                N1AmlhhyFyPUzo1Bp/ThqweaYbZ+yuXJnq98Ils/f3Da70kpojcBV0qQm234
                bcl1HFFVdiOMo13xzg/ImR6wbP7FQf/AwAgNE1l60TR9HxujdLoSeTcpniP7
                hsIYOcnfcjA+SCagYZISeUz0AZMEIAMrNNuUfkgcy5jGI/INpPC4vxbxhNY6
                5adI/dYOUh5ueyh4hJwhE7Me7uDuDpjCPdzfSVQeKMwpzCsskKJCXmFCYVJh
                XGFRwVbUz3B+A5uahrjABAAA
            """,
            """
                androidx/compose/runtime/MutableState.class:
                H4sIAAAAAAAA/4VR0WoTURA9c3eT3aQxbmOradRaBTHxwa3FBzGlIKIYSBCa
                EIQ83SZrvM1mt+TeDX3cb/HBj/BBlj76UeJsKkUM1Zc7c+aeOTOc+fHz23cA
                L/CQ8FhGk0WsJuf+OJ6fxTrwF0lk1Dzwe4mRJ2HQN9IEDojQOxy86p7KpfRD
                GU39Dyenwdi0j9ZL3Ws1V2KHg0H7qE3w/m50YBN2/93soEhwp4EZyjAJCFvN
                1voChEKzxVOYqa+Y2811YmtIKDaZmSeb3VlsQhX5vcDIiTSS+8V8abFVlD9u
                /oBAM66fqxztczZ5TnibpdWyqItylq6CcAvup3qWPrXdLPXowK3ZNfGe9sVx
                3bMa4mWWfrz4Wr34Uqw0bNf2Co9st+g5udgB4cn1/v15E96OBoS9/7idm7G8
                dMDrR/JMf47N6uPZzBBKfTWNpEkW/F3ux8liHLxTIYOd40uRodKKJ76Oopib
                VBxp9l+gwD44bIDgi7koMdrNEcqMN1C5wjdg/c4sPFjF+9jj+IYZVVa5OYLV
                gdfBZgc13OIUWx1s4/YIpHEH9RGfEDsaDY27Gvd0DksaGxqVX4ySaL/HAgAA
            """,
            """
                androidx/compose/runtime/SnapshotStateKt.class:
                H4sIAAAAAAAA/5WSXW/TMBSGX6efC2XLCoO142sbsA4Jsk1IXAwhIQRSRNZK
                C1RCvXIbU9ymdpQ4VS/7r5BAgl7zoxB2WqmaEBfcvOf4PY+P7ZP8+v3tB4Dn
                OCJoUREmkoczdyAnsUyZm2RC8QlzA0Hj9ItUgaKKvVcVEAJnRKfUjagYup3+
                iA20WyDYnmSK9iPmiSXc+UzwtOUd+//sfXF1wznBoS+ToTtiqp9QLlKXCiF1
                iUudt6VqZ1GkqaP1+R9FmsWxTBQLOzFLcvTtbMBik1RQJSgFKuvvV2ETlF9y
                wdUrgp2Wv24RqISL4flxt4YartvYwCZBzZCcRl0aZYyAePp9/liqiAv3gika
                UkX1TazJtKBnSIxUjUCzY5NYujjjJjvRWXhKcLCY1+zF3LZ2rTw41eams5g3
                rRNyVnYsHQuGPCN48j8j0yc6Vz7Ss7EiKL6Rob73ls8Fa2eTPks+mH0EdV8O
                zLMSbtYrc+9y2d4TU55ybb1ez53ADmSWDNg7btDGCu3+BeIUFopYDqGBEsp6
                vZ//YQXtAPZ3bHyqX/uKrZ9mQDjQWs4rFRxqrS0pONjW8eGqWsnZR7k+wGMd
                X2i3rvvf6KHg4aaHHQ+3cNvDLhoemtjrgaS4g7s9FFOUUtxL4aS4/wetLc8s
                8QIAAA==
            """,
            """
                androidx/compose/runtime/State.class:
                H4sIAAAAAAAA/31Qy0rDQBQ9k6RpjK/4bquIy+rCVHEhvsCNUKgItojQ1diO
                dWw6KZ1pcZlvceFHuJDg0o8Sb1pXKm7uvefce+7r4/P1DcABNhg2uWoPYtl+
                Cltxrx9rEQ6GysieCOuGG5EHYyifNI5qj3zEw4irTnh19yha5vjsN8UQ/OTy
                cBi8jjA3PBoKhuXy9l+6XHm70SC/UOvGJpIqvBSGt7nhxFm9kU3rssx4mQED
                6xL/JDNUoai9x3CUJnO+VbD8NPGtIDOe7d0X0mTH8dIkYPtWxbpeDOySdZgm
                t+8vzvuz65YczwlyWYd9hq3a/8+gXViDZeNzo8k1QV3xvn6IzTi/2zUMU3XZ
                UdwMB5T26/Fw0BIXMiJQvJ70upFa3kXiXKmYRDJW2qX5yGF8GX3LBX0dRUIW
                PNjfkY3S2BewTv6UKqZI4zdhVzFdxUwVs5ijEPNVBFhogmksYqkJV2NZY0Vj
                VWNNZzD/Bf5sje8BAgAA
                """
        )
    }
}
/* ktlint-enable max-line-length */
