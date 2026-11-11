package dev.ncerdon.speedrunport.f3;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;
import org.joml.Matrix3x2fStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public final class ClassicPieChart {
    private static final int RADIUS = 160;
    private static final int RIM = 10;
    private static final int WHITE = 0xFFFFFFFF;
    private static final DecimalFormat PERCENT = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private ClassicPieChart() {}

    public static void render(GuiGraphicsExtractor graphics, Font font, ProfileResults results, String path) {
        List<ResultField> fields = results.getTimes(path);
        if (fields.isEmpty()) {
            return;
        }
        ResultField current = fields.removeFirst();

        Window window = Minecraft.getInstance().getWindow();
        int centerX = window.getWidth() - RADIUS - 10;
        int centerY = window.getHeight() - 320;

        graphics.nextStratum();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.identity();
        pose.scale(1.0F / window.getGuiScale());

        ColoredMesh mesh = new ColoredMesh();
        double start = 0.0;
        for (ResultField field : fields) {
            addSlice(mesh, centerX, centerY, start, field.percentage, field.getColor() | 0xFF000000);
            start += field.percentage;
        }
        mesh.submit(graphics);

        String name = ProfileResults.demanglePath(current.name);
        String header = ("unspecified".equals(name) ? "" : "[0] ") + (name.isEmpty() ? "ROOT " : name + " ");
        int headerY = centerY - 80 - 16;
        graphics.text(font, header, centerX - RADIUS, headerY, WHITE, true);
        String total = PERCENT.format(current.globalPercentage) + "%";
        graphics.text(font, total, centerX + RADIUS - font.width(total), headerY, WHITE, true);

        for (int i = 0; i < fields.size(); i++) {
            ResultField field = fields.get(i);
            int color = field.getColor() | 0xFF000000;
            int y = centerY + 80 + i * 8 + 20;
            String label = ("unspecified".equals(field.name) ? "[?] " : "[" + (i + 1) + "] ") + field.name;
            graphics.text(font, label, centerX - RADIUS, y, color, true);
            String local = PERCENT.format(field.percentage) + "%";
            graphics.text(font, local, centerX + RADIUS - 50 - font.width(local), y, color, true);
            String global = PERCENT.format(field.globalPercentage) + "%";
            graphics.text(font, global, centerX + RADIUS - font.width(global), y, color, true);
        }

        pose.popMatrix();
    }

    private static void addSlice(ColoredMesh mesh, int centerX, int centerY, double startPercent, double percent, int color) {
        int segments = Mth.floor(percent / 4.0) + 1;
        int rimColor = 0xFF000000 | (color >> 1 & 0x7F7F7F);
        float previousX = 0.0F;
        float previousY = 0.0F;
        float previousDepth = 0.0F;
        for (int step = segments; step >= 0; step--) {
            float angle = (float) ((startPercent + percent * step / segments) * Math.PI * 2.0 / 100.0);
            float depth = Mth.cos(angle) * RADIUS * 0.5F;
            float x = centerX + Mth.sin(angle) * RADIUS;
            float y = centerY - depth;
            if (step < segments) {
                mesh.triangle(centerX, centerY, previousX, previousY, x, y, color);
                if (depth <= 0.0F && previousDepth <= 0.0F) {
                    mesh.quad(previousX, previousY, previousX, previousY + RIM, x, y + RIM, x, y, rimColor);
                }
            }
            previousX = x;
            previousY = y;
            previousDepth = depth;
        }
    }
}
