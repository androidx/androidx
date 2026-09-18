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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.ConstrainedImageDecoder.ANDROID_NS;
import static androidx.wear.protolayout.renderer.inflater.ConstrainedImageDecoder.ATTR_DRAWABLE;
import static androidx.wear.protolayout.renderer.inflater.ConstrainedImageDecoder.TAG_ANIMATED_VECTOR;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidSeekableAnimatedImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;

import org.jspecify.annotations.NonNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Objects;

/** Resource resolver for seekable Android animated resources. */
public class DefaultAndroidSeekableAnimatedImageResourceByResIdResolver
        implements AndroidSeekableAnimatedImageResourceByResIdResolver {

    private final @NonNull Resources mAndroidResources;
    private final boolean mRestrictImageSize;

    /**
     * Constructor.
     *
     * @param androidResources An Android Resources instance for the tile service's package. This is
     *     normally obtained from {@code PackageManager#getResourcesForApplication}.
     */
    public DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
            @NonNull Resources androidResources) {
        this(androidResources, /* restrictImageSize= */ false);
    }

    /**
     * Constructor with param to restrict image size.
     *
     * @param androidResources An Android Resources instance for the tile service's package. This is
     *     normally obtained from {@code PackageManager#getResourcesForApplication}.
     * @param restrictImageSize Whether to restrict the size of decoded images.
     */
    public DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
            @NonNull Resources androidResources, boolean restrictImageSize) {
        this.mAndroidResources = androidResources;
        this.mRestrictImageSize = restrictImageSize;
    }

    @Override
    public @NonNull Drawable getDrawableOrThrow(
            @NonNull AndroidSeekableAnimatedImageResourceByResId resource)
            throws ResourceAccessException {
        if (resource.getAnimatedImageFormat() == AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD) {
            try {
                final XmlPullParser parser = mAndroidResources.getXml(resource.getResourceId());
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                int type;
                do {
                    type = parser.next();
                } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);
                if (type != XmlPullParser.START_TAG) {
                    throw new XmlPullParserException("No start tag found");
                }
                if (!Objects.equals(parser.getName(), TAG_ANIMATED_VECTOR)) {
                    throw new IllegalArgumentException(
                            "Only <animated-vector> drawables are supported to prevent unbounded"
                                    + " memory allocation.");
                }
                if (mRestrictImageSize) {
                    int drawableResId =
                            attrs.getAttributeResourceValue(ANDROID_NS, ATTR_DRAWABLE, 0);
                    if (drawableResId != 0) {
                        ConstrainedImageDecoder.verifyVectorDrawableXml(
                                mAndroidResources, drawableResId);
                    }
                }
                return SeekableAnimatedVectorDrawable.createFromXmlInner(
                        mAndroidResources, parser, attrs, null);
            } catch (XmlPullParserException | IOException e) {
                Log.e("SeekableAVD", "Error building pipeline", e);
            }
        }

        throw new ResourceAccessException("Unsupported animated image format");
    }
}
