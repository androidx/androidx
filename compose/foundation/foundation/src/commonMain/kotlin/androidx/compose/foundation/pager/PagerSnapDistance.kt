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

package androidx.compose.foundation.pager

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.runtime.Stable

/**
 * [PagerSnapDistance] defines the way the [Pager] will treat the distance between the current page
 * and the page where it will settle.
 */
@Stable
interface PagerSnapDistance {

    /**
     * Provides a chance to change where the [Pager] fling will settle.
     *
     * @param startPage The current page right before the fling starts.
     * @param suggestedTargetPage The proposed target page where this fling will stop. This target
     *   will be the page that will be correctly positioned (snapped) after naturally decaying with
     *   [velocity] using a [DecayAnimationSpec].
     * @param velocity The initial fling velocity.
     * @param pageSize The page size for this [Pager] in pixels.
     * @param pageSpacing The spacing used between pages in pixels.
     * @return An updated target page where to settle. Note that this value needs to be between 0
     *   and the total count of pages in this pager. If an invalid value is passed, the pager will
     *   coerce within the valid values.
     */
    fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int
    ): Int

    companion object {
        /**
         * Limits the maximum number of pages that can be flung per fling gesture.
         *
         * @param pages The maximum number of extra pages that can be flung at once.
         */
        fun atMost(pages: Int): PagerSnapDistance {
            requirePrecondition(pages >= 0) {
                "pages should be greater than or equal to 0. You have used $pages."
            }
            return PagerSnapDistanceMaxPages(pages)
        }
    }
}

/**
 * Limits the maximum number of pages that can be flung per fling gesture.
 *
 * @param pagesLimit The maximum number of extra pages that can be flung at once.
 */
internal class PagerSnapDistanceMaxPages(private val pagesLimit: Int) : PagerSnapDistance {
    override fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int,
    ): Int {
        debugLog {
            "PagerSnapDistanceMaxPages: startPage=$startPage " +
                "suggestedTargetPage=$suggestedTargetPage " +
                "velocity=$velocity " +
                "pageSize=$pageSize " +
                "pageSpacing$pageSpacing"
        }
        val startPageLong = startPage.toLong()
        val minRange = (startPageLong - pagesLimit).coerceAtLeast(0).toInt()
        val maxRange = (startPageLong + pagesLimit).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return suggestedTargetPage.coerceIn(minRange, maxRange)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is PagerSnapDistanceMaxPages) {
            this.pagesLimit == other.pagesLimit
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return pagesLimit.hashCode()
    }
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.PagerSnapDistance) {
        println("PagerSnapDistance: ${generateMsg()}")
    }
}
