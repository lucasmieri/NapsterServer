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


public class NapsterPeer {
	
	public static Scanner userInput = new Scanner(System.in);
	
	public static void main (String args[]) throws Exception{


		InetAddress IPAddress= InetAddress.getByName("127.0.0.1");
		int serverPort = 10098;
		
		Scanner input = new Scanner(System.in);

  	    System.out.println("Infome a porta numerica do peer : "); 
  	    int clientPort = Integer.parseInt(input.nextLine());//Integer.parseInt(userInput.nextLine());// blocking, change client input do int
  	    
		

  	    System.out.println("Infome o IP do peer : "); 
  	    String clientAddress = userInput.nextLine();// blocking
		
		
		
  	    System.out.println("Infome a pasta : "); 
  	    String clientPath = userInput.nextLine();// blocking
        File[] listFiles = new File(clientPath).listFiles();
		
		
		
 
        ArrayList<String> fileNames = new ArrayList<String>();
        String myFileNames = null;
        for (File elementeFile : listFiles) {
			if (elementeFile.isFile())
            	myFileNames=elementeFile.getName();
                fileNames.add(myFileNames);
        }
		
        System.out.println("Arquivos encontrados na pasta do Peer:"+fileNames);

        
        ArrayList<String> peerFile = new ArrayList<String>();
        DatagramSocket clientSocket = new DatagramSocket(clientPort);
        
        
    	peerAcessRequest pr = new peerAcessRequest(clientPort, clientPath);
        pr.start();
       
        
    	ServerHandler server = new ServerHandler(clientSocket);
    	server.start();

    	String downloadFile = "";
    	
        Scanner in = new Scanner(System.in);
        while (true) {
        	try {
        		
        	System.out.println("---------------------------------------------------------------------------------");	
        	System.out.println("Menu de Opcoes");
        	System.out.println("Pressione 0 para JOIN");

        	System.out.println("Pressione 1 para SEARCH");

        	System.out.println("Pressione 2 para DOWNLOAD");
        	
        	System.out.println("Pressione 3 para LEAVE");
        	
        	int menu = in.nextInt();
        	
        	Boolean received = false; //wating for true
        	
        	ArrayList<String> ListaReqs = new ArrayList<String>();
        	
        	Comunication requisicao = new Comunication(); //start comunication
        	
        	

                if (menu == 0) { 
                    requisicao.setmsg("JOIN");
                    requisicao.setList(fileNames);
                    //aguardo por aprovacao
                    Comunication resposta = waitResponse("JOIN_OK", server, requisicao, clientSocket, IPAddress,
                            serverPort); // blocking

                    System.out.println("JOIN estabelecido");
                  
                    System.out.println("Sou peer " + resposta.getAddress().toString().replace("/", "") + ":"
                            + resposta.getPort() + " com arquivos");
                    printList(fileNames);
                    System.out.println("Pressione 0 para voltar ao Menu de Opcoes ou qualquer outro valor para sair");
                    int menuAuxiliar = in.nextInt();
                    
                    if(menuAuxiliar!=0) { 
                        requisicao.setmsg("LEAVE");

                   
                        Comunication resposta1 = waitResponse("LEAVE_OK", server, requisicao, clientSocket, IPAddress,
                                serverPort); // blocking

                    }
                    if(menuAuxiliar==0) {
                  System.out.println("Voltando ao Menu de Opcoes, JOIN concluido!");  
                    }

                } else if (menu == 1) { 
                    
                    requisicao.setmsg("SEARCH");

                    System.out.println("Entre com o nome do arquivo desejado: ");
                    downloadFile = userInput.nextLine();
                    ListaReqs.add(downloadFile);
                    requisicao.setList(ListaReqs);

                   
                    Comunication resposta = waitResponse("SEARCH_OK", server, requisicao, clientSocket, IPAddress,
                            serverPort); // blocking

                    peerFile = resposta.getList();

                    System.out.println("Peers doador:");
                    printList(resposta.getList());
                    
                    System.out.println("Pressione 0 para voltar ao Menu de Opcoes ou qualquer outro valor para sair");
                    int menuAuxiliar = in.nextInt();
                    
                    if(menuAuxiliar!=0) { 
                        Comunication requisicao1 = new Comunication();
                        requisicao1.setmsg("LEAVE");

                   
                        Comunication resposta1 = waitResponse("LEAVE_OK", server, requisicao1, clientSocket, IPAddress,
                                serverPort); // blocking

                    }
                    
                    if(menuAuxiliar==0) {
                    System.out.println("Voltando ao Menu de Opcoes, SEARCH concluido!");
                    }

                    ListaReqs.remove(ListaReqs);
                } else if (menu == 2) { 

                    requisicao.setmsg("SEARCH");
                    System.out.println("Entre com o nome do arquivo desejado: ");
                    downloadFile = userInput.nextLine();
                    ListaReqs.add(downloadFile);
                    requisicao.setList(ListaReqs);

                   
                    Comunication resposta = waitResponse("SEARCH_OK", server, requisicao, clientSocket, IPAddress,
                            serverPort); // blocking

                    peerFile = resposta.getList();
                    int peerFileSize=peerFile.size();
                    int count=0;
                    
                    boolean persists = false;
                    
                    if(!received) {
                    	 persists = true;
                    }
                    
                    else {
                    	 persists = false;
                    } 
                    	
                    
                    	System.out.println("Procurando pelo arquivo...");
                        while (persists) {
                      
                            	do {

                            	int index = count;
                            	Thread.sleep(6000);
                                String ip = peerFile.get(count).split(":")[0];
                                String count_aux=peerFile.get(count);
                                String[] count_split=count_aux.split(":");// moving the list
                                int port = Integer.parseInt(count_split[1]);


                                Socket mySocket = new Socket(ip, port);
                                OutputStream os = mySocket.getOutputStream();
                                DataOutputStream writer = new DataOutputStream(os);

                                writer.writeBytes(downloadFile + "\n");

                                String file_client = new File(clientPath, downloadFile).getAbsolutePath();
                                InputStream data_inputstream= mySocket.getInputStream();
                            
                                DataInputStream is = new DataInputStream(mySocket.getInputStream());
                                FileOutputStream fos = new FileOutputStream(
                                        new File(clientPath, downloadFile).getAbsolutePath());
                                byte[] buffer = new byte[1024];


                                int read;
                                if((read = is.read(buffer)) != -1) {
                                do {
                                	try{
                                		fos.write(buffer, 0, read);
                                	}catch (Exception e) { System.out.println(e.getMessage());}
                                	
                                }while((read = is.read(buffer)) != -1);
                                }
                                fos.close();
                                is.close();

                                byte[] b = new byte[32];
                                
                                InputStream fis = new FileInputStream(
                                        new File(clientPath, downloadFile).getAbsolutePath());
                                        
                                fis.read(b);

                                if (new String(b).equals("DOWNLOAD_NEGADO")) {
                                    new File(clientPath, downloadFile).delete();

                                    
                                    
                                    if(count==peerFileSize- 1) {
                                    	index=0;
                                    }
                                    else {
                                    	count++;
                                    }
                                    
                                    System.out.println("Download Negado");
                                    System.out.println("Download foi solicitado para o"+port+ip);
                                    System.out.println("pedindo agora para o peer "+ peerFile.get(index));

                                } else {
                                    received = true;
                                    persists=false;
                                    fileNames.add(downloadFile);
                                    System.out.println("Arquivo " + downloadFile + " baixado com sucesso na pasta ");
                                    System.out.println("Favor verificar o diretorio :");
                                    System.out.println(clientPath);
                                    break;

                                }
                         
                          
                             count++;
                        	}while(peerFileSize>count);

                        
                        ListaReqs.remove(ListaReqs);
                        count=0;
                        Comunication requisicao1 = new Comunication();
           
                        try {
                        	ListaReqs.add(downloadFile);
                        	requisicao1.setmsg("UPDATE");
                        	requisicao1.setList(ListaReqs);
                        
                        Comunication resposta1 = waitResponse("UPDATE_OK", server, requisicao1, clientSocket, IPAddress,
                                serverPort); // blocking
                        
        
                        }catch (Exception e) { System.out.println(e.getMessage());}
                    }
                    
                    
                    System.out.println("Pressione 0 para voltar ao Menu de Opcoes ou qualquer outro valor para sair");
                    int menuAuxiliar = in.nextInt();
                    
                    if(menuAuxiliar!=0) { 
                        Comunication requisicao1 = new Comunication();
                        requisicao1.setmsg("LEAVE");

                   
                        Comunication resposta1 = waitResponse("LEAVE_OK", server, requisicao1, clientSocket, IPAddress,
                                serverPort); // blocking

                    }
                    
                   if(menuAuxiliar==0) {
                    System.out.println("Voltando ao Menu de Opcoes, Download concluido!");
                   }
                    
                    
                } else if (menu == 3) { 

                    requisicao.setmsg("LEAVE");

               
                    Comunication resposta = waitResponse("LEAVE_OK", server, requisicao, clientSocket, IPAddress,
                            serverPort); // blocking

                    System.out.println("Leave concluido, obrigado por usar!");
                

                }
            } catch (Exception e) { System.out.println(e.getMessage());
            	
                System.out.println("Menu Exception");
            }
        }
		
		
	}

