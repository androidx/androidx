#!/bin/bash
set -e

cd "$(dirname $0)"
# pwd is now frameworks/support/development/referenceDocs

if [[ -z "$1" ]]; then
      printf "Please supply the git SHA at which the build was cut\n"
      exit
fi

tipOfTreeSha=$1

printf "============================ STEP 1 =============================== \n"
printf "== Generate the out directory: $(pwd)/out/compose-ref-docs \n"
printf "=================================================================== \n"

refDocDir="$(pwd)/out/compose-ref-docs"
# Remove the existing directory to avoid conflicts from previous runs
rm -rf $refDocDir
mkdir -p $refDocDir

cd ../../ui
# pwd is now frameworks/support/ui

printf "============================ STEP 2 =============================== \n"
printf "== Generate the Compose reference docs at ${tipOfTreeSha} \n"
printf "=================================================================== \n"

repo sync -j64
git reset --hard $tipOfTreeSha
git fetch "https://android.googlesource.com/platform/frameworks/support" refs/changes/23/1215823/8 && git cherry-pick FETCH_HEAD
./gradlew distTipOfTreeDokkaDocs

printf "============================ STEP 3 =============================== \n"
printf "== Copy the zip dokkaTipOfTreeDocs-0.zip and format the reference docs \n"
printf "=================================================================== \n"

cp ../../../out/dist/ui/dokkaTipOfTreeDocs-0.zip $refDocDir
cd $refDocDir
# pwd is now frameworks/support/development/referenceDocs/out/compose-ref-docs
unzip dokkaTipOfTreeDocs-0.zip
rm reference/java -rf
rm reference/kotlin/package-list -f
rm reference/kotlin/sample -rf
rm reference/kotlin/androidx/annotation -rf
rm reference/kotlin/androidx/benchmark -rf
rm reference/kotlin/androidx/tracing -rf
rm -rf reference/kotlin/androidx/ui/androidview
rm -rf reference/kotlin/androidx/ui/core/samples
mv reference/kotlin/androidx/_toc.yaml reference/kotlin/androidx/ui/.
mv reference/kotlin/androidx/packages.html reference/kotlin/androidx/ui/.
mv reference/kotlin/androidx/classes.html reference/kotlin/androidx/ui/.
sed -i "s/href=\"animation\//href=\"..\/animation\//g" reference/kotlin/androidx/ui/classes.html
sed -i "s/href=\"animation\//href=\"..\/animation\//g" reference/kotlin/androidx/ui/packages.html
sed -i "s/href=\"compose/href=\"..\/compose/g" reference/kotlin/androidx/ui/packages.html
sed -i "s/href=\"compose/href=\"..\/compose/g" reference/kotlin/androidx/ui/classes.html
sed -i "s/href=\"ui\//href=\"/g" reference/kotlin/androidx/ui/packages.html
sed -i "s/href=\"ui\//href=\"/g" reference/kotlin/androidx/ui/classes.html
sed -i "s/path: \/reference\/kotlin\/androidx\/classes.html/path: \/reference\/kotlin\/androidx\/ui\/classes.html/g" reference/kotlin/androidx/ui/_toc.yaml
sed -i "s/path: \/reference\/kotlin\/androidx\/packages.html/path: \/reference\/kotlin\/androidx\/ui\/packages.html/g" reference/kotlin/androidx/ui/_toc.yaml
sed -i "s/href=\"animation\/package-summary.html\">androidx.animation/href=\"..\/animation\/package-summary.html\">androidx.animation/g" reference/kotlin/androidx/ui/packages.html

grep -rl "{% setvar book_path %}/reference/kotlin/androidx/_book.yaml{% endsetvar %}" reference/kotlin/androidx/animation | xargs sed -i "s/{% setvar book_path %}\/reference\/kotlin\/androidx\/_book.yaml{% endsetvar %}/{% setvar book_path %}\/reference\/kotlin\/androidx\/ui\/_book.yaml{% endsetvar %}/g"

grep -rl "{% setvar book_path %}/reference/kotlin/androidx/_book.yaml{% endsetvar %}" reference/kotlin/androidx/compose | xargs sed -i "s/{% setvar book_path %}\/reference\/kotlin\/androidx\/_book.yaml{% endsetvar %}/{% setvar book_path %}\/reference\/kotlin\/androidx\/ui\/_book.yaml{% endsetvar %}/g"

grep -rl "{% setvar book_path %}/reference/kotlin/androidx/_book.yaml{% endsetvar %}" reference/kotlin/androidx/ui | xargs sed -i "s/{% setvar book_path %}\/reference\/kotlin\/androidx\/_book.yaml{% endsetvar %}/{% setvar book_path %}\/reference\/kotlin\/androidx\/ui\/_book.yaml{% endsetvar %}/g"

printf "============================ STEP 4 =============================== \n"
printf "== Generate the language switchers \n"
printf "=================================================================== \n"

# The switcher script still requires python2 to run correctly
cd reference
# pwd is now frameworks/support/development/referenceDocs/out/compose-ref-docs/reference
if [ -f ../../../switcher.py ]; then
    python2 ./../../../switcher.py --work androidx
else
    printf "============================ STEP 4b ============================== \n"
    printf "== The switcher script is not available at %tipOfTreeSha.\n"
    printf "== Please checkout AOSP master run the following command \n"
    printf "=================================================================== \n"
    printf "\`\`\`\npython2 ./aosp-master/tools/doc_generation/switcher4.py --work androidx\n\`\`\`\n"
fi

printf "============================ STEP 5 =============================== \n"
printf "== Run the following command to copy the docs into Google3 \n"
printf "=================================================================== \n"

printf "
\`\`\`
g4d -f androidx-ref-docs-stage && \
cd third_party/devsite/android/en/reference && \
g4 sync && \
cp -r $(pwd)/* . && \
/google/data/ro/projects/devsite/two/live/devsite2.par stage kotlin/androidx
\`\`\`\n"

exit
