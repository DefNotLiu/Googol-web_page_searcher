package com.example.servingwebcontent;

import java.util.Vector;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes.Name;

import javax.xml.crypto.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.sql.Timestamp;

public class Barrels extends UnicastRemoteObject implements Barrels_I, Runnable{
    //palavra, links
    static AtomicInteger numberBarrels = new AtomicInteger(0);
    static HashMap<String, HashSet<String>> index = new HashMap<String, HashSet<String>>();
    static ArrayList<Integer> msgIndexSaver = new ArrayList<Integer>(); 
    //verificar à chegada se tamanho desta fila corresponde ao indice da mensagem de chegada. Se não, verificar qual o elemento que está em falta
    //link, metadados(url,title,citation,links que apontam para este)
    static HashMap<String, SaveInformationBarrel> pointedLinks = new HashMap<String, SaveInformationBarrel>();

    String MULTICAST_ADDRESS = "224.3.2.2";
    int PORT = 4321;

    final Lock lockPointedLinks = new ReentrantLock();
    final Lock lockIndex = new ReentrantLock();

    private String name;
    private Thread t;
    
    public Barrels(String nome) throws RemoteException{
        name = nome;
        t = new Thread(this, name);
    }

    public Vector<SaveInformationBarrel> searchBarrels(String search){
        Vector<SaveInformationBarrel> result = new Vector<>();
               
        String[] palavrasSearch = search.split(" ");

        Vector<HashSet<String>> palavrasUrls = new Vector<>();
        
        lockIndex.lock();
        for(String palavra: palavrasSearch){
            //o hashset é null caso n exista tal palavra no index
            palavrasUrls.add(index.get(palavra));
        }
        lockIndex.unlock();
        
        Vector<String> listaUrl = new Vector<>();   

        if(palavrasUrls.isEmpty()){
            return null;
        }
//
        for(HashSet<String> urls: palavrasUrls){
            
            if(urls == null){
                continue;
            }

            for(String url: urls){
                if(!listaUrl.contains(url))
                    listaUrl.add(url);
            }
        }

        lockPointedLinks.lock();
        for(String ulrString: listaUrl){
            result.add(pointedLinks.get(ulrString));
        }
        lockPointedLinks.unlock();

        /*for(HashSet<String> urls: palavrasUrls){
            
            if(urls == null){
                continue;
            }

            for(String url: urls){
                
                int count=0;
                
                for(HashSet<String> urls2: palavrasUrls){
                    for(String url2: urls2){
                        if(url.equals(url2)){
                            count+=1;
                            break;
                        }
                    }
                    if(count == palavras){
                        break;
                    }
                }

                if(count == palavras){
                    listaUrl.add(url);
                }
            }
        }

        lockPointedLinks.lock();
        for(String ulrString: listaUrl){
            result.add(pointedLinks.get(ulrString));
        }
        lockPointedLinks.unlock();*/
        for (int i = 0; i < result.size(); i++) {
            System.out.println(result.get(i).getUrl());
        } 
        return result;
    } 
    
