import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 3;
        int K = 6;
        UnlimitedQueue q = new UnlimitedQueue();
        ResourceManager rm = new ResourceManager(2,2);

        GeneratorThread[] generatorThreads = new GeneratorThread[N];
        for(int i = 0; i < N; i++){
            generatorThreads[i] = new GeneratorThread(q, K);
            generatorThreads[i].setName("Generator" + i);
            generatorThreads[i].start();
        }

        ConsumerThread[] consumerThreads = new ConsumerThread[K];
        for(int i = 0; i < K; i++){
            consumerThreads[i] = new ConsumerThread(q, rm, i);
            consumerThreads[i].setName("Consumer" + i);
            consumerThreads[i].start();
        }

        for(int i = 0; i < 20; i++){
            Thread.sleep(1000);
            for(ConsumerThread c : consumerThreads)
                System.out.println("Messages Done: " + c.nDone);
            System.out.println("Resources Available A: " + rm.nA + ", B: " + rm.nB);
        }

        for(GeneratorThread gen : generatorThreads){
            gen.interrupt();
        }

        for(ConsumerThread gen : consumerThreads){
            gen.interrupt();
            gen.join();
        }
        System.out.println("Resources Available A: " + rm.nA + ", B: " + rm.nB);
    }
}

class Message {
    int value;
    int type;

    public Message(int type) {
        this.value = type;
        this.type = type;
    }
}

class UnlimitedQueue {
    ArrayList<Message> queue;

    public UnlimitedQueue() {
        queue = new ArrayList<>();
    }

    public synchronized void putMessage(Message m) {
        queue.add(m);
        notifyAll();
    }

    public synchronized Message takeMessage(int type) throws InterruptedException {
        while (queue.size() == 0 || queue.get(0).type != type) {
            wait();
        }
        Message msgr;
        msgr = queue.removeFirst();
        return msgr;
    }
    
}

class ResourceManager {
    int nA, nB;

    public ResourceManager(int nA, int nB) {
        this.nA = nA;
        this.nB = nB;
    }

    public synchronized boolean getResource() throws InterruptedException {
        while(nA == 0 && nB == 0) {
            wait();
        }
        if(nA != 0) {
            nA--;
            return true;
        }
        nB--;
        return false;
    }

    public synchronized void putResource(boolean ResourceA) throws InterruptedException {
        if(ResourceA)
            nA++;
        else
            nB++;
        notify();
    }
}

class GeneratorThread extends Thread {
    UnlimitedQueue queue;
    int K;

    public GeneratorThread(UnlimitedQueue queue, int K) {
        this.queue = queue;
        this.K = K;
    }

    public void run() {
        while(true){
            Message msg = new Message((int)(Math.random()*K));
            queue.putMessage(msg);
        }

    }
}

class ConsumerThread extends Thread {
    UnlimitedQueue queue;
    ResourceManager resources;
    int nDone = 0;
    int type;

    public ConsumerThread(UnlimitedQueue queue, ResourceManager resources, int type) {
        this.queue = queue;
        this.resources = resources;
        this.type = type;
    }

    public void run() {
        try{
            while(true){
                Message msg = queue.takeMessage(type);
                boolean resourceA = resources.getResource();
                try{
                    sleep(100);
                }
                finally {
                    resources.putResource(resourceA);
                }
                nDone++;
            }
        }
        catch(InterruptedException e){
            System.out.println("ConsumerThread interrupted!");
        }
    }

}