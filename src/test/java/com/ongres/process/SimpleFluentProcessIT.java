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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.ongres.process.FluentProcess.*;

public class SimpleFluentProcessIT {

    @Test
    public void testShell() throws Exception {
        Assertions.assertIterableEquals(
                Arrays.asList("hello", "world"),
                $("echo hello; echo world")
                        .stream().collect(Collectors.toList()));
    }

    @Test
    public void testShellPipe() throws Exception {
        Assertions.assertIterableEquals(
                Arrays.asList("hello", "world"),
                $("echo hello; echo world")
                        .pipe$("cat")
                        .stream().collect(Collectors.toList()));
    }

    @Test
    public void testString() {

        Assertions.assertEquals($("echo hello| wc -c").get().trim(),"6");
    }
}
