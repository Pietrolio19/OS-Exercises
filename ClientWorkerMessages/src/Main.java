
import java.util.ArrayList;
import java.util.concurrent.Semaphore;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        int L = 20;
        int K = 5;
        int T1 = 100; //ms
        int T2 = 300; //ms

        Queue msgQueue = new Queue(L);
        Counter counter = new Counter();

        ClientThread[] ct = new ClientThread[5];
        for(int i=0; i<5; i++){
            ct[i] = new ClientThread(counter, msgQueue, K);
            ct[i].setName("CT"+i);
            ct[i].start();
        }

        WorkerThread[] wt = new WorkerThread[3];
        for(int i=0; i<3; i++){
            wt[i] = new WorkerThread(msgQueue, T1, T2);
            wt[i].setName("WT"+i);
            wt[i].start();
        }

        Thread.sleep(30000);
        for(int i=0; i<5; i++){
            ct[i].setEnd();
        }

        int totalSent = 0;
        for(int i=0; i<5; i++){
            ct[i].join();
            totalSent += ct[i].msgNum;
            System.out.println(ct[i].getName() + "msgSent:" + ct[i].msgNum);
        }
        System.out.println("Total sent:" + totalSent);

        int totalProc = 0;
        for(int i=0; i<3; i++){
            wt[i].interrupt();
            wt[i].join();
            totalProc += wt[i].numProc;
            System.out.println(wt[i].getName() + "msgProc:" + wt[i].numProc);
        }
        System.out.println("Total Proc:" + totalProc);
    }
}

class Msg{
    int id;
    int value;
    int result;
    ClientThread ct;

    public Msg(int id, int v, ClientThread c){
        this.id = id;
        this.value = v;
        this.ct = c;
    }
}

class Counter{
    int id = 0;
    Semaphore mutex = new Semaphore(1);

    public int getIdentifier(int k) throws InterruptedException {
        mutex.acquire();
        int v = id++;
        id += k;
        mutex.release();
        return v;
    }
}

class Queue{
    int L;
    ArrayList<Msg> messages = new ArrayList<>(L);
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote;

    public Queue(int l){
        this.L = l;
        this.vuote = new Semaphore(L);
    }

    public void putMessage(Msg m) throws InterruptedException{
        vuote.acquire();
        mutex.acquire();
        messages.add(m);
        mutex.release();
        piene.release();
    }

    public void putMessage(Msg[] m) throws InterruptedException{
        vuote.acquire(m.length);
        mutex.acquire();
        for(Msg ms : m)
            messages.add(ms);
        mutex.release();
        piene.release(m.length);
    }

    public Msg getMessage() throws InterruptedException{
        piene.acquire();
        mutex.acquire();
        Msg m = messages.remove(0);
        mutex.release();
        vuote.release();
        return m;
    }
}

class ClientThread extends Thread{
    int K;
    Msg[] msgs = null;
    Counter counter;
    Queue queue;
    Semaphore response = new Semaphore(0);
    boolean end = false;
    int msgNum = 0;

    public ClientThread(Counter c, Queue q, int k){
        this.counter = c;
        this.queue = q;
        this.K = k;
    }

    public void run(){
        try{
            while(!end){
                int k = 2 + (int)(Math.random()*(K-2+1));
                int id = counter.getIdentifier(k);
                msgs = new Msg[k];
                response = new Semaphore(0);
                for(int i=0; i<k; i++){
                    msgs[i] = new Msg(id+i, i+1, this);
                    queue.putMessage(msgs[i]);
                    msgNum++;
                }
                response.acquire(k);
                for(int i=0; i<k; i++){
                    System.out.println("msg" + i + ": " + msgs[i].id + ", " + msgs[i].value + ", " + msgs[i].result);
                }
            }
            System.out.println(getName() + " end");
        }
        catch(InterruptedException e){

        }
    }
    public void setEnd(){
        this.end = true;
    }

    public void setResult() {
        response.release();
    }
}

class WorkerThread extends Thread{
    Queue queue;
    int T1, T2;
    int numProc = 0;

    public WorkerThread(Queue q, int t1, int t2){
        this.queue = q;
        this.T1 = t1;
        this.T2 = t2;
    }

    public void run(){
        try{
            while(true){
                Msg m = queue.getMessage();
                sleep(T1+(int)(Math.random()*(T2-T1)));
                m.result = m.value * 2;
                numProc++;
                m.ct.setResult();
            }
        }
        catch(InterruptedException e){

        }
    }
}

