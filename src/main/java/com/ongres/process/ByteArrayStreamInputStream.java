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
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

class ByteArrayStreamInputStream extends InputStream {
  private final Stream<byte[]> stream;
  private final Iterator<byte[]> iterator;
  private byte[] buffer = null;
  private int bufferIndex = 0;

  public ByteArrayStreamInputStream(Stream<byte[]> stream) {
    this.stream = stream;
    this.iterator = stream.iterator();
  }

  @Override
  public int read(byte[] buffer, int off, int len) throws IOException {
    if (fill() < 0) {
      return -1;
    }

    final int size = Math.min(len, this.buffer.length - this.bufferIndex);
    System.arraycopy(this.buffer, this.bufferIndex, buffer, off, size);
    this.bufferIndex += size;
    return size;
  }

  @Override
  public int read() throws IOException {
    try {
      if (fill() < 0) {
        return -1;
      }
      int read = buffer[bufferIndex++] & 0xFF;

      return read;
    } catch (IllegalStateException ex) {
      return -1;
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof IOException) {
        return -1;
      }
      throw ex;
    }
  }

  private int fill() {
    while (buffer == null || bufferIndex >= buffer.length) {
      if (iterator.hasNext()) {
        buffer = iterator.next();
        bufferIndex = 0;
        return buffer.length;
      } else {
        return -1;
      }
    }
    return buffer.length - bufferIndex;
  }

  @Override
  public int available() throws IOException {
    if (buffer != null) {
      return buffer.length - bufferIndex;
    }
    return iterator.hasNext() ? 1 : 0;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }

}
