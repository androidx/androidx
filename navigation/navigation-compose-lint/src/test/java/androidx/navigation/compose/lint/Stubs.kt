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

internal val NAV_BACK_STACK_ENTRY = bytecodeStub(
    filename = "NavBackStackEntry.kt",
    filepath = "androidx/navigation",
    checksum = 0x6920c3ac,
    source = """
    package androidx.navigation

    public class NavBackStackEntry
""",
    """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcTFnZyfq5dakZhbkJMqxBzvXaLE
    oMUAAMRK5d0sAAAA
    """,
    """
    androidx/navigation/NavBackStackEntry.class:
    H4sIAAAAAAAAAI2Ru04CQRSG/zPAoisKKiqosTNeCleMncZEjCYkiIkYGqqB
    3eBwmU12hw12PItvYGViYYilD2U8u9rZOMWX8/9nMucyn19v7wBOsU3YldoN
    fOVOHC0j1ZNG+dppyKgqu4OmYVxrEzxlQYRCX0bSGUrdc+46fa9rskgRrHOl
    lbkgpPYPWjlkYNlII0tIm0cVEvbq/6pwRliuD3wzVNq59Yx0pZHsiVGU4lYp
    RoZAA7YmKlbHHLkVws5satuiJGxR4Gg2Lc2mJ+KYqpmPZ0sURHzrhPgFFP/U
    PBoYbvPKdz1Cvq601xiPOl7wIDtDdlbqflcOWzJQsf417aY/DrrejYpF+X6s
    jRp5LRUqzl5q7ZtkvBAVCN5CfLjpeCnMDVZOonmWw1fMvXAgUGJaiZlGmZn7
    uYB52El+M+E6tpIvIyxwLtdGqobFGpaYyMco1LCMlTYoxCqKnA9hh1gLYX0D
    +QLjIO8BAAA=
    """
)

internal val NAV_CONTROLLER = bytecodeStub(
    filename = "NavController.kt",
    filepath = "androidx/navigation",
    checksum = 0xa6eda16e,
    source = """
    package androidx.navigation

    public class NavController {
        public fun getBackStackEntry(route: String) = NavBackStackEntry()
    }
""",
    """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcTFnZyfq5dakZhbkJMqxBzvXaLE
    oMUAAMRK5d0sAAAA
    """,
    """
    androidx/navigation/NavController.class:
    H4sIAAAAAAAAAI1SW08TQRT+Ztru4oJlQbkrioDclAXikzVGIZqU1GrEkBie
    pttJmXY7m+xOG3zjt/gL9AmjiSE++qOMZ8pGrGhkkz1nzjnf+WbO5fuPz18B
    PMA6w5zQ9SRW9aNAi65qCKNiHVRFdyfWJomjSCYuGIPfFF0RREI3gpe1pgyN
    ixyD80hpZR4z5JZX9odQgOMhD5chbw5VyjBf+S97iWGkIc22CFt7hsQzCrxj
    KC1Xzm/cM4nSjdLKv9j6k0v23jhpBE1paolQOg2E1rHpwdOgGptqJ4oIVUji
    jpEDKDLMtmITKR00u+1AaSMTLaKgrO29qQpTFz7DWHgow1aW/kokoi0JyLD0
    +1PPmlP6y+OpP6O45mEE1xkWL1WJi3EPE7afoxcJqW+V7NUvpBF1YQT5eLub
    o9EyKwoMrEWuI2WtDTrVNxkenh6Pe3ySe9w/Pfb4AO8Z9ugXJ0+Pt/gG2y58
    e+9wn+8W/dw038hvOX6BtGMZthixY+nSo/D7pr3eMrQcO3FdMgxXlJbVTrsm
    kzeiFklbZRyKaF8kytqZc+Z1RxvVlmXdVaki19PzYTIs/Bn9NZg+mLcXd5JQ
    PleWcSrL2b/Ah01wWmD7caqS9tnWSlZAmtmWrp5g4GMvvEzS6TnzWCE5dAbA
    FXikRzBInlwveZvQnHRxbXT4E8a+YOLtCSY/9LE4lGlZxs+QGYs9FTFF8dUM
    d5X0Gv0uywyOez15F/dJPyHvNFHNHCBXxo0ybpLErBW3yriNuQOwFHcwfwA3
    hZdiIYWTYjDFYooimT8B9qxp7xsEAAA=
    """
)

