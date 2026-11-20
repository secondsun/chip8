package net.saga.console.chip8.core;

import java.util.Arrays;

/**
 * Multi-mode framebuffer supporting:
 * - Standard CHIP-8 (64x32, 1-bit)
 * - SUPER-CHIP (128x64, 1-bit)
 * - XO-CHIP (64x32 or 128x64, 2-bitplane / 4-color)
 * - Mega-Chip (256x192, double-buffered 256-color ARGB8 palette + alpha/blend modes)
 */
public final class DisplayBuffer {
    public static final int LO_RES_WIDTH = 64;
    public static final int LO_RES_HEIGHT = 32;
    public static final int HI_RES_WIDTH = 128;
    public static final int HI_RES_HEIGHT = 64;
    public static final int MEGA_WIDTH = 256;
    public static final int MEGA_HEIGHT = 192;

    private final byte[] plane0 = new byte[HI_RES_WIDTH * HI_RES_HEIGHT];
    private final byte[] plane1 = new byte[HI_RES_WIDTH * HI_RES_HEIGHT];
    private boolean hiRes = false;
    private int activePlanes = 0b01;

    // Mega-Chip 256x192 ARGB8 double-buffered state
    private boolean megaMode = false;
    private final int[] megaPalette = new int[256];
    private final int[] megaBackArgb = new int[MEGA_WIDTH * MEGA_HEIGHT];
    private final byte[] megaBackIndices = new byte[MEGA_WIDTH * MEGA_HEIGHT];
    private final int[] megaFrontArgb = new int[MEGA_WIDTH * MEGA_HEIGHT];
    private int megaSpriteWidth = 8;
    private int megaSpriteHeight = 8;
    private int megaScreenAlpha = 0xFF;
    private int megaBlendMode = 0;
    private int megaCollisionColor = 0xFF;

    public DisplayBuffer() {
        resetMegaPalette();
    }

    public int width() {
        if (megaMode) return MEGA_WIDTH;
        return hiRes ? HI_RES_WIDTH : LO_RES_WIDTH;
    }

    public int height() {
        if (megaMode) return MEGA_HEIGHT;
        return hiRes ? HI_RES_HEIGHT : LO_RES_HEIGHT;
    }

    public boolean isHiRes() {
        return hiRes;
    }

    public void setHiRes(boolean hiRes) {
        this.hiRes = hiRes;
    }

    public boolean isMegaMode() {
        return megaMode;
    }

    public void setMegaMode(boolean enabled) {
        this.megaMode = enabled;
        if (enabled) {
            this.hiRes = false;
            this.activePlanes = 0b01;
            Arrays.fill(megaBackArgb, 0xFF000000);
            Arrays.fill(megaBackIndices, (byte) 0);
            Arrays.fill(megaFrontArgb, 0xFF000000);
        } else {
            this.hiRes = false;
            Arrays.fill(plane0, (byte) 0);
            Arrays.fill(plane1, (byte) 0);
        }
    }

    public int activePlanes() {
        return activePlanes;
    }

    public void setActivePlanes(int mask) {
        this.activePlanes = mask & 0b11;
    }

    public int megaSpriteWidth() {
        return megaSpriteWidth;
    }

    public void setMegaSpriteWidth(int nn) {
        int v = nn & 0xFF;
        this.megaSpriteWidth = (v == 0) ? 256 : v;
    }

    public int megaSpriteHeight() {
        return megaSpriteHeight;
    }

    public void setMegaSpriteHeight(int nn) {
        int v = nn & 0xFF;
        this.megaSpriteHeight = (v == 0) ? 256 : v;
    }

    public int megaScreenAlpha() {
        return megaScreenAlpha;
    }

    public void setMegaScreenAlpha(int nn) {
        this.megaScreenAlpha = nn & 0xFF;
    }

    public int megaBlendMode() {
        return megaBlendMode;
    }

    public void setMegaBlendMode(int mode) {
        this.megaBlendMode = mode & 0x0F;
    }

    public int megaCollisionColor() {
        return megaCollisionColor;
    }

    public void setMegaCollisionColor(int colorIndex) {
        this.megaCollisionColor = colorIndex & 0xFF;
    }

