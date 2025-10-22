/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package androidx.compose.material3.adaptive.navigation3

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

sealed interface TestKey

data object HomeKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<HomeKey> =
        object : Parcelable.Creator<HomeKey> {
            override fun createFromParcel(parcel: Parcel): HomeKey {
                return HomeKey
            }

            override fun newArray(size: Int): Array<HomeKey?> {
                return arrayOfNulls(size)
            }
        }
}

data object ListKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<ListKey> =
        object : Parcelable.Creator<ListKey> {
            override fun createFromParcel(parcel: Parcel): ListKey {
                return ListKey
            }

            override fun newArray(size: Int): Array<ListKey?> {
                return arrayOfNulls(size)
            }
        }
}

data class DetailKey(val id: String) : Parcelable, TestKey {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DetailKey> {
        override fun createFromParcel(parcel: Parcel): DetailKey {
            return DetailKey(parcel)
        }

        override fun newArray(size: Int): Array<DetailKey?> {
            return arrayOfNulls(size)
        }
    }
}

data class ExtraKey(val id: String) : Parcelable, TestKey {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExtraKey> {
        override fun createFromParcel(parcel: Parcel): ExtraKey {
            return ExtraKey(parcel)
        }

        override fun newArray(size: Int): Array<ExtraKey?> {
            return arrayOfNulls(size)
        }
    }
}

data object MainKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<MainKey> =
        object : Parcelable.Creator<MainKey> {
            override fun createFromParcel(parcel: Parcel): MainKey {
                return MainKey
            }

            override fun newArray(size: Int): Array<MainKey?> {
                return arrayOfNulls(size)
            }
        }
}

data object SupportingKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<SupportingKey> =
        object : Parcelable.Creator<SupportingKey> {
            override fun createFromParcel(parcel: Parcel): SupportingKey {
                return SupportingKey
            }

            override fun newArray(size: Int): Array<SupportingKey?> {
                return arrayOfNulls(size)
            }
        }
}

const val HomeScreenTestTag = "HomeScreen"
const val ListScreenTestTag = "ListScreen"
const val DetailScreenTestTag = "DetailScreen"
const val DetailPlaceholderScreenTestTag = "PlaceholderScreen"
const val ExtraScreenTestTag = "ExtraScreen"

const val NavDisplayTestTag = "NavDisplay"

@Composable
fun NavScreen(
    backStack: List<TestKey>,
    backNavigationBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    directive: PaneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
) {
    val listDetailSceneStrategy =
        rememberListDetailSceneStrategy<TestKey>(
            backNavigationBehavior = backNavigationBehavior,
            directive = directive,
        )
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize().testTag(NavDisplayTestTag),
        sceneStrategy = listDetailSceneStrategy,
        entryProvider =
            entryProvider {
                entry<HomeKey> { RedBox("Home", Modifier.testTag(HomeScreenTestTag)) }

                entry<ListKey>(
                    metadata =
                        ListDetailSceneStrategy.listPane(ListKey) {
                            PurpleBox(
                                "Placeholder",
                                Modifier.testTag(DetailPlaceholderScreenTestTag),
                            )
                        }
                ) {
                    BlueBox("List", Modifier.testTag(ListScreenTestTag))
                }

                entry<DetailKey>(metadata = ListDetailSceneStrategy.detailPane(ListKey)) {
                    GreenBox("Detail(${it.id})", Modifier.testTag(DetailScreenTestTag))
                }

                entry<ExtraKey>(metadata = ListDetailSceneStrategy.extraPane(ListKey)) {
                    OrangeBox("Extra", Modifier.testTag(ExtraScreenTestTag))
                }
            },
    )
}

val MockDualPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 16.dp,
    )

@Composable
fun BlueBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0.0f, 0.4f, 1.0f, 1.0f))
            .border(10.dp, Color(0.0f, 0f, 1.0f, 1.0f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun RedBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Color(1.0f, 0.3f, 0.3f, 1.0f)).border(10.dp, Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun GreenBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Color(0.2f, 0.9f, 0.7f, 1.0f)).border(10.dp, Color.Green),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun OrangeBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(1f, 0.75f, 0f, 1.0f))
            .border(10.dp, Color(1f, 0.6f, 0f, 1.0f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}

@Composable
fun PurpleBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0.3f, 0.2f, 1f, 1.0f))
            .border(10.dp, Color(0.3f, 0f, 1f, 1.0f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp))
    }
}
