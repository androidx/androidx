/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.CustomTypeConverter
import androidx.room.vo.Field
import com.squareup.javapoet.TypeName

object ProcessorErrors {
    private fun String.trim(): String {
        return this.trimIndent().replace("\n", " ")
    }
    val MISSING_QUERY_ANNOTATION = "Query methods must be annotated with ${Query::class.java}"
    val MISSING_INSERT_ANNOTATION = "Insertion methods must be annotated with ${Insert::class.java}"
    val MISSING_DELETE_ANNOTATION = "Deletion methods must be annotated with ${Delete::class.java}"
    val MISSING_UPDATE_ANNOTATION = "Update methods must be annotated with ${Update::class.java}"
    val MISSING_RAWQUERY_ANNOTATION = "RawQuery methods must be annotated with" +
            " ${RawQuery::class.java}"
    val INVALID_ON_CONFLICT_VALUE = "On conflict value must be one of @OnConflictStrategy values."
    val INVALID_INSERTION_METHOD_RETURN_TYPE = "Methods annotated with @Insert can return either" +
            " void, long, Long, long[], Long[] or List<Long>."
    val TRANSACTION_REFERENCE_DOCS = "https://developer.android.com/reference/android/arch/" +
            "persistence/room/Transaction.html"

    fun insertionMethodReturnTypeMismatch(definedReturn: TypeName,
                                          expectedReturnTypes: List<TypeName>): String {
        return "Method returns $definedReturn but it should return one of the following: `" +
                expectedReturnTypes.joinToString(", ") + "`. If you want to return the list of" +
                " row ids from the query, your insertion method can receive only 1 parameter."
    }

    val ABSTRACT_METHOD_IN_DAO_MISSING_ANY_ANNOTATION = "Abstract method in DAO must be annotated" +
            " with ${Query::class.java} AND ${Insert::class.java}"
    val INVALID_ANNOTATION_COUNT_IN_DAO_METHOD = "An abstract DAO method must be" +
            " annotated with one and only one of the following annotations: " +
            DaoProcessor.PROCESSED_ANNOTATIONS.joinToString(",") {
        it.java.simpleName
    }
    val CANNOT_RESOLVE_RETURN_TYPE = "Cannot resolve return type for %s"
    val CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS = "Cannot use unbound generics in query" +
            " methods. It must be bound to a type through base Dao class."
    val CANNOT_USE_UNBOUND_GENERICS_IN_INSERTION_METHODS = "Cannot use unbound generics in" +
            " insertion methods. It must be bound to a type through base Dao class."
    val CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS = "Cannot use unbound fields in entities."
    val CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES = "Cannot use unbound generics in Dao classes." +
            " If you are trying to create a base DAO, create a normal class, extend it with type" +
            " params then mark the subclass with @Dao."
    val CANNOT_FIND_GETTER_FOR_FIELD = "Cannot find getter for field."
    val CANNOT_FIND_SETTER_FOR_FIELD = "Cannot find setter for field."
    val MISSING_PRIMARY_KEY = "An entity must have at least 1 field annotated with @PrimaryKey"
    val AUTO_INCREMENTED_PRIMARY_KEY_IS_NOT_INT = "If a primary key is annotated with" +
            " autoGenerate, its type must be int, Integer, long or Long."
    val AUTO_INCREMENT_EMBEDDED_HAS_MULTIPLE_FIELDS = "When @PrimaryKey annotation is used on a" +
            " field annotated with @Embedded, the embedded class should have only 1 field."

    fun multiplePrimaryKeyAnnotations(primaryKeys: List<String>): String {
        return """
                You cannot have multiple primary keys defined in an Entity. If you
                want to declare a composite primary key, you should use @Entity#primaryKeys and
                not use @PrimaryKey. Defined Primary Keys:
                ${primaryKeys.joinToString(", ")}""".trim()
    }

