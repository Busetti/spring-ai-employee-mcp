package com.example.employeemcp.dynamic.provider;

import com.example.employeemcp.dynamic.registry.DynamicToolRegistry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DynamicToolCallbackProvider implements ToolCallbackProvider {

    private final DynamicToolRegistry dynamicRegistry;

    public DynamicToolCallbackProvider(DynamicToolRegistry dynamicRegistry) {
        this.dynamicRegistry = dynamicRegistry;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> callbacks = dynamicRegistry.allCallbacks();
        return callbacks.toArray(new ToolCallback[0]);
    }
}
