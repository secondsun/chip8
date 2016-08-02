package net.saga.console.chip8;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Chip8Utils {

    private Chip8Utils() {
    }

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

    /**
     *
     * Packs a graphics row (8 pixels of the sprite) into a btye
     *
     * @param x
     * @param y
     * @param video
     * @return
     */
    public static byte getSpriteRow(int x, int y, byte[] video) {
        x = x % 64;
        y = y % 32;
        byte byte1 = video[x + y * 64];
        byte byte2 = video[x + 1 + y * 64];
        byte byte3 = video[x + 2 + y * 64];
        byte byte4 = video[x + 3 + y * 64];
        byte byte5 = video[x + 4 + y * 64];
        byte byte6 = video[x + 5 + y * 64];
        byte byte7 = video[x + 6 + y * 64];
        byte byte8 = video[x + 7 + y * 64];

        return (byte) ((byte1 << 7)
                | (byte2 << 6)
                | (byte3 << 5)
                | (byte4 << 4)
                | (byte5 << 3)
                | (byte6 << 2)
                | (byte7 << 1)
                | (byte8));

    }

}
