// Copyright 2012 Google Inc. All Rights Reserved.

package android.support.appcompat.view.menu;

import android.support.appcompat.view.Menu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.appcompat.view.MenuItem;
import android.support.appcompat.view.SubMenu;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * @author trevorjohns@google.com (Trevor Johns)
 */
public class MenuBuilder implements Menu {

  public void flagActionItems() {
  }

  public ArrayList<MenuItemImpl> getVisibleItems() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public ArrayList<MenuItemImpl> getActionItems() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public ArrayList<MenuItemImpl> getNonActionItems() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public void addMenuPresenter(MenuPresenter presenter) {
    //To change body of created methods use File | Settings | File Templates.
  }

  public MenuItemImpl getExpandedItem() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public void close(boolean b) {
  }

  public void changeMenuMode() {
    //To change body of created methods use File | Settings | File Templates.
  }

  /**
   * Called by menu items to execute their associated action
   */
  public interface ItemInvoker {
    public boolean invokeItem(MenuItemImpl item);
  }

  public MenuBuilder(Context themedContext) {
  }

  @Override
  public MenuItem add(CharSequence charSequence) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MenuItem add(int i) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MenuItem add(int i, int i1, int i2, CharSequence charSequence) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MenuItem add(int i, int i1, int i2, int i3) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public SubMenu addSubMenu(CharSequence charSequence) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public SubMenu addSubMenu(int i) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public SubMenu addSubMenu(int i, int i1, int i2, CharSequence charSequence) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public SubMenu addSubMenu(int i, int i1, int i2, int i3) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int addIntentOptions(int i, int i1, int i2, ComponentName componentName, Intent[] intents,
      Intent intent, int i3, MenuItem[] menuItems) {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void removeItem(int i) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void removeGroup(int i) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void clear() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setGroupCheckable(int i, boolean b, boolean b1) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setGroupVisible(int i, boolean b) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setGroupEnabled(int i, boolean b) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean hasVisibleItems() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MenuItem findItem(int i) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int size() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MenuItem getItem(int i) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void close() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean performShortcut(int i, KeyEvent keyEvent, int i1) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean isShortcutKey(int i, KeyEvent keyEvent) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean performIdentifierAction(int i, int i1) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void setQwertyMode(boolean b) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public MenuBuilder setDefaultShowAsAction(int showAsActionIfRoom) {
    return null;
  }

  public void setCallback(MenuPresenter.Callback actionMode) {
  }

  public void startDispatchingItemsChanged() {
  }

  public void stopDispatchingItemsChanged() {
    //To change body of created methods use File | Settings | File Templates.
  }

  public boolean isQwertyMode() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setShortcutsVisible(boolean shortcutsVisible) {
  }

  public boolean isShortcutsVisible() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setHeaderIconInt(Drawable icon) {
  }

  public void setHeaderTitleInt(CharSequence title) {
  }

  public void setHeaderViewInt(View view) {
  }

  public boolean expandItemActionView(MenuItemImpl item) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean collapseItemActionView(MenuItemImpl item) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getActionViewStatesKey() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean getOptionalIconsVisible() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Context getContext() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public void onItemsChanged(boolean b) {
  }

  public boolean performItemAction(MenuItemImpl item, int i) {
    return false;  //To change body of created methods use File | Settings | File Templates.
  }
}
