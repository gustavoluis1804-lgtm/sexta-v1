package com.gustavo.sextafeira;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Arrays;
import java.util.Comparator;

/** Read active notifications only on an explicit command. No persisted notification history. */
public class NotificationsService extends NotificationListenerService {
    public static NotificationsService instance;
    @Override public void onListenerConnected() { instance=this; AgentRuntime.get(this).refresh(); }
    @Override public void onListenerDisconnected() { if(instance==this) instance=null; AgentRuntime.get(this).refresh(); }
    @Override public void onDestroy() { if(instance==this) instance=null; AgentRuntime.get(this).refresh(); super.onDestroy(); }
    public String readActive() {
        StatusBarNotification[] items=getActiveNotifications();
        if(items==null || items.length==0) return "Você não tem notificações ativas.";
        Arrays.sort(items,Comparator.comparingLong(StatusBarNotification::getPostTime).reversed());
        StringBuilder output=new StringBuilder(); int count=0;
        for(StatusBarNotification item:items) {
            Notification n=item.getNotification();
            if((n.flags & Notification.FLAG_GROUP_SUMMARY)!=0 || item.getPackageName().equals(getPackageName())) continue;
            CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE), text=n.extras.getCharSequence(Notification.EXTRA_TEXT);
            if(title==null && text==null) continue;
            String label=item.getPackageName();
            try { label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(item.getPackageName(),0)).toString(); } catch(Exception ignored) {}
            if(count>0) output.append("\n");
            output.append(label).append(": ");
            if(title!=null) output.append(title).append(". ");
            if(text!=null) output.append(text);
            if(++count==5 || output.length()>1400) break;
        }
        return count==0?"Não há notificações com texto disponível.":"Suas notificações mais recentes:\n"+output;
    }
}
