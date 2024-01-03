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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.padding
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class LoggerNode(var log: MutableList<String>, name: String) : Modifier.Node() {
    var name: String = name
        set(value) {
            log.add("update($field -> $value)")
            field = value
        }
    override fun onAttach() {
        log.add("attach($name)")
    }

    override fun onDetach() {
        log.add("detach($name)")
    }
}

private class LoggerElement(
    val log: MutableList<String>,
    val name: String,
) : ModifierNodeElement<LoggerNode>() {
    override fun create(): LoggerNode = LoggerNode(log, name)

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is LoggerElement && other.name == name
    }

    override fun update(node: LoggerNode) {
        node.name = name
    }
}

private fun Modifier.logger(log: MutableList<String>, name: String) =
    this then LoggerElement(log, name)

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeAttachOrderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun attachOrderInitialComposition() {
        // Arrange.
        val log = mutableListOf<String>()
        val a = LoggerNode(log, "a")
        val b = LoggerNode(log, "b")
        val c = LoggerNode(log, "c")
        val d = LoggerNode(log, "d")

        rule.setContent {
            Box(modifierOf(a, b)) {
                Box(modifierOf(c, d))
            }
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(a)",
                "attach(b)",
                "attach(c)",
                "attach(d)",
            )
        }
    }

    @Test
    fun attachOrderUpdate() {
        // Arrange.
        val log = mutableListOf<String>()
        val padding = Modifier.padding(0)
        val a = LoggerNode(log, "a")
        val b = LoggerNode(log, "b")
        val c = LoggerNode(log, "c")
        val d = LoggerNode(log, "d")

        var parentChain by mutableStateOf<Modifier>(padding)
        var childChain by mutableStateOf<Modifier>(padding)

        rule.setContent {
            Box(parentChain) {
                Box(childChain)
            }
        }

        rule.runOnIdle {
            parentChain = Modifier
                .elementOf(a)
                .then(padding)
                .elementOf(b)
            childChain = Modifier
                .elementOf(c)
                .then(padding)
                .elementOf(d)
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(a)",
                "attach(b)",
                "attach(c)",
                "attach(d)",
            )
        }
    }

    @Test
    fun attachOrderWhenMiddleIsRemoved() {
        // Arrange.
        val log = mutableListOf<String>()
        var parentChain by mutableStateOf<Modifier>(
            Modifier
                .logger(log, "a")
                .logger(log, "b")
                .logger(log, "c")
        )

        rule.setContent {
            Box(parentChain)
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(a)",
                "attach(b)",
                "attach(c)",
            )
            log.clear()
        }

        rule.runOnIdle {
            parentChain = Modifier
                .logger(log, "a")
                .logger(log, "c")
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "detach(c)",
                "update(b -> c)",
            )
            log.clear()
        }

        rule.runOnIdle {
            parentChain = Modifier
                .logger(log, "a")
                .logger(log, "b")
                .logger(log, "c")
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(c)",
                "update(c -> b)",
            )
            log.clear()
        }
    }

    @Test
    fun addMultipleNodesInMiddle() {
        // Arrange.
        val log = mutableListOf<String>()
        var parentChain by mutableStateOf<Modifier>(
            Modifier
                .logger(log, "a")
                .padding(10)
                .padding(10)
                .logger(log, "z")
        )

        rule.setContent {
            Box(parentChain)
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(a)",
                "attach(z)",
            )
            log.clear()
        }

        rule.runOnIdle {
            parentChain = Modifier
                .logger(log, "a")
                .padding(10)
                .logger(log, "b")
                .logger(log, "c")
                .padding(10)
                .logger(log, "z")
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(c)",
                "attach(b)",
            )
            log.clear()
        }
    }

    @Test
    fun addMultipleNodesInMiddleMultipleLayouts() {
        // Arrange.
        val log = mutableListOf<String>()
        var parentChain by mutableStateOf<Modifier>(
            Modifier
                .logger(log, "a")
                .padding(10)
                .padding(10)
                .logger(log, "d")
        )

        var childChain by mutableStateOf<Modifier>(
            Modifier
                .logger(log, "e")
                .padding(10)
                .padding(10)
                .logger(log, "h")
        )

        rule.setContent {
            Box(parentChain) {
                Box(childChain)
            }
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                "attach(a)",
                "attach(d)",
                "attach(e)",
                "attach(h)",
            )
            log.clear()
        }

        rule.runOnIdle {
            parentChain = Modifier
                .logger(log, "a")
                .padding(10)
                .logger(log, "b")
                .logger(log, "c")
                .padding(10)
                .logger(log, "d")

            childChain = Modifier
                .logger(log, "e")
                .padding(10)
                .logger(log, "f")
                .logger(log, "g")
                .padding(10)
                .logger(log, "h")
        }

        rule.runOnIdle {
            assertThat(log).containsExactly(
                // parent updates first
                "attach(c)",
                "attach(b)",
                // then child
                "attach(g)",
                "attach(f)",
            )
            log.clear()
        }
    }

    // Regression test for b/289461011
    @Test
    fun reusable_nodes_in_movable_content() {
        var active by mutableStateOf(true)
        var inBox by mutableStateOf(true)
        val content = movableContentOf {
            ReusableContentHost(active = active) {
                BasicText("Hello World")
            }
        }

        rule.setContent {
            if (inBox) {
                Box {
                    content()
                }
            } else {
                content()
            }
        }

        rule.runOnIdle {
            active = false
        }

        rule.runOnIdle {
            inBox = false
        }

        rule.runOnIdle {
            active = true
        }

        rule.waitForIdle()
    }
}
