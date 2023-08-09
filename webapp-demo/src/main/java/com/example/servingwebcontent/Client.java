package com.example.servingwebcontent;

import java.io.File;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;



public class Client extends UnicastRemoteObject implements Client_I{

    protected Client() throws RemoteException {
        super();    
    }

    private static String name;

    public void print_on_client(String s) throws RemoteException{
        System.out.println("> " + s);
    }

    public Vector<SaveInformationBarrel> searchClient(Vector<SaveInformationBarrel> results, boolean webclient) throws RemoteException{
        System.out.println(results.size());
        if(results.isEmpty()){
            System.out.println("Pesquisa nao encontrada");
            return null;
        }
        Scanner sc = new Scanner(System.in);

        Vector<SaveInformationBarrel> sorted = new Vector<SaveInformationBarrel>();

        while(results.size()>0){
            int max_idx = 0;

            for (int j = 0; j < results.size(); j++){
                if (results.get(j).getListUrls().size() > results.get(max_idx).getListUrls().size())
                    max_idx = j;
                if(results.size()==1)
                    max_idx = 0;
            }
            System.out.println(results.get(max_idx).getUrl());
            sorted.add(results.get(max_idx));
            results.remove(max_idx);
        }

        if(webclient)
            return sorted;
        
        System.out.println(sorted.size());
        int indice = 0;
        while(indice<sorted.size()){
            int temp = 0;
            while(temp<10 && indice<sorted.size()){
                
                    System.out.println();
                    System.out.println("titulo: " + sorted.get(indice).getTitle());
                    System.out.println("URL: " + sorted.get(indice).getUrl());
                    System.out.println("Citaçao: " + sorted.get(indice).getCitations());
                    System.out.println();
    
                indice++;
                temp++;
            }
            
            System.out.println("Mais resultados(y/n)");
            
            switch(sc.nextLine()){
                case "y":
                    break;
                case "n":
                    //sc.close();
                    return null;
                default:
                    System.out.println("Bad input (y/n)");
            }
                  
        }
        
        //sc.close();
        return sorted;
    }

    public static void main(String args[]){
        
        try{
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
            Client c = new Client(); 

            /*if(args[0] == null){
                System.out.println("Arguments not valid");
                System.exit(1);
            }
            else{
                name = args[0];
            }*/
            
            h.subscribe("client", (Client_I) c);
            
           

            Scanner sc = new Scanner(System.in);
                     
            String op;
            while(true){
                menu();
                op = sc.nextLine();
                switch(op){
                    case "0":
                        pesquisa(h,c);
                        break;
                    case "1": //adicionei isto
                        acrescentar(h,c, "");
                        break;
                    case "2": 
                        consultar(h,c);
                        break;
                    case "3":
                        sc.close();
                        return;
                    default:
                        System.out.println("Bad input");
                        break;
                }
                sc.reset();
            } 

            
        }catch(Exception e){
            System.out.println("Exception in main: " + e);
            //e.printStackTrace();
        }
    }


    private static void menu() {
        System.out.println("Escolha 1 opçao:");
        System.out.println("0 - Pesquisa");
        System.out.println("1 - Acrescentar url"); //adicionei isto
        System.out.println("2 - Consultar info sistema");
        System.out.println("3 - Sair");
    }


    public static void pesquisa(SearchModule_I h, Client_I c){
        try {
            Scanner sc = new Scanner(System.in);
            System.out.println("Introduza a sua pesquisa: ");
            String message = sc.nextLine();

            h.search(message, name, c, false);

            
        } catch (Exception e) {
            System.out.println("Exception in pesquisa: " + e);
            e.printStackTrace();
        }
       
        
    }

    //adicionei isto
    public static void acrescentar(SearchModule_I h, Client c, String tmp) throws RemoteException{
        try{
            if(tmp==null || tmp.equals("")){
                Scanner sc = new Scanner(System.in);
                System.out.println("Introduza o Url que pretende acrescentar: ");
                String url = sc.nextLine();
                h.acrescentarSearch(url);
            }
            else
                h.acrescentarSearch(tmp);

        }catch(Exception e){
            System.out.println("Exception in acrescentar: "+e);
            e.printStackTrace();
        }
    }

    private static void consultar(SearchModule_I h, Client c) {
        try{
            int nrDownload = h.consultarDownload();
            System.out.println();
            System.out.println(nrDownload + " Downloader(s)");
            System.out.println();
        }catch(Exception e){
            System.out.println("Exception in consultar: "+e);
            e.printStackTrace();
        }
    }

}