internal val NAV_GRAPH_BUILDER = bytecodeStub(
    filename = "NavGraphBuilder.kt",
    filepath = "androidx/navigation",
    checksum = 0xf26bfe8b,
    source = """
    package androidx.navigation

    public class NavGraphBuilder
""",
    """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AZc0lnZiXUpSfmVKhl5dYlpmeWJKZ
    n6eXnJ9bkF+cKiTol1jmXpRYkOFUmpmTklrkXSLECRTyyC8u8S5RYtBiAAA0
    5BaVVQAAAA==
    """,
    """
    androidx/navigation/NavGraphBuilder.class:
    H4sIAAAAAAAAAI1RTUsCURQ996ljTVajfWlFmwiqRaPRrggqKAQzqHDT6ukM
    +nJ8EzNPcelv6R+0ClqEtOxHRXemVq16i8M951zu1/v8ensHcIRNwrbUXhQq
    b+xqOVJdaVSo3aYcXUXyqXc+VIHnR3kQwXmUI+kGUnfdm/aj3zF5ZAjWidLK
    nBIyu3utAnKwbGSRJ2RNT8WEncY/6h8Tio1+aAKl3WvfSE8ayZoYjDI8JiWQ
    I1CfpbFKWJUjr0bYmk5sW5SFLRyOppPydHIoqnSe+3i2hCOSrEPiCij96XjQ
    NzziRej5hMWG0n5zOGj70b1sB6yUGmFHBi0ZqYT/ivZdOIw6/qVKSOV2qI0a
    +C0VK3bPtA5NulqMGgRfIHk8cnIQxjVmbsp5k/1XzLxwIFBmtFIxiwpj4ScB
    s7BTfz3FVWykn0WYY6/wgEwd83UsMGIxAaeOIkoPoBhLWGY/hh1jJYb1DRVj
    VoXpAQAA
    """
)

internal val NAV_GRAPH_COMPOSABLE = bytecodeStub(
    filename = "NavGraphBuilder.kt",
    filepath = "androidx/navigation/compose",
    checksum = 0x6920624a,
    source = """
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
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AZc0lnZiXUpSfmVKhl5dYlpmeWJKZ
    n6eXnJ9bkF+cKiTol1jmXpRYkOFUmpmTklrkXSLECRTyyC8u8S7hEuXiBirU
    S61IzC3ISRViC0kFCSsxaDEAAFP1RV1sAAAA
    """,
    """
    androidx/navigation/compose/NavGraphBuilderKt.class:
    H4sIAAAAAAAAAL1VS1MTQRD+ZvMkoiYbBQKKqCgvcUN8XGJRJZRaKSM+ohzk
    NNmsYfKYpXYmKbxx9WT5F/wH3iwPFuXRH2XZkwRIMBZcdFPb09PT/X090z2b
    n7++fQdwF/cZlrmsBL6o7DqSt0WVa+FLx/WbO77ynA3efhLwne21lmhUvOCp
    joExJGu8zZ0Gl1XnebnmuWQNMSS6Qbzc8BjezheH4R7DyxePkEo6ELKaL9Z9
    3RDSqbWbzruWdE2Ych73tJX8wibDp38E/mD5b7hr3K2XNIlHUgfvD3HeSKHz
    q52crhf9oOrUPF0OuCBQLqWveZdgw9cbrUYjzxB9oLeFWo1jhGG6LxkhtRdI
    3nAK0mSqhKtiOMNw0d323Hov/gUPeNMjR4a5+eLxGgzZ7sLmKM7iXAKjOM8Q
    CfyW9uJIMcRcnwiljiNN1Zw1Oc32V+/Gqc6Xwf6Tk2HmpBJSqxyhMnz879Uc
    xP2zlrFydy2OqcPj6c84dRDzzNO8wjWnLVnNdoiuFDMiwsDqRrHIviuMliWt
    ssLwYX/vZmJ/L2ElrYQ1YXXVc51hwjp6451xcim5vzdpZdmilbVy0WSI9HBu
    PBmZTNth28rGOpJloz8+R614nNxHhngnet7WgPcZk1COUcZUxd52+ks0d8qr
    MNAsB5+NoCW1aHrO+mFPkd/0Ac2jXeo9RXAHfK/f7xgH+1hpbtc1Q3jdr1BH
    ni8K6W20mmUveN3tUbvou7yxyQNh5j3jSElUJdetgPTZV90sCrItlKDlw/vz
    8OhuUi+W/Fbgeo+Fic/0Yja7EX2OWIGFMMxjIYMIogjBodlLmpsKpxftxFck
    l2yb5C37AskvHecsyag5ZiQIBJjpuuMixjpwaaQwTutGS2OCInKduBjuGFuI
    luKms/pkhn4n8F8awj86wH95CP9UH//k3/kt+usw8jbu0ficrNN0Ile2ECpg
    poCrJHGtgOuYLeAGbm6BKcxhfgujChGFBYUxhVRHSSssKiwp3FLIKEwpLP8G
    Q1+Sp54GAAA=
    """
)

