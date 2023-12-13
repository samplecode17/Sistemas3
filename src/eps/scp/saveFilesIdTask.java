package eps.scp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class saveFilesIdTask implements Runnable{



    private final InvertedIndex inverted;

    private final String outputDirectory;

    private final CyclicBarrier barrier;



    public saveFilesIdTask(String outputDirectory, InvertedIndex inverted, CyclicBarrier barrier){
        this.outputDirectory = outputDirectory;
        this.inverted = inverted;
        this.barrier = barrier;
    }
    @Override
    public void run() {
        try {
            //File IdsFile = new File(outputDirectory +"/"+ DFilesIdsName);
            FileWriter fw = new FileWriter(outputDirectory + "/" + inverted.getDFilesIdsName());
            BufferedWriter bw = new BufferedWriter(fw);
            Set<Map.Entry<Integer,String>> keySet = inverted.getFiles().entrySet();
            Iterator keyIterator = keySet.iterator();

            while (keyIterator.hasNext() )
            {
                Map.Entry<Integer,String> entry = (Map.Entry<Integer,String>) keyIterator.next();
                bw.write(entry.getKey() + "\t" + entry.getValue() + "\n");
            }
            bw.close(); // Cerramos el fichero.

        } catch (IOException e) {
            System.err.println("Error creating FilesIds file: " + outputDirectory + inverted.getDFilesIdsName() + "\n");
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}
