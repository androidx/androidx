/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * The {@link PreferenceInflater} is used to inflate preference hierarchies from
 * XML files.
 */
class PreferenceInflater {
    private static final String TAG = "PreferenceInflater";

    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = new Class[] {
            Context.class, AttributeSet.class};

    private static final HashMap<String, Constructor> CONSTRUCTOR_MAP = new HashMap<>();

    private final Context mContext;

    private final Object[] mConstructorArgs = new Object[2];

    private PreferenceManager mPreferenceManager;

    private String[] mDefaultPackages;

    private static final String INTENT_TAG_NAME = "intent";
    private static final String EXTRA_TAG_NAME = "extra";

    public PreferenceInflater(Context context, PreferenceManager preferenceManager) {
        mContext = context;
        init(preferenceManager);
    }

    private void init(PreferenceManager preferenceManager) {
        mPreferenceManager = preferenceManager;
        setDefaultPackages(new String[] {"android.support.v14.preference.",
                "android.support.v7.preference."});
    }

    /**
     * Sets the default package that will be searched for classes to construct
     * for tag names that have no explicit package.
     *
     * @param defaultPackage The default package. This will be prepended to the
     *            tag name, so it should end with a period.
     */
    public void setDefaultPackages(String[] defaultPackage) {
        mDefaultPackages = defaultPackage;
    }

    /**
     * Returns the default package, or null if it is not set.
     *
     * @see #setDefaultPackages(String[])
     * @return The default package.
     */
    public String[] getDefaultPackages() {
        return mDefaultPackages;
    }

    /**
     * Return the context we are running in, for access to resources, class
     * loader, etc.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Inflate a new item hierarchy from the specified xml resource. Throws
     * InflaterException if there is an error.
     *
     * @param resource ID for an XML resource to load (e.g.,
     *        <code>R.layout.main_page</code>)
     * @param root Optional parent of the generated hierarchy.
     * @return The root of the inflated hierarchy. If root was supplied,
     *         this is the root item; otherwise it is the root of the inflated
     *         XML file.
     */
    public Preference inflate(int resource, @Nullable PreferenceGroup root) {
        XmlResourceParser parser = getContext().getResources().getXml(resource);
        try {
            return inflate(parser, root);
        } finally {
            parser.close();
        }
    }

