#!/usr/bin/env python3

import os, fnmatch, re, sys

#### ####
# WIP script to automagically update compose syntax.
#### ####

UI_DIRECTORY = os.path.normpath(os.path.join(os.path.dirname(__file__), "../ui"))

NO_CHILDREN_REGEX = re.compile(r"<(?P<function>[^\/\s>]+) ?(?P<parameters>[^\/>]+?)?[\s]?[\/]>", re.MULTILINE)
# Ensure no character at start to avoid matching generics like List<String>
WITH_CHILDREN_START_REGEX = re.compile(r"(?<!\w)<(?P<function>[^\/\s>]+) ?(?P<parameters>[^\/>]+?)?[\s]?(?P<end>>|(?=<))", re.MULTILINE)
WITH_CHILDREN_END_REGEX = re.compile(r"<\/[^\/\s>]+ ?>", re.MULTILINE)

# Match separate parameters separated by whitespace, unless the whitespace is between
# quotes or lambdas so we can group foo=" bar  " or foo={ bar() } - use DOTALL so we can match
# multi-line lambdas like foo= {
#                                 bar()
#                              }
PARAMETER_REGEX = re.compile(r'[^\s]+?["{].*?[}"]|[^\s]+', re.MULTILINE | re.DOTALL)

# Match any spaces around named parameters or lambdas for removal later to aid
# processing and ensure consistent spacing - we don't want to match whitespace
# here as that will collapse lines.
PARAMETER_SYMBOL_REGEX = re.compile(r"[ ]*([{}=])[ ]*", re.MULTILINE)

def parameter_replace(match, replacement):
  total_match = match.group(0)
  return total_match.replace(match.group("parameter"), replacement)

def no_child_replace(match):
  function = match.group("function")
  parameters = match.group("parameters")
  converted_function = function + "("
  if parameters:
    # Replace " { " with "{" etc
    parameters = re.sub(PARAMETER_SYMBOL_REGEX, r"\1", parameters)
    split_parameters = re.findall(PARAMETER_REGEX, parameters)
    # Add a comma to every parameter except for the last
    for i in range(len(split_parameters)):
      # This character breaks python regex, so if we match it in a comment just ignore it
      if split_parameters[i] == "*":
        continue
      replacement = split_parameters[i]
      # Needs to be a named parameter for now
      if "=" not in replacement:
        replacement += "=" + replacement
      if i != len(split_parameters) - 1:
        replacement += ","
      # Now lets clean up spacing around braces and equals
      replacement = replacement.replace("{", "{ ")
      replacement = replacement.replace("}", " }")
      replacement = replacement.replace("=", " = ")
      # Find anything without characters on either side (we want to match 'text', and not 'textStyle')
      # Then replace only the part without the spaces inside the overall match and return
      parameters = re.sub(r"(^|\s)(?P<parameter>" + re.escape(split_parameters[i]) + r")($|\s)", lambda m: parameter_replace(m, replacement), parameters)
    converted_function = converted_function + parameters
  converted_function = converted_function + ")"
  return converted_function

def with_child_replace(match):
  # Hack to ignore generic parameters: 'fun <T>'
  if match.group(0) == "<T>":
    return match.group(0)
  # If we haven't eaten a '>', it means we matched another '<' on the way, so return without
  # converting the inside
  if not match.group("end"):
    # Return with the inside unconverted
    return match.group("function") + "(" + match.group("parameters")
  converted_function = no_child_replace(match)
  # Remove brackets if there are no parameters
  if converted_function.endswith("()"):
    converted_function = converted_function[:-2]
  converted_function += " {"
  return converted_function

def main(args):
  if len(args) != 2:
    print("Usage: ./development/update_compose_syntax.py <relative/path/to/dir>")
    print("Note: the path starts from the ui folder, i.e dir should look like 'material/integration-tests/material-demos'")
    sys.exit(1)
  directory = args[1]
  directory = os.path.join(UI_DIRECTORY, directory)
  for path, dirs, files in os.walk(directory):
    for filename in fnmatch.filter(files, "*.kt"):
      filepath = os.path.join(path, filename)
      with open(filepath) as f:
        content = f.read()
        # Replace lambdas arrows for now as they mess with tag finding
        content = content.replace("->", "~~~~")
        content = re.sub(NO_CHILDREN_REGEX, no_child_replace, content)
        content = re.sub(WITH_CHILDREN_START_REGEX, with_child_replace, content)
        content = re.sub(WITH_CHILDREN_END_REGEX, "}", content)
        # Hack because if we have nested tags and lambdas inside the parameter we can't fix the closing tag nicely
        content = content.replace("}>", "}) {")
        # Revert change to lambda arrows
        content = content.replace("~~~~", "->")
        with open(filepath, "w") as f:
          f.write(content)

main(sys.argv)
