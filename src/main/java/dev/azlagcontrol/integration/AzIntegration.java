package dev.azlagcontrol.integration;

/** Contract for optional plugin integrations. */
public interface AzIntegration {
    void register();
    void unregister();
}
