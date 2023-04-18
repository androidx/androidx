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

package androidx.com.android.tools.idea.lang.aidl.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class AidlVisitor extends PsiElementVisitor {

  public void visitAnnotationElement(@NotNull AidlAnnotationElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitBody(@NotNull AidlBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitConstantDeclaration(@NotNull AidlConstantDeclaration o) {
    visitDeclaration(o);
  }

  public void visitDottedName(@NotNull AidlDottedName o) {
    visitNamedElement(o);
  }

  public void visitEnumDeclaration(@NotNull AidlEnumDeclaration o) {
    visitDeclaration(o);
  }

  public void visitEnumeratorDeclaration(@NotNull AidlEnumeratorDeclaration o) {
    visitDeclaration(o);
  }

  public void visitExpression(@NotNull AidlExpression o) {
    visitPsiCompositeElement(o);
  }

  public void visitImport(@NotNull AidlImport o) {
    visitPsiCompositeElement(o);
  }

  public void visitInterfaceDeclaration(@NotNull AidlInterfaceDeclaration o) {
    visitDeclaration(o);
  }

  public void visitMethodDeclaration(@NotNull AidlMethodDeclaration o) {
    visitDeclaration(o);
  }

  public void visitNameComponent(@NotNull AidlNameComponent o) {
    visitNamedElement(o);
  }

  public void visitPackage(@NotNull AidlPackage o) {
    visitPsiCompositeElement(o);
  }

  public void visitParameter(@NotNull AidlParameter o) {
    visitPsiCompositeElement(o);
  }

  public void visitParcelableDeclaration(@NotNull AidlParcelableDeclaration o) {
    visitDeclaration(o);
  }

  public void visitQualifiedName(@NotNull AidlQualifiedName o) {
    visitNamedElement(o);
  }

  public void visitTypeElement(@NotNull AidlTypeElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitUnionDeclaration(@NotNull AidlUnionDeclaration o) {
    visitDeclaration(o);
  }

  public void visitVariableDeclaration(@NotNull AidlVariableDeclaration o) {
    visitDeclaration(o);
  }

  public void visitDeclaration(@NotNull AidlDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitNamedElement(@NotNull AidlNamedElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitPsiCompositeElement(@NotNull AidlPsiCompositeElement o) {
    visitElement(o);
  }

}
