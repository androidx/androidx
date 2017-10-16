Author: aurimas@google.com
Updated: 6/6/2017

Instructions on how to compile and apply refaster rules to support library

0. Download error-prone and refaster jars
http://errorprone.info/docs/refaster will have up to date instructions

1. Compile the refaster rule (in this example IsAtLeastO.java)
java -cp /path/to/android.jar:/path/to/support-compat.jar:javac-9-dev-r3297-4.jar:error_prone_refaster-2.0.18.jar com.google.errorprone.refaster.RefasterRuleCompiler IsAtLeastO.java --out `pwd`/myrule.refaster

2. Update build to use the refaster rule
Add compiler args to error-prone in SupportLibraryPlugin.groovy
'-XepPatchChecks:refaster:/path/to/refaster/myrule.refaster',
'-XepPatchLocation:' + project.projectDir

3. Compile support library using the refaster rule
./gradlew assembleErrorProne

4. Apply patches
error-prone will produce patch files like "design/error-prone.patch" and to apply them, cd into the
directory e.g. "design" and then run:
patch -p0 -u -i error-prone.patch

5. Rules have been applied! Celebrate!