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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.text.Spanned;
import android.text.style.CharacterStyle;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Utility class for common operations on the car app models
 *
 */
@RestrictTo(LIBRARY)
public final class ModelUtils {
    /**
     * Checks whether all non-browsable rows have attached at least one {@link DistanceSpan} in
     * either the title or secondary text.
     *
     * @throws IllegalArgumentException if the input list contains any non-Row instances, or if any
     *                                  non-browsable row does not have a {@link DistanceSpan}
     *                                  instance
     */
    public static void validateAllNonBrowsableRowsHaveDistance(@NonNull List<Item> rows) {
        int spanSetCount = 0;
        int nonBrowsableRowCount = 0;
        for (Item rowObj : rows) {
            if (!(rowObj instanceof Row)) {
                throw new IllegalArgumentException("Item in the list is not a Row");
            }
            Row row = (Row) rowObj;

            if (!row.isBrowsable()) {
                nonBrowsableRowCount++;
            }

            if (checkRowHasSpanType(row, DistanceSpan.class)) {
                spanSetCount++;
            }
        }

        if (nonBrowsableRowCount > spanSetCount) {
            throw new IllegalArgumentException(
                    "All non-browsable rows must have a distance span attached to either its "
                            + "title or texts");
        }
    }

    /**
     * Checks whether all rows have attached at least one {@link DurationSpan} or
     * {@link DistanceSpan }in either the title or secondary text.
     *
     * @throws IllegalArgumentException if the input list contains any non-Row instances, or if any
     *                                  non-browsable row does not have either a {@link
     *                                  DurationSpan} or {@link DistanceSpan} instance
     */
    public static void validateAllRowsHaveDistanceOrDuration(@NonNull List<Item> rows) {
        for (Item rowObj : rows) {
            if (!(rowObj instanceof Row)) {
                throw new IllegalArgumentException("Item in the list is not a Row");
            }
            Row row = (Row) rowObj;
            if (!(checkRowHasSpanType(row, DistanceSpan.class)
                    || checkRowHasSpanType(row, DurationSpan.class))) {
                throw new IllegalArgumentException(
                        "All rows must have either a distance or duration span attached to either"
                                + " its title or texts");
            }
        }
    }

    /**
     * Checks whether all rows have only small-sized images if they are set.
     *
     * @throws IllegalArgumentException if the input list contains any non-Row instances, or if an
     *                                  image set in any rows is using {@link Row#IMAGE_TYPE_LARGE}
     */
    public static void validateAllRowsHaveOnlySmallImages(@NonNull List<Item> rows) {
        for (Item rowObj : rows) {
            if (!(rowObj instanceof Row)) {
                throw new IllegalArgumentException("Item in the list is not a Row");
            }
            Row row = (Row) rowObj;
            if (row.getImage() != null && row.getRowImageType() == Row.IMAGE_TYPE_LARGE) {
                throw new IllegalArgumentException("Rows must only use small-sized images");
            }
        }
    }

    /**
     * Checks whether any rows have both a marker and an image.
     *
     * @throws IllegalArgumentException if the input list contains any non-Row instances, or if
     *                                  both a marker and an image are set in a row
     */
    public static void validateNoRowsHaveBothMarkersAndImages(@NonNull List<Item> rows) {
        for (Item rowObj : rows) {
            if (!(rowObj instanceof Row)) {
                throw new IllegalArgumentException("Item in the list is not a Row");
            }
            Row row = (Row) rowObj;


            Metadata metadata = row.getMetadata();
            if (metadata != null) {
                boolean hasImage = row.getImage() != null;
                Place place = metadata.getPlace();
                boolean hasMarker = place != null && place.getMarker() != null;

                if (hasImage && hasMarker) {
                    throw new IllegalArgumentException(
                            "Rows can't have both a marker and an image");
                }
            }
        }
    }

    /**
     * Returns {@code true} if the given row has a span of the given type, {@code false} otherwise.
     */
    private static boolean checkRowHasSpanType(Row row, Class<? extends CharacterStyle> spanType) {
        CarText title = requireNonNull(row.getTitle());
        if (checkCarTextHasSpanType(title, spanType)) {
            return true;
        }

        List<CarText> texts = row.getTexts();
        for (int i = 0; i < texts.size(); i++) {
            CarText text = texts.get(i);
            if (checkCarTextHasSpanType(text, spanType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if the given {@link CarText} has a span of the given type, {@code false}
     * otherwise.
     */
    private static boolean checkCarTextHasSpanType(
            CarText carText, Class<? extends CharacterStyle> spanType) {
        if (carText.isEmpty()) {
            return false;
        }
        Spanned spanned = (Spanned) carText.toCharSequence();
        for (Object span : spanned.getSpans(0, spanned.length(), Object.class)) {
            int start = spanned.getSpanStart(span);
            int end = spanned.getSpanEnd(span);
            if (spanType.isInstance(span)
                    && start >= 0
                    && start != end
                    && start < spanned.length()) {
                return true;
            }
        }
        return false;
    }

    private ModelUtils() {
    }
}
