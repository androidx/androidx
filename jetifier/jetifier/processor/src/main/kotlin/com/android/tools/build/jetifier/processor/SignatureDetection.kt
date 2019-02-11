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

package com.android.tools.build.jetifier.processor

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

private val signatureFilePattern = Pattern.compile(
    "^(/|\\\\)*meta-inf(/|\\\\)[^/\\\\]*\\.(SF|DSA|RSA|(SIG(-[^.]*)?))$",
    Pattern.CASE_INSENSITIVE
)

private val manifestPattern = Pattern.compile(
    "^(/|\\\\)*meta-inf(/|\\\\)manifest\\.mf$",
    Pattern.CASE_INSENSITIVE
)

private val manifestSignatureDataPattern = Pattern.compile(
    "(SHA1|SHA-1|SHA256|SHA-256)-Digest",
    Pattern.CASE_INSENSITIVE
)

fun isSignatureFile(file: ArchiveFile): Boolean {
    if (signatureFilePattern.matcher(file.relativePath.toString()).matches()) {
        return true
    }

    if (!manifestPattern.matcher(file.relativePath.toString()).matches()) {
        return false
    }

    val content = StringBuilder(file.data.toString(StandardCharsets.UTF_8)).toString()
    return manifestSignatureDataPattern.matcher(content).find()
}

class SignatureFilesFoundJetifierException(message: String) : Exception(message)