package net.saga.console.chip8.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/**
 * Built-in SHA-1 ROM metadata database and parallel ROM directory scanner.
 * Uses JDK 27 {@link LazyConstant} (JEP 531) and {@link StructuredTaskScope} (JEP 533).
 */
public final class RomDatabase {
    private static final HexFormat HEX = HexFormat.of();

    public record RomMetadata(
        String sha1,
        String title,
        String author,
        String category,
        Chip8Variant recommendedVariant,
        Quirks recommendedQuirks,
        int recommendedCyclesPerFrame,
        String inputPresetId,
        String controlsHint
    ) {}

    public record ScannedRom(
        Path path,
        String displayName,
        String sha1,
        long sizeBytes,
        Optional<RomMetadata> metadata,
        Optional<String> sidecarNotes
    ) {
        public Chip8Variant suggestedVariant() {
            if (metadata.isPresent()) {
                return metadata.get().recommendedVariant();
            }
            String lower = path.getFileName().toString().toLowerCase();
            if (lower.endsWith(".mc8")) {
                return Chip8Variant.MEGA_CHIP;
            }
            if (lower.endsWith(".xo8")) {
                return Chip8Variant.XO_CHIP;
            }
            if (lower.endsWith(".sc8")) {
                return Chip8Variant.SUPER_CHIP_1_1;
            }
            return Chip8Variant.COSMAC_VIP;
        }
    }

