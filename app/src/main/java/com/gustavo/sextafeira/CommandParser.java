package com.gustavo.sextafeira;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local, deterministic intent parsing. No screen data or commands are sent to a model. */
public final class CommandParser {
    public static final String[] EMOTIONS = {"Neutra", "Feliz", "Triste", "Brava", "Surpresa", "Sonolenta", "Empolgada", "Rindo", "Confusa", "Piscando", "Apaixonada", "Pensativa"};
    private static final String[][] ALIASES = {
        {"neutra","neutro","normal"}, {"feliz","alegre","sorria","sorrir"},
        {"triste","chateada"}, {"brava","bravo","irritada","zangada"},
        {"surpresa","surpreso","assustada"}, {"sonolenta","sono","durma"},
        {"empolgada","animada"}, {"rindo","rir","ria","risada","gargalhada"},
        {"confusa","confuso"}, {"piscando","piscadinha","piscar","pisque"},
        {"apaixonada","amor","coracoes"}, {"pensativa","pensando","pense"}
    };
    public static final class Command {
        public final String type, value;
        public final int index;
        Command(String type, String value, int index) { this.type=type; this.value=value; this.index=index; }
        public boolean needsScreen() { return type.equals("click") || type.equals("write") || type.equals("read-screen") || type.equals("scroll") || type.equals("back"); }
    }
    static Command cmd(String type) { return new Command(type,"",-1); }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }
    public static Command parse(String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.length()>1500) return cmd("unknown");
        String original=raw.trim();
        original=original.replaceFirst("(?iu)^(oi[, ]+|olá[, ]+|ei[, ]+)?(sexta[ -]?feira|sexta|cesta feira|sesta feira)[, :]*", "").trim();
        original=original.replaceFirst("(?iu)^por favor[, ]+", "").replaceFirst("(?iu)[, ]+por favor$", "").trim();
        String text=normalize(original);
        if (text.matches(".*\\b(nao|nunca|jamais)\\b.*") && !text.startsWith("escreva ") && !text.startsWith("digite ")) return cmd("cancel");
        if (text.matches("(cancelar|cancele|parar|pare)")) return cmd("cancel");
        if (text.matches("(ajuda|comandos|o que voce faz|me ajude)")) return cmd("help");
        if (text.matches("(voltar|volte|volta|retornar)")) return cmd("back");
        if (text.matches("(inicio|ir para (o )?inicio|tela inicial|va para (a )?tela inicial|home)")) return cmd("home");
        if (text.matches("(recentes|apps recentes|aplicativos recentes|abrir recentes)")) return cmd("recents");
        if (text.matches("(leia|ler|leia a|ler a|o que tem na) tela")) return cmd("read-screen");
        if (text.matches("(leia|ler|leia as|ler as|minhas) notificacoes")) return cmd("read-notifications");
        if (text.matches("(abrir|abra|mostrar|mostre) (as )?notificacoes")) return cmd("notification-shade");
        if (text.matches("(role|rolar|deslize|deslizar)( a tela)?( para)? (baixo|cima|direita|esquerda)")) return new Command("scroll",text.substring(text.lastIndexOf(' ')+1),-1);
        if (text.matches("(aumentar|aumente|subir|suba)( o)? volume|volume mais|mais volume")) return cmd("volume-up");
        if (text.matches("(diminuir|diminua|abaixar|abaixe)( o)? volume|volume menos|menos volume")) return cmd("volume-down");
        if (text.matches("(que horas sao|qual e a hora|hora|horas)")) return cmd("time");
        if (text.matches("(qual e a data|que dia e hoje|data|dia de hoje)")) return cmd("date");
        if (text.matches("(bateria|quanto tenho de bateria|nivel da bateria|quanto de bateria)")) return cmd("battery");
        if (text.matches("(configuracoes|abrir configuracoes|abra as configuracoes|ajustes)")) return cmd("settings");
        if (text.matches("(ativar|mostrar)( o)? botao flutuante")) return cmd("bubble-on");
        if (text.matches("(desativar|esconder|ocultar)( o)? botao flutuante")) return cmd("bubble-off");
        if (text.matches("(parar|desativar|desligar)( o)? controle( do celular)?")) return cmd("control-off");
        if (text.matches("(desativar|desligar|silenciar)( a)? voz|fique em silencio")) return cmd("voice-off");
        if (text.matches("(ativar|ligar)( a)? voz|volte a falar")) return cmd("voice-on");
        Matcher match=Pattern.compile("(?iu)^(escreva|digite|escrever|digitar)\\s+(.+)$").matcher(original);
        if (match.matches()) return new Command("write",match.group(2).trim(),-1);
        match=Pattern.compile("(?iu)^(toque|tocar|clique|clicar|aperte)(?:\\s+(?:em|no|na|o|a))?\\s+(.+)$").matcher(original);
        if (match.matches()) return new Command("click",match.group(2).trim(),-1);
        match=Pattern.compile("(?iu)^(pesquise|pesquisar|busque|buscar|procure)(?:\\s+(?:por|sobre))?\\s+(.+)$").matcher(original);
        if (match.matches()) return new Command("search",match.group(2).trim(),-1);
        match=Pattern.compile("(?iu)^(ligar|ligue|discar|disque)(?:\\s+para)?\\s+([+0-9 ()-]{3,30})$").matcher(original);
        if (match.matches()) return new Command("dial",match.group(2).trim(),-1);
        match=Pattern.compile("(?iu)^(abrir|abra|abre|iniciar)(?:\\s+(?:o|a|app|aplicativo))?\\s+(.+)$").matcher(original);
        if (match.matches()) return new Command("open-app",match.group(2).trim(),-1);
        String emotion=text.replaceFirst("^(fique|fica|ficar|seja|mostre|mostrar|mude para|mudar para|expressao|modo|de uma|de um)\\s+", "").replaceFirst("^(com|uma expressao de|uma cara de)\\s+", "");
        for (int i=0;i<ALIASES.length;i++) for(String alias:ALIASES[i]) if(emotion.equals(alias)) return new Command("emotion",EMOTIONS[i],i);
        return cmd("unknown");
    }
    private CommandParser() {}
}
