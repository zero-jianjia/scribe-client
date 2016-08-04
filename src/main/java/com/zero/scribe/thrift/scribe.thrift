#!/usr/local/bin/thrift --gen cpp:pure_enums --gen php

##  Copyright (c) 2007-2008 Facebook
##
##  Licensed under the Apache License, Version 2.0 (the "License");
##  you may not use this file except in compliance with the License.
##  You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
##  Unless required by applicable law or agreed to in writing, software
##  distributed under the License is distributed on an "AS IS" BASIS,
##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
##  See the License for the specific language governing permissions and
##  limitations under the License.
##
## See accompanying file LICENSE or visit the Scribe site at:
## http://developers.facebook.com/scribe/

include "thrift-0.9.3\\contrib\\fb303\\if\\fb303.thrift"

namespace java com.sina.scribe.core

enum ResultCode {
  SUCCESS,
  TRY_LATER
}

struct MessageEntry {
  1:  string category,
  2:  binary content
}

service Scribe extends fb303.FacebookService {
  # the method name must be Log!!!
  ResultCode Log(1: list<MessageEntry> msgs);
}