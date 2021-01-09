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

# Create a new SDK
$ gen_sdk --action new_sdk --sdk 1 --modules=IPSEC,SDK_EXTENSIONS
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
    choices=['print_binary', 'new_sdk'],
    metavar='ACTION',
    required=True,
    help='Which action to take (print_binary|new_sdk).'
  )
  parser.add_argument(
    '--sdk',
    type=int,
    metavar='SDK',
    help='The extension SDK level to deal with (int)'
  )
  parser.add_argument(
    '--modules',
    metavar='MODULES',
    help='Comma-separated list of modules providing new APIs. Required for action=new_sdk.'
  )
  return parser.parse_args(argv)


"""Print the binary representation of the db proto to stdout."""
def PrintBinary(database):
  sys.stdout.buffer.write(database.SerializeToString())


def NewSdk(database, new_version, modules):
  new_requirements = {}

  # Gather the previous highest requirement of each module
  for prev_version in sorted(database.versions, key=lambda v: v.version):
    for prev_requirement in prev_version.requirements:
      new_requirements[prev_requirement.module] = prev_requirement.version

  # Add new requirements of this version
  for module in modules:
    new_requirements[module] = SdkVersion(version=new_version)

  to_proto = lambda m : ExtensionVersion.ModuleRequirement(module=m, version=new_requirements[m])
  module_requirements = [to_proto(m) for m in new_requirements]
  extension_version = ExtensionVersion(version=new_version, requirements=module_requirements)
  database.versions.append(extension_version)

  module_names = ','.join([SdkModule.Name(m) for m in modules])
  print('Created a new extension SDK level %d with modules %s' % (new_version, module_names))


def main(argv):
  args = ParseArgs(argv)
  with args.database.open('r') as f:
    database = google.protobuf.text_format.Parse(f.read(), ExtensionDatabase())

  if args.modules:
    modules = [SdkModule.Value(m) for m in args.modules.split(',')]

  {
    'print_binary': lambda : PrintBinary(database),
    'new_sdk': lambda : NewSdk(database, args.sdk, modules)
  }[args.action]()

  if args.action in ['new_sdk']:
    with args.database.open('w') as f:
      f.write(google.protobuf.text_format.MessageToString(database))

if __name__ == '__main__':
  main(sys.argv[1:])
