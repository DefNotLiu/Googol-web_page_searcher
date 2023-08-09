package com.example.servingwebcontent;

import java.rmi.*;
import java.util.Vector;

public interface SearchModule_I extends Remote {
    public String sayHello() throws java.rmi.RemoteException;    

    public Vector<SaveInformationBarrel> search(String message, String name, Client_I c, boolean webclient) throws java.rmi.RemoteException;

    public void subscribe(String name, Client_I c) throws java.rmi.RemoteException;
    public void subscribe(String name, Barrels_I c) throws java.rmi.RemoteException;
    public void subscribe(String name, Download_I c) throws java.rmi.RemoteException;

    public void acrescentarSearch(String url) throws java.rmi.RemoteException; //add isto

    public int consultarDownload() throws java.rmi.RemoteException;
    
}
