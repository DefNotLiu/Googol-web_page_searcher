package com.example.servingwebcontent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;

public class SearchModule extends UnicastRemoteObject implements SearchModule_I{
    private Vector<Client_I> clients = new Vector<>();
    private HashMap<String, Vector<String>> searched = new HashMap<String, Vector<String>>();
    private Barrels_I clientBarrels;
    private Download_I clientURLQueue;

    private static final long serialVersionUID = 1L;

    public SearchModule() throws RemoteException{
        super();
    }

    public String sayHello() throws RemoteException{
        System.out.println("Printing on server...");
        return "Server listening...";
    }

    //falta trocar tipo de dados
    public Vector<SaveInformationBarrel> search(String m, String name, Client_I c, boolean webclient) throws RemoteException {
		System.out.println("Server: client " + name + " searched: " + m);
        
        if(searched.get(name) == null) {
            Vector<String> v = new Vector<>();
            v.add(m);
            searched.put(name, v); 
        }
        
        searched.get(name).add(m);

        Vector<SaveInformationBarrel> result = new Vector<>();

        System.out.println(clientBarrels);
        result = clientBarrels.searchBarrels(m);

        Vector<SaveInformationBarrel> a =  c.searchClient(result, webclient);

        return a;
	}

    //add isto
    public void acrescentarSearch(String url) throws RemoteException {
        
        if(clientURLQueue.addUrl(url)){
            System.out.println("Adicionado com sucesso");
        }
        else{
            System.out.println("Nao foi possivel adicionar");
        }
    }

    public int consultarDownload()throws RemoteException{
        
        int nrDownload = clientURLQueue.getNr();
        
        return nrDownload;
    } 

    public void subscribe(String name, Client_I c) throws RemoteException {
		System.out.println("Subscribing " + name);
		System.out.print("> ");
		clients.add(c);
	}
    public void subscribe(String name, Barrels_I c) throws RemoteException {
		System.out.println("Subscribing " + name);
		System.out.print("> ");
		clientBarrels = c;
	}
    public void subscribe(String name, Download_I c) throws RemoteException {
		System.out.println("Subscribing " + name);
		System.out.print("> ");
		clientURLQueue = c;
	}

    

    public static void main(String args[]){

       InetAddress[] ip;
       
        String ficheiro = "serverIp.txt";

        try {
            //So dá com o server num pc especifico
            System.setProperty("java.rmi.server.logCalls", "true");
            ip = InetAddress.getAllByName(/*"LAPTOP-HMR4DU6M" */"LAPTOP-M0EQKHP2");
            for (int i = 0; i < ip.length; i++) {
                System.out.println(ip[i]);
            }
            File myObj = new File(ficheiro);
            FileWriter myWriter = new FileWriter(myObj);
            myWriter.write(ip[1].getHostAddress());
            myWriter.close();
            System.setProperty("java.rmi.server.hostname", ip[1].getHostAddress());
            SearchModule server = new SearchModule();
            

            Registry registry = LocateRegistry.createRegistry(1099); // Default RMI registry port
            registry.rebind("RemoteInterface", server);
            /*System.setProperty("java.rmi.server.hostname", ip[1].getHostAddress());
            SearchModule h = new SearchModule();
            Registry a = LocateRegistry.createRegistry(8000);//.rebind("projeto", h);
            Naming.rebind("rmi://"+ip[1].getHostAddress()+":8000/projeto", h);
            System.out.println(System.getProperty("java.rmi.server.hostname"));*/
            System.out.println("Hello server ready.");
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();                   
        }
    }

   
}