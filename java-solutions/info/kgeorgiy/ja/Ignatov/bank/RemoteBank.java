package info.kgeorgiy.ja.Ignatov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>(); // id -> account
    private final ConcurrentMap<String, Person> individuals = new ConcurrentHashMap<>(); // passport -> Person
    private final ConcurrentMap<String, Set<String>> individualAccounts = new ConcurrentHashMap<>(); // passport -> Set<id>

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
        if (Objects.isNull(id)) {
            return null;
        }
        final String[] parts = id.split(":");
        final String passport = parts[0];
        final Account account = new RemoteAccount(id); // account with zero balance
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            if (individualAccounts.putIfAbsent(passport, new ConcurrentSkipListSet<>()) != null) {
                individualAccounts.get(passport).add(id);
            }
            return account;
        } else {
            return accounts.get(id);
        }
    }

    @Override
    public Set<String> getAllIds(Person person) {
        if (Objects.isNull(person)) {
            return null;
        }
        if (person instanceof LocalPerson localPerson) {
            return localPerson.getAllIds();
        } else {
            return individualAccounts.get(((RemotePerson) person).getPassport());
        }
    }

    @Override
    public boolean isExists(Person person) throws RemoteException {
        if (Objects.isNull(person) || Objects.isNull(person.getPassport())) {
            return false;
        }
        return individuals.containsKey(person.getPassport());
    }

    @Override
    public Account getAccount(final Person person, final String id) {
        if (Objects.isNull(person) || Objects.isNull(id)) {
            return null;
        }
        if (person instanceof LocalPerson localPerson) {
            return localPerson.getAccount(id);
        }
        return accounts.get(id);
    }

    @Override
    public boolean createIndividual(String name, String surname, String passport) throws RemoteException {
        if (Objects.isNull(passport)) {
            return false;
        }
        Person person = new RemotePerson(name, surname, passport);
        if (individuals.putIfAbsent(passport, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            individualAccounts.putIfAbsent(passport, new ConcurrentSkipListSet<>());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Person getIndividual(String passport, boolean isLocal) throws RemoteException {
        if (Objects.isNull(passport)) {
            return null;
        }
        Person person = individuals.get(passport);
        if (person == null) {
            return null;
        }
        if (!isLocal) {
            return person;
        }
        final Set<String> personIds = getAllIds(person);
        ConcurrentMap<String, LocalAccount> accounts = new ConcurrentHashMap<>();
        for (var personId : personIds) {
            accounts.put(personId, (LocalAccount) getAccount(person, personId));
        }
        return new LocalPerson(person.getName(), person.getSurName(), person.getPassport(), accounts);
    }

}
