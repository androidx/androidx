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
package androidx.com.android.tools.idea.lang.aidl;

import androidx.com.android.tools.idea.lang.aidl.parser.AidlParser;
import androidx.com.android.tools.idea.lang.aidl.lexer.AidlLexer;
import androidx.com.android.tools.idea.lang.aidl.lexer.AidlTokenTypeSets;
import androidx.com.android.tools.idea.lang.aidl.lexer.AidlTokenTypes;
import androidx.com.android.tools.idea.lang.aidl.psi.AidlFile;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class AidlParserDefinition implements ParserDefinition {
  private static final IFileElementType AIDL_FILE_ELEMENT_TYPE = new IFileElementType(AidlFileType.INSTANCE.getLanguage());

  @Override
  @NotNull
  public Lexer createLexer(Project project) {
    return new AidlLexer();
  }

  @NotNull
  @Override
  public PsiParser createParser(Project project) {
    return new AidlParser();
  }

  @NotNull
  @Override
  public IFileElementType getFileNodeType() {
    return AIDL_FILE_ELEMENT_TYPE;
  }

  @Override
  @NotNull
  public TokenSet getWhitespaceTokens() {
    return AidlTokenTypeSets.WHITESPACES;
  }

  @Override
  @NotNull
  public TokenSet getCommentTokens() {
    return AidlTokenTypeSets.COMMENTS;
  }

  @Override
  @NotNull
  public TokenSet getStringLiteralElements() {
    return AidlTokenTypeSets.LITERALS;
  }

  @Override
  @NotNull
  public PsiElement createElement(ASTNode node) {
    return AidlTokenTypes.Factory.createElement(node);
  }

  @NotNull
  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new AidlFile(viewProvider);
  }

  @NotNull
  @Override
  public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return ParserDefinition.SpaceRequirements.MAY;
  }
}
