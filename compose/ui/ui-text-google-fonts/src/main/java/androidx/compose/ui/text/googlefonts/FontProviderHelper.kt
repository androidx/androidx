/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.googlefonts

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Resources
import androidx.annotation.WorkerThread
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.res.FontResourcesParserCompat
import java.util.Arrays

@SuppressLint("ListIterator") // this is not a hot code path, nor is it optimized
@WorkerThread
internal fun GoogleFont.Provider.checkAvailable(
    packageManager: PackageManager,
    resources: Resources,
    getSignatures: (PackageManager, String) -> List<Signature> = PackageInfoCompat::getSignatures,
): Boolean {
    // check package is available (false return paths)
    @Suppress("DEPRECATION")
    val providerInfo = packageManager.resolveContentProvider(providerAuthority, 0) ?: return false
    if (providerInfo.packageName != providerPackage) return false

    // now check signatures (true or except after this)
    val signatures =
        try {
            getSignatures(packageManager, providerInfo.packageName).map { it.toByteArray() }
        } catch (_: PackageManager.NameNotFoundException) {
            return false
        }
    if (signatures.isEmpty()) {
        throwFormattedCertsMissError(signatures)
    }
    val sortedSignatures = signatures.sortedWith(ByteArrayComparator)

    if (matchesAnyCertSet(sortedSignatures, loadCertsIfNeeded(resources))) {
        return true
    }

    if (systemCertificatesRes != 0) {
        val appInfo = providerInfo.applicationInfo
        val isSystemApp =
            appInfo != null &&
                (appInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        if (isSystemApp) {
            val systemCerts = FontResourcesParserCompat.readCerts(resources, systemCertificatesRes)
            if (matchesAnyCertSet(sortedSignatures, systemCerts)) {
                return true
            }
        }
    }

    throwFormattedCertsMissError(signatures)
}

@SuppressLint("ListIterator") // not a hot code path, not optimized
private fun matchesAnyCertSet(
    sortedSignatures: List<ByteArray>,
    expectedCertSets: List<List<ByteArray?>?>,
): Boolean {
    if (sortedSignatures.isEmpty()) return false
    return expectedCertSets.any { certList ->
        if (certList.isNullOrEmpty() || certList.size != sortedSignatures.size) return@any false
        val expected = certList.sortedWith(ByteArrayComparator)
        for (i in expected.indices) {
            if (!Arrays.equals(expected[i], sortedSignatures[i])) return@any false
        }
        true
    }
}

@SuppressLint("ListIterator") // not a hot code path, not optimized
private fun throwFormattedCertsMissError(signatures: List<ByteArray>): Nothing {
    val fullDescription =
        if (signatures.isEmpty()) {
            "emptyList()"
        } else {
            signatures.joinToString(",", prefix = "listOf(listOf(", postfix = "))") { repr(it) }
        }
    throw IllegalStateException(
        "Provided signatures did not match. Actual signatures of package are:\n\n$fullDescription"
    )
}

private fun repr(b: ByteArray): String {
    return b.joinToString(",", prefix = "byteArrayOf(", postfix = ")")
}

private fun GoogleFont.Provider.loadCertsIfNeeded(resources: Resources): List<List<ByteArray?>?> {
    if (certificates != null) {
        return certificates
    }
    if (certificatesRes == 0) {
        return emptyList()
    }

    return FontResourcesParserCompat.readCerts(resources, certificatesRes)
}

private val ByteArrayComparator = Comparator { l: ByteArray, r: ByteArray ->
    if (l.size != r.size) {
        return@Comparator l.size - r.size
    }
    var i = 0
    while (i < l.size) {
        if (l[i] != r[i]) {
            return@Comparator l[i] - r[i]
        }
        ++i
    }
    0
}
