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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FluentProcess implements AutoCloseable {

  private static final ByteArrayInputStream EMPTY_INPUT_STREAM =
      new ByteArrayInputStream(new byte[0]);

  public static final int STDERR = 2;
  public static final int STDOUT = 1;

  public static final String NEWLINE_DELIMITER = "\n";

  /**
   * Build a process from a command with (optional) arguments.
   */
  public static FluentProcessBuilder builder(String command, String...args) {
    return new FluentProcessBuilder(command)
        .args(args);
  }

  /**
   * Start a process from a command with (optional) arguments.
   */
  public static FluentProcess start(String command, String...args) {
    return new FluentProcessBuilder(command)
        .args(args)
        .start();
  }

  private final CustomProcessBuilder<?> processBuilder;
  private final CustomProcess process;
  private final Instant start;
  private final Set<Integer> allowedExitCodes;
  private final Duration timeout;
  private final boolean closeAfterLast;
  private final Map<Integer, Integer> outputs;
  private final ArrayList<Closeable> closeables;
  private final Optional<Instant> end;

  public FluentProcess(FluentProcessBuilder builder,
      CustomProcessBuilder<?> processBuilder,
      CustomProcess process,
      Instant start) {
    this.processBuilder = processBuilder;
    this.process = process;
    this.start = start;
    this.allowedExitCodes = builder.allowedExitCodes;
    this.timeout = builder.timeout;
    this.closeAfterLast = builder.closeAfterLast;
    this.outputs = builder.outputs;
    this.closeables = new ArrayList<>();
    registerCloseable(process.getOutputStream());
    registerCloseable(process.getInputStream());
    registerCloseable(process.getErrorStream());
    this.end = Optional.ofNullable(start)
        .flatMap(s -> Optional.ofNullable(timeout).map(t -> s.plus(t)));
  }

  private FluentProcess(FluentProcess parent, Set<Integer> allowedExitCodes) {
    this.processBuilder = parent.processBuilder;
    this.process = parent.process;
    this.start = parent.start;
    this.allowedExitCodes = allowedExitCodes;
    this.timeout = parent.timeout;
    this.closeAfterLast = parent.closeAfterLast;
    this.outputs = parent.outputs;
    this.closeables = parent.closeables;
    this.end = Optional.ofNullable(start)
        .flatMap(s -> Optional.ofNullable(timeout).map(t -> s.plus(t)));
  }

  private FluentProcess(FluentProcess parent, Duration timeout) {
    this.processBuilder = parent.processBuilder;
    this.process = parent.process;
    this.start = parent.start;
    this.allowedExitCodes = parent.allowedExitCodes;
    this.timeout = timeout;
    this.closeAfterLast = parent.closeAfterLast;
    this.outputs = parent.outputs;
    this.closeables = parent.closeables;
    this.end = Optional.ofNullable(start)
        .flatMap(s -> Optional.ofNullable(timeout).map(t -> s.plus(t)));
  }

  private FluentProcess(FluentProcess parent, boolean closeAfterLast) {
    this.processBuilder = parent.processBuilder;
    this.process = parent.process;
    this.start = parent.start;
    this.allowedExitCodes = parent.allowedExitCodes;
    this.timeout = parent.timeout;
    this.closeAfterLast = closeAfterLast;
    this.outputs = parent.outputs;
    this.closeables = parent.closeables;
    this.end = Optional.ofNullable(start)
        .flatMap(s -> Optional.ofNullable(timeout).map(t -> s.plus(t)));
  }

  private FluentProcess(FluentProcess parent, Map<Integer, Integer> outputs) {
    this.processBuilder = parent.processBuilder;
    this.process = parent.process;
    this.start = parent.start;
    this.allowedExitCodes = parent.allowedExitCodes;
    this.timeout = parent.timeout;
    this.closeAfterLast = parent.closeAfterLast;
    this.outputs = outputs;
    this.closeables = parent.closeables;
    this.end = Optional.ofNullable(start)
        .flatMap(s -> Optional.ofNullable(timeout).map(t -> s.plus(t)));
  }

  /**
   * Return a {@code FluentProcess} that allow only exit code 0.
   */
  public FluentProcess withoutAllowedExitCodes() {
    return new FluentProcess(this, new HashSet<>(Stream.of(0)
        .collect(Collectors.toSet())));
  }

  /**
   * Return a {@code FluentProcess} with a set of allowed exit code values that will be considered
   *  as successful exit codes for the command.
   * <p>
   * Warning: overrides the default value that considers 0 as a successful exit code.
   * </p>
   */
  public FluentProcess withAllowedExitCodes(Set<Integer> exitCodes) {
    return new FluentProcess(this, new HashSet<>(exitCodes));
  }

  /**
   * Return a {@code FluentProcess} adding specified allowed exit codes that will be considered as
   *  successful exit codes for the command.
   */
  public FluentProcess withAllowedExitCodes(int...exitCodes) {
    Set<Integer> allowedExitCodes = new HashSet<>(this.allowedExitCodes);
    allowedExitCodes.addAll(Arrays.stream(exitCodes)
        .boxed().collect(Collectors.toList()));
    return new FluentProcess(this, allowedExitCodes);
  }

  /**
   * Return a {@code FluentProcess} adding an allowed exit code that will be considered a
   *  successful exit code for the command.
   */
  public FluentProcess withAllowedExitCode(int exitCode) {
    Set<Integer> allowedExitCodes = new HashSet<>(this.allowedExitCodes);
    allowedExitCodes.add(exitCode);
    return new FluentProcess(this, allowedExitCodes);
  }

  /**
   * Return a {@code FluentProcess} that does not have a timeout.
   */
  public FluentProcess withoutTimeout() {
    return new FluentProcess(this, (Duration) null);
  }

  /**
   * Return a {@code FluentProcess} that fails alter specified timeout.
   */
  public FluentProcess withTimeout(Duration timeout) {
    return new FluentProcess(this, timeout);
  }

  /**
   * Return a {@code FluentProcess} that throw exception only when closed.
   * <p>
   * By default a failed process will throw an exception when beyond the last element.
   * </p>
   */
  public FluentProcess withoutCloseAfterLast() {
    return new FluentProcess(this, false);
  }

  /**
   * Return a {@code FluentProcess} that throw exception only when beyond the last element.
   * <p>
   * This is the default.
   * </p>
   */
  public FluentProcess withCloseAfterLast() {
    return new FluentProcess(this, true);
  }

  /**
   * Redirect stdout to stderr.
   */
  public FluentProcess withStdoutToStderr() {
    Map<Integer, Integer> outputs = new HashMap<>(this.outputs);
    outputs.put(FluentProcess.STDOUT, FluentProcess.STDERR);
    return new FluentProcess(this, outputs);
  }

  /**
   * Redirect stderr to stdout.
   */
  public FluentProcess withStderrToStdout() {
    Map<Integer, Integer> outputs = new HashMap<>(this.outputs);
    outputs.put(FluentProcess.STDERR, FluentProcess.STDOUT);
    return new FluentProcess(this, outputs);
  }

  /**
   * Do not output stdout.
   */
  public FluentProcess withoutStdout() {
    Map<Integer, Integer> outputs = new HashMap<>(this.outputs);
    outputs.remove(FluentProcess.STDOUT);
    return new FluentProcess(this, outputs);
  }

  /**
   * Do not output stderr.
   */
  public FluentProcess withoutStderr() {
    Map<Integer, Integer> outputs = new HashMap<>(this.outputs);
    outputs.remove(FluentProcess.STDERR);
    return new FluentProcess(this, outputs);
  }

  /**
   * Stream process output line by line and throws an Exception if the process fails.
   */
  public Stream<String> stream() {
    return streamOutputLines().map(OutputLine::line);
  }

  /**
   * Stream process stdout line by line and throws an Exception if the process fails.
   */
  public Stream<String> streamStdout() {
    return streamOutputLines().filter(OutputLine::isStdout).map(OutputLine::line);
  }

  /**
   * Stream process stderr line by line and throws an Exception if the process fails.
   */
  public Stream<String> streamStderr() {
    return streamOutputLines().filter(OutputLine::isStderr).map(OutputLine::line);
  }

  /**
   * Stream process output line by line and throws an Exception if the process fails.
   */
  public Stream<OutputLine> streamOutputLines() {
    Map<Integer, ProcessOutputInputStream> processOutputDataInputStreams =
        streamInputs()
            .collect(Collectors.toMap(Map.Entry::getKey,
                this::createProcessOutputInputStreamEntry));
    ProcessOutputLineIterator processStreamIterator = new ProcessOutputLineIterator(
        processOutputDataInputStreams, closeAfterLast);
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(processStreamIterator, Spliterator.ORDERED),
        false)
        .onClose(() -> {
          try {
            processStreamIterator.close();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
  }

  private ProcessOutputInputStream createProcessOutputInputStreamEntry(
      SimpleEntry<Integer, InputStream> inputStreamForOutput) {
    return new ProcessOutputInputStream(this, inputStreamForOutput.getValue());
  }

  /**
   * Stream process output data and throws an Exception if the process fails.
   */
  public Stream<OutputData> streamOutputData() {
    ProcessOutputDataIterator processOutputIterator =
        createProcessOutputIteratorEntry(process, start,
            streamInputs().collect(Collectors.toList()));
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(processOutputIterator, Spliterator.ORDERED),
        false)
        .onClose(() -> {
          try {
            processOutputIterator.close();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
  }

  private Stream<SimpleEntry<Integer, InputStream>> streamInputs() {
    return Stream.of(
        new SimpleEntry<>(outputs.compute(STDOUT, (key, value) -> key), process.getInputStream()),
        new SimpleEntry<>(outputs.compute(STDERR, (key, value) -> key), process.getErrorStream()))
        .filter(e -> outputs.containsKey(e.getKey()));
  }

  /**
   * Stream process byte arrays and throws an Exception if the process fails.
   */
  public Stream<byte[]> streamBytes() {
    return streamOutputData().map(OutputData::bytes);
  }

  /**
   * Stream process stdout and throws an Exception if the process fails.
   */
  public Stream<byte[]> streamStdoutBytes() {
    return streamOutputData().filter(OutputData::isStdout).map(OutputData::bytes);
  }

  /**
   * Stream process stderr and throws an Exception if the process fails.
   */
  public Stream<byte[]> streamStderrBytes() {
    return streamOutputData().filter(OutputData::isStderr).map(OutputData::bytes);
  }

  private ProcessOutputDataIterator createProcessOutputIteratorEntry(
      CustomProcess process, Instant start,
      List<SimpleEntry<Integer, InputStream>> inputStreamForOutputList) {
    final Map<Integer, InputStream> inputStreamForOutputs = inputStreamForOutputList.stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new ProcessOutputDataIterator(this, closeAfterLast, inputStreamForOutputs);
  }

  /**
   * Just run the command discarding the output and return true if process did not
   * fail.
   */
  public boolean isSuccessful() {
    try (Stream<?> stream = streamOutputData()) {
      return stream.allMatch(data -> true);
    } catch (RuntimeException ex) {
      return false;
    }
  }

  /**
   * Just run the command discarding the output.
   */
  public void join() {
    try (Stream<?> stream = streamOutputData()) {
      stream.allMatch(data -> true);
    }
  }

  /**
   * Return the process output.
   * <p>
   * This function does not throw an exception if command fail, it rather wrap the
   * exception inside the {@code Output} object.
   * </p>
   */
  public Output tryGet() {
    return tryGet(NEWLINE_DELIMITER);
  }

  /**
   * Return the process output lines delimited by specified character.
   * <p>
   * This function does not throw an exception if command fail, it rather wrap the
   * exception inside the {@code Output} object.
   * </p>
   */
  public Output tryGet(CharSequence delimiter) {
    try {
      Stream<OutputLine> stream = streamOutputLines();
      Optional<Map<Integer, String>> output = Optional.of(
          stream
          .reduce(new HashMap<Integer, StringJoiner>(
              Stream.of(
                  new SimpleEntry<>(STDOUT, new StringJoiner(delimiter)),
                  new SimpleEntry<>(STDERR, new StringJoiner(delimiter)))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
              (outputBuilder, line) -> {
                outputBuilder.get(line.fd).add(line.line);
                return outputBuilder;
              },
              (u, v) -> v))
          .map(outputBuilder -> outputBuilder.entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
      try {
        stream.close();
      } catch (Exception ex) {
        return new Output(
            output.map(o -> o.get(STDOUT)),
            output.map(o -> o.get(STDERR)),
            Optional.of(ex));
      }
      return new Output(
          output.map(o -> o.get(STDOUT)),
          output.map(o -> o.get(STDERR)),
          Optional.empty());
    } catch (Exception ex) {
      return new Output(
          Optional.empty(),
          Optional.empty(),
          Optional.of(ex));
    }
  }

  /**
   * Return the process output.
   */
  public String get() {
    return get(NEWLINE_DELIMITER);
  }

  /**
   * Return the process lines delimited by specified character.
   */
  public String get(CharSequence delimiter) {
    return stream().collect(Collectors.joining(delimiter));
  }

  /**
   * Return an {@code InputStream} with the process stdout.
   * <p>
   * Closing the {@code InputStream} also destroy the process.
   * </P>
   */
  public InputStream asInputStream() {
    final InputStream stdoutInputStream = streamInputs()
        .filter(e -> e.getKey() == STDOUT)
        .map(Map.Entry::getValue)
        .findAny()
        .orElse(EMPTY_INPUT_STREAM);
    return new ProcessOutputInputStream(this, stdoutInputStream);
  }

  /**
   * Write content of an {@code Stream<byte[]>} to the process stdin.
   */
  public void writeBytesToStdin(Stream<byte[]> stream) {
    writeToStdin(new ByteArrayStreamInputStream(stream));
  }

  /**
   * Write content of an {@code Stream<String>} to the process stdin.
   */
  public void writeToStdin(Stream<String> stream) {
    writeToStdin(new LineStreamInputStream(stream));
  }

  /**
   * Write content of an {@code InputStream} to the process stdin.
   */
  public void writeToStdin(InputStream inputStream) {
    writeToStdin(inputStream, true);
  }

  private void writeToStdin(InputStream inputStream, boolean closeProcess) {
    try {
      OutputStream outputStream = process.getOutputStream();
      try {
        byte[] inputBuffer = new byte[8192];
        while (true) {
          int value = inputStream.read(inputBuffer);
          if (value >= 0) {
            if (value > 0) {
              outputStream.write(inputBuffer, 0, value);
            }
          } else {
            break;
          }
        }
      } finally {
        outputStream.close();
        if (closeProcess) {
          process.waitFor();
          close();
        }
      }
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Write process stdout to an {@code OutputStream}.
   */
  public void writeToOutputStream(OutputStream outputStream) {
    try {
      registerCloseable(outputStream);
      try {
        InputStream inputStream = streamInputs().filter(e -> e.getKey() == STDOUT)
            .map(Map.Entry::getValue).findAny().orElse(null);
        if (inputStream != null) {
          byte[] inputBuffer = new byte[8192];
          while (true) {
            int value = inputStream.read(inputBuffer);
            if (value >= 0) {
              if (value > 0) {
                outputStream.write(inputBuffer, 0, value);
              }
            } else {
              break;
            }
          }
        }
      } finally {
        process.waitFor();
        close();
      }
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Useful method to transform this object using a lambda.
   * <p>
   * Can be used, for instance, to retrieve the original process with:
   * <pre>
   * java.lang.Process process = fluentProcess.as(JdkProcess::asProcess);
   * </pre>
   * </p>
   */
  public <T> T as(Function<FluentProcess, T> transformer) {
    return transformer.apply(this);
  }

  /**
   * Pipe this process with supplied process.
   * <p>
   * This method create a thread using the default {@code CompletableFuture} {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess pipe(FluentProcess fluentProcess) {
    InputStream inputStream = new ByteArrayStreamInputStream(
        withoutCloseAfterLast().streamStdoutBytes());
    registerCloseable(inputStream);
    runAsyncWithStdin(fluentProcess, inputStream, this::close, null);
    return fluentProcess;
  }

  /**
   * Pipe this process with supplied process.
   * <p>
   * This method create a thread using the specified {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess pipe(FluentProcess fluentProcess, Executor executor) {
    InputStream inputStream = new ByteArrayStreamInputStream(
        withoutCloseAfterLast().streamStdoutBytes());
    registerCloseable(inputStream);
    runAsyncWithStdin(fluentProcess, inputStream, this::close, executor);
    return fluentProcess;
  }

  /**
   * Pipe this process stdout with process started from specified command.
   * <p>
   * This method create a thread using the default {@code CompletableFuture} {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess pipe(String command, String...args) {
    InputStream inputStream = new ByteArrayStreamInputStream(
        withoutCloseAfterLast().streamStdoutBytes());
    registerCloseable(inputStream);
    FluentProcess fluentProcess = new FluentProcessBuilder(command)
        .args(args)
        .start();
    runAsyncWithStdin(fluentProcess, inputStream, this::close, null);
    return fluentProcess;
  }

  /**
   * Write an {@code InputStream} to this process stdout.
   * <p>
   * This method create a thread using the default {@code CompletableFuture} {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStream(InputStream inputStream) {
    runAsyncWithStdin(this, inputStream, inputStream::close, null);
    return this;
  }

  /**
   * Write an {@code InputStream} to this process stdout.
   * <p>
   * This method create a thread using the specified {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStream(InputStream inputStream, Executor executor) {
    runAsyncWithStdin(this, inputStream, inputStream::close, executor);
    return this;
  }

  /**
   * Write a {@code Stream<String>} to this process stdout.
   * <p>
   * This method create a thread using the default {@code CompletableFuture} {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStream(Stream<String> stream) {
    InputStream inputStream = new LineStreamInputStream(stream);
    runAsyncWithStdin(this, inputStream, inputStream::close, null);
    return this;
  }

  /**
   * Write a {@code Stream<String>} to this process stdout.
   * <p>
   * This method create a thread using the specified {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStream(Stream<String> stream, Executor executor) {
    InputStream inputStream = new LineStreamInputStream(stream);
    runAsyncWithStdin(this, inputStream, inputStream::close, executor);
    return this;
  }

  /**
   * Write a {@code Stream<byte[]>} to this process stdout.
   * <p>
   * This method create a thread using the default {@code CompletableFuture} {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStreamOfBytes(Stream<byte[]> stream) {
    InputStream inputStream = new ByteArrayStreamInputStream(stream);
    runAsyncWithStdin(this, inputStream, inputStream::close, null);
    return this;
  }

  /**
   * Write a {@code Stream<byte[]>} to this process stdout.
   * <p>
   * This method create a thread using the specified {@code Executor}.
   * The thread will be closed when the process exits.
   * </p>
   */
  public FluentProcess inputStreamOfBytes(Stream<byte[]> stream, Executor executor) {
    InputStream inputStream = new ByteArrayStreamInputStream(stream);
    runAsyncWithStdin(this, inputStream, inputStream::close, executor);
    return this;
  }

  private void runAsyncWithStdin(FluentProcess fluentProcess,
      InputStream inputStream, AutoCloseable closeable, Executor executor) {
    final CompletableFuture<Void> future;
    if (executor == null) {
      future = CompletableFuture
          .runAsync(() -> fluentProcess.writeToStdin(inputStream, false));
    } else {
      future = CompletableFuture
          .runAsync(() -> fluentProcess.writeToStdin(inputStream, false), executor);
    }
    final CompletableFuture<Void> futureWithClose = future
        .thenRun(() -> {
          try {
            closeable.close();
          } catch (RuntimeException ex) {
            throw ex;
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });
    fluentProcess.registerCloseable(futureWithClose::join);
  }

  @Override
  public String toString() {
    return processBuilder.command()
        .stream()
        .map(this::quoteArg)
        .collect(Collectors.joining(" "));
  }

  private static final String UNQUOTED_CHARACTERS = "|&;<>()$`\\\"' \t\n*?[#˜=%";
  private static final Character ESCAPE_CHARACTER = '\\';

  private String quoteArg(String arg) {
    if (UNQUOTED_CHARACTERS.chars().mapToObj(c -> (char) c).anyMatch(c -> arg.indexOf(c) >= 0)) {
      return UNQUOTED_CHARACTERS.chars().mapToObj(c -> String.valueOf((char) c))
          .reduce(arg, (c, escapedArg) -> escapedArg.replace(c, ESCAPE_CHARACTER + c), (u, v) -> v);
    }
    return arg;
  }

  CustomProcess getProcess() {
    return process;
  }

  /**
   * Closes the process by destroying it forcibly if not yet terminated.
   * <p>
   * If the process exit code is not an allowed one this method will throw
   *  a {@code PocessException}.
   * </p>
   * <p>
   * By default this method is called when the process exit and all outputs are fully read.
   * If you need to read any process output even in case of process failure consider call method
   *  {@code FluentProcess.withoutCloseAfterLast()} or
   *  {@code FluentProcessBuilder.dontCloseAfterLast()}.
   * </p>
   */
  @Override
  public void close() {
    Exception exception = closeAndGetException();
    if (exception != null) {
      if (exception instanceof RuntimeException) {
        throw RuntimeException.class.cast(exception);
      }
      throw new RuntimeException(exception);
    }
  }

  void registerCloseable(Closeable closeable) {
    this.closeables.add(closeable);
  }

  void checkTimeout() {
    if (this.end.map(e -> Instant.now().isAfter(e)).orElse(false)) {
      Exception exception = closeAndGetException();
      ProcessTimeoutException timeoutException = new ProcessTimeoutException(
          timeout, processBuilder.command());
      if (exception != null) {
        timeoutException.addSuppressed(exception);
      }
      throw timeoutException;
    }
  }

  void microSleep() {
    try {
      Thread.sleep(0, 20_000);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean isClosed() {
    return !process.isAlive();
  }

  private Exception closeAndGetException() {
    Exception exception = null;
    if (process.isAlive()) {
      process.destroyForcibly();
      try {
        int exitCode = process.waitFor();
        exception = checkExitCode(exitCode);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      int exitCode = process.exitValue();
      exception = checkExitCode(exitCode);
    }
    while (!closeables.isEmpty()) {
      try {
        closeables.remove(0).close();
      } catch (Exception ex) {
        if (exception == null) {
          exception = ex;
        } else {
          exception.addSuppressed(ex);
        }
      }
    }
    return exception;
  }

  private Exception checkExitCode(int exitCode) {
    if (!allowedExitCodes.contains(exitCode)) {
      return new ProcessException(exitCode, processBuilder.command());
    }
    return null;
  }

}
