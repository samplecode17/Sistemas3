package eps.scp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class saveInvertedIndexTask implements Runnable{


    private final InvertedIndex inverted;

    private final String outputDirectory;


    private int numberOfFiles, remainingFiles;
    private long remainingKeys=0;

    private String key="";

    public saveInvertedIndexTask(InvertedIndex inverted, String outputDirectory){
        this.inverted = inverted;
        this.outputDirectory = outputDirectory;
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

        Iterator keyIterator = keySet.iterator();
        remainingKeys =  keySet.size();
        remainingFiles = numberOfFiles;


        List threads = new ArrayList<>();
        Lock lock = new ReentrantLock();
        CyclicBarrier barrier = new CyclicBarrier(numberOfFiles);


        // Bucle para recorrer los ficheros de indice a crear.
        for (int f=1;f<=numberOfFiles;f++)
        {
            int finalF = f;
            Thread thread = Thread.startVirtualThread(()->{
                try {
                    long keysByFile = 0;
                    File KeyFile = new File(outputDirectory +"/"+ inverted.getDIndexFilePrefix() + String.format("%03d", finalF));
                    FileWriter fw = new FileWriter(KeyFile);
                    BufferedWriter bw = new BufferedWriter(fw);
                    // Calculamos el número de claves a guardar en este fichero.

                    keysByFile =  remainingKeys / remainingFiles;
                    lock.lock();
                    remainingKeys -= keysByFile;
                    lock.unlock();
                    // Recorremos las claves correspondientes a este fichero.
                    lock.lock();
                    while (keyIterator.hasNext() && keysByFile>0) {
                        key = (String) keyIterator.next();
                        inverted.saveIndexKey(key,bw);  // Salvamos la clave al fichero.
                        keysByFile--;
                    }
                    lock.unlock();
                    bw.close(); // Cerramos el fichero.
                    lock.lock();
                    remainingFiles--;
                    lock.unlock();
                } catch (IOException e) {
                    System.err.println("Error creating Index file " + outputDirectory + "/IndexFile" + finalF);
                    e.printStackTrace();
                    System.exit(-1);
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }

            });

        }
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}
