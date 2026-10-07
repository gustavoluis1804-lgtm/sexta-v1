package com.gustavo.sextafeira;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class AgentRuntime {
    public interface Listener { void event(JSONObject event); }
    private static AgentRuntime instance;
    private final Context context;
    public final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<Listener> listeners=new ArrayList<>();
    private final ArrayList<JSONObject> history=new ArrayList<>();
    private TextToSpeech tts;
    private boolean speechReady;
    private int emotion;
    private String status="Pronta para ajudar. Toque no microfone ou digite um comando.";
    private boolean voice;
    private boolean bubble;

    public static synchronized AgentRuntime get(Context context) {
        if(instance==null) instance=new AgentRuntime(context.getApplicationContext());
        return instance;
    }
    private AgentRuntime(Context ctx) {
        context=ctx;
        voice=context.getSharedPreferences("agent",0).getBoolean("voice",true);
        bubble=context.getSharedPreferences("agent",0).getBoolean("bubble",true);
        tts=new TextToSpeech(context,code->{
            speechReady=code==TextToSpeech.SUCCESS;
            if(speechReady) {
                int language=tts.setLanguage(new Locale("pt","BR"));
                if(language==TextToSpeech.LANG_MISSING_DATA || language==TextToSpeech.LANG_NOT_SUPPORTED) speechReady=false;
                tts.setSpeechRate(1.0f);
            }
            refresh();
        });
    }
    public void addListener(Listener listener) { listeners.add(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }
    public int getEmotion() { return emotion; }
    public boolean bubbleEnabled() { return bubble; }
    public void setEmotion(int value) { emotion=Math.max(0,Math.min(11,value)); refresh(); }
    public void setVoice(boolean value) {
        voice=value; context.getSharedPreferences("agent",0).edit().putBoolean("voice",voice).apply();
        if(!value && tts!=null) tts.stop();
        refresh();
    }
    public void setBubble(boolean value) {
        bubble=value; context.getSharedPreferences("agent",0).edit().putBoolean("bubble",bubble).apply();
        if(PhoneControlService.instance!=null) PhoneControlService.instance.updateBubble();
        refresh();
    }
    public void stopSpeech() { if(tts!=null) tts.stop(); }
    public void refresh() { emit("state",status); }
    public void emit(String type,String message) {
        try {
            JSONObject event=state(); event.put("type",type); event.put("message",message);
            for(Listener listener:new ArrayList<>(listeners)) listener.event(event);
        } catch(Exception ignored) {}
    }
    public JSONObject state() {
        JSONObject object=new JSONObject();
        try {
            object.put("emotion",emotion).put("status",status).put("voice",voice).put("speechReady",speechReady).put("bubble",bubble)
                .put("microphone",context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
                .put("control",PhoneControlService.instance!=null).put("notifications",NotificationsService.instance!=null);
            object.put("history",new JSONArray(history));
        } catch(Exception ignored) {}
        return object;
    }
    public void tell(String message,boolean speak) {
        status=message;
        try {
            JSONObject item=new JSONObject().put("message",message).put("time",DateFormat.getTimeInstance(DateFormat.SHORT,new Locale("pt","BR")).format(new Date()));
            history.add(0,item); while(history.size()>20) history.remove(history.size()-1);
        } catch(Exception ignored) {}
        emit("reply",message);
        if(speak && voice && speechReady) tts.speak(message.substring(0,Math.min(message.length(),1800)),TextToSpeech.QUEUE_FLUSH,null,"sexta-reply");
    }
    public void execute(String input) {
        CommandParser.Command command=CommandParser.parse(input);
        execute(command);
    }
    public void execute(CommandParser.Command command) {
        stopSpeech();
        try {
            PhoneControlService control=PhoneControlService.instance;
            switch(command.type) {
                case "emotion": setEmotion(command.index); tell("Expressão: "+command.value+".",true); return;
                case "cancel": tell("Comando cancelado.",true); return;
                case "help": emit("help", "comandos"); tell("Diga: abrir WhatsApp, ler tela, tocar em um botão, escrever um texto, rolar para baixo, voltar, ler notificações ou fique feliz.",true); return;
                case "open-app": openApp(command.value); return;
                case "search": launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(command.value))),"Pesquisando: "+command.value+"."); return;
                case "dial": launch(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",command.value.replaceAll("[^+0-9]",""),null)),"Abri o discador. Confira o número e toque em ligar."); return;
                case "settings": launch(new Intent(Settings.ACTION_SETTINGS),"Abrindo as configurações."); return;
                case "time": tell("São "+DateFormat.getTimeInstance(DateFormat.SHORT,new Locale("pt","BR")).format(new Date())+".",true); return;
                case "date": tell("Hoje é "+DateFormat.getDateInstance(DateFormat.FULL,new Locale("pt","BR")).format(new Date())+".",true); return;
                case "battery":
                    BatteryManager battery=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
                    int level=battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    tell(level>=0 && level<=100?"A bateria está em "+level+" por cento.":"Não consegui consultar a bateria.",true); return;
                case "volume-up": case "volume-down":
                    AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,command.type.equals("volume-up")?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);
                    tell(command.type.equals("volume-up")?"Aumentei o volume de mídia.":"Diminuí o volume de mídia.",false); return;
                case "voice-on": setVoice(true); tell(speechReady?"Respostas faladas ativadas.":"Ative uma voz em português nas configurações de texto para fala do Android.",true); return;
                case "voice-off": setVoice(false); tell("Respostas faladas desativadas.",false); return;
                case "bubble-on": setBubble(true); tell(control!=null?"Botão flutuante ativado.":"Ative o controle da tela para usar o botão flutuante.",true); return;
                case "bubble-off": setBubble(false); tell("Botão flutuante oculto. Você pode chamá-la pelo aplicativo.",true); return;
                case "control-off":
                    if(control!=null) control.disableSelf();
                    tell("Controle da tela desativado.",true); return;
                case "read-notifications":
                    if(NotificationsService.instance==null) { tell("Ative a leitura de notificações na aba Acessos.",true); return; }
                    setEmotion(11); tell(NotificationsService.instance.readActive(),true); return;
                case "back": case "home": case "recents": case "notification-shade":
                    if(!requireControl(control)) return;
                    int action=command.type.equals("back")?AccessibilityService.GLOBAL_ACTION_BACK:command.type.equals("home")?AccessibilityService.GLOBAL_ACTION_HOME:command.type.equals("recents")?AccessibilityService.GLOBAL_ACTION_RECENTS:AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS;
                    tell(control.performGlobalAction(action)?"Comando executado.":"O Android não conseguiu executar esse comando.",true); return;
                case "read-screen":
                    if(!requireControl(control)) return;
                    setEmotion(11); tell(control.readScreen(),true); return;
                case "click":
                    if(!requireControl(control)) return;
                    control.clickText(command.value); return;
                case "write":
                    if(!requireControl(control)) return;
                    tell(control.writeText(command.value),true); return;
                case "scroll":
                    if(!requireControl(control)) return;
                    control.scroll(command.value); return;
                default: setEmotion(8); tell("Não reconheci esse comando. Experimente “abrir WhatsApp”, “ler tela” ou “fique feliz”.",true);
            }
        } catch(SecurityException error) {
            tell("O Android bloqueou essa ação. Confira as permissões na aba Acessos.",true);
        } catch(Exception error) {
            tell("Não consegui concluir a ação. Tente novamente com o aplicativo desejado aberto.",true);
        }
    }
    private boolean requireControl(PhoneControlService service) {
        if(service!=null) return true;
        tell("Ative o controle da tela na aba Acessos para usar esse comando.",true); return false;
    }
    private void launch(Intent intent,String reply) {
        try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); tell(reply,true); }
        catch(android.content.ActivityNotFoundException error) { tell("Não há um aplicativo instalado para essa ação.",true); }
    }
    private void openApp(String name) {
        String query=CommandParser.normalize(name);
        if(query.equals("camera")) { launch(new Intent("android.media.action.STILL_IMAGE_CAMERA"),"Abrindo a câmera."); return; }
        if(query.equals("telefone") || query.equals("discador")) { launch(new Intent(Intent.ACTION_DIAL),"Abrindo o telefone."); return; }
        if(query.equals("navegador") || query.equals("internet")) { launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")),"Abrindo o navegador."); return; }
        if(query.equals("configuracoes") || query.equals("ajustes")) { launch(new Intent(Settings.ACTION_SETTINGS),"Abrindo as configurações."); return; }
        PackageManager manager=context.getPackageManager();
        List<ResolveInfo> apps=manager.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        LinkedHashMap<String,ResolveInfo> exact=new LinkedHashMap<>(), partial=new LinkedHashMap<>();
        for(ResolveInfo app:apps) {
            String label=CommandParser.normalize(app.loadLabel(manager).toString());
            if(label.equals(query)) exact.put(app.activityInfo.packageName,app);
            else if(label.contains(query)) partial.put(app.activityInfo.packageName,app);
        }
        List<ResolveInfo> matches=new ArrayList<>((exact.isEmpty()?partial:exact).values());
        if(matches.isEmpty()) { tell("Não encontrei o aplicativo “"+name+"” instalado. Diga o nome mostrado no ícone.",true); return; }
        if(matches.size()>1) { tell("Encontrei mais de um aplicativo. Diga o nome completo: "+matches.get(0).loadLabel(manager)+" ou "+matches.get(1).loadLabel(manager)+".",true); return; }
        ResolveInfo app=matches.get(0);
        launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.activityInfo.packageName,app.activityInfo.name),"Abrindo "+app.loadLabel(manager)+".");
    }
}