    fun primaryKeyColumnDoesNotExist(columnName: String, allColumns: List<String>): String {
        return "$columnName referenced in the primary key does not exists in the Entity." +
                " Available column names:${allColumns.joinToString(", ")}"
    }

    val DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE = "Dao class must be an abstract class or" +
            " an interface"
    val DATABASE_MUST_BE_ANNOTATED_WITH_DATABASE = "Database must be annotated with @Database"
    val DAO_MUST_BE_ANNOTATED_WITH_DAO = "Dao class must be annotated with @Dao"

    fun daoMustHaveMatchingConstructor(daoName: String, dbName: String): String {
        return """
                $daoName needs to have either an empty constructor or a constructor that takes
                $dbName as its only parameter.
                """.trim()
    }

    val ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY = "Entity class must be annotated with @Entity"
    val DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES = "@Database annotation must specify list" +
            " of entities"
    val COLUMN_NAME_CANNOT_BE_EMPTY = "Column name cannot be blank. If you don't want to set it" +
            ", just remove the @ColumnInfo annotation or use @ColumnInfo.INHERIT_FIELD_NAME."

    val ENTITY_TABLE_NAME_CANNOT_BE_EMPTY = "Entity table name cannot be blank. If you don't want" +
            " to set it, just remove the tableName property."

    val CANNOT_BIND_QUERY_PARAMETER_INTO_STMT = "Query method parameters should either be a" +
            " type that can be converted into a database column or a List / Array that contains" +
            " such type. You can consider adding a Type Adapter for this."

    val QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE = "Query/Insert method parameters cannot " +
            "start with underscore (_)."

    val CANNOT_FIND_QUERY_RESULT_ADAPTER = "Not sure how to convert a Cursor to this method's " +
            "return type"

    val INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT = "Method annotated with" +
            " @Insert but does not have any parameters to insert."

    val DELETION_MISSING_PARAMS = "Method annotated with" +
            " @Delete but does not have any parameters to delete."

    val UPDATE_MISSING_PARAMS = "Method annotated with" +
            " @Update but does not have any parameters to update."

    val TRANSACTION_METHOD_MODIFIERS = "Method annotated with @Transaction must not be " +
            "private, final, or abstract. It can be abstract only if the method is also" +
            " annotated with @Query."

    val TRANSACTION_MISSING_ON_RELATION = "The return value includes a Pojo with a @Relation." +
            " It is usually desired to annotate this method with @Transaction to avoid" +
            " possibility of inconsistent results between the Pojo and its relations. See " +
            TRANSACTION_REFERENCE_DOCS + " for details."

    val CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER = "Type of the parameter must be a class " +
            "annotated with @Entity or a collection/array of it."

    val DB_MUST_EXTEND_ROOM_DB = "Classes annotated with @Database should extend " +
            RoomTypeNames.ROOM_DB

    val LIVE_DATA_QUERY_WITHOUT_SELECT = "LiveData return type can only be used with SELECT" +
            " queries."

    val OBSERVABLE_QUERY_NOTHING_TO_OBSERVE = "Observable query return type (LiveData, Flowable" +
            ", DataSource, DataSourceFactory etc) can only be used with SELECT queries that" +
            " directly or indirectly (via @Relation, for example) access at least one table. For" +
            " @RawQuery, you should specify the list of tables to be observed via the" +
            " observedEntities field."

    val RECURSIVE_REFERENCE_DETECTED = "Recursive referencing through @Embedded and/or @Relation " +
            "detected: %s"

    private val TOO_MANY_MATCHING_GETTERS = "Ambiguous getter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."

    fun tooManyMatchingGetters(field: Field, methodNames: List<String>): String {
        return TOO_MANY_MATCHING_GETTERS.format(field, methodNames.joinToString(", "))
    }

    private val TOO_MANY_MATCHING_SETTERS = "Ambiguous setter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."

    fun tooManyMatchingSetter(field: Field, methodNames: List<String>): String {
        return TOO_MANY_MATCHING_SETTERS.format(field, methodNames.joinToString(", "))
    }

