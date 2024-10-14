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

package androidx.appsearch.app;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.checker.initialization.qual.UnderInitialization;
import androidx.appsearch.checker.nullness.qual.RequiresNonNull;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a property path returned from searching the AppSearch Database.
 *
 * <p> One of the use cases for this class is when searching the AppSearch Database for the snippet
 * matching use case. In this case you will get back {@link SearchResult.MatchInfo} objects that
 * contain a property path signifying the location of  a match within the database. This is a
 * string that may look something like "foo.bar[0]". {@link PropertyPath} parses this string and
 * breaks it up into a List of {@link PathSegment}s. These may represent either a property or a
 * property and a 0-based index into the property. For instance, "foo.bar[1]" would be parsed
 * into a {@link PathSegment} with a property name of foo and a {@link PathSegment} with a
 * property name of bar and an index of 1. This allows for easier manipulation of the property
 * path.
 *
 * <p> This class won't perform any retrievals, it will only parse the path string. As such, it
 * may not necessarily refer to a valid path in the database.
 *
 * @see SearchResult.MatchInfo
 */
public class PropertyPath implements Iterable<PropertyPath.PathSegment> {
    private final List<PathSegment> mPathList;

    /**
     * Constructor directly accepting a path list
     * @param pathList a list of PathSegments
     */
    public PropertyPath(@NonNull List<PathSegment> pathList) {
        mPathList = new ArrayList<>(pathList);
    }

    /**
     * Constructor that parses a string representing the path to populate a List of PathSegments
     * @param path the string to be validated and parsed into PathSegments
     * @throws IllegalArgumentException when the path is invalid or malformed
     */
    public PropertyPath(@NonNull String path) {
        Preconditions.checkNotNull(path);
        mPathList = new ArrayList<>();
        try {
            recursivePathScan(path);
        } catch (IllegalArgumentException e) {
            // Throw the entire path in a new exception, recursivePathScan may only know about part
            // of the path.
            throw new IllegalArgumentException(e.getMessage() + ": " + path);
        }
    }

