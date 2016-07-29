package net.saga.console.chip8;

public final class Input {

    private Input() {}
    
    private static int KEYS = 0;
    
    public static void press(int i) {
        KEYS = 0xF & i;
    }
   
    public static void unpress() {
        KEYS = 0;
    }
   
    /**
     * returns the currently pressed keys 
     * 
     * @return the int value of the key pressed
     */
    public static int read() {
        return KEYS;
    }
    
}
