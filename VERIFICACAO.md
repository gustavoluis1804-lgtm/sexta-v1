# Verificação da versão 1.0

Realizada em 7 de outubro de 2026.

- Código Java e recursos Android compilados contra o SDK oficial 35.
- APK gerado para Android 8 / API 26 ou superior, com alvo API 35.
- Alinhamento do APK confirmado com `zipalign`.
- Assinatura verificada com `apksigner`: esquemas v2 e v3 válidos.
- Arquivo DEX analisado pelas ferramentas do SDK: versão 038 válida para API 26.
- Manifesto confirma as atividades, os serviços protegidos por permissões do
  sistema e as três permissões: microfone, internet e ajuste de áudio.
- 56 verificações do parser e da seleção de comandos passaram, incluindo as
  12 expressões, acentos, preservação do texto a escrever, cancelamentos e comandos
  não reconhecidos.
- Interface verificada em Chromium headless, com 20 verificações de interação:
  expressões, passagem de comandos à ponte nativa, formulário, exemplos, botões
  de acesso, desligamento do controle e estado das permissões.
- Sem erros JavaScript e sem largura excedente nas telas de 320, 360, 390, 430
  e 600 pixels. As telas de assistente e acessos foram inspecionadas visualmente.

## O que ainda depende do aparelho

Os testes de interface usam uma ponte Android simulada para confirmar os botões
e a renderização. Eles não simulam nem comprovam o funcionamento do microfone ou
da acessibilidade do Android real.

Não foi possível instalar este APK em um celular físico nesta sessão. Portanto,
a voz em português, os serviços de texto para fala, o botão flutuante e a interação
com aplicativos do usuário precisam ser confirmados após a instalação.

## Roteiro de teste no celular

1. Instalar e abrir o app. Confirmar que o rosto aparece e as 12 expressões mudam.
2. Na aba Acessos, permitir o microfone. Tocar em Falar e dizer “bateria”.
3. Confirmar que a frase é recebida e a resposta aparece. Se houver uma voz em
   português instalada, conferir a resposta falada.
4. Ativar o controle da tela. Abrir as configurações do Android, chamar pelo
   botão flutuante e dizer “ler tela”.
5. Pedir “toque em” seguido do nome de um botão visível. Confirmar a ação.
6. Em um campo de texto, pedir “escreva olá”. Conferir o texto preenchido.
7. Ativar a leitura de notificações e pedir “ler notificações”.
8. Pedir “desativar controle” e confirmar que o botão flutuante desaparece.

Se a voz não funcionar, a mensagem de erro mostrará o motivo recebido do serviço
de reconhecimento. O campo de texto e o microfone do teclado continuam disponíveis.
