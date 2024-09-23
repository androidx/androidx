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

package androidx.navigation.compose.lint

import androidx.compose.lint.test.bytecodeStub

internal val NAV_BACK_STACK_ENTRY =
    bytecodeStub(
        filename = "NavBackStackEntry.kt",
        filepath = "androidx/navigation",
        checksum = 0xfef36ae,
        source =
            """
    package androidx.navigation

    public class NavBackStackEntry
""",
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
    Ks0rycxNFeIKSs1NzU1KLfIu4RLl4gZK66VWJOYW5KQKsYWkFpd4lygxaDEA
    ALrkMh5XAAAA
    """,
        """
    androidx/navigation/NavBackStackEntry.class:
    H4sIAAAAAAAA/41Ru0oDQRQ9d5JsdI2axKjx2YmPwk3EThGMKCysCipprCbZ
    RSePWdidhNjlW/wDK8FCgqUfJd5d7WxsDudxh/uYz6+3dwCH2CBsSe1HofJH
    jpZD9SCNCrVzJYcN2e7eGoZzbaKnPIhQ7MihdHpSPzjXrU7QNnlkCNax0sqc
    EDI7u80CcrBsZJEnZM2jignb3r86HBFKXjc0PaWdy8BIXxrJnugPMzwqJTCd
    AAjUZX+kElVj5tcJm5OxbYuqsEWR2WRcnYwPRI0auY9nSxRFUnVAydvKn8b7
    XcOznoV+QJj3lA6uBv1WEN3JVo+dshe2Za8pI5XoX9O+DQdRO7hQiVi5GWij
    +kFTxYrTU61Dk+4Yow7Bp/gdOrkMY5WVk2ogt/eKqRcmAiuMVmpmscpY+CnA
    NOw0X0txGevpvxFmOCvcI+Ni1sWci3kUmaLkooyFe1CMChY5j2HHWIphfQMn
    9fXa9AEAAA==
    """
    )

internal val NAV_CONTROLLER =
    bytecodeStub(
        filename = "NavController.kt",
        filepath = "androidx/navigation",
        checksum = 0xeb9f76f4,
        source =
            """
    package androidx.navigation

    public class NavController {
        public fun getBackStackEntry(route: String) = NavBackStackEntry()
    }
""",
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
    Ks0rycxNFeIKSs1NzU1KLfIu4RLl4gZK66VWJOYW5KQKsYWkFpd4lygxaDEA
    ALrkMh5XAAAA
    """,
        """
    androidx/navigation/NavController.class:
    H4sIAAAAAAAA/41SW08TQRT+ZtruwoJlQbkrioDclAXikzVGIZrU1GrEkBie
    pttJmXY7m+xOG3zjt/gL9AmjiSE++qOMZ8pGrGhkkz3X73wzc875/uPzVwD3
    ETDMC11PYlU/CrToqoYwKtZBVXR3Y22SOIpk4oIx+E3RFUEkdCN4WWvK0LjI
    MTgPlVbmEUNuZXV/GAU4HvJwGfLmUKUMC5X/spcYRhvS7IiwtWdIPKXEO4bS
    SuX8xD2TKN0orf6Lrb+4ZM+Nk0bQlKaWCKXTQGgdmx48DaqxqXaiiFCFJO4Y
    OYAiw1wrNpHSQbPbDpQ2MtEiCsranpuqMHXhM4yHhzJsZeWvRCLakoAMy79f
    9aw5pb9cnvozhqseRnGNYelSL3Ex4WHS9nPsIiH1rZLd+oU0oi6MoBhvd3M0
    WmbFoBVgYC2KHynrbZJV32J4cHo84fEp7nH/9NjjA7znWNMvTp0eb/NNtlP4
    9t7hPn9e9HMzfDO/7fgF0o5l2GaWd/nS8/D7Rr7RMrQhu3FdMoxUlJbVTrsm
    kzeiFkn71DgU0b5IlPWz4OzrjjaqLcu6q1JFoSfnE2VY/DP7azp9MG8v7iSh
    fKYs43RWs3+BD1vgtMX24/RKWmqSK+QFtpekC2snGPjYS6+SdHrBPNZIDp8B
    MAiP9CiGKJLrFe8QmpMuro+NfML4F0y+PcHUhz4Whyoty8QZMmOxVhHTlF/P
    cFdI36XfZZnDca8nl7FB+jFFZ4hq9gC5Mq6XcaOMOdwkE7fKmMftA7AUC1g8
    gJvCS7GUwkkxlOJOiiK5PwElc7kHIAQAAA==
    """
    )

