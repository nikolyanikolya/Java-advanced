package info.kgeorgiy.ja.Ignatov.bank;

import org.junit.*;
import org.junit.rules.TestName; 

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class BankTest {

    private static Bank bank;
    private static final int PORT = 8888;
    private static final String OBJECT = "bank";
    private static final String URL = "//localhost:" + PORT + "/" + OBJECT;
    private static final int MAX_INDIVIDUALS = 1000;
    private static final int MAX_ACCOUNTS = 1000;
    private static String subId = "239";
    private static String name = "defaultName";
    private static String surname = "defaultSurname";
    private static String passport = "defaultPassport";
    private static final int TIMEOUT = 50;
    private static final int THREADS = 10;
    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void BeforeAllTests() throws RemoteException, MalformedURLException {
        LocateRegistry.createRegistry(PORT);
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        Naming.rebind(URL, bank);
    }

    @Test
    public void test01() throws RemoteException {
        setPersonData(testName.getMethodName());
        Person localPerson = bank.getIndividual(passport, true);
        Assert.assertNull("expected null localPerson", localPerson);
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        localPerson = bank.getIndividual(passport, true);
        Assert.assertNotNull("expected not null localPerson", localPerson);
        Assert.assertTrue("localPerson must be presented at the bank", bank.isExists(localPerson));
        Account account = bank.getAccount(localPerson, getId(passport, subId));
        Assert.assertNull("expected null account", account);
        account = bank.createAccount(getId(passport, subId));
        Assert.assertNotNull("expected not null account", account);
        Assert.assertEquals("expected empty set of accounts", 0,
                bank.getAllIds(localPerson).size());
    }

    @Test
    public void test02() throws RemoteException {
        setPersonData(testName.getMethodName());
        Person remotePerson = bank.getIndividual(passport, false);
        Assert.assertNull("expected Null remotePerson", remotePerson);
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        remotePerson = bank.getIndividual(passport, false);
        Assert.assertNotNull("expected not null remotePerson", remotePerson);
        Assert.assertTrue("remotePerson must be presented at the bank", bank.isExists(remotePerson));
        Account account = bank.getAccount(remotePerson, getId(passport, subId));
        Assert.assertNull("expected null account", account);
        account = bank.createAccount(getId(passport, subId));
        Assert.assertNotNull("expected not null account", account);
        Assert.assertEquals("expected zero account balance", 0, account.getAmount());
        Assert.assertEquals("incorrect id of account", getId(passport, subId), account.getId());
        Assert.assertEquals("Wrong number of accounts for remotePerson",
                1, bank.getAllIds(remotePerson).size());
    }

    @Test
    public void test03() throws RemoteException {
        setPersonData(testName.getMethodName());
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        var localPerson = bank.getIndividual(passport, true);
        var remotePerson = bank.getIndividual(passport, true);
        Assert.assertNotSame("expected that localPerson and remotePerson are different objects",
                localPerson, remotePerson);
        Assert.assertEquals("expected the same number of accounts for local and remote person",
                bank.getAllIds(localPerson), bank.getAllIds(remotePerson));
        Assert.assertEquals("expected empty set of accounts for localPerson",
                bank.getAllIds(localPerson), new HashSet<>());
    }

    @Test
    public void test04() throws RemoteException {
        for (int i = 0; i < MAX_INDIVIDUALS; i++) {
            String name = "Ivan" + i;
            String surname = "Ivanov" + i;
            String passport = "passport" + i;
            Assert.assertTrue("expected successful adding of new person",
                    bank.createIndividual(name, surname, passport));
            var remotePerson = bank.getIndividual(passport, false);
            Assert.assertNotNull("expected not null remotePerson", remotePerson);
            Assert.assertTrue("remotePerson must be presented at the bank", bank.isExists(remotePerson));
            Assert.assertEquals("invalid name of remotePerson", name, remotePerson.getName());
            Assert.assertEquals("invalid surname of remotePerson", surname, remotePerson.getSurName());
            Assert.assertEquals("invalid passport of remotePerson", passport, remotePerson.getPassport());
            Assert.assertEquals("expected empty set of accounts for remotePerson",
                    0, bank.getAllIds(remotePerson).size());
            var localPerson = bank.getIndividual(passport, true);
            Assert.assertNotNull("expected not null localPerson", localPerson);
            Assert.assertTrue("local must be presented at the bank", bank.isExists(localPerson));
            Assert.assertEquals("invalid name of localPerson", name, localPerson.getName());
            Assert.assertEquals("invalid surname of localPerson", surname, localPerson.getSurName());
            Assert.assertEquals("invalid passport of localPerson", passport, localPerson.getPassport());
            Assert.assertEquals("expected empty set of accounts for localPerson",
                    0, bank.getAllIds(localPerson).size());
        }
    }

    @Test
    public void test05() throws RemoteException {
        Assert.assertNull("expected null account", bank.getAccount(null, "239"));
        Assert.assertFalse("expected failure while adding of new person",
                bank.createIndividual("Ivan", null, null));
        Assert.assertFalse("expected failure while adding of new person",
                bank.createIndividual(null, "Ivanov", null));
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(null, null, "123"));
        Person person = new RemotePerson(null, null, null);
        Assert.assertFalse("expected failure while adding of new person",
                bank.createIndividual(null, null, null));
        Assert.assertFalse("person must not be presented at bank", bank.isExists(person));
    }

    @Test
    public void test06() throws RemoteException {
        setPersonData(testName.getMethodName());
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        var remotePerson = bank.getIndividual(passport, false);
        var localPerson = bank.getIndividual(passport, true);
        Assert.assertNotSame("expected that localPerson and remotePerson are different objects",
                localPerson, remotePerson);
        Account account1 = bank.createAccount(getId(passport, subId));
        Assert.assertEquals("expected one account of remotePerson",
                1, bank.getAllIds(remotePerson).size());
        Account account2 = bank.createAccount(getId(passport, subId));
        Assert.assertSame("expected that each account would be created only once",
                account1, account2);
        Assert.assertEquals("expected one account of remotePerson",
                1, bank.getAllIds(remotePerson).size());
        Assert.assertEquals("expected empty set of accounts for localPerson",
                0, bank.getAllIds(localPerson).size());
    }

    @Test
    public void test07() throws RemoteException {
        setPersonData(testName.getMethodName());
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        Account account = bank.createAccount(getId(passport, subId));
        Assert.assertEquals("expected zero account balance", 0, account.getAmount());
        account.setAmount(100);
        Assert.assertEquals("incorrect account balance after setting",
                100, account.getAmount());
    }

    @Test
    public void test08() throws RemoteException {
        setPersonData(testName.getMethodName());
        Assert.assertTrue("expected successful adding of new person",
                bank.createIndividual(name, surname, passport));
        Assert.assertNull("expected null account by null id and person",
                bank.getAccount(null, null));
        Assert.assertNull("expected null account by null id",
                bank.createAccount(null));
        var remotePerson = bank.getIndividual(passport, false);
        Assert.assertNotNull("expected not null remotePerson", remotePerson);
        var localPerson = bank.getIndividual(passport, true);
        Assert.assertNull("expected null account by null id",
                bank.getAccount(remotePerson, null));
        for (int i = 0; i < MAX_ACCOUNTS; i++) {
            String subId = "subId" + i;
            Assert.assertNull("expected that such account of remotePerson does not exist",
                    bank.getAccount(remotePerson, getId(passport, subId)));
            Assert.assertNull("expected that such account of localPerson does not exist",
                    bank.getAccount(localPerson, getId(passport, subId)));
            Account account = bank.createAccount(getId(passport, subId));
            Assert.assertNotNull("expected not null account", account);
            Assert.assertNotNull("expected not null account of remotePerson",
                    bank.getAccount(remotePerson, getId(passport, subId)));
            Assert.assertNull("expected not null account of localPerson",
                    bank.getAccount(localPerson, getId(passport, subId)));
            Assert.assertEquals("wrong number of accounts of remotePerson",
                    i + 1, bank.getAllIds(remotePerson).size());
            Assert.assertEquals("expected empty set of localPerson accounts",
                    0, bank.getAllIds(localPerson).size());
        }
    }

    @Test
    public void test09() {
        final ExecutorService executors = Executors.newFixedThreadPool(THREADS);
        setPersonData(testName.getMethodName());
        Collection<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> {
                long threadId = Thread.currentThread().getId();
                String s = "";
                try {
                    String name = BankTest.name + threadId;
                    String surname = BankTest.surname + threadId;
                    String passport = BankTest.passport + threadId;
                    String subId = BankTest.subId + threadId;
                    if (!bank.createIndividual(name, surname, passport)) {
                        s += "Expected successful adding of a new person";
                    }
                    var remotePerson = bank.getIndividual(passport, false);

                    s = checkCond(checkCond(s,
                                    Objects.isNull(remotePerson),
                                    "expected not null remotePerson\n"),
                            !bank.isExists(remotePerson),
                            "remotePerson must be presented at the bank\n");

                    Account account = bank.getAccount(remotePerson, getId(passport, subId));
                    s = checkCond(s, account != null, "expected null account\n");

                    account = bank.createAccount(getId(passport, subId));
                    if (account == null) {
                        s += "expected not null account\n";
                        return s;
                    }

                    return checkCond(checkCond(checkCond(s, account.getAmount() != 0,
                                            "expected zero account balance\n"),
                                    !getId(passport, subId).equals(account.getId()),
                                    "incorrect id of account\n"),
                            bank.getAllIds(remotePerson).size() != 1,
                            "Wrong number of accounts for remotePerson\n");

                } catch (RemoteException e) {
                    return "Some errors occurred while executing methods of a remoted interface. " + e.getMessage();
                }
            });
        }

        try {
            List<String> errors = executors.invokeAll(tasks).stream().map(s -> {
                try {
                    return s.get();
                } catch (InterruptedException | ExecutionException e) {
                    return "Error while executing the task. " + e.getMessage();
                }
            }).filter(s -> !s.isEmpty()).toList();
            if (!errors.isEmpty()) {
                Assert.fail("Bank operations are not thread-safe\n" + String.join("\n", errors));
            }
        } catch (InterruptedException e) {
            Assert.fail("Some of threads were interrupted. " + e.getMessage());
        }

        executors.shutdown();

        try {
            if (!executors.awaitTermination(
                    (long) THREADS * TIMEOUT, TimeUnit.SECONDS)) {
                Assert.fail("Pool did not terminate...");
            }
        } catch (InterruptedException e) {
            Assert.fail("Some threads were interrupted. " + e.getMessage());
        }

    }

    @AfterClass
    public static void afterAllMethods() throws RemoteException, MalformedURLException, NotBoundException {
        Naming.unbind(URL);
    }

    private static String getId(String passport, String subId) {
        return passport + ":" + subId;
    }

    private static void setPersonData(String methodName) {
        name = methodName + "Name";
        surname = methodName + "Surname";
        passport = methodName + "Passport";
        subId = methodName + "subId";
    }

    private static String checkCond(String s, boolean condition, String message) {
        if (condition) {
            s += message;
        }
        return s;
    }
}
