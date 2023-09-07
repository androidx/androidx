#!/bin/bash

primitives=("Float" "Long" "Int")
suffixes=("f" "L" "")

scriptDir=`dirname ${PWD}/${0}`

for index in ${!primitives[@]}
do
  primitive=${primitives[$index]}
  firstLower=`echo ${primitive:0:1} | tr '[:upper:]' '[:lower:]'`
  lower="${firstLower}${primitive:1}"
  echo "generating ${primitive}ObjectMap.kt"
  sed -e "s/PKey/${primitive}/g" -e "s/pKey/${lower}/g" ${scriptDir}/PKeyObjectMap.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/${primitive}ObjectMap.kt
  echo "generating ${primitive}ObjectMapTest.kt"
  sed -e "s/PValue/${primitive}/g" ${scriptDir}/ObjectPValueMap.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/Object${primitive}Map.kt

  suffix=${suffixes[$index]}
  echo "generating Object${primitive}Map.kt"
  sed -e "s/PValue/${primitive}/g" -e "s/ValueSuffix/${suffix}/g" ${scriptDir}/ObjectPValueMapTest.kt.template > ${scriptDir}/../src/commonTest/kotlin/androidx/collection/Object${primitive}MapTest.kt
  echo "generating Object${primitive}MapTest.kt"
  sed -e "s/PKey/${primitive}/g" -e"s/pKey/${lower}/g" -e "s/KeySuffix/${suffix}/g" ${scriptDir}/PKeyObjectMapTest.kt.template > ${scriptDir}/../src/commonTest/kotlin/androidx/collection/${primitive}ObjectMapTest.kt

  echo "generating ${primitive}Set.kt"
  sed -e "s/PKey/${primitive}/g" -e"s/pKey/${lower}/g" ${scriptDir}/PKeySet.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/${primitive}Set.kt
  echo "generating ${primitive}SetTest.kt"
  sed -e "s/PKey/${primitive}/g" -e"s/pKey/${lower}/g" -e "s/KeySuffix/${suffix}/g" ${scriptDir}/PKeySetTest.kt.template > ${scriptDir}/../src/commonTest/kotlin/androidx/collection/${primitive}SetTest.kt

  echo "generating ${primitive}List.kt"
  sed -e "s/PKey/${primitive}/g" -e"s/pKey/${lower}/g" ${scriptDir}/PKeyList.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/${primitive}List.kt
  echo "generating ${primitive}ListTest.kt"
  sed -e "s/PKey/${primitive}/g" -e"s/pKey/${lower}/g" -e "s/KeySuffix/${suffix}/g" ${scriptDir}/PKeyListTest.kt.template > ${scriptDir}/../src/commonTest/kotlin/androidx/collection/${primitive}ListTest.kt
done

for keyIndex in ${!primitives[@]}
do
  key=${primitives[$keyIndex]}
  firstLower=`echo ${key:0:1} | tr '[:upper:]' '[:lower:]'`
  lowerKey="${firstLower}${key:1}"
  keySuffix=${suffixes[$keyIndex]}
  for valueIndex in ${!primitives[@]}
  do
    value=${primitives[$valueIndex]}
    valueSuffix=${suffixes[$valueIndex]}
    echo "generating ${key}${value}Map.kt"
    sed -e "s/PKey/${key}/g" -e "s/pKey/${lowerKey}/g" -e "s/PValue/${value}/g" ${scriptDir}/PKeyPValueMap.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/${key}${value}Map.kt
    echo "generating ${key}${value}MapTest.kt"
    sed -e "s/PKey/${key}/g" -e "s/pKey/${lowerKey}/g" -e "s/PValue/${value}/g" -e "s/ValueSuffix/${valueSuffix}/g" -e "s/KeySuffix/${keySuffix}/g" ${scriptDir}/PKeyPValueMapTest.kt.template > ${scriptDir}/../src/commonTest/kotlin/androidx/collection/${key}${value}MapTest.kt
  done
done
