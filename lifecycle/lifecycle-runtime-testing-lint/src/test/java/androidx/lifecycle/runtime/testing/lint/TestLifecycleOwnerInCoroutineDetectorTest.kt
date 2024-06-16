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

package androidx.lifecycle.runtime.testing.lint

import androidx.lifecycle.testing.lint.TestLifecycleOwnerInCoroutineDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TestLifecycleOwnerInCoroutineDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = TestLifecycleOwnerInCoroutineDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TestLifecycleOwnerInCoroutineDetector.ISSUE)

    private val lifecycleStub: TestFile =
        bytecode(
            "libs/lifecycle.jar",
            kotlin(
                    """
        package androidx.lifecycle

        abstract class Lifecycle {
            enum class State { CREATED, STARTED, RESUMED }
        }
        """
                )
                .indented(),
            0xf3659215,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AJcrFnVqRmFuQk6qXlp8vxBaSWlzi
        XcKlySWenV+Sk5lXoZecX5RfWpKZl1qsVwKUFOIDKXEqzcxJSS0q9i5RYtBi
        AAAIhggPWgAAAA==
        """,
            """
        androidx/lifecycle/Lifecycle＄State.class:
        H4sIAAAAAAAA/41T30/TUBT+btetXa0yJiIg/sJNuk0ZoII6RCYMnQxJKC4x
        eypbxULXJmtH9I0n/xD/AsFEjUaz+OgfZTz3DgkEozz03Hu++51zvnvu6c9f
        X74DuI0lhhHLa7R8p/E67zov7fqbumvnK392KTO0QlsBY5iqbFrbVt61vI18
        yWs3Zyr/DyzMFhjOHI1TIDPEZhzPCWcZ+o1Dac2w5XgbhXKmyhAxMlUdMSga
        oogzyOErJ2BInaQqg5ayqVTKs5o2Q/J4CYbTXYbfajie5TKwMqnatty2TVVG
        jUztZIWUVLVYeV4yGdInitBxFn1xSDjHcLZ2SNjK+qZdDwsKzjNE667vkfA+
        I3OcoWMQQxoGcEFBHwkQmldeMtz9SyszJ7vFncOh864VBIW/5Tr6kKTkEq7w
        57lKkoUM3o8/PVTmV0vFtdKCjjR0fuPrhJlrxVWBGV0sQ9hqyXy+zLFcF7vB
        oM7UXTEgKtIaQXEVhoqcjhSS3L/F0FvZ8kMi5Zft0GpYoUXXkJrbERprxk2U
        nnSLoNcO98Zp15hgmO7s6Jo0IGlSIqFJqqz+eMsGOjuTalJOSuOdnXH2SFGl
        H+9iUkLalNQIfTJ9UR4+ySg1hv/VUAVz1ArRVQb9AB7bCmmA5/0GoT0Vx7Of
        tZvrdmvNWnfFePp1y61aLYf7+2DcdDY8K2y3eKKy59kt8Sy8sZrpt1t1e9Hh
        vMHVthc6TbvqBA4FFj3Pp+qO7wVzE9SoKN1dnhvkTwDQauyvOb4mhvgw8l4R
        k/40sgvklRAB75mSzQ1/grZLW4lAEIUJ8iLZK10KTvFEYncaZ8S5gh4kKOKx
        iFMRRy+SdMJT9wsGoO2h/yOGO7j4XvhxmqTL+xyDojhLS0rZzxjpQOIcJqrq
        3RNco1pcfuogc5r08LPB2DdIS9nIHkbNrLyHrJmN7uGmme0WUjFGXzdkEbLA
        xr5CepHMRz5h/ANGhTMhCycrnMmocG5+xu0P6N8VYg63Q8YTIYpmBEU8ou52
        rx5BWazzeErrAzq/Q+ypGiJlTJdxlyzucXO/jAJmamABsWZriAXQAzwMoARI
        BzAC5H4DVwG07rEFAAA=
        """,
            """
        androidx/lifecycle/Lifecycle.class:
        H4sIAAAAAAAA/31QTU8UQRB91TM7uw6jLCiwq8hXiAEPDhATEiVEJTHZZNRE
        yF721DvTYLOzPcl0L8Hb/hb+AScTD2bD0R9lqFnUI314Ve/1q+rq+v3n5y8A
        r7FGWJYmKwudXca5PlXp9zRXcfIvq4MIzXN5IeNcmrP4S/9cpa4OjxAcaKPd
        IcHb2u5GqCEI4aNO8N03bQkryX2N3xLmkkHhcm3iT8rJTDrJmhheeDwYVVAj
        0IClS12xHc6yXcLmZByFoiVC0ZyMQ9EQjRetyXhP7NAb8j7Ubq4C0RSVd4+4
        D2rHTjpF2LhvmM2pqY4lQvRffDVw/JejIuPq2UQb9Xk07KvyRPZzVuaTIpV5
        V5a64n/FqGOMKo9yaa3iDYTHxahM1Udd3bW/jozTQ9XVVrP5vTEFP6oLY/11
        CN5cdaiaGAHjU2bxlPMiXv5A45oTgWeMwVT0scwY3RnwACFHD88ZQ9Za7F18
        18bKtKqNVY77rM+wN+rB6+BhB48YMVtBs4M5zPdAFo/xpAffIrRYsAgsFm8B
        Whw3Ci0CAAA=
        """
        )

    private val testLifecycleOwnerStub: TestFile =
        bytecode(
            "libs/testlifecycleowner.jar",
            kotlin(
                    """
        package androidx.lifecycle.testing

        import androidx.lifecycle.Lifecycle

        class TestLifecycleOwner {
            var currentState: Lifecycle.State = Lifecycle.State.STARTED
        }
        """
                )
                .indented(),
            0x5140a562,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AJcrFnVqRmFuQk6qXlp8vxBaSWlzi
        XcKlySWenV+Sk5lXoZecX5RfWpKZl1qsVwKUFOIDKXEqzcxJSS0q9i5RYtBi
        AAAIhggPWgAAAA==
        """,
            """
        androidx/lifecycle/testing/TestLifecycleOwner.class:
        H4sIAAAAAAAA/41TXU8TQRQ9s213S62l5ZviJyDQImxB3kpQREmaVDC0aUx4
        WrYjTNnuJrvTim88+UP8BWo0Gh9Mw6M/yninrFg+jLzcO/fec8+ZuTPz89f3
        HwBWsMKwYLl13xP1I9MRr7n91na4KXkghbtvVsmX/2S337jcN8AY0g2rbZmO
        RYjtvQa3pYEIg74qXCHXGCJzuVoSMegJRGEwTF6hcMY6XZGW5Ab6GIxKdX2n
        +vwZw3T5/y3FJG4g2YcEbjIk7Zbvc1d2K0n0q4KGNENUHoiAwbyK8N+nLDL0
        73O50UPKMDOXu862GKbKnr9vNrjc8y3hBqbluh6VhEfrLU9utRxHCQQXBWbn
        rsOfq9GkVql54fFaHOMMdw496QjXbLSbpnAl913LMUuu9Elb2IGBCYZh+4Db
        h6H4S8u3mpyAXcmLd1nsyVQUyX5RXedt3EngFu4yZMqh4AsurbolLTqM1mxH
        6EExZWIM7JBSR0JFBVrVlxg2O8dDCW1MS2jpznFCi2unQTw61jle1grsaezk
        va6ltZ1sOpLVCtFXJ+9SlEklOsfZaDyW1iejcSMdV2zLjJQwfPnaFg8lw8RO
        y5WiyUtuWwRiz+Hrf8dP72HDq9Oo+8vC5Vut5h73qxZhGAbKnm05NcsXKg6T
        0xe5zkZ3jjRR8Vq+zTeF6hkPe2qX1LFEbzKqxkSenij5BYp08iPkI+Rj3WiR
        ojXC0DmRzH9FPP8JqS/IfKRYg0k2hUi3k/4X/QIdBYU8xWMAg+QzGMIwoRWX
        SRVVi+U/I/PhjETvJo2e5ljY3LutDEYxFhI9Ia8p9PxA9hvu5efPbUp16CHf
        yCku5FOr8a7GklJkIbWG5a59iEfkNyl7n7YwuYtICVMlTJPFA2VmSpjF3C5Y
        gBzyu+gLMBhgPoAe0D9CMsBQgOEAowHGfgPeFoO+3AQAAA==
        """
        )

    private val coroutineStub: TestFile =
        bytecode(
            "libs/coroutinecontext.jar",
            kotlin(
                    """
        package kotlinx.coroutines

        object EmptyCoroutineContext : CoroutineContext

        interface CoroutineContext
        """
                )
                .indented(),
            0x37041da2,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AJcrFnVqRmFuQk6qXlp8vxBaSWlzi
        XcKlySWenV+Sk5lXoZecX5RfWpKZl1qsVwKUFOIDKXEqzcxJSS0q9i5RYtBi
        AAAIhggPWgAAAA==
        """,
            """
        kotlinx/coroutines/CoroutineContext.class:
        H4sIAAAAAAAA/41OPUsDQRB9s2c+PL8uRiHWYusmwc5KAsJBRFCwuWpzWWVz
        l13I7oUr87ssJLU/Spwz2Fg5A2/evGHezOfX+weAG5wRLgsXSmNrmbuVq4Kx
        2svJL504G3QdOiBCslBrJUtl3+TjbKFzViNCb7rblw86qLkK6pYgluuI7amB
        FoEKlmrTdENm8xHhfLvpxmIgYpEwex1sN2MxpGY4JlxN//ESnwGh/1e+LgIh
        fnbVKtf3ptSEi6fKBrPUL8abWanvrHVBBeOsb/M97GEXAqc/2EOf64i9W5zt
        DFGKToouI/YbiFMc4DADeRzhOIPwOPFIvgHKkw26UwEAAA==
        """,
            """
        kotlinx/coroutines/EmptyCoroutineContext.class:
        H4sIAAAAAAAA/41STW/TQBB9uwmN6xqalo+mlO9SqfSA24obFVKJimQpGIlW
        kaqeNs6qbOLsInsdhVtO/BD+QcWhEkgoghs/CjFrAkg0B2ztzL63M88zs/7+
        49MXAE+wwbDZNzZVehQmJjOFVVrm4cHgrX3X/I2bRls5sjUwhnpPDEWYCn0a
        vur0ZEJshWF9hsbF9EsMc3tKK/uMobL5qB2gBs9HFfMMVftG5Qxbrf+t5imD
        t5ekpZwP7jS8KD482o+bBwEWEcwTWafSWiY7DXvSdjKhdB4KrY0VVhnax8bG
        RZqS1NL0u+FLaUVXWEEcHwwrNCTmDJXO+kSNlEPbtOvukPZk7Pu8wcs1GXvf
        3vPGZLzLt9nzmse/fpjjde5Cdxk2ZjU2o6er/3KP+5Zh7XWhrRrISA9Vrjqp
        3P/bBc2uabqSYbFFWXEx6MjsSFAMw3LLJCJti0w5PCX9Q1NkiXyhHFidCrcv
        yGKH5lcFXRmtVTdQ8nepeYeXyXN66f4I3SMUkmduUFvn8M/K4/vTYJDMA7LB
        rwAs0A6UePlP8gpFu2fhM/jxOa58xNJZSXCsl/YOHpa/K82HBK6doBLheoQb
        ZLHiTCOiGm+egOVYwy06zxHkuJ3D+wnnrXWf6wIAAA==
        """
        )

    private val runTestCoroutineStub: TestFile =
        bytecode(
            "libs/testbuilders.jar",
            kotlin(
                    """
        package kotlinx.coroutines.test

        import kotlinx.coroutines.*

        public fun runTest(
            context: CoroutineContext = EmptyCoroutineContext,
            timeout: Int = 0,
            testBody: () -> Unit
        ) { }
        """
                )
                .indented(),
            0xd166e09a,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AJcrFnVqRmFuQk6qXlp8vxBaSWlzi
        XcKlySWenV+Sk5lXoZecX5RfWpKZl1qsVwKUFOIDKXEqzcxJSS0q9i5RYtBi
        AAAIhggPWgAAAA==
        """,
            """
        kotlinx/coroutines/test/TestBuildersKt.class:
        H4sIAAAAAAAA/61TW08TURD+Tm+7ra2UcpG7KLeCyBbEW0pMoIFkBaqxSEzw
        5bBd6rbbXbJ7lsCL4W/4M/SJ+GB49kcZ52xbRW0iD7TJzHfm8s2cmT3ff3z9
        BmAVzxlmG66wLedUM1zPDYTlmL4mTF9oeyQ2Asuump6/LRQwhmydn3DN5k5N
        e3VYNw2yRhkUL3BkMIOe3+nCVurAkusI81QU9XaUVj9pakeBYwjLdXxtq40K
        xfl9hnc3w7XW8b91LFF8EVJP7bheTaub4tDjFgVzx3EFbyWWXVEObLtI1zJa
        JVQkGSauVLHI7Dnc1nRHeJRvGb6CWwwDxgfTaLQJXnOPN00KZJjL7/w9t+IV
        S0WS1KixNDK4nUIaPQyqXMGGWz1T0cswc61RUMvCappkZGA6w+T/xszQ017d
        dNU84oFNie9vaIX6v3eWo893Id9sHouzvysoGKYp6OXK3nq5tMmw0K2trpnF
        NEYxlsQIxmnV17iMgrtpxJFIIYJ7DL2dq+2agle54DSnSPMkSg+GSRGn6TYk
        iJD91JKoQKi6zPDy8nwodXmeimQjoRpqqazaPo3ks5fnI5ECW1FViiEUDVGM
        UHwlnU2MqLlYjvwFRTKuMCqJXKefPxZ39XUuNWhxsZJbNcmxQxcrB81D09vj
        h7Yp812D2/vcs+S5bUxWrJrDReARnn4TOPLD0Z0Ty7fI/evbXf/9MBgyFcGN
        xi4/blOkKm7gGeaWJQ/DbY79FsOVRCzTWGOQvwiG5ZwRJRuwTWc5vMxCLnWB
        7GIuR/JzGLZCMkFXTyOFR4QnW4FIoi8kyqAfA+SXqBeDlLEa5il4TLonSi41
        DAx1Nok7GCIsq5aJKk56bDz28RNSXzBxicnt8Tgd4tG1hdHFC9xvNfGEZAyR
        jBK2MxhmJeivUNEENaJSCwmKexpGF/CMdImqTFHk9AGiOmZ0zJLEnI485nUs
        4MEBmI9FPDyA6iPuY8lH0kefj/4Q9/rQfgIMLZFBnQUAAA==
        """
        )

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package example.foo

                import androidx.lifecycle.Lifecycle
                import androidx.lifecycle.testing.TestLifecycleOwner
                import kotlinx.coroutines.test.runTest

                fun testSetCurrentStateInRunTest() = runTest {
                    val owner = TestLifecycleOwner()
                    owner.currentState = Lifecycle.State.RESUMED
                }

                fun testSetCurrentStateInRunTestWithTimeOut() = runTest(timeout = 5000) {
                    val owner = TestLifecycleOwner()
                    owner.currentState = Lifecycle.State.RESUMED
                }
            """
                ),
                coroutineStub,
                lifecycleStub,
                runTestCoroutineStub,
                testLifecycleOwnerStub
            )
            .run()
            .expect(
                """
src/example/foo/test.kt:8: Error: Incorrect use of currentState property inside of Coroutine, please use the suspending setCurrentState() function. [TestLifecycleOwnerInCoroutine]
                fun testSetCurrentStateInRunTest() = runTest {
                                                     ~~~~~~~
src/example/foo/test.kt:13: Error: Incorrect use of currentState property inside of Coroutine, please use the suspending setCurrentState() function. [TestLifecycleOwnerInCoroutine]
                fun testSetCurrentStateInRunTestWithTimeOut() = runTest(timeout = 5000) {
                                                                ~~~~~~~
2 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package example.foo

                import androidx.lifecycle.Lifecycle
                import androidx.lifecycle.testing.TestLifecycleOwner
                import kotlinx.coroutines.test.runTest

                fun testSetCurrentStateInRunTest() = runTest {
                    val owner = TestLifecycleOwner()
                    owner.setCurrentState(Lifecycle.State.RESUMED)
                }
            """
                ),
                coroutineStub,
                lifecycleStub,
                runTestCoroutineStub,
                testLifecycleOwnerStub
            )
            .run()
            .expectClean()
    }
}
