package com.polyglotmesh.config;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
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
    public PolyglotContextFactory polyglotContextFactory(
            Engine polyglotEngine,
            GraalVMSecurityProperties securityProperties) {
        return new PolyglotContextFactory(polyglotEngine, securityProperties);
    }

    public static final class PolyglotContextFactory {

        private final Engine engine;
        private final GraalVMSecurityProperties securityProperties;

        public PolyglotContextFactory(Engine engine) {
            this(engine, new GraalVMSecurityProperties());
        }

        public PolyglotContextFactory(
                Engine engine,
                GraalVMSecurityProperties securityProperties) {
            this.engine = engine;
            this.securityProperties = securityProperties;
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

        public Context createPythonContext(OutputStream outputStream) {
            return createBuilder("python")
                    .out(outputStream)
                    .err(outputStream)
                    .build();
        }

        private Context.Builder createBuilder(String... languages) {
            Context.Builder builder = Context.newBuilder(languages)
                    .engine(engine)
                    .allowAllAccess(false)
                    .allowHostAccess(HostAccess.NONE)
                    .allowHostClassLookup(className -> false)
                    .allowIO(IOAccess.NONE)
                    .allowCreateThread(false)
                    .allowNativeAccess(false);

            if (securityProperties.getStatementLimit() > 0) {
                ResourceLimits resourceLimits = ResourceLimits.newBuilder()
                        .statementLimit(securityProperties.getStatementLimit(), source -> true)
                        .build();
                builder.resourceLimits(resourceLimits);
            }
            return builder;
        }

        public int getMaxSourceLength() {
            return securityProperties.getMaxSourceLength();
        }

        public long getExecutionTimeoutMs() {
            return securityProperties.getExecutionTimeoutMs();
        }
    }
}
