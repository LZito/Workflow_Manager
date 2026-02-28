package at.lzito.workflowmanager.engine;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import at.lzito.workflowmanager.model.Workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyManager implements NativeKeyListener {

    private final Map<String, Runnable> bindings = new HashMap<>();
    private final Consumer<String> logger;

    public HotkeyManager(Consumer<String> logger) {
        this.logger = logger;
        // Suppress JNativeHook's verbose logging
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.WARNING);
    }

    public void register() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            logger.accept("Hotkey manager registered.");
        } catch (NativeHookException e) {
            logger.accept("Failed to register native hook: " + e.getMessage());
        }
    }

    public void unregister() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {}
    }

    public void bindWorkflows(List<Workflow> workflows, java.util.function.BiConsumer<Workflow, Runnable> onBind) {
        bindings.clear();
        for (Workflow w : workflows) {
            if (w.getHotkey() != null && !w.getHotkey().isBlank()) {
                String key = w.getHotkey().toLowerCase();
                Runnable action = () -> logger.accept("Hotkey triggered: " + w.getName());
                bindings.put(key, action);
                onBind.accept(w, action);
            }
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Hotkey matching logic will be implemented here
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {}
}
