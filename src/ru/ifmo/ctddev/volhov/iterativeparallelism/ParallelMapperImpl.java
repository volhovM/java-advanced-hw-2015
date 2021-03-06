package ru.ifmo.ctddev.volhov.iterativeparallelism;

import com.sun.jmx.remote.internal.ArrayQueue;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an abstraction of pool capable to hold threads and run certain functions on them.
 * <p>
 * It contains a queue, that holds functions, given by {@link ru.ifmo.ctddev.volhov.iterativeparallelism.ParallelMapperImpl#map},
 * and "feeds" them to threads that are able to run.
 * @author volhovm
 *         Created on 3/25/15
 */
public class ParallelMapperImpl implements ParallelMapper {
    // (task, isTaken)
    private volatile boolean isTerminated = false;
    private final ArrayDeque<Consumer<Void>> queue;
    private final Thread[] threads;

    /**
     * Creates class with given number of threads to execute tasks on
     * @param threads   number of threads
     */
    public ParallelMapperImpl(int threads) {
        queue = new ArrayDeque<>();
        this.threads = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            this.threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isTerminated) {
                        Consumer<Void> data = null;
                        synchronized (queue) {
                            if (!queue.isEmpty()) {
                                data = queue.pop();
                            }
                        }
                        if (data != null) {
                            data.accept(null);
                            synchronized (queue) {
                                queue.notifyAll();
                            }
                            continue;
                        }
                        if (isTerminated) {
                            return;
                        }
                        try {
                            synchronized (queue) {
                                queue.wait();
                            }
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    }
                }
            });
            this.threads[i].start();
        }
    }

    /**
     * Maps the sequence, using the number of threads, containing in this object entry. It actually puts functions
     * that map every single list item, to the special queue, and eventually threads execute it and put the
     * result in the result list.
     *
     * @param f function to map
     * @param args  list to map
     * @param <T> type of initial array item
     * @param <R> type of result array item
     * @return  mapped list
     * @throws InterruptedException
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (isTerminated) throw new IllegalStateException("This entry of ParallelMapperImpl was already closed");
        final int argsize = args.size();
        AtomicInteger counter = new AtomicInteger(0);
        ArrayList<R> retList = new ArrayList<>(args.size());
        for (int i = 0; i < argsize; i++) retList.add(null);
        for (int i = 0; i < argsize; i++) {
            final int ind = i;
            synchronized (queue) { //sync on volatile, do I need that?
                queue.push((whatever) -> {
                    T elem;
                    elem = args.get(ind);
                    R res = f.apply(elem);
                    synchronized (retList) {
                        retList.set(ind, res);
                    }
                    counter.incrementAndGet();
                    synchronized (queue) {
                        queue.notifyAll();
                    }
                });
            }
        }
        synchronized (queue) {
            queue.notifyAll();
            while (counter.get() < argsize) {
                queue.wait();
            }
        }
        return retList;
    }

    /**
     * Closes this object, stopping all threads from execution. After this method is invoked, object can't be used.
     * @throws InterruptedException if
     */
    @Override
    public void close() throws InterruptedException {
        isTerminated = true;
        for (Thread i : threads) {
            i.interrupt();
            i.join();
        }
    }
}
