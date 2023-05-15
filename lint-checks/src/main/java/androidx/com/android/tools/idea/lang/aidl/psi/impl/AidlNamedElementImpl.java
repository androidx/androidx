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
package androidx.com.android.tools.idea.lang.aidl.psi.impl;

import androidx.com.android.tools.idea.lang.aidl.psi.AidlNamedElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AidlNamedElementImpl extends AidlPsiCompositeElementImpl implements AidlNamedElement {
  public AidlNamedElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  @Override
  public String getName() {
    return getLeaf().getText();
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    getLeaf().replaceWithText(name);
    return this;
  }

  @NotNull
  private LeafPsiElement getLeaf() {
    PsiElement leaf = this;
    while (true) {
      PsiElement last = leaf.getLastChild();
      if (last == null) {
        break;
      }
      leaf = last;
    }
    //noinspection ConstantConditions
    return (LeafPsiElement)leaf;
  }

  @Nullable
  @Override
  public ItemPresentation getPresentation() {
    final PsiElement parent = getParent();
    if (parent instanceof NavigationItem) {
      return ((NavigationItem)parent).getPresentation();
    }
    return null;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(\"" + this.getText() + "\")";
  }

  // TODO provide a better icon for
  //@Override
  //public Icon getIcon(int flags) {
  //  final ItemPresentation presentation = getPresentation();
  //  return presentation == null ? super.getIcon(flags) : presentation.getIcon(true);
  //}
}
