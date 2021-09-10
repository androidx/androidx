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

import androidx.compose.lint.test.compiledStub

internal val NAV_BACK_STACK_ENTRY = compiledStub(
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

internal val NAV_CONTROLLER = compiledStub(
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
