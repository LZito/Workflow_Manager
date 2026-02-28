package at.lzito.workflowmanager.application.port;

import at.lzito.workflowmanager.domain.Workflow;

import java.util.List;
import java.util.function.Consumer;

/**
 * Output port for global hotkey registration.
 * Implementations live in the infrastructure layer.
 */
public interface HotkeyRegistryPort {

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
