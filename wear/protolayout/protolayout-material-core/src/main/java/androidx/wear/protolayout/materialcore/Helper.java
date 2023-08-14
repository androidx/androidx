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

package androidx.wear.protolayout.materialcore;

import static androidx.wear.protolayout.DimensionBuilders.dp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.protolayout.DeviceParametersBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.TypeBuilders.StringProp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

/** Helper class used for ProtoLayout Material core. */
public class Helper {
    private Helper() {}

    /** Returns given value if not null or throws {@code NullPointerException} otherwise. */
    @NonNull
    public static <T> T checkNotNull(@Nullable T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /** Returns radius in {@link DpProp} of the given diameter. */
    @NonNull
    public static DpProp radiusOf(@NonNull DpProp diameter) {
        return dp(diameter.getValue() / 2);
    }

    /** Returns String representation of tag from byte array. */
    @NonNull
    public static String getTagName(@NonNull byte[] tagData) {
        return new String(tagData, StandardCharsets.UTF_8);
    }

    /** Returns byte array representation of tag from String. */
    @NonNull
    public static byte[] getTagBytes(@NonNull String tagName) {
        return tagName.getBytes(StandardCharsets.UTF_8);
    }

    /** Returns the String representation of metadata tag from the given ElementMetadata. */
    @NonNull
    public static String getMetadataTagName(@NonNull ElementMetadata metadata) {
        return getTagName(getMetadataTagBytes(metadata));
    }

    /** Returns the metadata tag from the given ElementMetadata. */
    @NonNull
    public static byte[] getMetadataTagBytes(@NonNull ElementMetadata metadata) {
        return checkNotNull(metadata).getTagData();
    }

    /** Returns true if the given Modifiers have Metadata tag set to the given String value. */
    public static boolean checkTag(@Nullable Modifiers modifiers, @NonNull String validTag) {
        return modifiers != null
                && modifiers.getMetadata() != null
                && validTag.equals(getMetadataTagName(modifiers.getMetadata()));
    }

    /**
     * Returns true if the given Modifiers have Metadata tag set to any of the value in the given
     * String collection.
     */
    public static boolean checkTag(
            @Nullable Modifiers modifiers, @NonNull Collection<String> validTags) {
        return modifiers != null
                && modifiers.getMetadata() != null
                && validTags.contains(getMetadataTagName(modifiers.getMetadata()));
    }

    /**
     * Returns true if the given Modifiers have Metadata tag set with prefix that is equal to the
     * given String and its length is of the given base array.
     */
    public static boolean checkTag(
            @Nullable Modifiers modifiers, @NonNull String validPrefix, @NonNull byte[] validBase) {
        if (modifiers == null || modifiers.getMetadata() == null) {
            return false;
        }
        byte[] metadataTag = getMetadataTagBytes(modifiers.getMetadata());
        byte[] tag = Arrays.copyOf(metadataTag, validPrefix.length());
        return metadataTag.length == validBase.length && validPrefix.equals(getTagName(tag));
    }

    /** Returns true if the given DeviceParameters belong to the round screen device. */
    public static boolean isRoundDevice(@NonNull DeviceParameters deviceParameters) {
        return deviceParameters.getScreenShape() == DeviceParametersBuilders.SCREEN_SHAPE_ROUND;
    }

    /** Returns a {@link StringProp} for the given string value. */
    @NonNull
    public static StringProp staticString(@NonNull String value) {
        return new StringProp.Builder(value).build();
    }

    /** Returns a {@link FloatProp} for the given float value. */
    @NonNull
    public static FloatProp staticFloat(float staticValue) {
        return new FloatProp.Builder(staticValue).build();
    }
}
