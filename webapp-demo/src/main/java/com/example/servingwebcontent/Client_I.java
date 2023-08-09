package com.example.servingwebcontent;


import java.rmi.*;
import java.util.Vector;

public interface Client_I extends Remote {
   public void print_on_client(String s) throws java.rmi.RemoteException;

   public Vector<SaveInformationBarrel> searchClient(Vector<SaveInformationBarrel> result, boolean webclient) throws java.rmi.RemoteException;
}
