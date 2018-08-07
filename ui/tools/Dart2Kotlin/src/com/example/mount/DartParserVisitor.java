// Generated from DartParser.g4 by ANTLR 4.7.1
package com.example.mount;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DartParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DartParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DartParser#libraryDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryDefinition(DartParser.LibraryDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#scriptTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScriptTag(DartParser.ScriptTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#libraryName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryName(DartParser.LibraryNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#topLevelDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTopLevelDefinition(DartParser.TopLevelDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionSignatureDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionSignatureDefinition(DartParser.FunctionSignatureDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#getterSignatureDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetterSignatureDefinition(DartParser.GetterSignatureDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#setterSignatureDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetterSignatureDefinition(DartParser.SetterSignatureDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(DartParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#getterDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetterDefinition(DartParser.GetterDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#setterDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetterDefinition(DartParser.SetterDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#staticFinalDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticFinalDeclarations(DartParser.StaticFinalDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#getOrSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetOrSet(DartParser.GetOrSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#importOrExport}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportOrExport(DartParser.ImportOrExportContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#libraryImport}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryImport(DartParser.LibraryImportContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#importSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportSpecification(DartParser.ImportSpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#combinator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombinator(DartParser.CombinatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(DartParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#libraryExport}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryExport(DartParser.LibraryExportContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#partDirective}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartDirective(DartParser.PartDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#partHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartHeader(DartParser.PartHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#partDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartDeclaration(DartParser.PartDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#uri}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUri(DartParser.UriContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(DartParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#declaredIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaredIdentifier(DartParser.DeclaredIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#finalConstVarOrType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinalConstVarOrType(DartParser.FinalConstVarOrTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#varOrType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarOrType(DartParser.VarOrTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#initializedVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializedVariableDeclaration(DartParser.InitializedVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#initializedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializedIdentifier(DartParser.InitializedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#initializedIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializedIdentifierList(DartParser.InitializedIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionSignature(DartParser.FunctionSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#returnType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnType(DartParser.ReturnTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(DartParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(DartParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(DartParser.FormalParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#normalFormalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNormalFormalParameters(DartParser.NormalFormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#optionalFormalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalFormalParameters(DartParser.OptionalFormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#optionalPositionalFormalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalPositionalFormalParameters(DartParser.OptionalPositionalFormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#namedFormalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedFormalParameters(DartParser.NamedFormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#normalFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNormalFormalParameter(DartParser.NormalFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#simpleFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleFormalParameter(DartParser.SimpleFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#fieldFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldFormalParameter(DartParser.FieldFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#defaultFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultFormalParameter(DartParser.DefaultFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#defaultNamedParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultNamedParameter(DartParser.DefaultNamedParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#classDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDefinition(DartParser.ClassDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#mixins}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMixins(DartParser.MixinsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#classMemberDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemberDefinition(DartParser.ClassMemberDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#methodSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodSignature(DartParser.MethodSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(DartParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#staticFinalDeclarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticFinalDeclarationList(DartParser.StaticFinalDeclarationListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#staticFinalDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticFinalDeclaration(DartParser.StaticFinalDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#operatorSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperatorSignature(DartParser.OperatorSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator(DartParser.OperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#binaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOperator(DartParser.BinaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#getterSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetterSignature(DartParser.GetterSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#setterSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetterSignature(DartParser.SetterSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#constructorSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorSignature(DartParser.ConstructorSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#redirection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedirection(DartParser.RedirectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#initializers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializers(DartParser.InitializersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#superCallOrFieldInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperCallOrFieldInitializer(DartParser.SuperCallOrFieldInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#assertInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssertInitializer(DartParser.AssertInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#fieldInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldInitializer(DartParser.FieldInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#factoryConstructorSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFactoryConstructorSignature(DartParser.FactoryConstructorSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#redirectingFactoryConstructorSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedirectingFactoryConstructorSignature(DartParser.RedirectingFactoryConstructorSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#constantConstructorSignature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantConstructorSignature(DartParser.ConstantConstructorSignatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#superclass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperclass(DartParser.SuperclassContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#interfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaces(DartParser.InterfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#mixinApplicationClass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMixinApplicationClass(DartParser.MixinApplicationClassContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#mixinApplication}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMixinApplication(DartParser.MixinApplicationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#enumType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumType(DartParser.EnumTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(DartParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(DartParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#metadata}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMetadata(DartParser.MetadataContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(DartParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#expressionWithoutCascade}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionWithoutCascade(DartParser.ExpressionWithoutCascadeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(DartParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(DartParser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(DartParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#nullLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(DartParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#numericLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(DartParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#booleanLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(DartParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(DartParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#singleLineString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleLineString(DartParser.SingleLineStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#multilineString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultilineString(DartParser.MultilineStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#symbolLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolLiteral(DartParser.SymbolLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#listLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListLiteral(DartParser.ListLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#mapLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapLiteral(DartParser.MapLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#mapLiteralEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapLiteralEntry(DartParser.MapLiteralEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#throwExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowExpression(DartParser.ThrowExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#throwExpressionWithoutCascade}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowExpressionWithoutCascade(DartParser.ThrowExpressionWithoutCascadeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpression(DartParser.FunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionExpressionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpressionBody(DartParser.FunctionExpressionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#thisExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisExpression(DartParser.ThisExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#newExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewExpression(DartParser.NewExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#constObjectExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstObjectExpression(DartParser.ConstObjectExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(DartParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(DartParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#namedArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedArgument(DartParser.NamedArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#cascadeSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCascadeSection(DartParser.CascadeSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#cascadeSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCascadeSelector(DartParser.CascadeSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(DartParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#compoundAssignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundAssignmentOperator(DartParser.CompoundAssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#conditionalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalExpression(DartParser.ConditionalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#ifNullExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfNullExpression(DartParser.IfNullExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(DartParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(DartParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(DartParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#equalityOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityOperator(DartParser.EqualityOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(DartParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#relationalOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalOperator(DartParser.RelationalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#bitwiseOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseOrExpression(DartParser.BitwiseOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#bitwiseXorExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseXorExpression(DartParser.BitwiseXorExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#bitwiseAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseAndExpression(DartParser.BitwiseAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#bitwiseOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseOperator(DartParser.BitwiseOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#shiftExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftExpression(DartParser.ShiftExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#shiftOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftOperator(DartParser.ShiftOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(DartParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#additiveOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveOperator(DartParser.AdditiveOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(DartParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#multiplicativeOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeOperator(DartParser.MultiplicativeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(DartParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#otherUnaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOtherUnaryExpression(DartParser.OtherUnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#prefixOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixOperator(DartParser.PrefixOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#minusOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusOperator(DartParser.MinusOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#negationOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegationOperator(DartParser.NegationOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#tildeOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTildeOperator(DartParser.TildeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#awaitExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAwaitExpression(DartParser.AwaitExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(DartParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#postfixOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixOperator(DartParser.PostfixOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#selector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelector(DartParser.SelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#incrementOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncrementOperator(DartParser.IncrementOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#assignableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableExpression(DartParser.AssignableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#unconditionalAssignableSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnconditionalAssignableSelector(DartParser.UnconditionalAssignableSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#assignableSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableSelector(DartParser.AssignableSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(DartParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#qualified}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified(DartParser.QualifiedContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeTest(DartParser.TypeTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#isOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsOperator(DartParser.IsOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeCast}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeCast(DartParser.TypeCastContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#asOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsOperator(DartParser.AsOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(DartParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(DartParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#nonLabelledStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonLabelledStatement(DartParser.NonLabelledStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(DartParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(DartParser.LocalVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#localFunctionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalFunctionDeclaration(DartParser.LocalFunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(DartParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(DartParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#forLoopParts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopParts(DartParser.ForLoopPartsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#forInitializerStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInitializerStatement(DartParser.ForInitializerStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(DartParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#doStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoStatement(DartParser.DoStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#switchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(DartParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#switchCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCase(DartParser.SwitchCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#defaultCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultCase(DartParser.DefaultCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#rethrowStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRethrowStatement(DartParser.RethrowStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#tryStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryStatement(DartParser.TryStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#onPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnPart(DartParser.OnPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#catchPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchPart(DartParser.CatchPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#finallyPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyPart(DartParser.FinallyPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(DartParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel(DartParser.LabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(DartParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(DartParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#yieldStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYieldStatement(DartParser.YieldStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#yieldEachStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYieldEachStatement(DartParser.YieldEachStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#assertStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssertStatement(DartParser.AssertStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(DartParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(DartParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(DartParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(DartParser.TypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAlias(DartParser.TypeAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#typeAliasBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAliasBody(DartParser.TypeAliasBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionTypeAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeAlias(DartParser.FunctionTypeAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link DartParser#functionPrefix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionPrefix(DartParser.FunctionPrefixContext ctx);
}