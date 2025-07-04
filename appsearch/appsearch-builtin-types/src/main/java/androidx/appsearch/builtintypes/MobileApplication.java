/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import android.net.Uri;

import androidx.appsearch.annotation.CurrentTimeMillisLong;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.StringSerializer;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents an installed app to enable searching using names, nicknames, and package names. */
@ExperimentalAppSearchApi
@Document(name = "builtin:MobileApplication")
public class MobileApplication extends Thing {

    @Document.StringProperty(
            indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES,
            tokenizerType = StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
    private final String mPackageName;

    @Document.StringProperty(
            indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES
    )
    private final String mDisplayName;

    // Thing does have mAlternateNames, however they are not indexed, so we need to add it here
    @Document.StringProperty(
            indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES
    )
    private final List<String> mAlternateNames;

    @Document.StringProperty(serializer = IconUriAsUri.class) private final Uri mIconUri;

    @Document.BytesProperty private final byte[] mSha256Certificate;

    // Property name set to update to match framework
    @Document.LongProperty(name = "updatedTimestamp",
            indexingType = LongPropertyConfig.INDEXING_TYPE_RANGE)
    private final long mUpdatedTimestampMillis;

    @Document.StringProperty private final String mClassName;

    /** Constructs the {@link MobileApplication}. */
    MobileApplication(
            @NonNull String namespace,
            @NonNull String id,
            int documentScore,
            long creationTimestampMillis,
            long documentTtlMillis,
            @Nullable String name,
            @Nullable List<String> alternateNames,
            @Nullable String description,
            @Nullable String image,
            @Nullable String url,
            @NonNull List<PotentialAction> potentialActions,
            @NonNull String packageName,
            @Nullable String displayName,
            @Nullable Uri iconUri,
            byte @NonNull [] sha256Certificate,
            long updatedTimestampMillis,
            @Nullable String className) {
        super(namespace, id, documentScore, creationTimestampMillis, documentTtlMillis, name,
                alternateNames, description, image, url, potentialActions);
        mPackageName = Preconditions.checkNotNull(packageName);
        mDisplayName = displayName;
        if (alternateNames == null) {
            mAlternateNames = Collections.emptyList();
        } else {
            mAlternateNames = Collections.unmodifiableList(alternateNames);
        }
        mIconUri = iconUri;
        mSha256Certificate = Preconditions.checkNotNull(sha256Certificate);
        mUpdatedTimestampMillis = updatedTimestampMillis;
        mClassName = className;
    }

    /**
     * Returns the package name this {@link MobileApplication} represents. For example,
     * "com.android.vending".
     */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the display name of the app. This is indexed. This is what is displayed in the
     * launcher. This might look like "Play Store".
     */
    public @Nullable String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Returns alternative names of the application. These are indexed. For example, you might have
     * the alternative name "pay" for a wallet app.
     */
    @Override
    public @NonNull List<String> getAlternateNames() {
        return mAlternateNames;
    }

    /**
     * Returns the full name of the resource identifier of the app icon, which can be used for
     * displaying results. The Uri could be
     * "android.resource://com.example.vending/drawable/2131230871", for example.
     */
    public @Nullable Uri getIconUri() {
        return mIconUri;
    }

    /** Returns the SHA-256 certificate of the application. */
    public byte @NonNull [] getSha256Certificate() {
        return mSha256Certificate;
    }

    /** Returns the last time the app was installed or updated on the device. */
    @CurrentTimeMillisLong
    public long getUpdatedTimestampMillis() {
        return mUpdatedTimestampMillis;
    }

    /**
     * Returns the fully qualified name of the Application class for this mobile app. This would
     * look something like "com.android.vending.SearchActivity". Combined with the package name, a
     * launch intent can be created with <code>
     *     Intent launcher = new Intent(Intent.ACTION_MAIN);
     *     launcher.setComponent(new ComponentName(app.getPackageName(), app.getClassName()));
     *     launcher.setPackage(app.getPackageName());
     *     launcher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *     launcher.addCategory(Intent.CATEGORY_LAUNCHER);
     *     appListFragment.getActivity().startActivity(launcher);
     *  </code>
     */
    public @Nullable String getClassName() {
        return mClassName;
    }

    /** Builder class for {@link MobileApplication}. */
    public static final class Builder extends BuilderImpl<Builder> {
        /**
         * Constructor for {@link Builder}.
         *
         * @param id The id of the document.
         * @param namespace The namespace of the document.
         * @param packageName The package name of the application.
         * @param sha256Certificate The SHA-256 certificate of the application.
         */
        public Builder(@NonNull String id, @NonNull String namespace, @NonNull String packageName,
                byte @NonNull [] sha256Certificate) {
            super(Preconditions.checkNotNull(id), Preconditions.checkNotNull(namespace),
                    Preconditions.checkNotNull(packageName),
                    Preconditions.checkNotNull(sha256Certificate));
        }

        /**
         * Constructor for {@link Builder} with all the existing values of a {@link
         * MobileApplication}.
         */
        public Builder(@NonNull MobileApplication mobileApplication) {
            super(Preconditions.checkNotNull(mobileApplication));
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> extends Thing.BuilderImpl<T> {
        private final String mPackageName;
        private String mDisplayName;
        private Uri mIconUri;
        private final byte[] mSha256Certificate;
        private long mUpdatedTimestampMillis;
        private String mClassName;
        private boolean mBuilt = false;

        BuilderImpl(@NonNull String id, @NonNull String namespace, @NonNull String packageName,
                byte @NonNull [] sha256Certificate) {
            super(namespace, id);
            mPackageName = Preconditions.checkNotNull(packageName);
            mSha256Certificate = Preconditions.checkNotNull(sha256Certificate);
        }

        BuilderImpl(@NonNull MobileApplication mobileApplication) {
            super(new Thing.Builder(mobileApplication).build());
            Preconditions.checkNotNull(mobileApplication);
            mPackageName = mobileApplication.mPackageName;
            mDisplayName = mobileApplication.mDisplayName;
            mIconUri = mobileApplication.mIconUri;
            mSha256Certificate = mobileApplication.mSha256Certificate;
            mUpdatedTimestampMillis = mobileApplication.mUpdatedTimestampMillis;
            mClassName = mobileApplication.mClassName;
        }

        /** Sets the display name. */
        public @NonNull T setDisplayName(@NonNull String displayName) {
            resetIfBuilt();
            mDisplayName = Preconditions.checkNotNull(displayName);
            return (T) this;
        }

        /** Sets the icon uri. */
        public @NonNull T setIconUri(@NonNull Uri iconUri) {
            resetIfBuilt();
            mIconUri = Preconditions.checkNotNull(iconUri);
            return (T) this;
        }

        /** Sets the last time the app was installed or updated on the device. */
        public @NonNull T setUpdatedTimestampMillis(
                @CurrentTimeMillisLong long updatedTimestampMillis) {
            resetIfBuilt();
            mUpdatedTimestampMillis = updatedTimestampMillis;
            return (T) this;
        }

        /** Sets the class name. */
        public @NonNull T setClassName(@NonNull String className) {
            resetIfBuilt();
            mClassName = Preconditions.checkNotNull(className);
            return (T) this;
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        private void resetIfBuilt() {
            if (mBuilt) {
                mAlternateNames = new ArrayList<>(mAlternateNames);
                mBuilt = false;
            }
        }

        /** Builds the {@link MobileApplication}. */
        @Override
        public @NonNull MobileApplication build() {
            mBuilt = true;
            return new MobileApplication(
                    mNamespace,
                    mId,
                    /*documentScore=*/mDocumentScore,
                    /*creationTimestampMillis=*/ mCreationTimestampMillis,
                    /*documentTtlMillis=*/ mDocumentTtlMillis,
                    /*name=*/ mName,
                    /*alternateNames=*/ mAlternateNames,
                    /*description=*/ mDescription,
                    /*image=*/ mImage,
                    /*url=*/ mUrl,
                    /*potentialActions=*/ mPotentialActions,
                    mPackageName,
                    mDisplayName,
                    mIconUri,
                    mSha256Certificate,
                    mUpdatedTimestampMillis,
                    mClassName);
        }
    }

    /** Allows MobileApplication to store iconUri as a Uri instead of a String. */
    static class IconUriAsUri implements StringSerializer<Uri> {
        @Override
        public @NonNull String serialize(@NonNull Uri iconUri) {
            return iconUri.toString();
        }

        @Override
        public @Nullable Uri deserialize(@Nullable String value) {
            if (value == null) {
                return null;
            }
            try {
                return Uri.parse(value);
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
}
