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

package androidx.appsearch.builtintypes;

import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppSearch document representing a Person entity modeled after
 * <a href="http://schema.org/Person">Person</a>.
 *
 * <p>The {@link Person} document includes commonly searchable properties such as name,
 * organization, and notes, as well as contact information such as phone numbers, email
 * addresses, etc, grouped by their label. The labeled contact information is present in a nested
 * {@link ContactPoint} document.
 */
@Document(name = "builtin:Person")
public class Person extends Thing {
    /** Holds type information for additional names for Person. */
    public static class AdditionalName {
        /** @exportToFramework:hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef({
                TYPE_UNKNOWN,
                TYPE_NICKNAME,
                TYPE_PHONETIC_NAME
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface NameType {
        }

        /** The additional name is unknown. */
        public static final int TYPE_UNKNOWN = 0;
        /** The additional name is a nickname. */
        public static final int TYPE_NICKNAME = 1;
        /** The additional name is a phonetic name. */
        public static final int TYPE_PHONETIC_NAME = 2;

        @NameType
        private final int mType;
        private final String mValue;

        /**
         * Constructs an {@link AdditionalName}.
         */
        public AdditionalName(@NameType int type,
                @NonNull String value) {
            mType = Preconditions.checkArgumentInRange(type, TYPE_UNKNOWN, TYPE_PHONETIC_NAME,
                    "type");
            mValue = value;
        }

        @NameType
        public int getType() {
            return mType;
        }

        public @NonNull String getValue() {
            return mValue;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AdditionalName)) {
                return false;
            }

