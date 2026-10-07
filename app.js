'use strict';
if(typeof globalThis==='undefined')window.globalThis=window;
const labels=['Neutra','Feliz','Triste','Brava','Surpresa','Sonolenta','Empolgada','Rindo','Confusa','Piscando','Apaixonada','Pensativa'];
const bridge=window.Android;
let state={emotion:0,history:[]};
const canvas=document.querySelector('#face');
const animator=new SextaFace.Animator(canvas,window.faceModels);
animator.setMotion(true,matchMedia('(prefers-reduced-motion: reduce)').matches);
animator.start();
function call(method,...args){
  if(bridge && typeof bridge[method]==='function') bridge[method](...args);
  else document.querySelector('#reply-text').textContent='Esta é a prévia da interface. Os comandos funcionam no aplicativo instalado no Android.';
}
function showPage(name){
  document.querySelectorAll('.page').forEach(page=>page.classList.toggle('active',page.id===name));
  document.querySelectorAll('[data-page]').forEach(button=>{
    button.classList.toggle('selected',button.dataset.page===name);
    if(button.dataset.page===name)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');
  });
  window.scrollTo({top:0});
  if(name==='home') requestAnimationFrame(()=>animator.resize());
  call('refresh');
}
document.querySelectorAll('[data-page]').forEach(button=>button.addEventListener('click',()=>showPage(button.dataset.page)));
document.querySelector('#listen').addEventListener('click',()=>call('listen'));
document.querySelector('#command-form').addEventListener('submit',event=>{
  event.preventDefault(); const input=document.querySelector('#command-input');const value=input.value.trim();
  if(value){call('execute',value);input.value='';input.blur();}
});
document.querySelectorAll('[data-command]').forEach(button=>button.addEventListener('click',()=>call('execute',button.dataset.command)));
document.querySelectorAll('[data-permission]').forEach(button=>button.addEventListener('click',()=>call('permission',button.dataset.permission)));
document.querySelector('#voice-toggle').addEventListener('change',event=>call('voice',event.target.checked));
document.querySelector('#bubble-toggle').addEventListener('change',event=>call('bubble',event.target.checked));
document.querySelector('#stop-control').addEventListener('click',()=>call('stopControl'));
labels.forEach((label,index)=>{
  const button=document.createElement('button');button.textContent=label;button.classList.toggle('active',index===0);button.setAttribute('aria-pressed',index===0?'true':'false');
  button.addEventListener('click',()=>{if(bridge)call('emotion',index);else{state.emotion=index;renderState();}});
  document.querySelector('#emotions').append(button);
});
const groups=[
  ['ABRIR E CONSULTAR',[
    ['Abrir WhatsApp','Use o nome de qualquer app instalado'],['Abrir câmera','Também funciona com telefone e navegador'],['Pesquise previsão do tempo','Abre a pesquisa no navegador'],['Bateria','Consulta o nível atual'],['Que horas são','Responde em voz alta'],['Que dia é hoje','Consulta a data'],['Ligue para 11999999999','Abre o discador para você conferir e ligar']]],
  ['CONTROLAR A TELA',[
    ['Ler tela','Lê o texto acessível do aplicativo aberto'],['Toque em Pesquisar','Use o nome de um botão visível'],['Escreva olá, tudo bem?','Selecione um campo de texto antes'],['Role para baixo','Também: cima, direita e esquerda'],['Voltar','Volta para a tela anterior'],['Tela inicial','Vai para o início do celular'],['Apps recentes','Abre os aplicativos recentes'],['Abrir notificações','Abre o painel do Android']]],
  ['NOTIFICAÇÕES E SOM',[
    ['Ler notificações','Lê até cinco notificações ativas'],['Aumente o volume','Ajusta o volume de mídia'],['Diminua o volume','Ajusta o volume de mídia'],['Desativar voz','Desliga as respostas faladas'],['Ativar voz','Liga as respostas faladas']]],
  ['EXPRESSÕES E CONTROLE',[
    ['Fique feliz','Troca o rosto da Sexta Feira'],['Fique com sono','Ativa a expressão sonolenta'],['Dê uma risada','Ativa a expressão rindo'],['Desativar botão flutuante','Oculta o botão sobre os apps'],['Ativar botão flutuante','Mostra o botão novamente'],['Desativar controle','Desliga o serviço de acessibilidade']]]
];
groups.forEach(([title,examples])=>{
  const group=document.createElement('div');group.className='command-group';const heading=document.createElement('h2');heading.textContent=title;group.append(heading);
  examples.forEach(([phrase,description])=>{
    const button=document.createElement('button');button.className='command-example';const span=document.createElement('span');span.textContent='“'+phrase+'”';const small=document.createElement('small');small.textContent=description;span.append(small);button.append(span);
    const arrow=document.createElementNS('http://www.w3.org/2000/svg','svg');const use=document.createElementNS('http://www.w3.org/2000/svg','use');use.setAttribute('href','#i-arrow');arrow.append(use);button.append(arrow);
    button.addEventListener('click',()=>{showPage('home');document.querySelector('#command-input').value=phrase;document.querySelector('#command-input').focus();});group.append(button);
  });document.querySelector('#command-list').append(group);
});
function renderState(){
  const index=Math.max(0,Math.min(11,Number(state.emotion)||0));
  if(animator.index!==index)animator.select(index);
  document.querySelector('#emotion-label').textContent=labels[index].toUpperCase();
  document.querySelectorAll('#emotions button').forEach((button,i)=>{button.classList.toggle('active',i===index);button.setAttribute('aria-pressed',i===index?'true':'false');});
  ['microphone','control','notifications'].forEach(key=>{const badge=document.querySelector('#p-'+key);badge.textContent=state[key]?'ATIVO':'PENDENTE';badge.classList.toggle('on',Boolean(state[key]));});
  document.querySelector('#voice-toggle').checked=Boolean(state.voice);
  document.querySelector('#bubble-toggle').checked=Boolean(state.bubble);
  document.querySelector('#stop-control').hidden=!state.control;
  document.querySelector('#face-state').textContent=state.control?'Com você, em qualquer app':'Ao seu comando';
  if(state.status)document.querySelector('#reply-text').textContent=state.status;
  const history=document.querySelector('#history');while(history.firstChild)history.removeChild(history.firstChild);
  const items=(state.history||[]).slice(0,5);
  document.querySelector('#history-count').textContent=String((state.history||[]).length).padStart(2,'0');
  if(!items.length){const empty=document.createElement('p');empty.className='empty';empty.textContent='Suas respostas aparecerão aqui.';history.append(empty);}
  items.forEach(item=>{const row=document.createElement('div');row.className='history-item';const p=document.createElement('p');p.textContent=item.message;const time=document.createElement('time');time.textContent=item.time;row.append(p,time);history.append(row);});
}
window.nativeEvent=event=>{
  state={...state,...event};renderState();
  if(event.type==='help')showPage('commands');
  if(event.type==='transcript')document.querySelector('#reply-text').textContent='Ouvi: “'+event.message+'”';
};
if(bridge)call('refresh');
document.addEventListener('visibilitychange',()=>{if(!document.hidden){animator.resize();call('refresh');}});
