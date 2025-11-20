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

package androidx.wear.protolayout.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS;
import static androidx.wear.protolayout.material.ButtonDefaults.DEFAULT_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.EXTRA_LARGE_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.LARGE_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.PRIMARY_COLORS;
import static androidx.wear.protolayout.materialcore.Button.Builder.CUSTOM_CONTENT;
import static androidx.wear.protolayout.materialcore.Button.Builder.ICON;
import static androidx.wear.protolayout.materialcore.Button.Builder.IMAGE;
import static androidx.wear.protolayout.materialcore.Button.Builder.NOT_SET;
import static androidx.wear.protolayout.materialcore.Button.Builder.TEXT;
import static androidx.wear.protolayout.materialcore.Button.METADATA_TAG_CUSTOM_CONTENT;
import static androidx.wear.protolayout.materialcore.Button.METADATA_TAG_ICON;
import static androidx.wear.protolayout.materialcore.Button.METADATA_TAG_IMAGE;
import static androidx.wear.protolayout.materialcore.Button.METADATA_TAG_TEXT;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.staticString;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter;
import androidx.wear.protolayout.LayoutElementBuilders.Image;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.material.Typography.TypographyName;
import androidx.wear.protolayout.materialcore.Button.Builder.ButtonType;
import androidx.wear.protolayout.proto.LayoutElementProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * ProtoLayout component {@link Button} that represents clickable button with the given content.
 *
 * <p>The Button is circular in shape. The recommended sizes are defined in {@link ButtonDefaults}.
 *
 * <p>The recommended set of {@link ButtonColors} styles can be obtained from {@link
 * ButtonDefaults}., e.g. {@link ButtonDefaults#PRIMARY_COLORS} to get a color scheme for a primary
 * {@link Button}.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * Button button = new Button...
 * Box box = new Box.Builder().addContent(button).build();
 *
 * Button myButton = (Button) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link Button} object from any layout element, {@link #fromLayoutElement}
 * method should be used, i.e.:
 *
 * <pre>{@code
 * Button myButton = Button.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
public class Button implements LayoutElement {
    private final androidx.wear.protolayout.materialcore.@NonNull Button mElement;

    Button(androidx.wear.protolayout.materialcore.@NonNull Button element) {
        mElement = element;
    }

    /** Builder class for {@link Button}. */
    public static final class Builder implements LayoutElement.Builder {
        private final @NonNull Context mContext;
        private @Nullable LayoutElement mCustomContent;
        private @NonNull DpProp mSize = DEFAULT_SIZE;
        private @Nullable String mText = null;
        private @Nullable Integer mTypographyName = null;
        private @Nullable String mIcon = null;
        private @Nullable DpProp mIconSize = null;
        private @Nullable String mImage = null;
        private @NonNull ButtonColors mButtonColors = PRIMARY_COLORS;
        @ButtonType private int mType = NOT_SET;
        private final androidx.wear.protolayout.materialcore.Button.@NonNull Builder mCoreBuilder;

        /**
         * Creates a builder for the {@link Button} from the given content. Custom content should be
         * later set with one of the following {@link #setIconContent(String)},
         * {@link #setIconContent(String, DpProp)}, {@link #setTextContent(String)},
         * {@link #setTextContent(String, int)} or {@link #setImageContent(String)}.
         *
         * @param context The application's context.
         * @param clickable Associated {@link Clickable} for click events. When the Button is
         *     clicked it will fire the associated action.
         */
        public Builder(@NonNull Context context, @NonNull Clickable clickable) {
            mContext = context;
            mCoreBuilder =
                    new androidx.wear.protolayout.materialcore.Button.Builder(clickable)
                            .setSize(mSize)
                            .setBackgroundColor(mButtonColors.getBackgroundColor());
        }

        /**
         * Sets the static content description for the {@link Button}. It is highly recommended to
         * provide this for button containing icon or image.
         */
        public @NonNull Builder setContentDescription(@NonNull CharSequence contentDescription) {
            return setContentDescription(staticString(contentDescription.toString()));
        }

        /**
         * Sets the content description for the {@link Button}. It is highly recommended to provide
         * this for button containing icon or image.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @NonNull Builder setContentDescription(@NonNull StringProp contentDescription) {
            mCoreBuilder.setContentDescription(contentDescription);
            return this;
        }

        /**
         * Sets the size for the {@link Button}. Strongly recommended values are {@link
         * ButtonDefaults#DEFAULT_SIZE}, {@link ButtonDefaults#LARGE_SIZE} and {@link
         * ButtonDefaults#EXTRA_LARGE_SIZE}. If not set, {@link ButtonDefaults#DEFAULT_SIZE} will be
         * used.
         */
        public @NonNull Builder setSize(@NonNull DpProp size) {
            mSize = size;
            mCoreBuilder.setSize(size);
            return this;
        }

        /**
         * Sets the size for the {@link Button}. Strongly recommended values are {@link
         * ButtonDefaults#DEFAULT_SIZE}, {@link ButtonDefaults#LARGE_SIZE} and {@link
         * ButtonDefaults#EXTRA_LARGE_SIZE}. If not set, {@link ButtonDefaults#DEFAULT_SIZE} will be
         * used.
         */
        public @NonNull Builder setSize(@Dimension(unit = DP) float size) {
            return setSize(dp(size));
        }

        /**
         * Sets the colors for the {@link Button}. If not set, {@link ButtonDefaults#PRIMARY_COLORS}
         * will be used.
         *
         * <p>Note: The content color will be ignored (and won't be returned by the getter) if the
         * Button content is an image.
         */
        public @NonNull Builder setButtonColors(@NonNull ButtonColors buttonColors) {
            mButtonColors = buttonColors;
            mCoreBuilder.setBackgroundColor(buttonColors.getBackgroundColor());
            return this;
        }

        /**
         * Sets the custom content for this Button. Any previously added content will be overridden.
         */
        public @NonNull Builder setCustomContent(@NonNull LayoutElement content) {
            resetContent();
            this.mCustomContent = content;
            this.mType = CUSTOM_CONTENT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon with the given size. Any previously
         * added content will be overridden. Provided icon will be tinted to the given content color
         * from {@link ButtonColors} and with the given size. This icon should be image with chosen
         * alpha channel and not an actual image.
         */
        public @NonNull Builder setIconContent(@NonNull String imageResourceId,
                @NonNull DpProp size) {
            resetContent();
            this.mIcon = imageResourceId;
            this.mType = ICON;
            this.mIconSize = size;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon with the default size that is half
         * of the set size of the button. Any previously added content will be overridden. Provided
         * icon will be tinted to the given content color from {@link ButtonColors}. This icon
         * should be image with chosen alpha channel and not an actual image.
         */
        public @NonNull Builder setIconContent(@NonNull String imageResourceId) {
            resetContent();
            this.mIcon = imageResourceId;
            this.mType = ICON;
            return this;
        }

        /**
         * Sets the content of this Button to be the given text with the default font for the set
         * size (for the {@link ButtonDefaults#DEFAULT_SIZE}, {@link ButtonDefaults#LARGE_SIZE} and
         * {@link ButtonDefaults#EXTRA_LARGE_SIZE} is {@link Typography#TYPOGRAPHY_TITLE2}, {@link
         * Typography#TYPOGRAPHY_TITLE1} and {@link Typography#TYPOGRAPHY_DISPLAY3} respectively).
         * Any previously added content will be overridden. Text should contain no more than 3
         * characters, otherwise it will overflow from the edges.
         */
        public @NonNull Builder setTextContent(@NonNull String text) {
            resetContent();
            this.mText = text;
            this.mType = TEXT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given text with the given font. If you need
         * more font related customization, consider using {@link #setCustomContent} with {@link
         * Text} component. Any previously added content will be overridden. Text should contain no
         * more than 3 characters, otherwise it will overflow from the edges.
         */
        public @NonNull Builder setTextContent(@NonNull String text,
                @TypographyName int typographyName) {
            resetContent();
            this.mText = text;
            this.mTypographyName = typographyName;
            this.mType = TEXT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given image, i.e. contacts photo. Any
         * previously added content will be overridden.
         */
        public @NonNull Builder setImageContent(@NonNull String imageResourceId) {
            resetContent();
            this.mImage = imageResourceId;
            this.mType = IMAGE;
            return this;
        }

        private void resetContent() {
            this.mText = null;
            this.mTypographyName = null;
            this.mIcon = null;
            this.mImage = null;
            this.mCustomContent = null;
            this.mIconSize = null;
        }

        /** Constructs and returns {@link Button} with the provided field and look. */
        @Override
        public @NonNull Button build() {
            // getCorrectContent will apply styling.
            mCoreBuilder.setContent(getCorrectContent(), mType);

            return new Button(mCoreBuilder.build());
        }

        @SuppressWarnings("deprecation") // Using Image.Builder as legacy
        private @NonNull LayoutElement getCorrectContent() {
            LayoutElement.Builder content;
            switch (mType) {
                case ICON:
                    {
                        DpProp iconSize =
                                mIconSize != null
                                        ? mIconSize
                                        : ButtonDefaults.recommendedIconSize(mSize);
                        content =
                                new Image.Builder()
                                        .setResourceId(checkNotNull(mIcon))
                                        .setHeight(checkNotNull(iconSize))
                                        .setWidth(iconSize)
                                        .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                                        .setColorFilter(
                                                new ColorFilter.Builder()
                                                        .setTint(mButtonColors.getContentColor())
                                                        .build());

                        return content.build();
                    }
                case TEXT:
                    {
                        @TypographyName
                        int typographyName =
                                mTypographyName != null
                                        ? mTypographyName
                                        : getDefaultTypographyForSize(mSize);
                        content =
                                new Text.Builder(mContext, checkNotNull(mText))
                                        .setMaxLines(1)
                                        .setTypography(typographyName)
                                        .setColor(mButtonColors.getContentColor());

                        return content.build();
                    }
                case IMAGE:
                    {
                        content =
                                new Image.Builder()
                                        .setResourceId(checkNotNull(mImage))
                                        .setHeight(mSize)
                                        .setWidth(mSize)
                                        .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS);
                        return content.build();
                    }
                case CUSTOM_CONTENT:
                    return checkNotNull(mCustomContent);
                case NOT_SET:
                    // Shouldn't happen.
                default:
                    // Shouldn't happen.
                    throw new IllegalArgumentException("Wrong Button type");
            }
        }

        @TypographyName
        private static int getDefaultTypographyForSize(@NonNull DpProp size) {
            if (size.getValue() == LARGE_SIZE.getValue()) {
                return Typography.TYPOGRAPHY_TITLE1;
            } else if (size.getValue() == EXTRA_LARGE_SIZE.getValue()) {
                return Typography.TYPOGRAPHY_DISPLAY3;
            } else {
                return Typography.TYPOGRAPHY_TITLE2;
            }
        }
    }

    /**
     * Returns the custom content of this Button if it has been added. Otherwise, it returns null.
     */
    public @Nullable LayoutElement getCustomContent() {
        if (!getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return null;
        }
        return getAnyContent();
    }

    /** Returns the icon content of this Button if it has been added. Otherwise, it returns null. */
    public @Nullable String getIconContent() {
        Image icon = getIconContentObject();
        return icon != null ? checkNotNull(icon.getResourceId()).getValue() : null;
    }

    /**
     * Returns the image content of this Button if it has been added. Otherwise, it returns null.
     */
    public @Nullable String getImageContent() {
        Image image = getImageContentObject();
        return image != null ? checkNotNull(image.getResourceId()).getValue() : null;
    }

    /** Returns the text content of this Button if it has been added. Otherwise, it returns null. */
    public @Nullable String getTextContent() {
        Text text = getTextContentObject();
        return text != null ? text.getText().getValue() : null;
    }

    private @NonNull LayoutElement getAnyContent() {
        return checkNotNull(mElement.getContent());
    }

    private @Nullable Image getIconContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_ICON)) {
            return null;
        }
        return (Image) getAnyContent();
    }

    private @Nullable Text getTextContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_TEXT)) {
            return null;
        }
        return Text.fromLayoutElement(getAnyContent());
    }

    private @Nullable Image getImageContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_IMAGE)) {
            return null;
        }
        return (Image) getAnyContent();
    }

    /** Returns click event action associated with this Button. */
    public @NonNull Clickable getClickable() {
        return mElement.getClickable();
    }

    /** Returns content description for this Button. */
    public @Nullable StringProp getContentDescription() {
        return mElement.getContentDescription();
    }

    /** Returns size for this Button. */
    public @NonNull ContainerDimension getSize() {
        return mElement.getSize();
    }

    /**
     * Returns button color of this Button.
     *
     * <p>Note that the content color will be unset if the content of this Button is an image.
     */
    public @NonNull ButtonColors getButtonColors() {
        ColorProp backgroundColor = mElement.getBackgroundColor();
        ColorProp contentColor = null;

        switch (getMetadataTag()) {
            case METADATA_TAG_TEXT:
                contentColor = checkNotNull(getTextContentObject()).getColor();
                break;
            case METADATA_TAG_ICON:
                contentColor =
                        checkNotNull(checkNotNull(getIconContentObject()).getColorFilter())
                                .getTint();
                break;
            case METADATA_TAG_IMAGE:
            case METADATA_TAG_CUSTOM_CONTENT:
                break;
            default: // fall out
        }

        if (contentColor == null) {
            contentColor = new ColorProp.Builder(0).build();
        }

        return new ButtonColors(backgroundColor, contentColor);
    }

    /** Returns metadata tag set to this Button. */
    @NonNull String getMetadataTag() {
        return mElement.getMetadataTag();
    }

    /**
     * Returns Button object from the given LayoutElement (e.g. one retrieved from a container's
     * content with {@code container.getContents().get(index)}) if that element can be converted to
     * Button. Otherwise, it will return null.
     */
    public static @Nullable Button fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof Button) {
            return (Button) element;
        }
        androidx.wear.protolayout.materialcore.Button coreButton =
                androidx.wear.protolayout.materialcore.Button.fromLayoutElement(element);
        // Now we are sure that this element is a Button.
        return coreButton == null ? null : new Button(coreButton);
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
        return checkNotNull(mElement.toLayoutElementProto());
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @Nullable Fingerprint getFingerprint() {
        return mElement.getFingerprint();
    }
}