internal val NAV_GRAPH_BUILDER =
    bytecodeStub(
        filename = "NavGraphBuilder.kt",
        filepath = "androidx/navigation",
        checksum = 0xced68271,
        source =
            """
    package androidx.navigation

    public class NavGraphBuilder
""",
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuaSTsxLKcrPTKnQy0ssy0xPLMnM
    z9NLzs8tyC9OFRL0SyxzL0osyHAqzcxJSS3yLhHiBAp55BeXeJdwiXJxAxXq
    pVYk5hbkpAqxhaSChJUYtBgARVljGmwAAAA=
    """,
        """
    androidx/navigation/NavGraphBuilder.class:
    H4sIAAAAAAAA/41Ru04CQRQ9d4BFVhTEF/hqjIlauGrsNCZioiFBTNTQUA3s
    BkaWWbM7EEu+xT+wMrEwxNKPMt5dqaxsTs7jzp07d76+3z8AnGCTsC21GwbK
    fXa0HKmuNCrQTkOOrkP51KsOle96YRZEKD7KkXR8qbvObfvR65gsUgTrTGll
    zgmp3b1mHhlYNtLIEtKmpyLCTv0f/U8JC/V+YHylnRvPSFcayZ4YjFI8JsWQ
    iwEE6rP/rGJ1yMw9ImxNxrYtysIWRWaTcXkyPhaHVM18vliiKOKqY4rPlv5c
    e9A3POdl4HqEQl1przEctL3wQbZ9dkr1oCP9pgxVrKemfR8Mw453pWJRuRtq
    owZeU0WK0wutA5O8L8IRBK9hOnK8FcYyKyfRQGb/DTOvTAQqjFZiprHGmP8t
    QA52kq8nuIqN5McIs5zlW0jVMFfDfA0FFJlioYYSFlugCEtY5jyCHWElgvUD
    nsoUQO4BAAA=
    """
    )

internal val NAV_GRAPH_COMPOSABLE =
    bytecodeStub(
        filename = "NavGraphBuilder.kt",
        filepath = "androidx/navigation/compose",
        checksum = 0xc3b35ff,
        source =
            """
    package androidx.navigation.compose

    import androidx.compose.runtime.Composable
    import androidx.navigation.NavBackStackEntry
    import androidx.navigation.NavGraphBuilder

    public fun NavGraphBuilder.composable(
        route: String, content: @Composable (NavBackStackEntry) -> Unit
    ) { }

    public fun NavGraphBuilder.navigation(route: String, builder: NavGraphBuilder.() -> Unit) { }
""",
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuaSTsxLKcrPTKnQy0ssy0xPLMnM
    z9NLzs8tyC9OFRL0SyxzL0osyHAqzcxJSS3yLhHiBAp55BeXeJdwiXJxAxXq
    pVYk5hbkpAqxhaSChJUYtBgARVljGmwAAAA=
    """,
        """
    androidx/navigation/compose/NavGraphBuilderKt.class:
    H4sIAAAAAAAA/71UTVMTQRB9s4EkBNRkI8qHIgrIl7ABvw6xqFIKrJQxqCAH
    OU02axiSzFI7kxTcuHqy/Av+A2+WB4vy6I+y7N0kJIFQcNFKpadn5vV7PdM9
    +/vPj58AHuEpwwKXBc8VhQNL8pooci1cadluZd9VjpXjtZce3999URXlguO9
    0hEwhvger3GrzGXR2sjvOTathhhi9SCeLzsMH2ay3XhP8aWzLaZN7QlZTGdL
    ri4Lae3VKtbHqrT9MGWtN7yl9Ow2w5d/RP5s4TzeF9wubWoya1J7hyc876XQ
    6ZUgp4ms6xWtPUfnPS6IlEvpal4XyLk6Vy2X0wzhZ3pXqJUo+hjG2pIRUjue
    5GUrI/1MlbBVBP0Mg/auY5ca8W+4xysOARmmZ7Kna9DluLPbA7iCqzEM4BpD
    r+dWtRNFgiFiuyQodRRJquakn9Nke/WmLnW/DOZZTYbxi0pIrdJiZfj836vZ
    yXu2lpF8fS+K0ZPrac840Yx57Whe4JrTkYxKLURPivmmzzdgYCXfMWjzQPhe
    irzCEsOn46P7seOjmBE3YsaQUXevBsOQ0fpHg3FkPn58NGKk2JyRMpbD8RD5
    Pcs3470jSbPHNFKRwLJU+NfXsBGNEryvCzrWQBsd6H4/oWXm52o2z9Rep+lL
    voeOjml+O7yq1KLiWKsnjUW4sabM2gE1oCK6pt7W4b4PME/VZ7GkGXpW3QK1
    5bWskE6uWsk73la9Uc2sa/PyNveEP28s9m2KouS66pE/+a6eRUbWhBK0ffKI
    nrceKDXkplv1bGdd+PHDjZjtekQbEEsw0OMXl8Zh9CKMEFI0e0tzv8LJOTP2
    HfF50yT7wLxO9lsAXiIb9q8ZMSyTP16HYxA3ArokErhJ+76XxBBFPAziIvSR
    prUQbUWDnmrZYfpdoH+ri/5Ah/7tLvqjbfoj5+sbeBxYC09o3KDVMbqROzsI
    ZTCewd0M7mEig0lMZXAf0ztgCjOY3cGAQq/CnMINhUTgJBXmFR4oLCgMK4wq
    LP4F8P5Vs6MGAAA=
    """
    )

