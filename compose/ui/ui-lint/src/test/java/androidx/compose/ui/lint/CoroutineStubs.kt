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
package androidx.compose.ui.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.TestFile

object CoroutineStubs {

    val coroutineContextTestFile: TestFile =
        bytecodeStub(
            filename = "CoroutineContext.kt",
            filepath = "kotlinx/coroutines",
            checksum = 0x2eedf37a,
            source =
                """
    package kotlinx.coroutines

    public interface CoroutineContext {
        public operator fun plus(context: CoroutineContext): CoroutineContext = context
        public operator fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = null
        public interface Key<E : Element>
        public interface Element : CoroutineContext {
            public val key: Key<*>
        }
    }
    """
                    .trimIndent(),
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2WNQQvCMAyFI4pgDyL9AYIiHjz07H04kV2E+QfGVmdwJqVN
                wZ9vy/Rk4IXHC3kfAEwBYJK0gO+oo9o11HnG7m1afjkO1tw5UtcIMpneBone
                Br0q2Z+a9nEeg0pUoTZ/nxENkotiHCOJ9Xpdx+AsdUj9dYwu+V7ikGwq2Sv9
                ZBmQcofnKEgZVvx83bLLsKWaSSLr+S3tSrZwgA8/eWd6zwAAAA==
                """,
            """
                kotlinx/coroutines/CoroutineContext＄DefaultImpls.class:
                H4sIAAAAAAAA/61UW08TQRT+poVui0VuUgUULxRprbLg5YUSElMx2VjRCOHF
                p+kylml3Z3F3toEH/4C/xkfjgz/AH2U8066I9ZIl4WHPbb7znZ1zZubb9y9f
                ATzGE4a1bqA9qY5tNwiDWEslIrvx02wESotjXX4m3vHY045/5EUWGMNkh/e4
                7XHVtl+1OsLVFrIMI0deHDG8rzRTkNZTgaqpUAxLzSBs2x2hWyGXKrK5UoHm
                WgZk7wR6J/Y8QlnuICGPPMPigNnu9HxbUjhU3LMdpUPKly7tc4xh1j0Ubjch
                eM1D7gsCMqxUmsMdqJ+J7BqSdr26X0QR42O4hMsMo2V9KKk9yym3lG0LzfDh
                4ppZfiFO0jW0vO0JXyjzFx83tzc2zpWzdbF/vLm3Xd+qV0kylP83ZZoQb3nC
                dK4rTvIoMaTbrGkLw1SCtV8KzQ+45hTL+L0sXRRmRMEIMLCuMei0Z46lsdbo
                8KWoYuEOQ/HsRWKonaOtFpbp/CYOHcCUG7NQoX6QwTAzvL7aJaKRRnAgGCaa
                FN+J/ZYI90wXGaabgcu9fR5K4yfBhTex0tIXjurJSFLo6a8J0HiGV09vzG+w
                wq5sK67jkAiLjlIibHg8igQtje0GceiK59IUm0vo9v8ohXVkMGLmQbqAUeSQ
                xX3yNsnPkM7XpgufMVH71Ec8IJmj0eUoZ5Xs0gCDSUz1Oeg5IIvBTnCWGSt9
                45nEGcgCpjFDFEOFrlIhNlxo9B+FSrhG6+t99JXTgrN/L5jFQ5LzpDNYQhlz
                uEt6JVdAlfQ90o/6RDV6yoFFypoj7PxbZB0sOLju4AYWHdzELQe3fwB+y2cT
                9QUAAA==
                """,
            """
                kotlinx/coroutines/CoroutineContext＄Element＄DefaultImpls.class:
                H4sIAAAAAAAA/7VUS1MTQRD+JoFsEoPhISiiUSEIIcKCoiJBqqyI1pYRLaG4
                eJosY9hkdxZ3Z1Nw8Ogf8Jd4pDxYnv1Rlr1J5BEta2PhYfu13V/3dPfM9x9f
                vgJYxjrDSsNVtiUPdNP13EBZUvh6+ZdYdqUSByq/YQtHSJV/Kt7xwFaGs2/7
                GhjDYJ03uW5zWdNfVevCVBriDH37duAzHM5WegAvRXEuFSJ5MUxVXK+m14Wq
                etySvs6ldBVXlkvypqs2A9smL81sBySRZMi1kfV609EtMnuS27ohlUfxlknn
                TTOMmnvCbHQAXnOPO4IcGWZmK92dKJ2ybIUgtVJhJ4MMBtK4gItUYoSTaBhk
                WIzSxbOjGWZ4H6n759n1DPpxKY0RjDL059WeRTsw39MKMExHnG+8JhTDx/Pf
                sPwLcRjtvKeq/rS2sbraU8z6/6l8bXujtF4qEGXI/+0K0Pryqi3CTjbEYRI5
                hg/ntzD/0MQMruJmuDy3GKJFhjkYhjq++kuh+C5XnGwxpxmn942FJBUSMLBG
                KNDjFDuwQmmRIXP6zjAUeyhXwwK9Hh2Frn/EejUsUcNJYBjp/r/QIKC+srsr
                GLIVsm8GTlV42+GYGIYrrsntHe5Zod4xTrwJpLIcYcim5VtkenIyYpp/99/j
                9+qMW2rLqkmuAo8AM4aUwivb3PcF/UpvuYFnimdWmGy8A7fzWyosIYa+sM3E
                U/QIJBDHfdKekx4jni0Op46QnfuGoeIRxj63HB8QTRNPQMNFCntI+ljbHZdx
                pQWXRRLjNLwVkhPENeKP6BuIdZQ2TdHuTFAFXTlvnOSc7CVnDlMEvNqKuHac
                +/qfc8dRIjpJ2UcwhHkqVye+mEhRsN7S75J+j/S1FuAyHhPPUXSeYqffIm7g
                toEZA7MoGJhD0cCdn3hWRYahBwAA
                """,
            """
                kotlinx/coroutines/CoroutineContext＄Element.class:
                H4sIAAAAAAAA/5VSTW/TQBB9aye2EwqkKZQk5askpXyIOlQcQHxIKIBkUYrU
                ShVSTpt0G22yWSPvOgq3/BYO/AgOKOqRH4UYhyAQXNyDZ968nZl9ntnvP75+
                A/AI2wz3R7FVUk/DfpzEqZVamLDzG3ZibcXUtl4rMRba+mAMlSGf8FBxPQjf
                94aiT6zL0MzRxkeRwRsI+1Z8oovv3N3LczclP2XYyZ/97N4LKmjuxckgHArb
                S7jUJuRax5ZbGRPej+1+qhRlrS6bhu+E5cfccuKc8cSl6bDMlDIDBjYifiqz
                qE3o+CHDk/msWnZqzuKbz37BwAlOavPZrtNmB+sVp+FVncBtux9OvxROP3te
                oxAUKsWswS7Dg1w/tBw96drKk0957igb71mG6y8vYXh8BkmtV+KEp8pG44/K
                +NhgWPmbYdjOqcDHdVK9eBRr/57vjEhV6VAONLdpIhg2DlJt5VhEeiKN7Cnx
                8s9iSUGktUg6ihsjKCwfxmnSF2+kosr6svLovzqPNoICrdnLll2ggSAg5hZF
                DkpokvfotEzeRYtsg7xDXMMr4Sqha6jjBsU3Kd5aVG3iNvnnVHUORax04UY4
                H+FChIuoEMRqhCrWumAGl3C5C89g3eCKQc2gbuAbBD8B9Tm4oacDAAA=
                """,
            """
                kotlinx/coroutines/CoroutineContext＄Key.class:
                H4sIAAAAAAAA/5VQy0oDQRCs3o3ZJL7iO74FBVHRjUEQoggSIwQVQcFLTpM4
                ypjNLGQmEm/7XR5kz36U2BvjxVOcQ1d1TddQPZ9f7x8AjrBB2G6FNlC65zfD
                Tti1SkvjV35pJdRW9uzWlXzzQISL02q5fD2MoxrIttT25Oz6RbwKPxD62b9t
                vMimPSHk/2oeUoSpwcP+jbTiUVjBk0771eWklJRsUkCgFus9lXRFZo+HvEQc
                TeScgpOLoz44+TjKPBXiaDeTiaM87TpFp+QU3WS8RNgZagVemhNQlbD/r5Up
                Cbn3D4eHOcLmEAYPCwRv4CK4nJAw83fqoMV3YzWtZacSCGOkIWTv1bMWttuR
                hNx92O005aUKuFm862qr2vJBGdUI5LnWoRVWhdqk+bMwgp+TwgrXaVbmUcBi
                OsusgCXGVdYduFjr4zLWGY/5A9Ls9epwa8jUkK0hh1GmnAvjmKiDDCaRryNl
                MGUwbTBjMPsNX1GO1pgCAAA=
                """,
            """
                kotlinx/coroutines/CoroutineContext.class:
                H4sIAAAAAAAA/5VSW08TQRT+Zrbd3ZaqW/BSioJCkZuwlfgEhMRgDQsVjBhj
                0qehDM3S7S7ZmSXw1l/hD/Bn+GAaHv1RxrOAN0zM8jDn+p3vnMk5375/+Qrg
                BZYYprqRDvzw1G1HcZRoP5TK3fhpbkShlqfaAmNwjsSJcAMRdtzd/SPZpqjB
                kDsOEsXQnG1m4Fmdy4SioZpR3HGPpN6PhR8qV4RhpIX2I7J3Ir2TBAGhjI7U
                DB8zta5ty7Ns7WuNQPZkmI4h1xorKzeqWc88zNr7xur66hxJhtr/vkt/FfuB
                JFj5itt9I7U4EFpQjPdODFolS0UhFWBgXYqf+qlXJ+vgOUM06FeLvMKL3Bn0
                i9zmF05qpjE7bx9WBv1lXmcrvDi0Nevwaq7CNo1J2x70HWOe13PLJSdftYdz
                w3zTqJub55/4+WeTO9ZW2bGrvE7VplO4MNJc2naZYTrjvtOhGwyLN9yP0ZVn
                DNm2mu6fwWpfumRd0RDLdspSz0LySh6KJNBe7zhQFh4zlP6MMCzcYH4LUwwz
                GUe3MM0wcj2z1KXxx94lofZ70gtPfOXTpbz8fT10Wtezb0UselLL+C9YYc/v
                hEInsaQ/eWEo441AKCUpVdyLkrgtX/sB5Uav6D7808qklSOXHiB4jiEPk/xZ
                8kxarkV6jp7FLh0C2CjAwHwKR/EXcOgakCALJKuknxBwEqOokR43C3hKeoL0
                swuKGSyS3qWqEvW+1YLh4baHOx4clMnEsIcR3G2BKdzD/RZKCg8UKgq2wqhC
                VWFM4aFCQSGv8EhhXGHiBxzlJBgpBQAA
                """
        )

    val coroutineScopeTestFile: TestFile =
        bytecodeStub(
            filename = "CoroutineScope.kt",
            filepath = "kotlinx/coroutines",
            checksum = 0x2041f1c4,
            source =
                """
    package kotlinx.coroutines

    public interface CoroutineScope {
        public val coroutineContext: CoroutineContext
    }

    class ContextScopeImp(context: CoroutineContext) : CoroutineScope {
        override val coroutineContext: CoroutineContext = context
    }


    public suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R {
        val newScope = ContextScopeImp(EmptyCoroutineContext)
        return newScope.block()
    }

    public object EmptyCoroutineContext : CoroutineContext
    """
                    .trimIndent(),
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2WNQQvCMAyFI4pgDyL9AYIiHjz07H04kV2E+QfGVmdwJqVN
                wZ9vy/Rk4IXHC3kfAEwBYJK0gO+oo9o11HnG7m1afjkO1tw5UtcIMpneBone
                Br0q2Z+a9nEeg0pUoTZ/nxENkotiHCOJ9Xpdx+AsdUj9dYwu+V7ikGwq2Sv9
                ZBmQcofnKEgZVvx83bLLsKWaSSLr+S3tSrZwgA8/eWd6zwAAAA==
                """,
            """
                kotlinx/coroutines/ContextScopeImp.class:
                H4sIAAAAAAAA/41SXU8TQRQ9sy3dZS10KR+WgihfUoqyhfiGMVESkk0qGjDE
                hKfpsinTbnfJ7rThkV/hD/AXaKLR+GAIj/4o453tBm0lysPMvffknnPn3js/
                fn77DuAJthiW2qH0RXBuu2EUdqUIvNjeDQPpnctDNzzznM6ZDsZgtXiP2z4P
                mvarRstzpY4Mw+KN7NRN+DpGGHJPRSDkM4ZKpf4vRlp4Z/2IYbkeRk275clG
                xEUQ2zwIQsmlCMnfD+V+1/d3GHS3TzFgMiz0te1Wr2MLgqOA+7YTyIj4wo11
                5Bmm3VPPbacCr3nEOx4lMqxV6sMd7vyBHCqRJj0sj3EUTIzBYshUVKyjaCKL
                SZqRO9QIw+qt+s1jGjOj0HCXIStPRcywcjNxYDHU/2TTk8Nyqpn1W9VlmEjz
                7Jee5CdccsK0Ti9D34Opa1RdYGBtws+FimrkndDP2bu8KJpaSUvO5UXfNbKl
                y4ttrcZejBVzllbWapmr9znNyh5MWSMqenv1LkuIQYxy1shZulLbVuv+71dS
                zx1ENtvU7NxBN5Ci4zlBT8Si4XvPf/8UGudueOIxFOrE2e92Gl70hlMOQ7Ee
                utw/4pFQcQquDGtd/5ABUfMw7EautycUZzblHP1VHVu00ixNL0enqHZMtkoj
                VPEoWY2sDoPsBkV1shpZa6N45ysmql8wVd34jNLHJPMR3ePIEFeHSZwC2ceE
                zfQ5mEVZrYo8M6mzST4Nt1+IpoT5tIyt1kl2pPoJpQ/X2rkENBLNfD8h1Rx8
                sZ3c66iR3SX0HuUtHCPj4L6DBw4WsUQulh2sYPUYLMZDrB3DiFGOUYlhxpiJ
                VTgdYy7G/C9SSg1BjQQAAA==
                """,
            """
                kotlinx/coroutines/CoroutineScope.class:
                H4sIAAAAAAAA/41QTWsbMRB9ktdrZ5Mma7tN/HEqJSS9dF1Teik9FENhwXXA
                hhDwSd6oRvZGKivZ+Ojf0kN/RA/F5NgfVTpr3IZgSAJi5s3w5mnm/f7z8xeA
                d2gxvJwZlyq9jBKTmblTWtqo+w8OE/NNlsAYwqlYiCgVehJdjKcycSUUGGoT
                6f6Tu0Y7uXQMZ+evew+pbokfGF71TDaJptKNM6G0jYTWxgmnDOG+cf15mhKr
                slWLvkgnroUT1OM3iwKdwPKwlwcwsBn1lyqv2oSu3zK8X6+qAa/zgIfrVUBv
                g8u8/LW+XnV4mw2qIW/yduHq9od3+933m17ZC4v5dCdf71FzaBX6OUx2TDh9
                ogWV+3pvZjTbGsy1Uzcy1gtl1TiVn+58YQiGZp4l8rNKJUNjS73cIfp0BDxs
                rPEYivBp0TpVeS5R5mhs4gmalD9St0ysvREKMYIY+zEO8IwgDmMcIRyBWVRQ
                HcG3qFk8t3hhcWxRtPD/AoCSp91SAgAA
                """,
            """
                kotlinx/coroutines/CoroutineScopeKt.class:
                H4sIAAAAAAAA/41TW08TURD+zrb0iqXUS7koIq1SrlsQFWkhEgKhsVTTNiSE
                p+2ykqXtLtk9rfDGkz/EX2B8MWpiGh598hcZ5yytUFqp2ew538x8c2bmzJmf
                v7/9ALCENYZY2eQV3TiRVdMya1w3NFveaMGCah5rr7kXjCF8pNQVuaIYh/Kb
                0pGmktbFEFLbuAzFRPbiRPmoXpXf1QyV66Zhy1tNtJhq2dsCGgRqiiCkprLX
                I6UYfqXzK536tV7B0nPZnvWl5npklJ4r5lNrqZku8XsV03TtWlI8a1qH8pHG
                S5aiU9KKYZhcuSggV6tUlFJFI1rsJprJBZNYE13LNLh2wp0iM9VjLwIMiS68
                zeoxP/17J00vL/oZfJlcobie29hkmO52kV09U/0IYcCPWwj3eF5/Q0UYPGnd
                0Dk9yETixpa1gkzt9uMO7gYQxD2GsZufgRdDFEE36maZnuhyorMdnZouPevH
                CEaDGMZ9uhpDe9988vHuCbddPrWor1Qx1TLDeK8BYQjGVbN6XNGEfMXhXxPD
                MNii7GhcOVC4QjqpWnfRlDOx+MUCBlYWQCLjiS5QktDBAoPaOBsJSD5XQBqS
                Lv9w48wBZFwlPCJtswm3r3EWZouTYWkk5mMRd0RKuiOBiE+gbZbsi3gi7iGW
                9CRd5x89ks+7ff7hlUBhnwi1SCnkGSKtdK8WHfuPUWWiiLGW9+YJ1wyb3FvH
                FE8dzmC713yZM7g3zAPq1UCWtLlataRZRTFgIhdTVSq7iqULuan0F/RDQ+E1
                i/BovkZ3XdUyRl23dTKvX04gNf+69a1iKVWNa1YbLVAwa5aqbeni9OGmz27H
                eViABDcumjWMPnhIek7SHlwQDYt+R3DvMwYbuP0F0Z3p2ZmveODCJ9FVvKA1
                QDto9kL0LRMaJ7cQ/BjDQ8cSJc0jJ0AUE4hRmJeOrxcrtHtI9tGeEmdIJPid
                TPwOJe2sz7BK+xZp45Tf4324MniSwWQGCUxlMI2ZDGYxtw9mYx7yPvw2+mwk
                bYxTeTYWbcRsPLWxZMPzBwVwSvaKBgAA
                """,
            """
                kotlinx/coroutines/EmptyCoroutineContext.class:
                H4sIAAAAAAAA/61VW08TQRT+ZgvdtixyUbl6p0oLyhbEG60o1ppsqNVYQjQ8
                DWXEhe0u7k4JPJjw5A/xF0h8wGhiiL75o4xnSkWhxCzGh5kz58z5zjl7vpnZ
                7z8+fQEwiRmG1KonHdvdMCue79Wk7YrALFTX5Gb+l573XCk2pA7G0LnC17np
                cHfZfLK4IipkjTAMHRGjGd7KEM3Zri2nGSKp9LwBHbEEWhBnaJGv7IBhpBi2
                mixh1pwaYYqpo0BN/ulQXvQtRc9fNleEXPS57QYmd11Pcml7tC55slRzHPLK
                hAiWfChe8pojreqaE+joZHgdrtT/+D0G2tGdQBdOMuiVPSPD5ZC9iCwL8n4e
                qurkrNgMV1Sy4IiqcFUCkStMTR0LMx26mNxcITudTdPMkPwbqcQoX3QEub35
                f/z8QzsM9GJQcXWGOr8qNhnC4VUmhliu4tQvVwKaulExq1SemynlCwYuwYiT
                cYihqxHQfCwkX+KSE1CrrkfoMWBqiqsJDGyV7Bu20jK0Whqne7G7lUhofVp9
                7G7Fvr3V+na3JrQMe6DHtK/volqnplwnGIw/Dz7D6DGaoINS6Q2FYTjk9+uY
                pJ7Nqp517e+XK96aGFulMIPPaq60q8Jy1+3AJq5nfvNP70jeWxIMHUXClGrV
                ReHPqfPA0F30KtyZ576t9IYxeTjWU+7zqpDCPxA0XraXXS5rPkEMy3WFn3d4
                EAjaSpS9ml8Rj2wVrr8Rbr6pMIwTZy1ERpRGvyKR5G1iROkdJFton15Q0u6Q
                ZiraSLaO7CCxTQsNUw1n0JnK0mzsOaCNVorldpxApA6eIm9NeY+MfsSp90ei
                e/Y8Gmi1Ok22gyXlaOisoTA6z31UZVOCs2ETnMN52r9b9+7fTzRwOFEMF/bb
                0FsPArR9hvZiBxc/ILldN0QwTfMAyS6KkqF4EySvR+O4QfImyXv1PLdwv/5r
                pFeSWnVlARELwxZSFtIYoSVGLVzFtQWwAGMwaT+AEaAnQOwn9GRn5FcHAAA=
                """
        )
}
