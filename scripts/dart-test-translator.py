import re
import sys
# import argparse # TODO(ryanmentley): Better arg options/parsing

# ===================================================================
# Whitespace handler
# 
# Handles lines consisting entirely of whitespace (so we don't
# comment them out)
# ===================================================================
ALL_WHITESPACE = re.compile(r"\s*")

def whitespace_handler(line):
  if ALL_WHITESPACE.fullmatch(line):
    return (True, line)
  else:
    return NO_MATCH

# ===================================================================
# Expectation handler
#
# Handles statements generally of the form expect(...)
# ===================================================================
EXPECTATION = re.compile(r"expect\((?P<actual>.+), (?P<expected>[^,]+)\);")
EQUALS_MATCHER = re.compile(r"equals\((?P<value>.+)\)")

def expecation_handler(line):
  match = EXPECTATION.fullmatch(line.strip())
  if match:
    expected = match.group('expected')
    actual = match.group('actual')

    if expected == 'isNull':
      return (True, 'assertThat({actual}).isNull'.format(actual=actual))
    elif expected == 'isTrue':
      return (True, 'assertThat({actual}).isTrue'.format(actual=actual))
    elif expected == 'isFalse':
      return (True, 'assertThat({actual}).isFalse'.format(actual=actual))
    elif EQUALS_MATCHER.fullmatch(expected):
      expected_value = EQUALS_MATCHER.fullmatch(expected).group('value')
      return (True, 'assertThat({expected}).isEqualTo({actual})'.format(expected=expected_value, actual=actual))
    else:
      return (True, 'assertThat({expected}).isEqualTo({actual})'.format(expected=expected, actual=actual))
  return NO_MATCH

# ===================================================================
# Test or group handler
#
# Handles tests or groups of tests
# e.g,
#   group('SemanticsNode', () {
# or
#     test('tagging', () {
# ===================================================================
TEST_OR_GROUP_STATEMENT = re.compile(r"""(?P<type>group|test)\(['"](?P<name>.+)['"], \(\) \{""")

def test_or_group_handler(line):
  match = TEST_OR_GROUP_STATEMENT.fullmatch(line.strip())
  if match:
    stmt_type = match.group('type')
    name = match.group('name')
    if stmt_type == 'group':
      sanitized_name = sanitize_for_class_name(name)
      return (True, [
        '@RunWith(JUnit4::class)',
        'class {name} {{'.format(name=sanitized_name)
      ])
    elif stmt_type == 'test':
      return (True, [
        '@Test',
        'fun `{name}`() {{'.format(name=name)
      ])

  return NO_MATCH

# ===================================================================
# Register all handlers here:
# ===================================================================
HANDLERS = [expecation_handler, whitespace_handler, test_or_group_handler]

# ===================================================================
# Global stuff
# ===================================================================
NO_MATCH = (False, None)
LEADING_WHITESPACE = re.compile(r"(?P<whitespace>\s*)(?P<content>.*)")

def sanitize_for_class_name(name):
  out_name = ''
  for word in name.split(' '):
    word_sanitized = ''
    for idx, char in enumerate(word.capitalize()):
      if (char >= 'a' and char <= 'z') or (char >= 'A' and char <= 'Z'):
        out_name += char
      elif char >= '0' and char <= '9':
        if idx > 0: # Only allowed for non-first char
          out_name += char
  return out_name

def main():
  with open(sys.argv[1]) as f:
    for line in f.readlines():
      ws_result = LEADING_WHITESPACE.fullmatch(line.strip("\n"))
      whitespace = ws_result.group('whitespace')
      content = ws_result.group('content')

      any_handled = False
      for handler in HANDLERS:
        (handled, output) = handler(content)
        if handled:
          if not isinstance(output, list):
            output = [output]
          for output_line in output:
            print(whitespace, output_line)
          any_handled = True
          continue
      if not any_handled:
        print('//', whitespace, content)

if __name__ == '__main__':
  main()
