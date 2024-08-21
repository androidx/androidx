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
import androidx.navigation.lint.common.kotlinAndBytecodeStub

internal val NAV_CONTROLLER =
    bytecodeStub(
        "NavController.kt",
        "androidx/navigation",
        0x40e8c1a8,
        """
package androidx.navigation

import kotlin.reflect.KClass

open class NavController {

    fun navigate(resId: Int) {}

    fun navigate(route: String) {}

    fun <T : Any> navigate(route: T) {}
}

inline fun NavController.createGraph(
    startDestination: Any,
    route: KClass<*>? = null,
): NavGraph { return NavGraph() }
""",
        """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJcnEn5+fqpVYk5hbkpAqx
haSChJUYtBgAOMX57WIAAAA=
""",
        """
androidx/navigation/NavController.class:
H4sIAAAAAAAA/41SW28SQRT+ZoFl2dIW0NYWW7UXLW21Sxt9kaaJNjFug2gs
4aVPA2zowDKb7A6kj/wW/4FPGh9M46M/ynhmIb1qdJM99++bc2bOz1/fvgN4
jj2GFS7bYSDaZ47kQ9HhSgTSqfHhYSBVGPi+F6bBGHJdPuSOz2XHed/sei2V
RoLB3BdSqAOGRGmzkUUKpo0k0gxJdSoihrXqP9krDNYk5xGu5G42GFKhF7lt
BuYyzJWql2cfq1DITkXXrFWDsON0PdUMuZCRw6UMVHxA5NQCVRv4fkUzBQPl
WcgzPOgFyhfS6Q77jpDKCyX3HVdqxki0ojTu0GGtU6/Vm8A/8JD3PSpk2Lja
xPgCKn9qK4s5zNu4i3sMhdsFN6aZEOlplvbrL29nDkr1epwu3M4x5KuTid55
ire54hQz+sMEPS3TIqMF6BZ7FD8T2iuT1d5lCM9HS7axYNhG7nxkG5Y26LeS
pC36Z6z5hfPRnlFmr1M/PpmUPFrOJYpGOblqWeejXGqLUntmziwab8cF6aOZ
cQFFLdKZKz5VlW19Mu2b7qdO+3RtCXZ6it7+MGjTCsxWhfRqg37TC+u86Xt6
+KDF/QYPhfYnwfWPA6lE33PlUESCQhev9epyERgyx6IjuRqEBLGPg0HY8t4I
jV+c4Btj9BUQVmDQFusvSd3SUpPcJs/RvZNObX2B9ZkMA09JmnHQxDOS2XEB
MrBJ5zEVRzT4RVxP498EWjFwfpycALU1jRmSmmKWcpqiQlpXpbcLha9YuE5k
EvCSKH1BlCaKRcrvaFv3n5s0VkTif1izf2VdorwTV9+/zm6gHMst7JKuUnSZ
ruTBCRIuHrp45NINr5KJNRfreHwCFuEJNk4wFcGOUIpgRtomYzNCPkIxwnTs
ln4DameR7LoEAAA=
""",
        """
androidx/navigation/NavControllerKt.class:
H4sIAAAAAAAA/61TbU8TQRB+9o62RxUoRSqv9YWqgC9XKr6WEA0GvVjQiCEx
JJqlXcrS6x252zYmJuon/4P/wm8aTQzxoz/KOHseCCLiBz7s7Mzs7DPPzM5+
//H5K4BpzDCMca8W+LL20vZ4W9a5kr5nL/L2nO+pwHddETxUKTCGzAZvc9vl
Xt1+tLohquQ1GY5VA8GVuB/wzXUGd7xyKFy58idQudLwlSs9OxBrLtn2wzmX
h2F54iCwKFuZwT/KdDOTs4dnHKv4Qd3eEGo14NILbe55voqiQnvRV4st16Wo
wr+iKISvuoLCkjNqXYazFtIM+ZjTRrtpS0+JwOOu7VARdF9WwxSOM/RX10W1
Ead5zAPeFBTIcGH8LzX+9ixpkHp5Yvk4utGTRhcy9Jqh4oG6J0IlvYiZhSzD
yL/KT+GE5iw9qWYZzHENmMPJNPoxQIAFWVgr7JkG5jD0FnSNe/1j//FqDNn9
RTEkAr+lBMPJA0aGoW9XqkJNrPGWqxjeHOlgOvsjD52ckX2NeNEqTe8Q7N1O
tSAUr3HF6YrRbJv0TZkWnVqAetrQikGHL6XWiqTVphjmt97m0ltv08aAES2t
Zkj8cg2dI33IKLJJo2iUkhmT9I5St2VkEkNW1rDMAVZMPvj2ztJoJZrGw6oh
Jpk9zbvSoCI65vwaPU5PRXpisdVcFcFTPer6Lf0qd5d5ILUdOzuXZJ1mrxWQ
Pvyk5SnZFI7XlqGk47u/vwz9pz9Pd4Z/T1jXkuLVxgLfjBOkl/xWUBXzUhuD
McbyPnxMwUCHbi/tg0ggSdZVsp7H/txk9tgn9F7M9pE0Z7+g/9lHDH6I4qdJ
Jqkb3cjiGumTdKMbFoYwDP0+OYxgNMLOUUSeIrV2Cqfp7vUIIYUbMYZF+01a
fWZsbMtOINOJMzhLuib2iq4laM+PJl6/R4ItHEjQxK1IMitimo3q6aFsvQTd
E3HaZp3bxTqPsZh1fod1PmZt4HbEu4Qy7XforEBkzq3AdHDewQUH45hwCPKi
g0u4vAIW4grsFaRCJEIUQ4yGyFLXQ5wKcfonI/JCLIwGAAA=
"""
    )

internal val NAV_DESTINATION =
    bytecodeStub(
        "NavDestination.kt",
        "androidx/navigation",
        0x89e4229a,
        """
package androidx.navigation

open class NavDestination
""",
        """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJcnEn5+fqpVYk5hbkpAqx
haSChJUYtBgAOMX57WIAAAA=
""",
        """
androidx/navigation/NavDestination.class:
H4sIAAAAAAAA/4VRO08CQRD+ZoEDTpSHiuAjUWOhFh4SO42Jj5iQICZqaKwW
7oLLYy/hFkLJb/EfWJlYGGLpjzLOnTRWNl++x+zMZPbr+/0DwAm2CLtSu0Nf
uRNHy7HqSKN87TTk+NoLjNKRTIIIua4cS6cvdce5a3W9tkkiRrDOlFbmnBDb
P2hmkIBlI44kIW6eVUDYq//f/pSQr/d801faufWMdKWR7InBOMZLUgjpEECg
HvsTFaoKM/eYsD2b2rYoCVvkmM2mqWJpNq2KCl0mPl8skRNhXZXC1/m/c496
hve88l2PkK0r7TVGg5Y3fJStPjuFut+W/aYcqlDPTfvBHw3b3o0KRfl+pI0a
eE0VKE4vtPZN1DjADgSfYb5zeBXGEisn0kDi8A2pVyYCZUYrMuNYZ8z8FiAN
O8o3IlzDZvRhhAXOMk+I1bBYw1INWeSYIl9DActPoAArWOU8gB2gGMD6AQ9d
W4PtAQAA
"""
    )

internal val NAV_GRAPH =
    bytecodeStub(
        "NavGraph.kt",
        "androidx/navigation",
        0x54a108f5,
        """
package androidx.navigation

open class NavGraph: NavDestination() {
    fun <T : Any> setStartDestination(startDestRoute: T) {}
}
""",
        """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJcnEn5+fqpVYk5hbkpAqx
haSChJUYtBgAOMX57WIAAAA=
""",
        """
androidx/navigation/NavGraph.class:
H4sIAAAAAAAA/31SXU8TQRQ9s2232wVpAUGo4AegFFC2Ep8sIfEjaE2tSpu+
8DRtN2X6MWt2pg2P/S3+A580PpjGR3+U8c62aA3iJnPvnXPvuXNm7/z4+fUb
gMfYZ1jjshkGonnuST4QLa5FIL0yH7wM+YezJBjDxhUVL3ylhYy2ScQY7EMh
hT5iiOV2arNIwHYRR5Ihrs+EYrhV+t9RBYZF5euK5qGe6sywlCu1+YB7XS5b
3tt622/owk6NhB9Wn1zOHOWq1Si9WQrCltf2dT3kQiqPSxnoqKXyyoEu97td
OnJOXZx3EvS17yBNOjuB7grptQc9T0jth5J3vaLUIbURDZXEPIlqnPmNzqTP
Ox7ynk+FDNv/EDuFVEyTVsH8nkVcd7GAJYaFyxSG+dJExRtf8ybXnDCrN4jR
2JgxKWPAwDqEnwuzy1PUfMTwfjTMutaKNV4OrYzljobkjHEsZ3llNDyw8uxZ
4vtHm5Kv1zOxrJWPbzjOaJhJ7Fp5+8DOJLPWq3GBYxofMGxdNcCpeZFMo6rK
MHMx2f2OpjfwPGj6DOmSkH6536v7YZXXu765fdDg3RoPhdlPwFRFtKhfP6R4
66Qvtej5RTkQSlD69+9++mekDG4l6IcN/1gY/uqEUxszpgpxFxa9SvNZJJQe
Kdlt2nlGNvnE7mc4n6J0jqwdgXHskJ0dFyAFl/w8ZgiJReQCVVvkk3sLmS9Y
/ptuE8XQl8clE7qJ0rhB+d2o+hr2DGZEzEXAg8jex0Pyx4SuUJvVU8SKyBZx
s4g1rFOIW0Xcxp1TMHO1jVOkFFyFTQVbYUZhS+FeZNMKs78AJaVXJ/gDAAA=
"""
    )

