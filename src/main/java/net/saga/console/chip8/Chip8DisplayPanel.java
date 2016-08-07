/*
 * The MIT License
 *
 * Copyright 2016 summers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.saga.console.chip8;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 *
 * @author summers
 */
public class Chip8DisplayPanel extends JPanel {
    
    private static final long serialVersionUID = 0x203920L;

    private Chip8 chip8 = new Chip8();
    
    @Override
    public void paint(Graphics g) {
        int width = super.getWidth();
        int height = super.getHeight();
        int pixelWidth = width / 64;
        int pixelHeight = height / 32;
        byte[] video = chip8.getScreen();
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                if (video[y * 64 + x]  == 0) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(x * pixelWidth, y*pixelHeight , pixelWidth, pixelHeight);
            }
        }
    }

    public void setChip8(Chip8 chip8) {
        this.chip8 = chip8;
    }
    
    
    
    
}
