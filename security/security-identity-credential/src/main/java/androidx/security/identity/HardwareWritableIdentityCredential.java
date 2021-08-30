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

package androidx.security.identity;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

@RequiresApi(Build.VERSION_CODES.R)
class HardwareWritableIdentityCredential extends WritableIdentityCredential {

    private static final String TAG = "HardwareWritableIdentityCredential";

    android.security.identity.WritableIdentityCredential mWritableCredential = null;

    HardwareWritableIdentityCredential(
            android.security.identity.WritableIdentityCredential writableCredential) {
        mWritableCredential = writableCredential;
    }

    @Override
    public @NonNull Collection<X509Certificate> getCredentialKeyCertificateChain(
            @NonNull byte[] challenge) {
        return mWritableCredential.getCredentialKeyCertificateChain(challenge);
    }

    static @NonNull android.security.identity.PersonalizationData convertPDFromJetpack(
            @NonNull PersonalizationData personalizationData) {

        android.security.identity.PersonalizationData.Builder builder =
                new android.security.identity.PersonalizationData.Builder();
        for (PersonalizationData.NamespaceData nsData : personalizationData.getNamespaceDatas()) {
            for (String entryName : nsData.getEntryNames()) {
                ArrayList<android.security.identity.AccessControlProfileId> acpIds =
                        new ArrayList<>();
                for (AccessControlProfileId id : nsData.getAccessControlProfileIds(entryName)) {
                    acpIds.add(new android.security.identity.AccessControlProfileId(id.getId()));
                }
                builder.putEntry(nsData.getNamespaceName(),
                        entryName,
                        acpIds,
                        nsData.getEntryValue(entryName));
            }
        }

        for (AccessControlProfile profile : personalizationData.getAccessControlProfiles()) {
            android.security.identity.AccessControlProfileId id =
                    new android.security.identity.AccessControlProfileId(
                            profile.getAccessControlProfileId().getId());
            android.security.identity.AccessControlProfile.Builder profileBuilder =
                    new android.security.identity.AccessControlProfile.Builder(id);
            profileBuilder.setReaderCertificate(profile.getReaderCertificate());
            profileBuilder.setUserAuthenticationTimeout(profile.getUserAuthenticationTimeout());
            profileBuilder.setUserAuthenticationRequired(profile.isUserAuthenticationRequired());
            builder.addAccessControlProfile(profileBuilder.build());
        }

        return builder.build();
    }

    @Override
    @NonNull
    public byte[] personalize(@NonNull PersonalizationData personalizationData) {
        return mWritableCredential.personalize(convertPDFromJetpack(personalizationData));
    }
}
