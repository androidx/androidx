#!/bin/bash
set -e

dremel --output csv < development/pullAndUploadScans/pull.dremel | tail -n +2 | bash