    public void loadMegaPalette(byte[] memory, int startAddress, int count) {
        int entries = Math.min(count & 0xFF, 254); // 0 and 255 are immutable transparent entries
        int mask = memory.length - 1;
        for (int i = 0; i < entries; i++) {
            int base = startAddress + i * 4;
            int a = memory[base & mask] & 0xFF;
            int r = memory[(base + 1) & mask] & 0xFF;
            int g = memory[(base + 2) & mask] & 0xFF;
            int b = memory[(base + 3) & mask] & 0xFF;
            megaPalette[i + 1] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        megaPalette[0] = 0x00000000;
        megaPalette[255] = 0x00FFFFFF;
    }

    public int getMegaPaletteColor(int index) {
        return megaPalette[index & 0xFF];
    }

    public void flipMegaBuffer(boolean clearBack) {
        System.arraycopy(megaBackArgb, 0, megaFrontArgb, 0, megaBackArgb.length);
        if (clearBack) {
            Arrays.fill(megaBackArgb, 0xFF000000);
            Arrays.fill(megaBackIndices, (byte) 0);
        }
    }

    public void reset() {
        Arrays.fill(plane0, (byte) 0);
        Arrays.fill(plane1, (byte) 0);
        this.hiRes = false;
        this.activePlanes = 0b01;
        this.megaMode = false;
        this.megaSpriteWidth = 8;
        this.megaSpriteHeight = 8;
        this.megaScreenAlpha = 0xFF;
        this.megaBlendMode = 0;
        this.megaCollisionColor = 0xFF;
        resetMegaPalette();
        Arrays.fill(megaBackArgb, 0xFF000000);
        Arrays.fill(megaBackIndices, (byte) 0);
        Arrays.fill(megaFrontArgb, 0xFF000000);
    }

    private void resetMegaPalette() {
        Arrays.fill(megaPalette, 0xFF000000);
        megaPalette[0] = 0x00000000;
        megaPalette[254] = 0xFFFFFFFF; // Built-in font color in Mega-Chip mode
        megaPalette[255] = 0x00FFFFFF; // Transparent sentinel
    }

    public void clearSelectedPlanes() {
        if (megaMode) {
            flipMegaBuffer(true);
            return;
        }
        if ((activePlanes & 0b01) != 0) {
            Arrays.fill(plane0, (byte) 0);
        }
        if ((activePlanes & 0b10) != 0) {
            Arrays.fill(plane1, (byte) 0);
        }
    }

    public void scrollDown(int lines) {
        if (lines <= 0) return;
        if (megaMode) {
            int shift = Math.min(lines, MEGA_HEIGHT);
            for (int y = MEGA_HEIGHT - 1; y >= shift; y--) {
                System.arraycopy(megaBackArgb, (y - shift) * MEGA_WIDTH, megaBackArgb, y * MEGA_WIDTH, MEGA_WIDTH);
                System.arraycopy(megaBackIndices, (y - shift) * MEGA_WIDTH, megaBackIndices, y * MEGA_WIDTH, MEGA_WIDTH);
            }
            Arrays.fill(megaBackArgb, 0, shift * MEGA_WIDTH, 0xFF000000);
            Arrays.fill(megaBackIndices, 0, shift * MEGA_WIDTH, (byte) 0);
            return;
        }
        int w = width();
        int h = height();
        int shift = Math.min(lines, h);
        if ((activePlanes & 0b01) != 0) scrollPlaneDown(plane0, w, h, shift);
        if ((activePlanes & 0b10) != 0) scrollPlaneDown(plane1, w, h, shift);
    }

    public void scrollUp(int lines) {
        if (lines <= 0) return;
        if (megaMode) {
            int shift = Math.min(lines, MEGA_HEIGHT);
            for (int y = 0; y < MEGA_HEIGHT - shift; y++) {
                System.arraycopy(megaBackArgb, (y + shift) * MEGA_WIDTH, megaBackArgb, y * MEGA_WIDTH, MEGA_WIDTH);
                System.arraycopy(megaBackIndices, (y + shift) * MEGA_WIDTH, megaBackIndices, y * MEGA_WIDTH, MEGA_WIDTH);
            }
            Arrays.fill(megaBackArgb, (MEGA_HEIGHT - shift) * MEGA_WIDTH, MEGA_HEIGHT * MEGA_WIDTH, 0xFF000000);
            Arrays.fill(megaBackIndices, (MEGA_HEIGHT - shift) * MEGA_WIDTH, MEGA_HEIGHT * MEGA_WIDTH, (byte) 0);
            return;
        }
        int w = width();
        int h = height();
        int shift = Math.min(lines, h);
        if ((activePlanes & 0b01) != 0) scrollPlaneUp(plane0, w, h, shift);
        if ((activePlanes & 0b10) != 0) scrollPlaneUp(plane1, w, h, shift);
    }

    public void scrollRight(int pixels) {
        if (megaMode) {
            int shift = Math.min(pixels, MEGA_WIDTH);
            for (int y = 0; y < MEGA_HEIGHT; y++) {
                int row = y * MEGA_WIDTH;
                for (int x = MEGA_WIDTH - 1; x >= shift; x--) {
                    megaBackArgb[row + x] = megaBackArgb[row + x - shift];
                    megaBackIndices[row + x] = megaBackIndices[row + x - shift];
                }
                Arrays.fill(megaBackArgb, row, row + shift, 0xFF000000);
                Arrays.fill(megaBackIndices, row, row + shift, (byte) 0);
            }
            return;
        }
        int w = width();
        int h = height();
        int shift = Math.min(pixels, w);
        if ((activePlanes & 0b01) != 0) scrollPlaneRight(plane0, w, h, shift);
        if ((activePlanes & 0b10) != 0) scrollPlaneRight(plane1, w, h, shift);
    }

    public void scrollLeft(int pixels) {
        if (megaMode) {
            int shift = Math.min(pixels, MEGA_WIDTH);
            for (int y = 0; y < MEGA_HEIGHT; y++) {
                int row = y * MEGA_WIDTH;
                for (int x = 0; x < MEGA_WIDTH - shift; x++) {
                    megaBackArgb[row + x] = megaBackArgb[row + x + shift];
                    megaBackIndices[row + x] = megaBackIndices[row + x + shift];
                }
                Arrays.fill(megaBackArgb, row + MEGA_WIDTH - shift, row + MEGA_WIDTH, 0xFF000000);
                Arrays.fill(megaBackIndices, row + MEGA_WIDTH - shift, row + MEGA_WIDTH, (byte) 0);
            }
            return;
        }
        int w = width();
        int h = height();
        int shift = Math.min(pixels, w);
        if ((activePlanes & 0b01) != 0) scrollPlaneLeft(plane0, w, h, shift);
        if ((activePlanes & 0b10) != 0) scrollPlaneLeft(plane1, w, h, shift);
    }

    /**
     * Draws a Mega-Chip sprite (either 1-bit font when startAddress < 0x100, or byte-per-pixel palette sprite).
     *
     * @return true if any non-transparent pixel collided with {@code megaCollisionColor}.
     */
    public boolean drawMegaSprite(byte[] memory, int startAddress, int rawX, int rawY, int heightNibble) {
        int mask = memory.length - 1;
        int originX = rawX & 0xFF;
        int originY = rawY & 0xFF;

        // Font sprites live below 0x100 and render 1-bit-per-pixel using palette 254 (default white)
        if (startAddress < 0x100) {
            int rows = (heightNibble == 0) ? 10 : heightNibble;
            int fontColor = megaPalette[254] != 0 ? megaPalette[254] : 0xFFFFFFFF;
            boolean collision = false;
            for (int r = 0; r < rows; r++) {
                int py = (originY + r) & 0xFF;
                if (py >= MEGA_HEIGHT) continue;
                int bits = memory[(startAddress + r) & mask] & 0xFF;
                for (int c = 0; c < 8; c++) {
                    if ((bits & (0x80 >>> c)) == 0) continue;
                    int px = (originX + c) & 0xFF;
                    int idx = py * MEGA_WIDTH + px;
                    if ((megaBackIndices[idx] & 0xFF) == megaCollisionColor) {
                        collision = true;
                    }
                    megaBackArgb[idx] = blendPixel(megaBackArgb[idx], fontColor, megaBlendMode);
                    megaBackIndices[idx] = (byte) 254;
                }
            }
            return collision;
        }

        // Byte-per-pixel Mega-Chip sprite (width x height from 03NN / 04NN)
        boolean collision = false;
        int sw = megaSpriteWidth;
        int sh = megaSpriteHeight;
        for (int r = 0; r < sh; r++) {
            int py = (originY + r) & 0xFF;
            if (py >= MEGA_HEIGHT) continue;
            int rowBase = startAddress + r * sw;
            int screenRow = py * MEGA_WIDTH;
            for (int c = 0; c < sw; c++) {
                int palIdx = memory[(rowBase + c) & mask] & 0xFF;
                if (palIdx == 0 || palIdx == 255) continue;
                int srcArgb = megaPalette[palIdx];
                if (srcArgb == 0) continue;

                int px = (originX + c) & 0xFF;
                int dstIdx = screenRow + px;
                if ((megaBackIndices[dstIdx] & 0xFF) == megaCollisionColor) {
                    collision = true;
                }
                megaBackArgb[dstIdx] = blendPixel(megaBackArgb[dstIdx], srcArgb, megaBlendMode);
                megaBackIndices[dstIdx] = (byte) palIdx;
            }
        }
        return collision;
    }

    private static int blendPixel(int dst, int src, int mode) {
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;

        return switch (mode) {
            case 1 -> packRgb((sr + dr * 3) >>> 2, (sg + dg * 3) >>> 2, (sb + db * 3) >>> 2); // 25%
            case 2 -> packRgb((sr + dr) >>> 1, (sg + dg) >>> 1, (sb + db) >>> 1);             // 50%
            case 3 -> packRgb((sr * 3 + dr) >>> 2, (sg * 3 + dg) >>> 2, (sb * 3 + db) >>> 2); // 75%
            case 4 -> packRgb(Math.min(255, sr + dr), Math.min(255, sg + dg), Math.min(255, sb + db)); // Additive
            case 5 -> packRgb((sr * dr) / 255, (sg * dg) / 255, (sb * db) / 255);             // Multiply
            default -> {
                int sa = (src >>> 24) & 0xFF;
                if (sa == 0xFF || sa == 0) {
                    yield 0xFF000000 | (sr << 16) | (sg << 8) | sb;
                }
                int inv = 255 - sa;
                yield packRgb(
                    (sr * sa + dr * inv) / 255,
                    (sg * sa + dg * inv) / 255,
                    (sb * sa + db * inv) / 255
                );
            }
        };
    }

    private static int packRgb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public boolean drawSprite(
        byte[] memory,
        int startAddress,
        int rawX,
        int rawY,
        int heightNibble,
        boolean clipping,
        boolean allowTallSprite
    ) {
        if (megaMode) {
            return drawMegaSprite(memory, startAddress, rawX, rawY, heightNibble);
        }

        int w = width();
        int h = height();
        int originX = Math.floorMod(rawX, w);
        int originY = Math.floorMod(rawY, h);

        boolean tall16 = (heightNibble == 0) && allowTallSprite;
        int spriteWidth = tall16 ? 16 : 8;
        int spriteHeight = tall16 ? 16 : heightNibble;
        if (spriteHeight == 0) {
            return false;
        }

        int bytesPerPlane = tall16 ? 32 : spriteHeight;
        int memOffset = startAddress;
        boolean collision = false;

        for (int plane = 0; plane < 2; plane++) {
            if ((activePlanes & (1 << plane)) == 0) {
                continue;
            }
            byte[] target = (plane == 0) ? plane0 : plane1;

            for (int row = 0; row < spriteHeight; row++) {
                int py = originY + row;
                if (clipping) {
                    if (py >= h) break;
                } else {
                    py %= h;
                }

                int rowBits;
                if (tall16) {
                    int b0 = memory[(memOffset + row * 2) % memory.length] & 0xFF;
                    int b1 = memory[(memOffset + row * 2 + 1) % memory.length] & 0xFF;
                    rowBits = (b0 << 8) | b1;
                } else {
                    rowBits = memory[(memOffset + row) % memory.length] & 0xFF;
                }

                for (int col = 0; col < spriteWidth; col++) {
                    int bitMask = 1 << (spriteWidth - 1 - col);
                    if ((rowBits & bitMask) == 0) continue;
                    int px = originX + col;
                    if (clipping) {
                        if (px >= w) break;
                    } else {
                        px %= w;
                    }
                    int idx = py * w + px;
                    if (target[idx] != 0) collision = true;
                    target[idx] ^= 1;
                }
            }
            memOffset += bytesPerPlane;
        }
        return collision;
    }

    public int getPixelColorIndex(int x, int y) {
        if (megaMode) {
            return megaBackIndices[y * MEGA_WIDTH + x] & 0xFF;
        }
        int w = width();
        int idx = y * w + x;
        return (plane0[idx] & 1) | ((plane1[idx] & 1) << 1);
    }

    public int getMegaFrontArgb(int x, int y) {
        return megaFrontArgb[y * MEGA_WIDTH + x];
    }

    public byte getSpriteRow(int x, int y) {
        int w = hiRes ? HI_RES_WIDTH : LO_RES_WIDTH;
        int h = hiRes ? HI_RES_HEIGHT : LO_RES_HEIGHT;
        int mx = Math.floorMod(x, w);
        int my = Math.floorMod(y, h);
        int result = 0;
        for (int bit = 0; bit < 8; bit++) {
            int px = (mx + bit) % w;
            result = (result << 1) | (plane0[my * w + px] & 1);
        }
        return (byte) result;
    }

    public byte[] copyPlane0() {
        return Arrays.copyOf(plane0, (hiRes ? HI_RES_WIDTH : LO_RES_WIDTH) * (hiRes ? HI_RES_HEIGHT : LO_RES_HEIGHT));
    }

    public DisplaySnapshot snapshot() {
        if (megaMode) {
            return new DisplaySnapshot(
                MEGA_WIDTH,
                MEGA_HEIGHT,
                true,
                true,
                new byte[0],
                Arrays.copyOf(megaFrontArgb, megaFrontArgb.length)
            );
        }
        int w = width();
        int h = height();
        int len = w * h;
        byte[] colors = new byte[len];
        for (int i = 0; i < len; i++) {
            colors[i] = (byte) ((plane0[i] & 1) | ((plane1[i] & 1) << 1));
        }
        return new DisplaySnapshot(w, h, hiRes, false, colors, null);
    }

    public byte[] copyRawPlane0() {
        return Arrays.copyOf(plane0, plane0.length);
    }

    public byte[] copyRawPlane1() {
        return Arrays.copyOf(plane1, plane1.length);
    }

    public MegaDisplayState copyMegaState() {
        if (!megaMode) {
            return null;
        }
        return new MegaDisplayState(
            megaSpriteWidth,
            megaSpriteHeight,
            megaScreenAlpha,
            megaBlendMode,
            megaCollisionColor,
            Arrays.copyOf(megaPalette, megaPalette.length),
            Arrays.copyOf(megaBackArgb, megaBackArgb.length),
            Arrays.copyOf(megaBackIndices, megaBackIndices.length),
            Arrays.copyOf(megaFrontArgb, megaFrontArgb.length)
        );
    }

    public void restoreState(boolean hiRes, int activePlanes, byte[] rawPlane0, byte[] rawPlane1, MegaDisplayState mega) {
        this.hiRes = hiRes;
        this.activePlanes = activePlanes & 0b11;
        System.arraycopy(rawPlane0, 0, this.plane0, 0, Math.min(rawPlane0.length, this.plane0.length));
        System.arraycopy(rawPlane1, 0, this.plane1, 0, Math.min(rawPlane1.length, this.plane1.length));
        if (mega != null) {
            this.megaMode = true;
            this.megaSpriteWidth = mega.spriteWidth();
            this.megaSpriteHeight = mega.spriteHeight();
            this.megaScreenAlpha = mega.screenAlpha();
            this.megaBlendMode = mega.blendMode();
            this.megaCollisionColor = mega.collisionColor();
            System.arraycopy(mega.palette(), 0, this.megaPalette, 0, this.megaPalette.length);
            System.arraycopy(mega.backArgb(), 0, this.megaBackArgb, 0, this.megaBackArgb.length);
            System.arraycopy(mega.backIndices(), 0, this.megaBackIndices, 0, this.megaBackIndices.length);
            System.arraycopy(mega.frontArgb(), 0, this.megaFrontArgb, 0, this.megaFrontArgb.length);
        } else {
            this.megaMode = false;
        }
    }

    private static void scrollPlaneDown(byte[] plane, int w, int h, int shift) {
        for (int y = h - 1; y >= shift; y--) {
            System.arraycopy(plane, (y - shift) * w, plane, y * w, w);
        }
        Arrays.fill(plane, 0, shift * w, (byte) 0);
    }

    private static void scrollPlaneUp(byte[] plane, int w, int h, int shift) {
        for (int y = 0; y < h - shift; y++) {
            System.arraycopy(plane, (y + shift) * w, plane, y * w, w);
        }
        Arrays.fill(plane, (h - shift) * w, h * w, (byte) 0);
    }

    private static void scrollPlaneRight(byte[] plane, int w, int h, int shift) {
        for (int y = 0; y < h; y++) {
            int rowStart = y * w;
            for (int x = w - 1; x >= shift; x--) {
                plane[rowStart + x] = plane[rowStart + x - shift];
            }
            Arrays.fill(plane, rowStart, rowStart + shift, (byte) 0);
        }
    }

    private static void scrollPlaneLeft(byte[] plane, int w, int h, int shift) {
        for (int y = 0; y < h; y++) {
            int rowStart = y * w;
            for (int x = 0; x < w - shift; x++) {
                plane[rowStart + x] = plane[rowStart + x + shift];
            }
            Arrays.fill(plane, rowStart + w - shift, rowStart + w, (byte) 0);
        }
    }

    public record MegaDisplayState(
        int spriteWidth,
        int spriteHeight,
        int screenAlpha,
        int blendMode,
        int collisionColor,
        int[] palette,
        int[] backArgb,
        byte[] backIndices,
        int[] frontArgb
    ) {}

    public record DisplaySnapshot(
        int width,
        int height,
        boolean hiRes,
        boolean megaMode,
        byte[] pixels,
        int[] argbPixels
    ) {
        public int pixelAt(int x, int y) {
            return pixels[y * width + x] & 0b11;
        }
    }
}
