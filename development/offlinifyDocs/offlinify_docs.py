#!/usr/bin/python3

import argparse
from pathlib import Path
import os
import re
import shutil
import sys
import urllib

SCRIPT_PATH = Path(__file__).parent.absolute()
DEFAULT_DIR  = os.path.abspath(os.path.join(SCRIPT_PATH, '../../../../out/androidx/docs-tip-of-tree/build/javadoc'))

STYLE_FNAME = 'style.css'
CSS_SOURCE_PATH = os.path.abspath(os.path.join(SCRIPT_PATH, STYLE_FNAME))

VERBOSE = False

def check_env():
  """
  Error early if any system setup is missing
  """
  try:
    from bs4 import BeautifulSoup
  except ModuleNotFoundError:
    print("ERROR: This script requires beatifulsoup module `bs4` to run. Please install with pip or another package manager.")
    sys.exit(-1)

def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument('--path', '-p', required=False)
  parser.add_argument('--quiet', '-q', required=False, action="store_true")
  parser.set_defaults(format=False)
  args = parser.parse_args()
  global VERBOSE
  VERBOSE = not args.quiet # just update the global, sorry
  return args

def log(msg, end = '\n', flush=False):
  if (VERBOSE):
    print(msg, end=end, flush=flush)

def sanitize_destination(fpath):
  """
  Ensure that destination always points to a Javadoc folder as this is the main param to script
  """
  if fpath is None:
    fpath = DEFAULT_DIR
    if (os.path.isdir(fpath)):
        return fpath
    else:
        print("Unable to find javadoc directory, ensure it's been created by running")
        print("    ./gradlew doclavaDocs -PofflineDocs=true")
        sys.exit(-1)

  # convert files to directories
  if (os.path.isfile(fpath)):
    result = Path(fpath).parent.absolute()
    log(f"Provided path: {fpath}")
    log(f"Using directory: {result}")
    fpath = result

  if (os.path.isdir(fpath)):
    if VERBOSE:
      print(f"Confirm that directory \033[4m{os.path.abspath(fpath)}\033[0m points to the root of generated javadoc files [Y/N]:", end=' ', flush=True)
      result = sys.stdin.readline().rstrip()
      valid_responses = ['Y', 'N']
      while result.upper() not in valid_responses:
        print("Please enter [Y/N]:", end=' ', flush=True)
        result = sys.stdin.readline().rstrip()
      if result.upper() == "N":
        sys.exit(-1)
    return os.path.abspath(fpath)
  else:
    print(f"Invalid path {fpath}, please specify the generated javadoc root directory")
    sys.exit(-1)

def copy_css_to_root(javadocroot):
  """
  Drop our style sheet into the root dir.
  """
  log(f"Copying {os.path.relpath(CSS_SOURCE_PATH)}", end = " to ", flush=True)
  dest_path = os.path.join(javadocroot, STYLE_FNAME)
  shutil.copy(CSS_SOURCE_PATH, dest_path)
  log(f"{os.path.relpath(dest_path)} ✅")
  return dest_path

def fix_css(soup, relative_css):
  """
  Replace any css links with a correct link
  """
  for tag in soup.find_all("link", rel="stylesheet"):
    tag.extract()

  new_tag = soup.new_tag("link", rel="stylesheet", href=relative_css)
  soup.head.append(new_tag)

def fix_links(soup, rootdir, file_loc, last_root):
  """
  Fix any in-javadoc links to be relative instead of absolute so they can be opened from the filesystem.
  """
  for atag in soup.find_all('a'):
    generated_path = atag.get('href')
    if generated_path is None:
      continue

    parsed_url = urllib.parse.urlparse(generated_path.lstrip('/'))
    non_root_generated = parsed_url.path

    # see if we can just fix it quick
    if last_root is not None and os.path.isfile(os.path.join(last_root, non_root_generated)):
      new_path = generate_relative_link(os.path.join(last_root, non_root_generated), file_loc)
      atag['href'] = urllib.parse.urlunparse(parsed_url._replace(path=new_path))
      continue
    else:
      # ¯\_(ツ)_/¯
      # walk back from file_loc to rootdir and try to append generated_path
      # this will catch situations where rootdir is above the expected path
      current_path = Path(file_loc).parent.absolute()
      last_current_path = None
      while current_path != last_current_path: # if there's a better way to detect root swap it
        test_path = os.path.join(current_path.as_posix(), non_root_generated)
        if os.path.isfile(test_path):
          last_root = current_path.as_posix()
          new_path = generate_relative_link(test_path, file_loc)
          new_path_abs = Path(file_loc).parent.joinpath(new_path).as_posix()
          if (os.path.commonprefix([rootdir, new_path_abs]) == rootdir):
            # if the found file is inside the rootdir, we'll update the url to work in the browser
            atag['href'] = urllib.parse.urlunparse(parsed_url._replace(path=new_path))
          else:
            log(f"not updating path {generated_path} to {new_path} because it points above {rootdir}")
          break
        else:
          last_current_path = current_path
          current_path = Path(current_path).parent
  return last_root

def generate_relative_link(destination, source):
  """
  Generate a relative link in a form that a web browser likes
  """
  absdest = os.path.abspath(destination)
  abssource = os.path.abspath(source)
  if os.path.isfile(abssource):
    abssource = Path(abssource).parent.absolute()
  return os.path.relpath(absdest, start=abssource)

def fix_html(javadocroot):
  """
  Inject css link and fix all <a href to work on the local file system for all files under javadocroot
  """
  from bs4 import BeautifulSoup

  css_path = copy_css_to_root(javadocroot)
  last_relative_root = None
  for html_file in list(Path(javadocroot).glob('**/*.html')):
    relative_css_path = os.path.relpath(css_path, Path(html_file).parent)
    with html_file.open() as fd:
      parsed_html = BeautifulSoup(fd, "html.parser")

    fix_css(parsed_html, relative_css_path)
    last_relative_root = fix_links(parsed_html, javadocroot, html_file, last_relative_root)

    # replace the file
    html_file.write_text(str(parsed_html))
    log(f"{os.path.relpath(html_file)} ✅", flush=False)

def main(args=None):
  check_env()
  args = parse_args()
  javadocpath = sanitize_destination(args.path)
  log(f"Javadoc root path: {os.path.relpath(javadocpath)} ✅")
  fix_html(javadocpath)

if __name__ == '__main__':
  main()
