package com.mycompany.jgroupsp2pcopy;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;
import util.Mensagem;
import util.Status;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PeerExemplo extends ReceiverAdapter {
  private HashMap<String, String> arqsLocais;
  private HashMap<String, Address> arqsGlobais;

  private JChannel channel;
  private String user_name = System.getProperty("user.name", "n/a");
  private final List<String> state = new LinkedList<>();
  private View currentView;
  private Map<String, Address> users = new HashMap<>();
  // private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

  public void viewAccepted(View new_view) {
    System.out.println("Alguém entrou ou saiu: ** view: " + new_view);
    currentView = new_view;

    Address coordinator = currentView.getMembers().get(0);
    Mensagem m = new Mensagem("LISTAR_ARQUIVOS");
    Message msg = new Message(coordinator, m);
    try {
      channel.send(msg);
    } catch (Exception e) {
      System.err.println("Erro ao enviar mensagem para o coordenador: " + e.getMessage());
    }
  }

  public void receive(Message msg) {
    Mensagem m = (Mensagem) msg.getObject();
    Mensagem resposta = new Mensagem();
    Mensagem res = new Mensagem();
    Message responseMsg;

    try {
      switch (m.getOperacao()) {

        case "ADICIONAR_ARQUIVO":

          String fileName = (String) m.getParam("fileName");
          String fileContent = (String) m.getParam("fileContent");

          res = new Mensagem("RESPONSE_HANDLER");

          if (arqsLocais.containsKey(fileName)) {
            res.setStatus(Status.ERROR);
            res.setParam("msg", "Arquivo já existe localmente");
          } else if (arqsGlobais.containsKey(fileName)) {
            res.setStatus(Status.ERROR);
            res.setParam("msg", "Arquivo já existe no sistema");
          } else {
            arqsLocais.put(fileName, fileContent);

            // ! sucesso
            res.setStatus(Status.OK);
            res.setParam("msg", "Arquivo incluído com sucesso");

            // ! ENVIA PROS OUTROS PEERS
            resposta = new Mensagem("ADICIONAR_ARQUIVO_RESPONSE");
            resposta.setParam("fileName", fileName);
            responseMsg = new Message(null, resposta);
            channel.send(responseMsg);
          }
          responseMsg = new Message(msg.getSrc(), res);
          channel.send(responseMsg);

          break;

        case "ADICIONAR_ARQUIVO_RESPONSE":
          String fileName1 = (String) m.getParam("fileName");

          arqsGlobais.put(fileName1, msg.getSrc());

          resposta.setStatus(Status.OK);
          resposta.setParam("msg", "Arquivo adicionado com sucesso");

          break;

        case "REMOVER_ARQUIVO":
          String fileToRemove = (String) m.getParam("fileName");

          res = new Mensagem("RESPONSE_HANDLER");

          if (arqsLocais.containsKey(fileToRemove)) {
            arqsLocais.remove(fileToRemove);

            res.setStatus(Status.OK);
            res.setParam("msg", "Arquivo removido com sucesso");

            // ! ENVIA PROS OUTROS PEERS
            resposta = new Mensagem("REMOVER_ARQUIVO_RESPONSE");
            resposta.setParam("fileName", fileToRemove);
            responseMsg = new Message(null, resposta);
            channel.send(responseMsg);

          } else {
            res.setStatus(Status.ERROR);
            res.setParam("msg", "Arquivo não encontrado");
          }

          responseMsg = new Message(msg.getSrc(), res);
          channel.send(responseMsg);
          break;

        case "REMOVER_ARQUIVO_RESPONSE":
          String fileToRemove1 = (String) m.getParam("fileName");

          if (arqsGlobais.containsKey(fileToRemove1)) {
            arqsGlobais.remove(fileToRemove1);
          }
          break;

        case "LISTAR_ARQUIVOS":
          resposta.setStatus(Status.OK);
          System.out.println("LISTA LOCAL");
          for (String a : arqsLocais.keySet()) {
            System.out.println(a);
          }
          System.out.println("LISTA GLOBAL");
          for (String a : arqsGlobais.keySet()) {
            System.out.println(a);
          }
          break;

        case "CONTEUDO_ARQUIVO":
          String fileToRetrieve = (String) m.getParam("fileName");

          res = new Mensagem("RESPONSE_HANDLER");

          if (arqsLocais.containsKey(fileToRetrieve)) {
            String content = arqsLocais.get(fileToRetrieve);
            res.setStatus(Status.OK);
            resposta.setParam("fileContent", content);
            System.out.println("Conteúdo: " + content);
          } else if (arqsGlobais.containsKey(fileToRetrieve)) {
            Address ownerAddress = arqsGlobais.get(fileToRetrieve);

            // ! PEDE CONTEUDO PRO PEER QUE TEM O ARQUIVO
            resposta = new Mensagem("SOLICITAR_CONTEUDO_ARQUIVO");
            resposta.setParam("fileName", fileToRetrieve);
            Message request = new Message(ownerAddress, resposta);
            channel.send(request);

            res.setStatus(Status.OK);
            res.setParam("msg", "Arquivo solicitado ao peer responsável");
          } else {
            res.setStatus(Status.ERROR);
            res.setParam("msg", "Arquivo não encontrado");
          }
          responseMsg = new Message(msg.getSrc(), res);
          channel.send(responseMsg);
          break;

        case "SOLICITAR_CONTEUDO_ARQUIVO":

          String fileToRetrieve1 = (String) m.getParam("fileName");
          if (arqsLocais.containsKey(fileToRetrieve1)) {
            String content = arqsLocais.get(fileToRetrieve1);
            // ! PEDE CONTEUDO PRO PEER QUE TEM O ARQUIVO
            resposta = new Mensagem("MOSTRA_ARQUIVO_SOLICITADO");
            resposta.setParam("fileContent", content);
            responseMsg = new Message(msg.getSrc(), resposta);
            channel.send(responseMsg);
          }
          break;
        case "LISTAR_ARQUIVOS_RESPONSE":
          Map<String, Address> files = (Map<String, Address>) m.getParam("files");
          arqsGlobais.putAll(files);

          resposta.setStatus(Status.OK);
          resposta.setParam("msg", "Lista de arquivos recebida com sucesso");
          break;

        case "MOSTRA_ARQUIVO_SOLICITADO":
          String fileRetrievedContent = (String) m.getParam("fileContent");
          System.out.println("Conteúdo (global): " + fileRetrievedContent);
          break;

        case "ARQUIVO_ADICIONADO":
          String nomeArquivo = (String) m.getParam("nomeArquivo");
          String enderecoArquivo = (String) m.getParam("enderecoArquivo");
          arqsGlobais.put(nomeArquivo, msg.getSrc());

          resposta.setStatus(Status.OK);
          resposta.setParam("msg", "Arquivo adicionado com sucesso");
          break;

        case "RESPONSE_HANDLER":
          String response = (String) m.getParam("msg");
          Status status = m.getStatus();
          System.out.println();
          if (response != null) {
            System.out.println(status.toString() + " - " + response);

          } else {
            System.out.println(status.toString());
          }

          break;

        default:
          resposta.setStatus(Status.ERROR);
          resposta.setParam("errorMsg", "Operação inválida");
          break;
      }

      synchronized (state) {
        state.add(m.toString());
      }

    } catch (Exception e) {
      System.err.println("Erro ao receber a mensagem, verifique: " + e.getMessage());
      System.err.println("Obj msg =" + msg);
      System.err.println("Objeto m = " + m);
    }
  }

  public void getState(OutputStream output) throws Exception {
    synchronized (state) {
      Util.objectToStream(state, new DataOutputStream(output));
    }
  }

  public void setState(InputStream input) throws Exception {
    List<String> list = Util.objectFromStream(new DataInputStream(input));
    synchronized (state) {
      state.clear();
      state.addAll(list);
    }
    System.out.println("received state (" + list.size() + " messages in chat history):");
    list.forEach(System.out::println);
  }

  private void start() throws Exception {
    if (!login()) {
      System.out.println("Falha no login. Encerrando o programa.");
      return;
    }
    arqsLocais = new HashMap<>();
    arqsGlobais = new HashMap<>();
    channel = new JChannel();
    channel.setReceiver(this);
    channel.connect("ClusterExemplo");
    channel.getState(null, 10000);

    eventLoop();
    channel.close();
  }

  private void eventLoop() {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      try {
        mostraMenu();
        System.out.print("> ");
        System.out.flush();
        String line = in.readLine().toLowerCase();
        System.out.println();

        switch (line) {
          case "1":
            System.out.print("Digite o nome do arquivo: ");
            String fileName = in.readLine();
            System.out.print("Digite o conteúdo do arquivo: ");
            String fileContent = in.readLine();
            Mensagem m = new Mensagem("ADICIONAR_ARQUIVO");
            m.setParam("fileName", fileName);
            m.setParam("fileContent", fileContent);
            Message msg = new Message(channel.getAddress(), m);
            channel.send(msg);
            break;

          case "2":
            System.out.print("Digite o nome do arquivo a ser removido: ");
            String fileToRemove = in.readLine();
            Mensagem m2 = new Mensagem("REMOVER_ARQUIVO");
            m2.setParam("fileName", fileToRemove);
            Message msg2 = new Message(channel.getAddress(), m2);
            channel.send(msg2);
            break;

          case "3":
            Mensagem m3 = new Mensagem("LISTAR_ARQUIVOS");
            Message msg3 = new Message(channel.getAddress(), m3);
            channel.send(msg3);
            break;

          case "4":
            System.out.print("Digite o nome do arquivo a ser exibido: ");
            String fileToDisplay = in.readLine();
            Mensagem m4 = new Mensagem("CONTEUDO_ARQUIVO");
            m4.setParam("fileName", fileToDisplay);
            Message msg4 = new Message(channel.getAddress(), m4);
            channel.send(msg4);
            break;

          case "exit":
            return;

          default:
            System.out.println("Opção inválida");
            break;
        }

      } catch (Exception e) {
        System.err.println("Erro ao enviar a mensagem, verifique: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private boolean login() {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Digite o nome de usuário: ");
    String username = scanner.nextLine();
    System.out.print("Digite a senha: ");
    String password = scanner.nextLine();

    if (username.equals("admin") && password.equals("password")) {
      System.out.println("Login bem-sucedido. Bem-vindo, " + username + "!");
      return true;
    } else {
      System.out.println("Credenciais inválidas. Tente novamente.");
      return false;
    }
  }

  private void mostraMenu() {
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
    }

    System.out.println();
    System.out.println("===== MENU =====");
    System.out.println("1 - Adicionar arquivo");
    System.out.println("2 - Remover arquivo");
    System.out.println("3 - Listar arquivos");
    System.out.println("4 - Exibir conteúdo do arquivo");
    System.out.println("exit - Sair");
    System.out.println("================");

  }

  public static void main(String[] args) throws Exception {
    new PeerExemplo().start();
  }

}