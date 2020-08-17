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

import java.util.Collection;
import java.util.stream.Collectors;

public class ProcessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int exitCode;
  private final Collection<String> args;

  public ProcessException(int exitCode, Collection<String> args) {
    super(buildMessage(exitCode, args));
    this.exitCode = exitCode;
    this.args = args;
  }

  protected ProcessException(String message, int exitCode, Collection<String> args) {
    super(message);
    this.exitCode = exitCode;
    this.args = args;
  }

  private static String buildMessage(int exitCode, Collection<String> args) {
    return "Command " + args.stream().collect(Collectors.joining(" "))
        + " exited with code " + exitCode;
  }

  public int getExitCode() {
    return exitCode;
  }

  public Collection<String> getArgs() {
    return args;
  }
}
