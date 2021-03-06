package ru.ifmo.ctddev.volhov.rmi.banksystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementation if {@link ru.ifmo.ctddev.volhov.rmi.banksystem.Person} interface that
 * is operated via rmi as it extends {@link java.rmi.server.UnicastRemoteObject}. Thus it's
 * imported to rmiregistry as created and no extra effort is needed, except unregistering.
 *
 * @see java.rmi.server.UnicastRemoteObject
 * @author volhovm
 *         Created on 5/14/15
 */
public class RemotePerson extends UnicastRemoteObject implements Person {
    private final LocalPerson person;

    public RemotePerson(String name, String surname, String id) throws RemoteException {
        super();
        this.person = new LocalPerson(name, surname, id);
    }

    @Override
    public String getName() throws RemoteException {
        return person.getName();
    }

    @Override
    public String getSurname() throws RemoteException {
        return person.getSurname();
    }

    @Override
    public String getId() throws RemoteException {
        return person.getId();
    }

    @Override
    public PersonType getType() throws RemoteException {
        return PersonType.Remote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemotePerson)) {
            return false;
        }

        RemotePerson that = (RemotePerson) o;

        if (person != null ? !person.equals(that.person) : that.person != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 123123123;
        result = 31 * result + (person != null ? person.hashCode() : 0);
        return result;
    }
}
