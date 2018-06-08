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

package androidx.transition;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.content.res.TypedArrayUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * This class inflates scenes and transitions from resource files.
 */
public class TransitionInflater {

    private static final Class<?>[] CONSTRUCTOR_SIGNATURE =
            new Class[]{Context.class, AttributeSet.class};
    private static final ArrayMap<String, Constructor> CONSTRUCTORS = new ArrayMap<>();

    private final Context mContext;

    private TransitionInflater(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Obtains the TransitionInflater from the given context.
     */
    public static TransitionInflater from(Context context) {
        return new TransitionInflater(context);
    }

    /**
     * Loads a {@link Transition} object from a resource
     *
     * @param resource The resource id of the transition to load
     * @return The loaded Transition object
     * @throws android.content.res.Resources.NotFoundException when the
     *                                                         transition cannot be loaded
     */
    public Transition inflateTransition(int resource) {
        XmlResourceParser parser = mContext.getResources().getXml(resource);
        try {
            return createTransitionFromXml(parser, Xml.asAttributeSet(parser), null);
        } catch (XmlPullParserException e) {
            throw new InflateException(e.getMessage(), e);
        } catch (IOException e) {
            throw new InflateException(
                    parser.getPositionDescription() + ": " + e.getMessage(), e);
        } finally {
            parser.close();
        }
    }

    /**
     * Loads a {@link TransitionManager} object from a resource
     *
     * @param resource The resource id of the transition manager to load
     * @return The loaded TransitionManager object
     * @throws android.content.res.Resources.NotFoundException when the
     *                                                         transition manager cannot be loaded
     */
    public TransitionManager inflateTransitionManager(int resource, ViewGroup sceneRoot) {
        XmlResourceParser parser = mContext.getResources().getXml(resource);
        try {
            return createTransitionManagerFromXml(parser, Xml.asAttributeSet(parser), sceneRoot);
        } catch (XmlPullParserException e) {
            InflateException ex = new InflateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (IOException e) {
            InflateException ex = new InflateException(
                    parser.getPositionDescription()
                            + ": " + e.getMessage());
            ex.initCause(e);
            throw ex;
        } finally {
            parser.close();
        }
    }

    //
    // Transition loading
    //
    private Transition createTransitionFromXml(XmlPullParser parser,
            AttributeSet attrs, Transition parent)
            throws XmlPullParserException, IOException {

        Transition transition = null;

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();

        TransitionSet transitionSet = (parent instanceof TransitionSet)
                ? (TransitionSet) parent : null;

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if ("fade".equals(name)) {
                transition = new Fade(mContext, attrs);
            } else if ("changeBounds".equals(name)) {
                transition = new ChangeBounds(mContext, attrs);
            } else if ("slide".equals(name)) {
                transition = new Slide(mContext, attrs);
            } else if ("explode".equals(name)) {
                transition = new Explode(mContext, attrs);
            } else if ("changeImageTransform".equals(name)) {
                transition = new ChangeImageTransform(mContext, attrs);
            } else if ("changeTransform".equals(name)) {
                transition = new ChangeTransform(mContext, attrs);
            } else if ("changeClipBounds".equals(name)) {
                transition = new ChangeClipBounds(mContext, attrs);
            } else if ("autoTransition".equals(name)) {
                transition = new AutoTransition(mContext, attrs);
            } else if ("changeScroll".equals(name)) {
                transition = new ChangeScroll(mContext, attrs);
            } else if ("transitionSet".equals(name)) {
                transition = new TransitionSet(mContext, attrs);
            } else if ("transition".equals(name)) {
                transition = (Transition) createCustom(attrs, Transition.class, "transition");
            } else if ("targets".equals(name)) {
                getTargetIds(parser, attrs, parent);
            } else if ("arcMotion".equals(name)) {
                if (parent == null) {
                    throw new RuntimeException("Invalid use of arcMotion element");
                }
                parent.setPathMotion(new ArcMotion(mContext, attrs));
            } else if ("pathMotion".equals(name)) {
                if (parent == null) {
                    throw new RuntimeException("Invalid use of pathMotion element");
                }
                parent.setPathMotion((PathMotion) createCustom(attrs, PathMotion.class,
                        "pathMotion"));
            } else if ("patternPathMotion".equals(name)) {
                if (parent == null) {
                    throw new RuntimeException("Invalid use of patternPathMotion element");
                }
                parent.setPathMotion(new PatternPathMotion(mContext, attrs));
            } else {
                throw new RuntimeException("Unknown scene name: " + parser.getName());
            }
            if (transition != null) {
                if (!parser.isEmptyElementTag()) {
                    createTransitionFromXml(parser, attrs, transition);
                }
                if (transitionSet != null) {
                    transitionSet.addTransition(transition);
                    transition = null;
                } else if (parent != null) {
                    throw new InflateException("Could not add transition to another transition.");
                }
            }
        }

        return transition;
    }

    private Object createCustom(AttributeSet attrs, Class expectedType, String tag) {
        String className = attrs.getAttributeValue(null, "class");

        if (className == null) {
            throw new InflateException(tag + " tag must have a 'class' attribute");
        }

        try {
            synchronized (CONSTRUCTORS) {
                Constructor constructor = CONSTRUCTORS.get(className);
                if (constructor == null) {
                    @SuppressWarnings("unchecked")
                    Class<?> c = mContext.getClassLoader().loadClass(className)
                            .asSubclass(expectedType);
                    if (c != null) {
                        constructor = c.getConstructor(CONSTRUCTOR_SIGNATURE);
                        constructor.setAccessible(true);
                        CONSTRUCTORS.put(className, constructor);
                    }
                }
                //noinspection ConstantConditions
                return constructor.newInstance(mContext, attrs);
            }
        } catch (Exception e) {
            throw new InflateException("Could not instantiate " + expectedType + " class "
                    + className, e);
        }
    }

    private void getTargetIds(XmlPullParser parser,
            AttributeSet attrs, Transition transition) throws XmlPullParserException, IOException {

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("target")) {
                TypedArray a = mContext.obtainStyledAttributes(attrs, Styleable.TRANSITION_TARGET);
                int id = TypedArrayUtils.getNamedResourceId(a, parser, "targetId",
                        Styleable.TransitionTarget.TARGET_ID, 0);
                String transitionName;
                if (id != 0) {
                    transition.addTarget(id);
                } else if ((id = TypedArrayUtils.getNamedResourceId(a, parser, "excludeId",
                        Styleable.TransitionTarget.EXCLUDE_ID, 0)) != 0) {
                    transition.excludeTarget(id, true);
                } else if ((transitionName = TypedArrayUtils.getNamedString(a, parser, "targetName",
                        Styleable.TransitionTarget.TARGET_NAME)) != null) {
                    transition.addTarget(transitionName);
                } else if ((transitionName = TypedArrayUtils.getNamedString(a, parser,
                        "excludeName", Styleable.TransitionTarget.EXCLUDE_NAME)) != null) {
                    transition.excludeTarget(transitionName, true);
                } else {
                    String className = TypedArrayUtils.getNamedString(a, parser,
                            "excludeClass", Styleable.TransitionTarget.EXCLUDE_CLASS);
                    try {
                        if (className != null) {
                            Class clazz = Class.forName(className);
                            transition.excludeTarget(clazz, true);
                        } else if ((className = TypedArrayUtils.getNamedString(a, parser,
                                "targetClass", Styleable.TransitionTarget.TARGET_CLASS)) != null) {
                            Class clazz = Class.forName(className);
                            transition.addTarget(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        a.recycle();
                        throw new RuntimeException("Could not create " + className, e);
                    }
                }
                a.recycle();
            } else {
                throw new RuntimeException("Unknown scene name: " + parser.getName());
            }
        }
    }

    //
    // TransitionManager loading
    //

    private TransitionManager createTransitionManagerFromXml(XmlPullParser parser,
            AttributeSet attrs, ViewGroup sceneRoot) throws XmlPullParserException, IOException {

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();
        TransitionManager transitionManager = null;

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("transitionManager")) {
                transitionManager = new TransitionManager();
            } else if (name.equals("transition") && (transitionManager != null)) {
                loadTransition(attrs, parser, sceneRoot, transitionManager);
            } else {
                throw new RuntimeException("Unknown scene name: " + parser.getName());
            }
        }
        return transitionManager;
    }

    private void loadTransition(AttributeSet attrs, XmlPullParser parser, ViewGroup sceneRoot,
            TransitionManager transitionManager) throws Resources.NotFoundException {

        TypedArray a = mContext.obtainStyledAttributes(attrs, Styleable.TRANSITION_MANAGER);
        int transitionId = TypedArrayUtils.getNamedResourceId(a, parser, "transition",
                Styleable.TransitionManager.TRANSITION, -1);
        int fromId = TypedArrayUtils.getNamedResourceId(a, parser, "fromScene",
                Styleable.TransitionManager.FROM_SCENE, -1);
        Scene fromScene = (fromId < 0) ? null : Scene.getSceneForLayout(sceneRoot, fromId,
                mContext);
        int toId = TypedArrayUtils.getNamedResourceId(a, parser, "toScene",
                Styleable.TransitionManager.TO_SCENE, -1);
        Scene toScene = (toId < 0) ? null : Scene.getSceneForLayout(sceneRoot, toId, mContext);

        if (transitionId >= 0) {
            Transition transition = inflateTransition(transitionId);
            if (transition != null) {
                if (toScene == null) {
                    throw new RuntimeException("No toScene for transition ID " + transitionId);
                }
                if (fromScene == null) {
                    transitionManager.setTransition(toScene, transition);
                } else {
                    transitionManager.setTransition(fromScene, toScene, transition);
                }
            }
        }
        a.recycle();
    }

}
