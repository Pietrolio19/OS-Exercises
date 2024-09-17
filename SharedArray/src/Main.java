import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 3;
        int M = 5;
        int K = 13;

        SharedInputArray sharedInputArray = new SharedInputArray(K, M);
        SharedResults sharedResults = new SharedResults(M);

        GeneratorThread[] gt = new GeneratorThread[N];
        for (int i = 0; i < N; i++) {
            gt[i] = new GeneratorThread(sharedInputArray, i, K);
            gt[i].setName("GT" + i);
            gt[i].start();
        }

        WorkerThread[] wt = new WorkerThread[M];
        for (int i = 0; i < M; i++) {
            wt[i] = new WorkerThread(sharedInputArray, sharedResults, i);
            wt[i].setName("WT" + i);
            wt[i].start();
        }

        CollectorThread collector = new CollectorThread(sharedResults, 0);
        collector.setName("Collector");
        collector.start();

        Thread.sleep(10000);

        for(GeneratorThread generatorThread : gt){
            generatorThread.interrupt();
            generatorThread.join();
            System.out.println(generatorThread.getName() + ": " + generatorThread.nOp);
        }

        for(WorkerThread workerThread : wt){
            workerThread.interrupt();
            workerThread.join();
            System.out.println(workerThread.getName() + " " + workerThread.nOpW);
        }

        collector.interrupt();
        collector.join();
        System.out.println(collector.getName() + " " + collector.nOpC);
    }
}

class SharedInputArray {
    int K;
    int [] sharedArray;
    boolean[] taken;
    boolean arrayIn = false;
    int allTake = 0;

    public SharedInputArray(int k, int m) {
        K = k;
        sharedArray = new int[K];
        taken = new boolean[m];
    }

    public synchronized void putArray(int[] array) throws InterruptedException {
        while(arrayIn) {
            wait();
        }
        arrayIn = true;
        sharedArray = array;
        notify();
    }

    public synchronized int[] getArray(int id) throws InterruptedException {
        while(!arrayIn || taken[id]) {
            wait();
        }
        taken[id] = true;
        allTake++;
        int[] array = sharedArray;
        if(allTake == taken.length) {
            reset();
        }
        notifyAll();
        return array;
    }

    public synchronized void reset() {
        sharedArray = new int[K];
        taken = new boolean[taken.length];
        arrayIn = false;
        allTake = 0;
    }
}

class SharedResults {
    int M;
    int[] results;
    boolean[] alreadyIn;
    int resultsIn = 0;

    public SharedResults(int M){
        this.M = M;
        results = new int[M];
        alreadyIn = new boolean[M];
    }

    public synchronized void putResult(int id, int res) throws InterruptedException {
        while(alreadyIn[id]) {
            wait();
        }
        alreadyIn[id] = true;
        results[id] = res;
        resultsIn++;
        notifyAll();
    }

    public synchronized int[] getResults() throws InterruptedException {
        while(resultsIn != results.length) {
            wait();
        }
        int[] resr = results;
        results = new int[results.length];
        alreadyIn = new boolean[alreadyIn.length];
        resultsIn = 0;
        notifyAll();
        return resr;
    }
}

class GeneratorThread extends Thread {
    SharedInputArray sharedInputArray;
    int id;
    int K;
    int nOp = 0;

    public GeneratorThread(SharedInputArray sharedInputArray, int id, int K) {
        this.sharedInputArray = sharedInputArray;
        this.id = id;
        this.K = K;
    }

    public void run() {
        try{
            while(true){
                int[] values = new int[K];
                int v = id;
                for(int i = 0; i < K; i++){
                    values[i] = v++;
                }
                sharedInputArray.putArray(values);
                int TG = (int)(100+(Math.random()*900));
                sleep(TG);
                nOp++;
            }
        }
        catch(InterruptedException e){
            System.out.println("GeneratorThread interrupted!");
        }
    }
}

class WorkerThread extends Thread {
    SharedInputArray sharedInputArray;
    SharedResults sharedResults;
    int id;
    int nOpW = 0;

    public WorkerThread(SharedInputArray sharedInputArray, SharedResults sharedResults, int id) {
        this.sharedInputArray = sharedInputArray;
        this.sharedResults = sharedResults;
        this.id = id;
    }

    public void run() {
        try{
            while(true){
                int[] array = sharedInputArray.getArray(id);
                int sum = 0;
                for(int i = 0; i < array.length; i++){
                    sum += array[i];
                }
                sharedResults.putResult(id, sum*id);
                int TW = (int)(Math.random()*1900);
                sleep(TW);
                nOpW++;
            }
        }
        catch(InterruptedException e){
            System.out.println("WorkerThread interrupted!");
        }
    }
}

class CollectorThread extends Thread {
    SharedResults sharedResults;
    int id;
    int nOpC = 0;

    public CollectorThread(SharedResults sharedResults, int id) {
        this.sharedResults = sharedResults;
        this.id = id;
    }

    public void run() {
        try{
            while(true){
                int[] result = sharedResults.getResults();
                for(int i = 0; i < result.length; i++){
                    System.out.print(result[i] + " ");
                }
                nOpC++;
            }
        }
        catch(InterruptedException e){
            System.out.println("CollectorThread interrupted!");
        }
    }
}