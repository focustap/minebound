package com.focustap.minebound.rpg;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SpriteResourcesTest {
    @Test void everyGeneratedFrameIsPackagedAndHasTransparencyAndVisiblePixels() throws Exception {
        String[] ids = {"wobbly_thing", "rabbick", "rudinn", "ponman", "jevil"};
        int[] counts = {4, 3, 4, 4, 3};
        for (int s = 0; s < ids.length; s++) for (int frame = 0; frame < counts[s]; frame++) {
            String resource = "/assets/minebound/textures/enemy/world1/" + ids[s] + "_" + frame + ".png";
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                BufferedImage image = ImageIO.read(stream);
                int transparent = 0, visible = 0;
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) == 0) transparent++; else visible++;
                }
                assertTrue(transparent > 100, resource + " has no transparent backdrop");
                assertTrue(visible > 30, resource + " has no visible sprite");
                assertTrue(image.getWidth() <= 42 && image.getHeight() <= 43);
            }
        }
    }
}