    val CANNOT_FIND_COLUMN_TYPE_ADAPTER = "Cannot figure out how to save this field into" +
            " database. You can consider adding a type converter for it."

    val CANNOT_FIND_STMT_BINDER = "Cannot figure out how to bind this field into a statement."

    val CANNOT_FIND_CURSOR_READER = "Cannot figure out how to read this field from a cursor."

    private val MISSING_PARAMETER_FOR_BIND = "Each bind variable in the query must have a" +
            " matching method parameter. Cannot find method parameters for %s."

    fun missingParameterForBindVariable(bindVarName: List<String>): String {
        return MISSING_PARAMETER_FOR_BIND.format(bindVarName.joinToString(", "))
    }

    private val UNUSED_QUERY_METHOD_PARAMETER = "Unused parameter%s: %s"
    fun unusedQueryMethodParameter(unusedParams: List<String>): String {
        return UNUSED_QUERY_METHOD_PARAMETER.format(
                if (unusedParams.size > 1) "s" else "",
                unusedParams.joinToString(","))
    }

    private val DUPLICATE_TABLES = "Table name \"%s\" is used by multiple entities: %s"
    fun duplicateTableNames(tableName: String, entityNames: List<String>): String {
        return DUPLICATE_TABLES.format(tableName, entityNames.joinToString(", "))
    }

    val DELETION_METHODS_MUST_RETURN_VOID_OR_INT = "Deletion methods must either return void or" +
            " return int (the number of deleted rows)."

    val UPDATE_METHODS_MUST_RETURN_VOID_OR_INT = "Update methods must either return void or" +
            " return int (the number of updated rows)."

    val DAO_METHOD_CONFLICTS_WITH_OTHERS = "Dao method has conflicts."

    fun duplicateDao(dao: TypeName, methodNames: List<String>): String {
        return """
                All of these functions [${methodNames.joinToString(", ")}] return the same DAO
                class [$dao].
                A database can use a DAO only once so you should remove ${methodNames.size - 1} of
                these conflicting DAO methods. If you are implementing any of these to fulfill an
                interface, don't make it abstract, instead, implement the code that calls the
                other one.
                """.trim()
    }

    fun pojoMissingNonNull(pojoTypeName: TypeName, missingPojoFields: List<String>,
                           allQueryColumns: List<String>): String {
        return """
        The columns returned by the query does not have the fields
        [${missingPojoFields.joinToString(",")}] in $pojoTypeName even though they are
        annotated as non-null or primitive.
        Columns returned by the query: [${allQueryColumns.joinToString(",")}]
        """.trim()
    }

    fun cursorPojoMismatch(pojoTypeName: TypeName,
                           unusedColumns: List<String>, allColumns: List<String>,
                           unusedFields: List<Field>, allFields: List<Field>): String {
        val unusedColumnsWarning = if (unusedColumns.isNotEmpty()) {
            """
                The query returns some columns [${unusedColumns.joinToString(", ")}] which are not
                use by $pojoTypeName. You can use @ColumnInfo annotation on the fields to specify
                the mapping.
            """.trim()
        } else {
            ""
        }
        val unusedFieldsWarning = if (unusedFields.isNotEmpty()) {
            """
                $pojoTypeName has some fields
                [${unusedFields.joinToString(", ") { it.columnName }}] which are not returned by the
                query. If they are not supposed to be read from the result, you can mark them with
                @Ignore annotation.
            """.trim()
        } else {
            ""
        }
        return """
            $unusedColumnsWarning
            $unusedFieldsWarning
            You can suppress this warning by annotating the method with
            @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH).
            Columns returned by the query: ${allColumns.joinToString(", ")}.
            Fields in $pojoTypeName: ${allFields.joinToString(", ") { it.columnName }}.
            """.trim()
    }

