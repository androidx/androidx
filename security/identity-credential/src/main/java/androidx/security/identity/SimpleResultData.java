/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of {@link ResultData} which stores a copy of all data.
 */
class SimpleResultData extends ResultData {

    protected byte[] mStaticAuthenticationData = null;
    protected byte[] mAuthenticatedData = null;
    protected byte[] mEcdsaSignature = null;
    protected byte[] mMessageAuthenticationCode = null;

    protected Map<String, Map<String, EntryData>> mData = new LinkedHashMap<>();

    private static class EntryData {
        @Status
        int mStatus;
        byte[] mValue;

        EntryData(byte[] value, @Status int status) {
            this.mValue = value;
            this.mStatus = status;
        }
    }

    SimpleResultData() {}

    @Override
    public @NonNull byte[] getAuthenticatedData() {
        return mAuthenticatedData;
    }

    @Override
    public @Nullable byte[] getMessageAuthenticationCode() {
        return mMessageAuthenticationCode;
    }

    @Override
    public @Nullable byte[] getEcdsaSignature() {
        return mEcdsaSignature;
    }

    @Override
    public @NonNull byte[] getStaticAuthenticationData() {
        return mStaticAuthenticationData;
    }

    @Override
    public @NonNull Collection<String> getNamespaces() {
        return Collections.unmodifiableCollection(mData.keySet());
    }

    @Override
    public @Nullable Collection<String> getEntryNames(@NonNull String namespaceName) {
        Map<String, EntryData> innerMap = mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        return Collections.unmodifiableCollection(innerMap.keySet());
    }

    @Override
    public @Nullable Collection<String> getRetrievedEntryNames(@NonNull String namespaceName) {
        Map<String, EntryData> innerMap = mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        for (Map.Entry<String, EntryData> entry : innerMap.entrySet()) {
            if (entry.getValue().mStatus == STATUS_OK) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private EntryData getEntryData(@NonNull String namespaceName, @NonNull String name) {
        Map<String, EntryData> innerMap = mData.get(namespaceName);
        if (innerMap == null) {
            return null;
        }
        return innerMap.get(name);
    }

    @Override
    @Status
    public int getStatus(@NonNull String namespaceName, @NonNull String name) {
        EntryData value = getEntryData(namespaceName, name);
        if (value == null) {
            return STATUS_NOT_REQUESTED;
        }
        return value.mStatus;
    }

    @Override
    public @Nullable byte[] getEntry(@NonNull String namespaceName, @NonNull String name) {
        EntryData value = getEntryData(namespaceName, name);
        if (value == null) {
            return null;
        }
        return value.mValue;
    }

    static class Builder {
        private SimpleResultData mResultData;

        Builder() {
            this.mResultData = new SimpleResultData();
        }

        Builder setStaticAuthenticationData(byte[] staticAuthenticationData) {
            this.mResultData.mStaticAuthenticationData = staticAuthenticationData;
            return this;
        }

        Builder setAuthenticatedData(byte[] authenticatedData) {
            this.mResultData.mAuthenticatedData = authenticatedData;
            return this;
        }

        Builder setEcdsaSignature(byte[] ecdsaSignature) {
            this.mResultData.mEcdsaSignature = ecdsaSignature;
            return this;
        }

        Builder setMessageAuthenticationCode(byte [] messageAuthenticationCode) {
            this.mResultData.mMessageAuthenticationCode = messageAuthenticationCode;
            return this;
        }

        private Map<String, EntryData> getOrCreateInnerMap(String namespaceName) {
            Map<String, EntryData> innerMap = mResultData.mData.get(namespaceName);
            if (innerMap == null) {
                innerMap = new LinkedHashMap<>();
                mResultData.mData.put(namespaceName, innerMap);
            }
            return innerMap;
        }

        Builder addEntry(String namespaceName, String name, byte[] value) {
            Map<String, EntryData> innerMap = getOrCreateInnerMap(namespaceName);
            innerMap.put(name, new EntryData(value, STATUS_OK));
            return this;
        }

        Builder addErrorStatus(String namespaceName, String name, @Status int status) {
            Map<String, EntryData> innerMap = getOrCreateInnerMap(namespaceName);
            innerMap.put(name, new EntryData(null, status));
            return this;
        }

        SimpleResultData build() {
            return mResultData;
        }
    }

}
