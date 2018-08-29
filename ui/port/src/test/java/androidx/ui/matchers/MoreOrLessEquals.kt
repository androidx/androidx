/*
 * Copyright 2018 The Android Open Source Project
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

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.matchers

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import kotlin.math.absoluteValue

/**
 * Asserts that two [Double]s are equal, within some tolerated error.
 *
 * Two values are considered equal if the difference between them is within
 * 1e-10 of the larger one. This is an arbitrary value which can be adjusted
 * using the `epsilon` argument. This matcher is intended to compare floating
 * point numbers that are the result of different sequences of operations, such
 * that they may have accumulated slightly different errors.
 *
 * See also:
 *
 *  * [closeTo], which is identical except that the epsilon argument is
 *    required and not named.
 *  * [InInclusiveRange], which matches if the argument is in a specified
 *    range.
 */
class MoreOrLessEquals(
    private val value: Double,
    private val epsilon: Double = 1e-7
) : BaseMatcher<Double>() {

    override fun matches(item: Any?): Boolean {
        if (item !is Double) {
            return false
        }
        if (item == value) {
            return true
        }
        return (item - value).absoluteValue <= epsilon
    }

    override fun describeTo(description: Description?) {
        description?.appendText("$value (±$epsilon)")
    }
}