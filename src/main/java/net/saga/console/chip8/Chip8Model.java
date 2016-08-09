/*
 * The MIT License
 *
 * Copyright 2016 summers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software")), to deal
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

import static java.lang.Integer.toHexString;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author summers
 */
public class Chip8Model extends DefaultTableModel {

    private static final long serialVersionUID = 1L;

    public Chip8Model() {
        super(20, 2);
        super.setColumnIdentifiers(new String[]{"", "V"});
        super.setValueAt("V0", 0, 0);
        super.setValueAt("V1", 1, 0);
        super.setValueAt("V2", 2, 0);
        super.setValueAt("V3", 3, 0);
        super.setValueAt("V4", 4, 0);
        super.setValueAt("V5", 5, 0);
        super.setValueAt("V6", 6, 0);
        super.setValueAt("V7", 7, 0);
        super.setValueAt("V8", 8, 0);
        super.setValueAt("V9", 9, 0);
        super.setValueAt("VA", 10, 0);
        super.setValueAt("VB", 11, 0);
        super.setValueAt("VC", 12, 0);
        super.setValueAt("VD", 13, 0);
        super.setValueAt("VE", 14, 0);
        super.setValueAt("VF", 15, 0);
        super.setValueAt("", 16, 0);
        super.setValueAt("I", 17, 0);
        super.setValueAt("PC", 18, 0);
        super.setValueAt("SP", 19, 0);
    }
    
    public void update(Chip8 chip8) {
        setValueAt(toHexString(chip8.getV0()), 0, 1);
        setValueAt(toHexString(chip8.getV1()), 1, 1);
        setValueAt(toHexString(chip8.getV2()), 2, 1);
        setValueAt(toHexString(chip8.getV3()), 3, 1);
        setValueAt(toHexString(chip8.getV4()), 4, 1);
        setValueAt(toHexString(chip8.getV5()), 5, 1);
        setValueAt(toHexString(chip8.getV6()), 6, 1);
        setValueAt(toHexString(chip8.getV7()), 7, 1);
        setValueAt(toHexString(chip8.getV8()), 8, 1);
        setValueAt(toHexString(chip8.getV9()), 9, 1);
        setValueAt(toHexString(chip8.getVA()), 10, 1);
        setValueAt(toHexString(chip8.getVB()), 11, 1);
        setValueAt(toHexString(chip8.getVC()), 12, 1);
        setValueAt(toHexString(chip8.getVD()), 13, 1);
        setValueAt(toHexString(chip8.getVE()), 14, 1);
        setValueAt(toHexString(chip8.getVF()), 15, 1);
        setValueAt("", 16, 1);
        setValueAt(toHexString(chip8.getiRegister()), 17, 1);
        setValueAt(toHexString(chip8.getPC()), 18, 1);
        setValueAt(toHexString(chip8.getSP()), 19, 1);
    }
    

}
