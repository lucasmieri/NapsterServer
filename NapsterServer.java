package MyNapster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NapsterServer {

	public static ConcurrentHashMap<String, Boolean> peerAlive = new ConcurrentHashMap<>(); // alive responses
    
    public static int serverPort = 10098;

    public static ConcurrentHashMap<String, ArrayList<String>> peerFiles = new ConcurrentHashMap<>(); // peers and files
    
    public static String serverAddress;
    
    public static String informedAddres;
    
    public static void main(String args[]) throws Exception {

       
        Scanner input = new Scanner(System.in);
      try {  
        System.out.println("Entre com o endereco IP do NapsterServer: ");
        System.out.println("Obs: Para usar a porta padrao (127.0.0.1) Digite: Default");
        informedAddres=input.nextLine();
        
        if(informedAddres.equals("Default")){
        	NapsterServer.serverAddress="127.0.0.1";
        }
        
        else {
        	NapsterServer.serverAddress=informedAddres;
        }
        
      }catch (Exception e) { System.out.println(e.getMessage());}
      
        
        //NapsterServer.serverAddress = input.nextLine();
        
        
    
        System.out.println("Set server port:"+serverPort);

        DatagramSocket serverSocket = new DatagramSocket(serverPort);

        

        
        System.out.println("Servidor Ativo");
        
        System.out.println("---------------------------------------------------------------------------------");
        // verificar a disponibilidade do peer
        checkAlive check = new checkAlive(serverSocket);
        check.start();

        while (true) {
            try {
                byte[] recBuffer = new byte[1024];
                int bufferSize = recBuffer.length;
                DatagramPacket recPacket = new DatagramPacket(recBuffer, bufferSize);
                serverSocket.receive(recPacket); // blocking

                //Recepcão de Novos Peers, inicia um novo Peer que e reconhecido pelo servidor
                clientReception peer = new clientReception(recPacket, serverSocket);
                peer.start();
            } catch (Exception e){ System.out.println(e.getMessage());

            }
        }
    }
    

    public static void send(MyNapster.Comunication resposta, DatagramSocket dataSocket, InetAddress InetAdd, int p) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
            BufferedOutputStream byteBuffer = new BufferedOutputStream(byteStream); 
            ObjectOutputStream objectOut = new ObjectOutputStream(byteBuffer);
            objectOut.flush();
            objectOut.writeObject(resposta);
            objectOut.flush();
            byte[] sendData = byteStream.toByteArray();
            int sendDataSize = sendData.length;

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendDataSize, InetAdd, p);
            dataSocket.send(sendPacket);
            objectOut.close();
        } catch (Exception e) {System.out.println(e.getMessage());

        }
    }

    public static void printFiles(ArrayList<String> files) {
    	int contador=0;
    	System.out.println("Arquivos identificados:");
        for (String file : files) {
            System.out.println(contador+":" + file);
            contador++;
        }

        System.out.println("---------------------------------------------------------------------------------");
    }
}

class clientReception extends Thread {

    public Comunication requisicao;
    public DatagramSocket serverSocket;

    public clientReception(DatagramPacket recPacket, DatagramSocket serverSocket) {
        try {
  
        	byte[] recGetData = recPacket.getData();
        	
        	
            ByteArrayInputStream byteStream = new ByteArrayInputStream(recGetData);
        	
            BufferedInputStream bufferInputByte = new BufferedInputStream(byteStream);
            ObjectInputStream objectIn = new ObjectInputStream(bufferInputByte);
            Comunication requisicao = (Comunication) objectIn.readObject();

            try {
            	
            InetAddress recPacktAdd = recPacket.getAddress();
            requisicao.setAddress(recPacktAdd);
            int recPacktPort = recPacket.getPort();
            requisicao.setPort(recPacktPort);

            this.requisicao = requisicao;
            this.serverSocket = serverSocket;}
            catch(Exception e) {
            	System.out.println("Server Request exception");
            	System.out.println(e.getMessage());}
        } catch (Exception e) { System.out.println(e.getMessage());

        }
    }

