/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.creation

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.integration.view.demos.examples.particleSphere
import androidx.compose.remote.integration.view.demos.examples.rcJsonParticleSphere
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.ArrayList
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComposeParticleSphereTest {

    private val scratchDir: java.io.File by lazy {
        java.io.File(System.getProperty("java.io.tmpdir"), "rc-demos-test").apply { mkdirs() }
    }

    private fun scratch(name: String): java.io.File = java.io.File(scratchDir, name)

    private fun dumpOpcodesAndOffsets(
        bytes: ByteArray,
        opcodesFile: java.io.File,
        offsetsFile: java.io.File,
    ) {
        val buffer = RemoteComposeBuffer()
        RemoteComposeBuffer.read(ByteArrayInputStream(bytes), buffer)
        val operations = ArrayList<Operation>()

        val captured = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(captured))
        try {
            buffer.inflateFromBuffer(operations)
        } finally {
            System.setOut(originalOut)
        }

        opcodesFile.printWriter().use { out ->
            for (op in operations) out.println(op.deepToString("  "))
        }

        offsetsFile.printWriter().use { out ->
            captured
                .toString(Charsets.UTF_8.name())
                .lineSequence()
                .filter { it.startsWith("### OP_OFFSET: ") }
                .forEach { out.println(it.removePrefix("### OP_OFFSET: ")) }
        }
    }

    private fun dumpOnMismatch(label: String, dsl: ByteArray, json: ByteArray): Boolean {
        if (!dsl.contentEquals(json)) {
            val dslFile = scratch("${label}_dsl_bytes.bin")
            val jsonFile = scratch("${label}_json_bytes.bin")
            val dslOpcodes = scratch("${label}_dsl_opcodes.txt")
            val dslOffsets = scratch("${label}_dsl_offsets.txt")
            val jsonOpcodes = scratch("${label}_json_opcodes.txt")
            val jsonOffsets = scratch("${label}_json_offsets.txt")
            dslFile.writeBytes(dsl)
            jsonFile.writeBytes(json)
            try {
                dumpOpcodesAndOffsets(dsl, dslOpcodes, dslOffsets)
                dumpOpcodesAndOffsets(json, jsonOpcodes, jsonOffsets)
            } catch (t: Throwable) {
                println("Failed to dump opcodes: $t")
            }
            println("    ### DUMPED DSL BYTES TO: ${dslFile.absolutePath}")
            println("    ### DUMPED JSON BYTES TO: ${jsonFile.absolutePath}")
            println("    ### DUMPED DSL OPCODES TO: ${dslOpcodes.absolutePath}")
            println("    ### DUMPED JSON OPCODES TO: ${jsonOpcodes.absolutePath}")
            return true
        }
        return false
    }

    @Test
    fun testRcDslParticleSphereComparison() {
        val dslWriter = particleSphere()
        val dslBytes = java.util.Arrays.copyOf(dslWriter.buffer(), dslWriter.bufferSize())
        val jsonBytes = rcJsonParticleSphere().buffer()

        dumpOnMismatch("ParticleSphere", dslBytes, jsonBytes)
        assertArrayEquals("ParticleSphere DSL and JSON should be identical", dslBytes, jsonBytes)
    }
}
