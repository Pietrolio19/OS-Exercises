
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 5;
        int M = 4;
        int K = 13;
        int TG = 100;
        int DG = 50;
        int S = 3;
        LimitedQueue queue = new LimitedQueue(K, N);
        UnlimitedQueue unlimitedQueue = new UnlimitedQueue();

        GeneratorThread[] gt = new GeneratorThread[N];
        for(int i = 0; i < N; i++){
            gt[i] = new GeneratorThread(queue,i, TG, DG);
            gt[i].setName("GT" +i);
            gt[i].start();
        }

        ProcessorThread[] pt = new ProcessorThread[M];
        for(int i = 0; i < M; i++){
            pt[i] = new ProcessorThread(unlimitedQueue, queue, S, N);
            pt[i].setName("PT" +i);
            pt[i].start();
        }

        PrinterThread[] printers = new PrinterThread[2];
        for(int i = 0; i < 2; i++){
            printers[i] = new PrinterThread(unlimitedQueue, N, i);
            printers[i].setName("Printer" +i);
            printers[i].start();
        }

        Thread.sleep(10000);
        int totalGen = 0;
        for(GeneratorThread g : gt){
            g.interrupt();
            g.join();
            totalGen += g.nGen;
            System.out.println(g.getName() + ": " + g.nGen);
        }
        System.out.println("Total Generated: " + totalGen);

        int totalDone = 0;
        for(ProcessorThread p : pt){
            p.setStop();
            p.interrupt();
            p.join();
            totalDone += p.nDone;
            System.out.println(p.getName() + ": " + p.nDone);
        }
        System.out.println("Total Done: " + totalDone);

        int totalPrint = 0;
        for(PrinterThread p : printers){
            p.interrupt();
            p.join();
            totalPrint += p.nOp;
            System.out.println(p.getName() + ": " + p.nOp);
        }
        System.out.println("Total Print: " + totalPrint);
        System.out.println("Remaining Messages: " + (queue.currentInside + unlimitedQueue.currentInside)); //riga non presente nella prova consegnata
    }
}

class Message {
    int value;
    int genId;
    int progId;

    public Message(int v, int gId, int pId) {
        value = v;
        genId = gId;
        progId = pId;
    }
}

class LimitedQueue {
    ArrayList<Message> messages;
    int K;
    int currentInside = 0;
    int[] nGenMsgs;

    public LimitedQueue(int K, int N) {
        messages = new ArrayList<>();
        this.K = K;
        nGenMsgs = new int[N];
    }

    public synchronized void put(Message msg) throws InterruptedException {
        while(currentInside == K)
            wait();
        currentInside++;
        messages.add(msg);
        nGenMsgs[msg.genId]++;
        notifyAll();
    }

    public synchronized Message[] get(int id, int S) throws InterruptedException {
        while(nGenMsgs[id] != S)
            wait();
        currentInside -= S;
        Message[] msgr = new Message[S];
        for(int i = 0; i < msgr.length; i++)
            msgr[i] = messages.remove(getMinProgId(id));
        nGenMsgs[id] = 0;
        notifyAll();
        return msgr;
    }

    public synchronized int getMinProgId(int id) {
        int pos = 0;
        for(int i = 0; i < messages.size(); i++) {
            if(messages.get(pos).genId == id){ //riga di codice non presente nella prova consegnata
                if(messages.get(i).progId < messages.get(pos).progId)
                    pos = i;
            }
            else { //riga non presente nella prova consegnata
                pos++;
            }
            if(pos >= messages.size()) //riga non presente nella prova consegnata
                return 0;
        }
        return pos;
    }

    public synchronized boolean checkById(int id, int S) throws InterruptedException {
        if(nGenMsgs[id] == S)
            return true;
        return false;
    }

}

class UnlimitedQueue {
    ArrayList<Message> results = new ArrayList<>();
    int currentInside = 0;

    public synchronized void putResult(Message msg) throws InterruptedException {
        results.add(msg);
        currentInside++;
        notify();
    }

    public synchronized Message[] getResults(int N) throws InterruptedException {
        while(results.size() < N)
            wait();
        currentInside -= N;
        Message[] resr = new Message[N];
        for(int i = 0; i < resr.length; i++)
            resr[i] = results.removeFirst();
        return resr;
    }
}

class GeneratorThread extends Thread {
    LimitedQueue messages;
    int id, TG, DG;
    int nGen = 0;
    int progId = 0;

    public GeneratorThread(LimitedQueue messages, int id, int TG, int DG) {
        this.messages = messages;
        this.id = id;
        this.TG = TG;
        this.DG = DG;
    }

    public void run() {
        try{
            int v = id+1; //riga di codice non presente nella prova consegnata
            while(true) {
                Message msg = new Message(v++, id, progId++);
                messages.put(msg);
                sleep(TG + (int)(Math.random()*(TG-DG)));
                nGen++;
            }
        } catch (InterruptedException e) {
            System.out.println("GeneratorThread interrupted!");
        }
    }
}

class ProcessorThread extends Thread {
    UnlimitedQueue results;
    LimitedQueue messages;
    int S;
    int N;
    boolean stop = false;
    int nDone = 0;

    public ProcessorThread(UnlimitedQueue results, LimitedQueue messages, int S, int N) {
        this.results = results;
        this.messages = messages;
        this.S = S;
        this.N = N;
    }

    public void run() {
        try{
            while(true) {
                int getFromId = 0;
                while(!messages.checkById(getFromId, S) && !stop) { //riga differente dalla prova consegnata
                    getFromId++;
                    if(getFromId == N) //riga non presente nella prova consegnata
                        getFromId = 0;
                }
                Message[] msgs = messages.get(getFromId, S); //nella prova gli argomenti sono al contrario
                int sum = 0;
                for(Message m : msgs) {
                    sum += m.value;
                }
                results.putResult(new Message(sum, msgs[0].genId, msgs[0].progId));
                nDone++;
            }
        }
        catch(InterruptedException e) {
            System.out.println("ProcessorThread interrupted!");
        }
    }

    public synchronized void setStop() { //funzione non presente nella prova consegnata
        stop = true;
    }
}

class PrinterThread extends Thread {
    UnlimitedQueue results;
    int N, id;
    int nOp = 0;

    public PrinterThread(UnlimitedQueue results, int N, int id) {
        this.results = results;
        this.N = N;
        this.id = id;
    }

    public void run() {
        try{
            while(true) {
                Message[] msgs = results.getResults(N);
                for(int i = 0; i < N; i++){
                    System.out.println(id + ": " + msgs[i].value + " " + msgs[i].genId + " " + msgs[i].progId);
                    nOp++; //riga non presente nella prova consegnata
                }
            }
        }
        catch(InterruptedException e) {
            System.out.println("PrinterThread interrupted!");
        }
    }
}
