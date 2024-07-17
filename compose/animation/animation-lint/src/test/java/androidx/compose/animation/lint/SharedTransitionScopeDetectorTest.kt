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

package androidx.compose.animation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SharedTransitionScopeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SharedTransitionScopeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            SharedTransitionScopeDetector.UnusedSharedTransitionModifierParameter,
            SharedTransitionScopeDetector.ConstantContentStateKeyInItemsCall,
        )

    private val AnimatedContentStub =
        bytecodeStub(
            filename = "AnimatedContent.kt",
            filepath = "androidx/compose/animation",
            checksum = 0x8604a7d6,
            source =
                """
package androidx.compose.animation

import androidx.compose.runtime.Composable

interface AnimatedVisibilityScope

interface AnimatedContentScope: AnimatedVisibilityScope

@Composable
fun <S> AnimatedContent(
    targetState: S,
    contentKey: (targetState: S) -> Any? = { it },
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit
) {}
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg8uCSSsxLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8TEnQEM1NTnPPzSlLzSrxLhMSDMxKLUlNCihLzijNBioKT8wtS
                vUu4eLmY0/LzhdhCUouB6pQYtBgA2lFPg3EAAAA=
                """,
            """
                androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄1.class:
                H4sIAAAAAAAA/6VUXU8TQRQ9s91+UmxB5UtFRYQWkKVVebCESCqEjRUTS5oY
                nqbdAYbuzprutsE3fou/QOIDiSaG+OiPMt5ZqjGIEmOyc+fsuR8zc8/sfv32
                8TOAR1hmWOXK6fjSObRavvfGD4TFlfR4KH1lrUVIOFVfhUKFz8Ppc8x0KQnG
                8KzW9kNXKuug51mSPB3FXavGvabDK7/6druqpSsH1kYflVa26xV6VisME3+u
                koTJMPn3SkkkGBIrUslwlSFWKDYYzIJdbGSRQiaDOAaICPdlwPC09n/Hpt0m
                pOr5bcEwU6gd8B63XK72rJfNA9EKK8XfKYZUgQ5apMFgyJBh+MKglZYbHUHv
                Op2BgetE2lv17bWt6noWo7iSJnKMYehHZ1+IkDs85Lqu14uRskybtDZgYG0N
                YuQ8lBotEXJKDLOnR2bGSMXzp0cTxiabMlOnR3lWTuQN/bqZ+fIuYaZieVOH
                lxkGVrjy1VvP7wbUXlZnWPiXHiZRYBg930hH7PKuS73Yv6CJl9ycS/zlin2B
                LnQZ5jBPvT+3k8U2bcKs+o7Quvgt7jZ4R/KmK7a1YcjVpBJbXa8pOn0mXZd7
                iofdDuGsrZToVF0eBIKuV25dtVw/kGqPtNn3HYZM3e92WmJD6szxV10VSk80
                ZCCp1JpSfhi1LcASaRsn0ei7wrgWW0tHg+4CMYuEpimC0ZyYM0+QPY4ktshm
                z1gMRjlDyCFPkTpjmTwGzcb8e21pDR2nowejzJEzbz9To2FcJX8pwkO09rWf
                q49GscDAJxivTzDyAePHERFHmWymvxQwhodkTRSx0C8Uox+Onh/gMc1PKHKC
                sm7sIGbjpo1bNiZx28Yd3LUxhXs7YAEd9v4O4gFmAswGGA6QC5D/DiMiC4S/
                BAAA
                """,
            """
                androidx/compose/animation/AnimatedContentKt.class:
                H4sIAAAAAAAA/7VUW28TRxT+Zn1br+3YWQIEp0AKSQnEZm2X3nAIpBYoK4yp
                6hBVytNkvZiN7dlodx2Flyp/o6/9B+UJ8VBFfeyPqnpmsybBceXmAVk+9/PN
                NzNn5+9/PvwJ4AEaDCUuOp7rdA4Nyx3su75tcOEMeOC4wtgILbvTcEVgi+B5
                kAJjKOzxA270uegaL3f3bIuiMYb8WDHDq5XmeGW92XODviOMvYOB8XooLLmM
                bzyLrOqUfK1+d5vhw1r74Xnk9ZWt9jT4tTLVrE7onbbuWrn5/4+pbbn7dr18
                hs4r4chFJPnl80DeUATOwDYaoc93+3ad4XbT9brGnh3setwhKlwIN+AntFpu
                0Br2+1SlWdHN2G9VaAw3zmzDoYwneN8wReARhGP5KWQZLltvbKsXYfzEPT6w
                qZDhzqTrOo20JUiX9pDFDPIacigwpKL1VegMmYB7XTtoE02bQT+PxrA47f6n
                ltSo5OrYgS917Nd82KeRe/MZRs48Dykvcv0i383SOONqCkUG1Wy1tzZajacM
                Ty4wYJPw6ll8getpLODGp2MwYcspLGaRQFKDglsMs6MTeGEHvMMDTkesDA5i
                9EIwKdJSgIH1pKFQ8tCRVoWsTpXh3fFRSTs+0pSCoilqTFPmlfAfhkhIHYVO
                U8UhpYpKhd2Kq8dHBaWWLMSKyqZSW1aVQrx4U7+kz24qf/2ezKpJXVVTelxV
                V2J6Uo/Ps4paSdSqhXSxrMd1pZL5j1qZYxWN4oqaDWVuMyV1YUYyr7FwU205
                qtEBnJ3DK6Pgx0+kRYoScUF6QpOczOqFnwm6rBHQ00MK+1Q5Qtx6GxboY433
                ezTp8YbbIRb5piPs1nCwa3tb8uWQvFyL97e550g/CqbbTlfwYOiRvfDzyXtj
                igPHdyi9cfq0MCyNZz9u/5OyHH3lVu8F348WyJpC2F6jz33fprTWdoeeZT9z
                ZO5aBLl9bjlUaQLjcrpIX5MjSd4P5D0nX45YblXPvMdsSb9E8o+w7CHJJF1c
                FnnUyV48KcQcLodAOWi4Qnlp6bhKHWthXwqPok6V9Dr98+GEIz2ShTTmiQUL
                GTwi2ATpueuJX3+D9g43j/Fl895q6T1unxB5TDIGlgsZzYQsMoSfo1+GvCfk
                awS2ELKax0bY9D1+JP0LxZcIfnkHMRNfmbhjYgV3TdzDqokSyjtgPu7D2EHe
                R8JHhY7Kx5wPzUfNx9c+HoRB3cc3Pr71ofr4zkfyXyUizovZBwAA
                """,
            """
                androidx/compose/animation/AnimatedContentScope.class:
                H4sIAAAAAAAA/5VQzU7CQBicr1Uo9Q8QFV/CUsLNEzExaYIxkcRLT0u7mqXt
                LqELwRvP5cFw9qGMXxtPnPCws7Ozmdlv9vvn8wvACDeEQOh0aVS6CRJTLEwp
                WVCFsMroYFwzmT4YbaW208QsZBNEaM/FWgS50O/B82wuE9uESxgeEPWqSjVT
                ubIff2nHhM4kMzZXOniSVqTCinuCU6xdHpEqaFUAAmWsb1R1GjBLQ0Jvt/V8
                p+9Uy3vr77ZDZ0DV3ZAQTv5ZjZ8dHeLZ68C27l7cXWYJ/tSslol8VLkk3L6s
                tFWFrL25HGttbJ1aNnhcHHG9RtWS+VWNPVzzHrLOfwQvhhuhFcGPcIJTpjiL
                cI6LGFSijU4Mp0S3xOUvVJXe6dwBAAA=
                """,
            """
                androidx/compose/animation/AnimatedVisibilityScope.class:
                H4sIAAAAAAAA/5VOTU/CQBB9s1UK9augJPgnXCDcPBETkyYYE0m89LS0q1na
                7hJ2IXDjd3kwnP1Rxm2NP8CZ5M2beZl58/X98Qlggj5hLHS+Nirf8cxUK2Ml
                F1pVwimj+bRhMn9VVi1Uqdx+npmVDEGEeCm2gpdCv/PnxVJmLkRA6M4K40ql
                +ZN0IhdO3BNYtQ28G9XQqQEEKvx8p+pu6Fk+IvSPh3bEBixisWdvg+NhzIZU
                i2PCZPb/N721d+r9qQ9GO6ndXeEI0dxs1pl8VKUk3L5stFOVbNZLOdXauOaw
                bXl3nOA3GK4b7OHG15E/feqzlSJIECZoJ+gg8hRnCc5xkYIsLnGVglnEFt0f
                0vO5tnUBAAA=
                """
        )

    private val SharedTransitionScopeStub =
        bytecodeStub(
            filename = "SharedTransitionScope.kt",
            filepath = "androidx/compose/animation",
            checksum = 0x738eebab,
            source =
                """
package androidx.compose.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SharedTransitionScope(
    content: @Composable SharedTransitionScope.(Modifier) -> Unit
) {
    // Do Nothing
}

// Note, the real version extends LookaheadScope, but not currently relevant for this
// detector.
interface SharedTransitionScope {
    fun Modifier.sharedBounds(
        sharedContentState: SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
    ): Modifier

    fun Modifier.sharedElement(
        state: SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
    ): Modifier

    @Composable
    fun rememberSharedContentState(key: Any): SharedContentState
}

@Composable
fun SharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit
) {}

class SharedContentState internal constructor(val key: Any)
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg8uCSSsxLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8TEnQEM1NTnPPzSlLzSrxLhMSDMxKLUlNCihLzijNBioKT8wtS
                vUu4eLmY0/LzhdhCUouB6pQYtBgA2lFPg3EAAAA=
                """,
            """
                androidx/compose/animation/SharedContentState.class:
                H4sIAAAAAAAA/5VRXU8TQRQ9s223y1qlrSAF/AahFGGB+IYfUYxJY0VDCS88
                TbeTMu12luxOG3jrb/EXaKLR+GAaH/1RxjvtBgV88WXOPTf3nnPn3p+/vn0H
                8AjrDGtcNaNQNk88P+weh7HwuJJdrmWovPoRj0RzJ1RaKF3XXIssGEO+zfvc
                C7hqeW8bbeHrLFIM9mOppH7KMF2uXSzYXjlgWKiFUctrC92IuFQx+ahQj4xi
                bzfUu70g2GZIdcSpA4fhdifUgVReu9/1JE0QKR54VaUj6pV+nIVLVv6R8DtJ
                8zse8a6gQoblf4zwV6ZuRFo0VA45XHVxBdfIuGx4BnkXaRQYipclcnBwfQIW
                phjS+kjGDF7tv/ZHH7RbQr8WpwxT5ZXLFgyFWvLxN0LzJteccla3n6KDMfNM
                mAcMrEP5E2nYBkXNTYZnw0HOtUqWa+WHA9dy0qXhYMvaYC8mneGgaOetOSI/
                3ttWPrVXOGOOOxzMpZ10PmNktpgRL41H3484Ldv8pe6Hx2K9oxnm93pKy66o
                qr6MZSMQz//ckdayEzYFw2RNKrHb6zZEtM+pxqwz9HlwwCNpeJJcvKh1dsNz
                om497EW+eCVNz2zSc3DJHZt0mvRoOUVzKcJlYjZhltAizBCzUCb2ktAizK8W
                J75isvIFxcrqZ0x/HFWuJH02llCh+Ma4lnDGbJ+isf4qxVmWGBRQwmwi75kL
                EWYqnzD94ZwmEs3cuCDRPD/pw9G7hDXCJ5Sdo7r5Q6SquFnFrSpu4w6FuFvF
                Pdw/BIuxgMVD2DFmYjyI4cQmLsWY/Q02LAWq7AMAAA==
                """,
            """
                androidx/compose/animation/SharedTransitionScope.class:
                H4sIAAAAAAAA/51UW08TQRT+Zhd6A3QBlVIQlYuiD25teOMJiYaSAsYaXvo0
                7Q447e4M2ZkSeDH8Ff+GD4Znf5Tx7LZGaJsSTHbm3L5z5tyyv37/+AlgCxsM
                Za6CWMvgwm/p6Ewb4XMlI26lVn79K49F8CXmyshEUW/pM5EFY/Da/Jz7IVen
                /lGzLVo2C5dh2qQO73VXBYbh+2ZtKHhX+gc6kCdSxNvD1sGnd7WyQtm65VaM
                he+knAiOpZFNGUp7mea6/Xp8BgxrNR2f+m1hmzGXylBIpW0a0/iH2h52w5BQ
                M73CPoQionQYSjExUVPEw2ky7G7WBrszKo+xtTJsDHvEXWVlJPzdVObNMMHN
                1jrahlL5B8LygFtOOic6d2m+LLnyyQUG1iH9hUykMnHBO4Zv11erBafoFBzv
                +qpAX8rnnEGaOyleX1WcMtv3PKfklN1KxpsgyvZe7S97k8Rl3tAhbbZvzRHN
                770ka+GGdeq2NcmiwlC5uzUDG0gVUkH+vVvqdsQlw8pdKzFnRgx1gY/eMIat
                /9lLhknTC1wcWeLbDm3Z0ufewKvqPHEPxc6/5WRYH7R+4jGPhBXxLVihrrtx
                S3yUIT222Pc5HoqXoWlgIlkVuBOUHTIkvyApQ63OEl2lM+/2hRs3gXP3AedR
                6IMTOkX0r0OW9aEO1tL7OdaJHpF2mhKaacCt4kEVD6vwMEss5qqYx6MGmMFj
                PGnQ/wcLBkWDvMGiQclQl7FksGzw1GDFIGfw7A9vI0cV/QQAAA==
                """,
            """
                androidx/compose/animation/SharedTransitionScopeKt.class:
                H4sIAAAAAAAA/6VUy1IbRxQ9PRLSaCzZsoiJkLFisGxjsD1CcZ4iTggVl1UW
                JBURNqya0YAbjXqo6RkKb1JUJR+Qbbb5g2TlyiJFZZmPSuX2aCAyUoEfi+m+
                fe/pe+5r+p9///wLwCN8zdDgshv4ontoO35/31euzaXo81D40u4854Hb3Qi4
                VEIrOo6/7z4Ls2AMxT1+wG2Py1372+091yFtiuHa2CsMt+bbPT/0hLT3Dvr2
                TiQdbVT2k0RqNO9tMvx8EWr5QftNw22OuRIJe83vih3hBs0Txh+kCJuP4zBu
                j94IIhmKvmuvxme+7blNSqrtB7v2nhtuB1xQmFxKP+SDkNf9cD3yPEJlHV+G
                rgxNWAzVofwEqQPJPbslw4DuC0dlkacaOs9dp5c4+I4HvO8SkOHufPtszZtD
                mo52sksJ5HEZVywUUGS4eVHZGabOlq3NX/hRyLAy/3qVG+96KS7lT+/m4236
                PaahZj/hMzFFPTg/pAuLtkSQ6vii1bruDo88Kt7GOxavNdprncnMeU6zmGHI
                6RGlGvmS4fwYaqfIZh5VfJDDDdzMYxLvWTAwx3D1JMg1N+RdHnJK3OgfpOjp
                YHrJ6QUMrKcFg4yHQkt1krpLDL8cH9Ws4yPLKBrxVj7dhr+BqlIvHh9VjDpr
                mCbhSUo1ZorpSrmULhn1TLyy+sTfv2UMMxuv5tNs5X4xF9+ZI2R1CJlK8Nar
                eB1Xg+mQSyepDTe18ebDpqcp8fTNIf3liiwnLjdexIARKv3T3Xm9xmTxEUN5
                LPXDHk1ZetXv0ut6pS2kux71t91gQ79NmtR3uLfJA6HPiTLXEbuSh1FA8vXv
                By9aSx4IJci88v/jxVA7az19hl6BFTohd3prfD8hyLekdINVjyvlktnq+FHg
                uE+Etk0nLjdH6LBE45bWo4QUpjGBDJ0+pZPWU7eQXShdeomrv+shw2e0Zkid
                oQfuc5LzAwgslGhvxpgslhOUSfsX2k5TihwJ03q+E/8rBDVoLyyU3if/i2NY
                8qjFLFMDIO3lONAC8U2TfcB3bYSvYCR88VrMoYLrJGvW5STbyZn0j7/C+gOz
                x7jxbGHxJW4NuB/HhWCFOIjL0H/ULDmfI9JZOn1JZ4t8fUy/a5WC+Cq+9All
                A7RJX6MK3t5CqoU7LdxtYR73WljAYgv38WALTOEh7C1cUphUqCtMKZQVLGqD
                QkPBVPhQYULh0X8DbMCoJggAAA==
                """
        )

    private val LazyListStub =
        bytecodeStub(
            filename = "LazyList.kt",
            filepath = "androidx/compose/foundation/lazy",
            checksum = 0xf4f32297,
            source =
                """
package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {}

interface LazyListScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    )

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    )
}

interface LazyItemScope

inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

inline fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index -> contentType(index, items[index]) }
    ) {
        itemContent(it, items[it])
    }
            """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg8uCSSsxLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8TEnQEM1NTnPPzSlLzSrxLhMSDMxKLUlNCihLzijNBioKT8wtS
                vUu49LgUMExKyy/NSwEbpZeTWFUpxOUDJH0yi4EGcfFyMafl5wuxhaSCuEoM
                WgwA3lzNQaEAAAA=
                """,
            """
                androidx/compose/foundation/lazy/LazyItemScope.class:
                H4sIAAAAAAAA/5VOy04CQRCsHpTH4gNUEvwJBog3T15MNlljIomXPQ27gxl2
                d4YwA0FPfJcHw9mPMvbiF9idVFd3JVX9/fP5BeAOA8JI2XztTL6TmatWzmu5
                cBubq2CclaX6eJcJQxx0NcvcSrdAhN5SbRWL9k0+z5c6Cy00CP2kcKE0Vj7p
                oNhA3RNEtW1wEtXQqQEEKvi+M/U2ZpZPCIPDvh2JoYhEj9lieNhPxZhqcUoY
                J/97kWM5pVvfEuPDqAiEaOY260w/mlITbl82NphKvxpv5qV+sNaFo5lvciRO
                8FcC10e8wg3PCXuecjdTNGK0YrRjdBAxRTfGGc5TkMcFLlMIj55H/xfnXlYi
                ZgEAAA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄items＄1.class:
                H4sIAAAAAAAA/8VTbW8TRxB+9uw4zsUkTspbwksDGHAc4OJA0jYO0ChNxIkj
                IGxZqvJpbW/MJue96PbOCnzKb+kvAFUCqZWqiI/9URWz54NGIqIFPvDh5mZn
                Zp+ZfWbm73/++AvAHfzMcJurThjIzr7TDnp7gRbOdhCrDo9koByfv3jueCQ8
                qaOHUUlGoqdL1WEwhundIPKlcnb6PUeqSISK+xTba3X4MDIMF4/4t2PVNoDa
                2Ug1whhiyK1IJaN7DJnybJMhW3ZnmwUMY8RGFjYZomdSMyx6X1BkjeCl6ge7
                gqFU9nZ4n1Oo6jqPWzuiHdVmj5iahE3xp8qNxnH2kheEXWdHRK2QS3oEVyqI
                +OBBm7Hv85YvKMySEcPkx5kYrv1H/tRUwBjGbVg4RWB7VYb8SttPGDKk5I1n
                iozuZr2xurm2XsA5nBgh43mGCS+l+5GIOBHDTT29fobazIwYMQIMbNco1B5r
                XxrtJWkdyjR/eGDb9FlFZlv5bPHwYDp7ls2zy9n84UHRWsgVM9PWA+uB/fa3
                XJb8Q+beAsPoCleBet4LYk1tNAkaDHOf0a5h3GQ4MehZR2zz2CcSX5f/f8fr
                7WBP1AZkxpGkGSRjzfv09H2lf6HmHtNQGl0H88TJ+9Ju7dJbKt5xi1IP4rAt
                fhGtuLu+HwmlCZVaNtTnfkwDu19/tPrEPoJjP0xA7Ep95r22Yc/NVGeOxnzB
                ltgVz65eqd5YXFomfd3s3FrQEWaOgzb3mzyUZrobRjCMe1KJzbjXEmFqGanL
                ruJRHJJ+7mmsItkTrupLLcm9+u+eMBRcpUS45nOtBR3H11XbD7RUXZrYZ0GH
                wR5wsiEN7snjCGKYSlM0P0qAKi1ClgYwR9+U2QyabnOmxSG5RKcSRdCMIlfJ
                vsHoq2QffiBZGFjpb+5MmCVEJrmxRB6L/hZ7aSR+TOJM9KXk5umBN71ptEl8
                R/6fEr2I5TR+AiiuEu7JtJJl8lpJJXOvcfpT2LkP2DmcSbDzOPvhRWeSGGD0
                T1i/vsH077jwKg2ukbTT8kGErCR03CKa7ibpFnCP/t9sznCfst+l+i5iCN9v
                IeNixsUlF5dxxaVOXXVxDde3wDTKmN1CTqOiMadxQ2NSY0xTjwy7YwRxm747
                SejiO+4q0wHaBgAA
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄items＄2.class:
                H4sIAAAAAAAA/8VUX1MbVRT/3c1flhUCQi2gNRaKIaFNk0KrTYqlFGQl0EqY
                vPC0SW7TS5K7THY3E3zqZ9EvUGfaOvXBYXz0Qzmeuwk0QjRaH3zYs+ece+7v
                /L+//f7zLwBWUGS4Y8lqyxbVTrpiN49th6ef2Z6sWq6wZbphfXeSLhApCMfd
                cReEy5vOQjYCxlAp1G23IWT6qN1MC+nylrQaZNwsV61c/9kzT1YUmpPe6nGZ
                fOHIalsEL2tpk67WeCvXp3pSPuIVN7eWY5j9aycRBBmu/b2jCMIM4byQwl2j
                XBND4uoF4bmCnFDKuaUSw/6wW/mbBwe51KD4L8DlU2S45oMGF+r8hCE+LCAD
                ozBGoOEDyqNbf4bYxTANjCOmrCYIOWEulQxE8aGOEKZI4T4XdGm18B6dpg6E
                hWzbdc4wTcCXk2SYL9itWvqIu+WWJSh4S0rbtbqJ7NnuntdokNXYn0OOYI4h
                UOOugU8wM4qPcY1hMXEZf4BLA1cRH0UEnzGEhKzyDgMzqSzvLPe8Zpm3IrjB
                EKWxKVkNjzIIJJZMA58joWMRSwpmRqeipRi04wzD5KDs7g1rPrV08OhOX+y+
                33yGiTPEXe5aVH6LdFqzHaCNZIqMKAJKqa6YAB12hOJeElelMFdOX8T00xe6
                FlMkqhETJZmkWe02ux6MEq9lw7EAicFt/dfvw1o0pO5mGUbzlrTlSdP2HNoH
                5eSAIfUvBiOCh6ro3Tn8IfHPZ6pYsY/5pf0auo//6TybU5vwCBuU+Fkct+ou
                Q3Lg21W0vVaFP+Zlr7bZcbl0CIJ6E2p3p6dT3F1/qvfh6Ds+iJ4sxs+4LT0V
                z8T7bd5j6fRkQc/MZ5ZX797P6MlNnWGkKGrScr0WhRHcsKtcDatdsRolqyWs
                coMfKMIwXhCSd2e/p5nb96QrmtyUbeEIUq2/204Gw5SStzYaluNwEsc3ZaVh
                O0LWaDSf21UGvVuTLaGwpgYViGGm56J0yQEytF0hmrIItFhMvWTEmzTKGtIk
                05t1Lt9GgPgo1CP2DWke0V8NfSyZeo2x5PJrTCaDP2H6R381dojGu+e4go/U
                vuAMX3EKWesh65hQi97D3aZbGv2nkq8I9hUm597iUw1vcV3DS9++QDTs49zw
                /Vzp2p/7mcI8Fuh8t2c3Sw7X1YtEvPLwkOzVDSOZOsXiGyTfYPki8nwfsnGO
                bOAmbvnV2iNJJ07z9TN4QjSIdTzu5RTAU/+/iW/p/78NJvbJ+wOKM0Ndzh4i
                YOKOiRUTq7hr4h6+MPEl7h+COcghf4iwgwcO1hx85WDeZ676ZRwjiC36vvZN
                t/8AKXvrop8IAAA=
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄items＄3.class:
                H4sIAAAAAAAA/8VUS1MbRxD+ZvVk2YAg4PBIHMUoREjYQmBwYsnEGEPYILCD
                VLpwWklreUCapfahEjn5tyR/wKmynXIOKSrH/KiUe1YCKyBbiXPIYXu7Z3q+
                fszX8+dfv/0O4DaKDCuGqNkWr7UzVat5Yjlm5onliZrhcktkGsaPp5kCiQJ3
                3F03wV2z6SRWImAM1cKx5Ta4yBy1mhkuXNMWRoOcm5Wakevde+KJqkRzMttd
                LZsvHBktg+BFPaPT0bpp53qWHlWOzKqbW88xzLw7SARBhuvvDxRBmCGc54K7
                61RrckBe3SQ8l1MQKjm3UGY4GHQqf7NUyqX75X8JLp8mx3UfVEtULSpHuKXT
                E5MhPigxDcPQhqDgI6qncw8MscvpahhFTHqNMQST+kJZQxQfqwhhghbcp5wO
                rRY+4MbpJsJctKxjynWSgK8Wy5AoWHY9c2S6FdvglLwhhOUanUL2vUbDqDRM
                chv5e84RzDIE6qar4TNMD+NTXGeYT14N0CemhinEhxHBFwwhLmpmm4Hp1Je3
                nvtes2LaEXzJECX+lI2GRyUEkgu6hq+QVDGPBQkzrVLX0gzKSZZhvF95dwax
                gO62P4cnL9PAZwHD2Dninuka1H+D1pRmK0CjyaQYkgJU0rFUArTZ5lJ7TlqN
                0lw7exZTz56pSkyKqEJKlGyyZoJTbIndCEbJUpbDscCMshTcUf/4KaxEQ/L0
                MsNw3hCWOG1ankOjIcOUGNL/ghsR3Jdt71Dx5+Q/p1Wxap2YV0Zt4Gj+p/3l
                nByGB9ikws/zuHXsMqT6PmNFy7Or5kOz4tW32jSmDkHQ7YRaHf60i3sbj9Ue
                HHXXB1FTxfi5tq2m49l4r88HzJ2aKqjZuezi6trdrJraUhmGirwuDNezKY3g
                plUzJV2tqtEoGzaXI1aSgmG0wIXZYX93ZfbAEy5vmrpocYfT0sbbAaUHSRfC
                tDcbhuOYZI5uiWrDcrioEzmfWjUGtdOTbS6xJvo1iGG6G6J8JQCyNF8hYlkE
                SiwmHzPSdSKzggzZ9Gxd2EsIkB6FfMe+p5UH9Je0j6XSLzGSWnyJ8VTwV0z+
                4g/HLsl4Zx/X8ImcGJzjS00iK11kFWNy1Lu4O3RKof9E6gXBvsD47Gt8ruA1
                bih47vsXSIZ9nHk/zrWO/0WcCcwhQft7Xb8ZCrgh3yTSZYT75C9PaKn0GeZf
                IfUKi5eR53qQtQtkDTdxy+/WPlkqaYq/Po1HJIPYwMNuTQE89v9b+IH+/xsx
                cUDR71GeWbrl5UMEdKzouK1jFWs67uBrHd/g7iGYgxzyhwg7uOdg3cG3DuZ8
                Zcpv4whBbNP3ne+68wbQeeTnqggAAA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄items＄4.class:
                H4sIAAAAAAAA/8VVbVMbVRR+7uZlwyaF8Ka8xBpLtCEBQlKotUlRSsGuhJQ2
                KVXR6iZZwkKyy+xuMtQZZ/gF/gj9A61jq/WDw/jRH+V47iaE8CIv6owf9t6z
                9577POece865f/z5628AZlBhuKHoZdPQyruJklHbMSw1sWHU9bJia4aeqCrf
                PEtkachqlr1sRzRbrVmRGRGM4bvstmFXNT2x1aglNN1WTV2pknKtWFbSnXsb
                db3E0azEUktKZbIXYpWJLl8ydtR0dktpKLShVxIyUVVUs03xWNfs9FyaYeTv
                7RHhZrh6tk0ivAzejEZwcxSW6DkutEyq2xqRUHTS42sM3553KjN5Wc8nC4Xj
                rh5jzsRJZc7hDzg3tGCQ97rNED7PhwD8CHRBwBVyvXm7DMHjngXQgyDX6mVw
                R+XxtQB86JfgwQAt2JsaHZq9mF9H84guzavpDWNbZZiJXjYyMvd4LGuYlcSW
                ahdNRSPfFF03bKXpZ86wc/VqlVj8EW5lk9aH0aO50M4VyiyTMLSSJeIthsHS
                plraboGsKqZSU0mR4Xq0IxsfFLfUkt2Zn3kOUknzKL2NsISreIeh+2hIRYwx
                uCoq3dEgBfQkXgDv4j0/IrjOcOtsvtbKqSBDGPdDRIxh+rLRZRA0Mo/JDFOX
                OyoiQUl0aE2uXiuqpogkg49ivaZU63Tfrui4HMANzEhIYZabOixRir1Pl9WR
                7SI+oFNyLl+Yzy0sMlw5UgoBpJHpwm3cIWt3CL/vZBD4Toqh+F93pJNlSXd5
                vDKdwmToPdBcUW2FIBVuVa3hoh7M+NDFB1Cwt7ngos1djUvPSSqTW3f29wak
                /T1JCArONNScgj6agvt7I8I0u+b2kSzEhGlXyht005LnvvT7917B5+UgFAF/
                RtEN/VnNqFvU3zhbgSF+iaoVkWfwtJrEDxcsV360s4UfdpWzLyT5L/dTTgE+
                BrUI/4EdU9uU0LFTn628UTdL6j21WK8s7lLztAiCLsnTaCbrbn5lflXqwJGW
                HRAplg8fSEtSPJwMd+r8g44oxbJSciw5MXvzdlKKLUoMXXmtoit23SQz3AtG
                WeVZbpSU6ppiakqxqhb4wNCT1XS1WWqtlcijum5rNVXWG5ql0VK7h80fNkl6
                M2RdV82FqmJZKv32LOqlqmFRC6Nk3TTKDFIzOEsaBx04LVIMwy2utSbTEYLR
                43Z07CJJNe+hZBQhBIP8NSL5C0p9AXfpn96d9v8CXCT7wB+iL2nlLs28SIKx
                +Et0xyZeoi/m+RmDL5xSekpjuLmPN/Amry8c4HOJIwstZAm9vP20cJ/C7ZwL
                xftCv+Ba7Cd0x2noC71GVMBrxF148sI59xWNvGd5EcUoxjs4Q23OEO1MOJwh
                TGKKtL/mBS/Q0ohj0zxv0vTHqR/SYX68Pxbfx/TEPlKvcPMVbv2IuedtRq+D
                NtbB1t9m68eH+MjZ78c8SQLFVXGspCbgaAyjSKMbBTxpee9CyZk/RZnm/y3T
                oRI7t/0e5cPiOlwylmR8LOM+ZBmfYFlGFivrYBZyeLAO0cKqhYcWHlmYsDDp
                yEPYIJRuQvmMvs8d7fW/AOmNErvuCgAA
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄itemsIndexed＄1.class:
                H4sIAAAAAAAA/8VUW28bRRT+Zn3NxkmcEJrGhWJaFxwn1HF6ocQhrXEdsoob
                ELYsoTyN7Yk7yXrW8u5aKS/kt/ALWiGlEkjI4pEfhTizdolRTQXlAUt75sy5
                fGfmzPn82+8//QLgLvYZtrlq9x3ZPsu3nG7PcUX+2PFVm3vSUXmbf/csXyVR
                la534GWkJ7qupdriTLQzhRgYQ+rU8Wyp8ieDbl4qT/QVtyml22zzGEIM1yf8
                x75qaVw3vzfWtmKIMER3pJLeLkMou9ZgCGettUYCMcyYCMMkg/dUugw71bc/
                a5GqSDVwTgXDraxVPeEDTimqk/+qeSJaXnFtwtSgGpRwJWvV69McmarT7+RP
                hNfsc0m34Uo5Hh/d7NC3bd60BYU9yL5e5s2FxyaG5KXt0O82RT+Gdxji1OAG
                t30RdMpK4F1cMbGMlQTmsWDCwCqD0SswLE0DNXpbhLHTsoNu6wbHdc4HZLQO
                a/XSYbmSwIeYmyHjDYbF6vjpngiPU5O5hugOQjQ5TIsZLcDATrVCT22cSa09
                J61NZ3g0PF8yh+emkWSmETdojdMaSg7PU+GrbJPdCMeH50ljK5oMpYzNMK2R
                lLFv7Ju//hANx6PJmMahE8/ucOWoZ13Hd2lEdME6w8qlMd3jfd4VNHrpzd2/
                8xTIs/4vxieGewzLf5mhtjjmvu0xXGT/+SDWWk5PjF/d9yRRg4zF6ptJ8R/9
                d4rT5psY9SkeUDtfHe32Kd0lV53G35rj91visWj6ncqZJ5RLqPT6kcFo+L6v
                PSl9bU7gmAcBiJmrpV9pe+Z6upCejHl78pq5qlm4Wdi4d3+b9Ir+Ryg7baHH
                3Glxu8H7UlOurgXDQlUqMWLN2DJTkx3FPb9P+rVvfOXJrrDUQLqS3KVL8jIk
                LKVEv2xz1xW0Xaiolu24UnWIA0+dNoM5as2e1LjL0/rEsDou0XitAApErTCN
                cJS+Vc014oveExVJ7tIuQxE05YjmIi8x+yJg2EOSiZGVVp2zqAmPUJCxQR7N
                O4M91xKPgjgdfXsi0xhnlgI9iS/GUYtAskRoS+P6B4Sl0eZy60MsX+DqxgVS
                04HTo8AxsNau4b3AP4f3STPoUtf/vNgK7fVv9mcY375E+kfcfBEYoiiTNCls
                FLCKx0FX7uMzVILC29ij9f+eOnxJhyjTMTOI4NYRQhY+svCxhSzWLOSwbtFT
                fHIE5lJ/8keIu9ikF3ex5eJOoNx1Me/Su+nezxNQkb6dIOHzPwDW9W/0kwcA
                AA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄itemsIndexed＄2.class:
                H4sIAAAAAAAA/8VUXVcbRRh+ZhOSsKwQsCAfWmPBGhJoGqC1khRFCnZLgNpg
                /MCbTTKkA8ksJ7ubA97Y36J/oJ5ja+uFh+OlP8rjO5uEpiQWrRde7Ow778w8
                79fzvn/8+etvAJbwLcOyJct1W5SPUyW7dmQ7PLVve7JsucKWqar13UkqR0tO
                OO6mOyNcXnNMWebHvDyzEAZjKOUObbcqZOqgUUsJ6fK6tKr0plYsW5nOs31P
                lhSok9poSels7sBqWGRFVlImPa3weqZDtVM84CU3s5JhmPx7I2EEGS6/2lAY
                IYZQVkjhrjAsxl/t10LLCc8VZIQiz8wWGPYvepWd7xHO/O5uJtkrpnMmskm6
                uOIbCs4c8hOG2EVOGhiA0Q8Nb1BszdIwRM+7bmAIUXVrmJDj5mzBQARv6ujD
                JVK4DwU9yuZenwRUnJCQDfuQM4wSfnesDNM5u15JHXC3WLcExWBJabtWM55t
                2932qlW6NdyVvTCmGMINq+rxnX2GsZfh2yk28A4u63gb7zIMvhx9GO8xBCrc
                NTCNiQFcwcxFXCFWX2W4Fe+Oo1vTI1gD44gP4APMMvQJlSQGZjJc7QHYM1fR
                F7ptr1ZUSbjGECHSF1QeKJz4rGngOtI6UlhQ9iZ0Ku8Sg3aUZhjpBXrvIur2
                SCvxsXcvjp6nrs9cql/byBZ3LeKORTqt1gjQpGFq6VcLKB2HSgjQ4bFQ0mOS
                yuT50umjqH76SNeiaoloJERoT7tJ7Tq7EoyQrC2EogHaBu/qv/8Q0iJ96u0C
                w0DWkrY8qdmeQw2ujOwyJP8Fq8OgIhmd1Gb4Mf7P+yJfso9419y4cM78p/PF
                jOrmTeQo/rYf1w5dhkTPmZy3vXqJ3+FFr7J+7HLpEASVqK/R5NX3+a3V+3oH
                jr7pg+iJfKwtbejJWDrWeef1B4eeyOnp6fTcjZvLaT2xrjP050VFWq5XJ2+C
                a3aZKzbbJatasOrCKlb5rloYhnJC8mZztDRTDzzpiho3ZUM4glSrLyYMldWU
                ktfXqpbjcNoOrctS1XaErBBRH9pUaL2Zmg2hsC71yhPDRMtEocsA0tR+fcS5
                MLRoVA1lkh8QsTXcoj2N37P9RwiQHIGax3nSfEp/1QLRRPIJBhNzTzCSCP6C
                0Z/8RtmlNdY8xxjeUt2DNr6SFLLWQtYxrCZBCzeHIEnAeOJnDE49Q4z+I1PP
                8b6G50gE8Nh/8wWtIR9r0bc11nxzZmscSczReaF1b5KMrpJ2nmRl5RO6r14Y
                ieQpUk+x+BQ3ziOnO5CNM2QDN/Ghn7EvaaeTpPn6CXxFaxD3sNWKK4Cv/f82
                vqH//81R7JETt8ndZSp4Zg8BE1kTt02s4GOT8rFqUknX9sAc3MH6HkIONhx8
                5uCug6QvjPvZHCSIHfru+1c//wuxpKVmkAkAAA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄itemsIndexed＄3.class:
                H4sIAAAAAAAA/8VUW1cbVRT+ziQkYRghYEEu2sYWa0igQ4BiJSmKFOyUALWJ
                8YIvk2RIDyRnWJmZLPDF/hb9A3UtW1sfXKw++qNc7jNJaEpi0frgw+zZZ5+z
                v33ff/z52+8AlvAdw4opynWbl4/1kl07sh1L37c9UTZdbgu9an5/omeJZLnj
                brnT3LVqjiHK1rFVnl4MgzGUsoe2W+VCP2jUdC5cqy7MKunUimUz3Xm374mS
                BHX0zRaXymQPzIZJVkRFN0i1YtXTHaLd4oFVctOraYbJvzcSRpDh8usNhRFi
                CGW44O4qw2L89X4ttJzwXE5GKPL0TIFh/yKtzFyPcOby+XSyV0znTGSS9HDV
                N6RNl2zSFm7+5MhiiF3krIYBaP1Q8BbF2CwRQ/R8CBqGEJWvhhmCcWOmoCGC
                t1X04RIJ3IeclDLZN28GKlKIi4Z9SC6PEn53zAzTWbte0Q8st1g3OcVgCmG7
                ZjOeHa9aNYtVi54Nd6UxjCmGcMOsetbuPsPYq/jtXGt4D5dVvIsrDIOvhh/G
                +wyBiuVquIaJAVzF9EVNsxDGdYZb8e5AuiU9otUwjvgAPsQMQx+XWWJgBsP1
                HoA9kxV9KdvxakWZhBsMEer+gswDhROfMTTMI6VCx4K0N6FSfZcYlKMUw0gv
                0HsX9XCPtFJj9h7K0fM97Lcw1a9tZNtyTWoek2RKrRGglcMk6ZcElI5DyQTo
                8phL7jFxZfJ8+fRRVD19pCpRSSIKMRE602kyOM7m2dVghE7KQigamFTmg3fV
                Fz+GlEif1F5gGMiYwhYnNdtzaNalmTxD8l80dhhUJq2zuxl+iv/z0ciV7COr
                a4VcuHL+0/1iWg70FrIUf9uPG4cuQ6Lnes7ZXr1k3bGKXmXjmFaNQxBUpL5G
                s7N+yG2v3Vc7cNQtH0RN5GJtblNNxlKxzjdvvjvURFZNXUvN3lxeSamJDZWh
                P8crwnS9OnkTXLfLluxnu2RWC2adyzWRl4RhKMuF1RyPlmTqgSdcXrMM0eAO
                J9HayyVDZTWEsOrrVdNxLDoObYhS1Xa4qFCrPrSp0GozNZtcYl3qlSeGiZaJ
                QpcBpGgA+6jnwlCiUbmXiX9Ara3gFp1pA5+dP0aA+AjkSs6R5DP6yyGIJpJP
                MJiYfYKRRPBXjP7sj0qeaKx5jzG8I+cHbXzJSWSlhaxiWO6CFm4WQeKA8cQv
                GJx6hhj9R6ae4wMFz5EI4LGv8yXRkI+15Nsaa+qc2RpHErN0X2i9mySjaySd
                I15a+ZTeSw0tkTyF/hSLT3HzPHKqA1k7Q9awjI/8jH1FJ5U4xZdP4GuiQdzD
                diuuAL7x/zv4lv7/d49ij5y4Te6uUMHTewgYyBi4bWAVnxiUjzWDSrq+B+bg
                Djb2EHKw6eBzB3cdJH1m3M/mIEHs0nfff/rFXzqdVK6bCQAA
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt＄itemsIndexed＄4.class:
                H4sIAAAAAAAA/8VWa1MaVxh+zoIsrkTxlnpJUxptg6AiXpJUjI0x2mxEtIGQ
                tva2wBFXYNdhF8b0S/MLOtO/0P6BpNOkTT90nH7sj+r0PQsiGuKl7Uw/cM67
                5/I+7/U5/PnXb78DmMUew7xm5MqmntuPZM3SnmnxyLZZMXKarZtGpKh98yQS
                pyGuW/aaParbvGSpRo7v89zorAzG8F28YNpF3YjsVksR3bB52dCKdKeUyWmx
                5r3tipEVSq3Ial2aXoifC1wl1GTW3OOx+K5W1WjDyEdUgsrzcgPikaHbscUY
                w9Cb7ZHhZrh6uk0yPAyeBZ3ULTLMBE93YaZuUsXWCYSCFBtLM3x/1q2FiYt6
                PtHC9YlU6qT7J6xZCNORRccmn5O8ZZPuGjZD4Cy/fOiArx0SLlE4aoln8J/0
                1ocu+MWpbgZ3UB1L++BFr4I29NGCvaPTpXNmuWWJUT49ulE1C5xhNnjRoKnC
                8ZG4Wc5HdrmdKWs6uagZhmlrNXcTpp2oFIuE0jEqjK2hezF8vEwaZUSRL5MO
                PWvJeJuhP7vDs4W6kk2trJU4HWS4HmzK1kZml2ft5tJNCiX5mAjWOwgouIp3
                Gbpfy6+MEQa5qhUrfGOb4TKFt0UR+PAe3lcwiusMncezI2OMwZXnlO7+45fr
                NvkQxngHQpg4qy1mZEQYHpzu1xtXWkIPINqBKUwzTF00rwySTk4xleHWuUxq
                YQDD5MVQZdyiBjjSk6iUMiJJ8wxeKpC0yBOFOzim+rCA2wpiWBReDirUHneo
                wpo6VcZduqUmkqmlxPIKw6VjbezDPay0Yxmr5OhelKGnlfnSHoWuchbR/AcM
                +zrLUD2dJBqHZ6iKD0+uc1sjFE0YWqq66LVhYmgXAyh1BSG4aHNfF9IzknLk
                6e2Dp33KwVNF8kvONFCb/F6a/AdPh6Qpds3tJVkKSVOuaY/fTUtt95U/fvBI
                Xo9QQkHpWNAM03hSMisWUbhASzGEL0BCMj4nwmxmIoYfz8k+QkNzKI+48ozH
                8F/uzzh88iW+Iv8P7ZgsUJeEWj7QSbNSzvJ7PFPJr+zTk2CRCspVW7VWxt8m
                15c2lSY9ypqjRAklA4fSqhIORAPNZ/45zyuhuBIdiY7P3ZiPKqEVhaE9qecN
                za6UyRr3spnjog3MrFZMa2VdyxR5SgwMXXHd4LVerK+MPqwYtl7iqlHVLZ2W
                Gsy8dET9lF/VMHh5uahZFqfPrhUjWzQtImYq3R2TMq7UYrSqC6V9rQLGMFjH
                SteQjgEMn7SjaRdRIoU2Kk0Zkt8vnlqSOTWChDX6pke18R2Hi2QvxCu7TSt3
                aRYt4w+FX6AzNP4CPaG2X9D/3GmsPI2B2j4u4y3RbTjULyShWaprVtAt+Kmu
                N0P2iHuBcM+VX3Et9DM6wyQESei58gqTEl5hxo3Hz527OzQKYvPgBj1iN5tw
                Aw3cAIbpPyZzpDk6J0EXFCDR0pBj1xLB36QvAf8xXRbXe0PhA3wwfoDYS3z4
                Eks/4aNnDUSPoy3ahNbbQOvFfajOfi8ekCRRbHcdK4kWnBODKNDoxhf4uh4B
                F4rOrKFE8/9d9DDIiDtk7jqlIrEFl4oNFZsqxeahiiRSKh4hvQVm4TE+2YJs
                4VMLn1nYsjBrYc6RB2CSlk7SkqFf1jmd+xtKpHKC8QsAAA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListKt.class:
                H4sIAAAAAAAA/81Y61cbxxX/jR7sahG2WIxtCMY0lsNDxpJW2E4RpsXELopB
                SQOmce3WXaQFL0grol1R7Lapk7bp+/06+dqe03xOP7ROfU4Ph5zTD/2j6t4Z
                rRYBAvGo23K0c+dx9869v/nNnR3++a+//R3ACH7PENOtfLlk5tfjuVJxtWQb
                8cVSxcrrjlmy4gX98aP4NBXTpu3cdiQwhsiyvqbTiLUUf2Nh2chRr59B4VqT
                pUKlaDFMDEzvslox4zOlvLloGuX09ErJKZhWfHmtGF+sWDk+lx2/5daS6cF5
                hveOZ2NsePfrewU2myutGp7BO5bppMeFDxd32yhXLMcsGvFJ0dYXCkaa4cJ0
                qbwUXzachbJukhe6ZZUcvepRtuRkK4UCaclF13cZCkNvnf+m5RhlSy/EM5ZT
                JgNmzpYQZujMPTRyK66FN/WyXjRIkaF/YHrnIqTrema5kSWKIIwTOKmgDREG
                KVeiWSxHhkqT748tQ1+zJWJQt1Y8mjcW9UrBYZg75spndgfGF6JnP6MSzjKE
                +ILoFhlh2N+HqKeZDqMbL4XQhZ4wgmhR4EMvQ9B0jKLN8McGdppQSDhfccxC
                nHc2jfZ44xqHhl0ZmxvdDdr4cX0fi83N0S5oustIK9Zg+hf1onbQfZ2hNawG
                xmdqsLej++1Z2m7u1m4Zcx6a9riMqIw+hlZ3F809WjVk9FMHJ8tkbWsNMpzY
                jqOEGEPANh8bDP6BwUwYw7jcikuIM6QOkXqjgpRRTUKSO2VSIONkYaAphXZQ
                kieFFEYUaLiyPQk1eFvCtSN5mZLwWQWjfIYjvD0iYax5aNoeoY0ruI7PNQuN
                cJxguHy4PSJhkiE/kHnx2zqMPtxsxWu4RSdr1IwuRt2cxDJEuSinZK0ncdiN
                Tif4TuSImivGo6ZZXyPFtuoaeQn/L//3SXKPI+UIxKT98EU6xjPZ2bmJ7ORN
                hisHj33LCJ06s5gL4S3cYThdt5YPKtqIh2tYdGWsvLFu5F/8WaQdczwlzqJ3
                /odnET8ZtqamDyljib43jnHM/MeMpY5wZu0xe4NzbPTQFHRJxc+Sewru82R5
                dCOU6r+q4MHxjFDG15tn/NQeGT+nYAG0R9q3EqW3b9S6/eV1NklzqXSz04OC
                NhlObYvhv5YRj79V98iIR18/SoxFhrHDZ0PPgEiKpRAsrDKc271o23Jjey3A
                GcPRybxOC+YrrvnpSst4EeIF6LBc4RUfDa6bvJagWj7JfOmNJ9PKxhPFF/EJ
                cdYT3iPTE+nzukWTN2TfNj1hRHbVumOk0u1LME2WyTTV/NqZSKC7Qw2ovkSL
                KFkiuPmHFp8sTUndmx+xiMz1Xw7IG08ioSFS0sLUJXPVqVBC0S7KSqS1+7za
                obZPhejFsBxWZblNDcjywAlh8ORZMin0Invq0dSkdZL0UnIg0t49LN7s2Mcq
                S6jCzVOus5sfyJ8+ZRTdp39ikc79fR4VPo9w64nTO6yfaRKJSm+3UCSHe7sW
                n6pNifhubMV3KA9Y4mzDqDff90lKUN78UEswTiGNcXapNRbWX4p7a5031+lG
                YFNvbZTfGEiBzdGhX1PyrvRZEjQYsEiS4MxvMAH//jrg597WUcLvs3wPNbDH
                E90rB7sqS/gRfXjWtu/lFdqFgclSnpw9OW1aRrZSXDDKc/zOxOcp5fTCvF42
                edvtDM2aS5buVMpUf+mt6j9RMtaaaZs0PLF196KL2c5RD6Vtam2zjp5bmdFX
                3QnCGcsyypMF3bYNGlZmS5Vyzrhl8rEu1+T8rumQpDt/gKcLtKCL/xOAWj+l
                1gT1+0i2DamtT9EeUzuo/JinE/xMKNOMaMXPqX66qggFp4ShNqjopPFfCG0J
                v3T1ZZK/4uM+aoREhqIyEiIDZ6jOZx1zvenoCbz7IZQ/49wGum4PxZ7ifHXu
                X1PpB2sTTpwAz2oyGQ/RpDL534fPUB9ZYq2iH5gfUi8K/1+hclgdoLIroA6R
                8F8NDsU+4Unx0j+gPoN291Lsr7i6gVc/QoA9w+jdYWqnqf0M1+92Bajx+Q3c
                +ARfCOJj4QZ3Jg3fc1xFi4TXJFyS8Kr43aBtQj3sOQ9915CfD1FDBNFLCPXg
                As4hSuVF9FN5leQ1kncwKBAep1B66D45hQwtEQWF13FbYD1PIU8TeLw2Q6j7
                RK2fan5RG0SWAK2uxMsC/fPBevSpvFBXr67HG3izuh5snFCUaKTYGQzSggTZ
                TGdQqq7MPAGTfcHgTiIQanuOK5B2YihRVB7CDUe3gawRWatQ91GMvYgQT86Q
                TFH0IwT8KAE+TvUZklmShTrgU3XAF/ElF/iiB3zRA77oAV90ge/C27h7ZEbe
                F6B9xQPtgQDtawK0hSpoRnNG3hC/5UaM9IYaMDJJjNQIihQFlSR7KWJkkhh5
                bRsjFz1GLjVkZNZjZNZj5EOPkV8+KCNXaEWaMvIdwuLgjDwauLsZWcOwMSPr
                RxswMkaxDFOMMWLkZWJkjGBOEiM1gjZFICcJwhRBl6T4r21j5KLHyHJDRmY9
                RmY9RnLg/fgNtV4Hv3/zvy6aqypHXXndlZYr77vygSsXXPljerrp+a1Yyp/g
                dyTXaH6b/HLuwZ9BJYO1DL6O9Qwe4XEG38A374HZ+BbevYcu0rPxbRuKjVM2
                VBtPbNy28Z6NPhvvi6EZG9+x8V0b3xPNfhuDNj6w8X0bso23RecPbPzw39km
                pwbQGQAA
                """,
            """
                androidx/compose/foundation/lazy/LazyListScope＄DefaultImpls.class:
                H4sIAAAAAAAA/7VV3U4TQRT+pj9sW1oKSIGKwqII5XcBlRhrTAyC2aQUY5HE
                mJgs7QBD29mmO4vohQ/gG/gEXuuNQRP1ygsfynimtELwL424F3POnDnzfWfP
                OXv267f3HwFcwxJD1pGluitKB1bRrdZcj1vbri9LjhKutCrO82dWjpac8FSh
                6Nb42F2+7fgVZVdrFc8AY+jec/Yd8pQ71vrWHi8qA0GGuFC8OlY6cmZ4mcm1
                x5PNnYb9laXsqoqQ1t5+1dr2ZVFjedZqU1vI2j9fmdxkmDi2PpSeX6u5dcVL
                6zVeb0SzclDkNa0YMBjWCz4dmEWnUvHMp0Ltms2XMp36jl/lUnmmdJX5A8cU
                0lS7wjMVOXA1Y7Yiu2nqnEQQZei4JaRQtxlSmRMhFlRdyB0KMY5OxGOIIMEw
                117eDCQZQpqI4V7mLJKow+lBbye6cY4hoaG948K+bruw9t8I/+188XdVv3/G
                hfQiGGBYavPzOUrfgoE0Q8TOFzbu5JdXGG60mcUWTjaOIVyI4jwuMgz/OXEG
                RhjCjYsMpcx/r4NunFFc0o1zmaGn5b7GlUPv5WQZAtX9IM0hppeoXsDAylqh
                CRI4EFqbp1lycuYwdLYyMVemBgwtuyVOfVlQTrG85tQ2nK0K7ZM5IXner27x
                etMSt6Xk9eWK43mcUGIF168X+arQZ+kHvlSiyjeFJ8j5jqRGaKTewwICCOnQ
                EOiOIowOinGedk/I2kFyOh3+gsQHRB71xt6h61MqFHrxCmGWS4XCDWVtanpm
                9hB9IbzRIAQIggkmIxEsktpPG6CLZBKDJEcoXaPoIq4U2Y64OPkYJBfTHS2u
                wQZXuEmRChukxN5i+DPM/NTQzGw6dIix8GnKxAnKPpL9RNlHlAMYJxnEVbL3
                0oUABTGHNLWVfgbpT6FhLFwnOUxBXSHf8ccI2piwkbExiSkb05ixMfsd3CPV
                CVwGAAA=
                """,
            """
                androidx/compose/foundation/lazy/LazyListScope＄items＄1.class:
                H4sIAAAAAAAA/6VTW08TQRT+Zru0paxQQO7erVpQWEDES/GCiHGTiomQJoan
                aTvgwO4M6e426BO/xRdfNSaYaGKIj/4o45ltVRKMCfqwZ07P+c53zpz5+u37
                py8A5rDEMM9VvaFlfdet6WBHh8Ld0LGq80hq5fr89Su3TKYsw2i1pndEQUYi
                CAszGTCG0W0d+VK5W83AlSoSDcV9ggfVOs8gxXDmUH4jVjXDGbqP2x5xdDCk
                F6SS0T2GVHG8wmAXvfGKgww6c7CRo0D0UoYMt8r/NmeJOkjV1NuCoZ+4y1u8
                yQmvNt0KsVG6UNaNTXdLRNUGlzQeV0pHvDXqSuz7vOoLglkyYmAew+XiIY5n
                1S1Ri0rjR0MM+d+xlTioikYGJxmytKgK92OR3NhzMIihHAYw7KAbPTlYGKVm
                OzMMfX8izS7U/GRhZkdZAz9LQW9ldW1xZWnZwXmc6KTgBYbecnv7T0XEaU/c
                XCJopujhmTGdxoDutG0cei1rVxpvmrw6tZ882OvOHezlrDxrHVk68gd7o/Yw
                m2az6bw1ak2nnuS+vklbWdsUzTJ0LXCl1atAxyE9KbGXjvlsj8QGj/3IC3b8
                MANiPNF6yHorzvC2eEwllLzy32VY+r/8LDU4KgjS8BxuMEwdb9gMbjI4h5dA
                O/2JmNqm+9tLui6MOHSN+xXekEafa8Yw9JSlEi2ttSNjz2MVyUB4qilDSaHF
                3+qmRp5SorHk8zAU9LNnWdV8HUq1SZJ5qesMuVUdN2risTRcI22uyhEmTJPk
                bHrtNH0jRoOkI5s+kqjRAHkFQpAgkJ6wP6LrfaK8BbJOK0qnqek1/wFCmop5
                ylh0Wuydsbib4Ax6KKkcbGXblcbrRR/l77VxeSC/SHz97QkeEMJUOBNXDzCw
                j5F9jP2N2fnF7OAUTlM+izO/7jOUYICuz7BefMS5D7j4PgnYuE+2j9LXcQu3
                aR0t4DANYLLX6WKLSds7eEjnHSIsoAOX1pHycNnDFQ9FjHuYwFUP1zC5DhZi
                Cu46OmjRIWZC9Ca2+wcsMXo8xwUAAA==
                """,
            """
                androidx/compose/foundation/lazy/LazyListScope.class:
                H4sIAAAAAAAA/61UT1PTQBR/m4Q2Df/SIFgqAkrBIkJCx/FShxkHRcNUdES5
                cFrabSdtuulkU6Z44ubZr+DH8OAwHP1Qji/bFlBAYGSmffve7m9/79++/Pz1
                /QcAPAWHwArllTDwKh27HDRbgWB2NWjzCo28gNs+/Xxgl1CUPBFtl4MWSwIh
                YNbpPsVDXrPf7dVZOUqCSkDzItYk8Dpf+vu4eMFOI4h8j9v1/aZdbfNy7E7Y
                Gz1ttbi4Q+DLbTA9Xy5dK0MXg5cZnhB+4l5UXJOR5EpBWLPrLNoLqYfslPMg
                ol1PW23fp3s+KxKY+xcsiGIkogbiOgkClbx7VRX+77wQx06MK91giU6r6vKI
                1VhYXDpf6bVr1PpWiAo3b9pFni9oZLq/95ZFFNko9kNp7qs4DSQWqVgAAdLA
                /Y4XWzgjSmWVkNmjw2eGklEMxTw6NPCnmKjranfv7F+PMXpPl+d6NXN0WFAc
                srlmKlnFUQuGrphaVssQh0h9oKdPmYlsxtIsxdGlVJ3k8beEoqekNN4sbB5/
                Jeag5EiYQ7gOF+Yl14w1ZqWdYcQN6SOWro9amq7nTclCkDspcQOX4hKWjABx
                q2Y6u9yL4XJO1bH+jCyuU4GAc73WnXxRsANYcLXBDggMlgPsHY8+HrQYgWTP
                ImD123b69K/t6PSNEJg/fyVs88hrMntd2r1Bnu77e9XBAATy9R3HkcUzXEZP
                GNhEH/iehrTJIhZu4YIAjeMazzqvsA4mFs/8+qXpFPBG8WZly71kVdr2I7fZ
                8kUSHhEYOruDLvvwlQa6TG17NU6jdohB5T50c3b5vic8zPgk+BenHyzkczln
                4bpPhWBoGttBOyyzDc9HiskexU6X4My9BD4D0OIpAkXDAkAC7aXYgiQ8wXVM
                xXbrcsq6MgWAQAMGT4BDEjiu/QnqSRWWURpo5RG6CJOwIi89BhvXCu4Po9eR
                XVBdGHXBdCENFqow5sIdGN8FImAC7u7CmICMgEkBA1JmBdwTMCXgvoBpATMC
                ZgUYAh7I04cC5gTkBMwLWPgNvv/FeEAHAAA=
                """
        )

    @Test
    fun unreferencedModifier_implicitParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        // Do nothing
    }
}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
    }

    @Test
    fun unreferencedModifier_namedParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope { sharedModifier ->
        // Do nothing
    }
}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope { sharedModifier ->
                            ~~~~~~~~~~~~~~
1 errors, 0 warnings"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixImplicitParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        // Remember call should cause no issue
        val myValue = remember { 100 }
        MyLayoutComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -13 +13
-         MyLayoutComposable()
+         MyLayoutComposable(modifier = it)"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixNamedParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope { sharedModifier ->
        MyLayoutComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope { sharedModifier ->
                            ~~~~~~~~~~~~~~
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -11 +11
-         MyLayoutComposable()
+         MyLayoutComposable(modifier = sharedModifier)"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixOnExistingModifier() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable(modifier = Modifier.size(100))
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}

// Fake size modifier
fun Modifier.size(size: Int): Modifier = this
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -11 +11
-         MyLayoutComposable(modifier = Modifier.size(100))
+         MyLayoutComposable(modifier = it.then(Modifier.size(100)))"""
            )
    }

    @Test
    fun unreferencedModifier_noQuickFix() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable()
        MyNotComposable()
        MyNotCompliantComposable()
        UtilityComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}

