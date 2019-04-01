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

package androidx.animation

import androidx.ui.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeyframeAnimationTest {

    @Test
    fun equalsStartAndEndValues() {
        val start = 0f
        val end = start // the same
        val fullTime = 400
        val animation = KeyframesBuilder<Float>().run {
            duration = fullTime
            start at 100
            0.5f at 200
            0.8f at 300
            end at fullTime
            build()
        }

        val atStart = animation.getValue(0L, start, end, 0f, ::lerp)
        assertThat(atStart).isEqualTo(start)

        val at250 = animation.getValue(250L, start, end, 0f, ::lerp)
        assertThat(at250).isEqualTo(0.65f)

        val atEnd = animation.getValue(fullTime.toLong(), start, end, 0f, ::lerp)
        assertThat(atEnd).isEqualTo(end)
    }
}
