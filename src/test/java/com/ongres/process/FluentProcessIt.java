/*-
 *  § 
 * docker-junit-extension
 *    
 * Copyright (C) 2019 OnGres, Inc.
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FluentProcessIt {

  @Test
  public void test() throws Exception {
    Assertions.assertIterableEquals(
        Arrays.asList("hello", "world"),
        FluentProcess.start("sh", "-c", "echo hello; echo world")
        .stream().collect(Collectors.toList()));
  }

  @Test
  public void testSuccessful() throws Exception {
    Assertions.assertTrue(FluentProcess.start("sh", "-c", "echo hello world")
        .isSuccessful());
  }

  @Test
  public void testUnsuccessful() throws Exception {
    Assertions.assertFalse(FluentProcess.start("sh", "-c", "[ -z 'hello world' ]")
        .isSuccessful());
  }

  @Test
  public void testJoin() throws Exception {
    FluentProcess.start("sh", "-c", "echo hello world")
        .join();
  }

  @Test
  public void testPipe() throws Exception {
    Assertions.assertIterableEquals(
        Arrays.asList("hello", "world"),
        FluentProcess.start("sh", "-c", "echo hello; echo world")
        .pipe("cat")
        .stream().collect(Collectors.toList()));
  }

  @Test
  public void testPipeWithLines() throws Exception {
    Assertions.assertEquals("hello world",
        FluentProcess.start("sh", "-c", "echo hello world")
            .pipe("cat")
            .get());
  }

  @Test
  public void testEnvironment() throws Exception {
    Assertions.assertEquals("A=1\nB=2",
        FluentProcess.builder("env")
        .clearEnvironment()
        .environment("A", "1")
        .environment("B", "2")
        .start()
        .get());
  }

  @Test
  public void testInput() throws Exception {
    try (Stream<String> inputStream = Stream.of("hello world")) {
      Assertions.assertEquals("hello world",
          FluentProcess.start("cat")
              .inputStream(inputStream)
              .get());
    }
  }

  @Test
  public void testWriteToOutputStream() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    FluentProcess.start("sh", "-c", "echo hello world")
        .writeToOutputStream(outputStream);
    Assertions.assertEquals("hello world\n",
        new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testError() throws Exception {
    Assertions.assertThrows(ProcessException.class,
        () -> FluentProcess.start("sh", "-c", "exit 1")
          .get());
  }

  @Test
  public void testDontCloseAfterLast() throws Exception {
    Stream<String> output = FluentProcess.builder("sh", "-c", "79")
        .dontCloseAfterLast()
        .start()
        .stream();
    Assertions.assertEquals("sh: 1: 79: not found", output.collect(Collectors.joining()));
    try {
      output.close();
      Assertions.fail();
    } catch (ProcessException ex) {
      Assertions.assertEquals("Command sh -c 79 exited with code 127", ex.getMessage());
      Assertions.assertEquals(127, ex.getExitCode());
      Assertions.assertIterableEquals(
          Stream.of("sh", "-c", "79")
          .collect(Collectors.toList()),
          ex.getArgs());
    }
  }

  @Test
  public void testTryGet() throws Exception {
    Output output = FluentProcess.start("sh", "-c", "echo hello world")
        .tryGet();
    Assertions.assertEquals("hello world",
        output.output().get());
    Assertions.assertFalse(output.exception().isPresent());
  }

  @Test
  public void testTryGetWithError() throws Exception {
    Output output = FluentProcess.start("sh", "-c", "echo hello world; 79")
        .tryGet();
    Assertions.assertFalse(output.output().isPresent());
    Assertions.assertTrue(output.exception().get() instanceof ProcessException);
    ProcessException ex = (ProcessException) output.exception().get();
    Assertions.assertEquals("Command sh -c echo hello world; 79 exited with code 127",
        ex.getMessage());
    Assertions.assertEquals(127, ex.getExitCode());
    Assertions.assertIterableEquals(
        Stream.of("sh", "-c", "echo hello world; 79")
        .collect(Collectors.toList()),
        ex.getArgs());
  }

  @Test
  public void testTryGetWithDontCloseAfterLast() throws Exception {
    Output output = FluentProcess.builder("sh", "-c", "echo hello world; 79")
        .dontCloseAfterLast()
        .start()
        .tryGet();
    Assertions.assertEquals("hello world",
        output.output().get());
    Assertions.assertTrue(output.exception().get() instanceof ProcessException);
    ProcessException ex = (ProcessException) output.exception().get();
    Assertions.assertEquals("Command sh -c echo hello world; 79 exited with code 127",
        ex.getMessage());
    Assertions.assertEquals(127, ex.getExitCode());
    Assertions.assertIterableEquals(
        Stream.of("sh", "-c", "echo hello world; 79")
        .collect(Collectors.toList()),
        ex.getArgs());
  }

  @Test
  public void testExitCode() throws Exception {
    try {
      FluentProcess.start("sh", "-c", "exit 79")
          .get();
      Assertions.fail();
    } catch (ProcessException ex) {
      Assertions.assertEquals(79, ex.getExitCode());
      Assertions.assertIterableEquals(
          Stream.of("sh", "-c", "exit 79")
          .collect(Collectors.toList()),
          ex.getArgs());
    }
  }

  @Test
  public void testAllowedExitCode() throws Exception {
    FluentProcess.builder("sh", "-c", "exit 79")
        .allowedExitCode(79)
        .start()
        .get();
  }

  @Test
  public void testStream() throws Exception {
    Assertions.assertEquals(1,
        FluentProcess.start("sh", "-c", "echo hello world")
        .stream()
        .peek(line -> Assertions.assertEquals("hello world", line))
        .count());
  }

  @Test
  public void testStreamEmptyBuffer() throws Exception {
    Assertions.assertTimeout(Duration.of(1, ChronoUnit.SECONDS),
        () -> Assertions.assertEquals(1,
            FluentProcess.start("sh", "-c", "echo hello world; sleep 2")
            .stream()
            .peek(line -> Assertions.assertEquals("hello world", line))
            .limit(1)
            .count()));
  }

  @Test
  public void testTimeout() throws Exception {
    Assertions.assertThrows(ProcessTimeoutException.class,
        () -> FluentProcess.start("sh", "-c", "sleep 3600")
            .withTimeout(Duration.of(10, ChronoUnit.MILLIS))
            .stream()
            .count());
  }

  @Test
  public void testTimeoutWithData() throws Exception {
    Assertions.assertTimeout(Duration.of(1, ChronoUnit.SECONDS),
        () -> Assertions.assertThrows(ProcessTimeoutException.class,
            () -> FluentProcess.start("sh", "-c", "sleep 3600")
            .withTimeout(Duration.of(10, ChronoUnit.MILLIS))
            .streamBytes()
            .count()));
  }

  @Test
  public void testLongPipedExecutionWithTimeout() throws Exception {
    Assertions.assertTimeout(Duration.of(2, ChronoUnit.SECONDS),
        () -> Assertions.assertThrows(ProcessTimeoutException.class,
            () -> FluentProcess.start("cat", "/dev/zero")
            .pipe(FluentProcess.start("cat"))
            .withTimeout(Duration.of(1, ChronoUnit.SECONDS))
            .stream()
            .count()));
  }

}
