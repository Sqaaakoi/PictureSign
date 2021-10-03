package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.picturesign.PictureDownloader;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

import java.util.Arrays;
import java.util.List;

public class PictureSignRenderer {

    public void render(SignBlockEntity signBlockEntity, MatrixStack matrixStack, int light, int overlay) {
        String text = signBlockEntity.getTextOnRow(0, false).getString() +
                signBlockEntity.getTextOnRow(1, false).getString() +
                signBlockEntity.getTextOnRow(2, false).getString();
        String url = text.replaceAll("!PS:", "").replaceAll(" ","");
        if (url.contains("imgur:")) url = url.replace("imgur:", "https://i.imgur.com/");
        if (url.contains("imgbb:")) url = url.replace("imgbb:", "https://i.ibb.co/");
        if (!url.contains("https://") && !url.contains("http://")) {
            url = "https://" + url;
        }
        if (!url.contains(".png") && !url.contains(".jpg") && !url.contains(".jpeg")) return;
        String lastLine = signBlockEntity.getTextOnRow(3, false).getString();

        if (!lastLine.matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) return;

        List<String> scale = Arrays.stream(lastLine.split(":")).toList();
        float width = 0;
        float height = 0;
        float x = 0;
        float y = 0;
        float z = 0;
        try {
            width = Float.parseFloat(scale.get(0));
            height = Float.parseFloat(scale.get(1));
            x = Float.parseFloat(scale.get(2));
            y = Float.parseFloat(scale.get(3));
            z = Float.parseFloat(scale.get(4));
        }
        catch (NumberFormatException ignored) {}

        // Download the picture data
        PictureDownloader.PictureData data = PictureDownloader.getInstance().getPicture(url);
        if (data == null || data.identifier == null) {
            return;
        }

        float xOffset = 0.0F;
        float zOffset = 0.0F;

        Quaternion yRotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(0F);

        if (signBlockEntity.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            Direction direction = signBlockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);
            switch (direction) {
                case NORTH -> {
                    zOffset = 1.01F;
                    xOffset = 1.0F;
                    yRotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F);
                }
                case SOUTH -> zOffset = 0.010F;
                case EAST -> {
                    zOffset = 1.01F;
                    yRotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0F);
                }
                case WEST -> {
                    yRotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F);
                    xOffset = 1.01F;
                }
            }
        }
        else if (signBlockEntity.getCachedState().contains(Properties.ROTATION)) {
            yRotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(signBlockEntity.getCachedState().get(Properties.ROTATION) * -22.5f);
        }
        else return;


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        matrixStack.push();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, data.identifier);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        matrixStack.translate(xOffset + x, 0.00F + y, zOffset + z);
        matrixStack.multiply(yRotation);

        Matrix4f matrix4f = matrixStack.peek().getModel();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrix4f, width, 0.0F, 1.0F).texture(1.0F, 1.0F).color(255, 255, 255, 255)
                .light(light).overlay(overlay).next();

        buffer.vertex(matrix4f, width, height, 1.0F).texture(1.0F, 0.0F).color(255, 255, 255, 255)
                .light(light).overlay(overlay).next();

        buffer.vertex(matrix4f, 0.0F, height, 1.0F).texture(0.0F, 0.0F).color(255, 255, 255, 255)
                .light(light).overlay(overlay).next();

        buffer.vertex(matrix4f, 0.0F, 0.0F, 1.0F).texture(0.0F, 1.0F).color(255, 255, 255, 255)
                .light(light).overlay(overlay).next();

        tessellator.draw();
        matrixStack.pop();

        RenderSystem.disableDepthTest();
    }
}
