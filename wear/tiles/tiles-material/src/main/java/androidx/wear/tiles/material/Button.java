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
import static androidx.wear.tiles.material.Helper.getTagBytes;
import static androidx.wear.tiles.material.Helper.getTagName;
import static androidx.wear.tiles.material.Helper.radiusOf;

import android.content.Context;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.DimensionBuilders.ContainerDimension;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.ColorFilter;
import androidx.wear.tiles.LayoutElementBuilders.Image;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.ModifiersBuilders.Background;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.ModifiersBuilders.Corner;
import androidx.wear.tiles.ModifiersBuilders.ElementMetadata;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Semantics;
import androidx.wear.tiles.material.Typography.TypographyName;
import androidx.wear.tiles.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiles component {@link Button} that represents clickable button with the given content.
 *
 * <p>The Button is circular in shape. The recommended sizes are defined in {@link ButtonDefaults}.
 *
 * <p>The recommended set of {@link ButtonColors} styles can be obtained from {@link
 * ButtonDefaults}., e.g. {@link ButtonDefaults#PRIMARY_BUTTON_COLORS} to get a color scheme for a
 * primary {@link Button}.
 */
public class Button implements LayoutElement {
    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with text. */
    static final String METADATA_TAG_TEXT = "TXTBTN";
    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with icon. */
    static final String METADATA_TAG_ICON = "ICNBTN";
    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with image. */
    static final String METADATA_TAG_IMAGE = "IMGBTN";
    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with custom
     * content.
     */
    static final String METADATA_TAG_CUSTOM_CONTENT = "CSTBTN";

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

        @NonNull static final Map<Integer, String> TYPE_TO_TAG = new HashMap<>();

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, ICON, TEXT, IMAGE, CUSTOM_CONTENT})
        @interface ButtonType {}

        @NonNull private final Context mContext;
        @Nullable private LayoutElement mCustomContent;
        @NonNull private final Clickable mClickable;
        @NonNull private CharSequence mContentDescription = "";
        @NonNull private DpProp mSize = DEFAULT_BUTTON_SIZE;
        @Nullable private String mText = null;
        @Nullable private Integer mTypographyName = null;
        @Nullable private String mIcon = null;
        @Nullable private DpProp mIconSize = null;
        @Nullable private String mImage = null;
        @NonNull private ButtonColors mButtonColors = PRIMARY_BUTTON_COLORS;
        @ButtonType private int mType = NOT_SET;

        static {
            TYPE_TO_TAG.put(ICON, METADATA_TAG_ICON);
            TYPE_TO_TAG.put(TEXT, METADATA_TAG_TEXT);
            TYPE_TO_TAG.put(IMAGE, METADATA_TAG_IMAGE);
            TYPE_TO_TAG.put(CUSTOM_CONTENT, METADATA_TAG_CUSTOM_CONTENT);
        }

        /**
         * Creates a builder for the {@link Button} from the given content. Custom content should be
         * later set with one of the following ({@link #setIconContent}, {@link #setTextContent},
         * {@link #setImageContent}.
         *
         * @param context The application's context.
         * @param clickable Associated {@link Clickable} for click events. When the Button is
         *     clicked it will fire the associated action.
         */
        public Builder(@NonNull Context context, @NonNull Clickable clickable) {
            mClickable = clickable;
            mContext = context;
        }

        /**
         * Sets the content description for the {@link Button}. It is highly recommended to provide
         * this for button containing icon or image.
         */
        @NonNull
        public Builder setContentDescription(@NonNull CharSequence contentDescription) {
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
        public Builder setCustomContent(@NonNull LayoutElement content) {
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
        @NonNull
        public Builder setIconContent(@NonNull String resourceId, @NonNull DpProp size) {
            resetContent();
            this.mIcon = resourceId;
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
        @NonNull
        public Builder setIconContent(@NonNull String resourceId) {
            resetContent();
            this.mIcon = resourceId;
            this.mType = ICON;
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
        @NonNull
        public Builder setTextContent(@NonNull String text) {
            resetContent();
            this.mText = text;
            this.mType = TEXT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given text with the given font. If you need
         * more font related customization, consider using {@link #setCustomContent} with {@link
         * Text} component. Any previously added content will be overridden. Text should contain
         * no more than 3 characters, otherwise it will overflow from the edges.
         */
        @NonNull
        public Builder setTextContent(@NonNull String text, @TypographyName int typographyName) {
            resetContent();
            this.mText = text;
            this.mTypographyName = typographyName;
            this.mType = TEXT;
            return this;
        }

        /**
         * Sets the content of this Button to be the given icon with the default size that is half
         * of the size of the button. Any previously added content will be overridden. Provided icon
         * will be tinted to the given content color from {@link ButtonColors}. This icon should be
         * image with chosen alpha channel and not an actual image.
         */
        @NonNull
        public Builder setImageContent(@NonNull String resourceId) {
            resetContent();
            this.mImage = resourceId;
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
        @NonNull
        @Override
        public Button build() {
            assertContentFields();

            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setClickable(mClickable)
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mButtonColors.getBackgroundColor())
                                            .setCorner(
                                                    new Corner.Builder()
                                                            .setRadius(radiusOf(mSize))
                                                            .build())
                                            .build())
                            .setMetadata(
                                    new ElementMetadata.Builder()
                                            .setTagData(
                                                    getTagBytes(
                                                            checkNotNull(TYPE_TO_TAG.get(mType))))
                                            .build());
            if (mContentDescription.length() > 0) {
                modifiers.setSemantics(
                        new ModifiersBuilders.Semantics.Builder()
                                .setContentDescription(mContentDescription.toString())
                                .build());
            }

            Box.Builder element =
                    new Box.Builder()
                            .setHeight(mSize)
                            .setWidth(mSize)
                            .setModifiers(modifiers.build());

            element.addContent(getCorrectContent());

            return new Button(element.build());
        }

        @NonNull
        private LayoutElement getCorrectContent() {
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
            if (numOfNonNull == 0 || mType == NOT_SET) {
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

    /**
     * Returns the custom content of this Button if it has been added. Otherwise, it returns null.
     */
    @Nullable
    public LayoutElement getCustomContent() {
        if (!getMetadataTag().equals(METADATA_TAG_CUSTOM_CONTENT)) {
            return null;
        }
        return getAnyContent();
    }

    /** Returns the icon content of this Button if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getIconContent() {
        Image icon = getIconContentObject();
        return icon != null ? checkNotNull(icon.getResourceId()).getValue() : null;
    }

    /**
     * Returns the image content of this Button if it has been added. Otherwise, it returns null.
     */
    @Nullable
    public String getImageContent() {
        Image image = getImageContentObject();
        return image != null ? checkNotNull(image.getResourceId()).getValue() : null;
    }

    /** Returns the text content of this Button if it has been added. Otherwise, it returns null. */
    @Nullable
    public String getTextContent() {
        Text text = getTextContentObject();
        return text != null ? text.getText() : null;
    }

    @NonNull
    private LayoutElement getAnyContent() {
        return checkNotNull(mElement.getContents().get(0));
    }

    @Nullable
    private Image getIconContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_ICON)) {
            return null;
        }
        return (Image) getAnyContent();
    }

    @Nullable
    private Text getTextContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_TEXT)) {
            return null;
        }
        return Text.fromLayoutElement(getAnyContent());
    }

    @Nullable
    private Image getImageContentObject() {
        if (!getMetadataTag().equals(METADATA_TAG_IMAGE)) {
            return null;
        }
        return (Image) getAnyContent();
    }

    /** Returns click event action associated with this Button. */
    @NonNull
    public Clickable getClickable() {
        return checkNotNull(checkNotNull(mElement.getModifiers()).getClickable());
    }

    /** Returns content description for this Button. */
    @Nullable
    public CharSequence getContentDescription() {
        Semantics semantics = checkNotNull(mElement.getModifiers()).getSemantics();
        if (semantics == null) {
            return null;
        }
        return semantics.getContentDescription();
    }

    /** Returns size for this Button. */
    @NonNull
    public ContainerDimension getSize() {
        return checkNotNull(mElement.getWidth());
    }

    private ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns button color of this Button. */
    @NonNull
    public ButtonColors getButtonColors() {
        ColorProp backgroundColor = getBackgroundColor();
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
                contentColor =
                        checkNotNull(checkNotNull(getImageContentObject()).getColorFilter())
                                .getTint();
                break;
            case METADATA_TAG_CUSTOM_CONTENT:
                break;
        }

        if (contentColor == null) {
            contentColor = new ColorProp.Builder().build();
        }

        return new ButtonColors(backgroundColor, contentColor);
    }

    /** Returns metadata tag set to this Button. */
    @NonNull
    String getMetadataTag() {
        return getMetadataTag(checkNotNull(mElement.getModifiers()));
    }

    @NonNull
    private static String getMetadataTag(Modifiers modifiers) {
        return getTagName(checkNotNull(modifiers.getMetadata()).getTagData());
    }

    private static boolean isButtonTag(@NonNull String tag) {
        return Builder.TYPE_TO_TAG.containsValue(tag);
    }

    /**
     * Returns Button object from the given LayoutElement if that element can be converted to
     * Button. Otherwise, returns null.
     */
    @Nullable
    public static Button fromLayoutElement(@NonNull LayoutElement element) {
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        Modifiers modifiers = boxElement.getModifiers();
        if (modifiers == null
                || modifiers.getMetadata() == null
                || !isButtonTag(getMetadataTag(modifiers))) {
            return null;
        }
        // Now we are sure that this element is a Button.
        return new Button(boxElement);
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return checkNotNull(mElement.toLayoutElementProto());
    }
}
