package net.saga.console.chip8.core.input;

import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.InputDevicePlugin;
import de.gurkenlabs.input4j.InputDevices;
import de.gurkenlabs.input4j.components.Axis;
import de.gurkenlabs.input4j.components.Button;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLStdinc;

/**
 * Cross-platform external gamepad manager.
 * Uses LWJGL SDL3 ({@code SDL_INIT_GAMEPAD}) as the primary backend (first-class Apple Silicon macOS,
 * Windows, and Linux controller database with hot-plugging), falling back to {@code input4j} (Java FFM)
 * if SDL3 is unavailable.
 */
public final class GamepadManager implements AutoCloseable {
    private static final float AXIS_DEADZONE = 0.45f;
    private static final int SDL_AXIS_DEADZONE = 14_000;
    private static final int SDL_TRIGGER_THRESHOLD = 12_000;

    public record GamepadStatus(boolean available, List<String> connectedNames, String statusMessage) {}

    private final KeypadState keypad;
    private final AtomicReference<Map<String, Integer>> bindings = new AtomicReference<>(InputPreset.COSMAC.gamepadBindings());
    private final AtomicReference<Consumer<String>> captureListener = new AtomicReference<>();
    private final AtomicReference<GamepadStatus> status = new AtomicReference<>(
        new GamepadStatus(false, List.of(), "Initializing gamepad subsystem...")
    );
    private final AtomicBoolean running = new AtomicBoolean(false);

    private InputDevicePlugin input4jPlugin;
    private Thread pollThread;

    public GamepadManager(KeypadState keypad) {
        this.keypad = keypad;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        pollThread = Thread.ofPlatform().daemon(true).name("chip8-gamepad-poll").start(this::pollLoop);
    }

    public void setBindings(Map<String, Integer> newBindings) {
        if (newBindings != null) {
            bindings.set(Map.copyOf(newBindings));
        }
    }

    public Map<String, Integer> getBindings() {
        return bindings.get();
    }

    /**
     * Registers a one-shot callback invoked with the next pressed gamepad control token (e.g. "DPAD_UP", "BTN_0").
     */
    public void captureNextControl(Consumer<String> onCaptured) {
        captureListener.set(onCaptured);
    }

    public void cancelCapture() {
        captureListener.set(null);
    }

    public GamepadStatus getStatus() {
        return status.get();
    }

    private void pollLoop() {
        if (tryRunSdl3Loop()) {
            return;
        }
        runInput4jFallbackLoop();
    }

    private boolean tryRunSdl3Loop() {
        boolean initialized;
        try {
            initialized = SDLInit.SDL_InitSubSystem(SDLInit.SDL_INIT_GAMEPAD);
        } catch (Throwable ignored) {
            return false;
        }
        if (!initialized) {
            return false;
        }

        Map<Integer, Long> openHandles = new HashMap<>();
        status.set(new GamepadStatus(true, List.of(), "SDL3 ready • No gamepad connected"));

        try {
            int scanTick = 0;
            while (running.get()) {
                SDLGamepad.SDL_UpdateGamepads();

                if (scanTick++ % 30 == 0) {
                    syncSdl3Gamepads(openHandles);
                }

                int mask = 0;
                Map<String, Integer> currentBindings = bindings.get();
                for (long handle : openHandles.values()) {
                    List<String> activeTokens = readSdl3Controls(handle);
                    dispatchCaptureIfNeeded(activeTokens);
                    for (String token : activeTokens) {
                        Integer key = currentBindings.get(token);
                        if (key != null && key >= 0 && key <= 0xF) {
                            mask |= (1 << key);
                        }
                    }
                }
                keypad.setMask(KeypadState.Source.GAMEPAD, mask);
                Thread.sleep(8);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            openHandles.values().forEach(SDLGamepad::SDL_CloseGamepad);
            openHandles.clear();
            keypad.setMask(KeypadState.Source.GAMEPAD, 0);
            SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_GAMEPAD);
        }
        return true;
    }

    private void syncSdl3Gamepads(Map<Integer, Long> openHandles) {
        Set<Integer> currentIds = new HashSet<>();
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        if (ids != null) {
            try {
                while (ids.hasRemaining()) {
                    currentIds.add(ids.get());
                }
            } finally {
                SDLStdinc.SDL_free(ids);
            }
        }

        // Close disconnected handles
        openHandles.entrySet().removeIf(entry -> {
            if (!currentIds.contains(entry.getKey())) {
                SDLGamepad.SDL_CloseGamepad(entry.getValue());
                return true;
            }
            return false;
        });

        // Open newly connected gamepads
        for (int id : currentIds) {
            if (!openHandles.containsKey(id)) {
                long handle = SDLGamepad.SDL_OpenGamepad(id);
                if (handle != 0L) {
                    openHandles.put(id, handle);
                }
            }
        }

        List<String> names = new ArrayList<>();
        for (long handle : openHandles.values()) {
            String name = SDLGamepad.SDL_GetGamepadName(handle);
            names.add((name == null || name.isBlank()) ? "SDL Gamepad" : name);
        }
        String msg = names.isEmpty()
            ? "No gamepad connected (SDL3 active)"
            : names.size() == 1 ? names.getFirst() : names.size() + " gamepads connected";
        status.set(new GamepadStatus(true, List.copyOf(names), msg));
    }