    val TYPE_CONVERTER_UNBOUND_GENERIC = "Cannot use unbound generics in Type Converters."
    val TYPE_CONVERTER_BAD_RETURN_TYPE = "Invalid return type for a type converter."
    val TYPE_CONVERTER_MUST_RECEIVE_1_PARAM = "Type converters must receive 1 parameter."
    val TYPE_CONVERTER_EMPTY_CLASS = "Class is referenced as a converter but it does not have any" +
            " converter methods."
    val TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR = "Classes that are used as TypeConverters must" +
            " have no-argument public constructors."
    val TYPE_CONVERTER_MUST_BE_PUBLIC = "Type converters must be public."

    fun duplicateTypeConverters(converters: List<CustomTypeConverter>): String {
        return "Multiple methods define the same conversion. Conflicts with these:" +
                " ${converters.joinToString(", ") { it.toString() }}"
    }

    // TODO must print field paths.
    val POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME = "Field has non-unique column name."

    fun pojoDuplicateFieldNames(columnName: String, fieldPaths: List<String>): String {
        return "Multiple fields have the same columnName: $columnName." +
                " Field names: ${fieldPaths.joinToString(", ")}."
    }

    fun embeddedPrimaryKeyIsDropped(entityQName: String, fieldName: String): String {
        return "Primary key constraint on $fieldName is ignored when being merged into " +
                entityQName
    }

    val INDEX_COLUMNS_CANNOT_BE_EMPTY = "List of columns in an index cannot be empty"

    fun indexColumnDoesNotExist(columnName: String, allColumns: List<String>): String {
        return "$columnName referenced in the index does not exists in the Entity." +
                " Available column names:${allColumns.joinToString(", ")}"
    }

    fun duplicateIndexInEntity(indexName: String): String {
        return "There are multiple indices with name $indexName. This happen if you've declared" +
                " the same index multiple times or different indices have the same name. See" +
                " @Index documentation for details."
    }

    fun duplicateIndexInDatabase(indexName: String, indexPaths: List<String>): String {
        return "There are multiple indices with name $indexName. You should rename " +
                "${indexPaths.size - 1} of these to avoid the conflict:" +
                "${indexPaths.joinToString(", ")}."
    }

    fun droppedEmbeddedFieldIndex(fieldPath: String, grandParent: String): String {
        return "The index will be dropped when being merged into $grandParent" +
                "($fieldPath). You must re-declare it in $grandParent if you want to index this" +
                " field in $grandParent."
    }

    fun droppedEmbeddedIndex(entityName: String, fieldPath: String, grandParent: String): String {
        return "Indices defined in $entityName will be dropped when it is merged into" +
                " $grandParent ($fieldPath). You can re-declare them in $grandParent."
    }

    fun droppedSuperClassIndex(childEntity: String, superEntity: String): String {
        return "Indices defined in $superEntity will NOT be re-used in $childEntity. If you want" +
                " to inherit them, you must re-declare them in $childEntity." +
                " Alternatively, you can set inheritSuperIndices to true in the @Entity annotation."
    }

    fun droppedSuperClassFieldIndex(fieldName: String, childEntity: String,
                                    superEntity: String): String {
        return "Index defined on field `$fieldName` in $superEntity will NOT be re-used in" +
                " $childEntity. " +
                "If you want to inherit it, you must re-declare it in $childEntity." +
                " Alternatively, you can set inheritSuperIndices to true in the @Entity annotation."
    }

    val RELATION_NOT_COLLECTION = "Fields annotated with @Relation must be a List or Set."

    fun relationCannotFindEntityField(entityName: String, columnName: String,
                                      availableColumns: List<String>): String {
        return "Cannot find the child entity column `$columnName` in $entityName." +
                " Options: ${availableColumns.joinToString(", ")}"
    }

    fun relationCannotFindParentEntityField(entityName: String, columnName: String,
                                            availableColumns: List<String>): String {
        return "Cannot find the parent entity column `$columnName` in $entityName." +
                " Options: ${availableColumns.joinToString(", ")}"
    }