    @RequiresNonNull("mPathList")
    private void recursivePathScan(@UnderInitialization PropertyPath this, String path)
            throws IllegalArgumentException {
        // Determine whether the path is just a raw property name with no control characters
        int controlPos = -1;
        boolean controlIsIndex = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == ']') {
                throw new IllegalArgumentException("Malformed path (no starting '[')");
            }
            if (c == '[' || c == '.') {
                controlPos = i;
                controlIsIndex = c == '[';
                break;
            }
            if (!Character.isLetterOrDigit(c)) {
                throw new IllegalArgumentException("Malformed path (non alphanumeric character)");
            }
        }

        if (controlPos == 0 || path.isEmpty()) {
            throw new IllegalArgumentException("Malformed path (blank property name)");
        }

        // If the path has no further elements, we're done.
        if (controlPos == -1) {
            // The property's cardinality may be REPEATED, but this path isn't indexing into it
            mPathList.add(new PathSegment(path, PathSegment.NON_REPEATED_CARDINALITY));
            return;
        }

        String remainingPath;
        if (!controlIsIndex) {
            String propertyName = path.substring(0, controlPos);
            // Remaining path is everything after the .
            remainingPath = path.substring(controlPos + 1);
            mPathList.add(new PathSegment(propertyName, PathSegment.NON_REPEATED_CARDINALITY));
        } else {
            remainingPath = consumePropertyWithIndex(path, controlPos);
            // No more path remains, we have nothing to recurse into
            if (remainingPath == null) {
                return;
            }
        }

        // More of the path remains; recursively evaluate it
        recursivePathScan(remainingPath);
    }

    /**
     * Helper method to parse the parts of the path String that signify indices with square brackets
     *
     * <p>For example, when parsing the path "foo[3]", this will be used to parse the "[3]" part of
     * the path to determine the index into the preceding "foo" property.
     * @param path the string we are parsing
     * @param controlPos the position of the start bracket
     * @return the rest of the path after the end brackets, or null if there is nothing after them
     */
    @Nullable
    @RequiresNonNull("mPathList")
    private String consumePropertyWithIndex(
            @UnderInitialization PropertyPath this, @NonNull String path, int controlPos) {
        Preconditions.checkNotNull(path);
        String propertyName = path.substring(0, controlPos);
        int endBracketIdx = path.indexOf(']', controlPos);
        if (endBracketIdx == -1) {
            throw new IllegalArgumentException("Malformed path (no ending ']')");
        }
        if (endBracketIdx + 1 < path.length() && path.charAt(endBracketIdx + 1) != '.') {
            throw new IllegalArgumentException(
                    "Malformed path (']' not followed by '.'): " + path);
        }
        String indexStr = path.substring(controlPos + 1, endBracketIdx);
        int index;
        try {
            index = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Malformed path (\"" + indexStr + "\" as path index)");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Malformed path (path index less than 0)");
        }
        mPathList.add(new PathSegment(propertyName, index));
        // Remaining path is everything after the [n]
        if (endBracketIdx + 1 < path.length()) {
            // More path remains, and we've already checked that charAt(endBracketIdx+1) == .
            return path.substring(endBracketIdx + 2);
        } else {
            return null;
        }
    }

    /**
     * Returns the {@link PathSegment} at a specified index of the PropertyPath.
     *
     * <p> Calling {@code get(1)} on a {@link PropertyPath} representing "foo.bar[1]" will return
     * a {@link PathSegment} representing "bar[1]". {@link PathSegment}s both with and without a
     * property index of {@link PathSegment#NON_REPEATED_CARDINALITY} are retrieved the same.
     * @param index the position into the PropertyPath
     * @throws ArrayIndexOutOfBoundsException if index is not a valid index in the path list
     */
    // Allow use of the Kotlin indexing operator
    @SuppressWarnings("KotlinOperator")
    @SuppressLint("KotlinOperator")
    @NonNull
    public PathSegment get(int index) {
        return mPathList.get(index);
    }

    /**
     * Returns the number of {@link PathSegment}s in the PropertyPath.
     *
     * <p> Paths representing "foo.bar" and "foo[1].bar[1]" will have the same size, as a property
     * and an index into that property are stored in one {@link PathSegment}.
     */
    public int size() {
        return mPathList.size();
    }

    /** Returns a valid path string representing this PropertyPath */
    @Override
    @NonNull
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mPathList.size(); i++) {
            result.append(get(i).toString());
            if (i < mPathList.size() - 1) {
                result.append('.');
            }
        }

        return result.toString();
    }

    /** Returns an iterator over the PathSegments within the PropertyPath */
    @NonNull
    @Override
    public Iterator<PathSegment> iterator() {
        return mPathList.iterator();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof PropertyPath)) {
            return false;
        }
        PropertyPath that = (PropertyPath) o;
        return ObjectsCompat.equals(mPathList, that.mPathList);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hashCode(mPathList);
    }

    /**
     * A segment of a PropertyPath, which includes the name of the property and a 0-based index
     * into this property.
     *
     * <p> If the property index is not set to {@link #NON_REPEATED_CARDINALITY}, this represents a
     * schema property with the "repeated" cardinality, or a path like "foo[1]". Otherwise, this
     * represents a schema property that could have any cardinality, or a path like "foo".
     */
    public static class PathSegment {
        /**
         * A marker variable to signify that a PathSegment represents a schema property that isn't
         * indexed into. The value is chosen to be invalid if used as an array index.
         */
        public static final int NON_REPEATED_CARDINALITY = -1;

        @NonNull private final String mPropertyName;
        private final int mPropertyIndex;

        /**
         * Creation method that accepts and validates both a property name and the index into the
         * property.
         *
         * <p> The property name may not be blank. It also may not contain square brackets or
         * dots, as they are control characters in property paths. The index into the property
         * may not be negative, unless it is {@link #NON_REPEATED_CARDINALITY}, as these are
         * invalid array indices.
         * @param propertyName the name of the property
         * @param propertyIndex the index into the property
         * @return A new PathSegment
         * @throws IllegalArgumentException if the property name or index is invalid.
         */
        @NonNull
        public static PathSegment create(@NonNull String propertyName, int propertyIndex) {
            ObjectsCompat.requireNonNull(propertyName);
            // A path may contain control characters, but a PathSegment may not
            if (propertyName.isEmpty() || propertyName.contains("[") || propertyName.contains("]")
                    || propertyName.contains(".")) {
                throw new IllegalArgumentException("Invalid propertyName value:" + propertyName);
            }
            // Has to be a positive integer or the special marker
            if (propertyIndex < 0 && propertyIndex != NON_REPEATED_CARDINALITY) {
                throw new IllegalArgumentException("Invalid propertyIndex value:" + propertyIndex);
            }
            return new PathSegment(propertyName, propertyIndex);
        }

        /**
         * Creation method that accepts and validates a property name
         *
         * <p> The property index is set to {@link #NON_REPEATED_CARDINALITY}
         * @param propertyName the name of the property
         * @return A new PathSegment
         */
        @NonNull
        public static PathSegment create(@NonNull String propertyName) {
            return create(ObjectsCompat.requireNonNull(propertyName), NON_REPEATED_CARDINALITY);
        }

        /**
         * Package-private constructor that accepts a property name and an index into the
         * property without validating either of them
         * @param propertyName the name of the property
         * @param propertyIndex the index into the property
         */
        PathSegment(@NonNull String propertyName, int propertyIndex) {
            mPropertyName = ObjectsCompat.requireNonNull(propertyName);
            mPropertyIndex = propertyIndex;
        }

        /** Returns the name of the property. */
        @NonNull
        public String getPropertyName() {
            return mPropertyName;
        }

        /**
         * Returns the index into the property, or {@link #NON_REPEATED_CARDINALITY} if this does
         * not represent a PathSegment with an index.
         */
        public int getPropertyIndex() {
            return mPropertyIndex;
        }

        /** Returns a path representing a PathSegment, either "foo" or "foo[1]" */
        @Override
        @NonNull
        public String toString() {
            if (mPropertyIndex != NON_REPEATED_CARDINALITY) {
                return mPropertyName + "[" + mPropertyIndex + "]";
            }
            return mPropertyName;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof PathSegment)) {
                return false;
            }
            PathSegment that = (PathSegment) o;
            return mPropertyIndex == that.mPropertyIndex
                    && mPropertyName.equals(that.mPropertyName);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mPropertyName, mPropertyIndex);
        }
    }
}
