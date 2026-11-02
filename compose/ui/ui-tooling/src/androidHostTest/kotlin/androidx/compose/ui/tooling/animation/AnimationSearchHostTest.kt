/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimation
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tooling.AnimateValueAsStateToolingHandle
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.ui.tooling.AnimationDebugMutableState
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.tooling.isAnimationPreviewEnabled
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

@OptIn(UiToolingDataApi::class)
class AnimationSearchHostTest {

    @After
    fun tearDown() {
        isAnimationPreviewEnabled = false
        AnimateXAsStateComposeAnimation.testOverrideAvailability(true)
        AnimatedContentComposeAnimation.testOverrideAvailability(true)
        InfiniteTransitionComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun emptySlotTables_returnsFalse() {
        assertMatchesTreeSearch(emptyList(), expected = false)
        assertMatchesTreeSearch(listOf(FakeCompositionData(emptyList())), expected = false)
    }

    @Test
    fun staticTreeAndUnsupportedPrimitives_returnsFalse() {
        val targetBased =
            TargetBasedAnimation(
                animationSpec = tween(200),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = 1f,
            )
        val decay = DecayAnimation(FloatExponentialDecaySpec(), initialValue = 0f)
        val root =
            callGroup(
                sourceInfo = "C(StaticPreview)10@100L20,11@130L20,12@160L20,13@190L20:Test.kt",
                children =
                    listOf(
                        callGroup(
                            sourceInfo = "CC(remember):Test.kt",
                            data = listOf("staticText", mutableStateOf(42)),
                        ),
                        callGroup(
                            sourceInfo = "CC(remember):Test.kt",
                            data = listOf(Animatable(0f)),
                        ),
                        callGroup(sourceInfo = "CC(remember):Test.kt", data = listOf(targetBased)),
                        callGroup(sourceInfo = "CC(remember):Test.kt", data = listOf(decay)),
                        nodeGroup(
                            sourceInfo = "C(Layout)20@300L10:Test.kt",
                            children = listOf(callGroup(sourceInfo = "C(Text)21@320L10:Test.kt")),
                        ),
                    ),
            )
        assertMatchesTreeSearch(listOf(FakeCompositionData(listOf(root))), expected = false)
    }

    @Test
    fun updateTransition_validatesSourceLocationAndNodeGroup() {
        // Root group without parent has null location in asTree().
        val rootTransition = callGroup(sourceInfo = "C(updateTransition)10@100L20:Test.kt")
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(rootTransition))),
            expected = false,
        )

        // Parent with null sourceInfo produces null location for child.
        val nullParentInfo =
            callGroup(
                sourceInfo = null,
                children = listOf(callGroup(sourceInfo = "C(updateTransition)10@100L20:Test.kt")),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(nullParentInfo))),
            expected = false,
        )

        // Parent with zero call locations produces null location for child.
        val emptyParentLocations =
            callGroup(
                sourceInfo = "C(Parent):Test.kt",
                children = listOf(callGroup(sourceInfo = "C(updateTransition)10@100L20:Test.kt")),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(emptyParentLocations))),
            expected = false,
        )

        // Parent with 1 non-repeatable location: 2nd call child gets null location.
        val exhaustedParentLocations =
            callGroup(
                sourceInfo = "C(Parent)10@100L20:Test.kt",
                children =
                    listOf(
                        callGroup(sourceInfo = "C(Text)11@120L10:Test.kt"),
                        callGroup(sourceInfo = "C(updateTransition)12@140L20:Test.kt"),
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(exhaustedParentLocations))),
            expected = false,
        )

        // Preceding non-call group does not consume parent's call location.
        val precededByControlFlowGroup =
            callGroup(
                sourceInfo = "C(Parent)10@100L20:Test.kt",
                children =
                    listOf(
                        callGroup(sourceInfo = "15@200L10:Test.kt"),
                        callGroup(sourceInfo = "C(updateTransition)12@140L20:Test.kt"),
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(precededByControlFlowGroup))),
            expected = true,
        )

        // Repeatable location (*) in parent supplies locations to arbitrary call indices.
        val repeatableParentLocation =
            callGroup(
                sourceInfo = "C(Parent)*10@100L20:Test.kt",
                children =
                    listOf(
                        callGroup(sourceInfo = "C(Text)11@120L10:Test.kt"),
                        callGroup(sourceInfo = "C(Text)12@140L10:Test.kt"),
                        callGroup(sourceInfo = "CC(updateTransition)13@160L20:Test.kt"),
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(repeatableParentLocation))),
            expected = true,
        )

        // NodeGroup with updateTransition sourceInfo is a NodeGroup (name = null, location = null).
        val nodeWithTransitionInfo =
            callGroup(
                sourceInfo = "C(Parent)10@100L20:Test.kt",
                children = listOf(nodeGroup(sourceInfo = "C(updateTransition)12@140L20:Test.kt")),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(nodeWithTransitionInfo))),
            expected = false,
        )

        // Prefix-similar names ("updateTransitionExtra" or "CCC(...)") must not match.
        val similarNameChildInfo =
            callGroup(
                sourceInfo = "C(Parent)10@100L20,11@130L20:Test.kt",
                children =
                    listOf(
                        callGroup(sourceInfo = "C(updateTransitionExtra)12@140L20:Test.kt"),
                        callGroup(sourceInfo = "CCC(updateTransition)13@160L20:Test.kt"),
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(similarNameChildInfo))),
            expected = false,
        )
    }

    @Test
    fun animatedVisibility_requiresDirectUpdateTransitionChild() {
        // AnimatedVisibility whose own sourceInfo has no locations (so child updateTransition has
        // location == null, isolating AnimatedVisibilitySearch from TransitionSearch).
        val withUpdateTransitionChild =
            callGroup(
                sourceInfo = "C(Parent)10@100L20:Test.kt",
                children =
                    listOf(
                        callGroup(
                            sourceInfo = "C(AnimatedVisibility):Test.kt",
                            children =
                                listOf(callGroup(sourceInfo = "C(updateTransition):Test.kt")),
                        )
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(withUpdateTransitionChild))),
            expected = true,
        )

        val withoutUpdateTransitionChild =
            callGroup(
                sourceInfo = "C(Parent)10@100L20:Test.kt",
                children =
                    listOf(
                        callGroup(
                            sourceInfo = "C(AnimatedVisibility):Test.kt",
                            children = listOf(callGroup(sourceInfo = "C(Box):Test.kt")),
                        )
                    ),
            )
        assertMatchesTreeSearch(
            listOf(FakeCompositionData(listOf(withoutUpdateTransitionChild))),
            expected = false,
        )
    }

    @Test
    fun animatedContent_checksDirectChildAndApiAvailability() {
        val tree =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(AnimatedContent):Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "C(updateTransition):Test.kt"
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertMatchesTreeSearch(tree, expected = true)

        AnimatedContentComposeAnimation.testOverrideAvailability(false)
        assertMatchesTreeSearch(tree, expected = false)
    }

    @Test
    fun animateValueAsState_checksRememberedToolingHandleAndApiAvailability() {
        val handle = fakeToolingHandle()

        // Handle inside nested remember group.
        val withRememberedHandle =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(animateValueAsState)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "C(Wrapper)12@140L10:Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf(handle),
                                                            )
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertMatchesTreeSearch(withRememberedHandle, expected = true)

        AnimateXAsStateComposeAnimation.testOverrideAvailability(false)
        assertMatchesTreeSearch(withRememberedHandle, expected = false)
        AnimateXAsStateComposeAnimation.testOverrideAvailability(true)

        // Unrelated remember sibling before or after the handle does not prevent finding it.
        val handleWithPrecedingRemember =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(animateValueAsState)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "C(Wrapper)12@140L10:Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf("unrelatedRemember"),
                                                            ),
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf(handle),
                                                            ),
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertThat(AnimationSearch.hasAnimations(handleWithPrecedingRemember)).isTrue()

        val handleWithSucceedingRemember =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(animateValueAsState)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "C(Wrapper)12@140L10:Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf(handle),
                                                            ),
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf("unrelatedRemember"),
                                                            ),
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertThat(AnimationSearch.hasAnimations(handleWithSucceedingRemember)).isTrue()

        // Tooling handle deeper than maxDepth (depth > 3) is not matched.
        val tooDeepHandle =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(animateValueAsState)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "C(Level1):Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "C(Level2):Test.kt",
                                                                children =
                                                                    listOf(
                                                                        callGroup(
                                                                            sourceInfo =
                                                                                "C(Level3):Test.kt",
                                                                            children =
                                                                                listOf(
                                                                                    callGroup(
                                                                                        sourceInfo =
                                                                                            "CC(remember):Test.kt",
                                                                                        data =
                                                                                            listOf(
                                                                                                handle
                                                                                            ),
                                                                                    )
                                                                                ),
                                                                        )
                                                                    ),
                                                            )
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertThat(AnimationSearch.hasAnimations(tooDeepHandle)).isFalse()

        // Missing tooling handle -> false.
        val withoutHandle =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "C(animateValueAsState)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "CC(remember):Test.kt",
                                                    data = listOf(Animatable(0f)),
                                                )
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertMatchesTreeSearch(withoutHandle, expected = false)
    }

    @Test
    fun rememberInfiniteTransition_checksDataDepthAndApiAvailability() {
        val transition = createInfiniteTransition()
        val toolingOverride = mutableStateOf<State<Long>?>(null)

        val validTree =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo =
                                            "C(rememberInfiniteTransition)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "CC(remember):Test.kt",
                                                    data = listOf(transition),
                                                ),
                                                callGroup(
                                                    sourceInfo = "C(run)12@140L10:Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "CC(remember):Test.kt",
                                                                data = listOf(toolingOverride),
                                                            )
                                                        ),
                                                ),
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertMatchesTreeSearch(validTree, expected = true)

        InfiniteTransitionComposeAnimation.testOverrideAvailability(false)
        assertMatchesTreeSearch(validTree, expected = false)
        InfiniteTransitionComposeAnimation.testOverrideAvailability(true)

        // Tooling override deeper than grandchild (great-grandchild) is not matched.
        val tooDeepOverride =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo =
                                            "C(rememberInfiniteTransition)11@120L20:Test.kt",
                                        children =
                                            listOf(
                                                callGroup(
                                                    sourceInfo = "CC(remember):Test.kt",
                                                    data = listOf(transition),
                                                ),
                                                callGroup(
                                                    sourceInfo = "C(Level1):Test.kt",
                                                    children =
                                                        listOf(
                                                            callGroup(
                                                                sourceInfo = "C(Level2):Test.kt",
                                                                children =
                                                                    listOf(
                                                                        callGroup(
                                                                            sourceInfo =
                                                                                "CC(remember):Test.kt",
                                                                            data =
                                                                                listOf(
                                                                                    toolingOverride
                                                                                ),
                                                                        )
                                                                    ),
                                                            )
                                                        ),
                                                ),
                                            ),
                                    )
                                ),
                        )
                    )
                )
            )
        assertMatchesTreeSearch(tooDeepOverride, expected = false)
    }

    @Test
    fun triggerSearch_respectsIsAnimationPreviewEnabled() {
        val debugState =
            AnimationDebugMutableState(
                state = mutableStateOf(true),
                states = { setOf(true, false) },
                label = "trigger",
            )
        val tree =
            listOf(
                FakeCompositionData(
                    listOf(
                        callGroup(
                            sourceInfo = "C(Parent)10@100L20:Test.kt",
                            children =
                                listOf(
                                    callGroup(
                                        sourceInfo = "CC(remember):Test.kt",
                                        data = listOf(debugState),
                                    )
                                ),
                        )
                    )
                )
            )

        isAnimationPreviewEnabled = false
        assertMatchesTreeSearch(tree, expected = false)

        isAnimationPreviewEnabled = true
        assertMatchesTreeSearch(tree, expected = true)
    }

    @Test
    fun multipleSlotTables_detectsAnimationInLaterTable() {
        val staticTable =
            FakeCompositionData(
                listOf(
                    callGroup(
                        sourceInfo = "C(Main)10@100L20:Test.kt",
                        children = listOf(callGroup(sourceInfo = "C(Text)11@120L10:Test.kt")),
                    )
                )
            )
        val subcompositionTable =
            FakeCompositionData(
                listOf(
                    callGroup(
                        sourceInfo = "C(Subcontent)20@200L20:Test.kt",
                        children =
                            listOf(callGroup(sourceInfo = "C(updateTransition)21@220L20:Test.kt")),
                    )
                )
            )
        assertMatchesTreeSearch(listOf(staticTable, subcompositionTable), expected = true)
    }

    private fun assertMatchesTreeSearch(
        slotTables: Collection<CompositionData>,
        expected: Boolean,
    ) {
        val fromSlotTables = AnimationSearch.hasAnimations(slotTables)
        val fromSlotTrees = AnimationSearch {
            PreviewAnimationClock({}, {})
        }
            .searchAny(slotTables.map { it.asTree() })
        assertThat(fromSlotTables).isEqualTo(expected)
        assertThat(fromSlotTables).isEqualTo(fromSlotTrees)
    }

    private fun fakeToolingHandle(): AnimateValueAsStateToolingHandle<Float, AnimationVector1D> =
        object : AnimateValueAsStateToolingHandle<Float, AnimationVector1D> {
            override val animatable: Animatable<Float, AnimationVector1D> = Animatable(0f)
            override val animationSpec: AnimationSpec<Float> = spring()

            override fun setToolingOverrideState(toolingOverrideState: State<Float>?) {}
        }

    private fun createInfiniteTransition(): InfiniteTransition {
        val ctor =
            InfiniteTransition::class.java.getDeclaredConstructor(String::class.java).apply {
                isAccessible = true
            }
        return ctor.newInstance("InfiniteTransition")
    }

    private class FakeCompositionData(override val compositionGroups: List<CompositionGroup>) :
        CompositionData {
        override val isEmpty: Boolean
            get() = compositionGroups.isEmpty()
    }

    private class FakeCompositionGroup(
        override val key: Any = 0,
        override val sourceInfo: String?,
        override val node: Any? = null,
        private val dataItems: List<Any?> = emptyList(),
        private val childrenGroups: List<CompositionGroup> = emptyList(),
    ) : CompositionGroup {
        // Mirror SlotTable's single-use DataIterator (`override fun iterator() = this`) per access.
        override val data: Iterable<Any?>
            get() =
                object : Iterable<Any?>, Iterator<Any?> {
                    private var index = 0

                    override fun iterator(): Iterator<Any?> = this

                    override fun hasNext(): Boolean = index < dataItems.size

                    override fun next(): Any? = dataItems[index++]
                }

        // Allocate fresh wrapper instances per iteration so reference identity (`===`) is never
        // assumed stable across iterations, matching SlotTableGroup.
        override val compositionGroups: Iterable<CompositionGroup>
            get() = Iterable {
                childrenGroups
                    .map { child ->
                        if (child is FakeCompositionGroup) {
                            FakeCompositionGroup(
                                key = child.key,
                                sourceInfo = child.sourceInfo,
                                node = child.node,
                                dataItems = child.dataItems,
                                childrenGroups = child.childrenGroups,
                            )
                        } else {
                            child
                        }
                    }
                    .iterator()
            }

        override val isEmpty: Boolean
            get() = childrenGroups.isEmpty()
    }

    /**
     * Builds a synthetic [CompositionGroup] representing a `@Composable` function call (`node ==
     * null`). [sourceInfo] uses the Compose compiler format, e.g.
     * `"C(MyComposable)*10@100L20:Test.kt"` (`CC(...)` for inline calls, `*` for repeatable child
     * call locations).
     */
    private fun callGroup(
        sourceInfo: String?,
        data: List<Any?> = emptyList(),
        children: List<CompositionGroup> = emptyList(),
    ): CompositionGroup =
        FakeCompositionGroup(
            sourceInfo = sourceInfo,
            node = null,
            dataItems = data,
            childrenGroups = children,
        )

    /**
     * Builds a synthetic [CompositionGroup] representing an emitted layout node (`node != null`).
     */
    private fun nodeGroup(
        sourceInfo: String?,
        data: List<Any?> = emptyList(),
        children: List<CompositionGroup> = emptyList(),
    ): CompositionGroup =
        FakeCompositionGroup(
            sourceInfo = sourceInfo,
            node = Any(),
            dataItems = data,
            childrenGroups = children,
        )
}