    /**
     * Inflate a new hierarchy from the specified XML node. Throws
     * InflaterException if there is an error.
     * <p>
     * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
     * reasons, inflation relies heavily on pre-processing of XML files
     * that is done at build time. Therefore, it is not currently possible to
     * use inflater with an XmlPullParser over a plain XML file at runtime.
     *
     * @param parser XML dom node containing the description of the
     *        hierarchy.
     * @param root Optional to be the parent of the generated hierarchy (if
     *        <em>attachToRoot</em> is true), or else simply an object that
     *        provides a set of values for root of the returned
     *        hierarchy (if <em>attachToRoot</em> is false.)
     * @return The root of the inflated hierarchy. If root was supplied,
     *         this is root; otherwise it is the root of
     *         the inflated XML file.
     */
    public Preference inflate(XmlPullParser parser, @Nullable PreferenceGroup root) {
        synchronized (mConstructorArgs) {
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            mConstructorArgs[0] = mContext;
            final Preference result;

            try {
                // Look for the root node.
                int type;
                do {
                    type = parser.next();
                } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);

                if (type != XmlPullParser.START_TAG) {
                    throw new InflateException(parser.getPositionDescription()
                            + ": No start tag found!");
                }

                // Temp is the root that was found in the xml
                Preference xmlRoot = createItemFromTag(parser.getName(),
                        attrs);

                result = onMergeRoots(root, (PreferenceGroup) xmlRoot);

                // Inflate all children under temp
                rInflate(parser, result, attrs);

            } catch (InflateException e) {
                throw e;
            } catch (XmlPullParserException e) {
                final InflateException ex = new InflateException(e.getMessage());
                ex.initCause(e);
                throw ex;
            } catch (IOException e) {
                final InflateException ex = new InflateException(
                        parser.getPositionDescription()
                                + ": " + e.getMessage());
                ex.initCause(e);
                throw ex;
            }

            return result;
        }
    }

    private @NonNull PreferenceGroup onMergeRoots(PreferenceGroup givenRoot,
            @NonNull PreferenceGroup xmlRoot) {
        // If we were given a Preferences, use it as the root (ignoring the root
        // Preferences from the XML file).
        if (givenRoot == null) {
            xmlRoot.onAttachedToHierarchy(mPreferenceManager);
            return xmlRoot;
        } else {
            return givenRoot;
        }
    }

    /**
     * Low-level function for instantiating by name. This attempts to
     * instantiate class of the given <var>name</var> found in this
     * inflater's ClassLoader.
     *
     * <p>
     * There are two things that can happen in an error case: either the
     * exception describing the error will be thrown, or a null will be
     * returned. You must deal with both possibilities -- the former will happen
     * the first time createItem() is called for a class of a particular name,
     * the latter every time there-after for that class name.
     *
     * @param name The full name of the class to be instantiated.
     * @param attrs The XML attributes supplied for this instance.
     *
     * @return The newly instantiated item, or null.
     */
    private Preference createItem(@NonNull String name, @Nullable String[] prefixes,
            AttributeSet attrs)
            throws ClassNotFoundException, InflateException {
        Constructor constructor = CONSTRUCTOR_MAP.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real,
                // and try to add it
                final ClassLoader classLoader = mContext.getClassLoader();
                Class<?> clazz = null;
                if (prefixes == null || prefixes.length == 0) {
                    clazz = classLoader.loadClass(name);
                } else {
                    ClassNotFoundException notFoundException = null;
                    for (final String prefix : prefixes) {
                        try {
                            clazz = classLoader.loadClass(prefix + name);
                            break;
                        } catch (final ClassNotFoundException e) {
                            notFoundException = e;
                        }
                    }
                    if (clazz == null) {
                        if (notFoundException == null) {
                            throw new InflateException(attrs
                                    .getPositionDescription()
                                    + ": Error inflating class " + name);
                        } else {
                            throw notFoundException;
                        }
                    }
                }
                constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);
                CONSTRUCTOR_MAP.put(name, constructor);
            }

            Object[] args = mConstructorArgs;
            args[1] = attrs;
            return (Preference) constructor.newInstance(args);

        } catch (ClassNotFoundException e) {
            // If loadClass fails, we should propagate the exception.
            throw e;
        } catch (Exception e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription() + ": Error inflating class " + name);
            ie.initCause(e);
            throw ie;
        }
    }

    /**
     * This routine is responsible for creating the correct subclass of item
     * given the xml element name. Override it to handle custom item objects. If
     * you override this in your subclass be sure to call through to
     * super.onCreateItem(name) for names you do not recognize.
     *
     * @param name The fully qualified class name of the item to be create.
     * @param attrs An AttributeSet of attributes to apply to the item.
     * @return The item created.
     */
    protected Preference onCreateItem(String name, AttributeSet attrs)
            throws ClassNotFoundException {
        return createItem(name, mDefaultPackages, attrs);
    }

    private Preference createItemFromTag(String name,
            AttributeSet attrs) {
        try {
            final Preference item;

            if (-1 == name.indexOf('.')) {
                item = onCreateItem(name, attrs);
            } else {
                item = createItem(name, null, attrs);
            }

            return item;

        } catch (InflateException e) {
            throw e;

        } catch (ClassNotFoundException e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription()
                    + ": Error inflating class (not found)" + name);
            ie.initCause(e);
            throw ie;

        } catch (Exception e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription()
                    + ": Error inflating class " + name);
            ie.initCause(e);
            throw ie;
        }
    }

    /**
     * Recursive method used to descend down the xml hierarchy and instantiate
     * items, instantiate their children, and then call onFinishInflate().
     */
    private void rInflate(XmlPullParser parser, Preference parent, final AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();

        int type;
        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            final String name = parser.getName();

            if (INTENT_TAG_NAME.equals(name)) {
                final Intent intent;

                try {
                    intent = Intent.parseIntent(getContext().getResources(), parser, attrs);
                } catch (IOException e) {
                    XmlPullParserException ex = new XmlPullParserException(
                            "Error parsing preference");
                    ex.initCause(e);
                    throw ex;
                }

                parent.setIntent(intent);
            } else if (EXTRA_TAG_NAME.equals(name)) {
                getContext().getResources().parseBundleExtra(EXTRA_TAG_NAME, attrs,
                        parent.getExtras());
                try {
                    skipCurrentTag(parser);
                } catch (IOException e) {
                    XmlPullParserException ex = new XmlPullParserException(
                            "Error parsing preference");
                    ex.initCause(e);
                    throw ex;
                }
            } else {
                final Preference item = createItemFromTag(name, attrs);
                ((PreferenceGroup) parent).addItemFromInflater(item);
                rInflate(parser, item, attrs);
            }
        }

    }

    private static void skipCurrentTag(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        do {
            type = parser.next();
        } while (type != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth));
    }

}
