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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drift tripwire for the embedded player's operation coverage.
 *
 * The embedded [RcPlayer] dispatches operations with hand-written `when`s over draw ops, layout
 * components, and modifiers. Opcodes dynamically read from baseline/AndroidX profiles are verified
 * against supported feature implementations or tracked in feature-specific unsupported lists with
 * TODOs.
 */
class EmbeddedOperationCoverageTest {

    @Test
    fun unsupportedOpcodesExplicitlyTrackedWithClTodos() {
        val registered = registeredOpcodes()

        // Verify that every opcode in UNSUPPORTED_OPCODES is actually registered in remote-core
        for (op in UNSUPPORTED_OPCODES) {
            assertTrue(
                "Unsupported opcode $op should be registered in remote-core",
                op in registered,
            )
        }
    }

    @Test
    fun testParticleOperationsNotSupportedYet() {
        // TODO(aosp/4156325): Remove this temporary assertion and enable particle system operations
        // when aosp/4156325 lands.
        for (op in UNSUPPORTED_PARTICLES_OPCODES) {
            assertFalse(
                "Particle op $op should not be supported in embedded player yet on main",
                isDrawOpSupported(op),
            )
        }
    }

    @Test
    fun testCustomComponentNotSupportedYet() {
        // TODO(aosp/4156908): Remove this temporary assertion and enable custom component support
        // when aosp/4156908 lands.
        for (op in UNSUPPORTED_CUSTOM_COMPONENT_OPCODES) {
            assertFalse(
                "Custom component op $op should not be supported in embedded player yet on main",
                isComponentSupported(op),
            )
        }
    }

    @Test
    fun testCollapsibleLayoutsNotSupportedYet() {
        // TODO(aosp/4156326): Remove this temporary assertion and enable collapsible layout support
        // when aosp/4156326 lands.
        for (op in UNSUPPORTED_COLLAPSIBLE_LAYOUT_OPCODES) {
            assertFalse(
                "Collapsible layout op $op should not be supported in embedded player yet on main",
                isComponentSupported(op),
            )
        }
    }

    @Test
    fun testMarqueeModifierNotSupportedYet() {
        // TODO(aosp/4156327): Remove this temporary assertion and enable marquee modifier support
        // when aosp/4156327 lands.
        for (op in UNSUPPORTED_MARQUEE_OPCODES) {
            assertFalse(
                "Marquee modifier op $op should not be supported in embedded player yet on main",
                isModifierSupported(op),
            )
        }
    }

    private fun isDrawOpSupported(opCode: Int): Boolean {
        // ParticlesLoop (188), ParticlesCreate (187), ParticlesCompare (189)
        return opCode !in UNSUPPORTED_PARTICLES_OPCODES
    }

    private fun isComponentSupported(opCode: Int): Boolean {
        // Custom (93), CollapsibleColumn (233), CollapsibleRow (230)
        return opCode !in UNSUPPORTED_CUSTOM_COMPONENT_OPCODES &&
            opCode !in UNSUPPORTED_COLLAPSIBLE_LAYOUT_OPCODES
    }

    private fun isModifierSupported(opCode: Int): Boolean {
        // MarqueeModifier (217)
        return opCode !in UNSUPPORTED_MARQUEE_OPCODES
    }

    private fun registeredOpcodes(): Set<Int> {
        val profiles =
            RcProfiles.PROFILE_BASELINE or
                RcProfiles.PROFILE_ANDROIDX or
                RcProfiles.PROFILE_EXPERIMENTAL
        return (0..MAX_OPCODE).filter { Operations.valid(it, API_LEVEL, profiles) }.toSet()
    }

    companion object {
        private const val API_LEVEL = 7
        private const val MAX_OPCODE = 255

        // TODO(aosp/4156325): ParticlesLoop (188), ParticlesCreate (187), ParticlesCompare (189)
        // Enable when particle system lands in aosp/4156325.
        private val UNSUPPORTED_PARTICLES_OPCODES = setOf(187, 188, 189)

        // TODO(aosp/4156908): Custom component (LAYOUT_CUSTOM = 93)
        // Enable when custom components land in aosp/4156908.
        private val UNSUPPORTED_CUSTOM_COMPONENT_OPCODES = setOf(93)

        // TODO(aosp/4156326): CollapsibleRow (230), CollapsibleColumn (233)
        // Enable when priority-aware collapsible layouts land in aosp/4156326.
        private val UNSUPPORTED_COLLAPSIBLE_LAYOUT_OPCODES = setOf(230, 233)

        // TODO(aosp/4156327): MarqueeModifier (217)
        // Enable when marquee modifier lands in aosp/4156327.
        private val UNSUPPORTED_MARQUEE_OPCODES = setOf(217)

        private val UNSUPPORTED_OPCODES =
            UNSUPPORTED_PARTICLES_OPCODES +
                UNSUPPORTED_CUSTOM_COMPONENT_OPCODES +
                UNSUPPORTED_COLLAPSIBLE_LAYOUT_OPCODES +
                UNSUPPORTED_MARQUEE_OPCODES
    }
}
