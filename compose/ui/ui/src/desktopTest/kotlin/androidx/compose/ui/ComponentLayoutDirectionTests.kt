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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth
import java.awt.Component
import java.awt.ComponentOrientation
import java.util.Locale
import javax.swing.JFrame
import javax.swing.SwingUtilities
import org.junit.Test


class ComponentLayoutDirectionTests {

    private val WindowTestMethods = ComponentTestMethods(
        create = { ComposeWindow() },
        setContent = { setContent{ it() } },
        display = { isVisible = true },
        dispose = { dispose() }
    )

    @Test
    fun `window componentOrientation modifies LayoutDirection`() =
        componentOrientationModifiesLayoutDirection(WindowTestMethods)

    @Test
    fun `window locale modifies LayoutDirection`() =
        localeModifiesLayoutDirection(WindowTestMethods)

    @Test
    fun `window component orientation overrides locale for LayoutDirection`() =
        componentOrientationOverridesLocaleForLayoutDirection(WindowTestMethods)

    @Test
    fun `window locale does not override component orientation for LayoutDirection`() =
        localeDoesNotOverrideComponentOrientationForLayoutDirection(WindowTestMethods)

    private val DialogWindowTestMethods = ComponentTestMethods(
        create = { ComposeDialog() },
        setContent = { setContent{ it() } },
        display = { isVisible = true },
        dispose = { dispose() }
    )

    @Test
    fun `dialog componentOrientation modifies LayoutDirection`() =
        componentOrientationModifiesLayoutDirection(DialogWindowTestMethods)

    @Test
    fun `dialog locale modifies LayoutDirection`() =
        localeModifiesLayoutDirection(DialogWindowTestMethods)

    @Test
    fun `dialog component orientation overrides locale for LayoutDirection`() =
        componentOrientationOverridesLocaleForLayoutDirection(DialogWindowTestMethods)

    @Test
    fun `dialog locale does not override component orientation for LayoutDirection`() =
        localeDoesNotOverrideComponentOrientationForLayoutDirection(DialogWindowTestMethods)

    private val PanelTestMethods = ComponentTestMethods(
        create = {
            val frame = JFrame("Hello")
            val panel = ComposePanel()
            frame.contentPane.add(panel)
            panel
        },
        setContent = { setContent{ it() } },
        display = {
            SwingUtilities.getWindowAncestor(this).isVisible = true
        },
        dispose = { SwingUtilities.getWindowAncestor(this).dispose() }
    )

    @Test
    fun `panel componentOrientation modifies LayoutDirection`() =
        componentOrientationModifiesLayoutDirection(PanelTestMethods)

    @Test
    fun `panel locale modifies LayoutDirection`() =
        localeModifiesLayoutDirection(PanelTestMethods)

    @Test
    fun `panel component orientation overrides locale for LayoutDirection`() =
        componentOrientationOverridesLocaleForLayoutDirection(PanelTestMethods)

    @Test
    fun `panel locale does not override component orientation for LayoutDirection`() =
        localeDoesNotOverrideComponentOrientationForLayoutDirection(PanelTestMethods)
}

private class ComponentTestMethods<C: Component, S: Any>(
    val create: () -> C,
    val setContent: C.(content: @Composable S.() -> Unit) -> Unit,
    val display: C.() -> Unit,
    val dispose: C.() -> Unit
)

private fun <C: Component, S: Any> componentOrientationModifiesLayoutDirection(
    componentTestMethods: ComponentTestMethods<C, S>
) = with(componentTestMethods) {
    runApplicationTest {
        lateinit var localLayoutDirection: LayoutDirection
        lateinit var component: C

        launchTestApplication {
            component = create()
            component.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT

            component.setContent {
                localLayoutDirection = LocalLayoutDirection.current
            }
            component.display()
        }
        awaitIdle()

        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the orientation changes the layout direction
        component.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
        awaitIdle()
        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Ltr)

        component.dispose()
    }
}

private fun <C: Component, S: Any> localeModifiesLayoutDirection(
    componentTestMethods: ComponentTestMethods<C, S>
) = with(componentTestMethods) {
    runApplicationTest {
        lateinit var localLayoutDirection: LayoutDirection
        lateinit var component: C

        launchTestApplication {
            component = create()
            component.locale = Locale("he")

            component.setContent {
                localLayoutDirection = LocalLayoutDirection.current
            }
            component.display()
        }
        awaitIdle()

        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the locale changes the layout direction
        component.locale = Locale("en")
        awaitIdle()
        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Ltr)

        component.dispose()
    }
}

private fun <C: Component, S: Any> componentOrientationOverridesLocaleForLayoutDirection(
    componentTestMethods: ComponentTestMethods<C, S>
) = with(componentTestMethods) {
    runApplicationTest {
        lateinit var localLayoutDirection: LayoutDirection
        lateinit var component: C

        launchTestApplication {
            component = create()
            component.locale = Locale("he")

            component.setContent {
                localLayoutDirection = LocalLayoutDirection.current
            }
            component.display()
        }
        awaitIdle()

        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the orientation changes the layout direction
        component.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
        awaitIdle()
        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Ltr)

        component.dispose()
    }
}

private fun  <C: Component, S: Any> localeDoesNotOverrideComponentOrientationForLayoutDirection(
    componentTestMethods: ComponentTestMethods<C, S>
) = with(componentTestMethods) {
    runApplicationTest {
        lateinit var localLayoutDirection: LayoutDirection
        lateinit var component: C

        launchTestApplication {
            component = create()
            component.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT

            component.setContent {
                localLayoutDirection = LocalLayoutDirection.current
            }
            component.display()
        }
        awaitIdle()

        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the locale doesn't change the layout direction
        component.locale = Locale("en")
        awaitIdle()
        Truth.assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        component.dispose()
    }
}
