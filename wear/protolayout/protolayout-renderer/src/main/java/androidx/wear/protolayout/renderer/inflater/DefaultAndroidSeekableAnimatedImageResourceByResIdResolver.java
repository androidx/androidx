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

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.renderer.common.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidSeekableAnimatedImageResourceByResIdResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/** Resource resolver for seekable Android animated resources. */
public class DefaultAndroidSeekableAnimatedImageResourceByResIdResolver
        implements AndroidSeekableAnimatedImageResourceByResIdResolver {

    @NonNull private final Resources mAndroidResources;

    /**
     * Constructor.
     *
     * @param androidResources An Android Resources instance for the tile service's package. This is
     *     normally obtained from {@code PackageManager#getResourcesForApplication}.
     */
    public DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
            @NonNull Resources androidResources) {
        this.mAndroidResources = androidResources;
    }

    @NonNull
    @Override
    public Drawable getDrawableOrThrow(
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
                return SeekableAnimatedVectorDrawable.createFromXmlInner(
                        mAndroidResources, parser, attrs, null);
            } catch (XmlPullParserException | IOException e) {
                Log.e("SeekableAVD", "Error building pipeline", e);
            }
        }

        throw new ResourceAccessException("Unsupported animated image format");
    }
}
