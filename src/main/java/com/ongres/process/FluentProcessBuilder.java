/*-
 *  § 
 * docker-junit-extension
 *    
 * Copyright (C) 2019 - 2020 OnGres, Inc.
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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FluentProcessBuilder {

  final String command;
  Supplier<CustomProcessBuilder<?>> processBuilderSupplier =
      () -> new JdkProcessBuilder(this);
  List<String> args = new ArrayList<>();
  Map<Integer, Integer> outputs = new HashMap<>(
      Stream.of(
          FluentProcess.STDOUT,
          FluentProcess.STDERR)
      .collect(Collectors.toMap(fd -> fd, fd -> fd)));
  Path workPath = null;
  Map<String, String> environment = new HashMap<>(System.getenv());
  boolean closeAfterLast = true;
  Set<Integer> allowedExitCodes = new HashSet<>(
      Stream.of(0).collect(Collectors.toList()));
  Duration timeout = null;

  /**
   * Create a builder for specified command.
   */
  public FluentProcessBuilder(String command) {
    this.command = command;
  }

  /**
   * Overwrite command arguments.
   */
  public FluentProcessBuilder args(List<String> args) {
    this.args.clear();
    this.args.addAll(args);
    return this;
  }

  /**
   * Overwrite command arguments.
   */
  public FluentProcessBuilder args(String...args) {
    args(Arrays.asList(args));
    return this;
  }

  /**
   * Add a command argument.
   */
  public FluentProcessBuilder arg(String arg) {
    this.args.add(arg);
    return this;
  }

  /**
   * Add a multiline command argument.
   */
  public FluentProcessBuilder multilineArg(String...lines) {
    return multilineArg(Arrays.asList(lines).stream());
  }

  /**
   * Add a multiline command argument.
   */
  public FluentProcessBuilder multilineArg(Collection<String> lines) {
    return multilineArg(lines.stream());
  }

  /**
   * Add a multiline command argument.
   */
  public FluentProcessBuilder multilineArg(Stream<String> lines) {
    this.args.add(lines.collect(Collectors.joining(FluentProcess.NEWLINE_DELIMITER)));
    return this;
  }

  /**
   * Set the command working path.
   */
  public FluentProcessBuilder workPath(Path workPath) {
    this.workPath = workPath;
    return this;
  }

  /**
   * Remove environment variables inherited by the JVM.
   */
  public FluentProcessBuilder clearEnvironment() {
    this.environment.clear();
    return this;
  }

  /**
   * Overwrite the command environment variables.
   */
  public FluentProcessBuilder environment(Map<String, String> environment) {
    this.environment.putAll(environment);
    return this;
  }

  /**
   * Set an environment variable for the command.
   */
  public FluentProcessBuilder environment(String name, String value) {
    this.environment.put(name, value);
    return this;
  }

  /**
   * When specified will cause the stream of a failed process to throw exception only when
   *  specifically closing it.
   * <p>
   * By default a failed process will throw an exception when beyond the last element.
   * </p>
   */
  public FluentProcessBuilder dontCloseAfterLast() {
    this.closeAfterLast = false;
    return this;
  }

  /**
   * Collection of allowed exit code values that will be considered as successful exit codes for the
   *  command.
   * <p>
   * Warning: overrides the default value that considers 0 as a successful exit code.
   * </p>
   */
  public FluentProcessBuilder allowedExitCodes(Collection<Integer> exitCodes) {
    this.allowedExitCodes.clear();
    this.allowedExitCodes.addAll(exitCodes);
    return this;
  }

  public FluentProcessBuilder withAllowedExitCodes(int...exitCodes) {
    this.allowedExitCodes.addAll(Arrays.stream(exitCodes)
        .boxed().collect(Collectors.toList()));
    return this;
  }

  /**
   * Add an allowed exit code that will be considered a successful exit code for the command.
   */
  public FluentProcessBuilder allowedExitCode(int exitCode) {
    this.allowedExitCodes.add(exitCode);
    return this;
  }

  /**
   * Redirect stdout to stderr.
   */
  public FluentProcessBuilder redirectStdoutToStderr() {
    this.outputs.put(FluentProcess.STDOUT, FluentProcess.STDERR);
    return this;
  }

  /**
   * Redirect stderr to stdout.
   */
  public FluentProcessBuilder redirectStderrToStdout() {
    this.outputs.put(FluentProcess.STDERR, FluentProcess.STDOUT);
    return this;
  }

  /**
   * Do not output stdout.
   */
  public FluentProcessBuilder noStdout() {
    this.outputs.remove(FluentProcess.STDOUT);
    return this;
  }

  /**
   * Do not output stderr.
   */
  public FluentProcessBuilder noStderr() {
    this.outputs.remove(FluentProcess.STDERR);
    return this;
  }

  /**
   * Start the process (in background) and return a {@code FluentProcess} instance wrapping the
   *  running process.
   */
  public FluentProcess start() {
    try {
      CustomProcessBuilder<?> builder = processBuilderSupplier.get();
      if (workPath != null) {
        builder.directory(workPath);
      }
      builder.setEnvironment(environment);
      CustomProcess process = builder.start();
      Instant start = Instant.now();
      return new FluentProcess(this, builder, process, start);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
