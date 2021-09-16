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

package android.support.wearable.complications;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container for complication data of all types.
 *
 * <p>A {@link androidx.wear.watchface.complications.ComplicationProviderService} should create
 * instances of
 * this class using {@link ComplicationData.Builder} and send them to the complication system in
 * response to
 * {@link androidx.wear.watchface.complications.ComplicationProviderService#onComplicationRequest}.
 * Depending on the type of complication data, some fields will be required and some will be
 * optional - see the documentation for each type, and for the builder's set methods, for details.
 *
 * <p>A watch face will receive instances of this class as long as providers are configured.
 *
 * <p>When rendering the complication data for a given time, the watch face should first call {@link
 * #isActiveAt} to determine whether the data is valid at that time. See the documentation for each
 * of the complication types below for details of which fields are expected to be displayed.
 *
 * @hide
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ComplicationData implements Parcelable {

    private static final String TAG = "ComplicationData";

    /** @hide */
    @IntDef({
            TYPE_EMPTY,
            TYPE_NOT_CONFIGURED,
            TYPE_SHORT_TEXT,
            TYPE_LONG_TEXT,
            TYPE_RANGED_VALUE,
            TYPE_ICON,
            TYPE_SMALL_IMAGE,
            TYPE_LARGE_IMAGE,
            TYPE_NO_PERMISSION,
            TYPE_NO_DATA
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ComplicationType {
    }

    /**
     * Type sent when a complication does not have a provider configured. The system will send data
     * of this type to watch faces when the user has not chosen a provider for an active
     * complication, and the watch face has not set a default provider. Providers cannot send data
     * of this type.
     *
     * <p>No fields may be populated for complication data of this type.
     */
    public static final int TYPE_NOT_CONFIGURED = 1;

    /**
     * Type sent when the user has specified that an active complication should have no provider,
     * i.e. when the user has chosen "Empty" in the provider chooser. Providers cannot send data of
     * this type.
     *
     * <p>No fields may be populated for complication data of this type.
     */
    public static final int TYPE_EMPTY = 2;

    /**
     * Type that can be sent by any provider, regardless of the configured type, when the provider
     * has no data to be displayed. Watch faces may choose whether to render this in some way or
     * leave the slot empty.
     *
     * <p>No fields may be populated for complication data of this type.
     */
    public static final int TYPE_NO_DATA = 10;

    /**
     * Type used for complications where the primary piece of data is a short piece of text
     * (expected to be no more than seven characters in length). The short text may be accompanied
     * by an icon or a short title (or both, but if both are provided then a watch face may choose
     * to display only one).
     *
     * <p>The <i>short text</i> field is required for this type, and is expected to always be
     * displayed.
     *
     * <p>The <i>icon</i> (and <i>burnInProtectionIcon</i>) and <i>short title</i> fields are
     * optional for this type. If only one of these is provided, it is expected that it will be
     * displayed. If both are provided, it is expected that one of these will be displayed.
     */
    public static final int TYPE_SHORT_TEXT = 3;

    /**
     * Type used for complications where the primary piece of data is a piece of text. The text may
     * be accompanied by an icon and/or a title.
     *
     * <p>The <i>long text</i> field is required for this type, and is expected to always be
     * displayed.
     *
     * <p>The <i>long title</i> field is optional for this type. If provided, it is expected that
     * this field will be displayed.
     *
     * <p>The <i>icon</i> (and <i>burnInProtectionIcon</i>) and <i>small image</i> fields are also
     * optional for this type. If provided, at least one of these should be displayed.
     */
    public static final int TYPE_LONG_TEXT = 4;

    /**
     * Type used for complications including a numerical value within a range, such as a percentage.
     * The value may be accompanied by an icon and/or short text and title.
     *
     * <p>The <i>value</i>, <i>min value</i>, and <i>max value</i> fields are required for this
     * type, and the value within the range is expected to always be displayed.
     *
     * <p>The <i>icon</i> (and <i>burnInProtectionIcon</i>), <i>short title</i>, and <i>short
     * text</i> fields are optional for this type. The watch face may choose which of these fields
     * to display, if any.
     */
    public static final int TYPE_RANGED_VALUE = 5;

    /**
     * Type used for complications which consist only of a tintable icon.
     *
     * <p>The <i>icon</i> field is required for this type, and is expected to always be displayed,
     * unless the device is in ambient mode with burn-in protection enabled, in which case the
     * <i>burnInProtectionIcon</i> field should be used instead.
     *
     * <p>The contentDescription field is recommended for this type. Use it to describe what data
     * the icon represents. If the icon is purely stylistic, and does not convey any information to
     * the user, then enter the empty string as the contentDescription.
     *
     * <p>No other fields are valid for this type.
     */
    public static final int TYPE_ICON = 6;

    /**
     * Type used for complications which consist only of a small image.
     *
     * <p>The <i>small image</i> field is required for this type, and is expected to always be
     * displayed, unless the device is in ambient mode, in which case either nothing or the
     * <i>burnInProtectionSmallImage</i> field may be used instead.
     *
     * <p>The contentDescription field is recommended for this type. Use it to describe what data
     * the image represents. If the image is purely stylistic, and does not convey any information
     * to the user, then enter the empty string as the contentDescription.
     *
     * <p>No other fields are valid for this type.
     */
    public static final int TYPE_SMALL_IMAGE = 7;

    /**
     * Type used for complications which consist only of a large image. A large image here is one
     * that could be used to fill the watch face, for example as the background.
     *
     * <p>The <i>large image</i> field is required for this type, and is expected to always be
     * displayed, unless the device is in ambient mode.
     *
     * <p>The contentDescription field is recommended for this type. Use it to describe what data
     * the image represents. If the image is purely stylistic, and does not convey any information
     * to the user, then enter the empty string as the contentDescription.
     *
     * <p>No other fields are valid for this type.
     */
    public static final int TYPE_LARGE_IMAGE = 8;

    /**
     * Type sent by the system when the watch face does not have permission to receive complication
     * data.
     *
     * <p>Fields will be populated to allow the data to be rendered as if it were of {@link
     * #TYPE_SHORT_TEXT} or {@link #TYPE_ICON} for consistency and convenience, but watch faces may
     * render this as they see fit.
     *
     * <p>It is recommended that, where possible, tapping on the complication when in this state
     * should trigger a permission request.
     */
    public static final int TYPE_NO_PERMISSION = 9;

    /** @hide */
    @IntDef({IMAGE_STYLE_PHOTO, IMAGE_STYLE_ICON})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageStyle {
    }

    /**
     * Style for small images which are photos that are expected to fill the space available. Images
     * of this style may be cropped to fit the shape of the complication - in particular, the image
     * may be cropped to a circle. Photos my not be recolored.
     *
     * <p>This is the default value.
     */
    public static final int IMAGE_STYLE_PHOTO = 1;

    /**
     * Style for small images that have a transparent background and are expected to be drawn
     * entirely within the space available, such as a launcher icon. Watch faces may add padding
     * when drawing these images, but should never crop these images. Icons may be recolored to fit
     * the complication style.
     */
    public static final int IMAGE_STYLE_ICON = 2;

    private static final String FIELD_START_TIME = "START_TIME";
    private static final String FIELD_END_TIME = "END_TIME";
    private static final String FIELD_SHORT_TITLE = "SHORT_TITLE";
    private static final String FIELD_SHORT_TEXT = "SHORT_TEXT";
    private static final String FIELD_LONG_TITLE = "LONG_TITLE";
    private static final String FIELD_LONG_TEXT = "LONG_TEXT";
    private static final String FIELD_VALUE = "VALUE";
    private static final String FIELD_MIN_VALUE = "MIN_VALUE";
    private static final String FIELD_MAX_VALUE = "MAX_VALUE";
    private static final String FIELD_ICON = "ICON";
    private static final String FIELD_ICON_BURN_IN_PROTECTION = "ICON_BURN_IN_PROTECTION";
    private static final String FIELD_SMALL_IMAGE = "SMALL_IMAGE";
    private static final String FIELD_SMALL_IMAGE_BURN_IN_PROTECTION =
            "SMALL_IMAGE_BURN_IN_PROTECTION";
    private static final String FIELD_LARGE_IMAGE = "LARGE_IMAGE";
    private static final String FIELD_TAP_ACTION = "TAP_ACTION";
    private static final String FIELD_IMAGE_STYLE = "IMAGE_STYLE";

    // Originally it was planned to support both content and image content descriptions.
    private static final String FIELD_CONTENT_DESCRIPTION = "IMAGE_CONTENT_DESCRIPTION";

    // Used for validation. REQUIRED_FIELDS[i] is an array containing all the fields which must be
    // populated for @ComplicationType i.
    private static final String[][] REQUIRED_FIELDS = {
            null,
            {}, // NOT_CONFIGURED
            {}, // EMPTY
            {FIELD_SHORT_TEXT}, // SHORT_TEXT
            {FIELD_LONG_TEXT}, // LONG_TEXT
            {FIELD_VALUE, FIELD_MIN_VALUE, FIELD_MAX_VALUE}, // RANGED_VALUE
            {FIELD_ICON}, // ICON
            {FIELD_SMALL_IMAGE, FIELD_IMAGE_STYLE}, // SMALL_IMAGE
            {FIELD_LARGE_IMAGE}, // LARGE_IMAGE
            {}, // TYPE_NO_PERMISSION
            {} // TYPE_NO_DATA
    };

    // Used for validation. OPTIONAL_FIELDS[i] is an array containing all the fields which are
    // valid but not required for type i.
    private static final String[][] OPTIONAL_FIELDS = {
            null,
            {}, // NOT_CONFIGURED
            {}, // EMPTY
            {
                    FIELD_SHORT_TITLE,
                    FIELD_ICON,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    FIELD_TAP_ACTION,
                    FIELD_CONTENT_DESCRIPTION
            }, // SHORT_TEXT
            {
                    FIELD_LONG_TITLE,
                    FIELD_ICON,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    FIELD_SMALL_IMAGE,
                    FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                    FIELD_IMAGE_STYLE,
                    FIELD_TAP_ACTION,
                    FIELD_CONTENT_DESCRIPTION
            }, // LONG_TEXT
            {
                    FIELD_SHORT_TEXT,
                    FIELD_SHORT_TITLE,
                    FIELD_ICON,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    FIELD_TAP_ACTION,
                    FIELD_CONTENT_DESCRIPTION,
            }, // RANGED_VALUE
            {
                    FIELD_TAP_ACTION,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    FIELD_CONTENT_DESCRIPTION
            }, // ICON
            {
                    FIELD_TAP_ACTION,
                    FIELD_SMALL_IMAGE_BURN_IN_PROTECTION,
                    FIELD_CONTENT_DESCRIPTION
            }, // SMALL_IMAGE
            {
                    FIELD_TAP_ACTION, FIELD_CONTENT_DESCRIPTION
            }, // LARGE_IMAGE
            {
                    FIELD_SHORT_TEXT,
                    FIELD_SHORT_TITLE,
                    FIELD_ICON,
                    FIELD_ICON_BURN_IN_PROTECTION,
                    FIELD_CONTENT_DESCRIPTION
            }, // TYPE_NO_PERMISSION
            {} // TYPE_NO_DATA
    };

    @NonNull
    public static final Creator<ComplicationData> CREATOR =
            new Creator<ComplicationData>() {
                @SuppressLint("SyntheticAccessor")
                @NonNull
                @Override
                public ComplicationData createFromParcel(@NonNull Parcel source) {
                    return new ComplicationData(source);
                }

                @NonNull
                @Override
                public ComplicationData[] newArray(int size) {
                    return new ComplicationData[size];
                }
            };

    @ComplicationType
    private final int mType;
    private final Bundle mFields;

    ComplicationData(@NonNull Builder builder) {
        mType = builder.mType;
        mFields = builder.mFields;
    }

    private ComplicationData(@NonNull Parcel in) {
        mType = in.readInt();
        mFields = in.readBundle(getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeBundle(mFields);
    }

    /**
     * Returns the type of this complication data.
     *
     * <p>Will be one of {@link #TYPE_SHORT_TEXT}, {@link #TYPE_LONG_TEXT}, {@link
     * #TYPE_RANGED_VALUE}, {@link #TYPE_ICON}, {@link #TYPE_SMALL_IMAGE}, {@link
     * #TYPE_LARGE_IMAGE}, {@link #TYPE_NOT_CONFIGURED}, {@link #TYPE_EMPTY}, {@link
     * #TYPE_NO_PERMISSION}, or {@link #TYPE_NO_DATA}.
     */
    @ComplicationType
    public int getType() {
        return mType;
    }

    /**
     * Returns true if the complication is active and should be displayed at the given time. If this
     * returns false, the complication should not be displayed.
     *
     * <p>This must be checked for any time for which the complication will be displayed.
     */
    public boolean isActiveAt(long dateTimeMillis) {
        return dateTimeMillis >= mFields.getLong(FIELD_START_TIME, 0)
                && dateTimeMillis <= mFields.getLong(FIELD_END_TIME, Long.MAX_VALUE);
    }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if
     * {@link #getRangedValue} can succeed.
     */
    public boolean hasRangedValue() {
        try {
            return isFieldValidForType(FIELD_VALUE, mType);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>value</i> field for this complication.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_RANGED_VALUE}.
     * Otherwise returns zero.
     */
    public float getRangedValue() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_VALUE, mType);
        return mFields.getFloat(FIELD_VALUE);
    }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if
     * {@link #getRangedMinValue} can succeed.
     */
    public boolean hasRangedMinValue() {
        try {
            return isFieldValidForType(FIELD_MIN_VALUE, mType);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>min value</i> field for this complication.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_RANGED_VALUE}.
     * Otherwise returns zero.
     */
    public float getRangedMinValue() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_MIN_VALUE, mType);
        return mFields.getFloat(FIELD_MIN_VALUE);
    }

    /**
     * Returns true if the ComplicationData contains a ranged max value. I.e. if
     * {@link #getRangedMaxValue} can succeed.
     */
    public boolean hasRangedMaxValue() {
        try {
            return isFieldValidForType(FIELD_MAX_VALUE, mType);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>max value</i> field for this complication.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_RANGED_VALUE}.
     * Otherwise returns zero.
     */
    public float getRangedMaxValue() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_MAX_VALUE, mType);
        return mFields.getFloat(FIELD_MAX_VALUE);
    }

    /**
     * Returns true if the ComplicationData contains a short title. I.e. if {@link #getShortTitle}
     * can succeed.
     */
    public boolean hasShortTitle() {
        try {
            return isFieldValidForType(FIELD_SHORT_TITLE, mType)
                    && (mFields.getParcelable(FIELD_SHORT_TITLE) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>short title</i> field for this complication, or {@code null} if no value was
     * provided for the field.
     *
     * <p>The value is provided as a {@link ComplicationText} object, from which the text to display
     * can be obtained for a given point in time.
     *
     * <p>The length of the text, including any time-dependent values at any valid time, is expected
     * to not exceed seven characters. When using this text, the watch face should be able to
     * display any string of up to seven characters (reducing the text size appropriately if the
     * string is very wide). Although not expected, it is possible that strings of more than seven
     * characters might be seen, in which case they may be truncated.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_SHORT_TEXT}, {@link
     * #TYPE_RANGED_VALUE}, or {@link #TYPE_NO_PERMISSION}.
     * Otherwise returns null.
     */
    @Nullable
    public ComplicationText getShortTitle() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_SHORT_TITLE, mType);
        return getParcelableField(FIELD_SHORT_TITLE);
    }

    /**
     * Returns true if the ComplicationData contains short text. I.e. if {@link #getShortText} can
     * succeed.
     */
    public boolean hasShortText() {
        try {
            return isFieldValidForType(FIELD_SHORT_TEXT, mType)
                    && (mFields.getParcelable(FIELD_SHORT_TEXT) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>short text</i> field for this complication, or {@code null} if no value was
     * provided for the field.
     *
     * <p>The value is provided as a {@link ComplicationText} object, from which the text to display
     * can be obtained for a given point in time.
     *
     * <p>The length of the text, including any time-dependent values at any valid time, is expected
     * to not exceed seven characters. When using this text, the watch face should be able to
     * display any string of up to seven characters (reducing the text size appropriately if the
     * string is very wide). Although not expected, it is possible that strings of more than seven
     * characters might be seen, in which case they may be truncated.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_SHORT_TEXT}, {@link
     * #TYPE_RANGED_VALUE}, or {@link #TYPE_NO_PERMISSION}.
     * Otherwise returns null.
     */
    @Nullable
    public ComplicationText getShortText() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_SHORT_TEXT, mType);
        return getParcelableField(FIELD_SHORT_TEXT);
    }

    /**
     * Returns true if the ComplicationData contains a long title. I.e. if {@link #getLongTitle}
     * can succeed.
     */
    public boolean hasLongTitle() {
        try {
            return isFieldValidForType(FIELD_LONG_TITLE, mType)
                    && (mFields.getParcelable(FIELD_LONG_TITLE) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>long title</i> field for this complication, or {@code null} if no value was
     * provided for the field.
     *
     * <p>The value is provided as a {@link ComplicationText} object, from which the text to display
     * can be obtained for a given point in time.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_LONG_TEXT}.
     * Otherwise returns null.
     */
    @Nullable
    public ComplicationText getLongTitle() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_LONG_TITLE, mType);
        return getParcelableField(FIELD_LONG_TITLE);
    }

    /**
     * Returns true if the ComplicationData contains long text. I.e. if {@link #getLongText} can
     * succeed.
     */
    public boolean hasLongText() {
        try {
            return isFieldValidForType(FIELD_LONG_TEXT, mType)
                    && (mFields.getParcelable(FIELD_LONG_TEXT) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>long text</i> field for this complication.
     *
     * <p>The value is provided as a {@link ComplicationText} object, from which the text to display
     * can be obtained for a given point in time.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_LONG_TEXT}.
     * Otherwise returns null.
     */
    @Nullable
    public ComplicationText getLongText() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_LONG_TEXT, mType);
        return getParcelableField(FIELD_LONG_TEXT);
    }

    /**
     * Returns true if the ComplicationData contains an Icon. I.e. if {@link #getIcon} can succeed.
     */
    public boolean hasIcon() {
        try {
            return isFieldValidForType(FIELD_ICON, mType)
                    && (mFields.getParcelable(FIELD_ICON) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>icon</i> field for this complication, or {@code null} if no value was provided
     * for the field. The image returned is expected to be single-color and so may be tinted to
     * whatever color the watch face requires (but note that {@link Drawable#mutate()} should be
     * called before drawables are tinted).
     *
     * <p>If the device is in ambient mode, and utilises burn-in protection, then the result of
     * {@link #getBurnInProtectionIcon} must be used instead of this.
     *
     * <p>Valid for the types {@link #TYPE_SHORT_TEXT}, {@link #TYPE_LONG_TEXT}, {@link
     * #TYPE_RANGED_VALUE}, {@link #TYPE_ICON}, or {@link #TYPE_NO_PERMISSION}.
     * Otherwise returns null.
     */
    @Nullable
    public Icon getIcon() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_ICON, mType);
        return getParcelableField(FIELD_ICON);
    }

    /**
     * Returns true if the ComplicationData contains a burn in protection Icon. I.e. if
     * {@link #getBurnInProtectionIcon} can succeed.
     */
    public boolean hasBurnInProtectionIcon() {
        try {
            return isFieldValidForType(FIELD_ICON_BURN_IN_PROTECTION, mType)
                    && (mFields.getParcelable(FIELD_ICON_BURN_IN_PROTECTION) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the burn-in protection version of the <i>icon</i> field for this complication, or
     * {@code null} if no such icon was provided. The image returned is expected to be an outline
     * image suitable for use in ambient mode on screens with burn-in protection. The image is also
     * expected to be single-color and so may be tinted to whatever color the watch face requires
     * (but note that {@link Drawable#mutate()} should be called before drawables are tinted, and
     * that the color used should be suitable for ambient mode with burn-in protection).
     *
     * <p>If the device is in ambient mode, and utilises burn-in protection, then the result of this
     * method must be used instead of the result of {@link #getIcon}.
     *
     * <p>Valid for the types {@link #TYPE_SHORT_TEXT}, {@link #TYPE_LONG_TEXT}, {@link
     * #TYPE_RANGED_VALUE}, {@link #TYPE_ICON}, or {@link #TYPE_NO_PERMISSION}.
     * Otherwise returns null.
     */
    @Nullable
    public Icon getBurnInProtectionIcon() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_ICON_BURN_IN_PROTECTION, mType);
        return getParcelableField(FIELD_ICON_BURN_IN_PROTECTION);
    }

    /**
     * Returns true if the ComplicationData contains a small image. I.e. if {@link #getSmallImage}
     * can succeed.
     */
    public boolean hasSmallImage() {
        try {
            return isFieldValidForType(FIELD_SMALL_IMAGE, mType)
                    && (mFields.getParcelable(FIELD_SMALL_IMAGE) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>small image</i> field for this complication, or {@code null} if no value was
     * provided for the field.
     *
     * <p>This may be either a {@link #IMAGE_STYLE_PHOTO photo style} image, which is expected to
     * fill the space available, or an {@link #IMAGE_STYLE_ICON icon style} image, which should be
     * drawn entirely within the space available. Use {@link #getSmallImageStyle} to determine which
     * of these applies.
     *
     * <p>As this may be any image, it is unlikely to be suitable for display in ambient mode when
     * burn-in protection is enabled, or in low-bit ambient mode, and should not be rendered under
     * these circumstances.
     *
     * <p>Valid for the types {@link #TYPE_LONG_TEXT} and {@link #TYPE_SMALL_IMAGE}.
     * Otherwise returns null.
     */
    @Nullable
    public Icon getSmallImage() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_SMALL_IMAGE, mType);
        return getParcelableField(FIELD_SMALL_IMAGE);
    }

    /**
     * Returns true if the ComplicationData contains a burn in protection small image. I.e. if
     * {@link #getBurnInProtectionSmallImage} can succeed.
     *
     * @throws IllegalStateException for invalid types
     */
    public boolean hasBurnInProtectionSmallImage() {
        try {
            return isFieldValidForType(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, mType)
                    && (mFields.getParcelable(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the burn-in protection version of the <i>small image</i> field for this complication,
     * or {@code null} if no such icon was provided. The image returned is expected to be an outline
     * image suitable for use in ambient mode on screens with burn-in protection. The image is also
     * expected to be single-color and so may be tinted to whatever color the watch face requires
     * (but note that {@link Drawable#mutate()} should be called before drawables are tinted, and
     * that the color used should be suitable for ambient mode with burn-in protection).
     *
     * <p>If the device is in ambient mode, and utilises burn-in protection, then the result of this
     * method must be used instead of the result of {@link #getSmallImage()}.
     *
     * <p>Valid for the types {@link #TYPE_LONG_TEXT} and {@link #TYPE_SMALL_IMAGE}.
     * Otherwise returns null.
     */
    @Nullable
    public Icon getBurnInProtectionSmallImage() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, mType);
        return getParcelableField(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION);
    }

    /**
     * Returns the <i>small image style</i> field for this complication.
     *
     * <p>The result of this method should be taken in to account when drawing a small image
     * complication.
     *
     * <p>Valid only for types that contain small images, i.e. {@link #TYPE_SMALL_IMAGE} and {@link
     * #TYPE_LONG_TEXT}.
     * Otherwise returns zero.
     *
     * @see #IMAGE_STYLE_PHOTO which can be cropped but not recolored.
     * @see #IMAGE_STYLE_ICON which can be recolored but not cropped.
     */
    @ImageStyle
    public int getSmallImageStyle() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_IMAGE_STYLE, mType);
        return mFields.getInt(FIELD_IMAGE_STYLE);
    }

    /**
     * Returns true if the ComplicationData contains a large image. I.e. if {@link #getLargeImage}
     * can succeed.
     */
    public boolean hasLargeImage() {
        try {
            return isFieldValidForType(FIELD_LARGE_IMAGE, mType)
                    && (mFields.getParcelable(FIELD_LARGE_IMAGE) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>large image</i> field for this complication. This image is expected to be of a
     * suitable size to fill the screen of the watch.
     *
     * <p>As this may be any image, it is unlikely to be suitable for display in ambient mode when
     * burn-in protection is enabled, or in low-bit ambient mode, and should not be rendered under
     * these circumstances.
     *
     * <p>Valid only if the type of this complication data is {@link #TYPE_LARGE_IMAGE}.
     * Otherwise returns null.
     */
    @Nullable
    public Icon getLargeImage() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_LARGE_IMAGE, mType);
        return getParcelableField(FIELD_LARGE_IMAGE);
    }

    /**
     * Returns true if the ComplicationData contains a tap action. I.e. if {@link #getTapAction}
     * can succeed.
     */
    public boolean hasTapAction() {
        try {
            return isFieldValidForType(FIELD_TAP_ACTION, mType)
                    && (mFields.getParcelable(FIELD_TAP_ACTION) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>tap action</i> field for this complication. The result is a {@link
     * PendingIntent} that should be fired if the complication is tapped on, assuming the
     * complication is tappable, or {@code null} if no tap action has been specified.
     *
     * <p>Valid for all non-empty types.
     * Otherwise returns null.
     */
    @Nullable
    public PendingIntent getTapAction() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_TAP_ACTION, mType);
        return getParcelableField(FIELD_TAP_ACTION);
    }

    /**
     * Returns true if the ComplicationData contains a content description. I.e. if
     * {@link #getContentDescription} can succeed.
     */
    public boolean hasContentDescription() {
        try {
            return isFieldValidForType(FIELD_CONTENT_DESCRIPTION, mType)
                    && (mFields.getParcelable(FIELD_CONTENT_DESCRIPTION) != null);
        } catch (BadParcelableException e) {
            return false;
        }
    }

    /**
     * Returns the <i>content description </i> field for this complication, for screen readers. This
     * usually describes the image, but may also describe the overall complication.
     *
     * <p>Valid for all non-empty types.
     */
    @Nullable
    public ComplicationText getContentDescription() {
        checkFieldValidForTypeWithoutThrowingException(FIELD_CONTENT_DESCRIPTION, mType);
        return getParcelableField(FIELD_CONTENT_DESCRIPTION);
    }

    /**
     * Returns the start time for this complication data (i.e. the first time at which it should
     * be considered active and displayed), this may be 0. See also {@link #isActiveAt(long)}.
     */
    public long getStartDateTimeMillis() {
        return mFields.getLong(FIELD_START_TIME, 0);
    }

    /**
     * Returns the end time for this complication data (i.e. the last time at which it should be
     * considered active and displayed), this may be {@link Long#MAX_VALUE}. See also {@link
     * #isActiveAt(long)}.
     */
    public long getEndDateTimeMillis() {
        return mFields.getLong(FIELD_END_TIME, Long.MAX_VALUE);
    }

    /**
     * Returns true if the complication data contains at least one text field with a value that may
     * change based on the current time.
     */
    public boolean isTimeDependent() {
        return isTimeDependentField(FIELD_SHORT_TEXT)
                || isTimeDependentField(FIELD_SHORT_TITLE)
                || isTimeDependentField(FIELD_LONG_TEXT)
                || isTimeDependentField(FIELD_LONG_TITLE);
    }

    private boolean isTimeDependentField(String field) {
        ComplicationText text = getParcelableField(field);

        return text != null && text.isTimeDependent();
    }

    private static boolean isFieldValidForType(String field, @ComplicationType int type) {
        for (String requiredField : REQUIRED_FIELDS[type]) {
            if (requiredField.equals(field)) {
                return true;
            }
        }
        for (String optionalField : OPTIONAL_FIELDS[type]) {
            if (optionalField.equals(field)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTypeSupported(int type) {
        return 1 <= type && type <= REQUIRED_FIELDS.length;
    }

    /**
     * The unparceling logic needs to remain backward compatible.
     */
    private static void checkFieldValidForTypeWithoutThrowingException(
            String field, @ComplicationType int type) {
        if (!isTypeSupported(type)) {
            Log.w(TAG, "Type " + type + " can not be recognized");
            return;
        }
        if (!isFieldValidForType(field, type)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Field " + field + " is not supported for type " + type);
            }
        }
    }

    private static void checkFieldValidForType(String field, @ComplicationType int type) {
        if (!isTypeSupported(type)) {
            throw new IllegalStateException("Type " + type + " can not be recognized");
        }
        if (!isFieldValidForType(field, type)) {
            throw new IllegalStateException(
                    "Field " + field + " is not supported for type " + type);
        }
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends Parcelable> T getParcelableField(String field) {
        try {
            return mFields.getParcelable(field);
        } catch (BadParcelableException e) {
            Log.w(
                    TAG,
                    "Could not unparcel ComplicationData. Provider apps must exclude wearable "
                            + "support complication classes from proguard.",
                    e);
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ComplicationData{" + "mType=" + mType + ", mFields=" + mFields + '}';
    }

    /** Builder class for {@link ComplicationData}. */
    public static final class Builder {
        @ComplicationType
        final int mType;
        final Bundle mFields;

        /** Creates a builder from given {@link ComplicationData}, copying its type and data. */
        @SuppressLint("SyntheticAccessor")
        public Builder(@NonNull ComplicationData data) {
            mType = data.getType();
            mFields = (Bundle) data.mFields.clone();
        }

        public Builder(@ComplicationType int type) {
            mType = type;
            mFields = new Bundle();
            if (type == TYPE_SMALL_IMAGE || type == TYPE_LONG_TEXT) {
                setSmallImageStyle(IMAGE_STYLE_PHOTO);
            }
        }

        /**
         * Sets the start time for this complication data. This is optional for any type.
         *
         * <p>The complication data will be considered inactive (i.e. should not be displayed) if
         * the current time is less than the start time. If not specified, the data is considered
         * active for all time up to the end time (or always active if end time is also not
         * specified).
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder setStartDateTimeMillis(long startDateTimeMillis) {
            mFields.putLong(FIELD_START_TIME, startDateTimeMillis);
            return this;
        }

        /**
         * Removes the start time for this complication data.
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder clearStartDateTime() {
            mFields.remove(FIELD_START_TIME);
            return this;
        }

        /**
         * Sets the end time for this complication data. This is optional for any type.
         *
         * <p>The complication data will be considered inactive (i.e. should not be displayed) if
         * the current time is greater than the end time. If not specified, the data is considered
         * active for all time after the start time (or always active if start time is also not
         * specified).
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder setEndDateTimeMillis(long endDateTimeMillis) {
            mFields.putLong(FIELD_END_TIME, endDateTimeMillis);
            return this;
        }

        /**
         * Removes the end time for this complication data.
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder clearEndDateTime() {
            mFields.remove(FIELD_END_TIME);
            return this;
        }

        /**
         * Sets the <i>value</i> field. This is required for the {@link #TYPE_RANGED_VALUE} type,
         * and is not valid for any other type. A {@link #TYPE_RANGED_VALUE} complication
         * visually presents a single value, which is usually a percentage. E.g. you
         * have completed 70% of today's target of 10000 steps.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setRangedValue(float value) {
            putFloatField(FIELD_VALUE, value);
            return this;
        }

        /**
         * Sets the <i>min value</i> field. This is required for the {@link #TYPE_RANGED_VALUE}
         * type, and is not valid for any other type. A {@link #TYPE_RANGED_VALUE} complication
         * visually presents a single value, which is usually a percentage. E.g. you have
         * completed 70% of today's target of 10000 steps.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setRangedMinValue(float minValue) {
            putFloatField(FIELD_MIN_VALUE, minValue);
            return this;
        }

        /**
         * Sets the <i>max value</i> field. This is required for the {@link #TYPE_RANGED_VALUE}
         * type, and is not valid for any other type.A {@link #TYPE_RANGED_VALUE} complication
         * visually presents a single value, which is usually a percentage. E.g. you have
         * completed 70% of today's target of 10000 steps.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setRangedMaxValue(float maxValue) {
            putFloatField(FIELD_MAX_VALUE, maxValue);
            return this;
        }

        /**
         * Sets the <i>long title</i> field. This is optional for the {@link #TYPE_LONG_TEXT} type,
         * and is not valid for any other type.
         *
         * <p>The value must be provided as a {@link ComplicationText} object, so that
         * time-dependent values may be included.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setLongTitle(@Nullable ComplicationText longTitle) {
            putOrRemoveField(FIELD_LONG_TITLE, longTitle);
            return this;
        }

        /**
         * Sets the <i>long text</i> field. This is required for the {@link #TYPE_LONG_TEXT} type,
         * and is not valid for any other type.
         *
         * <p>The value must be provided as a {@link ComplicationText} object, so that
         * time-dependent values may be included.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setLongText(@Nullable ComplicationText longText) {
            putOrRemoveField(FIELD_LONG_TEXT, longText);
            return this;
        }

        /**
         * Sets the <i>short title</i> field. This is valid for the {@link #TYPE_SHORT_TEXT}, {@link
         * #TYPE_RANGED_VALUE}, and {@link #TYPE_NO_PERMISSION} types, and is not valid for any
         * other type.
         *
         * <p>The value must be provided as a {@link ComplicationText} object, so that
         * time-dependent values may be included.
         *
         * <p>The length of the text, including any time-dependent values, should not exceed seven
         * characters. If it does, the text may be truncated by the watch face or might not fit in
         * the complication.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setShortTitle(@Nullable ComplicationText shortTitle) {
            putOrRemoveField(FIELD_SHORT_TITLE, shortTitle);
            return this;
        }

        /**
         * Sets the <i>short text</i> field. This is required for the {@link #TYPE_SHORT_TEXT} type,
         * is optional for the {@link #TYPE_RANGED_VALUE} and {@link #TYPE_NO_PERMISSION} types, and
         * is not valid for any other type.
         *
         * <p>The value must be provided as a {@link ComplicationText} object, so that
         * time-dependent values may be included.
         *
         * <p>The length of the text, including any time-dependent values, should not exceed seven
         * characters. If it does, the text may be truncated by the watch face or might not fit in
         * the complication.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setShortText(@Nullable ComplicationText shortText) {
            putOrRemoveField(FIELD_SHORT_TEXT, shortText);
            return this;
        }

        /**
         * Sets the <i>icon</i> field. This is required for the {@link #TYPE_ICON} type, and is
         * optional for the {@link #TYPE_SHORT_TEXT}, {@link #TYPE_LONG_TEXT}, {@link
         * #TYPE_RANGED_VALUE}, and {@link #TYPE_NO_PERMISSION} types.
         *
         * <p>The provided image must be single-color, so that watch faces can tint it as required.
         *
         * <p>If the icon provided here is not suitable for display in ambient mode with burn-in
         * protection (e.g. if it includes solid blocks of pixels), then a burn-in safe version of
         * the icon must be provided via {@link #setBurnInProtectionIcon}.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setIcon(@Nullable Icon icon) {
            putOrRemoveField(FIELD_ICON, icon);
            return this;
        }

        /**
         * Sets the burn-in protection version of the <i>icon</i> field. This should be provided if
         * the <i>icon</i> field is provided, unless the main icon is already safe for use with
         * burn-in protection.  This icon should have fewer lit pixels, and should use darker
         * colors to prevent LCD burn in issues.
         *
         * <p>The provided image must be single-color, so that watch faces can tint it as required.
         *
         * <p>The provided image must not contain solid blocks of pixels - it should instead be
         * composed of outlines or lines only.
         *
         * <p>If this field is set, the <i>icon</i> field must also be set.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setBurnInProtectionIcon(@Nullable Icon icon) {
            putOrRemoveField(FIELD_ICON_BURN_IN_PROTECTION, icon);
            return this;
        }

        /**
         * Sets the <i>small image</i> field. This is required for the {@link #TYPE_SMALL_IMAGE}
         * type, and is optional for the {@link #TYPE_LONG_TEXT} type.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setSmallImage(@Nullable Icon smallImage) {
            putOrRemoveField(FIELD_SMALL_IMAGE, smallImage);
            return this;
        }

        /**
         * Sets the burn-in protection version of the <i>small image</i> field. This should be
         * provided if the <i>small image</i> field is provided, unless the main small image is
         * already safe for use with burn-in protection.
         *
         * <p>The provided image must not contain solid blocks of pixels - it should instead be
         * composed of outlines or lines only.
         *
         * <p>If this field is set, the <i>small image</i> field must also be set.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setBurnInProtectionSmallImage(@Nullable Icon smallImage) {
            putOrRemoveField(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION, smallImage);
            return this;
        }

        /**
         * Sets the display style for this complication data. This is valid only for types that
         * contain small images, i.e. {@link #TYPE_SMALL_IMAGE} and {@link #TYPE_LONG_TEXT}.
         *
         * <p>This affects how watch faces will draw the image in the complication.
         *
         * <p>If not specified, the default is {@link #IMAGE_STYLE_PHOTO}.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         * @see #IMAGE_STYLE_PHOTO which can be cropped but not recolored.
         * @see #IMAGE_STYLE_ICON which can be recolored but not cropped.
         */
        @NonNull
        public Builder setSmallImageStyle(@ImageStyle int imageStyle) {
            putIntField(FIELD_IMAGE_STYLE, imageStyle);
            return this;
        }

        /**
         * Sets the <i>large image</i> field. This is required for the {@link #TYPE_LARGE_IMAGE}
         * type, and is not valid for any other type.
         *
         * <p>The provided image should be suitably sized to fill the screen of the watch.
         *
         * <p>Returns this Builder to allow chaining.
         *
         * @throws IllegalStateException if this field is not valid for the complication type
         */
        @NonNull
        public Builder setLargeImage(@Nullable Icon largeImage) {
            putOrRemoveField(FIELD_LARGE_IMAGE, largeImage);
            return this;
        }

        /**
         * Sets the <i>tap action</i> field. This is optional for any non-empty type.
         *
         * <p>The provided {@link PendingIntent} may be fired if the complication is tapped on. Note
         * that some complications might not be tappable, in which case this field will be ignored.
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder setTapAction(@Nullable PendingIntent pendingIntent) {
            putOrRemoveField(FIELD_TAP_ACTION, pendingIntent);
            return this;
        }

        /**
         * Sets the <i>content description</i> field for accessibility. This is optional for any
         * non-empty type. It is recommended to provide a content description whenever the
         * data includes an image.
         *
         * <p>The provided text will be read aloud by a Text-to-speech converter for users who may
         * be vision-impaired. It will be read aloud in addition to any long, short, or range text
         * in the complication.
         *
         * <p>If using to describe an image/icon that is purely stylistic and doesn't convey any
         * information to the user, you may set the image content description to an empty string
         * ("").
         *
         * <p>Returns this Builder to allow chaining.
         */
        @NonNull
        public Builder setContentDescription(@Nullable ComplicationText description) {
            putOrRemoveField(FIELD_CONTENT_DESCRIPTION, description);
            return this;
        }

        /**
         * Constructs and returns {@link ComplicationData} with the provided fields. All required
         * fields must be populated before this method is called.
         *
         * @throws IllegalStateException if the required fields have not been populated
         */
        @NonNull
        @SuppressLint("SyntheticAccessor")
        public ComplicationData build() {
            // Validate.
            for (String requiredField : REQUIRED_FIELDS[mType]) {
                if (!mFields.containsKey(requiredField)) {
                    throw new IllegalStateException(
                            "Field " + requiredField + " is required for type " + mType);
                }

                if (mFields.containsKey(FIELD_ICON_BURN_IN_PROTECTION)
                        && !mFields.containsKey(FIELD_ICON)) {
                    throw new IllegalStateException(
                            "Field ICON must be provided when field ICON_BURN_IN_PROTECTION is"
                                    + " provided.");
                }

                if (mFields.containsKey(FIELD_SMALL_IMAGE_BURN_IN_PROTECTION)
                        && !mFields.containsKey(FIELD_SMALL_IMAGE)) {
                    throw new IllegalStateException(
                            "Field SMALL_IMAGE must be provided when field"
                                    + " SMALL_IMAGE_BURN_IN_PROTECTION is provided.");
                }
            }

            return new ComplicationData(this);
        }

        @SuppressLint("SyntheticAccessor")
        private void putIntField(@NonNull String field, int value) {
            ComplicationData.checkFieldValidForType(field, mType);
            mFields.putInt(field, value);
        }

        @SuppressLint("SyntheticAccessor")
        private void putFloatField(@NonNull String field, float value) {
            ComplicationData.checkFieldValidForType(field, mType);
            mFields.putFloat(field, value);
        }

        /** Sets the field with obj or removes it if null. */
        @SuppressLint("SyntheticAccessor")
        private void putOrRemoveField(@NonNull String field, @Nullable Object obj) {
            ComplicationData.checkFieldValidForType(field, mType);
            if (obj == null) {
                mFields.remove(field);
                return;
            }
            if (obj instanceof String) {
                mFields.putString(field, (String) obj);
            } else if (obj instanceof Parcelable) {
                mFields.putParcelable(field, (Parcelable) obj);
            } else {
                throw new IllegalArgumentException("Unexpected object type: " + obj.getClass());
            }
        }
    }
}
