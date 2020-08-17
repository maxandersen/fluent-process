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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

class ProcessOutputDataIterator implements Iterator<OutputData>, Closeable {

  private final FluentProcess fluentProcess;
  private final Map<Integer, InputStream> inputStreams;
  private final boolean closeOnLast;
  private OutputData frame = null;

  ProcessOutputDataIterator(FluentProcess fluentProcess,
      boolean closeOnLast, Map<Integer, InputStream> inputStreams) {
    this.fluentProcess = fluentProcess;
    this.inputStreams = inputStreams;
    inputStreams.entrySet().stream().map(Map.Entry::getValue)
        .forEach(fluentProcess::registerCloseable);
    this.closeOnLast = closeOnLast;
  }

  @Override
  public boolean hasNext() {
    if (isClosed()) {
      if (closeOnLast) {
        try {
          close();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
      return false;
    }
    fluentProcess.checkTimeout();

    while (frame == null) {
      frame = availableInputStream()
          .map(this::readAvailables)
          .orElse(null);
      if (isClosed()) {
        if (closeOnLast) {
          try {
            close();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
        return false;
      }
      if (frame == null) {
        fluentProcess.checkTimeout();

        fluentProcess.microSleep();
      }
    }

    return true;
  }

  public int available() {
    fluentProcess.checkTimeout();

    final int available = Optional.ofNullable(frame)
        .map(frame -> frame.bytes().length)
        .orElseGet(() -> availableInputStream()
            .map(e -> {
              try {
                return e.getValue().available();
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            })
            .orElse(0));

    if (available <= 0) {
      fluentProcess.microSleep();
    }

    return available;
  }

  public boolean isClosed() {
    return fluentProcess.isClosed()
        && frame == null
        && !availableInputStream().isPresent();
  }

  private Optional<Entry<Integer, InputStream>> availableInputStream() {
    return inputStreams.entrySet().stream()
        .filter(inputStream -> {
          try {
            return inputStream.getValue().available() > 0;
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        })
        .findAny();
  }

  private OutputData readAvailables(Map.Entry<Integer, InputStream> inputStream) {
    try {
      int available = inputStream.getValue().available();
      if (available > 0) {
        byte[] outputBuffer = new byte[available];
        int value = inputStream.getValue().read(outputBuffer);
        if (value > 0) {
          return new OutputData(inputStream.getKey(), outputBuffer);
        }
      }
      return null;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public OutputData next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    try {
      return frame;
    } finally {
      frame = null;
    }
  }

  @Override
  public void close() throws IOException {
    fluentProcess.close();
  }

}
