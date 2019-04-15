/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.CraneWrapper
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.test.android.AndroidUiTestRunner
import com.google.common.truth.Truth
import com.google.r4a.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SpacerUiTest : AndroidUiTestRunner() {

    private val bigConstraints = DpConstraints(
        maxWidth = 5000.dp,
        maxHeight = 5000.dp
    )

    @Test
    fun fixedSpacer_Sizes() {
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Container constraints=bigConstraints>
                        <OnChildPositioned onPositioned={ position ->
                            size = position.size
                        }>
                            <FixedSpacer width height />
                        </OnChildPositioned>
                    </Container>
                </MaterialTheme>
            </CraneWrapper>
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun fixedSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Center>
                        <Container constraints=DpConstraints(
                            maxWidth = containerWidth,
                            maxHeight = containerHeight
                        )>
                            <OnChildPositioned onPositioned={ position ->
                                size = position.size
                            }>
                                <FixedSpacer width height />
                            </OnChildPositioned>
                        </Container>
                    </Center>
                </MaterialTheme>
            </CraneWrapper>
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes() {
        var size: PxSize? = null
        val width = 71.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Container constraints=bigConstraints>
                        <OnChildPositioned onPositioned={ position ->
                            size = position.size
                        }>
                            <WidthSpacer width />
                        </OnChildPositioned>
                    </Container>
                </MaterialTheme>
            </CraneWrapper>
        }
        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            Truth.assertThat(size?.height?.round()?.value).isEqualTo(dm.heightPixels)
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val width = 40.dp
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Center>
                        <Container constraints=DpConstraints(
                            maxWidth = containerWidth,
                            maxHeight = containerHeight
                        )>
                            <OnChildPositioned onPositioned={ position ->
                                size = position.size
                            }>
                                <WidthSpacer width />
                            </OnChildPositioned>
                        </Container>
                    </Center>
                </MaterialTheme>
            </CraneWrapper>
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun heightSpacer_Sizes() {
        var size: PxSize? = null
        val height = 7.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Container constraints=bigConstraints>
                        <OnChildPositioned onPositioned={ position ->
                            size = position.size
                        }>
                            <HeightSpacer height />
                        </OnChildPositioned>
                    </Container>
                </MaterialTheme>
            </CraneWrapper>
        }
        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()?.value).isEqualTo(dm.widthPixels)
        }
    }

    @Test
    fun heightSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val height = 23.dp
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Center>
                        <Container constraints=DpConstraints(
                            maxWidth = containerWidth,
                            maxHeight = containerHeight
                        )>
                            <OnChildPositioned onPositioned={ position ->
                                size = position.size
                            }>
                                <HeightSpacer height />
                            </OnChildPositioned>
                        </Container>
                    </Center>
                </MaterialTheme>
            </CraneWrapper>
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(containerWidth.toIntPx())
        }
    }
}