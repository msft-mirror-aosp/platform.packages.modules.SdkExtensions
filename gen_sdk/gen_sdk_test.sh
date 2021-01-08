#!/bin/bash -e
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

# Verifies the tool correctly prints the binary representation of a db.
function test_print_binary() {
  # Golden binary rep generated with:
  # $ gqui from textproto:testdata/test_extensions_db.textpb \
  #        proto extensions_db.proto:ExtensionDatabase \
  #        --outfile rawproto:- | xxd -p
  cat > golden_binary << EOF
0a0a080112060803120208010a1a08021206080312020801120608021202
080212060801120208020a1a080312060803120208031206080212020802
12060801120208020a220804120608031202080312060802120208021206
0801120208041206080512020804
EOF

  diff golden_binary <(gen_sdk --action print_binary --database testdata/test_extensions_db.textpb | xxd -p)
}
test_print_binary
