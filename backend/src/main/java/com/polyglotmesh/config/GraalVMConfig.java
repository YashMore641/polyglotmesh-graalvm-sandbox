package com.polyglotmesh.config;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.OutputStream;

@Configuration
public class GraalVMConfig {

    @Bean(destroyMethod = "close")
    public Engine polyglotEngine() {
        return Engine.newBuilder().build();
    }

    @Bean
    public PolyglotContextFactory polyglotContextFactory(Engine polyglotEngine) {
        return new PolyglotContextFactory(polyglotEngine);
    }

    public static final class PolyglotContextFactory {

        private final Engine engine;

        public PolyglotContextFactory(Engine engine) {
            this.engine = engine;
        }

        public Context createContext() {
            return createBuilder("js", "python").build();
        }

        public Context createJavaScriptContext(OutputStream outputStream) {
            return createBuilder("js")
                    .out(outputStream)
                    .err(outputStream)
                    .build();
        }

        private Context.Builder createBuilder(String... languages) {
            return Context.newBuilder(languages)
                    .engine(engine)
                    .allowAllAccess(false)
                    .allowHostAccess(HostAccess.NONE)
                    .allowHostClassLookup(className -> false)
                    .allowIO(IOAccess.NONE)
                    .allowCreateThread(false)
                    .allowNativeAccess(false);
        }
    }
}
