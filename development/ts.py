#!/usr/bin/env python3
# This program reads stdin, prepends the current time to each line, and prints the result

from datetime import datetime
import sys

for line in sys.stdin:
    now = datetime.now()
    print(str(now) + " " + line, end="")
