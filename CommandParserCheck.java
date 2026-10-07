import com.gustavo.sextafeira.CommandParser;

public class CommandParserCheck {
    private static int total;
    private static void check(String input,String expected) {
        String actual=CommandParser.parse(input).type;
        if(!expected.equals(actual)) throw new AssertionError(input+": expected "+expected+", received "+actual);
        total++;
    }
    public static void main(String[] args) {
        String[][] cases={
            {"Abrir WhatsApp","open-app"},{"Sexta Feira, abra o YouTube, por favor","open-app"},
            {"Abra a câmera","open-app"},{"Abrir configurações","settings"},
            {"Ler tela","read-screen"},{"Leia a tela","read-screen"},
            {"Toque em Pesquisar","click"},{"Clique no botão de pesquisa","click"},
            {"Escreva Olá, tudo bem?","write"},{"Digite não quero sair","write"},
            {"Role para baixo","scroll"},{"Deslize a tela para cima","scroll"},
            {"Role para esquerda","scroll"},{"Voltar","back"},{"Tela inicial","home"},
            {"Apps recentes","recents"},{"Abrir notificações","notification-shade"},
            {"Ler notificações","read-notifications"},{"Aumente o volume","volume-up"},
            {"Diminua o volume","volume-down"},{"Que horas são?","time"},
            {"Que dia é hoje?","date"},{"Bateria","battery"},
            {"Pesquise previsão do tempo","search"},{"Ligue para +55 (11) 99999-9999","dial"},
            {"Fique feliz","emotion"},{"Fique com sono","emotion"},{"Dê uma risada","emotion"},
            {"Desativar voz","voice-off"},{"Ativar voz","voice-on"},
            {"Desativar botão flutuante","bubble-off"},{"Ativar botão flutuante","bubble-on"},
            {"Desativar controle","control-off"},{"Ajuda","help"},
            {"Não abra WhatsApp","cancel"},{"Nunca toque em enviar","cancel"},
            {"Pare","cancel"},{"Hoje estou feliz mas não abra apps","cancel"},
            {"Estou feliz hoje","unknown"},{"Fique triste e depois feliz","unknown"},
            {"","unknown"},{"Excluir todos os arquivos","unknown"}
        };
        for(String[] item:cases) check(item[0],item[1]);
        String[] emotions={"neutra","feliz","triste","brava","surpresa","sonolenta","empolgada","rindo","confusa","piscando","apaixonada","pensativa"};
        for(int i=0;i<emotions.length;i++) {
            CommandParser.Command command=CommandParser.parse("Fique "+emotions[i]);
            if(!command.type.equals("emotion") || command.index!=i) throw new AssertionError("Wrong emotion: "+emotions[i]);
            total++;
        }
        String written=CommandParser.parse("Sexta Feira, escreva Olá, não esqueça: 10h!").value;
        if(!written.equals("Olá, não esqueça: 10h!")) throw new AssertionError("Written text was modified: "+written);
        total++;
        if(!CommandParser.parse("ler tela").needsScreen() || CommandParser.parse("abrir WhatsApp").needsScreen()) throw new AssertionError("Screen routing");
        total++;
        System.out.println(total+" command parsing and routing checks passed.");
    }
}
