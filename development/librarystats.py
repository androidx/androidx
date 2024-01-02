#!/usr/bin/env python3

"""
A script that can be used to query maven.google.com to get information about androidx artifacts to
see what stable/latest versions are available and when they were released.

Usage:
./librarystats.py

"""

from datetime import datetime
import requests
import re
import os
from xml.dom.minidom import parseString

current_directory = os.path.dirname(__file__)

libraries = []
docs_public_build = os.path.join(current_directory, r'../docs-public/build.gradle')

with open(docs_public_build, "r") as f:
    build_file_contents = f.read()
    p = re.compile('\("(androidx\..*)\:[0-9]+\.[0-9]+\.[0-9]+.*"\)')
    libraries = [val for val in p.findall(build_file_contents) if not val.endswith("-samples")]

cache_directory = os.path.join(current_directory, r'cache')
if not os.path.exists(cache_directory):
    os.makedirs(cache_directory)

def getOrDownloadMetadata(library_to_fetch):
    cache_file_name = "cache/" + library_to_fetch + ".xml"
    if os.path.isfile(cache_file_name):
        with open(cache_file_name, "r") as f:
            return f.read()
    url = "https://dl.google.com/android/maven2/" + library_to_fetch.replace(".", "/").replace(":", "/") + "/maven-metadata.xml"
    r = requests.get(url, allow_redirects=True)
    if not r.ok:
        return None
    with open(cache_file_name, "w") as f:
        f.write(r.text)
    return r.text

def getOrDownloadUpdatedDate(library_to_fetch, version_to_fetch):
    cache_file_name = "cache/" + library_to_fetch + "-" + version_to_fetch + ".txt"
    if os.path.isfile(cache_file_name):
        with open(cache_file_name, "r") as f:
            return f.read()
    artifact_id = library_to_fetch.split(":")[-1]
    url = "https://dl.google.com/android/maven2/" + library_to_fetch.replace(".", "/").replace(":", "/") + "/" + version_to_fetch + "/" + artifact_id + "-" + version_to_fetch + ".pom"
    r = requests.get(url, allow_redirects=True)
    last_updated_pattern = "%a, %d %b %Y %H:%M:%S %Z"
    timestamp = datetime.strptime(r.headers["Last-Modified"], last_updated_pattern).strftime("%Y %m %d")
    with open(cache_file_name, "w") as f:
        f.write(timestamp)
    return timestamp

print("Library,Latest Stable,Latest Stable Release Date,Latest Version,Latest Version Release Date")

for library in libraries:
    metadata = getOrDownloadMetadata(library)
    if metadata is None:
        print(library + ",-,-,-,-")
        continue
    document = parseString(metadata)
    versions = [a.childNodes[0].nodeValue for a in document.getElementsByTagName("version")]
    line = library + ","
    latest_stable_version = None
    for version in reversed(versions):
        if "-" not in version:
            latest_stable_version = version
            break
    if latest_stable_version:
        line += latest_stable_version + "," + getOrDownloadUpdatedDate(library, latest_stable_version) + ","
    else:
        line += "-,-,"
    latest_version = versions[-1]
    line += latest_version + "," + getOrDownloadUpdatedDate(library, latest_version)
    print(line)
