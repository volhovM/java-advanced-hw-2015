package ru.ifmo.ctddev.volhov.rmi;

import ru.ifmo.ctddev.volhov.rmi.banksystem.Bank;
import ru.ifmo.ctddev.volhov.rmi.banksystem.BankImpl;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 * This class implements bank server that's capable of accepting requests
 * and changing user data as specified in {@link ru.ifmo.ctddev.volhov.rmi.banksystem.Bank} interface.
 *
 * @author volhovm
 *         Created on 5/13/15
 */
public class RMIServer {
    public static void main(String[] args) {
        try {
            Bank bank = new BankImpl();
            Naming.rebind("//localhost/bank", bank);
            System.out.println("Successfully started bank server");
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Couldn't start the bank server");
            e.printStackTrace();
        }
    }
}
