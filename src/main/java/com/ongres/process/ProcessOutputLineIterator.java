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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

class ProcessOutputLineIterator implements Iterator<OutputLine>, Closeable {

  private final Map<Integer, ProcessOutputInputStream> processOutputDataInputStreams;
  private final Map<Integer, BufferedReader> bufferedReaders;
  private final boolean closeOnLast;
  private OutputLine line = null;

  ProcessOutputLineIterator(
      Map<Integer, ProcessOutputInputStream> processOutputDataInputStreams,
      boolean closeOnLast) {
    this.processOutputDataInputStreams = processOutputDataInputStreams;
    this.bufferedReaders = processOutputDataInputStreams.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            e -> new BufferedReader(new InputStreamReader(
                e.getValue(),
                StandardCharsets.UTF_8))));
    this.closeOnLast = closeOnLast;
  }

  @Override
  public boolean hasNext() {
    if (line != null) {
      return true;
    }

    while (line == null) {
      for (Map.Entry<Integer, BufferedReader> bufferedReaderEntry : bufferedReaders.entrySet()) {
        Integer fd = bufferedReaderEntry.getKey();
        BufferedReader bufferedReader = bufferedReaderEntry.getValue();
        if (tryReadLine(fd, bufferedReader)) {
          return true;
        }
      }
      if (processOutputDataInputStreams.values().stream()
          .allMatch(ProcessOutputInputStream::isClosed)) {
        if (closeOnLast) {
          try {
            close();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
        return false;
      }
    }

    return true;
  }

  private boolean tryReadLine(Integer fd, BufferedReader bufferedReader) {
    try {
      if (bufferedReader.ready()) {
        String line = bufferedReader.readLine();
        if (line == null) {
          return false;
        } else {
          this.line = new OutputLine(fd, line);
          return true;
        }
      }
      return false;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public OutputLine next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    try {
      return line;
    } finally {
      line = null;
    }
  }

  @Override
  public void close() throws IOException {
    IOException ioException = null;
    for (BufferedReader bufferedReader : this.bufferedReaders.values()) {
      try {
        bufferedReader.close();
      } catch (IOException ex) {
        if (ioException == null) {
          ioException = ex;
        } else {
          ioException.addSuppressed(ex);
        }
      }
    }
    if (ioException != null) {
      throw ioException;
    }
  }

}
