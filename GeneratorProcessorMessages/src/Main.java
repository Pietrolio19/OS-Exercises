
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 4;
        int M = 2;
        Counter counter = new Counter();
        Queue msgQueue = new Queue(13);

        GeneratorThread[] gt = new GeneratorThread[N];
        for(int i=0; i<N; i++){
            gt[i] = new GeneratorThread(counter, msgQueue);
            gt[i].setName("GT" + i);
            gt[i].start();
        }

        ProcessorThread[] pt = new ProcessorThread[M];
        for(int i=0; i<M; i++){
            pt[i] = new ProcessorThread(msgQueue);
            pt[i].setName("PT" + i);
            pt[i].start();
        }

        Thread.sleep(30000);

        for(GeneratorThread t : gt)
            t.setEnd();

        int totalMsg = 0;
        for(GeneratorThread t : gt) {
            t.interrupt();
            t.join();
            totalMsg += t.numMsg;
            System.out.println(t.getName() + ": " + t.numMsg);
        }
        System.out.println("Totale Messaggi: " + totalMsg);

        msgQueue.waitEmpty();
        int totalCouples = 0;
        for(ProcessorThread p : pt){
            p.interrupt();
            p.join();
            totalCouples += p.couples;
            System.out.println(p.getName() + ": " + p.couples);
        }
        System.out.println("Totale Coppie: " + totalCouples);
    }
}

class Counter{
    private int identifier = 0;

    public synchronized int getIdentifier(){
        return identifier++;
    }
}

class Message{
    int id;
    int value;

    public Message(int id, int v){
        this.id = id;
        this.value = v;
    }

    @Override
    public String toString() {
        return "("+id+","+value+")";
    }
}

class Queue{
    int L;
    int nextIdExt = 0;
    private ArrayList<Message> msgQueue = new ArrayList<>(L);

    public Queue(int L){
        this.L = L;
    }

    public synchronized void putMessage(Message msg) throws InterruptedException{
        while(msgQueue.size() == L)
            wait();
        msgQueue.add(msg);
        notifyAll();
    }

    public synchronized Message[] get2() throws InterruptedException {
        Message[] r;
        while((r = getMsg()) == null ) {
            wait();
        }
        nextIdExt += 2;
        msgQueue.remove(r[0]);
        msgQueue.remove(r[1]);
        notifyAll();
        return r;
    }

    private Message[] getMsg() {
        Message[] r = new Message[2];
        int p=0;
        for(Message m : msgQueue) {
            if(m.id == nextIdExt || m.id == nextIdExt+1) {
                r[p++] = m;
            }
        }
        if(p==2)
            return r;
        return null;
    }

    public synchronized void waitEmpty() throws InterruptedException {
        while(msgQueue.size()>1)
            wait();
    }
}

class GeneratorThread extends Thread{
    Counter counter;
    Queue msgQueue;
    int numMsg = 0;
    boolean end = false;

    public GeneratorThread(Counter c, Queue q){
        this.counter = c;
        this.msgQueue = q;
    }

    public void run(){
        try{
            while(!end){
                Message msg = new Message(counter.getIdentifier(),(int)(Math.random()*10));
                numMsg++;
                sleep((int)(Math.random()*5000));
                msgQueue.putMessage(msg);
            }
        }
        catch(InterruptedException e){

        }
    }

    public void setEnd(){
        end = true;
    }
}

class ProcessorThread extends Thread{
    Queue msgQueue;
    int couples = 0;

    public ProcessorThread(Queue q){
        this.msgQueue = q;
    }

    public void run(){
        try{
            while(true){
                Message[] msg2 = msgQueue.get2();
                couples++;
                System.out.println("msg1: " + msg2[0] + " msg2: " + msg2[1]);
            }
        }
        catch(InterruptedException e){

        }
    }
}