    public static void main(String[] args){

        Barrels tmp;
        try {
            tmp = new Barrels("Barrel " + numberBarrels.get());
            numberBarrels.incrementAndGet();
            tmp.t.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SaveInformationParser(String msg, FileWriter writerIndex, FileWriter writerData, MulticastSocket socket, int count){

        //MulticastSocket socket;
        try {
            //socket = new MulticastSocket(PORT);
            //InetSocketAddress group = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            //NetworkInterface netIf;
            //netIf = NetworkInterface.getByName("bge0");
            InetAddress groupSND = InetAddress.getByName(MULTICAST_ADDRESS);
            //socket.joinGroup(group, netIf);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String msgPrint = timestamp + " | " + msg;

            String[] msgParser = msg.split(";");
            if(msgParser[0].equals("type | barrelInformation")){
                //DownloadToBarrels = "type | barrelInformation; " + "msgIndex | " + msgIndex + "; " + "originThread | " + threadName + "; " +
        //"url_list | " + listUrls.size() + "; " + "plain_text | " + words.size() + "; " +//header
        //"url | " + this.URL + "; " + "title | " + this.title + "; " + "citation | " + this.citations + ";" + listUrlsTmp + wordsTmp;
                
                System.out.println(msgPrint.substring(0, 200) + "...");

                int msgIndex = Integer.parseInt(msgParser[1].split(" \\| ")[1]);

                //verificar se já foi recebida(pode ter sido pedido caso faltasse algum)
                if(msgIndexSaver.contains(msgIndex))
                    return;
                else{
                    msgIndexSaver.add(msgIndex);
                    if(count%5 == 0 && count>0){
                        for (int i = 0; i < msgIndex; i++) {
                            if(!msgIndexSaver.contains(i)){
                                String resendMsg = "type | barrelInformationResend; index | " + i + ";";
                                System.out.println("resend:\n" + resendMsg + "\n");
                                byte[] ansBuffer = resendMsg.getBytes();
                                DatagramPacket ansPacket = new DatagramPacket(ansBuffer, ansBuffer.length, groupSND, PORT);
                                socket.send(ansPacket);
                            }
                        }
                    }
                }

                System.out.println("\nINDEX MSG : " + msgIndex + "\n");
                //parsing e colocar na estrutura, lista de urls i=8:urlSize+7
                int urlSize = Integer.parseInt(msgParser[3].split(" \\| ")[1]);
                int wordsSize = Integer.parseInt(msgParser[4].split(" \\| ")[1]);
                String url = msgParser[5].split(" \\| ")[1];

                String title = "";
                if(msgParser[6].split(" \\| ").length==1)
                    title = "No title";
                else
                    title = msgParser[6].split(" \\| ")[1];

                String citation = "";
                if(msgParser[7].split(" \\| ").length==1)
                    citation = "No citation";
                else
                    citation = msgParser[7].split(" \\| ")[1];

                ArrayList<String> listaNova = new ArrayList<String>();
                
                SaveInformationBarrel tmpPointedLinks = new SaveInformationBarrel(title, citation, listaNova, url);

                //colocar os links
                lockPointedLinks.lock();
                //*este get só não é null quando é adicionado um SaveInformation temporario para armazenar a Lista de Urls
                //*dois casos onde se adicionam Urls no pointedLinks: quando é adicionado o Url vindo do download e quando se faz parsing
                //dos urls e é necessário guardar o Url vindo do download na sua lista
                //*caso for um SaveInformation apenas com a UrlList é o segundo caso acima(tem title e citation como "")
                if(pointedLinks.get(url)!=null){
                    if(pointedLinks.get(url).getCitations().equals("") && pointedLinks.get(url).getTitle().equals(""))
                        //adicionar o title e citation e manter os Urls ja indicados
                        tmpPointedLinks = new SaveInformationBarrel(title, citation, pointedLinks.get(url).getListUrls(), url);
                }
                pointedLinks.put(url, tmpPointedLinks);
                for (int i = 0; i < urlSize; i++) {
                    if(msgParser[i+8].equals(""))
                        continue;
                    if(msgParser[i+8].split(" \\| ").length>1 && pointedLinks.get(msgParser[i+8].split(" \\| ")[1])!=null){
                        if(pointedLinks.get(msgParser[i+8].split(" \\| ")[1]).getListUrls().contains(url))
                            continue;
                        pointedLinks.get(msgParser[i+8].split(" \\| ")[1]).getListUrls().add(url);
                    }
                    else{//se site nao existe vamos apenas inserir a lista com o URL atual a apontar para ele, aguardando os metadados pelo Downloader
                        ArrayList<String> tmpInformationUrlList = new ArrayList<String>();
                        tmpInformationUrlList.add(url);
                        SaveInformationBarrel tmpInformation = new SaveInformationBarrel("", "", tmpInformationUrlList, url);
                        //System.out.println("parserError: " + msgParser[i+8]);
                        pointedLinks.put(msgParser[i+8].split(" \\| ")[1], tmpInformation);
                    }
                }
                lockPointedLinks.unlock();

                //colocar no indice
                lockIndex.lock();
                for (int i = 0; i < wordsSize; i++) {
                    if(msgParser[i+urlSize+8].split(" \\| ").length>1){
                        HashSet<String> tmpListaParaIndex = index.get(msgParser[i+urlSize+8].split(" \\| ")[1]);
                        if(tmpListaParaIndex!=null){
                            tmpListaParaIndex.add(url);
                            index.put(msgParser[i+urlSize+8].split(" \\| ")[1], tmpListaParaIndex);
                        }
                        else{
                            tmpListaParaIndex = new HashSet<String>();
                            tmpListaParaIndex.add(url);
                            index.put(msgParser[i+urlSize+8].split(" \\| ")[1], tmpListaParaIndex);
                        }
                    }
                }
                lockIndex.unlock();


            }
            else if(msgParser[0].equals("type | checkExist")){
                //CheckIfUrlExist = "type | checkExistAns; " + "originThread | " + threadIndex + "; " + "url | " + Url + ";";
                System.out.println(msgPrint);
                String AnswerUrlExist = "type | checkExistAns; " + "destinationThread | " + msgParser[1].split(" \\| ")[1] + ";" + " ans | ";
                lockPointedLinks.lock();
                
                if(msgParser[2].split(" \\| ").length<2 ||
                    pointedLinks.get(msgParser[2].split(" \\| ")[1])==null || 
                (pointedLinks.get(msgParser[2].split(" \\| ")[1]).getTitle().equals("") &&
                pointedLinks.get(msgParser[2].split(" \\| ")[1]).getCitations().equals("")))
                    AnswerUrlExist+="no;";
                else
                    AnswerUrlExist+="yes;";
            
                lockPointedLinks.unlock();
                byte[] ansBuffer = AnswerUrlExist.getBytes();
                DatagramPacket ansPacket = new DatagramPacket(ansBuffer, ansBuffer.length, groupSND, PORT);
                socket.send(ansPacket);
            }
            else if(msgParser[0].split(" | ")[0].equals("fragmentedPacket")){
                int numFragments = Integer.parseInt(msgParser[0].split(" \\| ")[1]);
                int fragsReceived = 0;
                int msgIndex = 0;
                String msgFinal = "";
                String[] msgFinalBuffer = new String[numFragments];
                byte[] buffer = new byte[65500];
                DatagramPacket fragRcv = new DatagramPacket(buffer, buffer.length);

                while(true){
                    System.out.println("fragsReceived " + fragsReceived);
                    String[] tmpParser = msg.split(";", 5);
                    int fragIndex = Integer.parseInt(tmpParser[1].split(" \\| ")[1]);
                    //0-fragsize,1-fragindex,resto igual
                    if(fragsReceived == 0 && fragIndex==0){
                        msgIndex = Integer.parseInt(tmpParser[3].split(" \\| ")[1]);
                        msgFinalBuffer[0] = msg;
                    }
                    else if(Integer.parseInt(tmpParser[3].split(" \\| ")[1])==msgIndex){
                        msgFinalBuffer[fragIndex] = msg;
                    }
                    fragsReceived+=1;
                    if(fragsReceived>=numFragments)
                        break;
                    socket.receive(fragRcv);
                    System.out.println("aqui");
                    msg = new String(fragRcv.getData(), 0, fragRcv.getLength());
                }

                //concatenar buffer
                for (int j = 0; j < msgFinalBuffer.length; j++) {
                    String bufferBuffer = msgFinalBuffer[j];
                    
                    if(j==0){
                        String[] bufferBufferBuffer = bufferBuffer.split(";", 3);
                        msgFinal = msgFinal.concat(bufferBufferBuffer[2].substring(1, bufferBufferBuffer[2].length()));
                    }
                    else{
                        String[] bufferBufferBuffer = bufferBuffer.split(";", 5);
                        msgFinal = msgFinal.concat(bufferBufferBuffer[4].substring(1, bufferBufferBuffer[4].length()));
                    }
                }

                System.out.println(msgFinal);
                /*File myObj = new File("filename1.txt");
                    if (myObj.createNewFile()) {
                        System.out.println("File created: " + myObj.getName());
                    } else {
                        System.out.println("File already exists.");
                    }
                FileWriter myWriter = new FileWriter("filename1.txt");
                myWriter.write(msgFinal);
                myWriter.close();*/

                
            }
        }catch (SocketException e) {
            e.printStackTrace();
        }catch (UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(){
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

             
            h.subscribe("barrels", (Barrels_I) this);
        } catch (Exception e) {
            System.out.println("Exception in Barrels: "+ e);
        }
        

        /*HashSet<String> barrels = new HashSet<String>();
        barrels.add("palavra.pt");
        index.put("palavra", barrels);
        ArrayList<String> lista =  new ArrayList<String>();
        lista.add("url1");
        lista.add("url2");
        SaveInformationBarrel saveInfo = new SaveInformationBarrel("palvra","abc","isto e citacao",lista);
        pointedLinks.put("palavra.pt", saveInfo);*/

        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(PORT);
            InetSocketAddress group = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            NetworkInterface netIf = NetworkInterface.getByName("bge0");
            socket.joinGroup(group, netIf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int count = 0;
        
        while(true){

            try {

                File saveIndex = new File(this.getName() + ".txt");
                File saveData = new File(this.getName() + ".txt");
                saveIndex.createNewFile();
                //saveData.createNewFile();
                FileWriter writerIndex = new FileWriter(saveIndex);
                FileWriter writerData = new FileWriter(saveData);
                
                byte[] buffer = new byte[65500];

                DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);
                socket.receive(packetRcv);
                System.out.println("aqui ");
                String messageRcv = new String(packetRcv.getData(), 0, packetRcv.getLength()); //msg received
                System.out.println(messageRcv);
                SaveInformationParser(messageRcv, writerIndex, writerData, socket, count);
                count++;
                /*int index = 0;
                while(true){
                    socket.receive(packetRcv);
                    messageRcv = new String(packetRcv.getData(), 0, packetRcv.getLength());
                    writerData.write(messageRcv);
                    System.out.println(messageRcv + index + "\n");
                    if(messageRcv.equals("exit"))
                        break;
                    index+=1;
                }*/

                System.out.println("saiu");
                writerIndex.close();
                writerData.close();

            }
            catch (FileNotFoundException e) {
                System.out.println("FileNotFoundException ");
                e.printStackTrace();
            }
            catch (SocketException e) {
                System.out.println("SocketException ");
                e.printStackTrace();
            }
            catch (IOException e) {
                System.out.println("IOException ");
                e.printStackTrace();
            }
        }
        //socket.close();
    }

    public String getName() {
        return name;
    }

}

class SaveInformationBarrel implements Serializable{

    public String title;
    public String citations;
    public String url;
    public ArrayList<String> listUrls;

    public SaveInformationBarrel(String title, String citations, ArrayList<String> listUrls, String url) {
        this.title = title;
        this.citations = citations;
        this.listUrls = listUrls;
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ArrayList<String> getListUrls() {
        return this.listUrls;
    }

    public void setListUrls(ArrayList<String> listUrls) {
        this.listUrls = listUrls;
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

    public String formatMessage(){

        String formattedMessage = "type | barrelInformation; " + "";

        return formattedMessage;
    }

    @Override
    public String toString() {
        return "{"  +
            ", title='" + getTitle() + "'" +
            ", citations='" + getCitations() + "'" +
            "}";
    }

}