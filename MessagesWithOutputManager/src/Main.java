import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 5;
        int M = 3;

        LimitedQueue queue = new LimitedQueue(15);
        OutputManager outputManager = new OutputManager(M);

        GeneratorThread[] gt = new GeneratorThread[N];
        for(int i = 0; i < N; i++){
            gt[i] = new GeneratorThread(queue, 100, i);
            gt[i].setName("GT" + i);
            gt[i].start();
        }

        WorkerThread[] wt = new WorkerThread[M];
        for(int i = 0; i < M; i++){
            wt[i] = new WorkerThread(queue, outputManager, 100, 50, N, i);
            wt[i].setName("WT" + i);
            wt[i].start();
        }

        OutputThread[] ot = new OutputThread[2];
        for(int i = 0; i < 2; i++){
            ot[i] = new OutputThread(outputManager);
            ot[i].setName("OT" + i);
            ot[i].start();
        }

        Thread.sleep(10000);

        int totalGen = 0;
        for(GeneratorThread generatorThread : gt){
            generatorThread.interrupt();
            generatorThread.join();
            System.out.println(generatorThread.getName() + " generated: " + generatorThread.nGen);
            totalGen += generatorThread.nGen;
        }
        System.out.println("Total Gen: " + totalGen);

        for(WorkerThread workerThread : wt){
            workerThread.interrupt();
            workerThread.join();
            System.out.println(workerThread.getName() + " processed: " + workerThread.nDone);
        }

        for(OutputThread outputThread : ot){
            outputThread.interrupt();
            outputThread.join();
            System.out.println(outputThread.getName() + " printed: " + outputThread.nPrint);
        }
    }
}

class Message {
    int value;
    int generatorId;

    public Message(int value, int generatorId) {
        this.value = value;
        this.generatorId = generatorId;
    }

}

class LimitedQueue{
    ArrayList<Message> queue;
    Semaphore mutex = new Semaphore(1);
    Semaphore full = new Semaphore(0);
    Semaphore empty;

    public LimitedQueue(int L){
        queue = new ArrayList<>();
        empty = new Semaphore(L);
    }

    public void putMessage(Message msg) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        queue.add(msg);
        mutex.release();
        full.release();
    }

    public Message[] getMessages(int n) throws InterruptedException {
        full.acquire(n);
        mutex.acquire();
        Message[] msgr = new Message[n];
        for (int i = 0; i < n; i++) {
            msgr[i] = queue.remove(0);
        }
        mutex.release();
        empty.release(n);
        return msgr;
    }
}

class OutputManager {
    int M;
    int[] results;
    Semaphore[] previousIn;
    Semaphore nAvailable = new Semaphore(0);

    public OutputManager(int M) {
        this.M = M;
        results = new int[M];
        previousIn = new Semaphore[M];
        for(int i = 0; i < M; i++){
            previousIn[i] = new Semaphore(1);
        }
    }

    public void putResult(int id, int res) throws InterruptedException {
        previousIn[id].acquire();
        results[id] = res;
        nAvailable.release();
    }

    public int[] getResults() throws InterruptedException {
        nAvailable.acquire(results.length);
        for(Semaphore s : previousIn){
            s.release();
        }
        int[] res = results;
        results = new int[M];
        return res;
    }
}

class GeneratorThread extends Thread{
    LimitedQueue queue;
    int nGen = 0;
    int X;
    int id;


    public GeneratorThread(LimitedQueue queue, int x, int id){
        this.queue = queue;
        this.X = x;
        this.id = id;
    }

    public void run(){
        try{
            int i = 1;
            while(true){
                Message msg = new Message(i++, id);
                queue.putMessage(msg);
                sleep(X);
                nGen++;
            }
        }
        catch(InterruptedException e){
            System.out.println("GeneratorThread interrupted!");
        }
    }
}

class WorkerThread extends Thread {
    LimitedQueue queue;
    OutputManager outputs;
    int T, D, N;
    int id;
    int nDone = 0;

    public WorkerThread(LimitedQueue queue, OutputManager outputs, int T, int D, int N, int id) {
        this.queue = queue;
        this.outputs = outputs;
        this.T = T;
        this.D = D;
        this.N = N;
        this.id = id;
    }

    public void run(){
        try {
            while(true){
                Message[] msgs = queue.getMessages(N);
                int sum = 0;
                for(Message msg : msgs){
                    sum += msg.value;
                }
                sleep(T + (int)(Math.random()*(T-D)));
                outputs.putResult(id, sum);
                nDone++;
            }
        }
        catch(InterruptedException e){
            System.out.println("WorkerThread interrupted!");
        }
    }
}

class OutputThread extends Thread {
    OutputManager outputs;
    int nPrint = 0;

    public OutputThread(OutputManager outputs) {
        this.outputs = outputs;
    }

    public void run(){
        try{
            while(true){
                int[] output = outputs.getResults();
                for(int i = 0; i < output.length; i++){
                    System.out.println(i + ": " + output[i]);
                }
                nPrint++;
            }
        }
        catch(InterruptedException e){
            System.out.println("OutputThread interrupted!");
        }
    }
}