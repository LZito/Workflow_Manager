package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.Workflow;

import java.util.List;
import java.util.function.Consumer;

/**
 * Registers global hotkeys and binds them to workflow activation callbacks.
 */
public interface HotkeyRegistry {

    /** Register the native hook with the OS (call once at startup). */
    void register();

    /** Unregister the native hook (call on shutdown). */
    void unregister();

    /**
     * Bind each workflow's hotkey so that pressing it triggers {@code onActivate}.
     * Clears any previously bound hotkeys first.
     *
     * @param workflows  list of workflows to bind
     * @param onActivate callback invoked on the JNativeHook thread when a hotkey fires
     */
    void bindWorkflows(List<Workflow> workflows, Consumer<Workflow> onActivate);

    /** Remove all current hotkey bindings without unregistering the hook. */
    void clearBindings();
}
