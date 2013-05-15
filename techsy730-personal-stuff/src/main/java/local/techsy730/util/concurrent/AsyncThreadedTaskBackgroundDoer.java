package local.techsy730.util.concurrent;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.*;

public class AsyncThreadedTaskBackgroundDoer
{
    private final Runnable toRun;
    private final Executor exec;
    
    private static final int DEFAULT_PRIORITY = 5;
        
    public AsyncThreadedTaskBackgroundDoer(Runnable toRun, Executor runWith)
    {
        if(toRun == null)
            throw new NullPointerException("toRun cannot be null");
        if(runWith == null)
            throw new NullPointerException("runWith cannot be null");
        this.toRun = toRun;
        this.exec = runWith;
    }
    
    private static Executor genExec(int maxPendingRequests, int targetThreads, int maxThreads, String threadName, boolean appendNumberToName)
    {
        java.util.concurrent.ThreadPoolExecutor toReturn = 
            new java.util.concurrent.ThreadPoolExecutor(
                targetThreads, maxThreads, 6, TimeUnit.SECONDS, 
                    maxPendingRequests <= 0 ?
                        new java.util.concurrent.LinkedBlockingQueue<Runnable>() :
                        new java.util.concurrent.ArrayBlockingQueue<Runnable>(maxPendingRequests),
                    new NamedThreadFactory(threadName, appendNumberToName));
        toReturn.allowCoreThreadTimeOut(true);
        return toReturn;
    }
    
    public AsyncThreadedTaskBackgroundDoer(Runnable toRun, int maxPendingRequests, int targetThreads, int maxThreads, String threadName, boolean appendNumberToName)
    {
        this(toRun, genExec(maxPendingRequests, targetThreads, maxThreads, threadName, appendNumberToName));
    }
    
    public AsyncThreadedTaskBackgroundDoer(Runnable toRun, int maxPendingRequests, String threadName)
    {
        this(toRun, maxPendingRequests, 0, 1, threadName, false);
    }
    
    /**
     * Signal to run the backing runnable task. If there are too many pending runs already going, an exception is thrown.<p>
     * If you don't care if the run was rejected or not, consider the {@link #trySignalToRun()} method.
     * @throws java.util.concurrent.RejectedExecutionException if another run of the backing task cannot be queued due to max pending tasks being hit
     */
    public void signalToRun()
    {
        exec.execute(toRun);
    }
    
    /**
     * Signal to run the backing runnable task. If there are too many pending runs already going, this becomes a no-op.<p>
     * If you do care if the run was rejected or not, consider the {@link #signalToRun()} method.
     */
    public void trySignalToRun()
    {
        try
        {
            signalToRun();
        }
        catch(java.util.concurrent.RejectedExecutionException err)
        {
            //Ignore, as the user asked for it
        }
    }
    
    /**
     * Tell the backing executor to {@link java.util.concurrent.ExecutorService#shutdown() shutdown},
     * if it supports such an action. This may cause future signals to run to fail.<br>
     * If the backing executor is setup to automatically clean up on disuse or on shutdown, this method is not needed.
     * All the constructors that do not take an {@link Executor} explicitly do use such auto-cleaning Executors.
     * 
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    public void shutdown()
    {
        if(exec instanceof java.util.concurrent.ExecutorService)
            ((java.util.concurrent.ExecutorService)exec).shutdown();
    }
    
    /**
     * Tell the backing executor to {@link java.util.concurrent.ExecutorService#shutdownNow() shutdown now},
     * if it supports such an action. This may cause future signals to run to fail.<br>
     * If the backing executor is setup to automatically clean up on disuse or on shutdown, this method is not needed.
     * All the constructors that do not take an {@link Executor} explicitly do use such auto-cleaning Executors.
     * 
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     * @return the pending actions that were not executed due to this method,
     *              or an empty list if the backing executor did not support shutdown actions.
     */
    public java.util.List<Runnable> shutdownNow()
    {
        return exec instanceof java.util.concurrent.ExecutorService ?
            ((java.util.concurrent.ExecutorService)exec).shutdownNow() :
            java.util.Collections.<Runnable>emptyList();
    }
    
    private static class NamedThreadFactory implements java.util.concurrent.ThreadFactory
    {
        private final String name;
        private final boolean shouldNumber;
        private final java.util.concurrent.atomic.AtomicInteger count;
        
