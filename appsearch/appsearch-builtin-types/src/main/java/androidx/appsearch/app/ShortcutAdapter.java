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

package androidx.appsearch.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.safeparcel.GenericDocumentParcel;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Util methods for Document <-> shortcut conversion.
 */
@ExperimentalAppSearchApi
public class ShortcutAdapter {

    private ShortcutAdapter() {
        // Hide constructor as utility classes are not meant to be instantiated.
    }

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static final String DEFAULT_DATABASE = "__shortcut_adapter_db__";

    /**
     * Represents the default namespace which should be used as the
     * {@link androidx.appsearch.annotation.Document.Namespace} for documents that
     * are meant to be donated as a shortcut through
     * {@link androidx.core.content.pm.ShortcutManagerCompat}.
     */
    public static final String DEFAULT_NAMESPACE = "__shortcut_adapter_ns__";

    private static final String FIELD_NAME = "name";

    private static final String SCHEME_APPSEARCH = "appsearch";
    private static final String NAMESPACE_CHECK_ERROR_MESSAGE = "Namespace of the document does "
            + "not match androidx.appsearch.app.ShortcutAdapter.DEFAULT_NAMESPACE."
            + "Please use androidx.appsearch.app.ShortcutAdapter.DEFAULT_NAMESPACE as the "
            + "namespace of the document if it will be used to create a shortcut.";

    private static final String APPSEARCH_GENERIC_DOC_PARCEL_NAME_IN_BUNDLE =
            "appsearch_generic_doc_parcel";

