# Gerar seu APK no GitHub

Você pode fazer pelo navegador do celular. Não precisa de Android Studio.
O pacote vem com o projeto e o arquivo `gerar-apk.yml`, que configura o GitHub Actions.

## Caminho mais fácil: enviar o ZIP

1. Baixe **Sexta-Feira-projeto.zip** e **gerar-apk.yml** recebidos nesta conversa.
   Mantenha o nome `Sexta-Feira-projeto.zip` e não extraia esse arquivo para o upload.
2. Entre na sua conta no [GitHub](https://github.com/new) e crie um repositório.
   Você pode chamá-lo de `sexta-feira`. Marque a opção de criar um README.
3. No repositório, escolha **Add file → Upload files**. Envie somente
   **Sexta-Feira-projeto.zip**, sem renomear, e salve em **Commit changes**.
4. Escolha **Add file → Create new file**. No campo do nome, digite exatamente:

   ```text
   .github/workflows/gerar-apk.yml
   ```

   Abra o arquivo `gerar-apk.yml` que você baixou, copie todo o texto para o editor
   do GitHub e salve em **Commit changes**, na branch padrão, normalmente `main`.
5. Abra **Actions → Gerar APK Sexta Feira**. A compilação inicia ao salvar na
   `main` ou `master`. Para iniciar outra execução, toque em **Run workflow → Run workflow**.
6. Abra a execução e espere ela terminar com o resultado verde. Em **Artifacts**,
   toque em **Sexta-Feira-APK**. Você precisa estar conectado à sua conta para baixar.
7. Extraia o ZIP baixado e instale **Sexta-Feira.apk** no seu Android.

Se alguma opção não aparecer no celular, ative **Versão para computador** no menu
do navegador. O GitHub Actions extrai o ZIP do projeto automaticamente.

O download fica disponível por 14 dias após a execução. Depois disso, você pode
executar o fluxo novamente para gerar um novo arquivo.

## Se quiser deixar todo o código no repositório

Extraia o ZIP e envie os arquivos de dentro da pasta `sexta-feira-android` para
a raiz do repositório. Na página inicial devem aparecer `app`, `tests`,
`build-apk.py`, `README.md` e os arquivos Gradle.

Inclua também `.github/workflows/gerar-apk.yml`. Se essa pasta não aparecer no
gerenciador de arquivos, crie o arquivo pelo editor do GitHub como no passo 4.
O mesmo fluxo funciona com o código extraído ou com o ZIP.

## Assinatura: teste e atualizações

O primeiro APK pode ser gerado sem configurar secrets. Nesse modo, cada execução
cria uma assinatura nova. Se você já instalou outro APK da Sexta Feira, será
necessário desinstalá-lo antes de instalar o novo; isso apaga os ajustes do app.

Para instalar novas versões sobre a anterior, use sempre a mesma chave de
assinatura. A chave fica fora do código do projeto. Não envie o arquivo de chave
como um arquivo comum do repositório.

Para configurar uma chave fixa com alias `sexta-feira` e a mesma senha para a
chave e o arquivo:

1. Converta sua chave para Base64. Em um computador com Python 3:

   ```sh
   python3 -c "import base64,pathlib; pathlib.Path('assinatura-base64.txt').write_text(base64.b64encode(pathlib.Path('sua-assinatura.keystore').read_bytes()).decode())"
   ```

2. No GitHub, abra **Settings → Secrets and variables → Actions → New repository secret**.
3. Crie **ANDROID_KEYSTORE_BASE64** com o conteúdo de `assinatura-base64.txt`.
4. Crie **ANDROID_KEYSTORE_PASSWORD** com a senha da sua chave.
5. Execute novamente **Gerar APK Sexta Feira**. O resumo indicará que sua chave
   foi usada. Guarde a chave original para assinar futuras atualizações.

Para atualizar o APK entregue anteriormente nesta conversa sem desinstalar,
precisa ser usada a chave que assinou aquele APK. Uma chave diferente permite
uma instalação nova depois da desinstalação, mas não uma atualização sobre ele.

## Se aparecer um erro

- **“Envie Sexta-Feira-projeto.zip…”**: o ZIP deve estar na raiz do repositório,
  com esse nome exato.
- **“Run workflow” não aparece**: confira se o YAML foi salvo em
  `.github/workflows/gerar-apk.yml`, na branch padrão, e se Actions está habilitado.
- **Erro nos secrets**: configure os dois secrets de assinatura ou remova ambos
  para usar o modo de teste.
- **Falha ao baixar as ferramentas**: tente executar novamente; a compilação
  baixa o SDK oficial do Android e o compilador Java pela internet.
- **APK não instala sobre o anterior**: configure a mesma assinatura ou
  desinstale a versão anterior antes de instalar.

## O que o fluxo faz

Baixa o código, prepara Java 17, extrai o projeto se necessário, executa os
56 testes de comandos, compila, alinha, assina e verifica o APK. O único arquivo
publicado como artifact é `Sexta-Feira.apk`.

Este fluxo foi verificado localmente. A execução no GitHub acontecerá depois que
você enviar os arquivos; nenhum repositório foi criado ou alterado por esta etapa.

Referências oficiais:

- [Iniciar um workflow manualmente](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
- [Baixar arquivos gerados pelo workflow](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)
- [Secrets no GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
