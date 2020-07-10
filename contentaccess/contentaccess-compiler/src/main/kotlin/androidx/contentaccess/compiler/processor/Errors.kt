/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.compiler.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic

fun missingEntityPrimaryKeyErrorMessage(entityName: String): String {
    return "Content entity $entityName doesn't have a primary key, a content entity must have one" +
            " field annotated with @ContentPrimaryKey."
}

fun missingFieldsInContentEntityErrorMessage(entityName: String): String {
    return "Content entity $entityName has no fields, a content entity must have at least one " +
            "field and exactly one primary key."
}

fun missingAnnotationOnEntityFieldErrorMessage(fieldName: String, entityName: String): String {
    return "Field $fieldName in $entityName is neither annotated with @ContentPrimaryKey nor " +
            "with @ContentColumn, all fields in a content entity must be be annotated by one of " +
            "the two"
}

fun entityFieldWithBothAnnotations(columnName: String, entityName: String): String {
    return "Field $columnName in $entityName is annotated with both @ContentPrimaryKey and " +
            "@ContentColumn, these annotations are mutually exclusive and  a field " +
            "can only be annotated by one of the two."
}

fun entityWithMultiplePrimaryKeys(entityName: String): String {
    return "Content entity $entityName has two or more " +
            "primary keys, a content entity must have exactly one field annotated with " +
            "@ContentPrimaryKey."
}

fun unsupportedColumnType(columnName: String, entityName: String, type: String): String {
    return "Field $columnName in $entityName is of type " +
            "$type which is not a supported column type."
}

fun nonInstantiableEntity(entityName: String): String {
    return "Entity $entityName is not instantiable. It has no non private non ignored " +
            "constructors, it must have one and only one such constructor, whether parametrized " +
            "or not."
}

fun entityWithMultipleConstructors(entityName: String): String {
    return "Entity $entityName has more than one non private non ignored constructor. Entities " +
            "should have only one non private non ignored constructor."
}

fun entityWithNullablePrimitiveType(fieldName: String, entityName: String): String {
    return "Field $fieldName of entity $entityName is of a primitive type but is marked as " +
            "nullable. Please use the boxed type instead of the primitive of type for nullable " +
            "fields"
}

fun missingEntityOnMethod(methodName: String): String {
    return "Method $methodName has no associated entity, " +
            "please ensure that either the content access object containing the method " +
            "specifies an entity inside the @ContentAccessObject annotation or that the " +
            "method specifies a content entity through the contentEntity parameter of " +
            "the annotation."
}

fun missingUriOnMethod(): String {
    return "Failed to determine URI for query, the " +
            "URI is neither specified in the associated ContentEntity, nor in the annotation " +
            "parameters."
}

fun badlyFormulatedOrderBy(orderByMember: String): String {
    return "orderBy member \"$orderByMember\" is either not " +
            "properly formulated or references columns that are not in the " +
            "associated entity. All members in the orderBy array should either be" +
            " a column name or a column name following by \"asc\" or \"desc\""
}

fun queriedColumnInProjectionNotInEntity(queriedColumn: String, entity: String): String {
    return "Column $queriedColumn being queried through the projection is not defined within the " +
            "specified entity $entity."
}

fun queriedColumnInProjectionTypeDoesntMatchReturnType(
    returnType: String,
    queriedColumnType: String,
    queriedColumn: String
): String {
    return "Return type $returnType does not match type" +
            " $queriedColumnType of column $queriedColumn being queried."
}

fun pojoHasMoreThanOneQualifyingConstructor(pojo: String): String {
    return "Pojo $pojo has more than one non private" +
            " constructor. Pojos should have only one non private constructor."
}

fun pojoIsNotInstantiable(pojo: String): String {
    return "Pojo $pojo is not instantiable! It has no non private non ignored " +
            "constructors, it must have one and only one such constructor, whether " +
            "parametrized or not."
}

fun pojoWithNullablePrimitive(fieldName: String, pojo: String): String {
    return "Field $fieldName of pojo $pojo is of a primitive type but is marked as " +
            "nullable. Please use the boxed type instead of the primitive of type for nullable " +
            "fields"
}