	public static void send(Comunication Com, DatagramSocket dataSocket, InetAddress InAdd,
			int p) {

        try {

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
            BufferedOutputStream bufferOut = new BufferedOutputStream(byteStream);
            ObjectOutputStream objectOut = new ObjectOutputStream(bufferOut);
            objectOut.flush();
            objectOut.writeObject(Com);
            objectOut.flush();
            byte[] byteArray = byteStream.toByteArray();
            byte[] sendData = byteArray;

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InAdd, p);
            dataSocket.send(sendPacket);
            objectOut.close();
        } catch (Exception e) { System.out.println(e.getMessage());

        }
		
	}
	




public static Comunication waitResponse(String r, ServerHandler myServerHandler, Comunication Com, DatagramSocket dataScoket, InetAddress InetAdd,
        int p) {
	  int timeoutResponse = 600; // time in seconds to send a new request
    send(Com, dataScoket, InetAdd, p);
    int t = 0;
    int sleepTime=1000;
    int sleepFator=100;
    int sleepBound=sleepTime*10;
    boolean control = false;
    while (!myServerHandler.getResposta().getmsg().equals(r)) {
        try {
        	
            Thread.sleep(sleepTime);
            if (sleepTime<sleepBound){
            	sleepTime+=sleepFator;
            }
            
            else if(!control) {
            	System.out.println("Demora na resposta, tente reiniciar a conecao ou aguarde pela resposta");
            	control=true;
            }
            t++;
        } catch (Exception e) { System.out.println(e.getMessage());
        }
        if (t == timeoutResponse) {
        	System.out.println("try to conect...");
        	t = 0;
        	send(Com, dataScoket, InetAdd, p);
            
        }
    }


    Comunication resposta = myServerHandler.getResposta();
    System.out.println("Fim da espera, resposta recebida");
    myServerHandler.setResposta(new Comunication());
    return resposta;
}

