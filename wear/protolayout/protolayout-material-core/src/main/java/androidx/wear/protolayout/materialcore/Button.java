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

package androidx.wear.protolayout.materialcore;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagName;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.radiusOf;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.Background;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.Corner;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.ModifiersBuilders.Semantics;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * ProtoLayout core component {@link Button} that represents clickable button with the given
 * content. This component is not meant to be used standalone, it's a helper component for the
 * Material library.
 *
 * <p>The Button is circular in shape. The recommended sizes and styles are defined in the public
 * Material library.
 *
 * <p>This Button doesn't have any styling applied, that should be done by the calling library.
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
    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with text. */
    public static final String METADATA_TAG_TEXT = "TXTBTN";

    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with icon. */
    public static final String METADATA_TAG_ICON = "ICNBTN";

    /** Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with image. */
    public static final String METADATA_TAG_IMAGE = "IMGBTN";

    /**
     * Tool tag for Metadata in Modifiers, so we know that Box is actually a Button with custom
     * content.
     */
    public static final String METADATA_TAG_CUSTOM_CONTENT = "CSTBTN";

    @NonNull private final Box mElement;

    Button(@NonNull Box element) {
        mElement = element;
    }

    /** Builder class for {@link Button}. */
    public static final class Builder implements LayoutElement.Builder {
        public static final int NOT_SET = -1;

        /** Button type to be used when setting a content which represents an icon. */
        public static final int ICON = 0;

        /** Button type to be used when setting a content which represents a text. */
        public static final int TEXT = 1;

        /** Button type to be used when setting a content which represents an image. */
        public static final int IMAGE = 2;

        /** Button type to be used when setting a content which is a custom one. */
        public static final int CUSTOM_CONTENT = 3;

        @NonNull static final Map<Integer, String> TYPE_TO_TAG = new HashMap<>();

        /** Button types. */
        @RestrictTo(Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({NOT_SET, ICON, TEXT, IMAGE, CUSTOM_CONTENT})
        public @interface ButtonType {}

        @NonNull private final Clickable mClickable;
        @Nullable private StringProp mContentDescription;
        @NonNull private DpProp mSize = dp(0f);
        @ButtonType private int mType = NOT_SET;
        @NonNull private ColorProp mBackgroundColor = argb(Color.BLACK);
        @Nullable private LayoutElement mContent;

        static {
            TYPE_TO_TAG.put(ICON, METADATA_TAG_ICON);
            TYPE_TO_TAG.put(TEXT, METADATA_TAG_TEXT);
            TYPE_TO_TAG.put(IMAGE, METADATA_TAG_IMAGE);
            TYPE_TO_TAG.put(CUSTOM_CONTENT, METADATA_TAG_CUSTOM_CONTENT);
        }

        /**
         * Creates a builder for the {@link Button} from the given content. Custom content should be
         * later set with ({@link #setContent} and specifying the correct content type.
         *
         * @param clickable Associated {@link Clickable} for click events. When the Button is
         *     clicked it will fire the associated action.
         */
        public Builder(@NonNull Clickable clickable) {
            mClickable = clickable;
        }

        /**
         * Sets the content description for the {@link Button}. It is highly recommended to provide
         * this for button containing icon or image.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        @NonNull
        public Builder setContentDescription(@NonNull StringProp contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        /** Sets the size for the {@link Button}. If not set, Button won't be shown. */
        @NonNull
        public Builder setSize(@NonNull DpProp size) {
            mSize = size;
            return this;
        }

        /** Sets the background colors for the {@link Button}. If not set, black is used. */
        @NonNull
        public Builder setBackgroundColor(@NonNull ColorProp backgroundColor) {
            mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the content for this Button. Any previously added content will be overridden.
         * Provided content should be styled and sized.
         */
        @NonNull
        public Builder setContent(@NonNull LayoutElement content, @ButtonType int type) {
            this.mContent = content;
            this.mType = type;
            return this;
        }

        /** Constructs and returns {@link Button} with the provided field and look. */
        @SuppressLint("CheckResult") // (b/247804720)
        @NonNull
        @Override
        public Button build() {
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setClickable(mClickable)
                            .setBackground(
                                    new Background.Builder()
                                            .setColor(mBackgroundColor)
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

            if (mContentDescription != null) {
                modifiers.setSemantics(
                        new Semantics.Builder().setContentDescription(mContentDescription).build());
            }

            Box.Builder element =
                    new Box.Builder()
                            .setHeight(mSize)
                            .setWidth(mSize)
                            .setModifiers(modifiers.build());

            if (mContent != null) {
                element.addContent(mContent);
            }

            return new Button(element.build());
        }
    }

    /** Returns the content of this Button if it has been added. Otherwise, it returns null. */
    @Nullable
    public LayoutElement getContent() {
        return mElement.getContents().get(0);
    }

    /** Returns click event action associated with this Button. */
    @NonNull
    public Clickable getClickable() {
        return checkNotNull(checkNotNull(mElement.getModifiers()).getClickable());
    }

    /** Returns content description for this Button. */
    @Nullable
    public StringProp getContentDescription() {
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

    /** Returns the background color for this Button. */
    @NonNull
    public ColorProp getBackgroundColor() {
        return checkNotNull(
                checkNotNull(checkNotNull(mElement.getModifiers()).getBackground()).getColor());
    }

    /** Returns metadata tag set to this Button. */
    @NonNull
    public String getMetadataTag() {
        return getMetadataTagName(
                checkNotNull(checkNotNull(mElement.getModifiers()).getMetadata()));
    }

    /**
     * Returns Button object from the given LayoutElement (e.g. one retrieved from a container's
     * content with {@code container.getContents().get(index)}) if that element can be converted to
     * Button. Otherwise, it will return null.
     */
    @Nullable
    public static Button fromLayoutElement(@NonNull LayoutElement element) {
        if (element instanceof Button) {
            return (Button) element;
        }
        if (!(element instanceof Box)) {
            return null;
        }
        Box boxElement = (Box) element;
        if (!checkTag(boxElement.getModifiers(), Builder.TYPE_TO_TAG.values())) {
            return null;
        }
        // Now we are sure that this element is a Button.
        return new Button(boxElement);
    }

    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return checkNotNull(mElement.toLayoutElementProto());
    }

    @Nullable
    @Override
    public Fingerprint getFingerprint() {
        return mElement.getFingerprint();
    }
}
