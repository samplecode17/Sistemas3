package eps.scp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class saveInvertedIndexTask implements Runnable{


    private final InvertedIndex inverted;

    private final String outputDirectory;


    private int numberOfFiles=0, remainingFiles=0;
    private long remainingKeys=0;

    private CyclicBarrier  main_barrier;

    private String key="";
    private static ReentrantLock lock = new ReentrantLock();


    private Iterator<String> keyIterator;

    public saveInvertedIndexTask(InvertedIndex inverted, String outputDirectory, CyclicBarrier  main_barrier){
        this.inverted = inverted;
        this.outputDirectory = outputDirectory;
        this.main_barrier=main_barrier;
    }

    @Override
    public void run() {


        Charset utf8 = StandardCharsets.UTF_8;
        Set<String> keySet = inverted.getHash().keySet();

        numberOfFiles = keySet.size()/inverted.getDKeysByFileIndex();
        // Calculamos el número de ficheros a crear en función del número de claves que hay en el hash.
        if (numberOfFiles>inverted.getDIndexMaxNumberOfFiles())
            numberOfFiles = inverted.getDIndexMaxNumberOfFiles();
        if (numberOfFiles<inverted.getDIndexMinNumberOfFiles())
            numberOfFiles = inverted.getDIndexMinNumberOfFiles();

        keyIterator = keySet.iterator();
        remainingKeys =  keySet.size();
        remainingFiles = numberOfFiles;


        List<Thread> threads = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(numberOfFiles+1);

        // Bucle para recorrer los ficheros de indice a crear.
        for (int f=1;f<=numberOfFiles;f++) {
            int finalF = f;

            Thread thread = Thread.startVirtualThread(() -> {
                try {

                    long keysByFile = 0;
                    File keyFile = new File(outputDirectory + "/" + inverted.getDIndexFilePrefix() + String.format("%03d", finalF));
                    FileWriter fw = new FileWriter(keyFile);
                    BufferedWriter bw = new BufferedWriter(fw);
                    lock.lock();
                    try{
                        keysByFile = remainingKeys / remainingFiles;
                        remainingKeys -= keysByFile;
                        remainingFiles--;
                    }finally {
                        lock.unlock();
                    }

                    while (keyIterator.hasNext() && keysByFile > 0) {
                        lock.lock();
                        try{
                            key = keyIterator.next();
                            inverted.saveIndexKey(key, bw); // Salvamos la clave al fichero.
                            keysByFile--;

                        }finally {
                            lock.unlock();
                        }

                    }


                    bw.close(); // Cerramos el fichero.
                    lock.lock();
                    try{

                    }finally {
                        lock.unlock();
                    }



                } catch (IOException e) {
                    System.err.println("Error creating Index file " + outputDirectory + "/IndexFile" + finalF);
                    e.printStackTrace();
                    System.exit(-1);
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }

            });
            threads.add(thread);
        }




        try {

            barrier.await();
            main_barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            inverted.cancelAllThreads(threads);
            throw new RuntimeException(e);
        }


    }
}
