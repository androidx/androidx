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

package androidx.core.appdigest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApkChecksum;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@SuppressWarnings("unchecked")
@RequiresApi(api = Build.VERSION_CODES.S)
class ChecksumsApiSImpl {
    private static final String TAG = "ChecksumsApiSImpl";

    private static final String APK_FILE_EXTENSION = ".apk";
    private static final String DIGESTS_FILE_EXTENSION = ".digests";
    private static final String DIGESTS_SIGNATURE_FILE_EXTENSION = ".signature";

    private ChecksumsApiSImpl() {}

    static @Nullable String getInstallerPackageName(@NonNull Context context,
            @NonNull String packageName) throws PackageManager.NameNotFoundException {
        InstallSourceInfo installSourceInfo = context.getPackageManager().getInstallSourceInfo(
                packageName);
        return installSourceInfo != null ? installSourceInfo.getInitiatingPackageName() : null;
    }

    static @NonNull ListenableFuture<Checksum[]> getChecksums(@NonNull Context context,
            @NonNull String packageName, boolean includeSplits, @Checksum.Type int required,
            @NonNull List<Certificate> trustedInstallers, @NonNull Executor executor)
            throws CertificateEncodingException, PackageManager.NameNotFoundException {
        final ResolvableFuture<Checksum[]> result = ResolvableFuture.create();

        if (trustedInstallers == Checksums.TRUST_ALL) {
            trustedInstallers = PackageManager.TRUST_ALL;
        } else if (trustedInstallers == Checksums.TRUST_NONE) {
            trustedInstallers = PackageManager.TRUST_NONE;
        } else if (trustedInstallers.isEmpty()) {
            throw new IllegalArgumentException(
                    "trustedInstallers has to be one of TRUST_ALL/TRUST_NONE or a non-empty "
                            + "list of certificates.");
        }

        context.getPackageManager().requestChecksums(packageName, includeSplits, required,
                trustedInstallers, new PackageManager.OnChecksumsReadyListener() {
                    @SuppressLint({"WrongConstant"})
                    @Override
                    public void onChecksumsReady(List<ApkChecksum> apkChecksums) {
                        if (apkChecksums == null) {
                            result.setException(
                                    new IllegalStateException("Checksums missing."));
                            return;
                        }

                        try {
                            Checksum[] checksums = new Checksum[apkChecksums.size()];
                            for (int i = 0, size = apkChecksums.size(); i < size; ++i) {
                                ApkChecksum apkChecksum = apkChecksums.get(i);
                                checksums[i] = new Checksum(apkChecksum.getSplitName(),
                                        apkChecksum.getType(), apkChecksum.getValue(),
                                        apkChecksum.getInstallerPackageName(),
                                        apkChecksum.getInstallerCertificate());
                            }
                            result.set(checksums);
                        } catch (Throwable e) {
                            result.setException(e);
                        }
                    }
                });

        return result;
    }

    @SuppressLint("WrongConstant")
    static void getInstallerChecksums(@NonNull Context context,
            String split, File file,
            @Checksum.Type int required,
            @Nullable String installerPackageName,
            @Nullable List<Certificate> trustedInstallers,
            SparseArray<Checksum> checksums) {
        if (trustedInstallers == Checksums.TRUST_ALL) {
            trustedInstallers = null;
        } else if (trustedInstallers == Checksums.TRUST_NONE) {
            return;
        } else if (trustedInstallers == null || trustedInstallers.isEmpty()) {
            throw new IllegalArgumentException(
                    "trustedInstallers has to be one of TRUST_ALL/TRUST_NONE or a non-empty "
                            + "list of certificates.");
        }

        final File digestsFile = findDigestsForFile(file);
        if (digestsFile == null) {
            return;
        }
        final File signatureFile = findSignatureForDigests(digestsFile);

        try {
            final android.content.pm.Checksum[] digests = readChecksums(digestsFile);
            final Signature[] certs;
            final Signature[] pastCerts;

            if (signatureFile != null) {
                final Certificate[] certificates = verifySignature(digests,
                        Files.readAllBytes(signatureFile.toPath()));
                if (certificates == null || certificates.length == 0) {
                    Log.e(TAG, "Error validating signature");
                    return;
                }

                certs = new Signature[certificates.length];
                for (int i = 0, size = certificates.length; i < size; i++) {
                    certs[i] = new Signature(certificates[i].getEncoded());
                }

                pastCerts = null;
            } else {
                if (TextUtils.isEmpty(installerPackageName)) {
                    Log.e(TAG, "Installer package is not specified.");
                    return;
                }
                final PackageInfo installer =
                        context.getPackageManager().getPackageInfo(installerPackageName,
                                PackageManager.GET_SIGNING_CERTIFICATES);
                if (installer == null) {
                    Log.e(TAG, "Installer package not found.");
                    return;
                }

                // Obtaining array of certificates used for signing the installer package.
                certs = installer.signingInfo.getApkContentsSigners();
                pastCerts = installer.signingInfo.getSigningCertificateHistory();
            }
            if (certs == null || certs.length == 0 || certs[0] == null) {
                Log.e(TAG, "Can't obtain certificates.");
                return;
            }

            // According to V2/V3 signing schema, the first certificate corresponds to the public
            // key in the signing block.
            byte[] trustedCertBytes = certs[0].toByteArray();

            final Set<Signature> trusted = convertToSet(trustedInstallers);

            if (trusted != null && !trusted.isEmpty()) {
                // Obtaining array of certificates used for signing the installer package.
                Signature trustedCert = isTrusted(certs, trusted);
                if (trustedCert == null) {
                    trustedCert = isTrusted(pastCerts, trusted);
                }
                if (trustedCert == null) {
                    return;
                }
                trustedCertBytes = trustedCert.toByteArray();
            }

            // Append missing digests.
            for (android.content.pm.Checksum digest : digests) {
                int type = digest.getType();
                if (checksums.indexOfKey(type) < 0) {
                    checksums.put(type,
                            new Checksum(split, type, digest.getValue(),
                                    installerPackageName, trustedCertBytes));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Installer package not found.", e);
        } catch (IOException e) {
            Log.e(TAG, "Error reading .digests or .signature", e);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidParameterException e) {
            Log.e(TAG, "Error validating digests", e);
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "Error encoding trustedInstallers", e);
        }
    }

    private static String buildDigestsPathForApk(String codePath) {
        if (!codePath.endsWith(APK_FILE_EXTENSION)) {
            throw new IllegalStateException("Code path is not an apk " + codePath);
        }
        return codePath.substring(0, codePath.length() - APK_FILE_EXTENSION.length())
                + DIGESTS_FILE_EXTENSION;
    }

    private static String buildSignaturePathForDigests(String digestsPath) {
        return digestsPath + DIGESTS_SIGNATURE_FILE_EXTENSION;
    }

    private static File findDigestsForFile(File targetFile) {
        String digestsPath = buildDigestsPathForApk(targetFile.getAbsolutePath());
        File digestsFile = new File(digestsPath);
        return digestsFile.exists() ? digestsFile : null;
    }

    private static File findSignatureForDigests(File digestsFile) {
        String signaturePath = buildSignaturePathForDigests(digestsFile.getAbsolutePath());
        File signatureFile = new File(signaturePath);
        return signatureFile.exists() ? signatureFile : null;
    }

    private static android.content.pm.Checksum[] readChecksums(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(is)) {
            ArrayList<android.content.pm.Checksum> checksums = new ArrayList<>();
            try {
                // 100 is an arbitrary very big number. We should stop at EOF.
                for (int i = 0; i < 100; ++i) {
                    checksums.add(readFromStream(dis));
                }
            } catch (EOFException e) {
                // expected
            }
            return checksums.toArray(new android.content.pm.Checksum[checksums.size()]);
        }
    }

