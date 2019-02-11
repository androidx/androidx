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

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.SignatureFilesFoundJetifierException
import com.android.tools.build.jetifier.processor.archive.Archive
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File

class SignatureIntegrationTest {

    private val signedLib = File(
        javaClass.getResource("/signatureDetectionTest/signedLibrary.jar").file)

    @Test
    fun archiveWithSignature_notJetified_shouldBeOk() {
        val processor = Processor.createProcessor3(
            // Since we give empty config, no jetification can happen. Thus jetifier thinks that
            // the library is not affected by it.
            Config.fromOptional()
        )

        val toFile = File.createTempFile("signatureTestResult.jar", "test")

        processor.transform(input = setOf(FileMapping(from = signedLib, to = toFile)))

        // Make sure that signatures were not stripped out
        val archive = Archive.Builder.extract(toFile)
        val mf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/MANIFEST.MF" }
        val rsa = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.RSA" }
        val sf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.SF" }
        Truth.assertThat(mf).isNotNull()
        Truth.assertThat(rsa).isNotNull()
        Truth.assertThat(sf).isNotNull()
    }

    @Test
    fun archiveWithSignature_notJetified_stripRequired_shouldNotStrip() {
        val processor = Processor.createProcessor3(
            Config.fromOptional(),
            stripSignatures = true)

        val toFile = File.createTempFile("signatureTestResult.jar", "test")

        processor.transform(input = setOf(FileMapping(from = signedLib, to = toFile)))

        val archive = Archive.Builder.extract(toFile)
        val mf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/MANIFEST.MF" }
        val rsa = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.RSA" }
        val sf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.SF" }
        Truth.assertThat(mf).isNotNull()
        Truth.assertThat(rsa).isNotNull()
        Truth.assertThat(sf).isNotNull()
    }

    @Test(expected = SignatureFilesFoundJetifierException::class)
    fun archiveWithSignature_andJetified_shouldThrowError() {
        val processor = Processor.createProcessor3(
            ConfigParser.loadDefaultConfig()!!
        )

        val toFile = File.createTempFile("signatureTestResult.jar", "test")

        processor.transform(input = setOf(FileMapping(from = signedLib, to = toFile)))
    }

    @Test
    fun archiveWithSignature_andJetified__stripRequired_shouldStrip() {
        val processor = Processor.createProcessor3(
            ConfigParser.loadDefaultConfig()!!,
            stripSignatures = true
        )

        val toFile = File.createTempFile("signatureTestResult.jar", "test")

        processor.transform(input = setOf(FileMapping(from = signedLib, to = toFile)))

        val archive = Archive.Builder.extract(toFile)
        val mf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/MANIFEST.MF" }
        val rsa = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.RSA" }
        val sf = archive.files.firstOrNull {
            it.relativePath.toString() == "META-INF/PFOPENSO.SF" }
        Truth.assertThat(mf).isNull()
        Truth.assertThat(rsa).isNull()
        Truth.assertThat(sf).isNull()
    }
}