    public void run() {

        String req = this.requisicao.getmsg();
        String RequisicaoGetPort= String.valueOf(this.requisicao.getPort());
        String ip_porta = this.requisicao.getAddress().toString().replace("/", "") + ":" + RequisicaoGetPort;
        Comunication resposta = new Comunication();

        // verificar o status de disponibilidade do peer
        if (req.equals("ALIVE_OK")) { 
            if (NapsterServer.peerAlive.containsKey(ip_porta)) {
               try { NapsterServer.peerAlive.replace(ip_porta, true);}
               catch (Exception e) { System.out.println(e.getMessage());
               }
            }
        } else if (req.equals("JOIN")) {
            if (!NapsterServer.peerFiles.containsKey(ip_porta)) {
   
            	ArrayList<String> reqToList = this.requisicao.getList();
            	try {
                NapsterServer.peerFiles.put(ip_porta, reqToList);
                NapsterServer.peerAlive.put(ip_porta, true);}catch (Exception e) {System.out.println(e.getMessage());}
            }

 
            resposta.setmsg("JOIN_OK");
            
            InetAddress requisicaoAdd = this.requisicao.getAddress();
            
            
            resposta.setAddress(requisicaoAdd);
            
            int requisicaoPort = this.requisicao.getPort();
            
            resposta.setPort(requisicaoPort);
            
            NapsterServer.send(resposta, this.serverSocket, resposta.getAddress(), resposta.getPort());

            System.out.println("Novo Peer se juntou ao Server");
            System.out.println("Peer " + ip_porta + " arquivos");
            if (this.requisicao.getList()==null) {
            	System.out.println("Peer " + ip_porta + " adicionado, sem arquivos a serem compartilhados, peca um novo join caso deseje compartilhar algum arquivo");
            }
            
            else{
            	ArrayList<String> RequisicaoToLIST = this.requisicao.getList();
            	NapsterServer.printFiles(RequisicaoToLIST);
            }
            
        }
        else if (req.equals("SEARCH")) {

            ArrayList<String> peerList = new ArrayList<String>();

            if (NapsterServer.peerFiles.containsKey(ip_porta)) {

                System.out.println("Peer " + ip_porta + " solicitou arquivo " + requisicao.getList().get(0));


                Set<String> peers = NapsterServer.peerFiles.keySet();
                int count=0;
                for (String peer : peers) {
                    if (!peer.equals(ip_porta) && (NapsterServer.peerFiles.get(peer)).contains(requisicao.getList().get(0))) {
                    	System.out.println("Peer numero:"+count+" contem os arquivos");
                        peerList.add(peer);
                    }
                    count++;
                }
            } else {
                peerList = new ArrayList<String>(); 
            }


            resposta.setmsg("SEARCH_OK");
            resposta.setList(peerList);
            DatagramSocket svSocket = this.serverSocket;
            InetAddress requAdd = this.requisicao.getAddress();
            int rePort = this.requisicao.getPort();
            
            NapsterServer.send(resposta, svSocket, requAdd,rePort );
        } 
        else if (req.equals("LEAVE")) { 
        	//busca de key
            if (NapsterServer.peerFiles.containsKey(ip_porta)) {
       
            	System.out.println("O Peer"+ip_porta+ "deixara o servidor");
            	try {
            		// tentativa de remocao dos files referentes ao peer leave
                NapsterServer.peerFiles.remove(ip_porta);
                // para da busca de status de alive para o peer removido
                NapsterServer.peerAlive.remove(ip_porta);}
            	 catch (Exception e) { System.out.println(e.getMessage());
                 }
            }


            resposta.setmsg("LEAVE_OK");
            //aprovacao de leave, informando socket, ad
            DatagramSocket sendSocket = this.serverSocket;
            InetAddress ReqAdd = this.requisicao.getAddress();
            int ReqPort = this.requisicao.getPort();
            NapsterServer.send(resposta, sendSocket, ReqAdd, ReqPort);
        } 
        
        else if (req.equals("UPDATE")) { 
            if (NapsterServer.peerFiles.containsKey(ip_porta)) {

                if (!NapsterServer.peerFiles.get(ip_porta).contains(requisicao.getList().get(0))) {

                    ArrayList<String> updatedList = NapsterServer.peerFiles.get(ip_porta);
                    updatedList.add(requisicao.getList().get(0));
                    NapsterServer.peerFiles.replace(ip_porta, updatedList);
                    //atualiza a ip_porta e updatedlist
                }
            }


            resposta.setmsg("UPDATE_OK");
            DatagramSocket sendSocket = this.serverSocket;
            InetAddress ReqAdd = this.requisicao.getAddress();
            int ReqPort = this.requisicao.getPort();
            NapsterServer.send(resposta, sendSocket, ReqAdd, ReqPort);
        }
    }
}

//checkAlive controla o status dos peers
class checkAlive extends Thread {

    public DatagramSocket serverSocket;

    public checkAlive(DatagramSocket serverSocket) {
        this.serverSocket = serverSocket; //inst serversocket
    }

    public void run() {

        Comunication alive = new Comunication();
        alive.setmsg("ALIVE");


        while (true) {// verificar todos os peers
            for (String peer : NapsterServer.peerAlive.keySet()) {
                try { //Peers vivos
                    if (NapsterServer.peerAlive.get(peer)) {
                    	
                    	DatagramSocket SvSocket = this.serverSocket;
                    	InetAddress GetName = InetAddress.getByName(peer.split(":")[0]);
                    	int PeerInt = Integer.parseInt(peer.split(":")[1]);
                    	//envia a mensagem de alive
                        NapsterServer.send(alive,SvSocket ,GetName ,PeerInt);
                        //replace de status antigo
                        NapsterServer.peerAlive.replace(peer, false);
                        // peers mortos
                    } else {
                        NapsterServer.peerAlive.remove(peer);
                        System.out.println("Peer " + peer + " morto. Eliminando seus arquivos");
                        //array com a lista de files do peer
                        ArrayList<String> FilestoRemove = NapsterServer.peerFiles.get(peer);
                        // mata as informacoes do peer
                        NapsterServer.printFiles(FilestoRemove);
                        NapsterServer.peerAlive.remove(peer);
                        NapsterServer.peerFiles.remove(peer);
                    }
                } catch (Exception e) {	System.out.println("Exception in checkAlive");
            	System.out.println(e.getMessage());

                }
            }
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            	System.out.println("Exception in checkAlive");
            	System.out.println(e.getMessage());

            }
        }
    }
}


