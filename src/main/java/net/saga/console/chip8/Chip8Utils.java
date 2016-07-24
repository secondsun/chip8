package net.saga.console.chip8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Chip8Utils {

    private Chip8Utils() {}

    public static Chip8 createFromRom(URL rom) throws IOException {
        try {
            return createFromRom(Paths.get(rom.toURI()));
        } catch (URISyntaxException ex) {
            Logger.getLogger(Chip8Utils.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }
    
    public static Chip8 createFromRom(String rom) throws IOException {
        return createFromRom(Paths.get(rom));
    }
    
    public static Chip8 createFromRom(File rom) throws IOException {
        return createFromRom(rom.toPath());
    }
    
    public static Chip8 createFromRom(Path rom) throws IOException {
    
            byte[] reader = Files.readAllBytes(rom);
            int index = 0x200;
            byte[] memory = new byte[4096];//4k memory
            for (byte read : reader) {
                memory[index] = (byte) (read);
                index += 1;
            }
                
            Chip8 chip8 = new Chip8(memory);    
            return chip8;
        
        
        
        
        
        
    }
    
}
