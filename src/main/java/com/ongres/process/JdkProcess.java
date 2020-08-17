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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JdkProcess extends CustomProcess {

  private final JdkProcessBuilder processBuilder;
  private final Process process;

  JdkProcess(JdkProcessBuilder processBuilder, Process process) {
    this.processBuilder = processBuilder;
    this.process = process;
  }

  @Override
  public List<String> command() {
    return processBuilder.command();
  }

  @Override
  public Map<String, String> environment() {
    return processBuilder.environment();
  }

  @Override
  public Path directory() {
    return processBuilder.directory();
  }

  @Override
  public OutputStream getOutputStream() {
    return process.getOutputStream();
  }

  @Override
  public InputStream getInputStream() {
    return process.getInputStream();
  }

  @Override
  public InputStream getErrorStream() {
    return process.getErrorStream();
  }

  @Override
  public int exitValue() {
    return process.exitValue();
  }

  @Override
  public Process destroyForcibly() {
    return process.destroyForcibly();
  }

  @Override
  public int waitFor() throws InterruptedException {
    return process.waitFor();
  }

  @Override
  public boolean isAlive() {
    return process.isAlive();
  }

  public static Process asProcess(FluentProcess process) {
    if (process.getProcess() instanceof JdkProcess) {
      return JdkProcess.class.cast(process.getProcess()).process;
    }
    throw new IllegalArgumentException(FluentProcess.class.getSimpleName() + " is using "
        + process.getProcess().getClass().getSimpleName());
  }
}
