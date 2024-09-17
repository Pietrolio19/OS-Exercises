

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class Esame26062023{
    public static void main(String[] args) throws InterruptedException{
        int N = 5;
        int M = 3;

        MsgQueue msgQueue = new MsgQueue(15);
        OutputManager outputManager = new OutputManager(M);

        Generator[] gt = new Generator[N];
        for(int i=0; i < N; i++){
            gt[i] = new Generator(i, 100, msgQueue);
            gt[i].setName("GT"+i);
            gt[i].start();
        }

        Worker[] wt = new Worker[M];
        for(int i=0; i < M; i++){
            wt[i] = new Worker(msgQueue, outputManager, i, 200, 1000, N);
            wt[i].setName("WT"+i);
            wt[i].start();
        }

        OutputThread[] ot = new OutputThread[2];
        for(int i=0; i<2; i++){
            ot[i] = new OutputThread(outputManager);
            ot[i].setName("OT"+i);
            ot[i].start();
        }

        Thread.sleep(10000);
        for(int i=0; i<N; i++){
            gt[i].interrupt();
        }
        for(int i=0; i<M; i++){
            wt[i].interrupt();
        }
        for(int i=0; i<2; i++){
            ot[i].interrupt();
        }

        int nGen = 0;
        for(int i=0; i<N; i++){
            gt[i].join();
            System.out.println(gt[i].getName()+ " " + gt[i].nGen);
            nGen += gt[i].nGen;
        }

        int nWork = 0;
        for(int i=0; i<M; i++){
            wt[i].join();
            System.out.println(wt[i].getName()+ " " + wt[i].nWork);
            nGen += wt[i].nWork;
        }

        int nOut = 0;
        for(int i=0; i<2; i++){
            ot[i].join();
            System.out.println(ot[i].getName()+ " " + ot[i].nOut);
            nGen += ot[i].nOut;
        }

        System.out.println("nGen:" + nGen + ", " + "nWork:" + nWork + ", " + "nOut:" + nOut);


    }
}

class Message {
    int id;
    int value;

    public Message(int id, int v){
        this.id = id;
        this.value = v;
    }
}

class MsgQueue{
    ArrayList<Message> queue;
    int maxLength;

    Semaphore mutex;
    Semaphore piene;
    Semaphore vuote;

    MsgQueue(int L){
        this.queue = new ArrayList<>(maxLength);
        this.maxLength = L;
        this.mutex = new Semaphore(1);
        this.piene = new Semaphore(0);
        this.vuote = new Semaphore(L);
    }

    public void putMessage(Message msg) throws InterruptedException{
        vuote.acquire();
        mutex.acquire();
        queue.add(msg);
        mutex.release();
        piene.release();
    }

    public Message[] getMessages(int N) throws InterruptedException{
        piene.acquire(N);
        mutex.acquire();
        Message[] r = new Message[N];
        for(int i=0; i < N; i++){
            r[i] = queue.removeFirst();
        }
        mutex.release();
        vuote.release(N);
        return r;
    }
}

class OutputManager{
    Object[] results = null;
    Semaphore available = new Semaphore(0);
    Semaphore[] empty = null;

    public OutputManager(int M){
        this.results = new Object[M];
        this.empty = new Semaphore[M];
        for(int i=0; i < empty.length; i++){
            this.empty[i] = new Semaphore(1);
        }
    }

    public void putResult(int i, Object result) throws InterruptedException{
        empty[i].acquire();
        results[i] = result;
        available.release();
    }

    public Object[] getResults() throws InterruptedException{
        available.acquire(results.length);
        Object[] r = results;
        results = new Object[results.length];
        for(Semaphore p: empty)
            p.release();
        return r;
    }
}

class Generator extends Thread{
    int id;
    int X;
    MsgQueue queue;
    int nGen = 0;

    public Generator(int id, int X, MsgQueue queue){
        this.id = id;
        this.X = X;
        this.queue = queue;
    }

    public void run(){
        try{
            while(true){
                int v = 1;
                queue.putMessage(new Message(id, v++));
                nGen++;
                sleep(X);
            }
        }
        catch(InterruptedException e){
            System.out.println(getName() + "Interrotto");
        }
    }
}

class Worker extends Thread{
    MsgQueue queue;
    OutputManager outputManager;
    int id,T,D,N, nWork;

    public Worker(MsgQueue q, OutputManager om, int id, int T, int D, int N){
        this.queue = q;
        this.outputManager = om;
        this.id = id;
        this.T = T;
        this.D = D;
        this.N = N;
    }

    public void run(){
        try{
            while(true){
                Message[] r = queue.getMessages(N);
                int sum = 0;
                for(int i=0; i<r.length; i++){
                    sum += r[i].value;
                }
                nWork++;
                sleep(T+(int)(Math.random() * D));
                outputManager.putResult(id, r);
            }
        }
        catch(InterruptedException e){
            System.out.println(getName()+ "Interrotto");
        }
    }
}

class OutputThread extends Thread{
    OutputManager outputManager;
    int nOut;


    public OutputThread(OutputManager om){
        this.outputManager = om;
    }

    public void run(){
        try{
            while(true){
                Object[] r = outputManager.getResults();
                System.out.println(getName() + " " + Arrays.toString(r));
                nOut++;
            }
        } catch(InterruptedException e){
            System.out.println(getName() + "Interrotto");
        }
    }
}


