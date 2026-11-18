package com.polyglotmesh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "polyglotmesh.security")
public class GraalVMSecurityProperties {

    private int maxSourceLength = 100_000;
    private long statementLimit = 0;
    private long executionTimeoutMs = 30_000;

    public int getMaxSourceLength() {
        return maxSourceLength;
    }

    public void setMaxSourceLength(int maxSourceLength) {
        this.maxSourceLength = maxSourceLength;
    }

    public long getStatementLimit() {
        return statementLimit;
    }

    public void setStatementLimit(long statementLimit) {
        this.statementLimit = statementLimit;
    }

    public long getExecutionTimeoutMs() {
        return executionTimeoutMs;
    }

    public void setExecutionTimeoutMs(long executionTimeoutMs) {
        this.executionTimeoutMs = executionTimeoutMs;
    }
}
