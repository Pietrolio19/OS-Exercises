
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;


public class Compito_PriorityResourceManager {
    public static void main(String[] args) throws InterruptedException {
        int N = 2;
        int M = 4;
        int P = 3;
        int T1 = 3;
        int T2 = 0;
        PriorityResourceManager rm = new PriorityResourceManager(N, P);
        Requester[] rq = new Requester[M];
        for (int i = 0; i < M; i++) {
            rq[i] = new Requester(i, i < P ? i : P - 1, T1, T2, rm);
            rq[i].setName("R" + i);
            rq[i].start();
            Thread.sleep(1000);
        }
        Thread.sleep(60000);
        for (int i = 0; i < rq.length; i++) {
            rq[i].interrupt();
        }
        for (int i = 0; i < rq.length; i++) {
            rq[i].join();
            System.out.println("R" + i + ": " + rq[i].count);
        }
    }
}

class PriorityResourceManager {
    int nResources;
    ArrayList<Integer>[] requests;

    PriorityResourceManager(int maxResources, int maxPriority) {
        this.nResources = maxResources;
        this.requests = new ArrayList[maxPriority];
        for (int i = 0; i < maxPriority; i++)
            this.requests[i] = new ArrayList<>();
    }

    public synchronized void acquire(int id, int priority) throws InterruptedException {
        requests[priority].add(id);
        while (check(id, priority)) {
            System.out.println("R" + id + " wait");
            wait();
        }
        nResources--;
    }

    private boolean check(int id, int priority) {
        //ritorna false se e solo se il thread può accedere alla risorsa
        if (nResources == 0)
            return true;

        for (int i = 0; i < priority; i++) { //se tutte le liste dell'array di richiesta sono piene ci sono thread in attesa con priorità maggiore
            if (!requests[i].isEmpty())
                return true;
        }

        if (requests[priority].get(0) == id) {
            requests[priority].remove(0);
            return false;
        }
        return true;
    }

    public synchronized void release() throws InterruptedException {
        nResources++;
        System.out.println(Thread.currentThread().getName() + " released");
        notifyAll();
    }
}

class Requester extends Thread {
    int id, prio, T1, T2, count;
    PriorityResourceManager manager;

    Requester(int id, int priority, int T1, int T2, PriorityResourceManager rm) {
        this.id = id;
        this.prio = priority;
        this.T1 = T1;
        this.T2 = T2;
        this.manager = rm;
    }

    public void run() {
        try {
            while (true) {
                System.out.println(getName() + " " + prio + " requesting");
                manager.acquire(id, prio);
                System.out.println(getName() + " " + prio + " acquired");
                try {
                    sleep(T1 * 1000);
                    System.out.println(getName() + " " + prio + " released");
                    count++;
                } finally {
                    manager.release();
                }
                sleep(T2 * 1000);
            }

        } catch (InterruptedException e) {
        }
    }
}