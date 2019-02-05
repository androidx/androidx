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

package com.android.tools.build.jetifier.processor.signatures

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.isSignatureFile
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.file.Paths

class SignaturePathCheckTest {

    @Test
    fun signatureFilePath_SF() {
        Truth.assertThat(checkSignatureFor("meta-inf/hello.sf")).isTrue()
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.sf")).isTrue()
        Truth.assertThat(checkSignatureFor("/META-INF/HELLO.SF")).isTrue()
    }

    @Test
    fun signatureFilePath_DSA() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.dsa")).isTrue()
        Truth.assertThat(checkSignatureFor("/META-INF/HELLO.DSA")).isTrue()
    }

    @Test
    fun signatureFilePath_RSA() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.rsa")).isTrue()
        Truth.assertThat(checkSignatureFor("/META-INF/HELLO.RSA")).isTrue()
    }

    @Test
    fun signatureFilePath_SIG() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.sig")).isTrue()
        Truth.assertThat(checkSignatureFor("/META-INF/HELLO.SIG")).isTrue()
    }

    @Test
    fun signatureFilePath_SIG123() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.sig-123")).isTrue()
        Truth.assertThat(checkSignatureFor("/META-INF/HELLO.SIG-123")).isTrue()
    }

    @Test
    fun signatureFilePath_sf_notUnix() {
        Truth.assertThat(checkSignatureFor("\\meta-inf\\hello.sf")).isTrue()
    }

    @Test
    fun signatureFilePath_DSA_notUnix() {
        Truth.assertThat(checkSignatureFor("\\meta-inf\\hello.DSA")).isTrue()
    }

    @Test
    fun signatureFilePath_RSA_notUnix() {
        Truth.assertThat(checkSignatureFor("\\meta-inf\\hello.RSA")).isTrue()
    }

    @Test
    fun signatureFilePath_SIG_notUnix() {
        Truth.assertThat(checkSignatureFor("\\meta-inf\\hello.SIG-123")).isTrue()
    }

    @Test
    fun signatureFilePath_SF_notInMetaInf() {
        Truth.assertThat(checkSignatureFor("/not-meta-inf/hello.SF")).isFalse()
        Truth.assertThat(checkSignatureFor("/meta-inf/old/hello.SF")).isFalse()
        Truth.assertThat(checkSignatureFor("/old/meta-inf/hello.SF")).isFalse()
        Truth.assertThat(checkSignatureFor("old/meta-inf/hello.SF")).isFalse()
    }

    @Test
    fun signatureFilePath_DSA_notInMetaInf() {
        Truth.assertThat(checkSignatureFor("/not-meta-inf/hello.DSA")).isFalse()
        Truth.assertThat(checkSignatureFor("/meta-inf/old/hello.DSA")).isFalse()
    }

    @Test
    fun signatureFilePath_RSA_notInMetaInf() {
        Truth.assertThat(checkSignatureFor("/not-meta-inf/hello.RSA")).isFalse()
        Truth.assertThat(checkSignatureFor("/meta-inf/old/hello.RSA")).isFalse()
    }

    @Test
    fun signatureFilePath_SIG_notInMetaInf() {
        Truth.assertThat(checkSignatureFor("/not-meta-inf/hello.SIG-1")).isFalse()
        Truth.assertThat(checkSignatureFor("/meta-inf/old/hello.SIG-1")).isFalse()
    }

    @Test
    fun signatureFilePath_SF_onlySubstring() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.SF.nope")).isFalse()
    }

    @Test
    fun signatureFilePath_DSA_onlySubstring() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.DSA.nope")).isFalse()
    }

    @Test
    fun signatureFilePath_RSA_onlySubstring() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.RSA.nope")).isFalse()
    }

    @Test
    fun signatureFilePath_SIG_onlySubstring() {
        Truth.assertThat(checkSignatureFor("/meta-inf/hello.SIG.nope")).isFalse()
    }

    @Test
    fun signatureFilePath_manifestFile_empty() {
        Truth.assertThat(checkSignatureFor("/meta-inf/manifest.mf")).isFalse()
    }

    @Test
    fun signatureFilePath_manifestFile_notRelated() {
        Truth.assertThat(
            checkSignatureFor(
                "/meta-inf/manifest.mf",
                "Hello world!".toByteArray())
        ).isFalse()
    }

    @Test
    fun signatureFilePath_manifestFile_containingSignature() {
        Truth.assertThat(
            checkSignatureFor(
                "/meta-inf/manifest.mf",
                "SHA1-Digest: (base64 representation of SHA1 digest)".toByteArray())
        ).isTrue()
    }

    @Test
    fun signatureFilePath_manifestFile_containingSignature2() {
        Truth.assertThat(
            checkSignatureFor(
                "/meta-inf/manifest.mf",
                "SHA-256-Digest: (base64 representation of SHA-256 digest)".toByteArray())
        ).isTrue()
    }

    @Test
    fun signatureFilePath_manifestFile_notInPath_containingSignature() {
        Truth.assertThat(
            checkSignatureFor(
                "/not-meta-inf/manifest.mf",
                "SHA1-Digest: (base64 representation of SHA1 digest)".toByteArray())
        ).isFalse()
    }

    private fun checkSignatureFor(path: String, content: ByteArray = byteArrayOf()): Boolean {
        return isSignatureFile(
            ArchiveFile(
                relativePath = Paths.get(path),
                data = content
            )
        )
    }
}