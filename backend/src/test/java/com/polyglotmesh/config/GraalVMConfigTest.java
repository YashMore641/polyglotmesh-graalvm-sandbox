package com.polyglotmesh.config;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class GraalVMConfigTest {

    @Autowired
    private GraalVMConfig.PolyglotContextFactory contextFactory;

    @Test
    void createsRestrictedContextWithJavaScriptAndPython() {
        try (Context context = contextFactory.createContext()) {
            assertEquals(42, context.eval("js", "21 + 21").asInt());
            assertEquals(42, context.eval("python", "21 + 21").asInt());
            assertThrows(PolyglotException.class,
                    () -> context.eval("js", "Java.type('java.lang.System')"));
        }
    }

    @Test
    void createsRestrictedJavaScriptContextWithControlledOutput() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Context context = contextFactory.createJavaScriptContext(output)) {
            assertEquals(42, context.eval("js", "21 + 21").asInt());
            assertThrows(PolyglotException.class,
                    () -> context.eval("js", "Java.type('java.lang.System')"));
            context.eval("js", "console.log('captured')");
        }
        assertEquals("captured", output.toString().trim());
    }
}
