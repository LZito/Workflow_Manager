package at.lzito.workflowmanager.workflow.infrastructure.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import at.lzito.workflowmanager.workflow.application.HotkeyRegistry;
import at.lzito.workflowmanager.workflow.domain.Workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HotkeyRegistry implementation backed by JNativeHook.
 *
 * <p>Hotkey format: modifier keys joined with {@code +} followed by the key name,
 * all lowercase — e.g. {@code "ctrl+alt+1"}, {@code "ctrl+shift+f5"}.
 *
 * <p>Modifier ordering in the combo string: ctrl → alt → shift → meta.
 */
public class JNativeHookRegistry implements HotkeyRegistry, NativeKeyListener {

    private final Consumer<String>      logger;
    private final Map<String, Workflow> bindings  = new HashMap<>();
    private Consumer<Workflow>          onActivate;

    public JNativeHookRegistry(Consumer<String> logger) {
        this.logger = logger;
        // Suppress JNativeHook's own verbose logging
        Logger.getLogger(GlobalScreen.class.getPackage().getName())
              .setLevel(Level.WARNING);
    }

    // ── HotkeyRegistry ────────────────────────────────────────────────────────

    @Override
    public void register() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            logger.accept("Hotkey registry active.");
        } catch (NativeHookException e) {
            logger.accept("Failed to register native hook: " + e.getMessage());
        }
    }

    @Override
    public void unregister() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {}
    }

    @Override
    public void bindWorkflows(List<Workflow> workflows, Consumer<Workflow> onActivate) {
        clearBindings();
        this.onActivate = onActivate;
        for (Workflow w : workflows) {
            if (w.hasHotkey()) {
                bindings.put(w.getHotkey().getRaw(), w);
            }
        }
        logger.accept("Hotkeys bound: " + bindings.size());
    }

    @Override
    public void clearBindings() {
        bindings.clear();
        onActivate = null;
    }

    // ── NativeKeyListener ─────────────────────────────────────────────────────

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        String combo = buildCombo(e);
        Workflow workflow = bindings.get(combo);
        if (workflow != null && onActivate != null) {
            onActivate.accept(workflow);
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a normalised combo string from a key event, e.g. {@code "ctrl+alt+1"}.
     * Modifier ordering: ctrl → alt → shift → meta.
     */
    private static String buildCombo(NativeKeyEvent e) {
        int mods = e.getModifiers();
        StringBuilder sb = new StringBuilder();

        if ((mods & NativeInputEvent.CTRL_MASK) != 0) {
            sb.append("ctrl+");
        }
        if ((mods & NativeInputEvent.ALT_MASK) != 0) {
            sb.append("alt+");
        }
        if ((mods & NativeInputEvent.SHIFT_MASK) != 0) {
            sb.append("shift+");
        }
        if ((mods & NativeInputEvent.META_MASK) != 0) {
            sb.append("meta+");
        }

        sb.append(NativeKeyEvent.getKeyText(e.getKeyCode()).toLowerCase());
        return sb.toString();
    }
}
