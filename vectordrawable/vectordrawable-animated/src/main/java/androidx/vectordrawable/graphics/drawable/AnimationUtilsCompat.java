/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Defines common utilities for working with animations.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class AnimationUtilsCompat {
    /**
     * Loads an {@link Interpolator} object from a resource
     *
     * @param context Application context used to access resources
     * @param id      The resource id of the animation to load
     * @return The animation object reference by the specified id
     */
    @SuppressWarnings("UnnecessaryInitCause") // requires API 24+
    @NonNull
    public static Interpolator loadInterpolator(@NonNull Context context, @AnimRes int id)
            throws NotFoundException {
        // From API 21, we added path Interpolator .
        if (Build.VERSION.SDK_INT >= 21) {
            Interpolator interp = AnimationUtils.loadInterpolator(context, id);
            ObjectsCompat.requireNonNull(interp, "Failed to parse interpolator, no start tag "
                    + "found");
            return interp;
        }

        XmlResourceParser parser = null;
        try {
            // Special treatment for the interpolator introduced at API 21.
            if (id == AndroidResources.FAST_OUT_LINEAR_IN) {
                return new FastOutLinearInInterpolator();
            } else if (id == AndroidResources.FAST_OUT_SLOW_IN) {
                return new FastOutSlowInInterpolator();
            } else if (id == AndroidResources.LINEAR_OUT_SLOW_IN) {
                return new LinearOutSlowInInterpolator();
            }
            parser = context.getResources().getAnimation(id);
            return createInterpolatorFromXml(context, parser);
        } catch (XmlPullParserException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } finally {
            if (parser != null) parser.close();
        }

    }

    @NonNull
    private static Interpolator createInterpolatorFromXml(@NonNull Context context,
            @NonNull XmlPullParser parser) throws XmlPullParserException, IOException {

        Interpolator interpolator = null;

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            AttributeSet attrs = Xml.asAttributeSet(parser);

            String name = parser.getName();

            switch (name) {
                case "linearInterpolator":
                    interpolator = new LinearInterpolator();
                    break;
                case "accelerateInterpolator":
                    interpolator = new AccelerateInterpolator(context, attrs);
                    break;
                case "decelerateInterpolator":
                    interpolator = new DecelerateInterpolator(context, attrs);
                    break;
                case "accelerateDecelerateInterpolator":
                    interpolator = new AccelerateDecelerateInterpolator();
                    break;
                case "cycleInterpolator":
                    interpolator = new CycleInterpolator(context, attrs);
                    break;
                case "anticipateInterpolator":
                    interpolator = new AnticipateInterpolator(context, attrs);
                    break;
                case "overshootInterpolator":
                    interpolator = new OvershootInterpolator(context, attrs);
                    break;
                case "anticipateOvershootInterpolator":
                    interpolator = new AnticipateOvershootInterpolator(context, attrs);
                    break;
                case "bounceInterpolator":
                    interpolator = new BounceInterpolator();
                    break;
                case "pathInterpolator":
                    interpolator = new PathInterpolatorCompat(context, attrs, parser);
                    break;
                default:
                    throw new RuntimeException("Unknown interpolator name: " + parser.getName());
            }
        }

        if (interpolator == null) {
            throw new RuntimeException("Failed to parse interpolator, no start tag found");
        }

        return interpolator;
    }

    private AnimationUtilsCompat() {
    }
}
