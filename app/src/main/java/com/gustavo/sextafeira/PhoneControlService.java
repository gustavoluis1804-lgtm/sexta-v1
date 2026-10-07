package com.gustavo.sextafeira;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PhoneControlService extends AccessibilityService {
    public static PhoneControlService instance;
    private WindowManager windowManager;
    private View bubble;
    private WindowManager.LayoutParams bubbleParams;
    @Override public void onServiceConnected() {
        instance=this;
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        updateBubble(); AgentRuntime.get(this).refresh();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // No automatic reading, recordings, cached screen content, or event logs.
    }
    @Override public void onInterrupt() { AgentRuntime.get(this).stopSpeech(); }
    @Override public void onDestroy() {
        removeBubble(); if(instance==this) instance=null;
        AgentRuntime.get(this).refresh(); super.onDestroy();
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
    private void removeBubble() {
        if(bubble!=null) { try { windowManager.removeView(bubble); } catch(Exception ignored) {} bubble=null; }
    }
    public void updateBubble() {
        removeBubble();
        if(!AgentRuntime.get(this).bubbleEnabled()) return;
        TextView button=new TextView(this);
        button.setText("••\n⌣"); button.setTextColor(Color.rgb(186,246,107)); button.setTextSize(18); button.setGravity(Gravity.CENTER); button.setLineSpacing(-dp(8),1);
        button.setContentDescription("Chamar Sexta Feira. Toque para falar ou arraste para mover.");
        GradientDrawable background=new GradientDrawable(); background.setColor(Color.rgb(16,32,24)); background.setCornerRadius(dp(28)); background.setStroke(dp(1),Color.rgb(130,167,89)); button.setBackground(background);
        bubbleParams=new WindowManager.LayoutParams(dp(56),dp(56),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);
        bubbleParams.gravity=Gravity.TOP|Gravity.END; bubbleParams.x=dp(12); bubbleParams.y=dp(180);
        button.setOnTouchListener(new View.OnTouchListener() {
            float downX,downY; int originX,originY; boolean moved;
            @Override public boolean onTouch(View view,MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) { downX=event.getRawX(); downY=event.getRawY(); originX=bubbleParams.x; originY=bubbleParams.y; moved=false; return true; }
                if(event.getAction()==MotionEvent.ACTION_MOVE) {
                    float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                    if(Math.abs(dx)+Math.abs(dy)>dp(8)) moved=true;
                    if(moved) {
                        bubbleParams.x=Math.max(0,Math.min(getResources().getDisplayMetrics().widthPixels-dp(56),originX-(int)dx));
                        bubbleParams.y=Math.max(dp(24),Math.min(getResources().getDisplayMetrics().heightPixels-dp(100),originY+(int)dy));
                        try { windowManager.updateViewLayout(view,bubbleParams); } catch(Exception ignored) {}
                    }
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP) { if(!moved) view.performClick(); return true; }
                return true;
            }
        });
        button.setOnClickListener(view->launchVoice());
        try { windowManager.addView(button,bubbleParams); bubble=button; }
        catch(Exception error) { AgentRuntime.get(this).tell("Não consegui mostrar o botão flutuante. Use o microfone dentro do aplicativo.",false); }
    }
    public void launchVoice() {
        Intent intent=new Intent(this,VoiceActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("targetPackage",currentPackage()); startActivity(intent);
    }
    private AccessibilityNodeInfo screenRoot() {
        // Ignore the assistant's own windows and the keyboard; keep the user's target app.
        List<AccessibilityWindowInfo> windows=getWindows();
        AccessibilityNodeInfo fallback=null;
        if(windows!=null) for(AccessibilityWindowInfo window:windows) {
            if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root=window.getRoot();
            if(root==null) continue;
            if(getPackageName().contentEquals(root.getPackageName()==null?"":root.getPackageName())) { root.recycle(); continue; }
            if(window.isActive() || window.isFocused()) { if(fallback!=null) fallback.recycle(); return root; }
            if(fallback==null) fallback=root; else root.recycle();
        }
        if(fallback!=null) return fallback;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null && getPackageName().contentEquals(root.getPackageName()==null?"":root.getPackageName())) { root.recycle(); return null; }
        return root;
    }
    public String currentPackage() {
        AccessibilityNodeInfo root=screenRoot();
        if(root==null) return "";
        String name=root.getPackageName()==null?"":root.getPackageName().toString(); root.recycle(); return name;
    }
    private ArrayList<AccessibilityNodeInfo> nodes(AccessibilityNodeInfo root) {
        ArrayList<AccessibilityNodeInfo> nodes=new ArrayList<>();
        if(root==null) return nodes;
        nodes.add(root);
        for(int i=0;i<nodes.size() && nodes.size()<350;i++) {
            AccessibilityNodeInfo node=nodes.get(i);
            for(int j=0;j<node.getChildCount() && nodes.size()<350;j++) {
                AccessibilityNodeInfo child=node.getChild(j); if(child!=null) nodes.add(child);
            }
        }
        return nodes;
    }
    private void release(List<AccessibilityNodeInfo> nodes) { for(AccessibilityNodeInfo node:nodes) node.recycle(); }
    public String readScreen() {
        ArrayList<AccessibilityNodeInfo> nodes=nodes(screenRoot());
        LinkedHashSet<String> texts=new LinkedHashSet<>(); int length=0;
        try {
            for(AccessibilityNodeInfo node:nodes) {
                if(!node.isVisibleToUser() || node.isPassword()) continue;
                CharSequence content=node.getText()!=null?node.getText():node.getContentDescription();
                if(content==null || content.toString().trim().isEmpty()) continue;
                String text=content.toString().trim();
                if(texts.add(text)) length+=text.length();
                if(texts.size()>=35 || length>=1500) break;
            }
        } finally { release(nodes); }
        return texts.isEmpty()?"Essa tela não fornece texto acessível. Abra outra tela e tente de novo.":"Na tela:\n"+String.join(". ",texts);
    }
    public void clickText(String target) {
        String query=CommandParser.normalize(target);
        ArrayList<AccessibilityNodeInfo> nodes=nodes(screenRoot());
        ArrayList<AccessibilityNodeInfo> exact=new ArrayList<>(),partial=new ArrayList<>();
        LinkedHashSet<String> exactIds=new LinkedHashSet<>(),partialIds=new LinkedHashSet<>();
        try {
            for(AccessibilityNodeInfo node:nodes) {
                if(!node.isVisibleToUser() || !node.isEnabled() || node.isPassword()) continue;
                String text=CommandParser.normalize(node.getText()==null?"":node.getText().toString());
                String description=CommandParser.normalize(node.getContentDescription()==null?"":node.getContentDescription().toString());
                Rect bounds=new Rect(); node.getBoundsInScreen(bounds);
                String id=bounds.toShortString();
                if(text.equals(query) || description.equals(query)) { if(exactIds.add(id)) exact.add(node); }
                else if(text.contains(query) || description.contains(query)) { if(partialIds.add(id)) partial.add(node); }
            }
            List<AccessibilityNodeInfo> matches=exact.isEmpty()?partial:exact;
            if(matches.isEmpty()) { AgentRuntime.get(this).tell("Não encontrei “"+target+"” na tela. Diga “ler tela” para consultar os nomes.",true); return; }
            if(matches.size()>1) { AgentRuntime.get(this).tell("Há mais de um item com esse nome. Diga o texto completo do botão.",true); return; }
            AccessibilityNodeInfo match=matches.get(0), click=match; boolean obtained=false;
            while(click!=null && !click.isClickable()) {
                AccessibilityNodeInfo parent=click.getParent();
                if(obtained) click.recycle(); click=parent; obtained=true;
            }
            boolean done=click!=null && click.isEnabled() && click.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if(obtained && click!=null) click.recycle();
            if(done) AgentRuntime.get(this).tell("Toquei em “"+target+"”.",true);
            else {
                Rect rect=new Rect(); match.getBoundsInScreen(rect);
                if(rect.isEmpty()) { AgentRuntime.get(this).tell("Esse item não permite toque.",true); return; }
                Path path=new Path(); path.moveTo(rect.centerX(),rect.centerY());
                gesture(path,80,"Toquei em “"+target+"”.");
            }
        } finally { release(nodes); }
    }
    public String writeText(String text) {
        ArrayList<AccessibilityNodeInfo> nodes=nodes(screenRoot());
        AccessibilityNodeInfo field=null; int count=0;
        try {
            for(AccessibilityNodeInfo node:nodes) {
                if(!node.isVisibleToUser() || !node.isEnabled() || !node.isEditable() || node.isPassword()) continue;
                if(node.isFocused()) { field=node; count=1; break; }
                count++; field=node;
            }
            if(field==null) return "Toque primeiro no campo em que deseja escrever. Campos de senha não são preenchidos.";
            if(count>1) return "Há vários campos. Toque no campo desejado e repita o comando.";
            Bundle args=new Bundle(); args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
            return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args)?"Preenchi o campo com o texto. Confira antes de enviar.":"Esse aplicativo não permite preencher o campo por comando.";
        } finally { release(nodes); }
    }
    public void scroll(String direction) {
        ArrayList<AccessibilityNodeInfo> nodes=nodes(screenRoot());
        if(nodes.isEmpty()) { AgentRuntime.get(this).tell("Não há uma tela disponível para rolar.",true); return; }
        Rect area=new Rect(); nodes.get(0).getBoundsInScreen(area);
        try {
            if(direction.equals("baixo") || direction.equals("cima")) for(AccessibilityNodeInfo node:nodes) {
                if(node.isVisibleToUser() && node.isScrollable() && node.performAction(direction.equals("baixo")?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                    AgentRuntime.get(this).tell("Rolei para "+direction+".",true); return;
                }
            }
        } finally { release(nodes); }
        if(area.width()<100 || area.height()<100) { AgentRuntime.get(this).tell("Não consegui identificar a área da tela.",true); return; }
        float x=area.centerX(),y=area.centerY(); Path path=new Path();
        if(direction.equals("baixo")) { path.moveTo(x,area.top+area.height()*.75f); path.lineTo(x,area.top+area.height()*.3f); }
        else if(direction.equals("cima")) { path.moveTo(x,area.top+area.height()*.3f); path.lineTo(x,area.top+area.height()*.75f); }
        else if(direction.equals("direita")) { path.moveTo(area.left+area.width()*.8f,y); path.lineTo(area.left+area.width()*.2f,y); }
        else { path.moveTo(area.left+area.width()*.2f,y); path.lineTo(area.left+area.width()*.8f,y); }
        gesture(path,380,"Rolei para "+direction+".");
    }
    private void gesture(Path path,long duration,String reply) {
        GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,duration)).build();
        boolean accepted=dispatchGesture(gesture,new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription description) { AgentRuntime.get(PhoneControlService.this).tell(reply,true); }
            @Override public void onCancelled(GestureDescription description) { AgentRuntime.get(PhoneControlService.this).tell("O gesto foi interrompido. Tente novamente.",true); }
        },null);
        if(!accepted) AgentRuntime.get(this).tell("O Android não permitiu executar o gesto.",true);
    }
}
