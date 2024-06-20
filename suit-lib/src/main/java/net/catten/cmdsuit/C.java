package net.catten.cmdsuit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class C {
    private C(){}

    public static final String LoggerName = C.class.getPackageName();

    public static Logger getDefaultLogger() {
        return LoggerFactory.getLogger(LoggerName);
    }
}
