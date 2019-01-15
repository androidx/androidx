# Script for adding git tags for each release

It is useful to tag commits for each androidx release for the following
reasons:

* This places the release history into the commit history.
* Developers can find which commits were included within each artifact
version.
* Tags are easy references for diff-ing two versions of an artifact.

This script takes in a file that lists the commits, artifacts, and versions
of that release.  A new release file must be added along with every release.
These files have the following filename format:
AndroidX-Release-YYYY-MM-DD.txt, where YYYY-MM-DD is the release date.

And have the following scructure:
<Release Date in YYYY-MM-DD format>
<SHA>:<artifactId>:<version>
<SHA>:<artifactId>:<version>
.
.
.
<SHA>:<artifactId>:<version>

The script then uses this file to generate two tags for each SHA in the file.
This first tag is maps the commit to the release date and artifactId:
<YYYY-MM-DD>-release-<artifactId>
The second tag maps the commit to the artifact version.  This is the last
commit included with that version (inclusive cuttoff):
<artifactId>-<version>