    private static List<String> readSdl3Controls(long handle) {
        List<String> active = new ArrayList<>();
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP)) active.add("DPAD_UP");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN)) active.add("DPAD_DOWN");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT)) active.add("DPAD_LEFT");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT)) active.add("DPAD_RIGHT");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH)) active.add("BTN_0");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_EAST)) active.add("BTN_1");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_WEST)) active.add("BTN_2");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH)) active.add("BTN_3");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER)) active.add("BTN_4");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER)) active.add("BTN_5");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_BACK)) active.add("BTN_6");
        if (SDLGamepad.SDL_GetGamepadButton(handle, SDLGamepad.SDL_GAMEPAD_BUTTON_START)) active.add("BTN_9");

        short lx = SDLGamepad.SDL_GetGamepadAxis(handle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX);
        short ly = SDLGamepad.SDL_GetGamepadAxis(handle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY);
        if (lx < -SDL_AXIS_DEADZONE) active.add("DPAD_LEFT");
        else if (lx > SDL_AXIS_DEADZONE) active.add("DPAD_RIGHT");
        if (ly < -SDL_AXIS_DEADZONE) active.add("DPAD_UP");
        else if (ly > SDL_AXIS_DEADZONE) active.add("DPAD_DOWN");

        if (SDLGamepad.SDL_GetGamepadAxis(handle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER) > SDL_TRIGGER_THRESHOLD) {
            active.add("BTN_7");
        }
        if (SDLGamepad.SDL_GetGamepadAxis(handle, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) > SDL_TRIGGER_THRESHOLD) {
            active.add("BTN_8");
        }
        return active;
    }

    private void runInput4jFallbackLoop() {
        try {
            input4jPlugin = InputDevices.init();
            input4jPlugin.onDevicesChanged(this::refreshInput4jStatus);
            refreshInput4jStatus();
        } catch (Throwable t) {
            status.set(new GamepadStatus(false, List.of(), "Gamepad subsystem unavailable"));
            running.set(false);
            return;
        }

        int refreshCounter = 0;
        while (running.get()) {
            try {
                Collection<InputDevice> devices = input4jPlugin.getAll();
                if (++refreshCounter >= 120) {
                    refreshCounter = 0;
                    refreshInput4jStatus();
                }

                int mask = 0;
                Map<String, Integer> currentBindings = bindings.get();
                for (InputDevice device : devices) {
                    device.poll();
                    List<String> activeTokens = readInput4jControls(device);
                    dispatchCaptureIfNeeded(activeTokens);
                    for (String token : activeTokens) {
                        Integer key = currentBindings.get(token);
                        if (key != null && key >= 0 && key <= 0xF) {
                            mask |= (1 << key);
                        }
                    }
                }
                keypad.setMask(KeypadState.Source.GAMEPAD, mask);
                Thread.sleep(8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable ignored) {
            }
        }
    }

    private void dispatchCaptureIfNeeded(List<String> activeTokens) {
        Consumer<String> listener = captureListener.get();
        if (listener != null && !activeTokens.isEmpty()) {
            if (captureListener.compareAndSet(listener, null)) {
                listener.accept(activeTokens.getFirst());
            }
        }
    }

    private void refreshInput4jStatus() {
        if (input4jPlugin == null) {
            return;
        }
        List<String> names = input4jPlugin.getAll().stream()
            .map(d -> {
                String n = d.getDisplayName();
                return (n == null || n.isBlank()) ? d.getName() : n;
            })
            .filter(n -> n != null && !n.isBlank())
            .toList();
        String msg = names.isEmpty()
            ? "No gamepad connected (input4j active)"
            : names.size() == 1 ? names.getFirst() : names.size() + " gamepads connected";
        status.set(new GamepadStatus(true, names, msg));
    }

    private static List<String> readInput4jControls(InputDevice device) {
        List<String> active = new ArrayList<>();
        int buttonIndex = 0;
        for (InputComponent comp : device.getComponents()) {
            float val = comp.getData();
            InputComponent.ID id = comp.getId();
            if (comp.isButton()) {
                if (val >= 0.5f) {
                    if (Button.DPAD_UP.equals(id)) active.add("DPAD_UP");
                    else if (Button.DPAD_DOWN.equals(id)) active.add("DPAD_DOWN");
                    else if (Button.DPAD_LEFT.equals(id)) active.add("DPAD_LEFT");
                    else if (Button.DPAD_RIGHT.equals(id)) active.add("DPAD_RIGHT");
                    else active.add("BTN_" + buttonIndex);
                }
                buttonIndex++;
            } else if (comp.isAxis()) {
                if (Axis.AXIS_X.equals(id)) {
                    if (val < -AXIS_DEADZONE) active.add("DPAD_LEFT");
                    else if (val > AXIS_DEADZONE) active.add("DPAD_RIGHT");
                } else if (Axis.AXIS_Y.equals(id)) {
                    if (val < -AXIS_DEADZONE) active.add("DPAD_UP");
                    else if (val > AXIS_DEADZONE) active.add("DPAD_DOWN");
                }
            }
        }
        return active;
    }

    @Override
    public void close() {
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
        keypad.setMask(KeypadState.Source.GAMEPAD, 0);
        if (input4jPlugin != null) {
            try {
                input4jPlugin.close();
            } catch (IOException ignored) {
            }
        }
    }
}
