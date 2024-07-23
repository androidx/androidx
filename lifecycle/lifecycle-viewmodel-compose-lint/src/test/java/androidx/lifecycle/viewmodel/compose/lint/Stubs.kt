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

package androidx.lifecycle.viewmodel.compose.lint.lint

import androidx.compose.lint.test.bytecodeStub

internal val VIEWMODEL =
    bytecodeStub(
        filename = "ViewModel.kt",
        filepath = "androidx/lifecycle",
        checksum = 0xacdef33f,
        source =
            """
                package androidx.lifecycle
                public open class ViewModel

                public class ViewModelStoreOwner
                public class ViewModelProvider {
                   public class Factory {
                        public fun <T : ViewModel> create(): T {
                            return MockStore<T>().vm
                        }

                        private class MockStore<T : ViewModel> {
                            lateinit var vm: T
                        }
                   }
                }
                public class CreationExtras
            """
                .trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuZSScxLKcrPTKnQy8lMS02uTM5J
                1SvLTC3PzU9JzdFLzs8tyC9OFeIOAwr5goS8S7hkuLiB4nqpFYm5BTmpQry+
                lUiySgxaDABrUUXUawAAAA==
                """,
        """
                androidx/lifecycle/CreationExtras.class:
                H4sIAAAAAAAA/4VRTS8DQRh+3qluWcXWZ30m4oKDLXEjEoSkSZEgvThNdwej
                29lkd0rd+lv8AyeJgzSOfpR4d7m7PHk+3pk878zX9/sHgF0sE1alCZNYhz0/
                0rcqeA4i5R8nSlodm5OeTWRaAhG8B/ko/UiaO/+i9aACW0KB4Oxro+0BobC+
                0SyjCMfFEEqEIXuvU8Ja49/b9wiVRju2kTb+mbIylFayJzqPBa5IGYxkAAK1
                2e/pTNWYhduElUHfdUVVuMJjNuhXB/0dUaOj4ueLIzyRTe1Qdrbc1OrpLA5V
                tNW23O+YKWGioY0673ZaKrmWrYidyUYcyKgpE53pP9O9irtJoE51JuYvu8bq
                jmrqVHN6aExs831SbEPw+n9ls9dgrLLycw0UN98w/MpEYJ7RyU0PC4zl3wGM
                wM3zxRznsJT/E2GUs/INCnWM1TFexwQ8pqjUMYmpG1CKacxwnsJNMZvC+QGU
                yd0P5AEAAA==
                """,
        """
                androidx/lifecycle/ViewModel.class:
                H4sIAAAAAAAA/31RTS8DQRh+3qlua5W2Pqo+ww0Hi7gRCRJJk5YE6cVpujuY
                djubdKfFrb/FP3CSOEjj6EeJd5c4ujx5Pt6ZeWbm8+vtHcA+VgjL0gS9SAeP
                Xqhvlf/kh8pravXQiAIV5kCEUlsOpBdKc+ddtNrKtzlkCM6hNtoeETIbm80C
                snBcjCFHGLP3Oias1v/b+IBQrnciG2rjNZSVgbSSPdEdZLgYJTCeAAjUYf9R
                J2qHWbBLWBsNXVdUhStKzEbDfKU6Gu6JHTrJfjw7oiSSuT1KVhf+jtzuWC53
                ypRQrGujzvvdlupdy1bIznQ98mXYlD2d6F/TvYr6PV+d6UQsXPaN1V3V1LHm
                9NiYyEqrIxNjHYLv/ls3eQrGKisv1UB26xX5FyYCC4xOagosMhZ+BjAON/WW
                UpzHcvo/hAnOCjfI1DBZw1QNRZSYolzDNGZuQDFmMcd5DDdGJYbzDcrOclbc
                AQAA
                """,
        """
                androidx/lifecycle/ViewModelProvider＄Factory＄MockStore.class:
                H4sIAAAAAAAA/5VUW28TRxT+Zn3ZtXHK2hSahJAGSIvtQDakKU1JGggpVJac
                FGFjWuVpsp46E693o92xSfrkp/6Q/gIqtSrqQ2Xx0If+KNQz9ioNNwsk68y5
                fOc7Z86c9b8v//obwAruM9zifjMMZPPI8eRPwj12PeE0pHi6HTSF9zAMerIp
                wvkH3FVBeDy/HbjtGmnCBGNYWa/fro7LX9uoHvAedzzut5zv9w6Eq9YY7Nd9
                JpIM6XXpS7XBkCiWGjmkYWaRgsWQVPsyYlgdW+ndnVLBVEuoRodhrlga3y5B
                i6V6nc6r1SBsOQdC7YVc+pHDfT9QXMmA9J1A7XQ9jTZ6xDo7njMHG/kMDBQs
                2IRuB8qTvnPQ6zjSVyL0uedUfBVSFelGJs4zlNR+GDx97OuBSO7Jn0WT7nco
                QnW86boiiu4fueJQN8NwvnhqxDVN01rT8/sEk1lcwBTDzLj2TFykS0ej+Vwu
                jr9KqUHPVKT5aMVcp6wbdzYszFEX7r5w2/FgHvKQdwRdjeFa8c0FeHu/V3BV
                9ztP7z98gHw1HtS2ULzJFdfj7vQStLdMi4wWYGBt8h9JbS2R1rzJUB/0C1lj
                0sgO+sPDsLVimZYxOeiXLWvQt1nZWDKWjaXEvdSLX9OGnXw0Y6emjdVB/4cX
                vyySy6as7HTSStvmlaRl2RnNvUzl6kxXvf4hu2hihWH+fTJM3KLBxmkMmZMl
                ZsidgBfbikI12fK56upQcovcDGer0hc73c6eCOt8zyNPoRq43GvwUGo7dk7U
                FHfb2/wwti8+6vpKdkTF78lIkmvz/1Wntl+PnjzuK7BcxfdFuOXxKBJkZmtB
                N3TFA6kLTMUUjTfosUSfRWr0jPorIblGloHPkSCd/gJIrpPHGSKAVPlPZH4b
                Qr4hmR46z2CDZG4EQJZsEFUOE0Sik78jtI7ly7/j3I//IPnsSeHj55hmz8iZ
                wJ0RUcXEzCnS3CnSfEw66u0j3I1RZ4exS5iNC92lqKGzFgqXn+Oz8sIfOPdq
                s+mY98IIF/Nqbe7U7T/FJp0mi0skcY9kgQJf4iusYmqofY3pGJ7A1vC8jW/p
                rFLWNZpDcReJCkoVlCtYwHVScaOCRTi7YHrwN3dxJtK/5Qj5CF9EMCPYEXIR
                JiJcijBL/v8ARg7ZkCkGAAA=
                """,
        """
                androidx/lifecycle/ViewModelProvider＄Factory.class:
                H4sIAAAAAAAA/5VSXU8TQRQ9M9tu222VBUH5UESo0iKyQNQHJCTahFhT0EjT
                xPA03Y516HY32Z1WeOtv8RfoiySamMZHf5Tx7tKg0URlHu7ce+bcc2fu3G/f
                P30BcB/rDKvCb4WBah07nnot3RPXk05Dybd7QUt6L8Kgr1oyLO4KVwfhSQaM
                wT4SfeF4wm87z5tH0tUZGAzmtvKV3mEwSuVGAWmYFlLIMKT0GxUxrNUuUugR
                CbqhFFoyLJTKf80l7vJ2fevvnJ1SuV4n5lItCNvOkdTNUCg/coTvB1poFZC/
                H+j9nhfrPbzIXYt7gds5IE9mMGbBjh+dbkvd6BYwgUKMXGEYr3UC7Snf2ZNa
                tIQWVIZ3+wb9A4tNLjZgYB3Cj1Uc0e/w1gZDZTiYtPg0t7g9HFg8a5wFWT49
                HGzydbbFMk/SX9+Z3ObPpmxjlj9NLWazw4GdWuHrZwdmLLXJkgJ1hvl/NTR3
                /iaG4v80I4MiQ2bUEYbCOWGto2kGKuQyjNWUL/d73aYM66LpETJRC1zhNUSo
                4ngE5g5U2xe6Fxefe9nzterKqt9XkaLjxz8/jMpUfV+GFU9EkaTQOgh6oSt3
                VawyM8ps/JGHDXCazXhxagiNKtkSRU7cHtrTK6fIfkiOy2TNBDSxQrZwRkAO
                Fu3jyBNiJMkPEjEg/xn2q1OMf8Tk+98ksr9I5EcSdxPOJayOWJdpN3CP7ATh
                HLdxBzM0QhxLmMVawl6mmwIVYk/RVa4ewqjiWhXTVWLOkou5Kq7jxiFYhHnc
                PEQ2ghVhIYIZIR/hVoTFCIUISz8ALZc+bgoEAAA=
                """,
        """
                androidx/lifecycle/ViewModelProvider.class:
                H4sIAAAAAAAA/41QTWsUQRB91bM7k0wmZhM/svnwkyAq4iRBEGIQNBAY2KgY
                2cueemfapLOz3TDduya3/S3+A0+CB1k8+qOCNXHxnMur916/oqvqz+XPXwBe
                4iFhS5qisro4T0v9ReUXeanSrlZfj2yhyo+VHetCVRGI0DqTY5mW0pykH/pn
                KvcRAkK4r432bwjBk6fdBE2EMRqICA1/qh3hcec6H7wmLHcG1pfapEfKy0J6
                yZ4YjgMelGqYrwEEGrB/rmu1zazYITyaTpJYtEUsWtNJLOZEezrZFdu0R8G7
                5u9voWiJOrlLdX90KHNvqwvC8+tMtjWLR2gTkv/PLwaeVzxgSljqaKPej4Z9
                VX2W/ZKdlY7NZdmVla71zEwyY1R1UErnFB8mPrajKleHun5b+zQyXg9VVzvN
                4bfGWC+9tsZhB4IPOtu9vi/jBqv0SgPNZz8w952JwCZj+M/EXcZkxucRcw1w
                jzFmb42zq4z3r7rW8YDrK/YXOJv0EGRYzHAjwxJaTLGcYQU3eyCHW7jdQ8Mh
                drjjEDqs/gWgamwlTAIAAA==
                """,
        """
                androidx/lifecycle/ViewModelStoreOwner.class:
                H4sIAAAAAAAA/41Ry0rDQBQ9d9qmNlZb3/W5lOrCqLhTBBWEQqug0o2raTLq
                tOkEkqmPXb/FP3AluJDi0o8Sb6K4dnM4jzvcw53Pr7d3AHtYJaxLE8SRDh69
                UN8o/8kPldfW6qEVBSq8tFGszh+MiosgQrUr76UXSnPrnXe6yrdF5AjOgTba
                HhJy9Y12GQU4LvIoEvL2TieEevN/K/YJU81eZENtvJayMpBWsif69zkuSymU
                UgCBeuw/6lRtMwt2CGujoeuKmnBFldloWBsNd8U2HRc+nh1RFenULqVvy3+r
                t3qWS54wJVSa2qizQb+j4ivZCdmZbka+DNsy1qn+Nd3LaBD76lSnYvFiYKzu
                q7ZONKdHxkRWWh2ZBDsQfIPfsulJGGusvEwDhc1XjL0wEVhkdDIzjyXG8s8A
                SnCzfDnDBaxkP0YY56x8jVwDEw1MNlBBlSmmGpjGzDUowSzmOE/gJphP4HwD
                iZB0Wu4BAAA=
                """
    )

internal val VIEWMODEL_COMPOSE =
    bytecodeStub(
        filename = "ViewModel.kt",
        filepath = "androidx/lifecycle/viewmodel/compose",
        checksum = 0xa5fbc03d,
        source =
            """
            package androidx.lifecycle.viewmodel.compose

            import androidx.compose.runtime.*
            import androidx.lifecycle.*

            inline fun <reified VM : ViewModel> viewModel(
                viewModelStoreOwner: ViewModelStoreOwner = ViewModelStoreOwner(),
                key: String? = null,
                factory: ViewModelProvider.Factory? = null,
                extras: CreationExtras = CreationExtras()
            ): VM {
                return factory!!.create()
            }

            @Composable
            inline fun <reified VM : ViewModel> viewModel(
                viewModelStoreOwner: ViewModelStoreOwner = ViewModelStoreOwner(),
                key: String? = null,
                noinline initializer: CreationExtras.() -> VM
            ): VM { return CreationExtras().run { initializer() } }
        """
                .trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuZSScxLKcrPTKnQy8lMS02uTM5J
                1SvLTC3PzU9JzdFLzs8tyC9OFeIOAwr5goS8S7hkuLiB4nqpFYm5BTmpQry+
                lUiySgxaDABrUUXUawAAAA==
                """,
        """
                androidx/lifecycle/viewmodel/compose/ViewModelKt.class:
                H4sIAAAAAAAA/9VWW28bRRT+Zr2215s02bhx67i3pHFzcZP4QgmQGy1pTUzs
                NNTBUAKUjb1JN7bXlXftNn1AEQ/wxBNPfUXiCQnxgKCAVEXhAQl+Bv8DcWZ9
                iUOsXECVila758zsmTPfd87Mmfn9r5+fAbiGNYaIauTKJT33KFzQ17XsVrag
                hau69rBYymmFcLZUfFAytXCGelK8Z9FygzEom2pVDRdUYyN8e21Ty1Kvg8FT
                bZgxfDOSbOO56Sdtlcra7YeGVp5O7vlKW2Xd2Jg+dORyuVTVc1o5GFez5GSr
                rfV8WVMtvWTcemSVVXN69FCP0wzfz2RSU4cbzb1QhFYyKYJ9unpwegkSw8V8
                ySroRnizWgzrhqWVDbUQThgcjalnTTdkBl/2vpbNL5WspUqhsKyW1aJGhgzD
                I8l/ZrcNpdFMJzpxSkYHuhhcmo1LgsLQ2eqXpmnjjg/24jQf3MswdpLouHGG
                psvycGgM/SNHpLYTfvTJOIsAQ1dQD64HW9YoSzAcO6sMjry2xeA9GAsG93oN
                HcPEyXLNMHiMdDP0NGEHc9q6WilYDM9epBWZaJPlo3bd0PHguzFEKZ/RDd2a
                oyyM8NUzglEZwwgxDByJzY0xGePc9vF/iVjLllqvGFnu3wzH61r0aLbfPuca
                czi+mfFjJPEqLytzjepyZW9E4xwoVwxLL9I4u62uFTSy6+CZ0dWC/phXn9j+
                6tMGihvXKJ+6US3laQsPtasPB7s6MYlXOvAyXmU4f1iQ3JiiRUFbXQ2OE9zx
                lqNrb+8Ho4QyaN3Xzb2+e5XYtYJaXMupXItQcTkq5QxfPMcF9W92VE/DaUqz
                1JxqqdQnFKsOOu0Z/3j4B1T58lwR6OcjnWvEVshRTP7c2Z6QBUmQBb8g72w3
                hdLVaDVfqSkDS8rOdkBYYJdlaWdbYX4WEiJCTJIExREQImJMJs0ZEP0s4or1
                SZLiDpz2il4h4uHfBRaRdr9yCZK8sPu59OtTtrPNm0pHIH4Ct1zvtPVTtkUX
                WXgWdj+zXSvdu58Kbtkp7T6JRRinSotUyKRoDRwVUPCCX49pa+IvNjpp52h0
                ppaMxt+VrQd8UwSPU2DdeJvOjnjj7OhsGkzkqb6Hku1O8XSpUs5qN7W1ykZz
                bprPWVULFdpOX6dTN5blVkfyou1FDqX7G1pcvtof7d9ndNJrIHmI9a+reY0P
                boczTv/ILJSUo4PRsVhkKkpKbCoWlUO36PYhzpMfhu6kbmhLleKaVl7h5YSH
                u5RVCxm1rPN2vdOT1jcM1aqUST+VttRsPqU+qP87d6dWlBJGVTd16rphGCXL
                LmsmxTRh0PabL6imqVFTroUvrvORve1iydBXd5g54A5RCBD5FoKo9MEJF7Xf
                o9YmSSdJf8jreYrucW8PfR2T4tjdp/D9gHPf8d2Gu/R10YrqppvT+/YF3Em6
                B+dxwfbqh4SLtnc/LqGfLLk2gMs0lmsKBuHAqu3LrXgQxBWy4fP/QT0ukgmf
                KH7yBB2/YPjuj7i66BOd1HSypE9020rKJypSzWKcLCaWDgHswAc2YLFbkrpt
                8CHIdo+bnm6SZwiwn+QlIjFAcpT+R0lebyF4vYVgokkw0SSYaBJM1AjagQ3T
                7JzYl3ViwzWcY96XOM65Ovop0THpHOsTf8JrAnYwjb04nyEnXeilm58PAZJX
                6B2il8OaI5cBunvOECwnWXRhliYWSfbZUHlFHG5CHW5CHUaMxgr1DETwYT2f
                UcDOxuv1bPzG80Ny9sK+ZFyo56I9Fadj0jXW59xHpZYBEUKXuyX+Ck3aQ2AV
                ouMlogpN3UsXHB9B6qWRvURsj2ikTtRFFjWiTrLfIzrbJDrbJDpbJ+rER9SS
                qe8s7iBNo+7Z1FfwMcn/R6mBSlBXicJ1onNjFY4E3khgPoGbuJVAHG8msIAE
                GZh4C4urUEw4TSRNpExIJi6auGRiyUTMxG0TgyaWTYRNDJi4bHJjl70MumiC
                d+jN2I7e/Rt/dMvZYg8AAA==
                """
    )
