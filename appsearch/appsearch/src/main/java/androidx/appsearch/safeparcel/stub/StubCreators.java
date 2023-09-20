/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appsearch.safeparcel.stub;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.safeparcel.GenericDocumentParcel;
import androidx.appsearch.app.safeparcel.PropertyConfigParcel;
import androidx.appsearch.app.safeparcel.PropertyParcel;

/**
 * Stub creators for any classes extending
 * {@link androidx.appsearch.safeparcel.SafeParcelable}.
 *
 * <p>We don't have SafeParcelProcessor in Jetpack, so for each
 * {@link androidx.appsearch.safeparcel.SafeParcelable}, a stub creator class needs to
 * be provided for code sync purpose.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StubCreators {
    /** Stub creator for {@link androidx.appsearch.app.StorageInfo}. */
    public static class StorageInfoCreator extends AbstractCreator {
    }

    /** Stub creator for {@link PropertyParcel}. */
    public static class PropertyParcelCreator extends AbstractCreator {
    }

    /** Stub creator for {@link PropertyConfigParcel}. */
    public static class PropertyConfigParcelCreator extends AbstractCreator {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.JoinableConfigParcel}.
     */
    public static class JoinableConfigParcelCreator extends AbstractCreator {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.StringIndexingConfigParcel}.
     */
    public static class StringIndexingConfigParcelCreator extends AbstractCreator {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.IntegerIndexingConfigParcel}.
     */
    public static class IntegerIndexingConfigParcelCreator extends AbstractCreator {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.DocumentIndexingConfigParcel}.
     */
    public static class DocumentIndexingConfigParcelCreator extends AbstractCreator {
    }

    /** Stub creator for {@link GenericDocumentParcel}. */
    public static class GenericDocumentParcelCreator extends AbstractCreator {
    }

    /** Stub creator for {@link androidx.appsearch.app.VisibilityPermissionDocument}. */
    public static class VisibilityPermissionDocumentCreator extends AbstractCreator {
    }

    /** Stub creator for {@link androidx.appsearch.app.VisibilityDocument}. */
    public static class VisibilityDocumentCreator extends AbstractCreator {
    }
}
