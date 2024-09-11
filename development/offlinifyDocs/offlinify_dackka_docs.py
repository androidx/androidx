#!/usr/bin/env python3

from argparse import ArgumentParser
import os
from pathlib import Path
from re import search
from shutil import copyfile, rmtree
from sys import exit

"""
Script which takes in Dackka docs and produces offline docs with CSS and relative links.
Run `python3 offlinify_dackka_docs.py --help` for argument descriptions.
"""

SCRIPT_PATH = Path(__file__).parent.absolute()
REL_PATH_TO_DOCS = '../../../../out/androidx/docs-tip-of-tree/build'
DEFAULT_INPUT  = os.path.abspath(os.path.join(SCRIPT_PATH, REL_PATH_TO_DOCS, 'docs'))
DEFAULT_OUTPUT = os.path.abspath(os.path.join(SCRIPT_PATH, REL_PATH_TO_DOCS, 'offlineDocs'))
REL_PATH_TO_LIBRARIES = 'reference/kotlin/androidx'
STYLE_FILENAME = 'style.css'
CSS_SOURCE_PATH = os.path.join(SCRIPT_PATH, STYLE_FILENAME)
PUBLISHED_DOCS_URL = 'https://developer.android.com'
INDEX_PAGES = ['classes.html', 'packages.html']

"""
Check environment and args, then create offline docs.
"""
def main():
  check_env()

  args = parse_args()
  input_path = check_input_path(args.input)
  output_path = check_output_path(args.output)
  library = check_library(args.library, input_path, output_path)

  process_input(input_path, output_path, library)

"""
Error early if any system setup is missing.
"""
def check_env():
  try:
    from bs4 import BeautifulSoup
  except ModuleNotFoundError:
    print('ERROR: This script requires beatifulsoup module `bs4` to run.')
    print('Please install with pip or another package manager.')
    exit(-1)

"""
Parses script args.
"""
def parse_args():
  parser = ArgumentParser(
    description='Converts Dackka docs to an offline version by adding CSS, fixing links, and ' \
        'removing book.yaml templating.'
  )
  parser.add_argument(
    '--input', required=False,
    help='Path to generated Dackka docs. This directory is expected to contain a `reference` ' \
         f'subdirectory. If no path is provided, {DEFAULT_INPUT} is used by default.')
  parser.add_argument(
    '--output', required=False,
    help='Path to store output offline docs. If a directory already exists at this path, it will' \
          f' be deleted. If no path is provided, {DEFAULT_OUTPUT} is used by default.'
  )
  parser.add_argument(
    '--library', required=False,
    help='Specific androidx library to convert docs for. Docs for this library are expected to be' \
          f' in a subdirectory of `{REL_PATH_TO_LIBRARIES}` within the input path. '\
          'If no library is provided, docs for all libraries are converted to offline mode.'
  )
  return parser.parse_args()

"""
Verify the provided input arg is a valid directory.
"""
def check_input_path(path):
  if path is None:
    if not os.path.exists(DEFAULT_INPUT):
      print(f'ERROR: Default input path `{DEFAULT_INPUT}` does not exist. Generate docs by running')
      print('    ./gradlew docs')
      exit(-1)
    return DEFAULT_INPUT

  path = os.path.normpath(path)
  if not os.path.exists(path):
    print(f'ERROR: Provided input path `{path}` does not exist.')
    exit(-1)

  if not os.path.isdir(path):
    print(f'ERROR: Provided input path `{path} does not point to a directory.')
    exit(-1)

  return path

"""
Verifies the output arg by creating a directory at the path, removing existing directory if needed.
"""
def check_output_path(path):
  if path is None:
    path = DEFAULT_OUTPUT

  if os.path.exists(path):
    if os.path.isdir(path):
      print(f'Removing existing directory at output path {path}')
      rmtree(path)
    else:
      print(f'ERROR: output path {path} exists but is not a directory.')
      exit(-1)

  os.makedirs(path)
  return path

"""
Verify the library arg by ensuring the input docs directory exists and making output directories.
"""
def check_library(library, input_path, output_path):
  if library is None:
    return None

  rel_library_path = os.path.join(REL_PATH_TO_LIBRARIES, library)
  input_library_path = os.path.join(input_path, rel_library_path)

  if not os.path.exists(input_library_path):
    print(f'ERROR: Docs directory for library {library} could not be found at')
    print(f'    {input_library_path}')
    exit(-1)

  os.makedirs(os.path.join(output_path, rel_library_path))

  return rel_library_path

