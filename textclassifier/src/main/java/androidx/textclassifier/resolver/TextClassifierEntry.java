/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier.resolver;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Objects;

/**
 * Represents an entry of the text classifier specified in the xml file.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TextClassifierEntry {
    static final String AOSP = "aosp";
    static final String OEM = "oem";

    @NonNull
    public final String packageName;
    @NonNull
    public final String certificate;

    private TextClassifierEntry(String packageName, String certificate) {
        this.packageName = packageName;
        this.certificate = certificate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextClassifierEntry that = (TextClassifierEntry) o;
        return Objects.equals(packageName, that.packageName)
                && Objects.equals(certificate, that.certificate);
    }

    /**
     * Creates an entry to represent an AOSP text classifier implementation.
     */
    @NonNull
    public static TextClassifierEntry createAospEntry() {
        return new TextClassifierEntry(AOSP, AOSP);
    }

    /**
     * Creates an entry to represent an OEM text classifier implementation.
     */
    @NonNull
    public static TextClassifierEntry createOemEntry() {
        return new TextClassifierEntry(OEM, OEM);
    }

    /**
     * Creates an entry to represent a text classifier that
     * implements {@link androidx.textclassifier.TextClassifierService}.
     */
    @NonNull
    public static TextClassifierEntry createPackageEntry(
            @NonNull String packageName,
            @NonNull String signature) {
        return new TextClassifierEntry(packageName, signature);
    }

    /**
     * Returns whether it is a entry that represents the AOSP text classifier implementation.
     */
    public boolean isAosp() {
        return packageName.equals(AOSP);
    }

    /**
     * Returns whether it is a entry that represents the OEM text classifier implementation.
     */
    public boolean isOem() {
        return packageName.equals(OEM);
    }


    @Override
    public int hashCode() {
        return Objects.hash(packageName, certificate);
    }

    @Override
    public String toString() {
        return "TextClassifierEntry{"
                + "packageName='" + packageName + '\''
                + ", certificate='" + certificate + '\''
                + '}';
    }
}