internal val NAV_HOST = bytecodeStub(
    filename = "NavHost.kt",
    filepath = "androidx/navigation/compose",
    checksum = 0x72aa34d0,
    source = """
    package androidx.navigation.compose

    import androidx.navigation.NavGraphBuilder

    public fun NavHost(route: String, builder: NavGraphBuilder.() -> Unit) { }
""",
    """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AZc0lnZiXUpSfmVKhl5dYlpmeWJKZ
    n6eXnJ9bkF+cKiTol1jmXpRYkOFUmpmTklrkXSLECRTyyC8u8S5RYtBiAAA0
    5BaVVQAAAA==
    """,
    """
    androidx/navigation/compose/NavHostKt.class:
    H4sIAAAAAAAAAJVSy24TMRQ9nrxDH0lKXwFKoS19AZNWsApCKhWFqCEgUrLp
    ypmY1MnErmY8Udn1W/gDdogFqljyUYg7k6SNVCRgpDn3+vjch33989e37wCe
    4CHDGlctT8vWma14X7a5kVrZju6dal/YNd5/rX1zaFJgDLkO73Pb5aptv212
    hENsjCE1FDE83aheKerGk6pdrna1caWyO/2e/TFQTpjetw+G3k55s8HQ+f+4
    Z4+qf+qbWnnl8dOTF4F0W8K7zPJBSVN+HhVbqWqvbXeEaXpcUkqulDZ8kL6m
    TS1w3TJDwtOBEWlkGJbGOpHKCE9x166osE1fOn4KNxhmnRPhdIfh77jHe4KE
    DOvjJxvcWfn6WTcbE5jEVBYTmKb7bA6aTyPPULiuZlj+26Uy5EeSN8LwFjec
    OKvXj9HUWQgJBtYNHYv4Mxl6JfJaOwyHF+fF7MV51spZAzMVmQVr9BfXcySx
    Smw3mbPIxnbnc/HiTCFesErJCFkp8eNz0kqnwpS7jGrSQYYNjXe59k9DpBGM
    gl+eGUG3rtUoy9GnU0GC7PANPu7SM4zv65ZgmK5KJWpBrym8I950RdiDdrjb
    4J4M10MyU5dtxU3gkb/6PlBG9kRF9aUvaftylHtXr4Sq1XXgOeJAhvGLw5jG
    IGJMiB1YiCP8SIYEkohhjVZ7xFtkJ7cK2a/IbRcKhF/CYeABYZLkE4Tr5M8N
    hMhgJko0iTxu0v5GpE5hM+QsItJRlXREb0W4im2y+8TOUu25Y8QqmK9ggRCL
    FRRxq4LbuHMM5mMJd4+R9pHwsewjE2Hexz0f932s/AYz8VZoLwQAAA==
    """
)
