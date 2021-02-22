#!/usr/bin/python3

import argparse
from pathlib import Path
import re
import shutil
import sys


REPLACEMENT_REGEX = r'^\{\% setvar book_path.*\%\}\n\{\%.*\%\}$'
STYLE_TEMPLATE = '<link rel="stylesheet" href="%s">'


def relative_style(html_file):
  return '../' * str(html_file).count('/') + 'style.css'


def main(args=None):
  parser = argparse.ArgumentParser()
  parser.add_argument('--path', '-p', required=True)
  parser.set_defaults(format=False)
  args = parser.parse_args()
  # copy style.css
  shutil.copyfile(sys.path[0] + '/style.css', args.path + 'style.css')
  # rewrite html files to include stylesheet link
  root = Path(args.path)
  regex = re.compile(REPLACEMENT_REGEX, flags=re.MULTILINE)
  for html_file in list(root.glob('**/*.html')):
    relative_html_file = html_file.relative_to(root)
    content = html_file.read_text()
    style_link = STYLE_TEMPLATE % relative_style(relative_html_file)
    html_file.write_text(regex.sub(style_link, content))

if __name__ == '__main__':
  main()