    /**
     * Converts given document to a {@link ShortcutInfoCompat.Builder}, which can be used to
     * construct a shortcut for donation through
     * {@link androidx.core.content.pm.ShortcutManagerCompat}. Applicable data in the given
     * document will be used to populate corresponding fields in {@link ShortcutInfoCompat.Builder}.
     *
     * <p>Note: Namespace of the given document is required to be set to {@link #DEFAULT_NAMESPACE}
     * if it will be used to create a shortcut; Otherwise an exception would be thrown.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.Namespace}
     *
     * <p>Note: The ShortcutID in {@link ShortcutInfoCompat.Builder} will be set to match the id
     * of given document. So an unique id across all documents should be chosen if the document
     * is to be used to create a shortcut.
     *
     * <p>see {@link ShortcutInfoCompat#getId()}
     * <p>see {@link androidx.appsearch.annotation.Document.Id}
     *
     * <p>{@link ShortcutInfoCompat.Builder} created this way by default will be set to hidden
     * from launcher. If remain hidden, they will not appear in launcher's surfaces (e.g. long
     * press menu) nor do they count toward the quota defined in
     * {@link androidx.core.content.pm.ShortcutManagerCompat#getMaxShortcutCountPerActivity(Context)}
     *
     * <p>See {@link ShortcutInfoCompat.Builder#setExcludedFromSurfaces(int)}.
     *
     * <p>Given document object will be stored in the form of {@link Bundle} in
     * {@link ShortcutInfoCompat}.
     *
     * <p>The document that was stored in {@link ShortcutInfoCompat} is discarded when the
     * shortcut is converted into {@link android.content.pm.ShortcutInfo}, meaning that the
     * document will not be persisted in the shortcut object itself once the shortcut is
     * published. i.e. Any shortcut returned from queries toward
     * {@link androidx.core.content.pm.ShortcutManagerCompat} would not carry any document at all.
     *
     * @param context the context used to provide the package and resources
     * @param document a document object annotated with
     *                 {@link androidx.appsearch.annotation.Document} that carries structured
     *                 data in a pre-defined format.
     * @return a {@link ShortcutInfoCompat.Builder} which can be used to construct a shortcut
     *         for donation through {@link androidx.core.content.pm.ShortcutManagerCompat}.
     * @throws IllegalArgumentException An exception would be thrown if the namespace in the given
     *                                  document object does not match {@link #DEFAULT_NAMESPACE}.
     * @throws AppSearchException An exception would be thrown if the given document object is not
     *                            annotated with {@link androidx.appsearch.annotation.Document} or
     *                            encountered an unexpected error during the conversion to
     *                            {@link GenericDocument}.
     */
    public static ShortcutInfoCompat.@NonNull Builder createShortcutBuilderFromDocument(
            final @NonNull Context context, @NonNull Object document) throws AppSearchException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(document);
        final GenericDocument doc = GenericDocument.fromDocumentClass(document);
        if (!DEFAULT_NAMESPACE.equals(doc.getNamespace())) {
            throw new IllegalArgumentException(NAMESPACE_CHECK_ERROR_MESSAGE);
        }
        final String name = doc.getPropertyString(FIELD_NAME);
        final Bundle extras = new Bundle();
        extras.putParcelable(APPSEARCH_GENERIC_DOC_PARCEL_NAME_IN_BUNDLE, doc.getDocumentParcel());
        return new ShortcutInfoCompat.Builder(context, doc.getId())
                .setShortLabel(!TextUtils.isEmpty(name) ? name : doc.getId())
                .setIntent(new Intent(Intent.ACTION_VIEW, getDocumentUri(doc)))
                .setExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)
                .setTransientExtras(extras);
    }

    /**
     * Extracts {@link GenericDocument} from given {@link ShortcutInfoCompat} if applicable.
     * Returns null if document cannot be found in the given shortcut.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static @Nullable GenericDocument extractDocument(
            final @NonNull ShortcutInfoCompat shortcut) {
        Preconditions.checkNotNull(shortcut);
        final Bundle extras = shortcut.getTransientExtras();
        if (extras == null) {
            return null;
        }

        GenericDocumentParcel genericDocParcel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            genericDocParcel = Api33Impl.getParcelableFromBundle(extras,
                    APPSEARCH_GENERIC_DOC_PARCEL_NAME_IN_BUNDLE, GenericDocumentParcel.class);
        } else {
            @SuppressWarnings("deprecation")
            GenericDocumentParcel tmp = (GenericDocumentParcel) extras.getParcelable(
                    APPSEARCH_GENERIC_DOC_PARCEL_NAME_IN_BUNDLE);
            genericDocParcel = tmp;
        }
        if (genericDocParcel == null) {
            return null;
        }
        return new GenericDocument(genericDocParcel);
    }

    /**
     * Returns an uri that uniquely identifies the given document object.
     *
     * @param document a document object annotated with
     *                 {@link androidx.appsearch.annotation.Document} that carries structured
     *                 data in a pre-defined format.
     * @throws AppSearchException if the given document object is not annotated with
     *                            {@link androidx.appsearch.annotation.Document} or encountered an
     *                            unexpected error during the conversion to {@link GenericDocument}.
     */
    public static @NonNull Uri getDocumentUri(final @NonNull Object document)
            throws AppSearchException {
        Preconditions.checkNotNull(document);
        return getDocumentUri(GenericDocument.fromDocumentClass(document));
    }

    private static @NonNull Uri getDocumentUri(final @NonNull GenericDocument obj) {
        Preconditions.checkNotNull(obj);
        return getDocumentUri(obj.getId());
    }

    /**
     * Returns an uri that identifies to the document associated with given document id.
     *
     * @param id id of the document.
     */
    public static @NonNull Uri getDocumentUri(final @NonNull String id) {
        Preconditions.checkNotNull(id);
        return new Uri.Builder()
                .scheme(SCHEME_APPSEARCH)
                .authority(DEFAULT_DATABASE)
                .path(DEFAULT_NAMESPACE + "/" + id)
                .build();
    }
    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static <T> T getParcelableFromBundle(
                @NonNull Bundle bundle,
                @NonNull String key,
                @NonNull Class<T> clazz) {
            Preconditions.checkNotNull(bundle);
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(clazz);
            return bundle.getParcelable(key, clazz);
        }
    }
}
