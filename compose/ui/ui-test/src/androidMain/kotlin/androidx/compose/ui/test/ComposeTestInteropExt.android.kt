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

package androidx.compose.ui.test

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.core.view.children
import androidx.test.espresso.ViewInteraction
import java.util.concurrent.atomic.AtomicReference
import kotlin.sequences.forEach

/**
 * Scopes the Compose interaction to the View hierarchy matched by the provided Espresso
 * [ViewInteraction].
 *
 * It resolves the View from the Espresso [interaction], locates all Compose roots within that view
 * hierarchy, and creates a new, scoped SemanticsNodeInteractionsProvider.
 *
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionBasicSample
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionRecyclerViewSample
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionFragmentSample
 */
@ExperimentalTestApi
fun ComposeUiTest.onRootWithViewInteraction(
    interaction: ViewInteraction
): SemanticsNodeInteractionsProvider {
    val matchedView = interaction.extractView()
    val composeRoots = matchedView.findAllComposeRoots()

    val provider =
        this as? TestOwnerProvider
            ?: error(
                "This implementation of ComposeUiTest does not support onRootWithViewInteraction."
            )
    val testOwner = provider.testOwner
    val scopedTestOwner =
        object : TestOwner by testOwner {
            override fun getRoots(atLeastOneRootExpected: Boolean) = composeRoots
        }
    val scopedContext = TestContext(scopedTestOwner)

    return ScopedSemanticsProvider(scopedContext)
}

private class ScopedSemanticsProvider(private val testContext: TestContext) :
    SemanticsNodeInteractionsProvider {

    override fun onNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean) =
        SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)

    override fun onAllNodes(matcher: SemanticsMatcher, useUnmergedTree: Boolean) =
        SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
}

/** Synchronously extracts a View from an Espresso [ViewInteraction]. */
private fun ViewInteraction.extractView(): View {
    val viewRef = AtomicReference<View>()

    this.check { view, noViewFoundException ->
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        viewRef.set(view)
    }

    return checkNotNull(viewRef.get()) { "Espresso matched the view, but the reference was null." }
}

/**
 * Recursively traverses the View hierarchy starting from [this] View to find all instances of
 * [ViewRootForTest].
 *
 * These roots represent the entry points for Compose UI within the Android View hierarchy.
 */
@Suppress("VisibleForTests")
private fun View.findAllComposeRoots(): Set<ViewRootForTest> {
    val roots = mutableSetOf<ViewRootForTest>()

    fun traverse(view: View) {
        if (view is ViewRootForTest) {
            roots.add(view)
        }

        if (view is ViewGroup) {
            view.children.forEach { child -> traverse(child) }
        }
    }

    traverse(this)
    return roots
}
