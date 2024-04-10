/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.catten.cmdsuit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 *
 * @author cattenlinger
 */
public class ToolSuitProcessContext {
    private final Logger logger = LoggerFactory.getLogger(C.LoggerName);

    private final SuitProcessBuilder builder;
    private Process process = null;
    
    public ToolSuitProcessContext(SuitProcessBuilder builder) {
        this.builder = builder;
    }

    public SuitContext getSuit() {
        return builder.getSuit();
    }

    public CompletableFuture<Process> start() throws IOException {
        return start(null);
    }

    public synchronized CompletableFuture<Process> start(Executor executor) throws IOException {
        if(process != null) return process.onExit();
        process = builder.buildProcessInternal().start();
        if(executor == null) return process.onExit();
        return setupIOMonitorsForProcess(process, executor);
    }

    private CompletableFuture<Process> setupIOMonitorsForProcess(Process process, Executor executor) {
        final var stdOutMon = new IOMonitor(process.getInputStream(), this);
        stdOutMon.setOnResult(l -> {
            if(onStdOutLine == null) return;
            onStdOutLine.accept(new Message("stdout", l));
        });

        final var stdErrMon = new IOMonitor(process.getErrorStream(), this);
        stdErrMon.setOnResult(l -> {
            if(onStdErrLine == null) return;
            onStdErrLine.accept(new Message("stderr", l));
        });

        return CompletableFuture.supplyAsync(() -> {
            while (process.isAlive()) {
                if((stdOutMon.update() + stdErrMon.update()) > 0) continue;
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(30));
            }

            try {
                final var r = process.onExit().get();
                stdErrMon.close();
                stdOutMon.close();
                return r;
            } catch (InterruptedException e) {
                logger.info("Process execution waiting was interrupted.");
                return process;
            } catch (ExecutionException e) {
                logger.error("Process execution meets error.", e);
                return process;
            } catch (IOException e) {
                logger.error("IO flush meets error.", e);
                return process;
            }
        }, executor);
    }

    private Consumer<Message> onStdOutLine = null;

    public ToolSuitProcessContext setOnStdOutLine(Consumer<Message> onStdOutLine) {
        this.onStdOutLine = onStdOutLine;
        return this;
    }

    private Consumer<Message> onStdErrLine = null;

    public ToolSuitProcessContext setOnStdErrLine(Consumer<Message> onStdErrLine) {
        this.onStdErrLine = onStdErrLine;
        return this;
    }

    public static final class Message {
        private final String sourceName;
        private final String line;

        public String getSourceName() {
            return sourceName;
        }

        public String getLine() {
            return line;
        }

        public Message(String sourceName, String line) {
            this.sourceName = sourceName;
            this.line = line;
        }

        @Override
        public String toString() {
            return "source: " + sourceName + "; message: " + line;
        }
    }

    private static class IOMonitor implements Closeable {
        private final Logger log = LoggerFactory.getLogger(C.LoggerName);
        private final InputStreamReader reader;

        public IOMonitor(InputStream inputStream, ToolSuitProcessContext ctx) {
            reader = new InputStreamReader(inputStream, ctx.getSuit().getCharset());
        }

        private StringBuilder sb = new StringBuilder(256);

        public int update() {
            int read = 0;
            try {
                while(reader.ready()) {
                    char c = (char) reader.read();
                    read++;
                    if(c == '\n' || c == '\r') {
                        yieldResult();
                        continue;
                    }
                    sb.append(c);
                }
                return read;
            } catch (Exception e) {
                log.error("IOMonitor error: ",e);
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            yieldResult();
            reader.close();
        }

        private void yieldResult() {
            final var old = sb;
            if(old.length() == 0) return;

            sb = new StringBuilder(256);
            if(onResult == null) return;

            final var str = old.toString();
            onResult.accept(str);
        }

        private Consumer<String> onResult = null;

        public void setOnResult(Consumer<String> onResult) {
            this.onResult = onResult;
        }
    }

}