// Composable that doesn't take a Modifier (not a layout)
@Composable
fun UtilityComposable() {

}

@Composable
fun MyNotCompliantComposable(sizeModifier: Modifier = Modifier) {

}

fun MyNotComposable(modifier: Modifier = Modifier) {

}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
    }

    @Test
    fun usedModifier_noIssues() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable(it)
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun constantLiteralInItemsCall_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionLayout {
        AnimatedContent(
            true,
        ) { targetState ->
            if (targetState) {
                items(10) {
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState("Foo"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(0),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
                items(List(10) { it }) {
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState("Foo"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(0),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
                itemsIndexed(List(10) { it }) { _, _ ->
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState("Foo"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(0),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
            } else {
                // Do Nothing
            }
        }
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                    """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                LazyListStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:20: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState("Foo"),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:27: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:34: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState(0),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:43: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState("Foo"),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:50: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:57: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState(0),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:66: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState("Foo"),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:73: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:80: Error: Each Composable within a LazyList items call should have unique content state keys. Make sure to either associate a unique key related to the item's data, or simply append the item's index to the key. [ConstantContentStateKeyInItemsCall]
                                state = rememberSharedContentState(0),
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
9 errors, 0 warnings"""
            )
    }

    @Test
    fun constantLiteralInIndividualItemCall_shouldNotWarn() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionLayout {
        AnimatedContent(
            true,
        ) { targetState ->
            if (targetState) {
                item {
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState("Foo"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + "Bar"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(0),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
            } else {
                // Do Nothing
            }
        }
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                    """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                LazyListStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun complexExpressionKeysInItemsCall_shouldNotWarn() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionLayout {
        AnimatedContent(
            true,
        ) { targetState ->
            if (targetState) {
                items(10) { index ->
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(10 + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
                items(List(10) { it }) { index ->
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(10 + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
                itemsIndexed(List(10) { it }) { index, _ ->
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(10 + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                    MyLayoutComposable(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("Foo" + index),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                    )
                }
            } else {
                // Do Nothing
            }
        }
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                    """
                ),
                SharedTransitionScopeStub,
                AnimatedContentStub,
                LazyListStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }
}