public static void printList(ArrayList<String> lista) {
	int contador = 0;
    for (String listElement : lista) {
        System.out.println(contador+":"+" " + listElement+ "");
        contador++;
    }
    
    System.out.println(lista.size()+" Encontrados");
}
}

//------------------------------- Class Definition -------------------------------


 class Comunication implements Serializable {
    /**
	 * 
	 */
	public static final long serialVersionUID = 1L;
	public String msg = " ";
	public InetAddress address;
	public int port;
	public ArrayList<String> list;

    public String getmsg() {
        return this.msg;
    }

    public void setmsg(String m) {
        this.msg = m;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public void setAddress(InetAddress a) {
        this.address = a;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ArrayList<String> getList() {
        return this.list;
    }

    public void setList(ArrayList<String> lista) {
        this.list = lista;
    }

    public void addFile(String s) {
        this.list.add(s);
    }
}

 

class AliveHandler extends Thread {

    public DatagramSocket clientSocket;
    public InetAddress IPAddress;
    public int serverPort;

    public AliveHandler(DatagramSocket socket, InetAddress address, int port) {
        this.clientSocket = socket;
        this.IPAddress = address;
        this.serverPort = port;
    }

    public void run() {
        try {
            Comunication resposta = new Comunication();
            resposta.setmsg("ALIVE_OK");
            DatagramSocket msgClientSocket = this.clientSocket;
            InetAddress msgIpAdress = this.IPAddress;
            int msgServerPort = this.serverPort;
            NapsterPeer.send(resposta, msgClientSocket, msgIpAdress, msgServerPort);
            //envia a mensagem de vivo para o servidor
        } catch (Exception e) { System.out.println(e.getMessage());

        }
    }
}


class ServerHandler extends Thread {

    public Comunication resposta;
    public DatagramSocket clientSocket;
    private Socket no=null;

    public ServerHandler(DatagramSocket socket) {
    	this.clientSocket = socket;
        this.resposta = new Comunication();
        
    }


    
    public void run() {
        try {
            while (true) {
            	
                byte[] recBuffer = new byte[1024];
                int buffer_size=recBuffer.length;
                DatagramPacket recPacket = new DatagramPacket(recBuffer, buffer_size);
                clientSocket.receive(recPacket); // blocking

                ByteArrayInputStream byteStream = new ByteArrayInputStream(recBuffer);
                ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(byteStream));
                Comunication resposta = (Comunication) objectIn.readObject();

                if (resposta.getmsg().equals("ALIVE")) {
                	try {
                    AliveHandler answer = new AliveHandler(this.clientSocket, recPacket.getAddress(),recPacket.getPort());
                    answer.start();}
                	catch (Exception e) { System.out.println("Alive Response: "+e.getMessage());}
                	
                } else {
                	//System.out.println("Not Alive");
                	System.out.println(resposta.getmsg());
                    setResposta(resposta);
                }
            }
        } catch (Exception e) { System.out.println("server handler"+e.getMessage());

        }
    }

    public void setResposta(Comunication r) {
        this.resposta = r;
    }

    public Comunication getResposta() {
        return this.resposta;
    }
}

class peerAcessRequest extends Thread {

    public int port;
    public String clientPath;

    public peerAcessRequest(int port, String filePath) {
        this.port = port;
        this.clientPath = filePath;
    }

    public void run() {
    	
    try {
    	ServerSocket serverSocket = new ServerSocket(this.port);

         
    	System.out.println("Making peer request");
            while (true) {
                try {
                	
                	Socket no = serverSocket.accept();
                    String receiveClientPath = this.clientPath;
                    // blocking
                    peerReception peer = new peerReception(no, receiveClientPath);
                    peer.start();
                } catch (Exception e) { System.out.println(e.getMessage());

                }
            }

        }catch (Exception e) { System.out.println("peer request"+e.getMessage());
    }
   }
}

class peerReception extends Thread {

    public Socket no;
    public String clientPath;

    public peerReception(Socket no, String f) {
        this.no = no;
        this.clientPath = f;
    }

    public void run() {

        try {

            InputStreamReader is = new InputStreamReader(this.no.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            String filetoSend = reader.readLine();

            DataOutputStream os = new DataOutputStream(this.no.getOutputStream());
            byte[] buffer = new byte[1024];


            if (new File(clientPath, filetoSend).isFile()) {

                FileInputStream fis = new FileInputStream(new File(clientPath, filetoSend).getAbsolutePath());

                int read = 0;
                while ((read = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }

                fis.close();
            } else {
                os.writeBytes("DOWNLOAD_NEGADO\n");
            }
        } catch (Exception e) { System.out.println(e.getMessage());

        }
    }
}