        NamedThreadFactory(String name, boolean shouldNumber)
        {
            this.name = name;
            this.shouldNumber = shouldNumber;
            this.count = shouldNumber ?
                new java.util.concurrent.atomic.AtomicInteger(0) :
                null;
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread toReturn = new Thread(r, shouldNumber ? name + String.valueOf(count.incrementAndGet()) : name);
            int newPriority = toReturn.getPriority();
            --newPriority;
            if(newPriority < Thread.MIN_PRIORITY)
                newPriority = Thread.MIN_PRIORITY;
            toReturn.setPriority(newPriority);
            toReturn.setDaemon(true);
            return toReturn;
        }
        
    }
    
    private static class PrioritizedRunnable implements Runnable, Comparable<PrioritizedRunnable>
    {
        final Runnable wrapped;
        /**
         * Higher values mean higher priority
         */
        final int priority;
        
        PrioritizedRunnable(Runnable toWrap, int priority)
        {
            this.wrapped = toWrap;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(PrioritizedRunnable other)
        {
            return priority - other.priority;
        }

        @Override
        public void run()
        {
            wrapped.run();
        }
        
    }
    
    private static class BoundedBlockingQueue<E>
        extends com.google.common.util.concurrent.ForwardingBlockingQueue<E>
        implements BlockingQueue<E>
    {
        private final int maxSize;
        private final boolean doesPutWait;
        //Although java.util.concurrent.ReentrantLock is nicer for just "plain ol' locking"
        //The com.google.common.util.concurrent.Monitor and Guard classes are better for "condition waiting" things.
        private final Monitor monitor;
        private final Monitor.Guard isNotFull;
        private final BlockingQueue<E> delegate;
        
        public BoundedBlockingQueue(final BlockingQueue<E> toWrap, final int maxSize, boolean doesPutWait)
        {
            this.delegate = toWrap;
            this.maxSize = maxSize;
            this.doesPutWait = doesPutWait;
            //Don't need fairness, this whole shebang is supposed to be a "whenever you get around to it" type thing anyways
            monitor = new Monitor();
            isNotFull = 
                new Monitor.Guard(monitor)
                {
                    @Override
                    public boolean isSatisfied()
                    {
                        return isFull();
                    }
                    
                };
        }
        
        private boolean isFull()
        {
            return delegate.size() <= maxSize;
        }

        @Override
        protected BlockingQueue<E> delegate()
        {
            return delegate;
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
            long start = System.nanoTime();
            boolean gotIn = monitor.enterWhen(isNotFull, timeout, unit);
            long end = System.nanoTime();
            if(gotIn)
            {
                //Have to subtract the difference to preserve total wait time
                long waited = end - start;
                long newToWait = unit.convert(waited, TimeUnit.NANOSECONDS) - waited;
                if(newToWait > 0)
                    return delegate.offer(e, newToWait, TimeUnit.NANOSECONDS);
                return delegate.offer(e);
            }
            return false;
        }

        @Override
        public void put(E e) throws InterruptedException
        {
            if(doesPutWait)
            {
                monitor.enterWhen(isNotFull);
                delegate.put(e);
            }
            else
            {
                boolean gotIn = monitor.enterIf(isNotFull);
                if(gotIn)
                    
            }
        }

        @Override
        public int remainingCapacity()
        {
            // TODO Auto-generated method stub
            return super.remainingCapacity();
        }

        @Override
        public boolean offer(E o)
        {
            // TODO Auto-generated method stub
            return super.offer(o);
        }

        @Override
        public int drainTo(Collection<? super E> c, int maxElements)
        {
            // TODO Auto-generated method stub
            return super.drainTo(c, maxElements);
        }

        @Override
        public int drainTo(Collection<? super E> c)
        {
            // TODO Auto-generated method stub
            return super.drainTo(c);
        }

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException
        {
            // TODO Auto-generated method stub
            return super.poll(timeout, unit);
        }

        @Override
        public E poll()
        {
            // TODO Auto-generated method stub
            return super.poll();
        }

        @Override
        public E remove()
        {
            // TODO Auto-generated method stub
            return super.remove();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            // TODO Auto-generated method stub
            return super.removeAll(collection);
        }

        @Override
        public boolean add(E element)
        {
            // TODO Auto-generated method stub
            return super.add(element);
        }

        @Override
        public boolean remove(Object object)
        {
            // TODO Auto-generated method stub
            return super.remove(object);
        }

        @Override
        public boolean retainAll(Collection<?> collection)
        {
            // TODO Auto-generated method stub
            return super.retainAll(collection);
        }

        @Override
        public void clear()
        {
            // TODO Auto-generated method stub
            super.clear();
        }
        
        
}
