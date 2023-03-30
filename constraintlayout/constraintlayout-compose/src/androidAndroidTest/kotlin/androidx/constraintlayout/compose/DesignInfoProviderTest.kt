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

package androidx.constraintlayout.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DesignInfoProviderTest {
    @get:Rule
    internal val rule = TestRule(createComposeRule())

    @Test
    fun designInfoProviderInRemember_withConstraintSet() {
        rule.setWithConstraintSet()

        // Fetch the current DesignInfoProvider, before recomposition
        val designInfoProvider = rule.getDesignInfoProvider()

        rule.forceRecomposition()

        // If the DesignInfoProvider was wrapped in a 'remember' statement, the instance should be
        // the same before and after recomposition
        assertSame(designInfoProvider, rule.getDesignInfoProvider())
    }

    @Test
    fun designInfoProviderInRemember_withDsl() {
        rule.setWithDsl()

        // Fetch the current DesignInfoProvider, before recomposition
        val designInfoProvider = rule.getDesignInfoProvider()

        rule.forceRecomposition()

        // If the DesignInfoProvider was wrapped in a 'remember' statement, the instance should be
        // the same before and after recomposition
        assertSame(designInfoProvider, rule.getDesignInfoProvider())
    }

    @Test
    fun withConstraintSet() {
        rule.setWithConstraintSet()

        val designInfoProvider = rule.getDesignInfoProvider()
        val designInfoJson = JSONObject(designInfoProvider.getDesignInfo(0, 0, ""))
        assertEquals("CONSTRAINTS", designInfoJson["type"])
        assertEquals(1, designInfoJson["version"])
        val contentJson = designInfoJson["content"] as JSONObject
        assertEquals(6, contentJson.length())
        val viewInfoList =
            contentJson.keys().asSequence().map { contentJson[it] as JSONObject }.toList()
        assertEquals(1, viewInfoList.filter { it.getBoolean("isRoot") }.size)
        val helpers = viewInfoList.filter { it.getBoolean("isHelper") }
        assertEquals(1, helpers.size)
        val helperReferences = helpers[0]["helperReferences"] as JSONArray
        assertEquals(2, helperReferences.length())
        assertEquals("box1", helperReferences[0])
        assertEquals("box2", helperReferences[1])
    }

    @Test
    fun withDsl() {
        rule.setWithDsl()

        val designInfoProvider = rule.getDesignInfoProvider()
        val designInfoJson = JSONObject(designInfoProvider.getDesignInfo(0, 0, ""))
        assertEquals("CONSTRAINTS", designInfoJson["type"])
        assertEquals(1, designInfoJson["version"])
        val contentJson = designInfoJson["content"] as JSONObject
        assertEquals(6, contentJson.length())
        val viewInfoList =
            contentJson.keys().asSequence().map { contentJson[it] as JSONObject }.toList()
        assertEquals(1, viewInfoList.filter { it.getBoolean("isRoot") }.size)
        val helpers = viewInfoList.filter { it.getBoolean("isHelper") }
        assertEquals(1, helpers.size)
        val helperReferences = helpers[0]["helperReferences"] as JSONArray
        assertEquals(2, helperReferences.length())
    }
}

/**
 * Utility test rule
 */
internal class TestRule(rule: ComposeContentTestRule) : ComposeContentTestRule by rule {
    private var recomposed by mutableStateOf(false)

    fun forceRecomposition() {
        if (!recomposed) {
            recomposed = true
            waitForIdle()
        } else {
            fail("Already recomposed")
        }
    }

    fun setWithConstraintSet() {
        recomposed = false
        setContent {
            NoInlineWrapper {
                @Suppress("UNUSED_EXPRESSION")
                recomposed // Read value so recomposition covers ConstraintLayout
                ConstraintLayout(
                    modifier = Modifier.size(50.dp),
                    constraintSet = ConstraintSet {
                        val box1 = createRefFor("box1")
                        val box2 = createRefFor("box2")
                        val guideline = createGuidelineFromStart(fraction = 0.5f)
                        val barrier = createEndBarrier(box1, box2)
                        val box3 = createRefFor("box3")

                        constrain(box1) {
                            top.linkTo(parent.top)
                            end.linkTo(guideline)
                        }
                        constrain(box2) {
                            top.linkTo(box1.bottom)
                            start.linkTo(guideline)
                        }

                        constrain(box3) {
                            start.linkTo(barrier)
                            top.linkTo(box2.bottom)
                        }
                    }
                ) {
                    Box(
                        Modifier
                            .layoutId("box1")
                            .size(if (recomposed) 10.dp else 5.dp) // Recompose part of the content
                    )
                    Box(
                        Modifier
                            .layoutId("box2")
                            .size(10.dp)
                    )
                    Box(
                        Modifier
                            .layoutId("box3")
                            .size(10.dp)
                    )
                }
            }
        }
        waitForIdle()
    }

    fun setWithDsl() {
        recomposed = false
        setContent {
            NoInlineWrapper {
                @Suppress("UNUSED_EXPRESSION")
                recomposed // Read value so recomposition covers ConstraintLayout
                ConstraintLayout(
                    modifier = Modifier.size(50.dp)
                ) {
                    val (box1, box2, box3) = createRefs()
                    val guideline = createGuidelineFromStart(fraction = 0.5f)
                    val barrier = createEndBarrier(box1, box2)
                    Box(modifier = Modifier
                        .size(if (recomposed) 10.dp else 5.dp) // Recompose part of the content
                        .constrainAs(box1) {
                            top.linkTo(parent.top)
                            end.linkTo(guideline)
                        })
                    Box(modifier = Modifier
                        .size(10.dp)
                        .constrainAs(box2) {
                            top.linkTo(box1.bottom)
                            start.linkTo(guideline)
                        })
                    Box(modifier = Modifier
                        .size(10.dp)
                        .constrainAs(box3) {
                            top.linkTo(box2.bottom)
                            start.linkTo(barrier)
                        })
                }
            }
        }
        waitForIdle()
    }

    fun getDesignInfoProvider(): DesignInfoProvider {
        val nodeInteraction = onNode(SemanticsMatcher.keyIsDefined(DesignInfoDataKey))
        nodeInteraction.assertExists()
        return nodeInteraction.fetchSemanticsNode().config[DesignInfoDataKey]
    }
}

/**
 * A [Composable] function that wraps the contents in a Box. It is intentionally not inline to
 * differentiate scope to the compiler.
 */
@Composable
private fun NoInlineWrapper(content: @Composable () -> Unit) {
    Box {
        content()
    }
}