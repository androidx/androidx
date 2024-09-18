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
