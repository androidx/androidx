// Copyright 2012 Google Inc. All Rights Reserved.

package android.support.appcompat.view;

import android.view.Menu;

/**
 * @author trevorjohns@google.com (Trevor Johns)
 */
public abstract class MenuCompat implements Menu {

    /**
     * This is the part of an order integer that the user can provide.
     *
     * @hide
     */
    protected static final int USER_MASK = 0x0000ffff;

    /**
     * This is the part of an order integer that supplies the category of the item.
     *
     * @hide
     */
    protected static final int CATEGORY_MASK = 0xffff0000;

    /**
     * Bit shift of the category portion of the order integer.
     *
     * @hide
     */
    protected static final int CATEGORY_SHIFT = 16;

}
