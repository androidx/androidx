/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Feb 4, 2004
 *
 */
package androidx.core.i18n.messageformat_icu.impl;

import androidx.annotation.RestrictTo;

import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.MissingResourceException;

/**
 * Provides access to ICU data files as InputStreams.  Implements security checking.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ICUData {
    /*
     * Return a URL to the ICU resource names resourceName.  The
     * resource name should either be an absolute path, or a path relative to
     * com.ibm.icu.impl (e.g., most likely it is 'data/foo').  If required
     * is true, throw an MissingResourceException instead of returning a null result.
     */
    public static boolean exists(final String resourceName) {
        URL i = null;
        if (System.getSecurityManager() != null) {
            i = AccessController.doPrivileged(new PrivilegedAction<URL>() {
                    @Override
                    public URL run() {
                        return ICUData.class.getResource(resourceName);
                    }
                });
        } else {
            i = ICUData.class.getResource(resourceName);
        }
        return i != null;
    }
        
    private static InputStream getStream(final Class<?> root, final String resourceName, boolean required) {
        InputStream i = null;
        
        if (System.getSecurityManager() != null) {
            i = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                    @Override
                    public InputStream run() {
                        return root.getResourceAsStream(resourceName);
                    }
                });
        } else {
            i = root.getResourceAsStream(resourceName);
        }

        if (i == null && required) {
            throw new MissingResourceException("could not locate data " +resourceName, root.getPackage().getName(), resourceName);
        }
        return i;
    }

    private static InputStream getStream(final ClassLoader loader, final String resourceName, boolean required) {
        InputStream i = null;
        if (System.getSecurityManager() != null) {
            i = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                    @Override
                    public InputStream run() {
                        return loader.getResourceAsStream(resourceName);
                    }
                });
        } else {
            i = loader.getResourceAsStream(resourceName);
        }
        if (i == null && required) {
            throw new MissingResourceException("could not locate data", loader.toString(), resourceName);
        }
        return i;
    }
    
    public static InputStream getStream(ClassLoader loader, String resourceName){
        return getStream(loader,resourceName, false);   
    }

    public static InputStream getRequiredStream(ClassLoader loader, String resourceName){
        return getStream(loader, resourceName, true);
    }

    /*
     * Convenience override that calls getStream(ICUData.class, resourceName, false);
     */
    public static InputStream getStream(String resourceName) {
        return getStream(ICUData.class, resourceName, false);
    }
        
    /*
     * Convenience method that calls getStream(ICUData.class, resourceName, true).
     */
    public static InputStream getRequiredStream(String resourceName) {
        return getStream(ICUData.class, resourceName, true);
    }

    /*
     * Convenience override that calls getStream(root, resourceName, false);
     */
    public static InputStream getStream(Class<?> root, String resourceName) {
        return getStream(root, resourceName, false);
    }
    
    /*
     * Convenience method that calls getStream(root, resourceName, true).
     */
    public static InputStream getRequiredStream(Class<?> root, String resourceName) {
        return getStream(root, resourceName, true);
    }
}