internal val NAV_HOST =
    bytecodeStub(
        "NavHost.kt",
        "androidx/navigation",
        0x5ce5aeda,
        """
package androidx.navigation

import kotlin.reflect.KClass

interface NavHost

inline fun NavHost.createGraph(
    startDestination: Any,
    route: KClass<*>? = null,
): NavGraph { return NavGraph() }
""",
        """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJcnEn5+fqpVYk5hbkpAqx
haSChJUYtBgAOMX57WIAAAA=
""",
        """
androidx/navigation/NavHost.class:
H4sIAAAAAAAA/32OzUoDMRSFz81of8a/qVqoiK9g2uLOlZviQFVQcDOrtBNL
OtMEmnToss/lQrr2oaR3qmsTOPecc+FLvn8+vwDcoUu4VjZfOpOvpVWVmalg
nJXPqnp0PjRBhGSuKiVLZWfyZTLXU24jQmdcuFAaK590ULkK6p4gFlXEWKql
XQsIVHC/NnXqs8sHhO5204pFT8QiYffR226Gok/1cki4Gf/zH36DkfFfui0C
hze3Wk71yJSacPW6ssEs9LvxZlLqB2td2AN8g/E4wO8RuNjrOS55Dhh5yLeR
IUrRTNFK0UbMFkcpjnGSgTxOcZZBeCQenR1YLcIOPwEAAA==
""",
        """
androidx/navigation/NavHostKt.class:
H4sIAAAAAAAA/61TbU8TQRB+9o62R1EsRSogrW9VAV+uVnwtIRqNerGgEUNi
SDRLu5Sl1ztzu200JsZP/gf/hd80mhjjR3+UcfY8QUTgix92dmZ25plndme/
//j0BcAMZhmKPGhGoWy+cAPeky2uZRi4C7x3L1T6vs6AMeTWeY+7Pg9a7oOV
ddEgr80w0IgE1+JuxJ+vMaxO1ncBqtX/hqjV26H2ZeBGYtUn271/y+dK1aZ2
gonr1Bjk/yk0Oz23d60T9TBquetCr0RcBsrlQRDqOEq5C6Fe6Po+RZV3i6IQ
vuILCkvP6jWp5hxkGUoJp/Vex5WBFlHAfdcLdET5sqEy2Mcw0lgTjXZS5iGP
eEdQIMPpyX/0uOlZNCCt2tTSPgziQBb7kaMXVJpH+rZQWgYxMwd5hond2s/g
oOEsA6nnGOxJA1jAoSxGMEqAZVleLW+ZAOYxDJVNj1v9xV3fiyG/vR2GVBR2
tWA4tMOYMAz/UaTcFKu862uGl/9pDL3tkXtOy8S25p91qzMb1IZ+l5oXmje5
5pRidXo2fURmRL8RoHtsG8WiwxfSaBXSmhcY7nx9U8h+fZO1Rq14GTVH4pdr
/CTp41aFTVsVq5rO2aT3VQcdK5cad/KWY4+ySvret7eOQavSBO7VDTHJJtd2
vk30+26FTXqQA3UZiIVuZ0VEj81gm/cLG9xf4pE0duLsX5QtmrRuRPrhR91A
y47wgp5Uko5vbn4Q+j1/n26M+paw/YuaN9rz/HlSILsYdqOGuCONMZZgLG3D
xwVY6DMXS/sYUkiTdZGsp4m/MJ0f+IihM/lhkvbcZ4w8+YCx93H8DMk03cMg
BnCJ9GnKGISDcRyGeZkCJlCMsQvIo0SRRjuCo5R7OUbI4EqC4dB+ldawnRi/
ZT+Q68cxHCfdEHtFaSnaS8XU63dIsfkdCdq4FkvmxEzzcT8OVcuSdIjJJuvC
H6xLOJGwLm2wLiWsLVyPeVdRo/0GnZWJzMll2B5OeTjtYRJTHkGe8XAW55bB
FM7DXUZGIaVQUSgq5OnWFY4oHP0JmQ7w4mgGAAA=
"""
    )

internal val TEST_NAV_HOST =
    bytecodeStub(
        "TestNavHost.kt",
        "androidx/navigation",
        0x2f602e26,
        """
package androidx.navigation

class TestNavHost: NavHost
""",
        """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJcnEn5+fqpVYk5hbkpAqx
haSChJUYtBgAOMX57WIAAAA=
""",
        """
androidx/navigation/TestNavHost.class:
H4sIAAAAAAAA/4VRTUtCQRQ99z01fVmpfWlW0q5a9EzaFUEFkWAGJW5cjb5H
Teo8cEZp6W/pH7QKWoS07EdF9z2jWgQt5nDOuXe4Z+68f7y8AjhAiVASyhsE
0ntwlRjJW2FkoNyGr01djC4CbWZAhMy9GAm3J9Ste9W+9zvs2oTiX1e/r8UJ
iSOppDkm2Ns7zTRmkHQQQ4oQM3dSE7Zq/ww/JGRr3cD0pHIvfSM8YQR7Vn9k
c34KIRUCCNRl/0GGqszM2ydsTMaOY+Wt6EzG+cm4YpXpNP72mLAyVthU4aY/
M/zMn/8VZ69rOPtZ4PmEhZpUfn3Yb/uDhmj32MnVgo7oNcVAhvrLdG6C4aDj
n8tQFK6Hysi+35RacvVEqcBE8zT2YfFqeGXTx4S7Ylxj5UYaiO8+w3liYqHI
mIjMGNYZ09MGzDIL6xsRFrAZfTJhjmvzLdhVLFSRqSKLHFMsVrGE5RZIYwWr
XNdIa+Q1kp+Mv2/DIQIAAA==
"""
    )

internal val NAVIGATION_STUBS =
    arrayOf(NAV_CONTROLLER, NAV_DESTINATION, NAV_GRAPH, NAV_HOST, TEST_NAV_HOST)

