package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Connected graph with readable branch views, a fit-all overview and a persistent inspector. */
final class SkillWebScreen extends RetroScreen {
    private final MineboundScreen parent;
    private CharacterProfile profile;
    private final List<String> branches=SkillGraph.NODES.values().stream().map(SkillGraph.Node::branch).distinct().toList();
    private float zoom=.6f,panX,panY,rotation;
    private int branchIndex,feedback;
    private String selected,notice="Select a node to see its effect and requirements.";
    private boolean dragging,overview;
    private Button buy;
    SkillWebScreen(MineboundScreen parent,CharacterProfile profile) {
        super("Minebound skill web"); this.parent=parent; this.profile=profile;
        var first=SkillGraph.NODES.values().stream().filter(n -> n.family().equals(profile.startingClass.label)).findFirst().orElseThrow();
        branchIndex=branches.indexOf(first.branch()); focusBranch();
    }
    @Override String pageLabel() { return "SKILLS / "+profile.skillPoints+" SP"; }
    @Override protected void init() {
        clearWidgets();
        small("<",12,20,() -> changeBranch(-1));
        small(">",204,20,() -> changeBranch(1));
        small("ALL",228,30,() -> { overview=true; rotation=0; panX=panY=0; zoom=.085f; });
        small("-",262,16,() -> zoom=Math.max(.085f,zoom*.8f));
        small("+",282,16,() -> zoom=Math.min(1.3f,zoom*1.25f));
        buy=button("SELECT A NODE",160,199,138,() -> { if (selected!=null) Protocol.send("SKILL",selected); });
        button("BACK",12,199,138,() -> minecraft.setScreen(parent)); refresh();
    }
    private void small(String label,int dx,int w,Runnable action) {
        var b=button(label,dx,30,w,action); b.setHeight(16);
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(switch(label) {
            case "ALL" -> "Show the whole web. Drag to pan; scroll to zoom.";
            case "<", ">" -> "Browse specializations. Each path reads from left to right.";
            default -> "Zoom the skill web.";
        })));
    }
    private void changeBranch(int delta) { branchIndex=Math.floorMod(branchIndex+delta,branches.size()); focusBranch(); }
    private void focusBranch() {
        overview=false;
        var root=SkillGraph.NODES.values().stream().filter(n -> n.branch().equals(branches.get(branchIndex))).findFirst().orElseThrow();
        rotation=-(float)Math.atan2(root.y(),root.x()); panX=-325; panY=0; zoom=.62f;
        selected=SkillGraph.NODES.values().stream().filter(n -> n.branch().equals(branches.get(branchIndex)))
                .filter(n -> SkillGraph.purchaseBlocker(profile,n).isEmpty()).findFirst().orElse(root).id(); refresh();
    }
    void accept(Protocol.Snapshot snapshot) {
        int before=profile.nodes.size(); profile=snapshot.profile(); parent.accept(snapshot);
        if (!snapshot.notice().isEmpty()) notice=snapshot.notice();
        if (profile.nodes.size()>before) { feedback=20; notice="Learned. Sheet updated."; }
        refresh();
    }
    private void refresh() {
        var n=SkillGraph.NODES.get(selected); if (buy==null) return;
        String blocker=n==null ? "Select a node." : SkillGraph.purchaseBlocker(profile,n);
        buy.active=blocker.isEmpty();
        buy.setMessage(Component.literal(n==null ? "SELECT A NODE" : profile.nodes.contains(n.id()) ? "LEARNED" : "LEARN / "+n.cost()+" SP"));
        buy.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(blocker.isEmpty() ? "Learn "+n.label() : blocker)));
    }
    private float rx(float x,float y) { return x*(float)Math.cos(rotation)-y*(float)Math.sin(rotation); }
    private float ry(float x,float y) { return x*(float)Math.sin(rotation)+y*(float)Math.cos(rotation); }
    private int nx(SkillGraph.Node n) { return x()+155+Math.round((rx(n.x(),n.y())+panX)*zoom); }
    private int ny(SkillGraph.Node n) { return y()+97+Math.round((ry(n.x(),n.y())+panY)*zoom); }
    private int radius() { return Math.max(2,Math.round(10*zoom)); }
    private boolean inside(double mx,double my) { return mx>=x()+12 && mx<=x()+298 && my>=y()+50 && my<=y()+145; }
    private SkillGraph.Node at(double mx,double my) {
        if (!inside(mx,my)) return null;
        return SkillGraph.NODES.values().stream().filter(n -> Math.hypot(nx(n)-mx,ny(n)-my)<=radius()+3)
                .min(java.util.Comparator.comparingDouble(n -> Math.hypot(nx(n)-mx,ny(n)-my))).orElse(null);
    }
    private void line(GuiGraphics g,int ax,int ay,int bx,int by,int color) {
        int steps=Math.max(Math.abs(bx-ax),Math.abs(by-ay));
        for (int i=0;i<=steps;i++) { int px=ax+(bx-ax)*i/Math.max(1,steps),py=ay+(by-ay)*i/Math.max(1,steps); g.fill(px,py,px+1,py+1,color); }
    }
    @Override void drawPage(GuiGraphics g) {
        var current=SkillGraph.NODES.values().stream().filter(n -> n.branch().equals(branches.get(branchIndex))).findFirst().orElseThrow();
        label(g,overview ? "ALL PATHS" : current.family()+" / "+current.branch(),38,34,162,current.color());
        panel(g,12,50,286,95);
        g.enableScissor(x()+13,y()+51,x()+297,y()+144);
        for (var n:SkillGraph.NODES.values()) {
            for (String id:n.links()) {
                var link=SkillGraph.NODES.get(id);
                boolean main=overview || n.branch().equals(current.branch()) && link.branch().equals(current.branch());
                line(g,nx(n),ny(n),nx(link),ny(link),main ? profile.nodes.contains(n.id()) ? n.color() : 0xFF796981 : 0xFF312B3D);
            }
        }
        for (var n:SkillGraph.NODES.values()) {
            int px=nx(n),py=ny(n),r=radius();
            boolean main=overview || n.branch().equals(current.branch());
            int color=!main ? 0xFF51455F : profile.nodes.contains(n.id()) ? FERN
                    : SkillGraph.purchaseBlocker(profile,n).isEmpty() ? GOLD : 0xFF796981;
            if (n.kind()==SkillGraph.Kind.ACTIVE) for (int row=-r;row<=r;row++) { int half=r-Math.abs(row); g.fill(px-half,py+row,px+half+1,py+row+1,color); }
            else { g.fill(px-r,py-r,px+r+1,py+r+1,INK); g.renderOutline(px-r,py-r,r*2+1,r*2+1,color); }
            if (profile.nodes.contains(n.id())) g.fill(px-1,py-1,px+2,py+2,FERN);
            if (n.id().equals(selected)) g.renderOutline(px-r-2,py-r-2,r*2+5,r*2+5,feedback>0 ? FERN : CREAM);
            if (!overview && main && zoom>=.45f) {
                String caption=n.kind()==SkillGraph.Kind.ACTIVE ? "ACTIVE" : n.effect().startsWith("attr:") ? "+1 "+n.effect().substring(5)
                        : switch(n.effect()) { case "efficiency" -> "-1 PP"; case "pact" -> "PACT"; case "hp" -> "+3 HP"; case "pp" -> "+3 PP"; default -> "+"+n.effect().toUpperCase(java.util.Locale.ROOT); };
                caption=font.plainSubstrByWidth(caption,42);
                g.drawCenteredString(font,caption,px,py+12,color);
            }
        }
        g.disableScissor();
        text(g,"GOLD: ready  GREEN: learned  GRAY: locked",14,135,LAVENDER);
        panel(g,12,150,286,44);
        var n=SkillGraph.NODES.get(selected);
        if (n!=null) {
            label(g,n.branch()+" / "+n.label(),17,154,276,n.color());
            String effect=n.ability()==null ? n.description() : n.ability().label()+" / "+n.ability().scaling+" / "+profile.cost(n.ability())+" PP";
            label(g,effect,17,166,276,CREAM);
            String reason=SkillGraph.purchaseBlocker(profile,n);
            label(g,feedback>0 ? notice : reason.isEmpty() ? "Available / "+n.cost()+" SP. Click LEARN to confirm." : reason,17,181,276,reason.isEmpty() ? GOLD : LAVENDER);
        } else wrap(g,notice,17,156,276,3,LAVENDER);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.render(g,mx,my,partial); var n=at(mx,my);
        if (n!=null) {
            var lines=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            for (String s:List.of(n.branch()+" / "+n.label(),n.description(),
                    SkillGraph.purchaseBlocker(profile,n).isEmpty() ? "Available / "+n.cost()+" SP" : SkillGraph.purchaseBlocker(profile,n)))
                lines.addAll(font.split(Component.literal(s),Math.min(220,width-24)));
            g.renderTooltip(font,lines,mx,my);
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if (super.mouseClicked(mx,my,button)) return true;
        if (button==0 && inside(mx,my)) {
            var n=at(mx,my);
            if (n!=null) { selected=n.id(); refresh(); } else dragging=true;
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if (dragging) { panX+=(float)dx/zoom; panY+=(float)dy/zoom; return true; }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button) { dragging=false; return super.mouseReleased(mx,my,button); }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if (!inside(mx,my) || vertical==0) return super.mouseScrolled(mx,my,horizontal,vertical);
        float old=zoom; zoom=Math.clamp(zoom*(vertical>0 ? 1.15f : .87f),.085f,1.3f);
        panX+=(float)(mx-x()-155)*(1/zoom-1/old); panY+=(float)(my-y()-97)*(1/zoom-1/old); return true;
    }
    @Override public void tick() { if (feedback>0) feedback--; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
