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
