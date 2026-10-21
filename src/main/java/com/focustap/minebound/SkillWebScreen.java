package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A genuine pannable/zoomable graph; code-drawn original icons sit over the art. */
final class SkillWebScreen extends RetroScreen {
    private final MineboundScreen parent;
    private CharacterProfile profile;
    private float zoom = .36f, panX, panY;
    private String selected, notice = "Drag to pan. Scroll to zoom. Select then confirm.";
    private boolean dragging;
    private Button buy;
    private int feedback;
    SkillWebScreen(MineboundScreen parent, CharacterProfile profile) {
        super("Minebound skill web"); this.parent = parent; this.profile = profile;
        double angle = Math.toRadians(-125 + profile.startingClass.ordinal()*60);
        panX = -(float)Math.cos(angle)*70; panY = -(float)Math.sin(angle)*70;
    }
    @Override protected void init() {
        buy = button("PURCHASE",160,199,138,() -> {
            if (selected != null) Protocol.send("SKILL",selected);
        });
        button("BACK",12,199,138,() -> minecraft.setScreen(parent));
        refresh();
    }
    void accept(Protocol.Snapshot snapshot) {
        int before = profile.nodes.size(); profile = snapshot.profile(); parent.accept(snapshot);
        if (!snapshot.notice().isEmpty()) notice = snapshot.notice();
        if (profile.nodes.size() > before) { feedback = 14; notice = "Learned. Your character sheet has updated."; }
        refresh();
    }
    private void refresh() {
        SkillGraph.Node n = SkillGraph.NODES.get(selected);
        if (buy != null) {
            buy.active = n != null && !profile.nodes.contains(selected) && SkillGraph.reachable(profile,n) && profile.skillPoints >= n.cost();
            buy.setMessage(Component.literal(n == null ? "SELECT A NODE" : "BUY: " + n.cost() + " POINT" + (n.cost() == 1 ? "" : "S")));
        }
    }
    private int sx(float v) { return x()+155+Math.round((v+panX)*zoom); }
    private int sy(float v) { return y()+104+Math.round((v+panY)*zoom); }
    private int radius() { return Math.max(4,Math.round(12*zoom)); }
    private SkillGraph.Node at(double mx, double my) {
        if (mx < x()+12 || mx > x()+298 || my < y()+44 || my > y()+157) return null;
        return SkillGraph.NODES.values().stream().filter(n -> Math.hypot(sx(n.x())-mx,sy(n.y())-my) <= radius()+3).findFirst().orElse(null);
    }
    private void line(GuiGraphics g,int ax,int ay,int bx,int by,int color) {
        int steps = Math.max(Math.abs(bx-ax),Math.abs(by-ay));
        for (int i=0;i<=steps;i++) { int px=ax+(bx-ax)*i/Math.max(1,steps), py=ay+(by-ay)*i/Math.max(1,steps); g.fill(px,py,px+1,py+1,color); }
    }
    @Override void drawPage(GuiGraphics g) {
        Art.draw(g,"web",x()+7,y()+25,296,169);
        text(g,"SKILL WEB / POINTS " + profile.skillPoints,12,30,GOLD);
        g.enableScissor(x()+12,y()+44,x()+298,y()+157);
        for (SkillGraph.Node n : SkillGraph.NODES.values()) {
            if (n.links().isEmpty()) line(g,sx(0),sy(0),sx(n.x()),sy(n.y()),0xFF50465D);
            for (String id : n.links()) {
                var link = SkillGraph.NODES.get(id);
                line(g,sx(n.x()),sy(n.y()),sx(link.x()),sy(link.y()),
                        profile.nodes.contains(n.id()) && profile.nodes.contains(id) ? n.color() : 0xFF50465D);
            }
        }
        for (SkillGraph.Node n : SkillGraph.NODES.values()) {
            int nx=sx(n.x()),ny=sy(n.y()),r=radius();
            int color = profile.nodes.contains(n.id()) ? n.color() : SkillGraph.reachable(profile,n) ? CREAM : 0xFF51485E;
            boolean active = n.kind() == SkillGraph.Kind.ACTIVE;
            if (active) for (int row=-r;row<=r;row++) { int half=r-Math.abs(row); g.fill(nx-half,ny+row,nx+half+1,ny+row+1,color); }
            else if (n.kind() == SkillGraph.Kind.ATTRIBUTE) g.fill(nx-r,ny-r,nx+r+1,ny+r+1,color);
            else { g.fill(nx-r+1,ny-r,nx+r,ny+r+1,color); g.fill(nx-r,ny-r+2,nx+r+1,ny+r-1,color); }
            if (selected != null && selected.equals(n.id())) g.renderOutline(nx-r-2,ny-r-2,r*2+5,r*2+5,feedback > 0 ? FERN : GOLD);
            // Tiny icon: cross for attributes, slash for actives, core gem for passives.
            if (active) line(g,nx-2,ny+2,nx+2,ny-2,INK);
            else if (n.kind() == SkillGraph.Kind.ATTRIBUTE) { g.fill(nx-2,ny,nx+3,ny+1,INK); g.fill(nx,ny-2,nx+1,ny+3,INK); }
            else g.fill(nx-1,ny-1,nx+2,ny+2,INK);
        }
        g.disableScissor();
        SkillGraph.Node n = SkillGraph.NODES.get(selected);
        if (n != null) {
            text(g,n.branch()+" / "+n.label(),12,163,n.color());
            wrap(g,n.description(),12,178,284,CREAM);
        } else wrap(g,notice,12,164,284,LAVENDER);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.render(g,mx,my,partial);
        SkillGraph.Node n = at(mx,my);
        if (n != null) {
            String state = profile.nodes.contains(n.id()) ? "PURCHASED" : SkillGraph.reachable(profile,n) ? "AVAILABLE" : "LOCKED";
            String required = n.links().isEmpty() ? "Common starting region" : n.links().stream().map(SkillGraph.NODES::get)
                    .map(parent -> parent.branch()+" / "+parent.label()).collect(java.util.stream.Collectors.joining(" OR "));
            g.renderComponentTooltip(font,List.of(Component.literal(n.branch()+" / "+n.label()),Component.literal(n.description()),
                    Component.literal(state+" / "+n.cost()+" SP"),Component.literal("Link: "+required),
                    Component.literal(n.ability() == null ? n.family()+" passive" : "Requires five purchased passives")),mx,my);
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if (super.mouseClicked(mx,my,button)) return true;
        if (button == 0 && my >= y()+44 && my <= y()+157 && mx >= x()+12 && mx <= x()+298) {
            var node = at(mx,my); if (node != null) { selected=node.id(); refresh(); } else dragging=true;
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if (dragging) { panX += (float)dx/zoom; panY += (float)dy/zoom; return true; }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button) { dragging=false; return super.mouseReleased(mx,my,button); }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if (my < y()+44 || my > y()+157) return super.mouseScrolled(mx,my,horizontal,vertical);
        float old=zoom; zoom = Math.clamp(zoom*(vertical > 0 ? 1.15f : .87f),.16f,1.6f);
        panX += (float)(mx-x()-155)*(1/zoom-1/old); panY += (float)(my-y()-104)*(1/zoom-1/old);
        return true;
    }
    @Override public void tick() { if (feedback > 0) feedback--; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
