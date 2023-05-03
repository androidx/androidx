/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.testing.embedding

import android.app.Activity
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitInfo
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// TODO(b/269360912): Support SplitController.
/**
 * A [TestRule] that will stub out the behavior of [ActivityEmbeddingController] and
 * [RuleController] with a more simple one that will support testing independent of the current
 * platform.
 */
class ActivityEmbeddingRule : TestRule {

    private val stubEmbeddingBackend = StubEmbeddingBackend()
    private val decorator = StubEmbeddingBackendDecorator(stubEmbeddingBackend)

    override fun apply(
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        base: Statement,
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        description: Description
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                EmbeddingBackend.overrideDecorator(decorator)
                try {
                    base.evaluate()
                } finally {
                    stubEmbeddingBackend.reset()
                    EmbeddingBackend.reset()
                }
            }
        }
    }

    /**
     * Overrides the return value of [ActivityEmbeddingController.isActivityEmbedded].
     *
     * @param activity [Activity] that will be passed to
     * [ActivityEmbeddingController.isActivityEmbedded].
     * @param isActivityEmbedded whether [ActivityEmbeddingController.isActivityEmbedded] should
     * return `true` for the `activity`.
     */
    fun overrideIsActivityEmbedded(activity: Activity, isActivityEmbedded: Boolean) {
        stubEmbeddingBackend.overrideIsActivityEmbedded(activity, isActivityEmbedded)
    }

    /**
     * Overrides the return value of [SplitController.splitSupportStatus].
     *
     * @param status the global [SplitController.SplitSupportStatus] that will be returned instead.
     */
    fun overrideSplitSupportStatus(status: SplitController.SplitSupportStatus) {
        stubEmbeddingBackend.splitSupportStatus = status
    }

    /**
     * Overrides current split info reported by [SplitController.splitInfoList]. The initial
     * override value is set to an empty [List].
     *
     * @param activity the [Activity] corresponding to the [splitInfoList].
     * @param splitInfoList a list of [SplitInfo] that will be emitted through
     * [SplitController.splitInfoList].
     */
    fun overrideSplitInfo(activity: Activity, splitInfoList: List<SplitInfo>) {
        stubEmbeddingBackend.overrideSplitInfo(activity, splitInfoList)
    }
}