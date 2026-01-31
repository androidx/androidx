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
package androidx.compose.remote.core.documentation;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DocumentedOperation {
    public static final int LAYOUT = 0;
    public static final int INT = 0;
    public static final int FLOAT = 1;
    public static final int BOOLEAN = 2;
    public static final int BUFFER = 4;
    public static final int UTF8 = 5;
    public static final int BYTE = 6;
    public static final int VALUE = 7;
    public static final int LONG = 8;
    public static final int SHORT = 9;

    public static final int FLOAT_ARRAY = 10;
    public static final int INT_ARRAY = 11;
    public static final int BYTE_ARRAY = 12;
    public static final int REPEATED_FLOAT = 13;
    public static final int REPEATED_INT = 14;
    public static final int REPEATED_BYTE = 15;

    @NonNull
    final String mCategory;
    int mId;
    @NonNull
    final String mName;
    @NonNull String mDescription = "";

    boolean mWIP;
    boolean mExperimental;
    int mAddedVersion;
    @Nullable String mTextExamples;
    @Nullable String mAdditionalDocumentation;

    @NonNull ArrayList<StringPair> mExamples = new ArrayList<>();
    @NonNull RepeatedField mFields = new RepeatedField("", null);
    @NonNull String mVarSize = "";
    int mExamplesWidth = 100;
    int mExamplesHeight = 100;

    static class RepeatedField implements DocumentedField {
        @NonNull ArrayList<DocumentedField> mFields = new ArrayList<>();
        @Nullable RepeatedField mParentFields;

        private String mName;

        RepeatedField(String name, @Nullable RepeatedField fields) {
            mName = name;
            mParentFields = fields;
        }

        public @NonNull ArrayList<DocumentedField> getFields() {
            return mFields;
        }

        public @Nullable RepeatedField getParent() {
            return mParentFields;
        }

        public void add(DocumentedField field) {
            mFields.add(field);
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Nullable
        @Override
        public String getVarSize() {
            return "";
        }

        @NonNull
        @Override
        public String toDoc() {
            StringBuilder builder = new StringBuilder();
            builder.append("<tr><td>" + mName + "</td><td colspan=\"2\">");
//            builder.append("    | ");
//            builder.append("REPEATED");
//            builder.append(" | ");
//            builder.append("REPEATED");
//            builder.append(" | ");
//            builder.append("REPEATED");
//            builder.append("\n");
            builder.append("<table>");
            builder.append("<tr><th>Type</th><th>Name</th><th>Description</th></tr>\n");
            for (DocumentedField field : mFields) {
                builder.append(field.toDoc());
            }
            builder.append("</table>");
            builder.append("</td></tr>");
            return builder.toString();
        }
    }

    /**
     * Returns the string representation of a field type
     */
    @NonNull
    public static String getType(int type) {
        switch (type) {
            case INT:
                return "INT";
            case FLOAT:
                return "FLOAT";
            case BOOLEAN:
                return "BOOLEAN";
            case BUFFER:
                return "BUFFER";
            case UTF8:
                return "UTF8";
            case BYTE:
                return "BYTE";
            case VALUE:
                return "VALUE";
            case LONG:
                return "LONG";
            case SHORT:
                return "SHORT";
            case FLOAT_ARRAY:
                return "FLOAT[]";
            case INT_ARRAY:
                return "INT[]";
            case BYTE_ARRAY:
                return "BYTE[]";
            case REPEATED_FLOAT:
                return "REPEATED FLOAT";
            case REPEATED_INT:
                return "REPEATED INT";
            case REPEATED_BYTE:
                return "REPEATED BYTE";
        }
        return "UNKNOWN";
    }

    public DocumentedOperation(
            @NonNull String category, int id, @NonNull String name, boolean wip) {
        mCategory = category;
        mId = id;
        mName = name;
        mWIP = wip;
        mExperimental = false;
        mAddedVersion = 6;
    }

    public DocumentedOperation(@NonNull String category, int id, @NonNull String name) {
        this(category, id, name, false);
    }

    @NonNull
    public ArrayList<DocumentedField> getFields() {
        return mFields.getFields();
    }

    public @NonNull String getCategory() {
        return mCategory;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public boolean isWIP() {
        return mWIP;
    }

    public boolean isExperimental() {
        return mExperimental;
    }

    public int getAddedVersion() {
        return mAddedVersion;
    }

    @Nullable
    public String getAdditionalDocumentation() {
        return mAdditionalDocumentation;
    }

    @NonNull
    public String getVarSize() {
        return mVarSize;
    }

    /**
     * Returns the size of the operation fields
     *
     * @return size in bytes
     */
    public int getSizeFields() {
        int size = 0;
        mVarSize = "";
        for (DocumentedField field : mFields.getFields()) {
            size += Math.max(0, field.getSize());
            if (field.getSize() < 0) {
                mVarSize += " + " + field.getVarSize() + " x 4";
            }
        }
        return size;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    @Nullable
    public String getTextExamples() {
        return mTextExamples;
    }

    @NonNull
    public ArrayList<StringPair> getExamples() {
        return mExamples;
    }

    public int getExamplesWidth() {
        return mExamplesWidth;
    }

    public int getExamplesHeight() {
        return mExamplesHeight;
    }

    /**
     * Set if the operation is experimental
     */
    @NonNull
    public DocumentedOperation experimental(boolean experimental) {
        mExperimental = experimental;
        return this;
    }

    /**
     * Set the version when the operation was added
     */
    @NonNull
    public DocumentedOperation addedVersion(int version) {
        mAddedVersion = version;
        return this;
    }

    /**
     * Set the name of the additional documentation file
     */
    @NonNull
    public DocumentedOperation additionalDocumentation(@NonNull String additionalDocumentation) {
        mAdditionalDocumentation = additionalDocumentation;
        return this;
    }

    /**
     * Document a field of the operation
     */
    @NonNull
    public DocumentedOperation field(int type, @NonNull String name, @NonNull String description) {
        mFields.add(new OperationField(type, name, description));
        return this;
    }

    /**
     * Document a field of the operation
     */
    @NonNull
    public DocumentedOperation field(
            int type, @NonNull String name, @NonNull String varSize, @NonNull String description) {
        mFields.add(new OperationField(type, name, varSize, description));
        return this;
    }

    /**
     * Add possible values for the operation field
     */
    @NonNull
    public DocumentedOperation possibleValues(@NonNull String name, int value) {
        ArrayList<DocumentedField> fields = mFields.getFields();
        if (!fields.isEmpty()) {
            DocumentedField field = fields.get(fields.size() - 1);
            if (field instanceof OperationField) {
                ((OperationField) field).possibleValue(name, "" + value);
            }
        }
        return this;
    }

    /**
     * Add a description
     */
    @NonNull
    public DocumentedOperation description(@NonNull String description) {
        mDescription = description;
        return this;
    }

    /**
     * Add arbitrary text as examples
     */
    @NonNull
    public DocumentedOperation examples(@NonNull String examples) {
        mTextExamples = examples;
        return this;
    }

    /**
     * Add an example image
     *
     * @param name      the title of the image
     * @param imagePath the path of the image
     */
    @NonNull
    public DocumentedOperation exampleImage(@NonNull String name, @NonNull String imagePath) {
        mExamples.add(new StringPair(name, imagePath));
        return this;
    }

    /**
     * Add examples with a given size
     */
    @NonNull
    public DocumentedOperation examplesDimension(int width, int height) {
        mExamplesWidth = width;
        mExamplesHeight = height;
        return this;
    }

    /**
     * Start a subsection
     */
    @NonNull
    public DocumentedOperation startSubsection(@NonNull String name) {
        RepeatedField repeatedField = new RepeatedField(name, mFields);
        mFields.add(repeatedField);
        mFields = repeatedField;
        return this;
    }

    /**
     * End a subsection
     */
    @NonNull
    public DocumentedOperation endSubsection() {
        if (mFields.getParent() != null) {
            mFields = mFields.getParent();
        }
        return this;
    }
}
