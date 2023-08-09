package com.example.servingwebcontent;

import java.rmi.Remote;

public interface Download_I extends Remote {

    public boolean addUrl(String url) throws java.rmi.RemoteException; //add isto

    public int getNr() throws java.rmi.RemoteException;
    
}
