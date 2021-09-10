/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler

import androidx.room.compiler.processing.util.compiler.steps.RawDiagnosticMessage
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import javax.tools.Diagnostic

/**
 * Custom message collector for Kotlin compilation that collects messages into
 * [RawDiagnosticMessage] objects.
 *
 * Neither KAPT nor KSP report location in the `location` parameter of the callback, instead,
 * they embed location into the messages. This collector parses these messages to recover the
 * location.
 */
internal class DiagnosticsMessageCollector : MessageCollector {
    private val diagnostics = mutableListOf<RawDiagnosticMessage>()

    fun getDiagnostics(): List<RawDiagnosticMessage> = diagnostics

    override fun clear() {
        diagnostics.clear()
    }

    override fun hasErrors(): Boolean {
        return diagnostics.any {
            it.kind == Diagnostic.Kind.ERROR
        }
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        // Both KSP and KAPT reports null location but instead put the location into the message.
        // We parse it back here to recover the location.
        val (strippedMessage, rawLocation) = if (location == null) {
            message.parseLocation() ?: message.stripPrefixes() to null
        } else {
            message.stripPrefixes() to location.toRawLocation()
        }
        diagnostics.add(
            RawDiagnosticMessage(
                kind = severity.kind,
                message = strippedMessage,
                location = rawLocation
            )
        )
    }

    /**
     * Parses the location out of a diagnostic message.
     *
     * Note that this is tailor made for KSP and KAPT where the location is reported in the first
     * line of the message.
     *
     * If location is found, this method will return the location along with the message without
     * location. Otherwise, it will return `null`.
     */
    private fun String.parseLocation(): Pair<String, RawDiagnosticMessage.Location>? {
        val firstLine = lineSequence().firstOrNull() ?: return null
        val match =
            KSP_LOCATION_REGEX.find(firstLine) ?: KAPT_LOCATION_AND_KIND_REGEX.find(firstLine)
                ?: return null
        if (match.groups.size != 4) return null
        return substring(match.range.last + 1) to RawDiagnosticMessage.Location(
            path = match.groupValues[1],
            line = match.groupValues[3].toInt(),
        )
    }

    /**
     * Removes prefixes added by kapt / ksp from the message
     */
    private fun String.stripPrefixes(): String {
        return stripKind().stripKspPrefix()
    }

    /**
     * KAPT prepends the message kind to the message, we'll remove it here.
     */
    private fun String.stripKind(): String {
        val firstLine = lineSequence().firstOrNull() ?: return this
        val match = KIND_REGEX.find(firstLine) ?: return this
        return substring(match.range.last + 1)
    }

    /**
     * KSP prepends ksp to each message, we'll strip it here.
     */
    private fun String.stripKspPrefix(): String {
        val firstLine = lineSequence().firstOrNull() ?: return this
        val match = KSP_PREFIX_REGEX.find(firstLine) ?: return this
        return substring(match.range.last + 1)
    }

    private fun CompilerMessageSourceLocation.toRawLocation(): RawDiagnosticMessage.Location {
        return RawDiagnosticMessage.Location(
            line = this.line,
            path = this.path
        )
    }

    private val CompilerMessageSeverity.kind
        get() = when (this) {
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.EXCEPTION -> Diagnostic.Kind.ERROR
            CompilerMessageSeverity.INFO,
            CompilerMessageSeverity.LOGGING -> Diagnostic.Kind.NOTE
            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.STRONG_WARNING -> Diagnostic.Kind.WARNING
            else -> Diagnostic.Kind.OTHER
        }

    companion object {
        // example: foo/bar/Subject.kt:2: warning: the real message
        private val KAPT_LOCATION_AND_KIND_REGEX = """^(.*\.(kt|java)):(\d+): \w+: """.toRegex()
        // example: [ksp] /foo/bar/Subject.kt:3: the real message
        private val KSP_LOCATION_REGEX = """^\[ksp] (.*\.(kt|java)):(\d+): """.toRegex()

        // detect things like "Note: " to be stripped from the message.
        // We could limit this to known diagnostic kinds (instead of matching \w:) but it is always
        // added so not really necessary until we hit a parser bug :)
        // example: "error: the real message"
        private val KIND_REGEX = """^\w+: """.toRegex()
        // example: "[ksp] the real message"
        private val KSP_PREFIX_REGEX = """^\[ksp] """.toRegex()
    }
}