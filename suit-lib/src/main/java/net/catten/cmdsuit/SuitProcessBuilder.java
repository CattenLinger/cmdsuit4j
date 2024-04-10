/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.catten.cmdsuit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author cattenlinger
 */
public class SuitProcessBuilder {
    
    private final SuitContext suit;

    private final Map<String, String> env = new HashMap<>();
   
    private ProcessBuilder pBuilder = null;
    private boolean isUpdated = true;
    
    public SuitProcessBuilder(SuitContext suit) {
        this.suit = suit;
        this.env.putAll(System.getenv());
    }

    public SuitContext getSuit() {
        return suit;
    }
    
    private final LinkedList<String> args = new LinkedList<>();
    public List<String> getArgs() {
        return Collections.unmodifiableList(args);
    }

    public SuitProcessBuilder args(String... args) {
        if(!this.args.isEmpty()) this.args.clear();
        this.args.addAll(Arrays.asList(args));
        onUpdate();
        return this;
    }
    
    public SuitProcessBuilder appendArg(String arg) {
        this.args.add(arg);
        onUpdate();
        return this;
    }
    
    private Path workDirectory = null;
    public Path getWorkDirectory() {
        return workDirectory;
    }
    
    public SuitProcessBuilder workDirectory(Path location) {
        workDirectory = location.toAbsolutePath();
        onUpdate();
        return this;
    }

    public SuitProcessBuilder env(String key, String value) {
        if(value == null) env.remove(key);
        else env.put(key, value);
        onUpdate();
        return this;
    }

    public Map<String, String> getEnvironments() {
        return Collections.unmodifiableMap(env);
    }

    private boolean inheritStdOutAndStdErr = false;

    public SuitProcessBuilder setInheritStdOutAndStdErr(boolean b) {
        inheritStdOutAndStdErr = true;
        return this;
    }

    public boolean isInheritStdOutAndStdErr() {
        return inheritStdOutAndStdErr;
    }

    ProcessBuilder buildProcessInternal() throws IOException {
        if(pBuilder != null && !isUpdated) return pBuilder;
        
        // If this builder is updated, make a new process builder.
        isUpdated = false;
        pBuilder = new ProcessBuilder();
        
        // Set argument
        final var argList = new ArrayList<String>(args.size() + 1);
        argList.add(suit.getExecutable().toString());
        argList.addAll(args);
        pBuilder.command(argList);
        
        // Set current path
        final Path workDir;
        if(workDirectory == null) workDir = Paths.get(".");
        else workDir = workDirectory;
        pBuilder.directory(workDir.toFile());

        // Set environment
        final var bEnv = pBuilder.environment();
        bEnv.clear();
        bEnv.putAll(env);

        //
        if(inheritStdOutAndStdErr) {
            pBuilder.inheritIO();
        }
        
        return pBuilder;
    }

    public ToolSuitProcessContext build() throws IOException {
        return new ToolSuitProcessContext(this);
    }
    
    private void onUpdate() {
        isUpdated = true;
    }
}