            return mType == ((AdditionalName) other).mType && mValue.equals(
                    ((AdditionalName) other).mValue);
        }

        @Override
        public int hashCode() {
            String str = mType + mValue;
            return str.hashCode();
        }
    }

    @Document.StringProperty
    private final String mGivenName;

    @Document.StringProperty
    private final String mMiddleName;

    @Document.StringProperty
    private final String mFamilyName;

    @Document.StringProperty
    final String mExternalUri;

    @Document.StringProperty
    final String mImageUri;

    @Document.BooleanProperty
    final boolean mIsImportant;

    @Document.BooleanProperty
    final boolean mIsBot;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final List<String> mNotes;

    @Document.LongProperty
    final List<Long> mAdditionalNameTypes;

    @Document.StringProperty(name = "additionalNames", indexingType =
            StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    final List<String> mAdditionalNamesList;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final List<String> mAffiliations;

    @Document.StringProperty
    private final List<String> mRelations;

    @Document.DocumentProperty(indexNestedProperties = true)
    private final List<ContactPoint> mContactPoints;

    private final List<AdditionalName> mTypedAdditionalNames;

    /**
     * Constructor for {@link Person}.
     *
     * @param builder The builder to construct the {@link Person} from.
     */
    @ExperimentalAppSearchApi
    public Person(@NonNull BuilderBase<?> builder) {
        super(builder);
        mGivenName = builder.mGivenName;
        mMiddleName = builder.mMiddleName;
        mFamilyName = builder.mFamilyName;
        mExternalUri = builder.mExternalUri != null ? builder.mExternalUri.toString() : null;
        mImageUri = builder.mImageUri != null ? builder.mImageUri.toString() : null;
        mIsImportant = builder.mIsImportant;
        mIsBot = builder.mIsBot;
        mNotes = Collections.unmodifiableList(new ArrayList<>(builder.mNotes));
        mAdditionalNameTypes = Collections.unmodifiableList(
                new ArrayList<>(builder.mAdditionalNameTypes));
        mAdditionalNamesList = Collections.unmodifiableList(
                new ArrayList<>(builder.mAdditionalNamesList));
        mAffiliations = Collections.unmodifiableList(new ArrayList<>(builder.mAffiliations));
        mRelations = Collections.unmodifiableList(new ArrayList<>(builder.mRelations));
        mContactPoints = Collections.unmodifiableList(new ArrayList<>(builder.mContactPoints));

        // For the additionalNames to to returned in the getter.
        List<AdditionalName> names = new ArrayList<>(mAdditionalNameTypes.size());
        for (int i = 0; i < mAdditionalNameTypes.size(); ++i) {
            names.add(new AdditionalName(mAdditionalNameTypes.get(i).intValue(),
                    mAdditionalNamesList.get(i)));
        }
        mTypedAdditionalNames = Collections.unmodifiableList(names);
    }

    /** Returns the given (or first) name for this {@link Person}. */
    public @Nullable String getGivenName() {
        return mGivenName;
    }

    /**
     * Returns the middle name(s) for this {@link Person}.
     *
     * <p>For a Person with multiple middle names, this method returns a flattened and whitespace
     * separated list. For example, "middle1 middle2 ..."
     */
    public @Nullable String getMiddleName() {
        return mMiddleName;
    }

    /** Returns the family (or last) name for this {@link Person}. */
    public @Nullable String getFamilyName() {
        return mFamilyName;
    }

    /**
     * Returns an external uri for this {@link Person}. Or {@code null} if no {@link Uri} is
     * provided. A {@link Uri} can be any of the following:
     * <ul>
     * <li>A {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}.
     * <li>A {@code mailto:} schema*
     * <li>A {@code tel:} schema*
     * </ul>
     * <p>For mailto: and tel: URI schemes, it is recommended that the path portion
     * refers to a valid contact in the Contacts Provider.
     */
    public @Nullable Uri getExternalUri() {
        if (mExternalUri != null) {
            return Uri.parse(mExternalUri);
        }
        return null;
    }

    /** Returns {@link Uri} of the profile image for this {@link Person}. */
    public @Nullable Uri getImageUri() {
        if (mImageUri != null) {
            return Uri.parse(mImageUri);
        }
        return null;
    }

    /**
     * Returns whether this {@link Person} is important to the user of this device with
     * regards to how frequently they interact.
     */
    public boolean isImportant() {
        return mIsImportant;
    }

    /** Returns whether this {@link Person} is a machine rather than a human. */
    public boolean isBot() {
        return mIsBot;
    }

    /** Returns the notes about this {@link Person}. */
    public @NonNull List<String> getNotes() {
        return mNotes;
    }

    /**
     * Returns a list of additional names for this {@link Person}.
     *
     * <p>Additional names can include something like phonetic names, or nicknames.
     *
     * <p>Different from {@link #getTypedAdditionalNames()}, the return value doesn't include
     * type information for the additional names.
     */
    public @NonNull List<String> getAdditionalNames() {
        return mAdditionalNamesList;
    }

    /**
     * Returns a list of {@link AdditionalName} for this {@link Person}.
     *
     * <p>Additional names can include something like phonetic names, or nicknames.
     *
     * <p>Each {@link AdditionalName} contains type information for the additional name.
     */
    public @NonNull List<AdditionalName> getTypedAdditionalNames() {
        return mTypedAdditionalNames;
    }

    /**
     * Returns a list of affiliation for this {@link Person}. Like company, school, etc.
     *
     * <p>For a contact with the title "Software Engineer" in a department "Engineering" at a
     * company "Cloud Company", this can include a flattened value of "Software Engineer,
     * Engineering, Cloud Company".
     */
    public @NonNull List<String> getAffiliations() {
        return mAffiliations;
    }

    /** Returns a list of relations for this {@link Person}, like "Father" or "Mother". */
    public @NonNull List<String> getRelations() {
        return mRelations;
    }

    /**
     * Returns a list of {@link ContactPoint}.
     *
     * <p>More information can be found in {@link ContactPoint}.
     */
    public @NonNull List<ContactPoint> getContactPoints() {
        return mContactPoints;
    }

    /** Builder class for {@link Person}. */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
        /**
         * Constructor for {@link Person.Builder}.
         *
         * @param namespace Namespace for the {@link Person} Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the {@link Person} Document. See
         *                  {@link Document.Id}.
         * @param name      The searchable full name of this {@link Person}. E.g. "Larry Page", or
         *                  "Page, Larry".
         */
        public Builder(@NonNull String namespace, @NonNull String id, @NonNull String name) {
            super(namespace, id, name);
        }

        /**
         * Constructor for {@link Builder} with all the existing values of an {@link Person}.
         */
        public Builder(@NonNull Person person) {
            super(person);
        }

        @Override
        @ExperimentalAppSearchApi
        @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.SUBCLASSES})
        public @NonNull Builder setAdditionalNamesList(@NonNull List<String> additionalNamesList) {
            return super.setAdditionalNamesList(additionalNamesList);
        }

        @Override
        @ExperimentalAppSearchApi
        @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.SUBCLASSES})
        public @NonNull Builder setAdditionalNameTypes(@NonNull List<Long> additionalNameTypes) {
            return super.setAdditionalNameTypes(additionalNameTypes);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private String mGivenName;
        private String mMiddleName;
        private String mFamilyName;
        private Uri mExternalUri;
        private Uri mImageUri;
        private boolean mIsImportant;
        private boolean mIsBot;
        // Make sure the lists are not null.
        private List<String> mNotes = Collections.emptyList();
        @AdditionalName.NameType
        private List<Long> mAdditionalNameTypes = Collections.emptyList();
        private List<String> mAdditionalNamesList = Collections.emptyList();
        private List<String> mAffiliations = Collections.emptyList();
        private List<String> mRelations = Collections.emptyList();
        private List<ContactPoint> mContactPoints = Collections.emptyList();

        /**
         * Constructor for {@link Person.BuilderBase}.
         *
         * @param namespace Namespace for the {@link Person} Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the {@link Person} Document. See
         *                  {@link Document.Id}.
         * @param name      The searchable full name of this {@link Person}. E.g. "Larry Page", or
         *                  "Page, Larry".
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id, @NonNull String name) {
            super(namespace, id);
            setName(Preconditions.checkNotNull(name));
        }

        /**
         * Constructor for {@link Person.BuilderBase} with all the existing values of an
         * {@link Person}.
         *
         * @param person The existing {@link Person} to copy values from.
         */
        public BuilderBase(@NonNull Person person) {
            super(person);
            mGivenName = person.getGivenName();
            mMiddleName = person.getMiddleName();
            mFamilyName = person.getFamilyName();
            mExternalUri = person.getExternalUri();
            mImageUri = person.getImageUri();
            mIsImportant = person.isImportant();
            mIsBot = person.isBot();
            mNotes = person.getNotes();
            mAffiliations = person.getAffiliations();
            mRelations = person.getRelations();
            mContactPoints = person.getContactPoints();
            setAdditionalNames(person.getTypedAdditionalNames());
        }

        /** Sets the given name of this {@link Person}. */
        public @NonNull T setGivenName(@NonNull String givenName) {
            mGivenName = Preconditions.checkNotNull(givenName);
            return (T) this;
        }

        /**
         * Sets the middle name of this {@link Person}.
         *
         * <p>For {@link Person} with multiple middle names, they can all be set in this
         * single string. Each middle name could be separated by a whitespace like "middleName1
         * middleName2 middleName3".
         */
        public @NonNull T setMiddleName(@NonNull String middleName) {
            mMiddleName = Preconditions.checkNotNull(middleName);
            return (T) this;
        }

        /** Sets the family name of this {@link Person}. */
        public @NonNull T setFamilyName(@NonNull String familyName) {
            mFamilyName = Preconditions.checkNotNull(familyName);
            return (T) this;
        }

        /**
         * Sets an external {@link Uri} for this {@link Person}. Or {@code null} if no
         * {@link Uri} is provided. A {@link Uri} can be any of the following:
         * <ul>
         * <li>A {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}.
         * <li>A {@code mailto:} schema*
         * <li>A {@code tel:} schema*
         * </ul>
         * <p>For mailto: and tel: URI schemes, it is recommended that the path
         * portion refers to a valid contact in the Contacts Provider.
         */
        public @NonNull T setExternalUri(@NonNull Uri externalUri) {
            mExternalUri = Preconditions.checkNotNull(externalUri);
            return (T) this;
        }

        /**
         * Sets the external {@link Uri} from a {@link String} representation for this
         * {@link Person}. A {@link Uri} can be any of the following:
         * <ul>
         * <li>A {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}.
         * <li>A {@code mailto:} schema*
         * <li>A {@code tel:} schema*
         * </ul>
         * <p>For mailto: and tel: URI schemes, it is recommended that the path
         * portion refers to a valid contact in the Contacts Provider.
         */
        @ExperimentalAppSearchApi
        public @NonNull T setExternalUri(@NonNull String externalUri) {
            mExternalUri = Uri.parse(Preconditions.checkNotNull(externalUri));
            return (T) this;
        }

        /** Sets the {@link Uri} of the profile image for the {@link Person}. */
        public @NonNull T setImageUri(@NonNull Uri imageUri) {
            mImageUri = Preconditions.checkNotNull(imageUri);
            return (T) this;
        }

        /**
         * Sets the {@link Uri} of the profile image from a {@link String} representation for
         * the {@link Person}.
         */
        @ExperimentalAppSearchApi
        public @NonNull T setImageUri(@NonNull String imageUri) {
            mImageUri = Uri.parse(Preconditions.checkNotNull(imageUri));
            return (T) this;
        }

        /** Sets whether this {@link Person} is important. */
        public @NonNull T setImportant(boolean isImportant) {
            mIsImportant = isImportant;
            return (T) this;
        }

        /** Sets whether this {@link Person} is important. */
        @ExperimentalAppSearchApi
        public @NonNull T setIsImportant(boolean isImportant) {
            mIsImportant = isImportant;
            return (T) this;
        }

        /** Sets whether this {@link Person} is a bot. */
        public @NonNull T setBot(boolean isBot) {
            mIsBot = isBot;
            return (T) this;
        }

        /** Sets whether this {@link Person} is a bot. */
        @ExperimentalAppSearchApi
        public @NonNull T setIsBot(boolean isBot) {
            mIsBot = isBot;
            return (T) this;
        }

        /** Sets the notes about this {@link Person}. */
        public @NonNull T setNotes(@NonNull List<String> notes) {
            mNotes = Preconditions.checkNotNull(notes);
            return (T) this;
        }

        /**
         * Sets a list of {@link AdditionalName} for that {@link Person}.
         *
         * <p>Only types defined in {@link AdditionalName.NameType} are accepted.
         */
        public @NonNull T setAdditionalNames(@NonNull List<AdditionalName> additionalNames) {
            Preconditions.checkNotNull(additionalNames);
            int size = additionalNames.size();
            mAdditionalNameTypes = new ArrayList<>(size);
            mAdditionalNamesList = new ArrayList<>(size);
            for (int i = 0; i < additionalNames.size(); ++i) {
                long type = Preconditions.checkArgumentInRange(additionalNames.get(i).getType(),
                        AdditionalName.TYPE_UNKNOWN,
                        AdditionalName.TYPE_PHONETIC_NAME,
                        "type");
                mAdditionalNameTypes.add(type);
                mAdditionalNamesList.add(additionalNames.get(i).getValue());
            }
            return (T) this;
        }

        /**
         * Sets a list of additional names for that {@link Person}.
         *
         * <p>This should only be called by the AppSearch compiler. All other usages should go
         * through {@link #setAdditionalNames(List)}.
         */
        @ExperimentalAppSearchApi
        @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.SUBCLASSES})
        public @NonNull T setAdditionalNamesList(@NonNull List<String> additionalNamesList) {
            Preconditions.checkNotNull(additionalNamesList);
            mAdditionalNamesList = new ArrayList<>(additionalNamesList);
            return (T) this;
        }

        /**
         * Sets a list of additional name types for that {@link Person}.
         *
         * <p>This should only be called by the AppSearch compiler. All other usages should go
         * through {@link #setAdditionalNames(List)}.
         */
        @ExperimentalAppSearchApi
        @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.SUBCLASSES})
        public @NonNull T setAdditionalNameTypes(@NonNull List<Long> additionalNameTypes) {
            Preconditions.checkNotNull(additionalNameTypes);
            for (int i = 0; i < additionalNameTypes.size(); ++i) {
                Preconditions.checkArgumentInRange(additionalNameTypes.get(i),
                        AdditionalName.TYPE_UNKNOWN,
                        AdditionalName.TYPE_PHONETIC_NAME,
                        "type");
            }
            mAdditionalNameTypes = new ArrayList<>(additionalNameTypes);
            return (T) this;
        }

        /**
         * Sets a list of affiliations for this {@link Person}. Like company, school,
         * etc.
         */
        public @NonNull T setAffiliations(@NonNull List<String> affiliations) {
            mAffiliations = Preconditions.checkNotNull(affiliations);
            return (T) this;
        }

        /** Sets a list of relations for this {@link Person}, like "Father" or "Mother". */
        public @NonNull T setRelations(@NonNull List<String> relations) {
            mRelations = Preconditions.checkNotNull(relations);
            return (T) this;
        }

        /**
         * Sets a list of {@link ContactPoint} for the {@link Person}.
         *
         * <p>More information could be found in {@link ContactPoint}.
         */
        public @NonNull T setContactPoints(@NonNull List<ContactPoint> contactPoints) {
            mContactPoints = Preconditions.checkNotNull(contactPoints);
            return (T) this;
        }

        /** Builds the {@link Person}. */
        @Override
        public @NonNull Person build() {
            Preconditions.checkState(mAdditionalNameTypes.size() == mAdditionalNamesList.size());
            return new Person(this);
        }
    }
}