internal val TEST_CODE =
    kotlinAndBytecodeStub(
            "Test.kt",
            "androidx/test",
            0xef517b0b,
            """
package androidx.test

val classInstanceRef = TestClass()

val classInstanceWithArgRef = TestClassWithArg(15)

val innerClassInstanceRef = Outer.InnerClass(15)

object TestGraph

object TestObject

class TestClass

class TestClassWithArg(val arg: Int)

object Outer {
    data object InnerObject

    data class InnerClass (
        val innerArg: Int,
    )
}

interface TestInterface
class InterfaceChildClass(val arg: Boolean): TestInterface
object InterfaceChildObject: TestInterface

abstract class TestAbstract
class AbstractChildClass(val arg: Boolean): TestAbstract()
object AbstractChildObject: TestAbstract()

// classes with companion object to simulate classes marked with @Serializable
class TestClassComp { companion object }

class TestClassWithArgComp(val arg: Int) { companion object }

object OuterComp {
    data object InnerObject

    data class InnerClassComp (
        val innerArg: Int,
    ) { companion object }
}

class InterfaceChildClassComp(val arg: Boolean): TestInterface { companion object }

abstract class TestAbstractComp { companion object }
class AbstractChildClassComp(val arg: Boolean): TestAbstractComp() { companion object }
object AbstractChildObjectComp: TestAbstractComp()
""",
            """
META-INF/main.kotlin_module:
H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3CJc/HCtZSkFpcIsYWkgiSU
GLQYABRWGrdkAAAA
""",
            """
androidx/test/AbstractChildClass.class:
H4sIAAAAAAAA/4VQTWsTURQ9781XMk3MJH6labW1KrRZmLS4U4ppQAhMu6gl
i2T1khnaRyYzMO9Fusxvce1GUAQXElz6o8T7JlUQFBfv3HvuO5z78f3Hl68A
nuMpw65IozyT0XVHx0p3ehOlczHV/SuZRP1EKOWBMbT+VF0Q/FJ6sBjclzKV
+pjB3h8dDBms/YNhBQ48HzZKxEV+ycBGFfjYKIOjQlJ9JRXDXvi/CV6Q/2Ws
e8aCjEcM9XCW6USmndNYi0hoQRI+f2vRUsxA2QCo4Yzq19KwLmXRIcPJatnw
eZMXb7X0ebDh85LVXC2PeJedVBtuwFu8a3175/LAPq//ZiVSt+ySE7jG6Yhh
O/z3UWgeau+Z0rOZpl37WRQz1EKZxmeL+STOL8QkoUojzKYiGYpcGn5T9N9k
i3wav5aGbJ4vUi3n8VAqSb+9NM200DJLFQ7pkHaxaMPclTJOuQOXcJfYMXFO
0W9/Rrm99QnVD4XmEaHRADvYI7y3VuEWauZulBk3OjMCemuvjjknRaf9EdX3
f7WprAU3NhyPC9zBE4qviiEd3B7DGuDOAHcH1PY+pWgOsInWGExhC9tjeAo1
hQcKvsJDBVchUKj/BOsI9uW0AgAA
""",
            """
androidx/test/AbstractChildClassComp＄Companion.class:
H4sIAAAAAAAA/5VSTW/TQBB9u07jxARIWz4SvgolSC0SdRJxK0IqQUiRUpCg
yqUHtLG3dBN7jbybqMec+CH8g56QOKCoR34UYtYJcKWX2Zn35s143/rnr+8/
ADzHE4Y9oeM8U/FZaKWx4cHI2FxEtneqkriXCGN6Wfq55YLQKtM+GEN9LGYi
TIT+FL4bjWVkfXgM5RdKK/uSwdvZHdawhnKAEnyGkj1VhqE9uNyqfYbOzmCS
2UTpcDxLQ6WtzLVIwtfyREwT28s0TZhGNssPRT6R+f7uMAB3Kzdb0T/yY1qw
dNfLTWNY/yM4lFbEwgrCeDrzyDzmQtUFMLAJ4WfKVW3K4g5DazEPAt7gAa9T
tphXLr54jcW8y9vslV/hF1/LvM5db5e5Ca3/8cbHXYbqX4MY/CNq3ptY8riX
xZLh+kBp+XaajmR+JEYJIRuDLBLJUOTK1Suw1tda5sVcSS8TfMimeSTfKMc1
30+1VakcKqOo+UDrzApL6ww6ZG7J3ZhO7h6YPnyLqtBZQOfa02+onBf0Q4rl
AuziEcXasgFVBECdUXZlJX5GJ1+Ja+eFnU5wawkuBUV2FdeI87BNVVCI7uE+
mnhcLHyAVvFLkwfUWz+G18d6Hxt9bOIGpbjZp5m3j8EMGmgSbxAY3DEo/wZN
V6SgDwMAAA==
""",
            """
androidx/test/AbstractChildClassComp.class:
H4sIAAAAAAAA/41SW08TQRT+ZrfXZZFSEQt4QUEsVdlCfBJCgjWaJoUHJE2E
p2k7wtDtrNmZEh75LT77QtSQaGKIj/4o45lthRiN4WHPmXP2O9+5/vj55RuA
p1hhmOeqE0eycxwYoU2w0dIm5m1TO5BhpxZyrWtR710WjOHOn8gdEhfoBOMy
ZNakkmadIVXeXWwyuOXFpo80sh5SyJHN430GtuvDw0geDnyCmgOpGRYaV6lk
lXLsC7NhaYh8lyG31g6HSZeuwjBvBVcyUllcZ1guN7qRIYbg8KgXSGVErHgY
vBBveT+kxhRx9Nsmijd53BXx6qCfGx4mMMmQvyBjqF6pgcv0qz5KmLJDmGaY
a0TxfnAoTCvmUumAKxUZbgimg63IbPXDkFof/13rpjC8ww0nn9M7cmmZzIq8
FaABd8l/LK1VpVdnmeHV+UnRc0pO8p2feE5hxHNyqdL5yWx2xamyZyz7fLSY
KTjTTtX9/j7jFFLb4xdWjkKmU7l0IWPp6GxmG/+/BiqM6sha91LXMMxs95WR
PVFXR1LLVig2LhukE6hFHcEw1pBKbPV7LRHvcMIwFBtRm4dNHktrD51+XSkR
JxMVFOy9jvpxW7yU9t/UME/zryxYpkmnaCIOpuzgqbxHZGVI3yJdtBdJ2iU7
nXgfk7VOaIe0VzlDvjLzGaOnCcOTYSSwgiWSkwMUrmHMboBelo0WhgJ9A67A
LoZ0uvIJox/+SeMPAEOaHBWVHQaXkKwW/ldMvGFnuPkRM6eJxyVim5DRRTpJ
Y9WEu0INAzXy3ybGO3tw67hbx2wd93CfnpirYx4P9sA0FvBwDzmNMY2yhqex
qJHRKGiMa5R+ATUXZWovBAAA
""",
            """
androidx/test/AbstractChildObject.class:
H4sIAAAAAAAA/4VSXWvUQBQ9M7ubZNPV1vrR3bZ+1PqgPpi2+GYR1kUhECPY
ZaH0aZIMdrrZDCSzpY/75A/xHxQfCgqy6Js/SrwTV0VETMi995w5c27mJl+/
ffgE4DHuMWyJIiu1ys4CIysT9JPKlCI1g2OVZ6+SE5kaF4xh/U/ZkMJPqYsG
g7OvCmWeMjTuPxh10ILjowmXoWmOVcWwHf23zxMGbz/Nax8f3G72wvhg2I8H
zzu4BL9N5GVrpcs3wYk0SSlUUQWiKLQRRmmqY23iaZ6T1ZVorA2ZBS+lEZkw
gjg+OW3QuZkNbRvAwMbEnymLdqjKdqnBfOb7vMvrZz7zvrzl3flsj++wZ67H
P79z+Aq30j2Gzejfc6GGrsWPxoZh4/W0MGoiw+JUVSrJZf/3W9OQBjqTDMuR
KmQ8nSSyHArSMKxGOhX5SJTK4gXpH+hpmcoXyoLewnj0ly12aV7N+pA9Oz7K
twg5lFcoc7pbNbpNKLCjoNx6eAHvvF6+sxCDqi2KnR8CtMkK8LD0a/Maqe21
9BH88AKd91g+rwmOu3W8ie36f6PPQgarR2iEuBriWojruEEl1kJ00TsCq7CO
DVqv4FfYrOB8Bw71GM6sAgAA
""",
            """
androidx/test/AbstractChildObjectComp.class:
H4sIAAAAAAAA/41SXWvUQBQ9M7ubzaarrfWju1Zrv6TVB9NW3yzCuigEYgS7
LEifJpuhnW42I8ls6eM++UP8B8WHgoIs+uaPEu/EpSKCmJB77zlzciY5yfcf
n74AeIJNhvsiS3KtkjPfyML4nbgwuRiY7rFKk9fxiaRRj97VwRhW/pT2qFzK
S02FwdlXmTLPGCrbD/pN1OB4qKLOUDXHqmDYCv9rv6cM7v4gLb08cGvgBtFB
rxN1XzRxBV6DyKsMG6HOj/wTaeJcqKzwRZZpI4zSNEfaROM0Jatr4VAbMvNf
SSMSYQRxfHRaoQyYLQ1bwMCGxJ8pi3ZoSnZpg+nE83iLl9d04n57z1vTyR7f
Yc/rLv/6weEL3Er3GFbDf+dDm9Yt92hoGJbfjDOjRjLITlWh4lR2fj85hdXV
iWSYD1Umo/EolnlPkIZhMdQDkfZFriyekd6BHucD+VJZ0J4Z9/+yxS5lVi1f
tG0jpL5CyKG+QJ3TWSvRPUK+jYN67eEF3PNyeXUmBh5jjWrzlwANsgJczF3e
vERqe8x9Bn97geZHzJ+XBMd6We9io/z/6NOQweIhKgGuB7gR4CZu0YilAC20
D8EK3MYyrRfwCtwp4PwEJ9evBbwCAAA=
""",
            """
androidx/test/InterfaceChildClass.class:
H4sIAAAAAAAA/4VQz28SQRT+ZhZY2FJZqFYKVq21teXg0sabprElMSFBTWrD
AU4DO9Ipy26yMzQ98rd49mKiMfFgiEf/KOMbSpqYGD3M9973fnxv3vv569t3
AM/whGFLxGGaqPAqMFKboB0bmb4XQ9k6V1HYioTWLhiDfyEuRRCJeBS8HVzI
oXHhMNT/bD4juBFwkWXIvVCxMkcMmb3efpfB2dvvFuGi4CEDj7hIRwysV0QR
qwVw3KJSc640w3bnvz97TgNG0hxbDVLuMZQ748REKg5eSyNCYQSV8MmlQ9sy
CwULoIljil8py5rkhQcMJ/NZxeNVvnjzmcf9FY/nnep8dsib7GS1kvN5jTed
Hx9y3M+clm9YnqprmXzWz1mlQ4bNzj/OQh+i+a6NPR0b2raVhJKh1FGxfDOd
DGR6JgYRRSqdZCiirkiV5cug9y6ZpkP5SlmycTqNjZrIrtKKssdxnBhhVBJr
HNApMzQnR69ib0uLcjsXecJHxI6Ic7Je4ytWGvUvKH1a1GwT2i6gjseE69dV
8FG2pyPPqtGlSXdtqRXYi5LNNj6j9PGvMsXrgqUMx84Ct7BL9iXlblPuTh9O
G+tt3G2jig1yUWtT/70+mMYm7vfhapQ1HmgUNR5q5DUqGmu/ARpxRv/QAgAA
""",
            """
androidx/test/InterfaceChildClassComp＄Companion.class:
H4sIAAAAAAAA/5VSTW/TQBB9u07jxARIWz4SvqGp1CJRNxW3IiQIQrKUggRV
Lj2gjb1tN7HXyLuJesyJH8I/6AmJA4p65EchZp0AV3qZnXlv3sz6rX/++v4D
wHNsMoRCJ0WukrPQSmPDSFtZHItY9k5VmvRSYUwvzz53XBBa5doHY2iOxFSE
qdAn4fvhSMbWh8dQfaG0si8ZvK3tQQMrqAaowGeo2FNlGLr9S+7aJ81Wf5zb
VOlwNM1C5RRapOEbeSwmqe3l2thiEtu8OBDFWBb724MA3O1c78T/yE9ZyTLs
XG4aw+ofwYG0IhFWEMazqUf2MRfqLoCBjQk/U67apSzpMnTmsyDgLR7wJmXz
We3ii9eaz/b4Lnvt1/jF1ypvcte7x9yEzf8yx8ddhvpfhxj8Q+reGVtyuZcn
kuF6X2n5bpINZXEohikha/08FulAFMrVS7ARaS2Lcq6ktwk+5pMilm+V49of
JtqqTA6UUdT8SuvcCkvrDLrkbsV9Mp3cPTHd/CFVofOAzpWn31A7L+lHFKsl
GOIxxcaiAXUEQJNRdmUpfkYnX4ob56WfTnBrAS4EZXYV14jz8ISqoBTdw320
sVEufIBO+VeTB9TbPIIXYTXCWoR13KAUNyOaefsIzKCFNvEGgcEdg+pvYvaF
YBIDAAA=
""",
            """
androidx/test/InterfaceChildClassComp.class:
H4sIAAAAAAAA/41S308TQRD+9lp67XFIWxRLEUUBKVW5gjwJIcEaTZOCCZIm
wtO2Xcq11z1zu2145G/x2ReihkQTQ3z0jzLOlgoxGsPDzTczN/PNr/3x88s3
AGtYY1jgshmFfvPY00JpryK1iA55Q5SP/KBZDrhS5bD7zgZjSLd5n3sBly3v
db0tGtpGjGH6T4I9EpckNkYYEhu+9PUmQ7ywv1RjiBWWai5spBzE4ZDNoxYD
23fhYiwFCzcoVB/5imGxeq3u1qlIS+gtw0Ps+wzJjUYwrOpdi2LeCC79UNq4
xbBSqHZCTRReu9/1fJMjeeC9EIe8F+hyKJWOeg0dRts86oho/WKi2w4mkWNI
XZIR0/VGuKq/7iKPabOHOwxz1TBqeW2h6xH3paJZZKi5pjDl7YR6pxcENHzm
d7PbQvMm15x8VrcfoxszI1JGgHbcIf+xb6wSac0VhlfnJ1nHylmD7/zEsdKj
jpWM585PZu1Vq8SeMfv5WDaRtvJWKfb9fcJKx3czl1aSUvLx5Eg6YehWGWaq
/3kN1BU1YRvfckfTy9ntSe13RUX2feXXA7F1NR09gXLYFAzjVV+KnV63LqI9
TjEM2WrY4EGNR76xh063IqWIBusUlOy8CXtRQ7z0zb+pYZ3aX1WwQmuOU1MJ
wimzd9Kf0HoShHcJs+ZNEsZM40iSXCZrk6ItQqd4htHi9GeMn5JlwRtmgrQS
ycmLKKSRMQcgzbDRvYh3YsjlmbsQjhQ/YfzDP2nci4AhTRI3kRom5zC4LNyv
mHzLzjD1ETOnA0+MRjMF2aCJPA23OuB+jKeEZfLfI8bZA8QquF/BgwrmME8q
Fip4iMUDMIUClg6QVMgoFBVchUfKmFmFCYX8L78zjKNFBAAA
""",
            """
androidx/test/InterfaceChildObject.class:
H4sIAAAAAAAA/4VSy27TQBQ9M0ljxzU0La+EUh59IGCB24odFVKJQLJkgkSj
SKiriT1tJ3HGkj2JusyKD+EPKhaVQEIR7PgoxB0TihAS2Jr7OHPvuZ4z/vb9
42cAT3CfYV3oJM9UchoYWZgg1EbmRyKW7ROVJq/7AxkbB4yhMRATEaRCHwe/
0ArD6p/dXTIXDA4WGGp7SivzjKHy4GHPhwPXQxV1hqo5UQXDZvT/+U8Z3L04
LYk8cNvthp2D7n6n/cLHEvw6gQ2GjSjLj4OBNP1cKF0EQuvMCKMyijuZ6YzT
lKiWo2FmiCx4JY1IhBGE8dGkQoIwa+rWgIENCT9VNtumKNmhAbOp5/EmL9ds
6n59x5uz6S7fZs8dl395X+MNbkt3GdaifyhDEx0LPB4akvDNWBs1kqGeqEL1
U7n/+7NJpnaWSIalSGnZGY/6Mu8KqmFYibJYpD2RK5vPQe8gG+exfKls0poT
9/6ixQ4JVqUz1mi1rILk79JBbb5CntNLV0XZPcoCqwb5hUfn8M7K7fV5MXAL
G2T9nwVYpAjUeOmi+QZV22fxE/jbc1z+gOWzEuDYLO0dbJX/IsMVIrh6iEqI
ayGuh9TapBCtEDexeghW0LA12i/gF7hdwP0BVe1yPcgCAAA=
""",
            """
androidx/test/Outer＄InnerClass.class:
H4sIAAAAAAAA/4VU308cVRT+7sz+mB0WmOVXKay0yorL0nYAW62FVgFFBpel
QkOs+HLZvcLAMoMzs6S+GJ76JzTRFxNjfOKhTRSMTQy2b/5NxnjuznS3LgSS
mXvOPXPOd757zrnz979//AngJlYZhrhT8Vy78sgMhB+Yy7VAeDnLcYQ3V+W+
nwRjMLb5Pjer3Nk0lze2RTlIQmVITNuOHdxjiOWt0TUGNT+6lkYcSR0xaAya
LVFmvE0GZqWhoy0FBWnyD7Zsn+Fq8fzUUwxtmyKwGiiUwGLQy+7unusIJ5gg
qLK79y3DMDG4GG246Hqb5rYINjxuO77JHccNeGC7pJfcoFSrVqfkARI68exj
SEvwXEV8zWvVgGEtf1EKyyq2VmrqQl5pdKNHZhygkgXuauDZDh22Jz/6Glho
pTNcarXN1uxqRXhJDOm4Isve08TOv+rAXQ1vUsP43p5wKgzX86ehT2eLkIng
MHIS/G2GrCz0eY7vSMe8dJw737EgHcfSyOINqV2nw29xf2vOrQiGTDPScgKx
Kc83Hg4aTZKJSR0TeJdOJL6p8SrNUm/+jMp/yZA7r+XUb75RFVTVuBtsCY+h
6zQKkSnuuEHVdswlEfAKDzjZlN19lW4Qk0tKLqAh3yH7I1vuiKtSofH8+eRg
SFf6FV0xTg50ehRD0xUtQbKNpEqyQ3vxWOs/OZhUxtlse1fCUAaUcfXFTwnF
iC2mjKTcLbx8rC52Gxrp5KhpSuhEZkbmFOn6pGa0DcT62ThbePlEpcB06PGE
kd5OeofUVzINeI3oDMS0uJGQXCeZPEH3GaOaxDzdueZMMSQf0McbO3QjYmG3
Oou2I0q13Q3hPZAFlXV0y7y6xj1b7iPj4ErNCexdYTn7tm+TaabZDIb21YCX
d5b4XuSda/W+zz2+K4jR/8LSTWaCtvqqW/PKYt6WEJcjiLVT6Wh6FPpJyTN3
yR8TaRrpdPVpXaTdPH1XSOqFY6QKg7+h/RntFHxGawdki3spvg8pkkXa9YXe
9K1TDgNpEpVmBwa9IaYpZ4RkvPAr2g8bcIm6sa8Okw4dIpgMkXsVPNwazM4M
oF8JwcqACWIpOaWeQ3k4eIxLTxtBIdlUg2wqIrsUsekFjBT6cTnKPRIVK5ON
ffc9NMlgujB4hMEQskSrCiYR6DJH6e+QlNSyz3Hl4TGudr11hBEZeYRRY/QI
145w42nLMbIRo9d40Go2ajAS1aDO4HfcbC2DFsUz3MJ7EY+vSMp25QpjvyAe
Oxz7C8oPiKuHYydQliTQNXp/lJZY2JNSvX1qUvsHmSTtmxXLNSqWw218QHmW
SU9KUu/Xa3C/Hkr3CZ9igcr3eR3QwgrJL8h+hzo1tQ7VwrSFuxbu4UNS8ZGF
Gcyug/mYw8fr6PTl84kPvb4mfBg+Mj66fHT7uFU33vZh+siS/h/DktczzQcA
AA==
""",
            """
androidx/test/Outer＄InnerObject.class:
H4sIAAAAAAAA/4VUS08TURT+7p0+ptMChSIUUBCpykNpQV1BTJRoHCzFCMEo
q9t2hKHtjM7cEpas/AkuXLpwxULigkQTg7Dzf/g3jOdOR4vgI2nP+c655zXf
uTNfv3/8DOAmbjGMCKfquXZ1Jy8tX+aXm9LycqbjWN5yecuqyDgYQ3pLbIt8
XTgb+Z9ejSE2bzu2vM2gjU+spRBFzEAEcYaI3LR9htHif2rPMejSXZGe7Www
9I5PFNt9Wl6KGCu63kZ+y5JlT9iOnxeO40ohbZdwyZWlZr1OUckTZXV0UuFN
4W8uuFUrGM/Uto6+PaSRrZdNUafZzo0XTz/T3MQzhty/ulErUa5b1C7qyk3L
Y+g5W4Vaz1fqATMGuKJDN0srq3dKC/dSGISRIOcQQ3ex5koKyy9ZUlSFFJTI
G9sa7YUpkVACDKxG/h1bWQVC1RmG54e7wwbPcoOnD3cNriuQDLVuKFe6Uz9+
ZWQPd2d5gd2N6/zobYyn+WImrQ3yQmRWT0cHI1lWYA+OX2uLiXSMvHHCjLBO
OKGw6jbL1AyZP+wxjgmG+CrZ0zXJMPS46Ui7YZnOtu3bRNKdNnF0IVqL6Cra
jlVqNsqWt6qIVPy5FVFfE56t7NDZsSJFpbYkXoR27nTtR8ITDYuG+K1JKrgC
C3Xh+xaZxorb9CrWfVuVGAhLrJ0ZDjO0j0hA9YBaD+lrZMVId5CO0mk0sK6T
lVcLUd7JA+j7BDimw2AgQ8dAqhWABJVSRZPk4UHyaJis9XS9D47a4VoYfrIz
vXXoDvu2U3v2/pJKW0Jv2MkkzUn3T069QzSyN/UF/A2i2t7UIfiTyF4weIFk
BDyuB8X6WglhMYX66M+IHagrTC8MAR3ZX1T0BwlA8hP40wMMfMD5/cChYZak
4pFjEp3E6o2g3xR9b9RoDBeInuF1aCZGTFw06ekuEcSYiRwur4P5uIKr6zB8
9Rv3EfORCUCfj3QAkiR/AL5kiZ3FBAAA
""",
            """
androidx/test/Outer.class:
H4sIAAAAAAAA/3VRwW7TQBB9u05ixzE0TSlNKE2BFmiKhNuKU6mQSgSSpZBK
bRQJ5bRJVmUTx5bsTdRjTnwIf1BxqAQSiuDGRyHGbiAHileetzPz5s3u7M9f
X74BeIFnDCsi6Eeh6l+4WsbaPRlrGZlgDMWBmAjXF8G5e9IdyJ42YTDkjlSg
9CsGY6fWdpBFzkYGJkNGf1Axw2rjBr2XDNZRz08rbfCEbnnNs9Zxs/7GwS3Y
eQreZthqhNG5O5C6GwkVxK4IglALrULaN0PdHPs+SS03hqEmMfed1KIvtKAY
H00MuhFLTD4xYGBDil+oxNujXX+foTabOjYvc5sXZ1ObW4b14yMvz6YHfI8d
ciPz2rT49085XuRJwQFLZGwvCGRU90VM1yukzvU8GKo33HV7QTexybD5X86f
qT5kMFuUez4kyfXTcaDVSHrBRMWq68vjxQxoyPWwLxmWGiqQzfGoK6OWIA5D
qRH2hN8WkUr8edBZHEVSsX0WjqOefKuSXGXep/1PF+zTY2TSCVaStyHcJi9H
WCTktLKp95g8N5kzYXb3CtZlmn4yJwMlPCXrXBOQJynAQuFv8Rqxk6/wFfz9
FZzPWLpMAwZ20nKOB/Rv0DkeEVYJa2mLLewSHpLMMgmXOjA8rHi442EVd2mL
NQ9lVDpgMe5hvYNsDDvG/Ri5GBsxqr8BxLf8XAEDAAA=
""",
            """
androidx/test/OuterComp＄InnerClassComp＄Companion.class:
H4sIAAAAAAAA/5VTTW8SURQ9d4YyMGKlVC34/YGVGu0AcVdjohgTEmqT2rDp
wjzgqQ+GN2bmTdMlK3+I/6ArExeGdOmPMt43oI0LE7u599x77rk3cx78+Pnt
O4CnaBCaQo/iSI2OAyMTE+ylRsadaPqp3tWaUSiSJCttEFpF2gMRymNxJIJQ
6A/B3mAsh8aDS8g/U1qZ5wS3sdUvYQV5Hzl4hJz5qBJCu3feYzuEVqM3iUyo
dDA+mgZKs0SLMHgl34s0NJ1IJyZOhyaKd0U8kfHOVt+HY4+u14dn5LtpxhK2
z7eNsPZbsCuNGAkjuOdMj1w2kGwo2gACTbh/rGzVZDRqEerzme87Vcd3yozm
s8LpZ7c6n7WdJr30Cs7pl7xTduxsm+yGzf9zx8N1wsY/Zj3cJKz+LSAU/xhK
8A5YsD0x/CqdaCQJl3pKyzfpdCDjAzEIuVPpRUMR9kWsbL1sls6WSn5L/22U
xkP5Wlmutp9qo6ayrxLFwy+0jowwfC5Bix8jZx3i7NifBH/oPa4CaxnnlUdf
UTjJ6Psc81nzMeocS4sBFOEDZWJ0YSl+wtlZiksnmf1WcHXRXAgydBGrzLl4
wFWF2Ru4hduoZegO583s8F08zP4O7AVryodwu1jrotLFOi4zxJUu7944BCWo
osZ8Aj/BtQT5X3gloppLAwAA
""",
            """
androidx/test/OuterComp＄InnerClassComp.class:
H4sIAAAAAAAA/41UXVMURxQ9Pfs1Oywwi18IJJq4McuuukA0MYqJijEMATRi
iGjy0OyOMLDMkJlZyrykfPInWJW8pCoPeeJBKwmkYlWK6Ft+UyqV0zPjomAs
qqD73ru3zz197u35+98//gRwGl8LHJduw/ecxr1aaAdh7VortP0xb2W1ZLku
raYMAuXmIATMJbkma03pLtSuzS/Z9TCHlEB21HGd8COBdNkanBVIlQdnC8gg
ZyANXUB3FNIlf0FAWAUY6MhDQ4H54aITCJQn90bhvEDHgh1abTQWsgSMOn/z
XNsNhwlZ91a/FaiSyd5Rj016/kJtyQ7nfem4QU26rhfK0PFoT3vhdKvZPK8u
lDXI+6BAQRUpNey7stUMBe7u+QKWNblTwfN75lnAPuxXDPooaejNhL7jUoT9
5cEXQOMo73RoZ+xyy2k2bD+HNw0cUW3pfRm//LxLF3S8xabK1VXbbQicLO+G
310xQSfJYyipAu8IDKgmvC7xXZVYVoljr0+sqMRqAQN4Q1knKcCiDBbHvIYt
UNw+abmhvaDuOBQPI6ethhEDw3iPN7K/ackm5+1A+RVduC1Qet0YcAbkfNOm
shkvXLR9gZ7dKOQ1Wm8mr2Fob30tqUW6rJIDAYbLk8teSIza0tpKzeGFfFc2
a1fiYRsjl9Bv1UPPn5L+MuWJn9oFA6NgzXwbTGBkj4O1TYBaX8Ql9TgvU9bn
PKbsUDZkKElOW1lL8bsh1JJXC/iklxm/5yiPqmsNPsL1rftHDa1XMzRz677B
P83UDU3Pcu/gnuLexbD+9IHey9TuEW1InBPdlzt7sqbWpw2lnv6U1cz0RN7M
KW/82YPUxD5Tp711f0TXtTiJYcFwnrYxopsdfeleMSTGnz1M8WAhzngoaHfS
7lL2jWIbXmf9vrSeMbOK84hQNzn0P3rlcF2g62XRBHI3mXRqme+//0bLDZ0V
23LXnMDhkFzaHhzOYTyl3ZOOa0+3VuZt/6YaJDU/Xl02Z6XvKD8Jds6Esr48
JVcTv7QT+7r05YpNZi8VKWyzs+kaM17Lr9tXHQVxOIGY3UWO70LjJxpcD6vO
U4Ob9LLcD3DvUZ9q1Wn6mSj6Bb2rzNa4G5VN5Cv9v6HzcYQwy7ULagwqxKzy
VAVf0jsYZ/O3bjUwtBQq5wsm/2PMmpoj7pnKr+hcb8Nlo2A1ginECQlMkeSe
Hz6287B45QF+PAmrDgyTpeKUfwJtrn8Thx61D8Vk822y+YTsC7KYefRSrrj2
8UTA4kD6u++hKwajlf4N9MeQt7imIBQCP11J+XPcFbWBJzgyt4mjPW9v4Lg6
uYFBc3ADJzZw6tGOawwkjF5sj6BsxTaPWIOIwe84vVMGPTkvcAbvJzy+4q7a
VapUf0YmvV79C9oPyKTWq1vQphTQCf7/qCLpuCe3ovalcvo/KObobytWaitW
wll8yDpztHOK1AdR+XPIJVR7o6Ik9gSjc2ITH/+CscdRJIXb0dSp+focNyjy
KK2L3O9E5WdIGbQFrrCvn9xBysJVC59aGIdFExMWPsMkEwJMYfoOzADdAa4F
MKI1G6hIMUBPgH0BzkTBswFqAQYi++J/Kz3doBgJAAA=
""",
            """
androidx/test/OuterComp＄InnerObject.class:
H4sIAAAAAAAA/41US08TURT+7p0+ptMC5SEUUHxQtFClBXVhICZIfAwpxQjB
KKvbdoSh7QzO3BKWrPQfuHDpwhULiQsSTQzKzp/kwnjuMApCMCbtOd8597zm
O3fm+89PXwDcwm2GYeHUPNeubRWk5cvCQkta3qzb3MiajmN5C5V1qyrjYAzp
dbEpCg3hrBZ+ezWG2LTt2PIug5YbXU4hipiBCOIMEblm+wwjpf+oP8WgS3dR
erazytCTGy0d9Tr0UsRwyfVWC+uWrHjCdvyCcBxXCmm7hMuuLLcaDYpKHiur
o50Krwl/bdatWcGIpnbn4esfNLb1siUaNN+5XOnkc02NPmfI/qsbtRKVhkXt
oq5cszyGrtNVqPV0tRGwY4ArSnSzvLg0U569n8IAjAQ5Bxk6S3VXUlhh3pKi
JqSgRN7c1Gg/TImEEmBgdfJv2coqEqpNMLzY3x4yeIYbPL2/bXBdgWSodUO5
0u36wSsjs789yYvsXlzn397FeJrPdae1AV6MTOrp6EAkw4rs0cEbbS6RjpE3
TpgR1gknFFbdJpmaoe+MXcYxyhBfIt94XTIMPmk50m5aprNp+zYRNXNEHl2M
w2V0lGzHKreaFctbUmQqDt2qaCwLz1Z26GxblKJanxcboZ09Wfux8ETTokH+
apIKrsFsQ/i+Raax6La8qvXAViX6wxLLp4bDBO0kEtDdr1ZE+jpZMdJtpKN0
Gg2sG2QV1FKUd2wP+i4BjvEwGMjRMZA6DECCSqmiSfLwIPlymKx1dXwIjo7C
tTD8eGd6+9AZ9j1K7do5I5WhGz1hJ5M0J903ln+PaGQn/xX8LaLaTn4f/Glk
Jxi8SDICHteDYr2HCWExhXrpz4gdqGtMLw0BHZk/VPQFCUDyM/izPfR/xPnd
wKFhkqTikWMM7cTqzaBfnr49ajSGC0TP0Ao0ExdNXDLp6a4QxLCJLEZWwHxc
xbUVGL765XzEfHQHoNdHOgBJkr8Ar2VkytEEAAA=
""",
            """
androidx/test/OuterComp.class:
H4sIAAAAAAAA/31RW2sTQRT+ZjbJbjaxTeMlibX10lqbCm5bfKpFqEVhIaZg
Q0DyNEmGOslmV3YnoY958of4D4oPBQUJ+uaPEs9so0Wk7rDnO9fvzDnz4+fn
rwCe4jFDRYT9OFL9U0/LRHtHYy3jw2j03gZjKA3ERHiBCE+8o+5A9rQNiyG3
r0KlnzNYm/V2EVnkXGRgM2T0O5Uw1BpXcD5jcPZ7QVrtgpsSx28etw6ahy+L
uAY3T84FhrVGFJ94A6m7sVBh4okwjLTQKiK9GenmOAiIaqkxjDSRea+lFn2h
Bfn4aGLRZMyIvBFgYEPynypjbZPW32Goz6ZFl1e5y0uzqcsdy/n+gVdn012+
zfa4lXlhO/zbxxwvcVOwywzNgh+GNEYgksTMwlBIHRd7Ydi4Yub1v8ts3KX5
/pv7e9P3GewWxZ8MiX75zTjUaiT9cKIS1Q3kweVOaPGHUV8yLDZUKJvjUVfG
LUE5DOVG1BNBW8TK2HNn8fJKkord42gc9+QrZWK1eZ/2P12wQ4+TSTdaM29F
uE5WjrBEyOlkU+shWZ7ZO2F26xzOWRremCcDj+gAxYsE5IkKcFD4U1yhbPMV
voC/PUfxExbPUoeFTZJlCt+jf4Xu8YBwlbCetljDFuEe0SwRcbkDy8d1Hzd8
3MQtUlHxUUWtA5bgNpY7yCZwE9xJkEuwkmD1F4DlAzIZAwAA
""",
            """
androidx/test/TestAbstract.class:
H4sIAAAAAAAA/3VRy04CMRQ9t8AgIwriC/AR3Rh14ahxpzFBExMS1EQNG1eF
mWgFOsm0EJd8i3/gysSFIS79KOPt6NbNyXnctqft1/f7B4AjrBHqUodJrMLn
wEbGBncMjY6xiezaPIhQfpIjGfSlfgiuO0+RczME70RpZU8Jme2ddhE5eD6y
yBOy9lEZwmrr/22PCXOtXmz7SgeXkZWhtJI9MRhluBQ5KDgAgXrsPyun9pmF
B4SNydj3RVX4osxsMp7aqk7Gh2KfznKfL54oCzd3SG513p2617Pc6jwOI0Kp
pXR0NRx0ouROdvrsVFpxV/bbMlFO/5n+bTxMutGFcqJ2M9RWDaK2MorThtax
lVbF2mQ3IfjSf03dGzBWWQWpBnK7b5h6ZSJQY/RScx11xuLvAArw03wlxWWs
pt9CmOaseI9MEzNNzDZRQpkp5pqoYP4eZLCARc4NfIMlA+8H7YuiztMBAAA=
""",
            """
androidx/test/TestAbstractComp＄Companion.class:
H4sIAAAAAAAA/5VSTW/TQBB9u07jxARIWz4SPspXkNJK1E3FrQipBCFZSkGC
Kpce0MZZYBN7jbzrqMec+CH8g56QOKCoR34UYtYJcENwmZ15b96M962///j6
DcBjPGToCj3OMzU+Da00NjymcDgyNhex7Wfpx44LQqtM+2AMzYmYiTAR+n34
ajSRsfXhMVSfKK3sUwavuz1sYA3VABX4DBX7QRmGncG/Ljlg6HUH08wmSoeT
WRoqbWWuRRI+l+9EkVC7Jl0R2yw/EvlU5gfbwwDcLdvsxH/It2nJMuz+3zSG
9V+CI2nFWFhBGE9nHhnGXKi7AAY2JfxUuWqPsnGPobOYBwFv8YA3KVvMa+ef
vNZivs/32DO/xs8/V3mTu9595iZs/d0VHzcZ6r+tYfBdx+7Ukq/9bCwZLg+U
li+LdCTzYzFKCNkYZLFIhiJXrl6BjUhrmfcTYYyk1wjeZEUeyxfKce3XhbYq
lUNlFDUfap1ZYWmdQY9srbi70sndo9In36EqdJenc23nC2pnJX2XYrUEe7hH
sbFsQB0B0GSUXViJH9HJV+LGWWmkE1xbgktBmV3EJeI83KcqKEW3cBttPCgX
bqFT/sDkAfU2T+BFWI+wEWETVyjF1YhmXj8BM2ihTbxBYHDDoPoTIO6Wpv0C
AAA=
""",
            """
androidx/test/TestAbstractComp.class:
H4sIAAAAAAAA/4VRXWsTQRQ9s5vPdWOT+pVYramtMc2D2xRBsEWoEWEhTUFL
QPI0ScY6yWZWdiahj/kt/oPiQ0FBgo/+KPHuNrYPQn25Z+6dc889c+fX728/
ADxHg2Gdq2EUyuGpZ4Q23jGFg742ER+YVjj5nAVjKI74jHsBVyfeUX8kBiYL
myGzL5U0rxjs+nbXRRoZBylkGVLmk9QM1fb10nsMuf1BsBSpX0/eigNXMlRZ
uAzNenscGur1RrOJJ5URkeKB90Z85NOAGhR1TgcmjA55NBbR3oXBmw4KWGHI
X4oxNP7j8mrwnosSVvOwcIthsx1GJ95ImH7EpdIeVyo03BBNe53QdKZBQO8r
/XV5KAwfcsOpZk1mNi2fxSEfBzCwMdVPZZzt0GnYZKgt5q5jlS3HKi7mjpWz
crXyYl61d60d9pLZr9M/v2SsohWzd1mskY2dPxsbhrV3U2XkRPhqJrXsB+Lg
yhz9TiscCoaVtlSiM530RXTMicOw2g4HPOjySMb5suj6SomoFXCtBTU778Np
NBBvZXxXWc7p/jMltUFbSiVPq8RLI9ykLEN4h9AiTCfZFmVevADCdOMcubPk
+smSDDRRo+heEJCHQ5jDjcvmMpIVwv2Owgd2juJX3D5LKjaeUnSIVyDFEhmp
J9qPsU34gup3SfFeD7aPso+Kj/tYoyMe+HiI9R6YxiNUe0hpOBobGhmN0h/c
zA/4OwMAAA==
""",
            """
androidx/test/TestClass.class:
H4sIAAAAAAAA/3VRu04CQRQ9d5BFVpQFX+CrVgsXjZ3GRE1MSFATNTRWA7vR
gWU2YQZCybf4B1YmFoZY+lHGO6utzcl53Jk5N/P1/f4B4BjbhHWpo2Gqoklo
Y2PDB4bLRBpTABGCnhzLMJH6Kbzt9OKuLSBH8E6VVvaMkNvda5eQh+djDgXC
nH1WhlBv/XPnCaHS6qc2UTq8jq2MpJXsicE4x3XIQdEBCNRnf6KcajCLDgk7
s6nvi5rwRcBsNq3NpkeiQRf5zxdPBMJNHZE7W3APHvQtF7pMo5hQbikd34wG
nXj4IDsJO9VW2pVJWw6V03+mf5+Oht34SjlRvxtpqwZxWxnF6bnWqZVWpdrg
EIL3/evp1messQozDeT33zD/ykSgzuhl5hI2GEu/AyjCz/LNDNexlX0HYYGz
0iNyTSw2sdREGQFTVJqoYvkRZLCCVc4NfIM1A+8HjoCWJ8sBAAA=
""",
            """
androidx/test/TestClassComp＄Companion.class:
H4sIAAAAAAAA/5VSTW/TQBB9s07jxARIWz4SyjepaJGom4pbERIEIUVKQYIq
lx7QJllgE3uNvJuox5z4IfyDnpA4oKhHfhRi1ilwQ3CZnXlv3oz3rb//+PoN
wCNsEjalGeWZHh3HTlkXH3LoJNLaTpZ+bPkgjc5MCCLUx3Im40Sa9/GrwVgN
XYiAUH6sjXZPCMHWdr+GFZQjlBASSu6DtoT7vX/asE9ob/UmmUu0icezNNbG
qdzIJH6u3slp4jqZsS6fDl2WH8h8ovL97X4E4Tett4Z/yLdpwRJ2/m8aYfWX
4EA5OZJOMibSWcBWkQ9VH0CgCePH2le7nI3ahNZiHkWiISJR52wxr5x+ChqL
+Z7YpWdhRZx+Lou68L175Cds/MWSEBuE6m9fCKGndyaOHe1kI0W42NNGvZym
A5UfykHCyFovG8qkL3Pt6zOw1jVG5cVcxe8Qvcmm+VC90J5rvp4ap1PV11Zz
81NjMicdr7Nos6clf1E+hX9O/t5bXMX+5nyuPPiCyklB3+ZYLsB7uMOxtmxA
FRFQJ87OnYkf8inOxLWTwkUvuLIEl4IiO48LzAW4y1VUiK7jBpq8wC+8iVbx
37IH3Fs/QtDFahdrXazjEqe43OWZV49AFg00mbeILK5ZlH8CxwhM7PQCAAA=
""",
            """
androidx/test/TestClassComp.class:
H4sIAAAAAAAA/31RXWsTQRQ9s5vPdWOT+pVYq9WmNu2D2xRBMEXQiLCQtqAl
IHmaJGOdZDMrO7Ohj/kt/oPiQ0FBgo/+KPHuNrYPQl7umXvn3HPP3Pn95/tP
AM+xy7DG1TAK5fDMM0Ib74RCO+Bat8PJlzwYQ3nEp9wLuDr1jvsjMTB52Ay5
A6mkecVgN3a6LrLIOcggz5Axn6VmWO8s0W0xFA4GwUJhawmzngSuZKjycBma
jc44NNTojaYTTyojIsUD7634xOPAtEOlTRQPTBgd8mgsotaltZsOSlhhKF6J
MWwv83c9teWigtUiLNxi2OyE0ak3EqYfcam0x5UKDTdE095RaI7iIKCXVf5Z
PBSGD7nhVLMmU5sWzpJQTAIY2JjqZzLJ9ug0bDLU5zPXsaqWY5XnM8cqWNX5
bMPet/bYS2a/yf76mrPKVsLdZ4lCPjH9bGzoE9/HysiJ8NVUatkPxOtra/Ql
7XAoGFY6UomjeNIX0QknDsNqJxzwoMsjmeSLousrJaJ0F4KanQ9hHA3EO5nc
1RZzuv9NQZN2lEkfVktWRrhJWY7wDqFFmE2zOmVe8nzC7O4FCufp9daCDGp7
StG9JKAIh7CAG1fNVaQLhPsDpY/sAuVvuH2eVmxsU3SIVyLFChlppNpPsEP4
gup3SfFeD7aPqo+aj/tYoyMe+FjHwx6YxiNs9JDRcDQea+Q0Kn8BDq7wpy0D
AAA=
""",
            """
androidx/test/TestClassWithArg.class:
H4sIAAAAAAAA/31QTWsTURQ9781nxsRM4leaaq3aRZtFJy3ulGIMCANRoZZ0
kdVLZkhfM5mBeS+ly/wW124ERXAhwaU/SrwvDa5EeJx7z32Hcz9+/f7+A8Bz
7DHsiDwpC5lcRzpVOjoj6GdCqXOpL3rl1ANjCC/FlYgykU+j9+PLdKI9WAzu
S5lLfcJg78cHQwZr/2BYhQMvgA2fuCinDCyuIsCtCjiqJNUXUjHsDv7f9QW5
T1PdMwZkGzM0BrNCZzKP3qZaJEILkvD5lUVrMAMVA6B2M6pfS8O6lCVHDP3V
shnwFg94uFoG9HjoB9y3WqvlMe+y17WmG/I271o/P7o8tE8bf5lP6rbtO6Fr
rI6ZaeCZWQ9nmnbpF0nKUB/IPH23mI/T8kyMM6o0B8VEZENRSsM3xeBDsSgn
6RtpyNbpItdyng6lkvTby/NCCy2LXOGIDmWvV2mau1HGKXfgEj4mdkKcUww6
31DpbH9F7fNas0toNECIJ4T3b1S4jbq5DGXGjQ5J/42NV2QORtHpfEHt0z9t
qjeCjQ3H0zXu4BnFV+shHdwZwYpxN8a9mNo+oBStGFtoj8AUtvFwBE+hrvBI
IVijqxAqNP4AZEwrjogCAAA=
""",
            """
androidx/test/TestClassWithArgComp＄Companion.class:
H4sIAAAAAAAA/5VSTW8TMRB99qbZZAmQtnwkfLdNpRZBt6m4FSGVIKRIKUhQ
hUMPyElM62TXi2wn6jEnfgj/oCckDijqkR+FGG8CHFEv45n35s14n/fnr+8/
ADzDJsMToQcmU4Oz2Enr4iMKrURY+0G50wNz0srSzw0fhFaZDsEYqkMxEXEi
9En8tjeUfRciYCg+V1q5FwzB1na3giUUIxQQMhTcqbIMO53LLNpnaG51RplL
lI6HkzRW2kmjRRK/kp/EOHGtTFtnxn2XmUNhRtLsb3cjcL9wtdH/R35Mc9bv
v9Q0huU/gkPpxEA4QRhPJwEZx3wo+wAGNiL8TPlql7JBk6Exm0YRr/GIVymb
TUsXX4LabLrHd9nLsMQvvhZ5lfvePeYnrP/fmRB3Gcp/7WEIfdfOyJG/rWwg
Ga53lJZvxmlPmiPRSwhZ6WR9kXSFUb5egJW21tLk4yW9SvQ+G5u+fK08V383
1k6lsqusouYDrTMnHK2zaJK1Bf+9dHL/uHTth1TF3gA6lx5/Q+k8px9RLObg
JtYoVuYNKCMCqoyyKwvxUzr5Qlw5z830gltzcC7Is6u4RlyAdaqiXHQP91HH
Rr7wARr5z0weUG/1GEEby22stLGKG5TiZptm3j4Gs6ihTrxFZHHHovgbuLwQ
4QkDAAA=
""",
            """
androidx/test/TestClassWithArgComp.class:
H4sIAAAAAAAA/41SW08TQRT+ZnvZ7VpkqYgFvCAXLRXZQnwSQoI1xk1KTZDU
GJ6m7Vi23c6a3WnDI7/FZ1+IGhJNDPHRH2U8s63woAkmu+fMOXPO953L/Pz1
9TuAJ9hgWOSyHYV++9hVIlbuAYlqwOP4ja+OdqNONey/N8EYnC4fcjfgsuO+
anZFS5lIMWS3femrHYZ0yVttMKRKq408MjBtpGGRzaMOA/PysHEtBwN5ClVH
fsywXLuaeYsYOkLtahCC9his7VYwply7On9ZCy79UJq4wbBRqvVCRflud9h3
falEJHngPhfv+CBQ1VDGKhq0VBjt8agnoq1RLzdtTGOGIXcBxrD+H8Vfkm/l
UcSsbn+OYakWRh23K1Qz4r6MXS5lqLiisNith6o+CAJqe+pPpXtC8TZXnHxG
f5iitTEtclqARtsj/7GvrQqd2rTRl+cnBdsoGrbhnJ/Y9BmOZRtWunh+smBu
GhX2lJnPJgpZx5gzKqkfH7KGk96furAsSplLWxknq/E2mWYxdX/rPcUwvz+Q
yu8LTw792G8GYveyfFptNWwLhsmaL0V90G+K6IBTDEOhFrZ40OCRr+2xM+9J
KaJkbIKS7dfhIGqJF76+mx3zNP5iwQbNMU39GpjVY6XyymRlSd8mXdAvjXSK
7EzifUTWDkUbpO3yGXLl+S+YOE0Q1saZwAoek5wZReE6JvV86aTRaB1w6B9h
uXrspDPlz5j4+E+Y/ChgDGNRUeY4uYhkcch/w/RbdoZbnzB/mnhSWE8IGb02
I2nMTbBXUSFdJf8dQrx7iJSHex4WPNzHIh2x5GEZK4dgMR7g4SGsGJMxSjHs
RGZjODGmYhR/A+R3KVn3AwAA
""",
            """
androidx/test/TestGraph.class:
H4sIAAAAAAAA/3VSTW/TQBB9s/mw4waalo8klO/2UDjgtuJGhVQqQJaMkWgU
qeppE6/aTRwb2Zuox5z4IfyDikMlkFAEN34UYtZEcEB4pTfzZt887Yz84+fn
rwCeYovQlmmcZzo+940qjN9jeJ3L92cOiNAayZn0E5me+m8HIzU0DiqE+r5O
tXlOqGw/6jdRQ91DFQ6has50QeiG//F8RnD3h0nZ7UHYFjeIjnoH0eHLJq7A
a3DxKmEzzPJTf6TMIJc6LXyZppmRRmecR5mJpknCVmvhODNs5r9RRsbSSK6J
yazCk5GFhgUQaMz1c23ZDmfxLmFrMfc80RGeaHG2mLvfP4jOYr4nduiF44pv
H+uiJax2j6yDYyd4MjaEjXfT1OiJCtKZLvQgUQd/n8bzH2axIqyGOlXRdDJQ
eU+yhrAeZkOZ9GWuLV8WvaNsmg/VK21Jd2nc/8cWu7yUajlJ1+6I411mdY4t
joJPrWT3mPl2Xo61x5dwL8rr+0sxuPUBY/O3AA3mgIuVP81tVttv5QvE8SWa
n7B6URYEHpZ4B5vlb8O7Z4P1E1QCXAtwPcAN3OQU7QAddE9ABW5hg+8LeAVu
F6j/Ai/P7yRzAgAA
""",
            """
androidx/test/TestInterface.class:
H4sIAAAAAAAA/32Oz0rDQBDGv9lo08Z/qVqoiK9g2tKbJy9CoCKoeMlpm2xl
m3QD2Wnpsc/lQXr2ocSJ3p2Bb76Zgd/M1/fHJ4ApBoRr7YqmtsU2YeM5eRVJ
HZtmoXMTggjxUm90Umn3njzNlybnEAGhPytrrqxLHg3rQrO+I6jVJhAstdJr
BQQqZb61bTcSV4wJg/2uG6mhilQsbjHc7yZqRO1yQriZ/fOP3BBk2M5uSyZE
L/W6yc2DrQzh6nnt2K7Mm/V2Xpl752rWbGvnO8LGAf5C4eJXz3EpdSy8Q8lO
hiBFmKKboodILI5SHOMkA3mc4iyD8og9+j/Vk+x/PAEAAA==
""",
            """
androidx/test/TestKt.class:
H4sIAAAAAAAA/4VUbU/TUBR+bjvWrgzW8b6BiLzo5gsFfBc0ISQmjRMSJBhC
YtJt11kYbdJ7R/jIb/EXqHwgkcQQP/qjjOc2xGkLug/3nvvc53l6zunpfvz8
+g3AAzxnGPKCZhT6zSNHciGdLVpeSQOMwd7zDj2n7QUtZ6O+xxuE6gyDLS7X
2p4QbiCkFzT4Jn/PMF6p1tJGMW+ZYaYWRi1nj8t65PmBcLwgCKUn/ZDi9VCu
d9ptYtmNlG3pStM8TORy0GAxlJMpvfXlh9WoFVtMX53ZBY0ePdq4Sj71P3Ee
/SioRGyGMUrEDQIepRuUTmOjI3k026VTGsP+5eJkEilpHkMYVkmMMJgrjbYf
+PIFg16pblNxV1RgoMyQXYm5eUygZGEc1xgm/12xgesMmYpb3VaiGxamMJ0S
JTM0MGthThGLtf1QUoLOay69pic9qls7ONRpHplacmoBA9tXgUaXR76KFihq
LjK8Oz8uW+fHljamWZqpJ3ZtumgTQVtg3z9mTeKVM6Zm64RmCOzpglnbINAk
MNcFLbtXPWWJpvyScgzcZ7C6NTEYqjfz+5Lmf7MTSP+Au8GhL/x6m692R5y6
tRY2OUOh5gd8vXNQ59GWRxyGfNeNE896E3aiBn/pq7vSheV2yhCL9KYz1BMd
ZfUZULce0ylLu0F7WU1kCqMBSWAZlNBDJw1P6DRBqPplvqD3U/wGnl5wFfNP
XQl59KVVxaQqm1ANYDCtGk2qzL9UJsZIyWLVGuLRwMwZxndOMXmC3jNM7diF
U8ycoHiGuTi+eYLRz79N+2NRBhZZjpCdjmd0tuh2jv7/HpL5shozPMIK7RuE
36KmVHahu6i6uO3iDu66uId5Fw4WdsFU+5d2kRcwBXICPQJZgX6BglBgn8CQ
wLDAgMDgL1Y9gwBpBQAA
""",
            """
androidx/test/TestObject.class:
H4sIAAAAAAAA/3VSTWvbQBB9s7ZlWXEbN/2InfQrH4ekhyoJvTUUktCCQFWh
MYaQ09pa0rVlCaS1ydGn/pD+g9BDoIVi2lt/VOmscNpDqRbezHs789gZ9PPX
l28AXmCb0JZpnGc6vvSNKozfZXjXH6qBqYMIraGcSj+R6YV/o1YIzqFOtXlF
qOzs9pqowfFQRZ1QNR90QVgL/2f6kuAeDpKy3YOwPW4QnXaPopPXTdyC12Dx
NmErzPILf6hMP5c6LXyZppmRRmecR5mJJknCVnfCUWbYzH+rjIylkayJ8bTC
s5GFhgUQaMT6pbZsj7N4n7A9n3meaAtPtDibz9wfH0V7PjsQe3Rcd8X3T45o
CVt7QNahbkd4PjKE9feT1OixCtKpLnQ/UUd/n8YLOMliRVgOdaqiybiv8q7k
GsJKmA1k0pO5tnwheqfZJB+oN9qSzsK4948t9nkp1XKSjt0Rx8fMHI4tjoJP
rWRPmPl2Xo61Z9dwr8rrp4tioImNEssCNNgKcLH0p3mVq+239BXi7BrNz1i+
KgWBzRIfYav8cXj3bLByjkqAuwHuBbiPB5xiNUAbnXNQgTWs830Br8DDAs5v
jU1b0HUCAAA=
"""
        )
        .bytecode
