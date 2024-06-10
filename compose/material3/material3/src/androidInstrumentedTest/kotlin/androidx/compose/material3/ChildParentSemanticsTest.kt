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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.internal.childSemantics
import androidx.compose.material3.internal.parentSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ChildParentSemanticsTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun childAndParent_setSemantics_mergesCorrectly() {
        rule.setContent {
            Box(
                Modifier.testTag(ParentTag).parentSemantics { onLongClick("onLongClick") { true } }
            ) {
                Box(
                    Modifier.testTag(ChildTag)
                        .semantics {}
                        .childSemantics { onClick("onClick") { true } }
                )
            }
        }

        rule
            .onNodeWithTag(ChildTag, true)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

        rule
            .onNodeWithTag(ParentTag, true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
    }

    @Test
    fun childAndParent_haveConflict_childWins() {
        rule.setContent {
            Box(Modifier.testTag(ParentTag).parentSemantics { contentDescription = "Parent" }) {
                Box(Modifier.testTag(ChildTag).childSemantics { contentDescription = "Child" })
            }
        }

        rule.onNodeWithTag(ChildTag, true).assertContentDescriptionEquals("Child")

        rule
            .onNodeWithTag(ParentTag, true)
            .assert(SemanticsMatcher.keyNotDefined(ContentDescription))
    }

    @Test
    fun parent_noChild_appliesToItself() {
        rule.setContent {
            Box(
                Modifier.testTag(ParentTag).parentSemantics { onLongClick("longClick") { true } }
            ) {}
        }

        rule
            .onNodeWithTag(ParentTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
    }

    @Test
    fun parent_withNonClickable_mergesCorrectly() {
        rule.setContent {
            Box(Modifier.testTag(ParentTag).parentSemantics { onLongClick("longClick") { true } }) {
                Text(
                    modifier = Modifier.semantics { contentDescription = "text" },
                    text = "Foo Bar"
                )
            }
        }

        rule
            .onNodeWithTag(ParentTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertContentDescriptionEquals("text")
    }
}

private const val ChildTag = "child"
private const val ParentTag = "parent"