fun pojoFieldNotInEntity(
    fieldName: String,
    fieldType: String,
    columnName: String,
    pojo: String,
    entity: String
): String {
    return "Field $fieldName of type $fieldType corresponding to content" +
            " provider column $columnName in object $pojo doesn't match a field with same " +
            "type and content column in content entity $entity"
}

fun constructorFieldNotIncludedInProjectionNotNullable(fieldName: String, returnType: String):
        String {
    return "Field $fieldName in return object constructor $returnType is not included in the" +
            " supplied projection and is not nullable. Constructor fields that are not" +
            " included in a query projection should all be nullable."
}

fun columnInProjectionNotIncludedInReturnPojo(columnName: String, returnType: String):
        String {
    return "Column $columnName in projection array isn't included in the return type $returnType"
}

fun nullableEntityColumnNotNullableInPojo(
    fieldName: String,
    fieldType: String,
    columnName: String,
    entity: String
): String {
    return "Field $fieldName of type $fieldType corresponding to content provider column " +
    "$columnName is not nullable, however that column is declared as nullable in the " +
            "associated entity $entity. Please mark the field as nullable."
}

fun columnOnlyAsUri(): String = ": is an invalid uri, please follow it up with the parameter name"

fun missingUriParameter(parameterName: String): String {
    return "Parameter $parameterName mentioned as the uri does not exist! Please add it to " +
            "the method parameters"
}

fun uriParameterIsNotString(parameterName: String): String {
    return "Parameter $parameterName mentioned as the uri should be of type String"
}

fun strayColumnInSelectionErrorMessage(): String = "Found stray \":\" in the selection"

fun selectionParameterNotInMethodParameters(param: String): String {
    return "Selection argument $param is not specified in the method's parameters."
}

fun columnInSelectionMissingFromEntity(columnName: String, entity: String): String {
    return "Column $columnName in selection parameter does not exist in content entity $entity"
}

fun columnInContentUpdateParametersNotInEntity(
    paramName: String,
    columnName: String,
    entity: String
): String {
    return "Parameter $paramName is annotated with @ContentColumn and specifies that it should " +
            "update column $columnName, however that column was not found in content entity $entity"
}

fun mismatchedColumnTypeForColumnToBeUpdated(
    paramName: String,
    columnName: String,
    paramType:
    String,
    entityType: String,
    columnType: String
): String {
    return "Parameter $paramName linked to column " +
            "$columnName is of type $paramType however that column's type " +
            "as specified by entity $entityType is $columnType"
}

fun methodSpecifiesWhereClauseWhenUpdatingUsingEntity(paramName: String): String {
    return "@ContentUpdate annotated method specifies an entity as" +
            " parameter $paramName but also specifies a where clause. " +
            "Updates using an entity happen by matching the primary key and do not " +
            "take into consideration the where clause."
}

fun updatingMultipleEntitiesAtTheSameType(entityType: String, methodName: String): String {
    return "There is more than one parameter of the entity type " +
            "$entityType to the @ContentUpdate annotated method" +
            " $methodName. Only one entity can be update at a time."
}

fun contentUpdateAnnotatedMethodNotReturningAnInteger(): String {
    return "Methods annotated with @ContentUpdate should return an integer."
}

fun unsureWhatToUpdate(): String {
    return "Not sure what this @ContentUpdate annotated method is supposed" +
            " to update, @ContentUpdate annotated methods should either specify a parameter " +
            "of the entity's type which will update the existing row in the content provider " +
            "using info from the entity object and will match on the primary key, or specify " +
            "one or more parameters annotated with @ContentColumn which will result in updating" +
            " rows matching any given criteria in the where clause to the specified values"
}

fun nullableUpdateParamForNonNullableEntityColumn(
    paramName: String,
    columnName: String,
    entity: String
): String {
    return "Parameter $paramName corresponding content column $columnName is nullable, however " +
            "that column is specified as non nullable in entity $entity. Please ensure that " +
            "parameter $paramName is non nullable too."
}

fun ProcessingEnvironment.warn(warning: String, element: Element) {
    this.messager.printMessage(Diagnostic.Kind.WARNING, warning, element)
}