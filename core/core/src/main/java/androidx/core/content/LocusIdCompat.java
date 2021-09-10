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

package androidx.core.content;

import android.content.Intent;
import android.content.LocusId;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * An identifier for an unique state (locus) in the application. Should be stable across reboots and
 * backup / restore.
 *
 * <p>Locus is a new concept introduced on
 * {@link android.os.Build.VERSION_CODES#Q Android Q} and it lets the intelligence service provided
 * by the Android system correlate state between different subsystems such as content capture,
 * shortcuts, and notifications.
 *
 * <p>For example, if your app provides an activity representing a chat between 2 users
 * (say {@code A} and {@code B}, this chat state could be represented by:
 *
 * <pre><code>
 * LocusIdCompat chatId = new LocusIdCompat("Chat_A_B");
 * </code></pre>
 *
 * <p>And then you should use that {@code chatId} by:
 *
 * <ul>
 *   <li>Setting it in the chat notification (through
 *   {@link androidx.core.app.NotificationCompat.Builder#setLocusId(LocusIdCompat)
 *   NotificationCompat.Builder.setLocusId(chatId)}).
 *   <li>Setting it into the {@link androidx.core.content.pm.ShortcutInfoCompat} (through
 *   {@link androidx.core.content.pm.ShortcutInfoCompat.Builder#setLocusId(LocusIdCompat)
 *   ShortcutInfoCompat.Builder.setLocusId(chatId)}), if you provide a launcher shortcut for that
 *   chat conversation.
 *   <li>Associating it with the {@link android.view.contentcapture.ContentCaptureContext} of the
 *   root view of the chat conversation activity (through
 *   {@link android.view.View#getContentCaptureSession()}, then
 *   {@link android.view.contentcapture.ContentCaptureContext.Builder
 *   new ContentCaptureContext.Builder(chatId).build()} and
 *   {@link android.view.contentcapture.ContentCaptureSession#setContentCaptureContext(
 *   android.view.contentcapture.ContentCaptureContext)} - see
 *   {@link android.view.contentcapture.ContentCaptureManager} for more info about content capture).
 *   <li>Configuring your app to launch the chat conversation through the
 *   {@link Intent#ACTION_VIEW_LOCUS} intent.
 * </ul>
 *
 * NOTE: The LocusId is only used by a on-device intelligence service provided by the Android
 * System, see {@link ContentCaptureManager} for more details.
 */
public final class LocusIdCompat {

    private final String mId;
    // Only guaranteed to be non-null on SDK_INT >= 29.
    private final LocusId mWrapped;

    /**
     * Construct a new LocusIdCompat with the specified id.
     *
     * @throws IllegalArgumentException if {@code id} is empty or {@code null}.
     */
    public LocusIdCompat(@NonNull String id) {
        mId = Preconditions.checkStringNotEmpty(id, "id cannot be empty");
        if (Build.VERSION.SDK_INT >= 29) {
            mWrapped = Api29Impl.create(id);
        } else {
            mWrapped = null;
        }
    }

    /**
     * Gets the canonical {@code id} associated with the locus.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mId == null) ? 0 : mId.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final LocusIdCompat other = (LocusIdCompat) obj;
        if (mId == null) {
            return other.mId == null;
        } else {
            return mId.equals(other.mId);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "LocusIdCompat[" + getSanitizedId() + "]";
    }

    /**
     * @return {@link LocusId} object from this compat object.
     */
    @NonNull
    @RequiresApi(29)
    public LocusId toLocusId() {
        return mWrapped;
    }

    /**
     * Returns an instance of LocusIdCompat from given {@link LocusId}.
     */
    @NonNull
    @RequiresApi(29)
    public static LocusIdCompat toLocusIdCompat(@NonNull final LocusId locusId) {
        Preconditions.checkNotNull(locusId, "locusId cannot be null");
        return new LocusIdCompat(Preconditions.checkStringNotEmpty(Api29Impl.getId(locusId),
                "id cannot be empty"));
    }

    @NonNull
    private String getSanitizedId() {
        final int size = mId.length();
        return size + "_chars";
    }

    // Inner class required to avoid VFY errors during class init.
    @RequiresApi(29)
    private static class Api29Impl {

        /**
         * @return {@link LocusId} object from this compat object.
         */
        @NonNull
        static LocusId create(@NonNull final String id) {
            return new LocusId(id);
        }

        /**
         * @return {@code id} from the LocusId object.
         */
        @NonNull
        static String getId(@NonNull final LocusId obj) {
            return obj.getId();
        }
    }
}
