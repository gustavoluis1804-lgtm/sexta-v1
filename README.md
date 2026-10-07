# Sexta Feira — assistente pessoal para Android

Primeira versão instalável da Sexta Feira. Usa reconhecimento de voz do Android,
respostas faladas em português, o rosto animado com 12 expressões e comandos locais
para abrir apps e ajudar a controlar a tela.

## Gerar o APK no GitHub

Veja **[GUIA-GITHUB.md](GUIA-GITHUB.md)**. Pelo celular, você pode enviar
`Sexta-Feira-projeto.zip` ao repositório e criar
`.github/workflows/gerar-apk.yml` com o arquivo pronto recebido nesta conversa.

O fluxo **Gerar APK Sexta Feira** prepara Java 17, extrai o projeto, testa,
compila e verifica o app. O APK fica em **Actions → execução → Artifacts →
Sexta-Feira-APK**. Também funciona com todo o código extraído na raiz do repositório.

## Instalar

1. Transfira `Sexta-Feira.apk` para um celular com Android 8 ou mais recente.
2. Abra o arquivo no celular e instale. Se o Android pedir, permita que o aplicativo
   usado para abrir o arquivo instale esse APK.
3. Abra **Sexta Feira → Acessos**. Configure **Microfone**.
4. Para controlar outros apps, toque em **Configurar controle**, escolha **Sexta Feira**
   nas opções de acessibilidade do Android e ative o serviço.
5. Para consultar os avisos, configure **Notificações**. Esse acesso é opcional.

Se aparecer “Configuração restrita”, abra **Configurações → Apps → Sexta Feira → ⋮ →
Permitir configurações restritas** e tente ativar novamente. A localização e o nome
podem variar por fabricante. A aba Acessos tem um botão para abrir as informações
do aplicativo.

## Usar em outro aplicativo

Abra o aplicativo desejado e toque no botão flutuante da Sexta Feira. Fale um comando
curto ou digite. A janela de voz fecha antes de executar a ação na tela que estava
aberta. Arraste o botão flutuante para mudar sua posição.

| Fale ou digite | Resultado |
| --- | --- |
| Abrir WhatsApp | Abre o aplicativo instalado com esse nome |
| Ler tela | Lê os textos acessíveis da tela |
| Toque em Pesquisar | Toca no item identificado por esse texto |
| Escreva olá, tudo bem? | Preenche o campo selecionado; confira antes de enviar |
| Role para baixo | Rola a tela; também aceita cima, direita e esquerda |
| Voltar | Volta para a tela anterior |
| Tela inicial | Abre o início do celular |
| Apps recentes | Abre os apps recentes |
| Ler notificações | Lê até cinco notificações ativas |
| Abrir notificações | Abre o painel de notificações |
| Aumente o volume | Ajusta o volume de mídia |
| Bateria | Consulta a porcentagem de bateria |
| Que horas são | Diz a hora |
| Pesquise previsão do tempo | Abre a pesquisa no navegador |
| Ligue para 11999999999 | Abre o discador; você confere e toca em ligar |
| Fique feliz | Troca a expressão; há 12 opções |
| Desativar voz | Desliga as respostas faladas |
| Desativar botão flutuante | Oculta o botão |
| Desativar controle | Desativa o serviço de acessibilidade |

Para escrever, toque primeiro no campo desejado. Se houver dois botões com o mesmo
nome, a assistente pede um nome mais específico. Uma frase negativa, como “não abra
WhatsApp”, cancela o comando.

## Voz

O microfone funciona em uma janela visível do aplicativo, e a escuta termina após
uma frase ou ao sair dessa janela. Se o aparelho tiver reconhecimento local,
ele é tentado primeiro. Se o português local não estiver disponível, a assistente
tenta o serviço de reconhecimento padrão do Android, que pode precisar de internet
e seguir as condições de processamento do fornecedor desse serviço.

Se a voz não estiver disponível, use o campo de texto ou o microfone do teclado.
As respostas faladas dependem de uma voz de texto para fala instalada em português;
o botão **Ajustar a voz do Android** abre essas configurações.

## Acesso e limites

Esta versão é um assistente de comandos locais. **Não está conectada a um modelo
de IA para conversas livres ou planejamento autônomo.** Não pede uma chave de API.
Funciona segundo as permissões concedidas pelo Android; um app comum não recebe
acesso irrestrito ao sistema ou aos dados privados de outros apps.

O serviço de acessibilidade consulta a tela apenas para executar o comando pedido.
Não registra eventos da tela nem envia seu conteúdo para servidores. Campos de
senha não são lidos ou preenchidos. Telas protegidas, jogos, imagens sem texto
acessível e alguns aplicativos podem limitar leitura ou interação.

A leitura de notificações consulta os avisos ativos apenas quando solicitada. As
últimas respostas aparecem durante a sessão e não são gravadas em um arquivo.
Os ajustes de voz e botão flutuante ficam salvos localmente. Nenhuma coleta de
dados, serviço de nuvem, conta ou anúncio foi incluído.

## Compilar o projeto

Abra esta pasta no Android Studio e compile o módulo `app` (SDK 35, Java 17,
Gradle 8.9, plugin Android 8.7.3). Não há bibliotecas externas de execução.

Como alternativa no Linux, com Java 17, Python 3 e acesso à internet:

```sh
python3 build-apk.py
```

O script baixa as ferramentas oficiais do SDK Android e o compilador Eclipse ECJ,
compila recursos e Java, converte para DEX, alinha e assina o APK. O resultado fica
em `build/Sexta-Feira.apk`. A chave de assinatura local fica na pasta `android-tools`,
fora do projeto. Guarde essa chave para assinar futuras atualizações compatíveis;
uma compilação em outra máquina cria uma assinatura diferente.

Para compilar e rodar os testes de comandos na mesma execução:

```sh
python3 build-apk.py --test
```

Uma chave existente pode ser usada com `--keystore CAMINHO`, com a senha em
`SEXTA_STORE_PASSWORD`. `SEXTA_KEY_PASSWORD` e `SEXTA_KEY_ALIAS` podem ser definidos
se a senha da chave ou o alias forem diferentes. O fluxo do GitHub usa os secrets
opcionais explicados no guia para manter a mesma assinatura em atualizações.

## Verificação realizada

Consulte `VERIFICACAO.md` para os resultados da compilação, assinatura e testes.
O funcionamento real do microfone, do reconhecimento em português e dos comandos
entre apps ainda precisa ser confirmado no celular do usuário.

## Referências de plataforma

- [Serviço de acessibilidade](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Reconhecimento de voz](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Configurações restritas no Android](https://support.google.com/android/answer/12623953?hl=pt-BR)
