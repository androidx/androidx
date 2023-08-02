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

package android.support.wearable.complications;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class CharSequenceSerializableHelper {
    private CharSequenceSerializableHelper() {}

    @SuppressWarnings("ParameterNotNullable") // Error prone is wrong about charSequence.length()
    public static void writeToStream(
            @Nullable CharSequence charSequence, @NonNull ObjectOutputStream stream)
            throws IOException {
        Boolean isNull = charSequence == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            Boolean isSpannable = charSequence instanceof SpannableString;
            stream.writeBoolean(isSpannable);
            if (isSpannable) {
                stream.writeUTF(
                        HtmlCompat.toHtml(
                                (SpannableString) charSequence,
                                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
            } else {
                // We don't rely on CharSequence having implemented toString().
                StringBuilder builder = new StringBuilder(charSequence.length());
                builder.append(charSequence);
                stream.writeUTF(builder.toString());
            }
        }
    }

    public static @Nullable CharSequence readFromStream(@NonNull ObjectInputStream stream)
            throws IOException {
        Boolean isNull = stream.readBoolean();
        if (isNull) {
            return null;
        }
        Boolean isSpannable = stream.readBoolean();
        if (isSpannable) {
            return HtmlCompat.fromHtml(stream.readUTF(), 0);
        } else {
            return stream.readUTF();
        }
    }
}
