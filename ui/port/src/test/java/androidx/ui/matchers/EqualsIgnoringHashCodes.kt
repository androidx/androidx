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

class EqualsIgnoringHashCodes(private val expected: String) : BaseMatcher<String>() {

    override fun describeTo(description: Description?) {
        description?.appendText("multi line description equals $expected")
    }

    override fun describeMismatch(item: Any?, description: Description?) {
        description?.appendText("expected normalized value\n" +
                expected +
                "\nbut got\n" +
                normalize(item))
    }

    override fun matches(item: Any?): Boolean {
        return expected == normalize(item)
    }

    private fun normalize(value: Any?) = (value as? String)?.replace(Regex("#[0-9a-f]+"), "#00000")
}