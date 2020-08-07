package net.sf.grotag.common;

import javax.swing.SwingUtilities;

/**
 * This is the 3rd version of SwingWorker (also known as SwingWorker 3), an
 * abstract class that you subclass to perform GUI-related work in a dedicated
 * thread. For instructions on and examples of using this class, see:
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html Note that
 * the API changed slightly in the 3rd version: You must now invoke start() on
 * the SwingWorker after creating it.
 */
public abstract class SwingWorker {
    private ThreadVar threadVar;
    private Object value;

    /**
     * Start a thread that will call the <code>construct</code> method and
     * then exit.
     */
    public SwingWorker() {
        final Runnable doFinished = new Runnable() {
            public void run() {
                finished();
            }
        };

        Runnable doConstruct = new Runnable() {
            public void run() {
                try {
                    setValue(construct());
                } finally {
                    threadVar.clear();
                }

                SwingUtilities.invokeLater(doFinished);
            }
        };

        Thread t = new Thread(doConstruct);

        threadVar = new ThreadVar(t);
    }

    /**
     * Set the value produced by worker thread.
     */
    private synchronized void setValue(Object x) {
        value = x;
    }

    /**
     * Return the value created by the <code>construct</code> method. Returns
     * null if either the constructing thread or the current thread was
     * interrupted before a value was produced.
     * 
     * @return the value created by the <code>construct</code> method
     */
    public Object get() {
        Object result = null;
        boolean gotResult = false;

        while (!gotResult) {
            Thread t = threadVar.get();

            if (t == null) {
                result = getValue();
                gotResult = true;
            } else {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    // Propagate
                    Thread.currentThread().interrupt();
                    result = null;
                    gotResult = true;
                }
            }
        }
        return result;
    }

    /**
     * Get the value produced by the worker thread, or null if it hasn't been
     * constructed yet.
     */
    protected synchronized Object getValue() {
        return value;
    }

    /**
     * Compute the value to be returned by the <code>get</code> method.
     */
    public abstract Object construct();

    /**
     * Called on the event dispatching thread (not on the worker thread) after
     * the <code>construct</code> method has returned.
     */
    public void finished() {
        // do nothing
    }

    /**
     * A new method that interrupts the worker thread. Call this method to force
     * the worker to stop what it's doing.
     */
    public void interrupt() {
        Thread t = threadVar.get();

        if (t != null) {
            t.interrupt();
        }
        threadVar.clear();
    }

    /**
     * Start the worker thread.
     */
    public void start() {
        Thread t = threadVar.get();

        if (t != null) {
            t.start();
        }
    }

    /**
     * Class to maintain reference to current worker thread under separate
     * synchronization control.
     */
    private static class ThreadVar {
        private Thread thread;

        ThreadVar(Thread t) {
            thread = t;
        }

        synchronized Thread get() {
            return thread;
        }

        synchronized void clear() {
            thread = null;
        }
    }
}
