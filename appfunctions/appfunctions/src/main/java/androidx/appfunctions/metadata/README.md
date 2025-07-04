# AppFunction Metadata

This document explains how the `AppFunctionMetadata` and related Kotlin classes are inspired by the
OpenAPI specification, and highlights the similarities, deviations, and important design choices
made in this implementation.

## Overview

The package defines a set of classes to represent metadata for AppFunctions. This metadata is
designed to be similar in spirit to the OpenAPI specification, which is used to describe REST APIs.
The goal here is to provide a structured and machine-readable way to define and understand the
inputs, outputs, and other characteristics of functions within an application, much like OpenAPI
does for web APIs.

## Similarities to OpenAPI

*   **Operation Object (`AppFunctionMetadata`):**  Just like an OpenAPI Operation Object describes
    an API endpoint, `AppFunctionMetadata` describes an AppFunction. It contains essential
    information needed to invoke and understand the function.
*   **Parameter Object (`AppFunctionParameterMetadata`):**  This class directly corresponds to the
    Parameter Object in OpenAPI. It defines the name, requirement status (`isRequired`), and data
    type (`dataType`) of a function parameter, similar to how OpenAPI describes request parameters.
*   **Schema Object (`AppFunctionDataTypeMetadata` and subclasses):**  `AppFunctionDataTypeMetadata`
    and its subclasses (`AppFunctionPrimitiveTypeMetadata`, `AppFunctionObjectTypeMetadata`,
    `AppFunctionArrayTypeMetadata`, `AppFunctionReferenceTypeMetadata`) are analogous to the Schema
    Object in OpenAPI. They are used to define the data types of parameters and responses, including
    primitive types, objects, arrays, and references to reusable schemas.
*   **Components Object (`AppFunctionComponentsMetadata`):**  This class mirrors the Components
    Object in OpenAPI. It provides a mechanism to define reusable data type schemas (`dataTypes`)
    that can be referenced from different parts of the function definition.

## Differences from OpenAPI

While inspired by OpenAPI, this implementation also deviates in several ways, tailored for the
specific needs of describing AppFunctions rather than web APIs:

*   **Focus on Function Metadata, Not API Endpoints:** OpenAPI is primarily designed to describe
    RESTful APIs. This implementation is focused on describing the metadata of *functions* within an
    application. Therefore, concepts specific to HTTP like paths, methods, request bodies, headers,
    and multiple response codes are absent or simplified.
*   **Simplified Response Handling (`AppFunctionResponseMetadata`):**  OpenAPI's `Responses Object`
    is quite complex, allowing for multiple response codes, headers, and content types.
    `AppFunctionResponseMetadata` is significantly simpler, primarily focusing on the `valueType`
   (the schema of the return value). This simplification is because the function in Kotlin is more
   controlled than a general web API, having a single primary successful response type.
*   **Schema Category and Version (`AppFunctionSchemaMetadata`) vs. Schema Object Naming:** In
    OpenAPI, the term "Schema Object" refers to the definition of data structures (like objects,
    arrays, primitive types). In `AppFunctionMetadata`, the class responsible for defining data
    types is named `AppFunctionDataTypeMetadata`.  The term "Schema" is instead used in
    `AppFunctionSchemaMetadata` to represent a *predefined function schema*, identified by a
    category, name, and version. This "AppFunction Schema" represents a higher-level concept of a
    function's overall purpose and contract. For instance, we might predefine a schema for
    "CreateNote," which specifies the function's required inputs and expected outputs.

## Persistent format

For each Metadata class, there is a corresponding internal "Document" class (e.g.,
`AppFunctionParameterMetadataDocument`) that represents a persistent format of it. These "Document"
classes are AppSearch documents. They are intended to be stored in the Android platform's AppSearch
database, enabling applications with the necessary permissions to efficiently look up and discover
available AppFunctions.

**Important Note on Document Class Divergence:** Due to the underlying storage mechanism of
AppSearch, the structure of the "Document" classes may diverge from their corresponding Metadata
classes. For instance, AppSearch has limitations on the data types it can directly store, and
complex structures like `Map<>` are not natively supported.  As a result, Document classes might
need to represent data in a flattened or transformed manner. You will notice that while Metadata
classes may utilize `Map<>` (e.g., `AppFunctionObjectTypeMetadata.properties`,
`AppFunctionComponentsMetadata.dataTypes`), the corresponding Document classes will likely use
alternative representations suitable for AppSearch, such as lists of key-value pairs or other
compatible structures.
