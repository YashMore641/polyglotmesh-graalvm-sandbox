package com.polyglotmesh.config;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            return Context.newBuilder("js", "python")
                    .engine(engine)
                    .allowAllAccess(false)
                    .allowHostAccess(HostAccess.NONE)
                    .allowHostClassLookup(className -> false)
                    .allowIO(IOAccess.NONE)
                    .allowCreateThread(false)
                    .allowNativeAccess(false)
                    .build();
        }
    }
}