    private static final LazyConstant<Map<String, RomMetadata>> KNOWN_ROMS = LazyConstant.of(() -> Map.ofEntries(
        entry(
            "ea9af3c09b0d9e265fcd92bcc5d51a2939fdf27a",
            "15 Puzzle", "Roger Ivie", "Puzzle",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 12,
            "cosmac", "Keys 0–F slide the matching tile into the empty slot."
        ),
        entry(
            "d40abc54374e4343639f993e897e00904ddf85d9",
            "Blinky", "Hans Christian Egeberg (1991)", "Arcade",
            Chip8Variant.SUPER_CHIP_1_1, Chip8Variant.SUPER_CHIP_1_1.defaultQuirks().withClipping(true), 20,
            "directional", "Pac-Man clone: 3=Up, E=Down, 7=Left, 8=Right (or use Directional preset)."
        ),
        entry(
            "193915dcde1365ae054c4eaa21a35baa27cd3356",
            "Breakout", "Carmelo Cortez (1979)", "Arcade",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 12,
            "cosmac", "4 (Q) = Move Left, 6 (E) = Move Right."
        ),
        entry(
            "f13766c14aeb02ad8d4d103cb5eadd282d20cddc",
            "Brix", "Andreas Gustafsson (1990)", "Arcade",
            Chip8Variant.CHIP_48, Chip8Variant.CHIP_48.defaultQuirks(), 15,
            "cosmac", "4 (Q) = Move Paddle Left, 6 (E) = Move Paddle Right."
        ),
        entry(
            "0d0cc129dad3c45ba672f85fec71a668232212cc",
            "Missile", "David Winter", "Shooter",
            Chip8Variant.CHIP_48, Chip8Variant.CHIP_48.defaultQuirks(), 12,
            "cosmac", "8 (S) = Fire missile at moving targets."
        ),
        entry(
            "607c4f7f4e4dce9f99d96b3182bfe7e88bb090ee",
            "Pong (1 Player)", "Classic", "Sports",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks().withDisplayWait(false), 12,
            "pong", "1 (1) = Paddle Up, 4 (Q) = Paddle Down."
        ),
        entry(
            "b232ef880bd6060fb45fa6effed7edf0ae95670e",
            "Pong (2 Player)", "Paul Vervalin (1990)", "Sports",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks().withDisplayWait(false), 12,
            "pong", "P1: 1/4 (1/Q) Up/Down • P2: C/D (4/R) Up/Down."
        ),
        entry(
            "5c28a5f85289c9d859f95fd5eadbdcb1c30bb08b",
            "Space Invaders", "David Winter", "Shooter",
            Chip8Variant.CHIP_48, Chip8Variant.CHIP_48.defaultQuirks(), 15,
            "space_invaders", "5 (W) = Start / Fire, 4 (Q) = Left, 6 (E) = Right."
        ),
        entry(
            "18b9d15f4c159e1f0ed58c2d8ec1d89325d3a3b6",
            "Tank", "Classic", "Action",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 14,
            "directional", "2=Down, 8=Up, 4=Left, 6=Right, 5=Fire."
        ),
        entry(
            "5f518084744bf3cb8733f6e5454dfd1634320563",
            "Tetris", "Fran Dachille (1991)", "Puzzle",
            Chip8Variant.CHIP_48, Chip8Variant.CHIP_48.defaultQuirks(), 12,
            "tetris", "4 (Q) = Rotate, 5 (W) = Left, 6 (E) = Right, 7 (A) = Drop."
        ),
        entry(
            "30f27e5cee5b325fd1681ee98a14de60bfbe951f",
            "1 - CHIP-8 Logo", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 15,
            "cosmac", "Verifies basic boot, font/sprite drawing, and loop execution."
        ),
        entry(
            "b9bbc12cee3f7b9d3b1f69161f7d7a2d86953379",
            "2 - IBM Logo", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 15,
            "cosmac", "Verifies 00E0, 6XNN, 7XNN, ANNN, DXYN, and 1NNN."
        ),
        entry(
            "b2dacf6d85785d6c2315ce449912c8a8a5954e2e",
            "3 - Corax+ Opcode Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 20,
            "cosmac", "Checks arithmetic, logic, skips, subroutines, BCD, and memory ops."
        ),
        entry(
            "55a6716dacc2f93dce3d39fb8d231083016a1cc0",
            "4 - Flags Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 20,
            "cosmac", "Thoroughly tests VF carry/borrow/shift edge cases including VF as target."
        ),
        entry(
            "e2149cb836131a142ca7e2dc2f2283381ae5faaa",
            "5 - Quirks Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 25,
            "cosmac", "Press 1=CHIP-8, 2=SUPER-CHIP, 3=XO-CHIP to validate variant quirks."
        ),
        entry(
            "455b9fc69cc06e2b5b72f7d1ac5f6c86ac349e77",
            "6 - Keypad Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 15,
            "cosmac", "Press 1=EX9E, 2=EXA1, 3=FX0A to interactively test all 16 keypad keys."
        ),
        entry(
            "b119651b5aa08557a85ca2ad5de3d1a86796b66b",
            "7 - Beep Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.COSMAC_VIP, Chip8Variant.COSMAC_VIP.defaultQuirks(), 15,
            "cosmac", "Hold any key to trigger the sound timer and test buzzer waveforms & pitch."
        ),
        entry(
            "477b3e09c43839ea5478b4f0e24536edab594f89",
            "8 - Scrolling Test", "Timendus Test Suite", "Test Suite",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 25,
            "cosmac", "Tests SUPER-CHIP & XO-CHIP scrolling (00CN/00DN/00FB/00FC) in lo-res & hi-res."
        ),
        entry(
            "627f01b20ce4d33f6df1aa88acb405a3a732bde0",
            "DVN8", "John Earnest (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 20,
            "octo", "128x64 3D dungeon crawler: W/A/S/D (or Arrows) move/turn, E/Space interact."
        ),
        entry(
            "e6af47843f0ecc3302027a3756dd7b389a15e437",
            "Black Rainbow", "John Earnest (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 20,
            "octo", "128x64 SUPER-CHIP arcade adventure: W/A/S/D or Arrows move, E/Space action."
        ),
        entry(
            "7a4a89870f2ab23c28024dd1c3dd52cf1af1ad00",
            "Octopeg", "Octojam (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 200,
            "octo", "128x64 Peggle clone: A/D or Left/Right aim launcher, E/Space fire ball."
        ),
        entry(
            "b76fbca2ec089c7e77f4a2f754db37854b99debc",
            "Rockto", "John Earnest (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 15,
            "octo", "128x64 Boulder Dash clone with scrolling: W/A/S/D or Arrows dig and dodge rocks."
        ),
        entry(
            "06a6692c92eb8077329b6d4e59d55479d60574a8",
            "Snake (Hi-Res)", "chip8Archive (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 15,
            "octo", "128x64 classic Snake: W/A/S/D or Arrows steer around the playfield."
        ),
        entry(
            "64536d549c986e9edf25de9fa89db60d2ade85c0",
            "Sub-Terr8nia", "Octojam (CC0)", "SUPER-CHIP",
            Chip8Variant.SUPER_CHIP_1_1, Quirks.octoSuperChip(), 60,
            "octo", "128x64 lunar lander: W/A/S/D or Arrows thrust and steer carefully."
        ),
        entry(
            "518c1d40f5d768ee49d2b7951d998588ef8238ba",
            "Wonky Pong", "Octojam IV (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 100,
            "octo", "4-color XO-CHIP Pong with programmable pattern audio."
        ),
        entry(
            "a98ed56f88f11156871d871d9200fc4bb45190a4",
            "Super Octo Track XO", "Octojam (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks().withMemoryIncrement(Quirks.MemoryIncrement.NONE), 100,
            "cosmac", "XO-CHIP music tracker! Tests 16-byte audio pattern buffer (F002) & pitch (FX3A)."
        ),
        entry(
            "33abb5f1ba7db3166636911c6cfa81a5ce5b861c",
            "Octo Paint", "chip8Archive (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 30,
            "octo", "4-color XO-CHIP drawing tool: W/A/S/D or Arrows move cursor, keys cycle planes/colors."
        ),
        entry(
            "1b6dcf8c02ea0b89a4f04ce28e7c39a5e7a513d6",
            "SK8 H8 1988", "Octojam (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 20,
            "octo", "4-color XO-CHIP skateboarding action with chiptune audio."
        ),
        entry(
            "440c5fbe9f5f840e76c308738fb0d37772d66674",
            "Super NeatBoy", "John Earnest (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks().withClipping(true), 1000,
            "octo", "128x64 4-color XO-CHIP platformer (Octojam 7): W/A/S/D or Arrows move, E/Space jump."
        ),
        entry(
            "8ebf74e790e58a8d5a7beff598bb32ed7eeeabf7",
            "Skyward", "Octojam (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks().withMemoryIncrement(Quirks.MemoryIncrement.NONE), 1000,
            "octo", "54 KB 4-color hi-res XO-CHIP climber showcasing 64 KB RAM & pattern audio."
        ),
        entry(
            "c606d52970b86edcca4e87e9f6fae4b1ccbbbb0f",
            "Chicken Scratch", "Octojam (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 500,
            "octo", "17 KB 4-color XO-CHIP Pac-Man-inspired arcade game."
        ),
        entry(
            "8b2fc2e08830b8a9e604d11c9b319e2cc0a581b3",
            "T8NKS", "Octojam (CC0)", "XO-CHIP",
            Chip8Variant.XO_CHIP, Chip8Variant.XO_CHIP.defaultQuirks(), 1000,
            "octo", "32 KB 4-color XO-CHIP artillery game with destructible terrain & audio."
        ),
        entry(
            "4b3f905becab70927c46844da81b66db105adc6d",
            "MegaChip Rainbow Showcase", "CHIP-8 Studio Demo", "Mega-Chip",
            Chip8Variant.MEGA_CHIP, Chip8Variant.MEGA_CHIP.defaultQuirks(), 1000,
            "cosmac", "256x192 ARGB8 palette demo: additive & 50% alpha blending, fonts, and 8-bit chiptune PCM music."
        ),
        entry(
            "1fccecbde6994057f5373a073446ddce449c9d54",
            "MegaChip Enable/Disable Test", "Oxiti8 (2022)", "Mega-Chip",
            Chip8Variant.MEGA_CHIP, Chip8Variant.MEGA_CHIP.defaultQuirks(), 1000,
            "cosmac", "Tests 0011 (MEGAON) and 0010 (MEGAOFF) transitions."
        ),
        entry(
            "c6297958827592933cf74f8760725906ce7ee139",
            "MegaChip Font & Wrapping Test", "Oxiti8 (2022)", "Mega-Chip",
            Chip8Variant.MEGA_CHIP, Chip8Variant.MEGA_CHIP.defaultQuirks(), 1000,
            "cosmac", "Tests built-in font rendering (palette 254) and 256-pixel wrapping in Mega-Chip mode."
        ),
        entry(
            "22f9ed2bd2eb17aa25ef74062755ee79362b9c52",
            "MegaChip FX0A Update Test", "Oxiti8 (2022)", "Mega-Chip",
            Chip8Variant.MEGA_CHIP, Chip8Variant.MEGA_CHIP.defaultQuirks(), 1000,
            "cosmac", "Verifies FX0A forces a screen update before waiting for keypress."
        ),
        entry(
            "b6f8729dd8535e7ed4646897e139ab80cfc25fc6",
            "MegaChip Scroll Quirk Test", "Oxiti8 (2022)", "Mega-Chip",
            Chip8Variant.MEGA_CHIP, Chip8Variant.MEGA_CHIP.defaultQuirks(), 1000,
            "octo", "W/A/S/D (5/7/8/9): 'PR' stays still on authentic MEGA-CHIP8; 'OK' only appears on quirky Mega8 scroll-flip."
        )
    ));

