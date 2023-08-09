package com.example.servingwebcontent;

import java.io.FileWriter;  
import java.io.IOException;
import java.io.File;

import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//class com informacao a guardar
//URL, título da página, citação de texto e palavras que sejam encontradas no documento HTML
class SaveInformation implements Serializable {

    private String URL;
    private String title;
    private String citations;
    private List<String> words;
    private List<String> listUrls;

    public SaveInformation(String URL, String title, String citations, List<String> words, List<String> listUrls){
        this.URL = URL;
        this.title = title;
        this.citations = citations;
        this.words = words;
        this.listUrls = listUrls;
    }

    public List<String> getListUrls() {
        return this.listUrls;
    }

    public void setListUrls(List<String> listUrls) {
        this.listUrls = listUrls;
    }

    public String getURL() {
        return this.URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public List<String> getWords() {
        return this.words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCitations() {
        return this.citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public String formatMessage(int msgIndex, String threadName){

        String listUrlsTmp = "";
        //+ lista de urls e palavras
        for (int i = 0; i < listUrls.size(); i++) {
            listUrlsTmp+=" url_" + i + " | " + listUrls.get(i) + ";";
        }

        String wordsTmp = "";
        //tirar os caracteres especiais das mensagens ; |
        for (int i = 0; i < words.size(); i++) {
            if(words.get(i).length()==0){
                words.remove(i);
                i--;
            }
            else {
                if(!words.get(i).matches("[a-zA-Z0-9]+")){

                    for (int j = 0; j < words.get(i).length(); j++) {
                        char charact = words.get(i).charAt(j);
                        if(charact=='|' || charact==';' || charact==',' || charact==' ' || charact=='\n' || charact=='\t'){
                            j-=1;
                            String tmp = words.get(i).replace(String.valueOf(charact), "");
                            if(tmp.length()==0){
                                words.remove(i);
                                i--;
                            }
                            else
                                words.set(i, tmp);
                        }
                    }
                }
                wordsTmp+=" word_" + i + " | " + words.get(i) + ";";
            }
        }
        
        String formattedMessage = "type | barrelInformation; " + "msgIndex | " + msgIndex + "; " + "originThread | " + threadName + "; " +
        "url_list | " + listUrls.size() + "; " + "plain_text | " + words.size() + "; " +//header
        "url | " + this.URL + "; " + "title | " + this.title + "; " + "citation | " + this.citations + ";" + listUrlsTmp + wordsTmp;

        return formattedMessage;
    }

    @Override
    public String toString() {
        return "{" +
            " URL='" + getURL() + "'" +
            ", title='" + getTitle() + "'" +
            ", citations='" + getCitations() + "}";
    }
}

class temporaryDownloaderHandler extends Thread{
    
    Download threadToWait;
    AtomicInteger numberDownloaders;

    public temporaryDownloaderHandler(Download thread, AtomicInteger numberDownloaders){
        this.threadToWait = thread;
        this.numberDownloaders = numberDownloaders;
    }

    public void run(){
        try {
            this.threadToWait.t.join();
            numberDownloaders.decrementAndGet();
            System.out.println(threadToWait.name + " saiu corretamente");
        } catch (InterruptedException e) {
            System.out.println("temporaryDownloaderHandler error for: " + threadToWait.name);
            e.printStackTrace();
        }
    }
}

public class Download extends UnicastRemoteObject implements Runnable, Download_I{

    static CopyOnWriteArrayList<String> Urls = new CopyOnWriteArrayList<String>();
    static CopyOnWriteArrayList<SaveInformation> Barrel = new CopyOnWriteArrayList<SaveInformation>();
    static AtomicInteger numberDownloaders = new AtomicInteger(0);  //atomic porqye as threads atualizam isto ao sair
    static AtomicInteger indexOfMsg = new AtomicInteger(0); //index das mensagens

    public String name;
    public Thread t;
    final static Lock lock = new ReentrantLock();
    final static Condition notEmpty = lock.newCondition();

    public static void increment() {
        numberDownloaders.incrementAndGet();
    }

    public void decrement() {
        numberDownloaders.decrementAndGet();
    }

    public static int value() {
        return numberDownloaders.get();
    }

    public Download(String name) throws RemoteException {
        this.name = name;
        t = new Thread(this, name);
    }

    public Download() throws RemoteException {
    }

    public String formatAskBarrel(String Url, String threadIndex){
 
        return "type | checkExist; " + "originThread | " + threadIndex + "; " + "url | " + Url + ";";

    }

    public void sendInformation(SaveInformation multicastPacket, MulticastSocket socket, InetAddress groupSND, int PORT, int index){

        try {
            byte[] buffer1 = multicastPacket.formatMessage(index, this.name).getBytes();
            System.out.println(multicastPacket.formatMessage(index, this.name) + "\n" + index);
            if(buffer1.length>65500){//dividir datapacket e mandar
                byte[][] splitedByteBuffer = byteBufferSplitter(buffer1, index);
                for (int j = 0; j < splitedByteBuffer.length; j++) {
                    String new1 = new String(splitedByteBuffer[j]);
                    System.out.println(new1);
                    System.out.println("sent");

                    DatagramPacket packetSnd = new DatagramPacket(splitedByteBuffer[j], splitedByteBuffer[j].length, groupSND, PORT); //sender packet
                    socket.send(packetSnd);
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                DatagramPacket packetSnd = new DatagramPacket(buffer1, buffer1.length, groupSND, PORT); //sender packet
                socket.send(packetSnd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int parseAnswer(String msg, String threadIndex){//0 se existir, 1 se n existir, 2 se n for a mensagem
        //"type | checkExistAns; " + "destinationThread | " + threadIndex + "; " + "ans | " + answer + ";";
        //"type | barrelInformationResend; index | " + i + ";"
        String[] msgParse = msg.split(";");
        if(msgParse[0].equals("type | checkExistAns") 
        && msgParse[1].equals(" destinationThread | " + threadIndex)){
            if(msgParse[2].equals(" ans | yes"))
                return -1;
            else
                return -2;
        }
        else if(msgParse[0].equals("type | barrelInformationResend") &&
        msgParse[1].split(" \\| ")[0].equals(" index") &&  Integer.parseInt(msgParse[1].split(" \\| ")[1])>=0){
            System.out.println("entra aqui");
            return Integer.parseInt(msgParse[1].split(" \\| ")[1]);
        }
            

        return -3;
    }

    public void emptyUrlList(CopyOnWriteArrayList<String> UrlList){
        for (int i = 0; i < Urls.size(); i++) {
            Urls.remove(i);
        }
    }

    public boolean addUrl(String url) throws RemoteException{
        //adicionar url a lista de urls
        
            lock.lock();
            if(Urls.size()==0)
                notEmpty.signal();
            if(Urls.add(url)){
                lock.unlock();
                return true;
            }
            lock.unlock();
            return false;

    }
  
    public int getNr() throws RemoteException {
        
        return numberDownloaders.get();
    }

    public byte[][] byteBufferSplitter(byte[] tmp, int indexMsg){

        System.out.println(tmp.length  + "\n\n\n");
        int numOfChunks = tmp.length/65000;
        if(tmp.length>numOfChunks*65000)
            numOfChunks+=1;
        byte[][] chunksArray = new byte[numOfChunks][];

        for (int i = 0; i < numOfChunks; i++) {
            if((i+1)*65000>tmp.length){
                int diff = tmp.length-i*65000;
                chunksArray[i] = Arrays.copyOfRange(tmp, i*65000, i*65000+diff);
            }
            else
                chunksArray[i] = Arrays.copyOfRange(tmp, i*65000, (i+1)*65000);
        }

        String[] stringBuffer = new String[numOfChunks];
        for (int i = 0; i < numOfChunks; i++) {
            String tmpString = new String(chunksArray[i]);
            if(i==0){
                stringBuffer[i] = "fragmentedPacket | " + numOfChunks + "; " + 
                "fragIndex | " + i + "; " + tmpString;
            }
            else{
                stringBuffer[i] = "fragmentedPacket | " + numOfChunks + "; " + 
                "fragIndex | " + i + "; "  + "type | barrelInformation; msgIndex | " + indexMsg +"; " + tmpString;
            }
            chunksArray[i] = stringBuffer[i].getBytes();
        }

        return chunksArray;
        
    }

    public static void main(String[] args){

        String tmp = 
        //"https://pt.wikipedia.org/wiki/Rita_Lee";
        "https://crawler-test.com/";
        //"https://pt.wikipedia.org/wiki/Hauru_no_Ugoku_Shiro";
        //"https://jsoup.org/cookbook/extracting-data/attributes-text-html";
        //Urls.add(tmp);

        String MULTICAST_ADDRESS = "224.3.2.2";
        int PORT = 4321;
        
        try {

            String ficheiro = "serverIp.txt";
            File myObj = new File(ficheiro);
            Scanner myReader = new Scanner(myObj);
            String data = myReader.nextLine();
            //System.out.println(data);
            myReader.close();
            //String name = "rmi://" + data + ":8000/projeto";
            //SearchModule_I h = (SearchModule_I) Naming.lookup(name);
            Registry registry = LocateRegistry.getRegistry(data); // Replace SERVER_IP with the actual IP
            SearchModule_I h = (SearchModule_I) registry.lookup("RemoteInterface");

            Download c = new Download(); 

            h.subscribe("queue", (Download_I) c);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            
            Download temp = new Download("Downloader " + Integer.toString(value()));
            System.out.println("Downloader " + Integer.toString(value()) + " ativo");
            increment();
            temporaryDownloaderHandler tempHandler = new temporaryDownloaderHandler(temp, numberDownloaders);
            temp.t.start();
            tempHandler.start();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public void run(){

        
        String MULTICAST_ADDRESS = "224.3.2.2";
        int PORT = 4321;

        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(PORT);  //to create socket without binding it->nothing in arguments (only for sending)
            InetSocketAddress group = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            NetworkInterface netIf = NetworkInterface.getByName("bge0");
            socket.joinGroup(group, netIf);
            InetAddress groupSND = InetAddress.getByName(MULTICAST_ADDRESS);

        
            while(true){

                //espera que seja acrescentado na fila
                if(Urls.isEmpty()){
                    lock.lock();
                    try{
                        while (Urls.size() == 0)
                            try {
                                notEmpty.await();
                                break;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    } finally { 
                        lock.unlock();
                    }
                }
                    
                if(indexOfMsg.get()==20){
                    emptyUrlList(Urls);
                    break;
                }

                try {

                    //verificar se esta no barrel
                    byte[] buffer = formatAskBarrel(Urls.get(0), this.name).getBytes();
                    DatagramPacket packetSnd = new DatagramPacket(buffer, buffer.length, groupSND, PORT); //sender packet
                    socket.send(packetSnd);
                    boolean contains = false;
                    while(true){
                        buffer = new byte[1024];
                        DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packetRcv);
                        String ans = new String(packetRcv.getData(), 0, packetRcv.getLength());
                        //System.out.println(ans);
                        int ansOpt = parseAnswer(ans, this.name);
                        System.out.println("ansOpt:\n" + ansOpt + "\n");
                        if(ansOpt==-1){
                            Urls.remove(0);
                            contains = true;
                            break;
                        }
                        else if(ansOpt==-2){
                            break;
                        }
                        else if(ansOpt>=0){
                            sendInformation(Barrel.get(ansOpt), socket, groupSND, PORT, ansOpt);
                        }
                    }
                    if(contains==true)
                        continue;
                    
                    //adicionar links na fila
                    Document doc = Jsoup.connect(Urls.get(0)).get();
                    Elements links = doc.select("a[href]");
                    for (Element link : links)
                        Urls.add(link.absUrl("href"));

                    //obter metadados e enviar para barrel
                    List<String> listUrls = links.eachAttr("abs:href");
                    List<String> plainText = new ArrayList<String>(Arrays.asList(doc.text().split(" "))) ;
                    String citations;
                    if(doc.text().length()>100)
                        citations = doc.text().substring(0, 100);
                    else
                        citations = doc.text().substring(doc.text().length());
                    SaveInformation multicastPacket = new SaveInformation(Urls.get(0), doc.title(), citations, plainText, listUrls);
                    Barrel.add(multicastPacket);
                    int tmpIndexMsg = indexOfMsg.get();

                    /*File myObj = new File("filename.txt");
                    if (myObj.createNewFile()) {
                        System.out.println("File created: " + myObj.getName());
                    } else {
                        System.out.println("File already exists.");
                    }
                    FileWriter myWriter = new FileWriter("filename.txt");
                    myWriter.write(multicastPacket.formatMessage(tmpIndexMsg, this.name));
                    myWriter.close();*/


                    /*byte[] buffer1 = multicastPacket.formatMessage(tmpIndexMsg, this.name).getBytes();
                    System.out.println(multicastPacket.formatMessage(tmpIndexMsg, this.name) + "\n" + tmpIndexMsg);
                    if(buffer1.length>65500){//dividir datapacket e mandar
                        byte[][] splitedByteBuffer = byteBufferSplitter(buffer1, tmpIndexMsg);
                        for (int j = 0; j < splitedByteBuffer.length; j++) {
                            String new1 = new String(splitedByteBuffer[j]);
                            System.out.println(new1);
                            System.out.println("sent");

                            packetSnd = new DatagramPacket(splitedByteBuffer[j], splitedByteBuffer[j].length, groupSND, PORT); //sender packet
                            socket.send(packetSnd);
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else{
                        packetSnd = new DatagramPacket(buffer1, buffer1.length, groupSND, PORT); //sender packet
                        socket.send(packetSnd);
                    }*/
                    sendInformation(multicastPacket, socket, groupSND, PORT, tmpIndexMsg);
                    
                    indexOfMsg.addAndGet(1);
                    Urls.remove(0);
                    
                }catch(SocketException e){
                    e.printStackTrace();
                    Urls.remove(0);
                }catch(IllegalArgumentException e){
                    e.printStackTrace();
                    Urls.remove(0);
                }catch(HttpStatusException e){
                    e.printStackTrace();
                    Urls.remove(0);
                }catch(UnsupportedMimeTypeException e){
                    e.printStackTrace();
                    Urls.remove(0);
                }catch (IOException e) {
                    e.printStackTrace();
                    Urls.remove(0);
                }

            }//while
        }catch (SocketException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