"""
Fixes all HTML files in the input directory (or just the pages for the library if a specific one is
provided) and puts the new pages in the output directory.
"""
def process_input(input_path, output_path, library):
  css_path = os.path.join(output_path, STYLE_FILENAME)
  copyfile(CSS_SOURCE_PATH, css_path)

  # Go through just the subdirectory for the specific library, or for the entire input directory.
  path_to_walk = os.path.join(input_path, library) if library is not None else input_path
  for root, dirs, files in os.walk(path_to_walk):
    rel_root = os.path.relpath(root, start=input_path)
    output_root = os.path.join(output_path, rel_root)

    for name in dirs:
      os.mkdir(os.path.join(output_root, name))

    for name in files:
      _, ext = os.path.splitext(name)
      input_file_path = os.path.join(root, name)
      output_file_path = os.path.join(output_root, name)
      rel_css_path = os.path.relpath(css_path, start=output_root)
      if ext == '.html':
        fix_html_file(input_file_path, input_path, output_file_path, rel_css_path, library, False)

  if library is not None:
    # In addition to the library pages, copy over package and class index pages.
    base_output_dir = os.path.join(output_path, REL_PATH_TO_LIBRARIES)
    rel_css_path = os.path.relpath(css_path, start = base_output_dir)
    for file in INDEX_PAGES:
      input_file_path = os.path.join(input_path, REL_PATH_TO_LIBRARIES, file)
      if os.path.exists(input_file_path):
        output_file_path = os.path.join(base_output_dir, file)
        fix_html_file(input_file_path, input_path, output_file_path, rel_css_path, library, True)

"""
Performs all fixes to the input HTML file and saves the resulting HTML at the output path.
"""
def fix_html_file(file_path, root_input_path, output_file_path, css_path, library, index_page):
  from bs4 import BeautifulSoup

  with open(file_path, 'r') as f:
    parsed = BeautifulSoup(f, 'html.parser')

  if index_page:
    filter_index(parsed, library)

  remove_book_template_strings(parsed)
  add_css(parsed, css_path)
  fix_links(parsed, file_path, root_input_path, library)

  with open(output_file_path, 'w') as f:
    f.write(str(parsed))

"""
Removes template strings containing book.yaml information for DAC.
"""
def remove_book_template_strings(page):
  # page.find_all wasn't working here because the template strings are not within HTML tags.
  for element in page.head.contents:
    if search('{%.*%}', element.text):
      element.extract()

"""
Replace any CSS links with a correct link.
"""
def add_css(page, relative_css):
  for tag in page.find_all('link', rel='stylesheet'):
    tag.extract()

  new_tag = page.new_tag('link', rel='stylesheet', href=relative_css)
  page.head.append(new_tag)

"""
Convert links to other pages in the generated docs into relative paths to work offline.
If docs are being converted for just one library, links for docs outside the library are converted
to a link to the published version.
"""
def fix_links(page, page_path, root_input_path, library):
  for a_tag in page.find_all('a'):
    original_path = a_tag.get('href')
    if original_path is None:
      continue
    if not original_path.startswith('/'):
      continue
    lstrip_original_path = original_path.lstrip('/')

    if page_should_be_linked(lstrip_original_path, library):
      abs_path = os.path.join(root_input_path, lstrip_original_path)
      abs_dir = os.path.dirname(abs_path)
      # Make sure the link will work -- this uses the directory because the basename of the path
      # might end with something like `Class.html#function`
      if os.path.exists(abs_dir):
        rel_path = os.path.relpath(abs_path, start=os.path.dirname(page_path))
        a_tag['href'] = rel_path
        continue

    # The link isn't in this library or doesn't exist locally, use the published page.
    a_tag['href'] = PUBLISHED_DOCS_URL + original_path

"""
Determines whether to link to the local version of the page at path.
"""
def page_should_be_linked(path, library):
  # All library docs are generated, so all pages are linked.
  if library is None:
    return True

  # The index pages are the only ones outside of the library dir that will exist.
  if os.path.basename(path) in INDEX_PAGES:
    return True

  # Check if the page is in the library dir.
  common_path = os.path.commonpath([library, path])
  return common_path == library

"""
For the class and package index pages, removes all rows which link outside the library.
"""
def filter_index(page, library):
  for row in page.find_all('tr'):
    link = row.a.get('href')
    common_path = os.path.commonpath([link.lstrip('/'), library])
    if link is not None and common_path != library:
      row.extract()

if __name__ == '__main__':
  main()
