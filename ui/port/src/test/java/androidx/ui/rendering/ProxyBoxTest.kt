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

package androidx.ui.rendering

import androidx.ui.rendering.proxybox.RenderSemanticsGestureHandler
import androidx.ui.semantics.SemanticsAction
import androidx.ui.semantics.SemanticsConfiguration
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/*
 * Copyright (C) 2017 The Android Open Source Project
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

@RunWith(JUnit4::class)
class ProxyBoxTest {

    @Test
    fun `RenderSemanticsGestureHandler adds-removes correct semantic actions`() {
        val renderObj = RenderSemanticsGestureHandler(
            onTap = {},
            onHorizontalDragUpdate = { _ -> }
        )

        var config = SemanticsConfiguration()
        renderObj.describeSemanticsConfiguration(config)
        assertThat(config.getActionHandler(SemanticsAction.tap), `is`(notNullValue()))
        assertThat(config.getActionHandler(SemanticsAction.scrollLeft), `is`(notNullValue()))
        assertThat(config.getActionHandler(SemanticsAction.scrollRight), `is`(notNullValue()))

        config = SemanticsConfiguration()
        renderObj.validActions = setOf(SemanticsAction.tap, SemanticsAction.scrollLeft)

        renderObj.describeSemanticsConfiguration(config)
        assertThat(config.getActionHandler(SemanticsAction.tap), `is`(notNullValue()))
        assertThat(config.getActionHandler(SemanticsAction.scrollLeft), `is`(notNullValue()))
        assertThat(config.getActionHandler(SemanticsAction.scrollRight), `is`(nullValue()))
    }
}