/*-
 *  § 
 * fluent-process
 *    
 * Copyright (C) 2020 OnGres, Inc.
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * § §
 */

package com.ongres.process;

import java.nio.charset.StandardCharsets;

public class OutputData {

  private final Integer fd;
  private final byte[] bytes;

  OutputData(Integer fd, byte[] bytes) {
    this.fd = fd;
    this.bytes = bytes;
  }

  public Integer fd() {
    return fd;
  }

  public byte[] bytes() {
    return bytes;
  }

  public boolean isStdout() {
    return fd == FluentProcess.STDOUT;
  }

  public boolean isStderr() {
    return fd == FluentProcess.STDERR;
  }

  @Override
  public String toString() {
    try {
      return fd + ":" + new String(bytes, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      return fd + ":" + ex.getMessage();
    }
  }
}