    val RELATION_IN_ENTITY = "Entities cannot have relations."

    val CANNOT_FIND_TYPE = "Cannot find type."

    fun relationAffinityMismatch(parentColumn: String, childColumn: String,
                                 parentAffinity: SQLTypeAffinity?,
                                 childAffinity: SQLTypeAffinity?): String {
        return """
        The affinity of parent column ($parentColumn : $parentAffinity) does not match the type
        affinity of the child column ($childColumn : $childAffinity).
        """.trim()
    }

    val CANNOT_USE_MORE_THAN_ONE_POJO_FIELD_ANNOTATION = "A field can be annotated with only" +
            " one of the following:" + PojoProcessor.PROCESSED_ANNOTATIONS.joinToString(",") {
        it.java.simpleName
    }

    fun relationBadProject(entityQName: String, missingColumnNames: List<String>,
                           availableColumnNames: List<String>): String {
        return """
        $entityQName does not have the following columns: ${missingColumnNames.joinToString(",")}.
        Available columns are: ${availableColumnNames.joinToString(",")}
        """.trim()
    }

    val MISSING_SCHEMA_EXPORT_DIRECTORY = "Schema export directory is not provided to the" +
            " annotation processor so we cannot export the schema. You can either provide" +
            " `room.schemaLocation` annotation processor argument OR set exportSchema to false."

    val INVALID_FOREIGN_KEY_ACTION = "Invalid foreign key action. It must be one of the constants" +
            " defined in ForeignKey.Action"

    fun foreignKeyNotAnEntity(className: String): String {
        return """
        Classes referenced in Foreign Key annotations must be @Entity classes. $className is not
        an entity
        """.trim()
    }

    val FOREIGN_KEY_CANNOT_FIND_PARENT = "Cannot find parent entity class."

    fun foreignKeyChildColumnDoesNotExist(columnName: String, allColumns: List<String>): String {
        return "($columnName) referenced in the foreign key does not exists in the Entity." +
                " Available column names:${allColumns.joinToString(", ")}"
    }

    fun foreignKeyParentColumnDoesNotExist(parentEntity: String,
                                           missingColumn: String,
                                           allColumns: List<String>): String {
        return "($missingColumn) does not exist in $parentEntity. Available columns are" +
                " ${allColumns.joinToString(",")}"
    }

    val FOREIGN_KEY_EMPTY_CHILD_COLUMN_LIST = "Must specify at least 1 column name for the child"

    val FOREIGN_KEY_EMPTY_PARENT_COLUMN_LIST = "Must specify at least 1 column name for the parent"

    fun foreignKeyColumnNumberMismatch(
            childColumns: List<String>, parentColumns: List<String>): String {
        return """
                Number of child columns in foreign key must match number of parent columns.
                Child reference has ${childColumns.joinToString(",")} and parent reference has
                ${parentColumns.joinToString(",")}
               """.trim()
    }

    fun foreignKeyMissingParentEntityInDatabase(parentTable: String, childEntity: String): String {
        return """
                $parentTable table referenced in the foreign keys of $childEntity does not exist in
                the database. Maybe you forgot to add the referenced entity in the entities list of
                the @Database annotation?""".trim()
    }

    fun foreignKeyMissingIndexInParent(parentEntity: String, parentColumns: List<String>,
                                       childEntity: String, childColumns: List<String>): String {
        return """
                $childEntity has a foreign key (${childColumns.joinToString(",")}) that references
                $parentEntity (${parentColumns.joinToString(",")}) but $parentEntity does not have
                a unique index on those columns nor the columns are its primary key.
                SQLite requires having a unique constraint on referenced parent columns so you must
                add a unique index to $parentEntity that has
                (${parentColumns.joinToString(",")}) column(s).
               """.trim()
    }

