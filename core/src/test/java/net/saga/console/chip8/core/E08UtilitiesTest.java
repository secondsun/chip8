package net.saga.console.chip8.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E08UtilitiesTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        chip8.executeOpcode(0x6064);
        chip8.executeOpcode(0x6127);
        chip8.executeOpcode(0x6212);
        chip8.executeOpcode(0x63AE);
        chip8.executeOpcode(0x64FF);
    }

    @Test
    void testBCD() {
        chip8.executeOpcode(0xA200);
        chip8.executeOpcode(0xF433); // 255 -> 2, 5, 5
        byte[] memory = chip8.copyMemory();
        assertEquals(2, memory[0x200]);
        assertEquals(5, memory[0x201]);
        assertEquals(5, memory[0x202]);
    }

    @Test
    void testRegisterSaveLoadAcrossIncrementModes() {
        chip8.executeOpcode(0xA200);
        chip8.executeOpcode(0xF355);
        assertEquals(0x204, chip8.getI());

        chip8.executeOpcode(0xA200);
        chip8.executeOpcode(0x6000);
        chip8.executeOpcode(0x6100);
        chip8.executeOpcode(0x6200);
        chip8.executeOpcode(0x6300);
        chip8.executeOpcode(0xF365);
        assertEquals(0x64, chip8.getV(0));
        assertEquals(0x27, chip8.getV(1));
        assertEquals(0x12, chip8.getV(2));
        assertEquals(0xAE, chip8.getV(3));
        assertEquals(0x204, chip8.getI());

        // SCHIP 1.1 does not modify I
        Chip8Machine schip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.SUPER_CHIP_1_1));
        schip.executeOpcode(0x6055);
        schip.executeOpcode(0xA300);
        schip.executeOpcode(0xF055);
        assertEquals(0x300, schip.getI());
    }

    @Test
    void testXoChipLongLoadRangeSaveLoadAndRplFlags() {
        Chip8Machine xo = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.XO_CHIP));
        // F000 4321 -> I = 0x4321 (4-byte instruction)
        xo.writeMemoryByte(0x200, (byte) 0xF0);
        xo.writeMemoryByte(0x201, (byte) 0x00);
        xo.writeMemoryByte(0x202, (byte) 0x43);
        xo.writeMemoryByte(0x203, (byte) 0x21);
        xo.stepInstruction();
        assertEquals(0x4321, xo.getI());
        assertEquals(0x204, xo.getPC());

        // 5XY2 / 5XY3 range save/load without changing I
        xo.executeOpcode(0x62AA);
        xo.executeOpcode(0x63BB);
        xo.executeOpcode(0x64CC);
        xo.executeOpcode(0x5242); // save V2..V4 to [I]
        assertEquals(0x4321, xo.getI());

        xo.executeOpcode(0x6200);
        xo.executeOpcode(0x6300);
        xo.executeOpcode(0x6400);
        xo.executeOpcode(0x5243); // load V2..V4 from [I]
        assertEquals(0xAA, xo.getV(2));
        assertEquals(0xBB, xo.getV(3));
        assertEquals(0xCC, xo.getV(4));

        // RPL flags FX75 / FX85
        xo.executeOpcode(0x6077);
        xo.executeOpcode(0xF075);
        xo.executeOpcode(0x6000);
        xo.executeOpcode(0xF085);
        assertEquals(0x77, xo.getV(0));

        // XO-CHIP pitch FX3A
        xo.executeOpcode(0x6180);
        xo.executeOpcode(0xF13A);
        assertEquals(0x80, xo.getAudioPitch());
    }

    @Test
    void testSaveStateRewindAndDisassembler() {
        RewindBuffer rewind = new RewindBuffer(4);
        rewind.push(chip8.createSaveState());
        chip8.executeOpcode(0x6011);
        assertEquals(0x11, chip8.getV(0));

        SaveState restored = rewind.popLatest().orElseThrow();
        chip8.restoreSaveState(restored);
        assertEquals(0x64, chip8.getV(0));

        Instruction decoded = Disassembler.decode(0xD125, 0);
        assertEquals("DRW V1, V2, 5", Disassembler.format(decoded));
    }

    @Test
    void testTimendusLogoAndCoraxRomsRenderPixels() throws IOException {
        Path ibmRom = Path.of("..", "roms", "tests", "2-ibm-logo.ch8");
        if (Files.isRegularFile(ibmRom)) {
            Chip8Machine machine = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.COSMAC_VIP));
            byte[] romBytes = Files.readAllBytes(ibmRom);
            assertTrue(RomDatabase.lookup(romBytes).isPresent());
            machine.loadRom(romBytes);
            for (int f = 0; f < 30; f++) {
                machine.stepFrame(20);
            }
            byte[] pixels = machine.display().copyPlane0();
            int lit = 0;
            for (byte p : pixels) {
                if (p != 0) lit++;
            }
            assertTrue(lit > 100, "IBM logo should render > 100 lit pixels, got " + lit);
        }

        Path superNeatBoy = Path.of("..", "roms", "xochip", "superneatboy.xo8");
        if (Files.isRegularFile(superNeatBoy)) {
            byte[] romBytes = Files.readAllBytes(superNeatBoy);
            var meta = RomDatabase.lookup(romBytes).orElseThrow();
            assertEquals(Chip8Variant.XO_CHIP, meta.recommendedVariant());
            Chip8Machine xo = new Chip8Machine(new Chip8Config(meta.recommendedVariant(), meta.recommendedQuirks(), meta.recommendedCyclesPerFrame()));
            xo.loadRom(romBytes);
            for (int f = 0; f < 30; f++) {
                xo.stepFrame(meta.recommendedCyclesPerFrame());
            }
            var snap = xo.display().snapshot();
            int nonZero = 0;
            for (byte p : snap.pixels()) {
                if (p != 0) nonZero++;
            }
            assertTrue(nonZero > 100, "Super NeatBoy XO-CHIP should render > 100 pixels, got " + nonZero);
        }
    }

    @Test
    void testMegaChipOpcodesPaletteBlendingCollisionAndRomExecution() throws IOException {
        Chip8Machine mega = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.MEGA_CHIP));
        // Enable Mega-Chip mode (256x192)
        mega.executeOpcode(0x0011);
        assertTrue(mega.display().isMegaMode());
        assertEquals(256, mega.display().width());
        assertEquals(192, mega.display().height());

        // 24-bit LDHI I, 0x012345 (4-byte instruction)
        mega.writeMemoryByte(0x200, (byte) 0x01);
        mega.writeMemoryByte(0x201, (byte) 0x01);
        mega.writeMemoryByte(0x202, (byte) 0x23);
        mega.writeMemoryByte(0x203, (byte) 0x45);
        mega.stepInstruction();
        assertEquals(0x012345, mega.getI());
        assertEquals(0x204, mega.getPC());

        // Load 2 ARGB8 palette entries at 0x012345: [1]=Red (0xFFFF0000), [2]=Blue (0xFF0000FF)
        byte[] pal = new byte[] {
            (byte) 0xFF, (byte) 0xFF, 0x00, 0x00,
            (byte) 0xFF, 0x00, 0x00, (byte) 0xFF
        };
        for (int i = 0; i < pal.length; i++) {
            mega.writeMemoryByte(0x012345 + i, pal[i]);
        }
        mega.executeOpcode(0x0202); // LDPAL 2
        assertEquals(0xFFFF0000, mega.display().getMegaPaletteColor(1));
        assertEquals(0xFF0000FF, mega.display().getMegaPaletteColor(2));

        // 2x2 sprite of color 1 at 0x020000
        mega.writeMemoryByte(0x020000, (byte) 1);
        mega.writeMemoryByte(0x020001, (byte) 1);
        mega.writeMemoryByte(0x020002, (byte) 1);
        mega.writeMemoryByte(0x020003, (byte) 1);
        mega.writeMemoryByte(0x204, (byte) 0x01);
        mega.writeMemoryByte(0x205, (byte) 0x02);
        mega.writeMemoryByte(0x206, (byte) 0x00);
        mega.writeMemoryByte(0x207, (byte) 0x00);
        mega.stepInstruction(); // I = 0x020000

        mega.executeOpcode(0x0302); // SPRW 2
        mega.executeOpcode(0x0402); // SPRH 2
        mega.executeOpcode(0x0901); // CCOL 1
        mega.executeOpcode(0x600A); // V0 = 10
        mega.executeOpcode(0x610A); // V1 = 10
        mega.executeOpcode(0xD010); // Draw at (10,10) -> no collision yet
        assertEquals(0, mega.getV(0xF));

        // Draw again with additive blend (0804) -> collides with CCOL 1!
        mega.executeOpcode(0x0804);
        mega.executeOpcode(0xD010);
        assertEquals(1, mega.getV(0xF));

        // Flip buffer with 00E0 -> front buffer now has red at (10,10)
        mega.executeOpcode(0x00E0);
        assertEquals(0xFFFF0000, mega.display().getMegaFrontArgb(10, 10));

        // Verify MegaChip Showcase ROM boots and renders colorful pixels
        Path showcase = Path.of("..", "roms", "megachip", "MegaChip Rainbow Showcase.mc8");
        if (Files.isRegularFile(showcase)) {
            byte[] romBytes = Files.readAllBytes(showcase);
            assertEquals(Chip8Variant.MEGA_CHIP, RomDatabase.lookup(romBytes).orElseThrow().recommendedVariant());
            mega.loadRom(romBytes);
            for (int f = 0; f < 5; f++) {
                mega.stepFrame(1000);
            }
            var snap = mega.display().snapshot();
            assertTrue(snap.megaMode());
            int colored = 0;
            for (int px : snap.argbPixels()) {
                if ((px & 0x00FFFFFF) != 0) colored++;
            }
            assertTrue(colored > 400, "MegaChip showcase should render > 400 colored pixels, got " + colored);
        }
    }

    @Test
    void testDvn8RendersDungeonAndMovesForwardOnKeyW() throws IOException {
        Path dvn8Path = Path.of("..", "roms", "superchip", "DVN8.sc8");
        if (Files.isRegularFile(dvn8Path)) {
            byte[] romBytes = Files.readAllBytes(dvn8Path);
            var meta = RomDatabase.lookup(romBytes).orElseThrow();
            assertTrue(meta.recommendedQuirks().jumpUsesV0(), "DVN8 requires BNNN + V0 jump tables");
            Chip8Machine machine = new Chip8Machine(new Chip8Config(
                meta.recommendedVariant(),
                meta.recommendedQuirks(),
                meta.recommendedCyclesPerFrame()
            ));
            machine.loadRom(romBytes);
            for (int f = 0; f < 60; f++) {
                machine.stepFrame(meta.recommendedCyclesPerFrame());
            }
            // Press 'W' (Octo key 0x5) to enter / step forward in dungeon
            machine.keypad().press(0x5);
            for (int f = 0; f < 10; f++) {
                machine.stepFrame(meta.recommendedCyclesPerFrame());
            }
            machine.keypad().release(0x5);
            for (int f = 0; f < 60; f++) {
                machine.stepFrame(meta.recommendedCyclesPerFrame());
            }
            int lit = 0;
            for (byte p : machine.display().copyPlane0()) {
                if (p != 0) lit++;
            }
            assertTrue(lit > 200, "DVN8 should stay rendered after pressing W, got " + lit + " lit pixels");
        }
    }
}
