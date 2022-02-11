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

package androidx.wear.tiles.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS;
import static androidx.wear.tiles.material.ButtonDefaults.DEFAULT_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.EXTRA_LARGE_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.LARGE_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.PRIMARY_BUTTON_COLORS;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.Helper.radiusOf;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ActionBuilders.Action;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.ColorFilter;
import androidx.wear.tiles.LayoutElementBuilders.FontStyle;
import androidx.wear.tiles.LayoutElementBuilders.Image;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.ModifiersBuilders.Background;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.ModifiersBuilders.Corner;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.material.Typography.TypographyName;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Tiles component {@link Button} that represents clickable button with the given content.
 *
 * <p>The Button is circular in shape. The recommended sizes are defined in {@link ButtonDefaults}.
 *
 * <p>The recommended set of {@link ButtonColors} styles can be obtained from {@link
 * ButtonDefaults}., e.g. {@link ButtonDefaults#PRIMARY_BUTTON_COLORS} to get a color scheme for a
 * primary {@link Button} which by default will have a solid background of {@link Colors#PRIMARY}
 * and content color of {@link Colors#ON_PRIMARY}.
 */
public class Button implements LayoutElement {
    @NonNull private final Box mElement;

    Button(@NonNull Box element) {
        mElement = element;
    }

    /** Builder class for {@link Button}. */
    public static final class Builder implements LayoutElement.Builder {
        private static final int NOT_SET = -1;
        private static final int ICON = 0;
        private static final int TEXT = 1;
        private static final int IMAGE = 2;
        private static final int CUSTOM_CONTENT = 3;

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, ICON, TEXT, IMAGE, CUSTOM_CONTENT})
        @interface ButtonType {}

        @Nullable private LayoutElement mCustomContent;
        @Nullable private LayoutElement.Builder mContent;
        @NonNull private final Action mAction;
        @NonNull private final String mClickableId;
        @NonNull private String mContentDescription = "";
        @NonNull private DpProp mSize = DEFAULT_BUTTON_SIZE;
        @Nullable private String mText = null;
        private @TypographyName int mTypographyName =
                getDefaultTypographyForSize(DEFAULT_BUTTON_SIZE);
        private boolean mIsTypographyNameSet = false;
        @Nullable private String mIcon = null;
        @Nullable private DpProp mIconSize = null;
        @Nullable private String mImage = null;
        @NonNull private ButtonColors mButtonColors = PRIMARY_BUTTON_COLORS;
        private @ButtonType int mType = NOT_SET;
        private boolean mDefaultSize = false;

        /**
         * Creates a builder for the {@link Button} from the given content. Custom content should be
         * later set with one of the following ({@link #setIconContent}, {@link #setTextContent},
         * {@link #setImageContent}.
         *
         * @param action Associated Actions for click events. When the Button is clicked it will
         *     fire the associated action.
         * @param clickableId The ID associated with the given action.
         */
        // Action is not a functional interface (and should not be used as one), suppress the
        // warning.
        @SuppressWarnings("LambdaLast")
        public Builder(@NonNull Action action, @NonNull String clickableId) {
            mAction = action;
            mClickableId = clickableId;
        }

        /**
         * Sets the content description for the {@link Button}. It is highly recommended to provide
         * this for button containing icon or image.
         */
        @NonNull
        public Builder setContentDescription(@NonNull String contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        /**
         * Sets the size for the {@link Button}. Strongly recommended values are {@link
         * ButtonDefaults#DEFAULT_BUTTON_SIZE}, {@link ButtonDefaults#LARGE_BUTTON_SIZE} and {@link
         * ButtonDefaults#EXTRA_LARGE_BUTTON_SIZE}. If not set, {@link
         * ButtonDefaults#DEFAULT_BUTTON_SIZE} will be used.
         */
        @NonNull
        public Builder setSize(@NonNull DpProp size) {
            mSize = size;
            return this;
        }

        /**
         * Sets the size for the {@link Button}. Strongly recommended values are {@link
         * ButtonDefaults#DEFAULT_BUTTON_SIZE}, {@link ButtonDefaults#LARGE_BUTTON_SIZE} and {@link
         * ButtonDefaults#EXTRA_LARGE_BUTTON_SIZE}. If not set, {@link
         * ButtonDefaults#DEFAULT_BUTTON_SIZE} will be used.
         */
        @NonNull
        public Builder setSize(@Dimension(unit = DP) float size) {
            mSize = dp(size);
            return this;
        }

        // TODO(b/203078514): Add getting color from the current Theme (from XML).
        /**
         * Sets the colors for the {@link Button}. If set, {@link ButtonColors#getBackgroundColor()}
         * will be used for the background of the button. If not set, {@link
         * ButtonDefaults#PRIMARY_BUTTON_COLORS} will be used.
         */
        @NonNull
        public Builder setButtonColors(@NonNull ButtonColors buttonColors) {
            mButtonColors = buttonColors;
            return this;
        }

        /**
         * Sets the custom content for this Button. Any previously added content will be overridden.
         */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content) {
            resetContent();
            this.mCustomContent = content;
            this.mType = CUSTOM_CONTENT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon. Any previously added content will
         * be overridden. Provided icon will be tinted to the given content color from {@link
         * ButtonColors} and with the given size. This icon should be image with chosen alpha
         * channel and not an actual image.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setIconContent(@NonNull String resourceId, @NonNull DpProp size) {
            resetContent();
            this.mIcon = resourceId;
            this.mType = ICON;
            this.mDefaultSize = false;
            this.mIconSize = size;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon with the default size that is half
         * of the set size of the button. Any previously added content will be overridden. Provided
         * icon will be tinted to the given content color from {@link ButtonColors}. This icon
         * should be image with chosen alpha channel and not an actual image.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setIconContent(@NonNull String resourceId) {
            resetContent();
            this.mIcon = resourceId;
            this.mType = ICON;
            this.mDefaultSize = true;
            return this;
        }

        /**
         * Sets the content of this Button to be the given text with the default font for the set
         * size (for the {@link ButtonDefaults#DEFAULT_BUTTON_SIZE}, {@link
         * ButtonDefaults#LARGE_BUTTON_SIZE} and {@link ButtonDefaults#EXTRA_LARGE_BUTTON_SIZE} is
         * {@link Typography#TYPOGRAPHY_TITLE2}, {@link Typography#TYPOGRAPHY_TITLE1} and {@link
         * Typography#TYPOGRAPHY_DISPLAY3} respectively). Any previously added content will be
         * overridden. Text should contain no more than 3 characters, otherwise it will overflow
         * from the edges.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setTextContent(@NonNull String text) {
            resetContent();
            this.mText = text;
            this.mType = TEXT;
            this.mDefaultSize = true;
            return this;
        }

        /**
         * Sets the content of this Button to be the given text with the given font. If you need
         * more font related customization, consider using {@link #setContent} with {@link Text}
         * component. Any previously added content will be overridden. Text should contain no more
         * than 3 characters, otherwise it will overflow from the edges.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setTextContent(@NonNull String text, @TypographyName int typographyName) {
            resetContent();
            this.mText = text;
            this.mTypographyName = typographyName;
            this.mIsTypographyNameSet = true;
            this.mType = TEXT;
            this.mDefaultSize = false;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon with the default size that is half
         * of the size of the button. Any previously added content will be overridden. Provided icon
         * will be tinted to the given content color from {@link ButtonColors}. This icon should be
         * image with chosen alpha channel and not an actual image.
         */
        // There are multiple methods to set different type of content, but there is general getter
        // getContent that will return LayoutElement set by any of them. b/217197259
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder setImageContent(@NonNull String resourceId) {
            resetContent();
            this.mImage = resourceId;
            this.mType = IMAGE;
            this.mDefaultSize = false;
            return this;
        }

        private void resetContent() {
            this.mText = null;
            this.mIsTypographyNameSet = false;
            this.mIcon = null;
            this.mImage = null;
            this.mCustomContent = null;
            this.mIconSize = null;
        }

        /** Constructs and returns {@link Button} with the provided field and look. */
        @NonNull
        @Override
        public Button build() {
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setClickable(
                                    new Clickable.Builder()
                                            .setId(mClickableId)
                                            .setOnClick(mAction)
                                            .build())
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mButtonColors.getBackgroundColor())
                                            .setCorner(
                                                    new Corner.Builder()
                                                            .setRadius(radiusOf(mSize))
                                                            .build())
                                            .build());
            if (!mContentDescription.isEmpty()) {
                modifiers.setSemantics(
                        new ModifiersBuilders.Semantics.Builder()
                                .setContentDescription(mContentDescription)
                                .build());
            }

            Box.Builder element =
                    new Box.Builder()
                            .setHeight(mSize)
                            .setWidth(mSize)
                            .setModifiers(modifiers.build());

            if (mType != NOT_SET) {
                element.addContent(getCorrectContent());
            }
            return new Button(element.build());
        }

        @NonNull
        private LayoutElement getCorrectContent() {
            assertContentFields();

            switch (mType) {
                case ICON:
                {
                    DpProp iconSize =
                            mDefaultSize
                                    ? ButtonDefaults.recommendedIconSize(mSize)
                                    : checkNotNull(mIconSize);
                    mContent =
                            new Image.Builder()
                                    .setResourceId(checkNotNull(mIcon))
                                    .setHeight(checkNotNull(iconSize))
                                    .setWidth(iconSize)
                                    .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                                    .setColorFilter(
                                            new ColorFilter.Builder()
                                                    .setTint(mButtonColors.getContentColor())
                                                    .build());

                    return mContent.build();
                }
                case TEXT:
                {
                    @TypographyName
                    int typographyName =
                            mIsTypographyNameSet
                                    ? mTypographyName : getDefaultTypographyForSize(mSize);
                    mContent =
                            new Text.Builder()
                                    .setText(checkNotNull(mText))
                                    .setMaxLines(1)
                                    .setTypography(typographyName)
                                    .setColor(mButtonColors.getContentColor());

                    return mContent.build();
                }
                case IMAGE:
                {
                    mContent =
                            new Image.Builder()
                                    .setResourceId(checkNotNull(mImage))
                                    .setHeight(mSize)
                                    .setWidth(mSize)
                                    .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS);
                    return mContent.build();
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

        private void assertContentFields() {
            int numOfNonNull = 0;
            if (mText != null) {
                numOfNonNull++;
            }
            if (mIcon != null) {
                numOfNonNull++;
            }
            if (mImage != null) {
                numOfNonNull++;
            }
            if (mCustomContent != null) {
                numOfNonNull++;
            }
            if (numOfNonNull == 0) {
                throw new IllegalArgumentException("Content is not set.");
            }
            if (numOfNonNull > 1) {
                throw new IllegalArgumentException(
                        "Too many contents are set. Only one content should be set in the button.");
            }
        }

        private @TypographyName int getDefaultTypographyForSize(@NonNull DpProp size) {
            if (size.getValue() == LARGE_BUTTON_SIZE.getValue()) {
                return Typography.TYPOGRAPHY_TITLE1;
            } else {
                if (size.getValue() == EXTRA_LARGE_BUTTON_SIZE.getValue()) {
                    return Typography.TYPOGRAPHY_DISPLAY3;
                } else {
                    return Typography.TYPOGRAPHY_TITLE2;
                }
            }
        }
    }

    /** Returns the content of this Button. Intended for testing purposes only. */
    @NonNull
    public LayoutElement getContent() {
        return checkNotNull(mElement.getContents().get(0));
    }

    /**
     * Returns click event action associated with this Button. Intended for testing purposes only.
     */
    @NonNull
    public Action getAction() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getClickable()).getOnClick());
    }

    /** Returns content description for this Button. Intended for testing purposes only. */
    @NonNull
    public String getContentDescription() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getSemantics())
                        .getContentDescription());
    }

    /** Returns size for this Button. Intended for testing purposes only. */
    @NonNull
    public ContainerDimension getSize() {
        return checkNotNull(mElement.getWidth());
    }

    private ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns button color of this Button. Intended for testing purposes only. */
    @NonNull
    public ButtonColors getButtonColors() {
        ColorProp backgroundColor = getBackgroundColor();
        LayoutElement mainElement = checkNotNull(mElement.getContents().get(0));
        int type = getType(mainElement);
        ColorProp contentColor;
        switch (type) {
            case Builder.ICON:
                contentColor =
                        checkNotNull(
                                checkNotNull(((Image) mainElement).getColorFilter()).getTint());
                break;
            case Builder.TEXT:
                contentColor =
                        checkNotNull(
                                checkNotNull(
                                                ((LayoutElementBuilders.Text) mainElement)
                                                        .getFontStyle())
                                        .getColor());
                break;
            case Builder.IMAGE:
            case Builder.CUSTOM_CONTENT:
            case Builder.NOT_SET:
            default:
                // Default color so we can construct ButtonColors object in case where content color
                // is not set (i.e. button with an actual image doesn't use content color, or
                // content is set to custom one by the user.
                contentColor = new ColorProp.Builder().build();
                break;
        }

        return new ButtonColors(backgroundColor, contentColor);
    }

    /**
     * Returns the type of the Button. Types are defined in {@link Builder}. This is used in {@link
     * #getButtonColors} to ease if the current Button has content color.
     *
     * <p>Type is determined by the content of the outer Box layout and if that content has the
     * color because if the content is set by some of the provided setters in the Builder it will
     * have color.
     */
    private @Builder.ButtonType int getType(LayoutElement element) {
        // To elementary Text class as Material Text when it goes to proto disappears.
        if (element instanceof LayoutElementBuilders.Text) {
            FontStyle fontStyle = ((LayoutElementBuilders.Text) element).getFontStyle();
            if (fontStyle != null && fontStyle.getColor() != null) {
                return Builder.TEXT;
            }
        }
        if (element instanceof Image) {
            // This means that the Button is either Button with an image or Button with an icon.
            // Only Button with an icon will have the tint color set.
            ColorFilter colorFilter = ((Image) element).getColorFilter();
            if (colorFilter != null && colorFilter.getTint() != null) {
                return Builder.ICON;
            } else {
                return Builder.IMAGE;
            }
        }
        return Builder.CUSTOM_CONTENT;
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return checkNotNull(mElement.toLayoutElementProto());
    }
}