internal val NAV_HOST =
    bytecodeStub(
        filename = "NavHost.kt",
        filepath = "androidx/navigation/compose",
        checksum = 0x6aac9b28,
        source =
            """
    package androidx.navigation.compose

    import androidx.navigation.NavGraphBuilder

    public fun NavHost(route: String, builder: NavGraphBuilder.() -> Unit) { }
""",
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuaSTsxLKcrPTKnQy0ssy0xPLMnM
    z9NLzs8tyC9OFRL0SyxzL0osyHAqzcxJSS3yLhHiBAp55BeXeJdwiXJxAxXq
    pVYk5hbkpAqxhaSChJUYtBgARVljGmwAAAA=
    """,
        """
    androidx/navigation/compose/NavHostKt.class:
    H4sIAAAAAAAA/5VSS2/TQBD+1nmHPhKXvgK0hbb0RXFawSkIqVQUrIaAaMml
    p42zpJs468peR+XW38I/4IY4oIojPwoxdpI2UpGAg7+Znf1m5vPM/vz17TuA
    J9hmWOWq6XuyeW4p3pMtrqWnLMfrnnmBsGq899oL9KHOgDEU2rzHLZerlvW2
    0RYORRMMmQGJ4el69ZpxpH2pWpVqx9OuVFa717U+hsqJygfWwcDbqWzUGdr/
    n/dsu/on3STllc/PTl+E0m0K/6rKByV15XncbLnq+S2rLXTD55JKcqU8zfvl
    a56uha5bYUj5XqhFFjmGhRElUmnhK+5atopkBtIJMrjFMO2cCqczSH/Hfd4V
    RGRYG/2z/swqN/91oz6GcUzkMYZJmmejLz6LIoN5k82w9LehMhSHlDdC8ybX
    nGJGt5egrbMIchGAgXUix6DLcxl5ZfKaOwyHlxel/OVF3igYfTMRmzlj+JXW
    CkQxymw3XTDIJnZnC8nSlJk0jXI6RlZO/ficNrKZqOQui7qZQ1WjUlf/aZO0
    h2Hyy3MtaPSeGlY5/nQmiJAfPMTHHXqLyX2vKRgmq1KJWthtCP+YN1wRafAc
    7ta5L6PzIJg7ki3FdeiTv/I+VFp2ha16MpB0fbXPveunQt2OvNB3xIGM8ucH
    OfV+xggROzCQRH/a80ghjQQe0mmP4gbZ8U0z/xWFLdMk/BItA2uEaaKPEa6T
    P9MnIoepuNA4irhN9xsxO4PNKGZQIBt3ycbhrRhX8YjsPkWnqffMCRI2Zm3M
    2aSlZOMO7tq4h4UTsACLWDpBNkAqwP0AuRiLAR4EWA6w8hvNMdYWNAQAAA==
    """
    )

internal val COMPOSABLE_NAV_HOST =
    androidx.navigation.lint.common.bytecodeStub(
        "StartDestinationLint.kt",
        "androidx/navigation",
        0xa9664947,
        """
         package androidx.navigation

import androidx.compose.runtime.Composable

// NavHost
public open class NavHostController

@Composable
public fun NavHost(
    navController: NavHostController,
    startDestination: Any,
) {}
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg8uQSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5cAmWLBJYlFJS6pxSWZeWDVPpl5
                QHEucS5euFElQFkhtpBUkAYuUS7u5PxcvdSKxNyCnFSYsBKDFgMAc6sML5MA
                AAA=
                """,
        """
                androidx/navigation/NavHostController.class:
                H4sIAAAAAAAA/41Ry04bMRQ910kmYUhJeIUARaKLCuiiExC7IiSgqogUQCoo
                G1ZOxgpOJrY0diKW+Zb+QVdILKqIJR9V9c6UD2BzdB7X9tH169/nPwCOsUP4
                LE2cWh0/RkZO9UB6bU10LaeX1vkLa3xqk0SlZRChPpRTGSXSDKKb3lD1fRkF
                QnCijfanhML+QbeKEoIQRZQJRf+gHWGv864XvhGWOyPrE22iK+VlLL1kT4yn
                Ba5KGSxkAAKN2H/UmWoxiw8Ju/NZGIqmCEWd2XxWaTTnsyPRovPSy69A1EU2
                d0TZ6Y1bL1P/XTmvTd6lo43/OvJc+MLGilBjQ11Pxj2V3slews5Kx/Zl0pWp
                zvSbGd7aSdpXP3QmNn9OjNdj1dVOc3pmjPX55Q6fIHgfb82z9TA2WUW5Bkpf
                nlD5zURgkzHIzTK2GKv/B7CAMM+3c9zAx/zzCIucVe9RaONDG0tt1FBniuU2
                VrB6D3JYwzrnDqFDwyH4B46/yoL5AQAA
                """,
        """
                androidx/navigation/StartDestinationLintKt.class:
                H4sIAAAAAAAA/41STVMTQRB9s4F8SSQJAgEUPwD5sMoFiht6oGJZbBmDZSwu
                nCabqTjJZsbamWxxzF/yZnmwOPujLHs2KaOBg5d+3W96Xk93z89f338AOMEB
                wwFXnVjLzrWveCK73Eqt/JblsX0jjJUqJRpS2Xc2B8ZQ7vGE+xFXXf+i3RMh
                sRmGXJMn59pYhld7jbsUJ+d1rWyso0jEp41ZodP9S4ad6e1QD75oI/x4qKwc
                CL+exrwdiVOGrYaOu35P2HbMpTI+V0rbtJTxm9o2h1FEWSV6wbRkHgWGzb62
                kVR+Lxn41JWIFY/8gFJIRYYmh3sMy+FnEfYnMh94zAeCEhl2924/+i+m5US6
                1MYCSrhfxAIWaV5mZpZ5VEjpP4fEUL1dkqHSmHTxXlje4ZYT5w2SDC2VOVNw
                Bgys7xyPDq+l8w7J6xwxnNyMqsWbUdEreynUxlC+Ga3XnPEO2XG27BFmCOcI
                589z7u4xc7Krd/2Pl33a/lxddwTDIhGiORy0RfzJLcy1oUMeXfJYunhCbnwc
                rzZQiTSSqLPpFhm2Z0//bOKftGJLD+NQvJVOcW1y5/KWHo7gYQ7juaxhHllk
                sEXRGfEeYemgWvyG8otqlexXNzZsk81SutvnDvkr40QUsJQKlVDBAzp/PsnL
                Ee463qMgn1bKp0J7qX2GfcLXxC5T/ZUrZAKsBqgF9J71ABt4GOARNq/ADB7j
                yRWyBvMGTw0KBksGFfJ/A2uvkva7AwAA
                """
    )

internal val COMPOSE_NAVIGATOR_DESTINATION_BUILDER =
    androidx.navigation.lint.common.bytecodeStub(
        "ComposeNavigatorDestinationBuilder.kt",
        "androidx/navigation/compose",
        0xcdeb9868,
        """
package androidx.navigation.compose

import kotlin.reflect.KClass
import androidx.navigation.*

public abstract class Navigator<D : NavDestination>

public open class ComposeNavigator : Navigator<ComposeNavigator.Destination>() {
    public open class Destination: NavDestination()
}

public class ComposeNavigatorDestinationBuilder :
    NavDestinationBuilder<ComposeNavigator.Destination> {
        public constructor(route: KClass<out Any>)
    }

public inline fun <reified T : Any> NavGraphBuilder.composable() {}

public inline fun <reified T : Any> NavGraphBuilder.navigation() {}
            """
            .trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/22MwQrCQAxEI4LQFBFWPOlJevLQP/CgFRQKPfkDi13ahW1S
                trHUv3drxZOBhDBvZgBgDgCzsBF8B4+41lR6tuWQku5tpcUyKTzRS2pLVS5q
                Veg+YxLPzhkfhCgIN+4kF8xw+yeePrhpuTMqyaanmBD7i+nE0sd0flpXjn24
                w+WvRIJBxfdwr163daAbjENdagbdtM6oxchy2cMB3j+iTMHRAAAA
                """,
        """
                androidx/navigation/compose/ComposeNavigator＄Destination.class:
                H4sIAAAAAAAA/51STU8UQRB91cPOwrDyKbgIgmsgAWMYJByMGhJZYzLJykHJ
                Xjj17nS0s7PdZLqXcNzf4j/wZOKBbDjyo4g1sxzAcNFDv6736vVHVff1ze9L
                AAdoEN5Ik+ZWpxexkef6m/Tamrhr+2fWqbg5no/HGZtvflTOa1OaqiDCi4dW
                s/2eLyCE77XR/pAQbO+0a6ggjDCBKmHCf9eO8Lb1v9d4R5hv9azPtIk/Ky9T
                6SVron8ecIlUwFQBIFCP9QtdsD2O0teEjdEwikRdlGM0nFyuj4b7Yo+OKlc/
                QjEnCts+YfPB692vkw999S9FVPGEMH1nA8LW35472aOBzlKV7/Y8N61pU0WY
                bWmjjgf9jspPZCdjZaFluzJry1wX/FasJcaovJlJ5xS3OvpqB3lXfdJFbuXL
                wHjdV23tNJs/GGN9eZxDA4Kf6LZzxYsxrjKLSw5UXv7C5E8OBNYYw1KcwjPG
                2tjALOI5wDpjxJpAHSusbpSrnuJ5+Qm5C+ytnSJI8CjBTIJZzHGI+QQLWDwF
                OTzGEucdIodlh/AP0ebo18ECAAA=
                """,
        """
                androidx/navigation/compose/ComposeNavigator.class:
                H4sIAAAAAAAA/51STW/TQBB9azux4wSahlISvqFFfIjWoeKA0qoSDUKyFHqg
                KJectvGqXcXZRd511WN/C/+AExIHVHHkRyHGTtVWPVSlB7+ZefNmZjXjP39/
                /gLwFssMr7lKMi2Tw0jxA7nHrdQqGuvpV21E1J/Z7VlGZz4Yw3hwWcmpduNS
                2cXOyx+EsVKVovXNdYZnVxriw2Oobkgl7SaD++LlsIEq/BAVBAye3ZeGYfW/
                XkKz5wcTbVOpok/C8oRbTpwzPXBpZ6yAWgFgYBPiD2URdclL3jCsHB81Q6ft
                nH2BEyy2j4/WgpbXcrpOl/WYt1X5/a3qNN2iaI3h+dU2Sq/oXX+pDPVzIcO7
                63bycZfOc1FxTrCVyzQR2erE0gn6OhEMcwOpxHY+3RXZF76bEtMa6DFPhzyT
                RXxCNmKlRNZPuTGCDlfbkXvUMs8oFe7oPBuLj7LQdT7nysqpGEojqfC9UtqW
                ow2ewKHjn9yn+BcIH1AUlTFQefUDte/kOHhIWC3JAI8IGzMBQtTJenhMGBJ3
                j7Qd1Kh1UeXiaWnvY4lsj/INqrkxghvjZoy5GE3Mk4tWjFtYGIEZ3MbiCBWD
                usEdg7aBb9D5B1SgkZCGAwAA
                """,
        """
                androidx/navigation/compose/ComposeNavigatorDestinationBuilder.class:
                H4sIAAAAAAAA/51TTVMTQRB9swnZsCCEKARQ8QPQQJANyMESChHUqpQxWmJx
                4TRJxjDJZtaamaQ48lv8B560PFiUR3+UZW8SMVVACRx2+nX369e9s72/fn//
                AWANawybXFV1KKuHvuJtWeNWhsqvhM1PoRH+TteWuplQvxDGStXhbLdkUBXa
                BWOoFc8SoarT/I0zqef1m+sTWN9cZ1i4cCMXcYbEhlTSbjJMZ4uN0AZS+Vp8
                DETF+q93Am7M+sIeg39OciNXrPM29wOuav7bcp0yNERUMVsMdc2vC1vWXCrj
                c6VC2+lu/FJoS60goGEHdNiyIgmPYabXoN5u+lJZoRUP/IKymqplxbgYZhiv
                HIhKo1f+jmveFERkeJg9PUZfZDcSqdFYwxjBqIdrSDHEspGfQNrDAK4zxO2B
                NAxbl7r905dKL5U55yIZxv5m3gjLq9xyijnNdow2jUXHYHSAgTUofigjL0+o
                usLw8vho0nMmnZPn+KgLUwQod3y0mkzH007eybPtTHIiPZKKTXvpRJJRbCAf
                //k54aQSkdgqQ+7i20gTPr36QrLobZ5ctdzFA4aly1S7yDIM9UkwzP//my03
                LH3/nbAqGEaLUolSq1kW+gMvBxRJF8MKD/a4lpHfCw7uyhpptDThufctZWVT
                FFRbGknpk818/m/pGYYLSgndWQVBrrcbtnRFvJKR3FRPYq8r0FeHFTi0oL29
                oH11EcMSec8o7pAdyqWHvmFs8StufCHXwSM6Ex3qCJYJT3RpGCeEDvKQobzf
                YSeRJ+tG2oME4tQPRKB/CgtYpFiXFsNqx+bwmOwW5Sdpqql9xAqYLuBmAbdw
                myBmCriDu/tgBvdwfx+uwYTBrMGcgWeQMZg3cP8AB4jQd18FAAA=
                """,
        """
                androidx/navigation/compose/ComposeNavigatorDestinationBuilderKt.class:
                H4sIAAAAAAAA/42STU8TQRjH/7OFdruiLS0gRUXFIgWNC8aTIFExysZajW16
                4TRsx3bodpbsThuOnPw+3owH03j0Qxmf2VYtvkQO87zn97zsfv326TOAB7jL
                8JirVhTK1omr+EC2uZahcv2wdxzGwt0b6dooE0bPRKylSmqe9mXQEtFLnQFj
                yB/xAXcDrtru68Mj4VM0xeCMOPwwEAyVSvVvrYj9IuLHnTFve73JsLvTeFj9
                nbi9e35Aekd3ZLxrw2ZY7oY6kMo9GvRcqbSIFA9cT+lIqlj6cQYOw7zfEX63
                FupaPwje8Ij3BBUyrFX+HGMiUjeQNnWcwQwuOriASwy5siy/K09uzjw6UNlM
                dCa8eq516Iq/sj/ok5ExeTI0Wx3v/Epo3uKaE8XqDVL0zZkRWSNAg3WNYVHy
                RBprk6zWFkNjeFpwhqeOlbdGyohFYy+VyF6yNtmKYw9P89Yi27A2U/Sm9r+8
                tyk7/c+kYd9nSdsGLf//X+teVzNM7YUtulWuKpWo9XuHImqMrleohj4PmjyS
                xh8Hs3XZJkY/Ituph/3IF8+lSZTe9pWWPdGUsaTKJ0qFOukVYwsWpsw9YOVL
                mEaa/NvkPSJtmWNtFLIfkUvtfDAlWCOZpiVszKJC9gLFbPLz5DNTjgKKpNeT
                6gwh586PLJ5Bzv9ELkwi6W0k1iruJFCGyzT34gFSHkoeljxcwVUP17Ds4Tpu
                HIDFuImVA6RjTMe4FaOcyGKMue/5/YwICwQAAA==
                """,
        """
                androidx/navigation/compose/Navigator.class:
                H4sIAAAAAAAA/41Qy27TQBQ9M06cxA3ULa+UdyXKIwscIlZpVYk2QopkikRR
                NllN4lGYxplBnnHUZb6FP2CFxAJFLPkoxLVTIQRdsLn3nDNn7uvHz6/fALzE
                LsOe0ElmVHIeabFQU+GU0dHEzD8aK6OTtWSyGhhD76Dfiy+zk60vrVO6pPuH
                8ZlYiCgVehq9HZ/JidtnCP/Waqgw+AdKK3fI4D19NmzCRy1AFXWGivugLMOT
                S/v9Mx7V34pnxqVKR2+kE4lwgjQ+X3i0JytCowhgYDPSz1XBOoSSFwzd1TIM
                eIsHq2WZeEhgtaw/bq2W7XqdXlmbd3iXd7yj6vdPPg8rxc8uFeszPPqfk7Ci
                9d7xeu7fY//hOMpVmsjs+czR7scmkQybsdLyJJ+PZfZejFNStmMzEelQZKrg
                F2LjVE2pRp4RDk5Nnk3ka1U87LzLtVNzOVRWkfOV1saVvWxlF5zOfHGQ4uoU
                7xCLSg5U21/Q+EyA4y5FvxR93KPYXBsQYIOyh/uly8ODMt/GQ8o98jTJc2UE
                b4CrA2wOEGKLILYHuIbrIzCLG7g5QtViw+KWRctix6L2C0Mu1/6bAgAA
                """
    )

