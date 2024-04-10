/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.catten.cmdsuit;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 *
 * @author cattenlinger
 */
public class SuitContext {
    private final Path executable;
    private final Path suitHome;

    private Charset charset = StandardCharsets.UTF_8;

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public SuitContext(Path executablePath) {
        this.executable = executablePath;
        this.suitHome = executablePath.getParent();
    }
    
    public SuitProcessBuilder buildProcess(String... args) {
        final var builder = new SuitProcessBuilder(this);
        builder.args(args);
        return builder;
    }
    
    public Path getExecutable() {
        return executable.toAbsolutePath();
    }
    
    public Path getHome() {
        return suitHome.toAbsolutePath();
    }

    public SuitProcessBuilder processBuilder() {
        return new SuitProcessBuilder(this);
    }
}
