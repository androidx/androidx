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

@file:Suppress("UnstableApiUsage")

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.bytecode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SnapshotStateListFastIterableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SnapshotStateListFastIterableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(SnapshotStateListFastIterableDetector.ISSUE)

    @Test
    fun fastForEach_onSnapshotStateList_reportsError() {
        lint()
            .files(
                SnapshotStateListStub,
                FastForEachStub,
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.runtime.snapshots.mutableStateListOf
                import androidx.compose.ui.util.fastForEach
                import androidx.compose.ui.util.fastForEachIndexed

                fun test() {
                    val list = mutableStateListOf(1, 2, 3)
                    list.fastForEach { }
                    list.fastForEachIndexed { _, _ -> }
                }
            """
                ),
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Error: Replace fastForEach with toList().fastForEach on SnapshotStateList (fastForEach performs two state reads per entry while toList().fastForEach performs only one for the whole loop) [SnapshotStateListFastIterable]
                    list.fastForEach { }
                         ~~~~~~~~~~~
src/test/test.kt:12: Error: Replace fastForEachIndexed with toList().fastForEachIndexed on SnapshotStateList (fastForEachIndexed performs two state reads per entry while toList().fastForEachIndexed performs only one for the whole loop) [SnapshotStateListFastIterable]
                    list.fastForEachIndexed { _, _ -> }
                         ~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 11: Replace with toList().fastForEach:
@@ -11 +11 @@
-                    list.fastForEach { }
+                    list.toList().fastForEach { }
Fix for src/test/test.kt line 12: Replace with toList().fastForEachIndexed:
@@ -12 +12 @@
-                    list.fastForEachIndexed { _, _ -> }
+                    list.toList().fastForEachIndexed { _, _ -> }
                """
            )
    }

    @Test
    fun fastForEach_onStandardList_noError() {
        lint()
            .files(
                FastForEachStub,
                kotlin(
                    """
                package test

                import androidx.compose.ui.util.fastForEach
                import androidx.compose.ui.util.fastForEachIndexed

                fun test() {
                    val list = listOf(1, 2, 3)
                    list.fastForEach { }
                    list.fastForEachIndexed { _, _ -> }
                }
            """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun forEach_onSnapshotStateList_noError() {
        lint()
            .files(
                SnapshotStateListStub,
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.runtime.snapshots.mutableStateListOf

                fun test() {
                    val list = mutableStateListOf(1, 2, 3)
                    list.forEach { }
                    list.forEachIndexed { _, _ -> }
                }
            """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun fastForEach_onCustomSnapshotStateListSubclass_reportsError() {
        lint()
            .files(
                SnapshotStateListStub,
                FastForEachStub,
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.ui.util.fastForEach

                class MyList<T> : SnapshotStateList<T>()

                fun test(list: MyList<Int>) {
                    list.fastForEach { }
                }
            """
                ),
            )
            .run()
            .expect(
                """
src/test/MyList.kt:10: Error: Replace fastForEach with toList().fastForEach on SnapshotStateList (fastForEach performs two state reads per entry while toList().fastForEach performs only one for the whole loop) [SnapshotStateListFastIterable]
                    list.fastForEach { }
                         ~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/MyList.kt line 10: Replace with toList().fastForEach:
@@ -10 +10 @@
-                    list.fastForEach { }
+                    list.toList().fastForEach { }
                """
            )
    }

    @Test
    fun insideSnapshotStateListSubclass_reportsError() {
        lint()
            .files(
                SnapshotStateListStub,
                FastForEachStub,
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.ui.util.fastForEach

                class MyList<T> : SnapshotStateList<T>() {
                    fun doSomething() {
                        fastForEach { }
                        this.fastForEach { }
                    }
                }
            """
                ),
            )
            .run()
            .expect(
                """
src/test/MyList.kt:9: Error: Replace fastForEach with toList().fastForEach on SnapshotStateList (fastForEach performs two state reads per entry while toList().fastForEach performs only one for the whole loop) [SnapshotStateListFastIterable]
                        fastForEach { }
                        ~~~~~~~~~~~
src/test/MyList.kt:10: Error: Replace fastForEach with toList().fastForEach on SnapshotStateList (fastForEach performs two state reads per entry while toList().fastForEach performs only one for the whole loop) [SnapshotStateListFastIterable]
                        this.fastForEach { }
                             ~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/MyList.kt line 9: Replace with toList().fastForEach:
@@ -9 +9 @@
-                        fastForEach { }
+                        toList().fastForEach { }
Fix for src/test/MyList.kt line 10: Replace with toList().fastForEach:
@@ -10 +10 @@
-                        this.fastForEach { }
+                        this.toList().fastForEach { }
                """
            )
    }

    @Test
    fun fastAny_onSnapshotStateList_reportsError() {
        lint()
            .files(
                SnapshotStateListStub,
                FastForEachStub,
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.runtime.snapshots.mutableStateListOf
                import androidx.compose.ui.util.fastAny
                import androidx.compose.ui.util.fastMap

                fun test() {
                    val list = mutableStateListOf(1, 2, 3)
                    list.fastAny { it > 1 }
                    list.fastMap { it * 2 }
                }
            """
                ),
            )
            .run()
            .expect(
                """
                src/test/test.kt:11: Error: Replace fastAny with toList().fastAny on SnapshotStateList (fastAny performs two state reads per entry while toList().fastAny performs only one for the whole loop) [SnapshotStateListFastIterable]
                                    list.fastAny { it > 1 }
                                         ~~~~~~~
                src/test/test.kt:12: Error: Replace fastMap with toList().fastMap on SnapshotStateList (fastMap performs two state reads per entry while toList().fastMap performs only one for the whole loop) [SnapshotStateListFastIterable]
                                    list.fastMap { it * 2 }
                                         ~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/test/test.kt line 11: Replace with toList().fastAny:
                @@ -11 +11
                -                     list.fastAny { it > 1 }
                +                     list.toList().fastAny { it > 1 }
                Fix for src/test/test.kt line 12: Replace with toList().fastMap:
                @@ -12 +12
                -                     list.fastMap { it * 2 }
                +                     list.toList().fastMap { it * 2 }
                """
                    .trimIndent()
            )
    }

    @Test
    fun fastMethod_inOtherPackage_noError() {
        lint()
            .files(
                SnapshotStateListStub,
                kotlin(
                    """
                package com.example.other

                inline fun <T> List<T>.fastCustom(action: (T) -> Unit) {}
                """
                ),
                kotlin(
                    """
                package test

                import androidx.compose.runtime.snapshots.SnapshotStateList
                import androidx.compose.runtime.snapshots.mutableStateListOf
                import com.example.other.fastCustom

                fun test() {
                    val list = mutableStateListOf(1, 2, 3)
                    list.fastCustom { }
                }
            """
                ),
            )
            .run()
            .expectClean()
    }

    companion object {
        private val SnapshotStateListStub =
            bytecode(
                "libs/snapshotstatelist.jar",
                kotlin(
                        """
                package androidx.compose.runtime.snapshots

                open class SnapshotStateList<T> : MutableList<T> by mutableListOf()

                fun <T> mutableStateListOf(vararg elements: T): SnapshotStateList<T> =
                    SnapshotStateList()
                """
                    )
                    .indented(),
                0xd62acade,
                "META-INF/main.kotlin_module:" +
                    "H4sIAAAAAAAA/2NgYGBmYGBggmIw4LLkUkrMSynKz0yp0EvOzy3IL07VKyrN" +
                    "K8nMTdUrzkssKM7ILykWEg6GMoNLEktSfTKLS7xLuPi4WEpSi0uE2EJSQXwl" +
                    "Bi0GANjobs5jAAAA",
                "androidx/compose/runtime/snapshots/SnapshotStateList.class:" +
                    "H4sIAAAAAAAA/6VX6Vcb1xX/vRFoY2INMhYYCMY2tgVyI+K4WYDSENuJRTCO" +
                    "DSULidNBGsOANOPOG1Gcpg1Nuq/p5u5bun7Ih+ScNjHtOT0cf+wf1dP73owG" +
                    "Ic3UpDlHmvd0l99d35urf//nH/8CcB5/ZTivWxXHNitbxbJdu2Vzo+jULdes" +
                    "GUVu6bf4mu3y4oK/W3B115gzuZsAY1idWpyYW9c39WJVt1aLV1fWjbI7Od1O" +
                    "8ih116wWhfLU4uLk9OTchu1WTau4vlkrmpZrOJZeLdZ0Z8NwePHZK3VXX6lK" +
                    "W5MMWitkAh0Mh/bDJhBnyB8UNYEkQ3zKtEx3miGWH11SkUZXGp1QGQ7vQc84" +
                    "jn7b0ziURkZw1ZGRilE1VikZr46Tdy3xTaroRjYFBYcZOtw1kzM8Ovf/pJlC" +
                    "j+mVCsORfHtWR1+iAPKUy9GXVPSirwsJ9DMkyLOaYbkM2XYdhly+FAK1RHr5" +
                    "ksBaElhDAmuYodO0KsYWAyuRKceo2ZuGihHP1CkikW8z1SrD0XxTCi7Y1SqB" +
                    "mrYlXTwWzpsqyC4QEifnbGe1uG64K45uWryoW5ZNGSAhXpy33fl6tUqOJ/24" +
                    "eBIFhqGwOpcs1yEAs8wT+AQlrbxmlDd8hOd0R68ZJMhwJiSZTZQFAbIqM1HE" +
                    "eBoP4WEVZ5AXQT9CCQwPlaG/kdmwLAxHMIM0CBOPChOPM6S8VMvUDkakb8xT" +
                    "mvD8mpJKLqWPlFRMe9RPUwXLVUN3VMygS1CeoobihityENYGYQ2T8vqCviou" +
                    "4pJAeYaq4bvoit4shSrGiS61RjArtObo4FSpqUtUBN21HZGx0Zaj0+BNir6J" +
                    "Ynr3h4p5XBW41xgGAhfCkIajuXtQCwLqM3QMeH1FSDD05EttepOi19vJDZjn" +
                    "8YKAoXqnbjp2reSdnoRr+7tk2bZEkbiKG94pepWhq0GUpdO90q1QoVYNV0XF" +
                    "S55BMPIwXr0ZcRuUVKxiTciaQpZfqt1yb8ubjdpkA1XBqZELZpD+3L4MNyVs" +
                    "IJTRCNLGLYFFAF1VnVLpeaXC9azXyTp5vmC+ZkjrdHV0cPqh4ja2hMBrKkqY" +
                    "TdP1+LqKz2NL7L4ksySvWrrD88sh8YXQqLThr6D8smhZ8aAToAvUJN6iioad" +
                    "pAS+ynAq7DrZk1n0XEvg6wzTEXfdAX1WsY1vpvENfIveMqExHcmHx1qIumQj" +
                    "rHxPWPk+4YU2K0N34w18hS6Oiu7qRFNqmzEaDBTxAN37G2JDr3plyxQ7etkp" +
                    "lYeZ8vru9lRa6VPSu9tpRTueVpIxWrtoVWj1yUmfPERritZRn12gNZ3M9e1u" +
                    "j3Ukd7c1di6Z7cgql9m48lTi3jvxjmRM65jNaZ39ynj8XFxL9CuP725fvneH" +
                    "EZNgZ4ckK0WsNK1d7SLqbK/2gNRWtUP9Ep3Y4xkhMHvMZzW0Q0RSmiYMyH1O" +
                    "6271I+b70atlo4zktMNSumEl0Ooh1pFwr3Nkt1cASoSM1rcHelTok2Z/lGZG" +
                    "G9iTHpQAvftITY4I7x6Mym2vNhQd07HWmJgfU682HKU1pB3fp9WeyBNUkpN7" +
                    "agpJjPiSpxp2rqe002L/wr07naIHz1FvLjLRoj1tQ9NDG3R5pxbMVUt36w5d" +
                    "Qh0X7AotmTnTMubrtRXDWRRToBiQ7LJeXdIdU/z2iSPXvbmsZG2a3CRSMDnM" +
                    "7M0ldEe2iu3jphfsulM2njYF4lFfdKlNEMfp7uukbwIpMC0r5kaK6XfiCOIH" +
                    "6Kc9zaT0/D1RZhCjHXB47EM8MPZPZF78ENouEn9Hz/tS4R16issUJJih7x9o" +
                    "p3oqOIKcPNE0J5KEgHuEViHbNfY39BR2MKDgXSki1HIey1cTu0E8GHh2VAIN" +
                    "+UDTZEpcDqoAGji7g+MxvB8gDXu8AEnFCZyUTqkSU/ExjxGNJks/1lbnTh/E" +
                    "OSamNDInAJ7wAboL2bN3cc6HOd8O0x3AdKOATwYxjuKPtCYE6phUofHMx77o" +
                    "B5w9G2CLsJ+INYF7YWcD8GwQdlaaaYT9mDSTUqSiZ2jiPkF86uBBTLYGMX0f" +
                    "7Cc/BvZM0Kl5WQxqRoG6gwusqR+8hkwHDUkjZVQfXY7hvY/cR08TjcaLsCYf" +
                    "2MEVpQmyvY88SA/oWdrRhOmnqyWk66wJZ39InvZz+BOt8SA5NGD6SK0uLR3c" +
                    "pcUWUBo3fdD9uRvcwfL/zt3LQe5eoV0jdy+24N+IPI+fPdh51O/TbuWP0W6V" +
                    "yCLf/GhFpqk5Isr1g0VJw3V441usSb+18WmMDu8tfr/e+lxLmdzIADYPFgAN" +
                    "4eEBfCE6gGvib51fgAnfbHxs4AN88T00XkZxqRFrsh4PrMdb6nBN/D3wvSj6" +
                    "XnSOfYA33g2F85zpDLK5jS/7zjzmO5MpZL8iOm0XXyvcxbdbOyITeJLBW/hO" +
                    "4MmbEuy7vienfU+SAuYu3m4tTNJHifnaMfxZrr/FX2S5GH5ITv5oGbESfiw/" +
                    "P8GdEn6Kn5Xwc/xiGYzjl/jVMs5w5Dh+zfEbjjRHF0ev3A9y9MnNCfkc4kKy" +
                    "IPczHDc4dI4KxyzHKscaxwaHLbkux7zcjHBMcJQ4pjkuclzieJ7jZY5XOG7/" +
                    "F6DizWkIFAAA",
                "androidx/compose/runtime/snapshots/SnapshotStateListKt.class:" +
                    "H4sIAAAAAAAA/51SXU8TQRQ9sy3bdkUoCygfigooBZQFQowRJDFGY0MFYxuM" +
                    "6dN0O5Zpd2fJzrThkb/hq7/AN40mhvjojzLeLQ0q9anZnftx7pkze3fuz19f" +
                    "vwPYwhrDQ67qcSTrJ54fhceRFl7cVkaGwtOKH+ujyGiv3IvKhhtRktrsmQwY" +
                    "Q77JO9wLuGp4B7Wm8AlNMbhh2/BaIC7YB+8Z9grV0mX29nJpkMO3Gd7uVB73" +
                    "y+0WqpXKgKI7tHOXlBdKUdzwmsLUYi6V9rhSEXFkRPF+ZPbbQUCsrAhEKJTR" +
                    "WeQY5lqRCaTymp3Qk8qIWPHAKyoTk4D0dQZXGCb9I+G3egqvecxDQUSGpUJ/" +
                    "H38h5USksb18OIyrGHEwjFGGrUEazGCMwd6RSppdhlQhkRzHhAMXkwzj/7kd" +
                    "hrFSr7NXwvA6N5wwK+ykaHasxICBtQg6kUm2TlF9g+H52WneOTt1rCnLsbK0" +
                    "8mOUzqzmyWTdtGu9tNbZfDpLNGvTzadmHNfOsi6cnif746Nt5YcSsU0az0FH" +
                    "hFVY8n0TfbW1lmFIP4vqgmG0JJXYb4c1EVeSkaXhLUU+Dw55LJO8B+bKsqG4" +
                    "accUz745P76oOlJLKj/9MyEMi5erF1f9D80pR+3YFy9koj7d23PYp4cNWEjj" +
                    "/E9/wBBsyu5R9ohwag8jK67zBflvcN99xrVPyb1giaxNVZueAsXD50zkcJ38" +
                    "cpeTwUqPlSW/SivDeomF+117Fw/IPyF0ig6eriJVxEz3ncUN8rhZxBxuVcE0" +
                    "buNOFbbGkMa8xoJGTmNRw/4NCXqxBGgEAAA=",
            )

        private val FastForEachStub =
            bytecode(
                "libs/listutils.jar",
                kotlin(
                        """
                package androidx.compose.ui.util

                inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
                    for (index in indices) {
                        val item = get(index)
                        action(item)
                    }
                }

                inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
                    for (index in indices) {
                        val item = get(index)
                        action(index, item)
                    }
                }

                inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean = true
                inline fun <T> List<T>.fastMap(transform: (T) -> T): List<T> = this
                """
                    )
                    .indented(),
                0x2d3e67d8,
                "META-INF/main.kotlin_module:" +
                    "H4sIAAAAAAAA/2NgYGBmYGBggmIw4LLkUkrMSynKz0yp0EvOzy3IL07VKyrN" +
                    "K8nMTdUrzkssKM7ILykWEg6GMoNLEktSfTKLS7xLuJS4JDC0lmbqlZZk5gix" +
                    "haSC1fBxsZQAWTC+EoMWAwCJA3aYhwAAAA==",
                "androidx/compose/ui/util/TestKt.class:" +
                    "H4sIAAAAAAAA/61WS3MbRRD+ZiVZa728FrbjB46dxCR+4EhRQkgsWeCEGIso" +
                    "TogVB2JeY2ltjy3tpnZXKsPJRVH8B65cOEAVcEs4UK5w419w4G8EesdrvYkr" +
                    "VKpU8+jp7q+/7t4Z/fn8t98BXEGJYYIbJcsUpf1E0aw8Nm09URWJqiPKiYJu" +
                    "O7edIBiDtstrPFHmxnbi7uauXiSpjyG8xW1n2bRu8eIOw+XpvNSStnlhO+n8" +
                    "numUhZHYrVUSW1Wj6AjTsBPL3upSemadYTNTWMi3e09n231l5gqFdPYkj5l5" +
                    "0jrWeWAIciRBzuVNazuxqzubFhdkwA3DdPiR8arprFbL5TRDT8bZEXZWRS/D" +
                    "6SYgYTi6ZfByImc4FpmLoh1EmGGwuKMX9zz7e9ziFZ0UGS5MdxJqkqy5TrYp" +
                    "rgiiiIUQQR9hc8lART/DQIP6TbNc1uVJEK8x+G3xlc7gm57JRTCIoTAGcIoh" +
                    "1pqrIEZIZ1t3KMTp3ExnMBGM4fUwRjHeSrRLRoOYoOCEUTP3CPh8F2Zd/Z/B" +
                    "2TAmcY5CFo5eYYh3ajEEhFHS9xlYjjpsSkxtTbU0VP+UW49WmdbeYwyTJ7UZ" +
                    "oTf5yLmYeunl+zUlW6n2Cvs1lZlvckXdpW/rVvo/eri/QzOIJEOwxstV/e4W" +
                    "w1BrrY/dRZDC5RAu4cpJxU4FcZXh2ou798VVvxbG27jOcKq9mvWsD3cUtX50" +
                    "QiFTVMiga7dkfPm/bptHDDuv/rZpeLthmmWdG269CKr3saWXRJE7uoolhkg9" +
                    "IzL8aCMNci+J3eGPGW68PLHOb6LyynkeabZDZaSUyDoWN+wt06qoWGkmKyk1" +
                    "kZX7/mPAO7rDS9zh5EGp1Hz0JCnuALoS9ki0L9wdNblSusTYxOHB+dDhQUjR" +
                    "lKNpUk7DyvGkaOrRQTikqL7RpHZ4MKok2Vm/enigKbNq3B9XVpSkLxXX/KOx" +
                    "o507JlkysPLsW/WPJ4wsrmo93a2GyWpA6qsttsGGbVLrJdtQN8RwC2KoFTEy" +
                    "WlftZhtttiVZw/bZ10owFFCffZdKMjdNKSaTV3AvXC/HLfdgu1B+Uw698xf3" +
                    "6LXw3zRLdMtH1xxe3KNKFfhmmfZ9eWHoq9XKpm55knjeLPLyOreEu/eEvWti" +
                    "2+BO1aL11P2q4YiKnjNqwhZ0XH8elxpPL8NYu1rLaWjNrFpFfVm43kc81fUO" +
                    "RbraFPjdtqFxBAH0wIcN2v1I8h6aF2bjoSfQ5uJxGn2LvuzsIQaeYpjhqn98" +
                    "0P89xmbHn+K0goXA3EjgKaYUPPzGx3745+9fydqHT2jshfIcY4yxcXxK2yGC" +
                    "iRHcGZpnCeQyra9DxWfyX5Ur68UbOE8rDTO4gGlyE8ObtJ6moCgk0piTIS+g" +
                    "n+QMn7vNjyC+oDmqkECV34E7jmAeFz1OP9PscsqexGmiwWn8Cd5yeS34PF5/" +
                    "deF1polXmHhFKMIoXeZhLKKviVe0zmu+ziuJtMcri4zHK0u8Fuu8Et15ZfGO" +
                    "x2uVlNyPXTvmdUPy8v8kzd3QesgqRk7dUCYlrIKbHqyG9zxYDUtN6Xy3O+wt" +
                    "LFO9usLmJOzsL22w8RbYD+qwt+uwK02w74N7li5elxAUbMrxEYo0PyBpnlJ6" +
                    "ZwO+HFbl7y7u0YwPc7iPtQ0wGwU82EDMRsDGuo2Hcuy38ZGNeRsfy21Wjks2" +
                    "btlY+RdKR84N5AsAAA==",
            )
    }
}
