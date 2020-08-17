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

import java.util.Optional;

public class Output {
  private final Optional<String> output;
  private final Optional<String> error;
  private final Optional<Exception> exception;

  Output(Optional<String> output, Optional<String> error,
      Optional<Exception> exception) {
    super();
    this.output = output;
    this.error = error;
    this.exception = exception;
  }

  public Optional<String> output() {
    return output;
  }

  public Optional<String> error() {
    return error;
  }

  public Optional<Exception> exception() {
    return exception;
  }
}