internal val DIALOG_NAVIGATOR_DESTINATION_BUILDER =
    androidx.navigation.lint.common.bytecodeStub(
        "DialogNavigatorDestinationBuilder.kt",
        "androidx/navigation/compose",
        0xe5b79ce2,
        """
package androidx.navigation.compose

import kotlin.reflect.KClass
import androidx.navigation.*

public open class DialogNavigator : Navigator<DialogNavigator.Destination>() {
    public open class Destination: NavDestination()
}

public class DialogNavigatorDestinationBuilder :
    NavDestinationBuilder<DialogNavigator.Destination> {
        public constructor(route: KClass<out Any>)
    }

public inline fun <reified T : Any> NavGraphBuilder.dialog() {}
            """
            .trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/31MTQuCQBCd6OQUBNvRW3Xq4D/wUHoIBA/9gyUXXVhnZHcU
                +/ct2jF68ODxvgBgCwCbyAS+wByPmhrPtpkz0pNttVgmhTd6S2eprUQdaj0V
                TOLZOeOjkUTjwUEqwQ7TH/Psxf3AwahLsYp6jdiXJoilpXQfrWuWv3NpteP2
                bwlT3MXXzMy6H5xR+yePYgqnQ6jkBFf4ABWxV1veAAAA
                """,
        """
                androidx/navigation/compose/DialogNavigator＄Destination.class:
                H4sIAAAAAAAA/51STU8UQRB91cPOwrjKNy5+IYaDaMIgITFGYyIQk0lWDmr2
                wqnZ6WBlZ7vJdC/huL+Ff+DJxAPZcPRHGWsGDmC46KFfV716lfro/vX75zmA
                bawSXmubl47z09TqEz7SgZ1Ne25w7LxJ91gX7mj/MuDKtT3jA9ta0wQRnt2W
                LPIbuogQv2PL4T0her7ebaGBOMEEmoSJ8I094U3nP7t4S5jt9F0o2KafTNC5
                Dlo4NTiJZECqYKoCEKgv/ClX3qZY+SvCyniUJKqt6jMeTS61x6MttUk7jYuz
                WM2oSrZFWLu1u5tjStGX/zBDE/cJd67lS5W/JNeCO0MuclNu9INsbNflhjDd
                YWv2h4NDU37Vh4Uwcx3X00VXl1z5V2Qrs9aUu4X23sieky9uWPbMR65iy5+H
                NvDAdNmziD9Y60JdzmMVSt7nam/Vcwk+FC+tfaDx4gcmv4uh8EgwrskmHgu2
                LgWYQiJ3hCeCiXAKbSwLu1JnPcDT+gPKEkTbOkCU4W6GexmmMSMmZjPMYf4A
                5LGARYl7JB5LHvEfdL2Neb0CAAA=
                """,
        """
                androidx/navigation/compose/DialogNavigator.class:
                H4sIAAAAAAAA/51STW/TQBB9azt24gSaBigJ3x9BtCBwqCoh2qoSbYVkKe2B
                olxy2sSrsIqzi7zrqsf+Fv4BJyQOqOLIj0IduxVUPVSlB7+ZefNmZjXj339+
                /ASwgi7DS66STMvkIFJ8X064lVpFYz37oo2ItiVP9WT3JKGzAIxh1L+o4q92
                /ULZucbdbWGsVKVmbWON4dmlZgTwGPx1qaTdYHAXlwYN+AhCVFBl8OxnaRhe
                /c9DaPR8f6ptKlW0IyxPuOXEObN9lxbGCqgVAAY2Jf5AFlGPvOQNjTo6bIZO
                2/n3VZ3qQvvocLna8lpOz+mxVeZtVn599Z2mWxQtMzy/3D7pFe+uvFKG+pmQ
                4e0VGwW4w9A9JziT38xlmojs9dTS+rd0Ihjm+lKJ3Xw2EtknPkqJafX1mKcD
                nskiPiUbsVIi20q5MYKOVtuTE2qZZ5QK93SejcUHWeg6H3Nl5UwMpJFU+F4p
                bcvRBo/h0OFPj1P8B4T3KYrKGKi8+I7aN3IcPCD0S9LHQ8LGiQAh6mQ9PCIM
                ibtL2g5q1LqocvGktPfwlOwq5RtUc20IN8b1GHMxmpgnF60YN3BzCGZwCwtD
                VAzqBrcN2gaBQecYqfVyAIADAAA=
                """,
        """
                androidx/navigation/compose/DialogNavigatorDestinationBuilder.class:
                H4sIAAAAAAAA/51TXU8TQRQ9sy3dslQoq1DAb6laqLIFSYwBQQFNGms1Ynjh
                adoOZdrtrNmZNjzyW/wHPml8MMRHf5TxbluxCRCBh5059+vcOzNnf/3+/gPA
                MpYZnnNVCwNZO/AU78g6NzJQXjVofQq08LYk94N6uRcIwi2hjVTdlI229Gsi
                tMEY9kqncVDVyfzVU1PPaJcdqF9ZW2GYO3cfG3GGxKpU0qwxzORKzcD4Unmh
                2PNF1XhvNn2u9crcDoN3RnA1X2rwDvd8rureu0qDIjREVDFbCsK61xCmEnKp
                tMeVCky3u/bKgSm3fZ+GHQqDthFJOAy3+g0anZYnlRGh4r5XVCakalnVNlIM
                E9V9UW32y9/zkLcEJTI8zJ0cY8CzHZHUaawURjHm4ArSDLFcZCfgOhjCVYa4
                2ZeaYf0il3/yTulMmTPukWH8b+StMLzGDSef1erESGYsWoajBQysSf4DGVkF
                QrVFhldHh1OONWUdf0eHPZgmQLGjw6WkG3etglVgG5nkpDuajs04biLJyDdU
                iP/8nLDSiYhsiSF/fi3ShM8uLUcWHebpJattPKBJL1BsI8cwMsDAkP3vey00
                DT39ZlATDGMlqUS53aqI8COv+ORxS0GV+zs8lJHddw5vyzpxtEPC2Q9tZWRL
                FFVHaknhY1G+/Kd3hlRRKRF2ZSDIdLaDdlgVr2VEN92n2OkRDNRhERZps68J
                kqqNGB6RtU5+i/aRvDvyDePzX3HtC5kWHtOa6KamsEB4speGCULoIgcZinvd
                7CQKtNsR9zCBOPUDJdDvhDnMk6+XFsNSd8/jCe0vKD5FU03vIlbETBHXi7iB
                mwRxq4jbuLMLpnEX93Zha0xqzGpkNRyNjMZ9DfsPJXcGl1gFAAA=
                """,
        """
                androidx/navigation/compose/DialogNavigatorDestinationBuilderKt.class:
                H4sIAAAAAAAA/41Rz28SQRT+ZqH8ahVKbQXUqpVa2oNLTU8tYtRG3YhohHDh
                NOyOMLDMNrsD6bEn/x9vxoNpPPpHGd9s0Rg1scnOe9/75pvvzdv59v3zFwAH
                2GV4zJUXBtI7tRWfyyHXMlC2G0xPgkjYx5L7wbB9sRGExyLSUsWSpzPpeyJ8
                pdNgDIUxn3Pb52povxmMhUtsgiHlxccZarXWv7qQ74uQn4wWXke7PYZmo3vY
                +tPtqHl5g1RDj2TUzCDDsDkJtC+VPZ5Pbam0CBX3bUfpUKpIulEaOYZ1dyTc
                STvQ7Znvv+UhnwoSMuzU/r7Gb0zHmAyp4wpWcCWHZVxlWK7K6vvqz6mZw7BS
                Nbf5RW1fagyG1dbi5q+F5h7XnDhrOk/QozETsiaAWkwMsGjzVBpUJ+TtMxyc
                nxVy52c5q2BdJBNKVqVMoGLV2VYuQwqrxPaseoJW8uXXDxlz9iGLbbsM1f++
                /YOJZkg+CzzBkG9JJdqz6UCEXT7wiSm2Apf7PR5KUy/IbEcOyWMWEs51glno
                iufSbJTfzZSWU9GTkSTlE6UCHfeKsA8LScTzFspYQorqe1Q9omyZf7FXzH5C
                PtH4aH4FqhRTNEMGeWwT3iAuQ3UBqxRJjiLWKN+P1WlaOzHaQo3yIWmuUZP1
                PhIONhxcd1BC2UEFNxzcxK0+WIRN3O5jKTLfnQh347j2A9YTBm1VAwAA
                """
    )
