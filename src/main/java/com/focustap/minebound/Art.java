package com.focustap.minebound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class Art {
    private Art() {}
    static void draw(GuiGraphics g,String asset,int x,int y,int width,int height) {
        var texture=ResourceLocation.fromNamespaceAndPath("minebound","textures/gui/"+asset+".png");
        g.blit(texture,x,y,width,height,0,0,1536,1024,1536,1024);
    }
}
