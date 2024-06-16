## Binary Compatibility Validator

The goal of the binary compatiblity validator is check for binary incompatible changes between two
sets of klibs. It does not handle Android / JVM targets, and within AndroidX those are checked by
Metalava.

### Structure

The project consists of a Klib dump parser that parses the format created by
[Kotlin/binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
and a binary compatibility checker which compares two sets of `LibraryAbi`s for incompatible
changes. `LibraryAbi`s can be obtained either by parsing dump text, or directly from a klib file
using `org.jetbrains.kotlin.library.abi.LibraryAbiReader`.

### List of implemented rules

The tool currently handles a subset of compatible changes listed below. Generally every change that
is not compatible is assumed to be incompatible and will result in a check failure.

If you believe that a change is incorrectly marked compatible, or have a feature request for a type
of compatible change that is not implemented,
[please file a bug](https://b.corp.google.com/issues/new?component=1102332).

### Compatible Changes

* General
  * Change the internals of existing implementations.
  * Add a new concrete entity
    * add a new class of any modality
    * add a new function with implementation to a class.
  * Change a private entity to a publicly accessible entity.
  * Make an entity visibility weaker (more accessible).
  * Add or remove an annotation other than @BinarySignatureName.
* Classes
    * Classifier kinds
      * Changing a class to a data class
      * Changing a data class to a non-data class, as long as the ABI surface is the same
      * Changing an interface to a functional interface
    * Inheritance
        * Abstract class to be non-abstract open
        * Sealed class to be non-sealed open
        * Sealed class to be non-sealed abstract
        * Changing the order of supertypes
        * Changing a supertype to one of its subtypes, as long as the ABI surface is the same
* Functions
    * Function kind
        * Non-inline function to be inline
    * Changes to function parameters and receivers
        * Adding a default value to a parameter
        * Changing a default value for a parameter
    * Inheritance
        * Abstract function to non-abstract open
* Properties
    * Changes between property kinds
        * Non-const property to be const
        * Non-inline property to be inline
        * Changing a non-open read-only property to a mutable property
* Type parameters
    * Change from a reified type parameter to a regular one

### Incompatible Changes

Although any changes not in the above list are assumed to be incompatible, the following are
explicitly tested for.

* General
  * Change a publicly accessible entity to a private entity.
  * Make an entity visibility stronger (less accessible).
  * Change an entity name.
  * Remove an existing concrete entity.
* Type Parameters
  * Adding new type parameters
  * Removing existing type parameters
  * Changing from a regular type parameter to a reified one
  * Changing type parameter variance
  * Changing the set of types in type parameter bounds
* Classes
  * Classifier kinds
    * To/from an enum class
    * To/from an annotation class
    * To/from a value class
    * Non-interface to/from an interface
    * Non-object to/from an object
    * Nested classifier to/from an inner classifier
    * Functional interface to an interface
* Inheritance
    * Non-abstract class to be abstract
    * Non-sealed classifier to be sealed
    * Changing the set of supertypes
* Functions
    * Function kinds
        * Inline function to be non-inline
        * To/from a suspending function
    * Parameters and receivers
        * Removing a default value from a parameter
        * Changing a vararg parameter to/from an array type parameter
        * Changing between inline, crossinline and noinline inline function parameters
        * Changing between an extension receiver and a function’s first parameter
* Properties
    * Property kinds
        * Const property to be non-const
        * Inline property to be non-inline
        * Mutable property to a read-only property
        * Open read-only property to an open mutable property

### Running the tools

Within AndroidX, these tools are used for the tasks `updateAbi` and `checkAbi`, see
`androidx.build.binarycompatibilityvalidator.BinaryCompatibilityValidation` for more information.

To run the parser stand-alone:

```
val parsed: LibraryAbi = KlibDumpParser(dumpFiletext).parse()
```

To run compatibility checks stand-alone:

```
BinaryCompatibilityChecker.checkAllBinariesAreCompatible(
    mapOf("<some target>" to libraryAbiAfter),
    mapOf("<some target>" to libraryAbiBefore),
)
```
