#!/bin/bash

# To add another value class to generate, add a new value to the end of each of these lists.

# TODO For Color collections, we don't want to expose them until there's a public API exposure,
#       or if there's a lot of duplicate copies generated internally.
#       When we do want to have one single public instance of the Color collections,
#       It should be hosted in the targetPackage "androidx.compose.ui.graphics" package
#       with visibility "public" located at outputDirectory
#       "../../../compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics"
#       Until then, there should be no issue with having a couple internal instances.

# The value class to generate collections for (e.g. Color or Offset).
valueClasses=(
  "Color"
)

# The destination package for the collection classes.
targetPackages=(
  "androidx.compose.foundation.demos.collection"
)

# The backing field in the value class that converts it to a primitive (e.g. packedValue).
backingProperties=(
  "value.toLong()"
)

# The primitive type of the backing list (e.g. Long or Float)
backingPrimitives=(
  "Long"
)

# An operation done on the primitive to convert to the value class parameter
toParams=(
  ".toULong()"
)

# The visibility of the top-level classes and functions (e.g. public or internal)
visibilities=(
  "internal"
)

# The package in which the value class resides (e.g. androidx.compose.ui.ui.collection).
valuePackages=(
  "androidx.compose.ui.graphics"
)

# Where the resulting files are output, relative to the directory this script is in.
outputDirectories=(
  "../../../compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/collection"
)

scriptDir=$(dirname "${PWD}/${0}")

for index in "${!valueClasses[@]}"
do
  class=${valueClasses[$index]}
  targetPackage=${targetPackages[$index]}
  backingProperty=${backingProperties[$index]}
  backingPrimitive=${backingPrimitives[$index]}
  toParam=${toParams[$index]}
  visibility=${visibilities[$index]}
  valuePackage=${valuePackages[$index]}
  outputDirectory=${outputDirectories[$index]}

  firstLower=$(echo "${class:0:1}" | tr '[:upper:]' '[:lower:]')
  lowerCaseClass="${firstLower}${class:1}"

  realOutputDirectory="$(realpath "${scriptDir}/${outputDirectory}")"

  outputSetPath="${realOutputDirectory}/${class}Set.kt"
  echo "generating ${outputSetPath}"
  sed -e "s/PACKAGE/${targetPackage}/" -e "s/VALUE_CLASS/${class}/g" \
    -e "s/vALUE_CLASS/${lowerCaseClass}/g" -e "s/BACKING_PROPERTY/${backingProperty}/g" \
    -e "s/PRIMITIVE/${backingPrimitive}/g" -e "s/TO_PARAM/${toParam}/g" \
    -e "s/VALUE_PKG/${valuePackage}/g" -e "s/VISIBILITY/${visibility}/g" \
    "${scriptDir}/ValueClassSet.kt.template" \
    > "${outputSetPath}"

  outputListPath="${realOutputDirectory}/${class}List.kt"
  echo "generating ${outputListPath}"
  sed -e "s/PACKAGE/${targetPackage}/" -e "s/VALUE_CLASS/${class}/g" \
    -e "s/vALUE_CLASS/${lowerCaseClass}/g" -e "s/BACKING_PROPERTY/${backingProperty}/g" \
    -e "s/PRIMITIVE/${backingPrimitive}/g" -e "s/TO_PARAM/${toParam}/g" \
    -e "s/VALUE_PKG/${valuePackage}/g" -e "s/VISIBILITY/${visibility}/g" \
    "${scriptDir}/ValueClassList.kt.template" \
    > "${outputListPath}"
done