    private static @NonNull android.content.pm.Checksum readFromStream(@NonNull DataInputStream dis)
            throws IOException {
        final int type = dis.readInt();

        final byte[] valueBytes = new byte[dis.readInt()];
        dis.read(valueBytes);
        return new android.content.pm.Checksum(type, valueBytes);
    }

    private static void writeChecksums(OutputStream os, android.content.pm.Checksum[] checksums)
            throws IOException {
        try (DataOutputStream dos = new DataOutputStream(os)) {
            for (android.content.pm.Checksum checksum : checksums) {
                writeToStream(dos, checksum);
            }
        }
    }

    private static void writeToStream(@NonNull DataOutputStream dos,
            @NonNull android.content.pm.Checksum checksum) throws IOException {
        dos.writeInt(checksum.getType());

        final byte[] valueBytes = checksum.getValue();
        dos.writeInt(valueBytes.length);
        dos.write(valueBytes);
    }

    private static Set<Signature> convertToSet(@Nullable List<Certificate> array) throws
            CertificateEncodingException {
        if (array == null) {
            return null;
        }
        final Set<Signature> set = new ArraySet<>(array.size());
        for (Certificate item : array) {
            set.add(new Signature(item.getEncoded()));
        }
        return set;
    }

    private static Signature isTrusted(Signature[] signatures, Set<Signature> trusted) {
        if (signatures == null) {
            return null;
        }
        for (Signature signature : signatures) {
            if (trusted.contains(signature)) {
                return signature;
            }
        }
        return null;
    }

    /**
     * Verifies signature over binary serialized checksums.
     * @param checksums array of checksums
     * @param signature detached PKCS7 signature in DER format
     * @return all certificates that passed verification
     */
    private static @NonNull Certificate[] verifySignature(android.content.pm.Checksum[] checksums,
            byte[] signature) throws NoSuchAlgorithmException, IOException, SignatureException {
        final byte[] blob;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            writeChecksums(os, checksums);
            blob = os.toByteArray();
        }

        try {
            CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(blob), signature);

            SignerInformationStore signers = cms.getSignerInfos();
            if (signers == null || signers.size() == 0) {
                throw new SignatureException("Signature missing signers");
            }

            ArrayList<Certificate> certificates = new ArrayList<>(signers.size());

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (SignerInformation signer : signers) {
                for (Object certHolderObject : cms.getCertificates().getMatches(signer.getSID())) {
                    X509CertificateHolder certHolder = (X509CertificateHolder) certHolderObject;
                    if (!signer.verify(
                            new JcaSimpleSignerInfoVerifierBuilder().build(certHolder))) {
                        throw new SignatureException("Verification failed");
                    }
                    certificates.add((X509Certificate) cf.generateCertificate(
                            new ByteArrayInputStream(certHolder.getEncoded())));
                }
            }

            return certificates.toArray(new Certificate[certificates.size()]);
        } catch (CMSException | CertificateException | OperatorCreationException e) {
            throw new SignatureException("Verification exception", e);
        }
    }
}
