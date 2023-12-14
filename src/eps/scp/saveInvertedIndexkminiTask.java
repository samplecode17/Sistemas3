package eps.scp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class saveInvertedIndexkminiTask implements Runnable{
    private Iterator<String> keyIterator;
    private int remainingFiles, finalF;
    private long remainingKeys;
    private ReentrantLock lock;

    private InvertedIndex inverted;
    private CyclicBarrier barrier;
    private String outputDirectory;
    private String key = "";
    public saveInvertedIndexkminiTask(Iterator<String> keyiterator,int remainingFiles,int finalF,InvertedIndex inverted,String outputdirectory, CyclicBarrier barrier, ReentrantLock lock, long remainingKeys, String key){
            this.keyIterator=keyiterator;
            this.remainingFiles=remainingFiles;
            this.finalF = finalF;
            this.inverted = inverted;
            this.remainingKeys=remainingKeys;
            this.outputDirectory= outputdirectory;
            this.barrier = barrier;
            this.lock=lock;
            this.key=key;
    }

    @Override
    public void run() {
        try {

            long keysByFile = 0;
            File keyFile = new File(outputDirectory + "/" + inverted.getDIndexFilePrefix() + String.format("%03d", finalF));
            FileWriter fw = new FileWriter(keyFile);
            BufferedWriter bw = new BufferedWriter(fw);




            keysByFile = remainingKeys / remainingFiles;

            lock.lock();
            try{

                remainingKeys -= keysByFile;
                remainingFiles--;
            }finally {
                lock.unlock();
            }

            while (keyIterator.hasNext() && keysByFile > 0) {

                lock.lock();
                try {
                    key = keyIterator.next();
                    inverted.saveIndexKey(key, bw); // Salvamos la clave al fichero.
                }finally {
                    lock.unlock();
                }
                keysByFile--;



            }


            bw.close(); // Cerramos el fichero.


            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            System.err.println("Error creating Index file " + outputDirectory + "/IndexFile" + finalF);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
