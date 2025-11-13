package com.chessgame.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ManualThreadPool {

    private final BlockingQueue<Runnable> taskQueue;
    private final WorkerThread[] workerThreads;

    public ManualThreadPool(int numThreads) {
        this.taskQueue = new LinkedBlockingQueue<>();
        this.workerThreads = new WorkerThread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workerThreads[i] = new WorkerThread();
            workerThreads[i].start();
        }
    }

    public void submit(Runnable task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class WorkerThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}