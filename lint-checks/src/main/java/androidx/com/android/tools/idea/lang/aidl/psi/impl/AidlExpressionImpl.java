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

// ATTENTION: This file has been automatically generated from Aidl.bnf. Do not edit it manually.

package androidx.com.android.tools.idea.lang.aidl.psi.impl;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import static androidx.com.android.tools.idea.lang.aidl.lexer.AidlTokenTypes.*;

import androidx.com.android.tools.idea.lang.aidl.psi.AidlExpression;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlQualifiedName;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlVisitor;

import androidx.com.android.tools.idea.lang.aidl.psi.*;

public class AidlExpressionImpl extends AidlPsiCompositeElementImpl implements AidlExpression {

  public AidlExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AidlVisitor visitor) {
    visitor.visitExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AidlVisitor) accept((AidlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public AidlExpression getExpression() {
    return findChildByClass(AidlExpression.class);
  }

  @Override
  @Nullable
  public AidlQualifiedName getQualifiedName() {
    return findChildByClass(AidlQualifiedName.class);
  }

  @Override
  @Nullable
  public PsiElement getCharvalue() {
    return findChildByType(CHARVALUE);
  }

  @Override
  @Nullable
  public PsiElement getFloatvalue() {
    return findChildByType(FLOATVALUE);
  }

  @Override
  @Nullable
  public PsiElement getHexvalue() {
    return findChildByType(HEXVALUE);
  }

  @Override
  @Nullable
  public PsiElement getIntvalue() {
    return findChildByType(INTVALUE);
  }

}
