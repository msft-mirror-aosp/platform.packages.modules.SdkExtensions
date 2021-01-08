#!/usr/bin/env python
#
# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""gen_sdk is a command line tool for managing the sdk extension proto db.

Example usages:
# Print a binary representation of the proto database.
$ gen_sdk --action print_binary
"""

import argparse
import google.protobuf.text_format
import pathlib
import sys

from sdk_pb2 import ExtensionVersion
from sdk_pb2 import ExtensionDatabase
from sdk_pb2 import SdkModule
from sdk_pb2 import SdkVersion


def ParseArgs(argv):
  parser = argparse.ArgumentParser('Manage the extension SDK database')
  parser.add_argument(
    '--database',
    type=pathlib.Path,
    metavar='PATH',
    default='extensions_db.textpb',
    help='The existing text-proto database to use. (default: extensions_db.textpb)'
  )
  parser.add_argument(
    '--action',
    choices=['print_binary'],
    metavar='ACTION',
    required=True,
    help='Which action to take (print_binary).'
  )
  return parser.parse_args(argv)


"""Print the binary representation of the db proto to stdout."""
def PrintBinary(database):
  sys.stdout.buffer.write(database.SerializeToString())


def main(argv):
  args = ParseArgs(argv)
  with args.database.open('r') as f:
    database = google.protobuf.text_format.Parse(f.read(), ExtensionDatabase())

  {
    'print_binary': lambda : PrintBinary(database),
  }[args.action]()


if __name__ == '__main__':
  main(sys.argv[1:])
