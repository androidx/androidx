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

package androidx.navigation.runtime.lint

import androidx.navigation.lint.common.bytecodeStub

internal val ACTIVITY_NAVIGATION_DESTINATION_BUILDER =
    bytecodeStub(
        "ActivityNavigatorDestinationBuilder.kt",
        "androidx/navigation",
        0x2a71d14,
        """
package androidx.navigation

import kotlin.reflect.KClass

public abstract class Navigator<D : NavDestination>

public open class ActivityNavigator : Navigator<ActivityNavigator.Destination>() {
    public open class Destination: NavDestination()
}

public class ActivityNavigatorDestinationBuilder :
    NavDestinationBuilder<ActivityNavigator.Destination> {
        public constructor(route: KClass<out Any>)
    }

public inline fun <reified T : Any> NavGraphBuilder.activity() {}
            """
            .trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/y2KQQrCQAxFI4LQuBBGvIDgxkXvUHUhFFx5gaETamCalGks
                7e0dqx8+fN77ALAGgFVuAf9gwL2XkJTDVIofufXGKu5UNcYj2/z4MU03Goxl
                sZc3x0CpNoeVzPZiafPe5etVxZLGuMgig7sOVhsecNtoV9Lkuz6S2zzpi49w
                hg8VwfQ8lQAAAA==
                """,
        """
                androidx/navigation/ActivityNavigator＄Destination.class:
                H4sIAAAAAAAA/5VSTU8UQRB91cPOLsMqHwouICIGiHpwAb1pSABDMsnCAche
                ODU7Ha3sbE8y3bvB2/4W/4EnEw5kw9EfZawZOEDggId+XfXqVeqj+8/fi0sA
                n7BC2NQ2yTNOzptWD/ib9pzZ5k7H84D9j8NrKstXvxrn2ZbRKojw5qE0kd/R
                BYTwC1v224Tg7bt2HRWEEcZQJYz57+wIH1v/Xf8zYbrVzXzKtnlgvE6018Kp
                3iCQoaiA8QJAoK7w51x4G2Ilm4Tl0TCKVEOVZzSszTVGwy21QbuVq5+hmlKF
                bIuw+mBfdweUomuP6r6KF4SJW5mE9XuiW+HdPqeJyT90vexpL0sMYbLF1hz2
                e2cmP9FnqTAzrayj07bOufBvyHpsrcn3Uu2cke1Gx1k/75h9LmLzR33ruWfa
                7FjEO9ZmviznsAIlr3Kzs+KRBBfFa5Y+UHn/G7VfYii8FAxLsoYlwfq1AOOI
                5A7wSjASTqGBeWGXy6wFvC4/nKxBtPVTBDGexHgaYxJTYmI6xgyenYIcnmNW
                4g6Rw5xD+A+jXaqlrQIAAA==
                """,
        """
                androidx/navigation/ActivityNavigator.class:
                H4sIAAAAAAAA/51RTW/TQBB9Yztx4gSaBigJUEo/+JQg6ceprSq1RUiWQg+0
                yiWnbbxqV3HWkncdlVt/C/+AExIHVHHkRyHGbgWVWlXAYd/OvHnzRpr58fPr
                NwBrWCI8FTpKExWddLSYqCNhVaI720OrJsp+3DunktQHEQ5612l/azavLV+x
                WnorjVW6qG5sbRBmb3T14RHKm0oru0VwX7zs11GGH6CECsGzx8oQnv/daB42
                3RslNla6815aEQkrmHPGE5fXQTlUcwCBRsyfqDzrchQtE16fnTYCp+X8eRWn
                MtM6O12pNL2m03W6tE7eTun7p7LTcPOmFcLczTvj6av/sTZC7VJKWP5nCx8P
                CM+uSC4pdjIVRzJ9M7K85t0kkoSpntJyLxsfyvRAHMbMNHvJUMR9kao8vyDr
                odYy3Y2FMZKPU91XR2yZpVwK9pMsHcp3Kte1P2TaqrHsK6O4cVvrxBajDebh
                8IEvTpHfm3GWs06RA6VXX1D9zIGDx4zlgvQxx1g/FyBAjX8PTxgD5h6yto0q
                W+ddLhaK/xEW+V/nep17bg3ghrgdYipEA9McohniDu4OQAb3MDNAyaBmcN+g
                ZeAbtH8BM+ZISEwDAAA=
                """,
        """
                androidx/navigation/ActivityNavigatorDestinationBuilder.class:
                H4sIAAAAAAAA/51TW08TQRT+Zlu6ZbmVKhRQ8cLFQpUtl8QYCAqoSWOtRgwv
                vDhtxzLtdjbZmTb4xm/xH/ik8cEQH/1RxrPbik0KSfVhZ77znfOdc2bm7M9f
                374D2MIWwyOuqoEvq6eu4m1Z40b6yt2rGNmW5mOpQ/nBM6GNVJFzvyW9qghs
                MIb3xcvUpOqP37k0tK/QYo9ye3ebYWXgCjbiDIkdqaTZZZjLFhu+8aRyA/HB
                ExXjvjzwuNbbK0cM7hXOnVyxztvc9biqua/LdfJQE6FioegHNbcuTDngUmmX
                K+WbqLp2S74ptTyPmh0K/JYRSTgM890C9XbTlcqIQHHPLSgTkFpWtI1RhqnK
                iag0uvI3POBNQYEM97P9bfQwh2GSGrU1inFMOBhDiiGWDe0E0g6GcI0hbk6k
                Zng82LX33yadJnPFDTJM/vG8EoZXueHEWc12jIaKhctwuICBNYg/laGVJ1Rd
                Z3h+fjbjWDPWxXd+1oEpAuQ7P9tIpuNpK2/l2X4mOZ0eT8XmnHQiyYgbysd/
                fEpYqUSYbIMhN/j8UYeb/zGCLDzG+j/rbCwzLA0ks5FlGOnRMiwP8D5rDUOP
                fOBXBcNEUSpRajXLInjHyx4x6aJf4d4RD2Rod8nhQ1mjHK2A8OLbljKyKQqq
                LbUk98X47f2dbIbRglIiiJ5dkOkc+q2gIl7IMN1sN8VRJ0GPDuuwaAq7M0BD
                aSOGB2Q9Id6ifSSXHvmKydUvuP6ZTAsPaU1EoWNYIzzdCcMUIUTIQYb8bhSd
                RJ52O8w9TCBO9UAB9ONgBavEdcJi2Ij2HDZpf0r+Gepq9hixAuYKuFHATdwi
                iPkCbuPOMZjGXdw7hq0xrbGgsajhaGQ0ljTs3xTF98w2BQAA
                """,
        """
                androidx/navigation/ActivityNavigatorDestinationBuilderKt.class:
                H4sIAAAAAAAA/41STW/TQBB966SJY/qRprQkAQqUtE17wCnqhTZEKiDAagiI
                RLnktHFMsomzRvY6Kree+D/cEAdUceRHIWbdSFTAoZJ35s2b8ZuZtX/++vYd
                wCH2GJ5wOQgDMTizJZ+JIVcikPaJq8RMqE+tSyoIX3iREjJJPouFP/DCU5UF
                Y8iP+YzbPpdD+21/7LnEphhMPhdgqFab/+tAyq9C/nE0Vzve6zI06p2j5t96
                x43rC2TqaiSihgmTYXMSKF9Iezyb2kIqL5Tctx2pQiEj4UZZWAzr7shzJ61A
                tWLff8dDPvWokGG3+u8YV5i2FhlSx0UsYsnCDSwzLFVE5UPlz97MYViu6Hmu
                kNvXWoVhtTmf/o2n+IArTpwxnaXoozFtctqAmkw0MCh5JjSqERocMBxenOet
                i3PLyBuXTpuiUS4RKBs1tmWZVGEU2b5RS9FJv/7x2dTvPmaJbIdh5xr/wKOJ
                Ykg/DwYew0pTSK8VT/te2OF9n5hCM3C53+Wh0PGczLXFkDTikLDVDuLQ9V4K
                nSi9j6USU68rIkGVJ1IGKukV4QAG0kg2zpewgAzFDyl6St7Qt7FfyH3FSqr+
                RV8GKmQztIWJPLYJbxBnUpzHKlkqRwFr5HeS6iyd3QRtoUr+iGpuUpP1HlIO
                NhzcclBEyUEZtx3cwd0eWIRN3OthIdLP/QgPErv2G3NDy2lXAwAA
                """,
        """
                androidx/navigation/Navigator.class:
                H4sIAAAAAAAA/41QXWsTQRQ9M5vPbbTb1o9UrW1BiuahG4NPaSm0BiGwVrCS
                lzxNsku8zWYWdmZDfctv8R/4JPggwUd/lHh3E0SEgi/nnnPmzL0z9+evb98B
                vMKhwJ7SYZpQeONrNaeJspRo/3JFk7QKIdA97XWDW2K9yFjShTw5C67VXPmx
                0hP/3eg6GtsTAe9fr4qSQOWUNNkzAef5i0EDFVRdlFETKNmPZAT2b5u3ehb3
                3QqmiY1J+28jq0JlFXtyNnf4XyKHeg4QEFP2byhXbWbhS4HOcuG5sind5aIo
                0mOyXNSOmstFq1bjU9GSbdmRbeei/ONzRXql/GaHm/UEnv3PKkQ++uh8bGlO
                9tOfd/8VucgoDqP0eGr506+TMBLYDEhHl9lsFKUf1ChmZztIxioeqJRyvTbr
                VzThHlnK3L1KsnQcvaH8YPd9pi3NogEZ4uS51oktZpnSISTvd72RfN2Mj1n5
                hQbKra+of2Ei8YSxsjKxx9hYcxcbXB08LVIO9ov6CAdcu5xpcObOEE4fd/vY
                7MPDFlNs97GDe0MIg/t4METZYMPgoUHTYNeg+hsUN/EQjAIAAA==
                """
    )
