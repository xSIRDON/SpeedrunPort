package dev.ncerdon.speedrunport.f3;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;

public record LineCollector(List<String> lines) implements DebugScreenDisplayer {
    @Override
    public void addPriorityLine(String line) {
        lines.add(line);
    }

    @Override
    public void addLine(String line) {
        lines.add(line);
    }

    @Override
    public void addToGroup(Identifier group, Collection<String> groupLines) {
        lines.addAll(groupLines);
    }

    @Override
    public void addToGroup(Identifier group, String line) {
        lines.add(line);
    }
}
