package org.example.commands;

import java.io.IOException;
import java.util.Map;

// Контракт для любого сценария (запись, чтение и т.д.)
public interface Command {
    void execute(Map<String, String> params) throws IOException;
}