    private RomDatabase() {}

    private static Map.Entry<String, RomMetadata> entry(
        String sha1,
        String title,
        String author,
        String category,
        Chip8Variant variant,
        Quirks quirks,
        int cpf,
        String inputPresetId,
        String controlsHint
    ) {
        return Map.entry(sha1, new RomMetadata(sha1, title, author, category, variant, quirks, cpf, inputPresetId, controlsHint));
    }

    public static String sha1Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HEX.formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    public static Optional<RomMetadata> lookup(byte[] romBytes) {
        return Optional.ofNullable(KNOWN_ROMS.get().get(sha1Hex(romBytes)));
    }

    public static Optional<RomMetadata> lookupByHash(String sha1) {
        return Optional.ofNullable(KNOWN_ROMS.get().get(sha1.toLowerCase()));
    }

    /**
     * Scans one or more directories concurrently for .ch8/.sc8/.xo8 ROMs using JDK 27
     * {@link StructuredTaskScope} (JEP 533).
     */
    public static List<ScannedRom> scanDirectories(List<Path> directories) {
        List<Path> romPaths = new ArrayList<>();
        for (Path dir : directories) {
            if (dir == null || !Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir, 3)) {
                walk.filter(Files::isRegularFile)
                    .filter(RomDatabase::isRomFile)
                    .forEach(romPaths::add);
            } catch (IOException ignored) {
                // Skip unreadable directories
            }
        }