    fun foreignKeyMissingIndexInChildColumns(childColumns: List<String>): String {
        return """
                (${childColumns.joinToString(",")}) column(s) reference a foreign key but
                they are not part of an index. This may trigger full table scans whenever parent
                table is modified so you are highly advised to create an index that covers these
                columns.
               """.trim()
    }

    fun foreignKeyMissingIndexInChildColumn(childColumn: String): String {
        return """
                $childColumn column references a foreign key but it is not part of an index. This
                may trigger full table scans whenever parent table is modified so you are highly
                advised to create an index that covers this column.
               """.trim()
    }

    fun shortcutEntityIsNotInDatabase(database: String, dao: String, entity: String): String {
        return """
                $dao is part of $database but this entity is not in the database. Maybe you forgot
                to add $entity to the entities section of the @Database?
                """.trim()
    }

    val MISSING_ROOM_GUAVA_ARTIFACT = "To use Guava features, you must add `guava`" +
            " artifact from Room as a dependency. androidx.room:guava:<version>"

    val MISSING_ROOM_RXJAVA2_ARTIFACT = "To use RxJava2 features, you must add `rxjava2`" +
            " artifact from Room as a dependency. androidx.room:rxjava2:<version>"

    fun ambigiousConstructor(
            pojo: String, paramName: String, matchingFields: List<String>): String {
        return """
            Ambiguous constructor. The parameter ($paramName) in $pojo matches multiple fields:
            [${matchingFields.joinToString(",")}]. If you don't want to use this constructor,
            you can annotate it with @Ignore. If you want Room to use this constructor, you can
            rename the parameters to exactly match the field name to fix the ambiguity.
            """.trim()
    }

    val MISSING_POJO_CONSTRUCTOR = """
            Entities and Pojos must have a usable public constructor. You can have an empty
            constructor or a constructor whose parameters match the fields (by name and type).
            """.trim()

    val TOO_MANY_POJO_CONSTRUCTORS = """
            Room cannot pick a constructor since multiple constructors are suitable. Try to annotate
            unwanted constructors with @Ignore.
            """.trim()

    val TOO_MANY_POJO_CONSTRUCTORS_CHOOSING_NO_ARG = """
            There are multiple good constructors and Room will pick the no-arg constructor.
            You can use the @Ignore annotation to eliminate unwanted constructors.
            """.trim()

    val RELATION_CANNOT_BE_CONSTRUCTOR_PARAMETER = """
            Fields annotated with @Relation cannot be constructor parameters. These values are
            fetched after the object is constructed.
            """.trim()

    val PAGING_SPECIFY_DATA_SOURCE_TYPE = "For now, Room only supports PositionalDataSource class."

    fun primaryKeyNull(field: String): String {
        return "You must annotate primary keys with @NonNull. \"$field\" is nullable. SQLite " +
                "considers this a " +
                "bug and Room does not allow it. See SQLite docs for details: " +
                "https://www.sqlite.org/lang_createtable.html"
    }

    val INVALID_COLUMN_NAME = "Invalid column name. Room does not allow using ` or \" in column" +
            " names"

    val INVALID_TABLE_NAME = "Invalid table name. Room does not allow using ` or \" in table names"

    val RAW_QUERY_BAD_PARAMS = "RawQuery methods should have 1 and only 1 parameter with type" +
            " String or SupportSQLiteQuery"

    val RAW_QUERY_BAD_RETURN_TYPE = "RawQuery methods must return a non-void type."

    fun rawQueryBadEntity(typeName: TypeName): String {
        return """
            observedEntities field in RawQuery must either reference a class that is annotated
            with @Entity or it should reference a Pojo that either contains @Embedded fields that
            are annotated with @Entity or @Relation fields.
            $typeName does not have these properties, did you mean another class?
            """.trim()
    }

    val RAW_QUERY_STRING_PARAMETER_REMOVED = "RawQuery does not allow passing a string anymore." +
            " Please use ${SupportDbTypeNames.QUERY}."
}
