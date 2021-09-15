# Useful utilities for debugging profiles

This package is a collection of scripts of various polish to help interactively verify aot profiles,
or the behavior of various versions of Android when art compiles profiles.

## Don't depend on these scripts
The scripts in this directory are not maintained, and should never be used in a production
dependency such as CI.

## Contributing

Feel free to add any useful scripts you've developed here. If you find yourself doing similar things
repeatedly, please consider wrapping up the commands in a script.

Things to check before contributing:

1. AOSP header is added to all files
2. Script contains a usage output
3. Script can run without depending on files in other directories, or takes all dependencies as
   command line arguments
4. Is reasonably useful.