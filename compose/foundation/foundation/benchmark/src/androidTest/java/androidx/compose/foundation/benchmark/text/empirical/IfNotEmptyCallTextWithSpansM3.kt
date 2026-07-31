/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text.empirical

import androidx.compose.foundation.benchmark.text.DoFullBenchmark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.test.filters.LargeTest
import org.junit.Assume
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Show the behavior of several spans stacked on top of each other, for the full length of the text.
 *
 * 38% of all text displayed contains at least 1 span. The most common # of spans is 16 (due to
 * usage of spans as text formatting).
 *
 * Spans are intentionally limited to
 * 1) Not MetricsAffectingSpans (usage is very low)
 * 2) Not inlineContent (usage is very low).
 *
 * TODO: If introducing more optimizations that depend on the "full length" assumption, confirm the
 *   frequency of spans that use the full length. This is not verified in the data set that produced
 *   this benchmark.
 */
class IfNotEmptyCallTextWithSpansM3(private val text: AnnotatedString) : EmpiricalTestCase() {
    private var toggleText = mutableStateOf(AnnotatedString(""))

    @Composable
    override fun MeasuredContent() {
        if (toggleText.value.text.isNotEmpty()) {
            Subject(toggleText.value)
        }
    }

    override fun toggleState() {
        if (toggleText.value.text.isEmpty()) {
            toggleText.value = text
        } else {
            toggleText.value = AnnotatedString("")
        }
    }
}

@LargeTest
@RunWith(Parameterized::class)
open class IfNotEmptyCallTextWithSpansParentM3(private val size: Int, private val spanCount: Int) :
    EmpiricalBench<IfNotEmptyCallTextWithSpansM3>() {
    override val caseFactory = {
        val text = generateCacheableStringOf(size)
        IfNotEmptyCallTextWithSpansM3(text.annotateWithSpans(spanCount))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters(): List<Array<Any>> = listOf()
    }
}

@LargeTest
@RunWith(Parameterized::class)
class AllAppsIfNotEmptyCallTextWithSpansM3(size: Int, spanCount: Int) :
    IfNotEmptyCallTextWithSpansParentM3(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = AllApps.TextLengthsWithSpans
    }
}

@LargeTest
@RunWith(Parameterized::class)
class SocialAppIfNotEmptyCallTextWithSpansM3(size: Int, spanCount: Int) :
    IfNotEmptyCallTextWithSpansParentM3(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = SocialApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ChatAppIfNotEmptyCallTextWithSpansM3(size: Int, spanCount: Int) :
    IfNotEmptyCallTextWithSpansParentM3(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = ChatApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ShoppingAppIfNotEmptyCallTextWithSpansM3(size: Int, spanCount: Int) :
    IfNotEmptyCallTextWithSpansParentM3(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = ShoppingApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}
