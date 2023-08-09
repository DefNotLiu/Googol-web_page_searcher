package com.example.servingwebcontent;

import java.rmi.Remote;
import java.util.*;

public interface Barrels_I extends Remote{
        public Vector<SaveInformationBarrel> searchBarrels(String search) throws java.rmi.RemoteException;
}