        if (romPaths.isEmpty()) {
            return List.of();
        }

        try (var scope = StructuredTaskScope.<ScannedRom>open()) {
            List<StructuredTaskScope.Subtask<ScannedRom>> tasks = romPaths.stream()
                .map(path -> scope.fork(() -> inspectRom(path)))
                .toList();
            scope.join();

            return tasks.stream()
                .filter(t -> t.state() == StructuredTaskScope.Subtask.State.SUCCESS)
                .map(StructuredTaskScope.Subtask::get)
                .sorted(Comparator.comparing(ScannedRom::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (ExecutionException e) {
            return List.of();
        }
    }

    private static ScannedRom inspectRom(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String sha1 = sha1Hex(bytes);
        Optional<RomMetadata> meta = lookupByHash(sha1);
        Optional<String> notes = readSidecarNotes(path);
        String fileName = path.getFileName().toString();
        String baseName = fileName.replaceFirst("\\.(ch8|sc8|xo8|mc8)$", "");
        String displayName = meta.map(RomMetadata::title).orElse(baseName);
        return new ScannedRom(path, displayName, sha1, bytes.length, meta, notes);
    }

    private static Optional<String> readSidecarNotes(Path romPath) {
        String name = romPath.getFileName().toString();
        String txtName = name.replaceFirst("\\.(ch8|sc8|xo8|mc8)$", ".txt");
        Path txtPath = romPath.resolveSibling(txtName);
        if (Files.isRegularFile(txtPath)) {
            try {
                return Optional.of(Files.readString(txtPath).trim());
            } catch (IOException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static boolean isRomFile(Path path) {
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".ch8") || lower.endsWith(".sc8") || lower.endsWith(".xo8") || lower.endsWith(".mc8");
    }
}
