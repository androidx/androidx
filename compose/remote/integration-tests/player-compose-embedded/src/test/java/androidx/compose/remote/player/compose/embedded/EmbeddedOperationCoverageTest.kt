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
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drift tripwire for the embedded player's operation coverage.
 *
 * The embedded [RcPlayer] dispatches operations with hand-written `when`s that have no compiler
 * exhaustiveness guarantee — `RcPlayerDrawing.executeOperations` over draw ops (no `else`, so an
 * unhandled op is *silently dropped*), and the component dispatch in `RcPlayer` over layout
 * managers. So when remote-core registers a new operation, the embedded player can fall out of sync
 * without any signal (this is what `operation_coverage.md` tried to track by hand).
 *
 * This test snapshots the set of opcodes remote-core registers. When core adds or removes an op the
 * set changes and this test fails. On failure: make sure the new op is handled by the embedded
 * player (`RcPlayerDrawing.executeOperations` for a draw op, the component `when` in `RcPlayer` for
 * a layout) — or confirm it intentionally renders nothing — and then update [GOLDEN_OPCODES] below.
 * The point is that the decision is made consciously, not silently skipped.
 */
class EmbeddedOperationCoverageTest {

    @Test
    fun coreRegisteredOpcodeSetMatchesAuditedGolden() {
        val registered = registeredOpcodes()
        assertEquals(
            "remote-core's registered operation set changed. Handle the added/removed op(s) in the " +
                "embedded player (RcPlayerDrawing.executeOperations / RcPlayer component dispatch) or " +
                "confirm they render nothing, then update GOLDEN_OPCODES.",
            GOLDEN_OPCODES.sorted(),
            registered.sorted(),
        )
    }

    private fun registeredOpcodes(): Set<Int> {
        // OR together the profiles this build defines (others, e.g. android-native, are defined
        // externally and throw) so the snapshot covers the full in-tree op set, not one slice.
        val profiles =
            RcProfiles.PROFILE_BASELINE or
                RcProfiles.PROFILE_EXPERIMENTAL or
                RcProfiles.PROFILE_DEPRECATED or
                RcProfiles.PROFILE_WIDGETS or
                RcProfiles.PROFILE_ANDROIDX
        return (0..MAX_OPCODE).filter { Operations.valid(it, API_LEVEL, profiles) }.toSet()
    }

    companion object {
        private const val API_LEVEL = 7
        private const val MAX_OPCODE = 255

        /**
         * The operation set the embedded player has been audited against (remote-core opcodes
         * registered for the in-tree profiles at API level 7). See the class doc — when this list
         * needs to change, that's a prompt to check embedded-player coverage of the changed op(s).
         */
        private val GOLDEN_OPCODES =
            setOf(
                0,
                2,
                14,
                16,
                38,
                39,
                40,
                42,
                43,
                44,
                46,
                47,
                48,
                49,
                51,
                52,
                53,
                54,
                55,
                56,
                58,
                59,
                63,
                64,
                65,
                66,
                67,
                80,
                81,
                83,
                101,
                102,
                103,
                107,
                108,
                123,
                124,
                125,
                126,
                127,
                128,
                129,
                130,
                131,
                133,
                134,
                135,
                136,
                137,
                138,
                139,
                140,
                141,
                142,
                143,
                144,
                145,
                146,
                147,
                148,
                149,
                150,
                151,
                152,
                153,
                154,
                155,
                156,
                157,
                158,
                159,
                160,
                161,
                163,
                164,
                165,
                166,
                167,
                168,
                169,
                170,
                171,
                172,
                173,
                174,
                175,
                176,
                177,
                178,
                179,
                180,
                181,
                182,
                183,
                184,
                185,
                186,
                187,
                188,
                190,
                191,
                192,
                193,
                194,
                196,
                197,
                198,
                199,
                200,
                201,
                202,
                203,
                204,
                205,
                206,
                207,
                208,
                209,
                210,
                211,
                212,
                213,
                214,
                215,
                216,
                217,
                218,
                219,
                220,
                221,
                222,
                223,
                224,
                225,
                226,
                227,
                228,
                229,
                230,
                231,
                232,
                233,
                234,
                235,
                236,
                237,
                238,
                239,
                240,
                241,
                242,
                243,
                244,
                245,
                246,
                247,
                248,
                249,
                250,
            )
    }
}
