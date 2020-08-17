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

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class JdkProcessBuilder extends CustomProcessBuilder<JdkProcess> {

  private final ProcessBuilder processBuilder;

  JdkProcessBuilder(FluentProcessBuilder builder) {
    this.processBuilder = new ProcessBuilder(
        Stream.concat(
            Stream.of(builder.command),
            builder.args.stream()
            ).toArray(String[]::new))
        .redirectError(Redirect.PIPE)
        .redirectInput(Redirect.PIPE)
        .redirectOutput(Redirect.PIPE);
  }

  @Override
  public List<String> command() {
    return new ArrayList<>(processBuilder.command());
  }

  @Override
  public Map<String, String> environment() {
    return new HashMap<>(processBuilder.environment());
  }

  @Override
  public void setEnvironment(Map<String, String> environment) {
    processBuilder.environment().clear();
    processBuilder.environment().putAll(environment);
  }

  @Override
  public Path directory() {
    return processBuilder.directory().toPath();
  }

  @Override
  public ProcessBuilder directory(Path directory) {
    return processBuilder.directory(directory.toFile());
  }

  @Override
  public JdkProcess start() throws IOException {
    return new JdkProcess(this, processBuilder.start());
  }

}
