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

package android.support.wearable.watchface.accessibility;

import android.content.Context;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ComplicationTextTemplate;
import android.support.wearable.complications.TimeDependentText;
import android.support.wearable.watchface.R;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utilities for making watch faces and complications accessible.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccessibilityUtils {

    private AccessibilityUtils() {}

    /**
     * Returns a new {@link ComplicationText} that displays the current time in the default
     * timezone.
     */
    @NonNull
    public static ComplicationText makeTimeAsComplicationText(@NonNull Context context) {
        final String format;
        if (DateFormat.is24HourFormat(context)) {
            format = "HH:mm";
        } else {
            format = "h:mm a";
        }
        return new ComplicationText.TimeFormatBuilder().setFormat(format).build();
    }

    /**
     * Returns {@link TimeDependentText} that describes the data in the complication in a
     * time-dependent way.
     */
    @NonNull
    public static TimeDependentText generateContentDescription(
            @NonNull Context context, @NonNull ComplicationData data) {
        ComplicationText desc = data.hasContentDescription() ? data.getContentDescription() : null;
        // If it's empty, it could be for a stylistic image, but we should still generate other
        // text, since the icon might still have long text, etc.
        if (desc == null || desc.isAlwaysEmpty()) {
            final ComplicationText text;
            final ComplicationText title;
            if (data.getType() == ComplicationData.TYPE_LONG_TEXT) {
                text = data.getLongText();
                title = data.getLongTitle();
            } else {
                text = data.hasShortText() ? data.getShortText() : null;
                title = data.hasShortTitle() ? data.getShortTitle() : null;
            }

            ComplicationTextTemplate.Builder builder = new ComplicationTextTemplate.Builder();
            boolean isBuilderEmpty = true;
            boolean hasTextOrTitle = false;
            if (text != null && !text.isAlwaysEmpty()) {
                builder.addComplicationText(text);
                isBuilderEmpty = false;
                hasTextOrTitle = true;
            }
            if (title != null && !title.isAlwaysEmpty()) {
                builder.addComplicationText(title);
                isBuilderEmpty = false;
                hasTextOrTitle = true;
            }

            final ComplicationText typeSpecificText;
            switch (data.getType()) {
                case ComplicationData.TYPE_NO_PERMISSION:
                    typeSpecificText =
                        ComplicationText.plainText(context.getString(R.string.a11y_no_permission));
                    break;
                case ComplicationData.TYPE_NO_DATA:
                    typeSpecificText =
                        ComplicationText.plainText(context.getString(R.string.a11y_no_data));
                    break;
                case ComplicationData.TYPE_RANGED_VALUE: {
                    // Most likely the range info is already in the short text.
                    if (hasTextOrTitle) {
                        typeSpecificText = null;
                    } else {
                        float value = data.getRangedValue();
                        float max = data.getRangedMaxValue();
                        // TODO(fuego): what do do with min? should we even say max? should it
                        // be a percentage?
                        typeSpecificText =
                                ComplicationText.plainText(
                                        context.getString(
                                                R.string.a11y_template_range, value, max));
                    }
                }
                break;
                default:
                    typeSpecificText = null;
            }

            if (typeSpecificText == null && isBuilderEmpty) {
                return ComplicationText.plainText("");
            }

            if (typeSpecificText != null) {
                if (isBuilderEmpty) {
                    return typeSpecificText;
                } else {
                    builder.addComplicationText(typeSpecificText);
                }
            }

            return builder.build();
        }

        return desc;
    }
}
