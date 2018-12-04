/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.WidgetType;

import java.util.Locale;

/**
 * A representation of the context in which text classification would be performed.
 */
public final class TextClassificationContext {
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_WIGET_TYPE = "widget_type";
    private static final String EXTRA_WIDGET_VERSION = "widget_version";

    private final String mPackageName;
    private final String mWidgetType;
    @Nullable private final String mWidgetVersion;

    @SuppressLint("RestrictedApi")
    TextClassificationContext(
            String packageName,
            String widgetType,
            String widgetVersion) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mWidgetType = Preconditions.checkNotNull(widgetType);
        mWidgetVersion = widgetVersion;
    }

    /**
     * Returns the package name for the calling package.
     */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the widget type for this classification context.
     */
    @NonNull
    @WidgetType
    public String getWidgetType() {
        return mWidgetType;
    }

    /**
     * Returns a custom version string for the widget type.
     *
     * @see #getWidgetType()
     */
    @Nullable
    public String getWidgetVersion() {
        return mWidgetVersion;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "TextClassificationContext{"
                        + "packageName=%s, widgetType=%s, widgetVersion=%s}",
                mPackageName, mWidgetType, mWidgetVersion);
    }

    /**
     * A builder for building a TextClassification context.
     */
    public static final class Builder {

        private final String mPackageName;
        private final String mWidgetType;

        @Nullable private String mWidgetVersion;

        /**
         * Initializes a new builder for text classification context objects.
         *
         * @param packageName the name of the calling package
         * @param widgetType the type of widget e.g. {@link TextClassifier#WIDGET_TYPE_TEXTVIEW}
         *
         * @return this builder
         */
        @SuppressLint("RestrictedApi")
        public Builder(@NonNull String packageName, @NonNull @WidgetType String widgetType) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mWidgetType = Preconditions.checkNotNull(widgetType);
        }

        /**
         * Sets an optional custom version string for the widget type.
         *
         * @return this builder
         */
        public Builder setWidgetVersion(@Nullable String widgetVersion) {
            mWidgetVersion = widgetVersion;
            return this;
        }

        /**
         * Builds the text classification context object.
         *
         * @return the built TextClassificationContext object
         */
        @NonNull
        public TextClassificationContext build() {
            return new TextClassificationContext(mPackageName, mWidgetType, mWidgetVersion);
        }
    }

    /**
     * Adds this classification to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PACKAGE_NAME, mPackageName);
        bundle.putString(EXTRA_WIGET_TYPE, mWidgetType);
        bundle.putString(EXTRA_WIDGET_VERSION, mWidgetVersion);
        return bundle;
    }

    /**
     * Extracts a {@link TextClassificationContext} from a bundle that was added using
     * {@link #toBundle()}.
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static TextClassificationContext createFromBundle(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);

        String packageName = bundle.getString(EXTRA_PACKAGE_NAME);
        String widgetType = bundle.getString(EXTRA_WIGET_TYPE);
        String widgetVersion = bundle.getString(EXTRA_WIDGET_VERSION);
        return new TextClassificationContext(packageName, widgetType, widgetVersion);
    }

    /**
     * @hide
     */
    @RequiresApi(28)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    Object toPlatform() {
        return new android.view.textclassifier.TextClassificationContext.Builder(
                mPackageName, mWidgetType).setWidgetVersion(mWidgetVersion).build();
    }
}
