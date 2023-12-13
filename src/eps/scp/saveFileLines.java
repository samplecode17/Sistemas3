package eps.scp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class saveFileLines implements Runnable{


    private InvertedIndex inverted;

    private String outputDirectory;

    private CyclicBarrier barrier;

    public saveFileLines(String outputDirectory, InvertedIndex inverted, CyclicBarrier barrier){
        this.outputDirectory = outputDirectory;
        this.inverted = inverted;
        this.barrier = barrier;
    }
    @Override
    public void run() {
        try {
            File KeyFile = new File(outputDirectory + "/" + inverted.getDFileLinesName());
            FileWriter fw = new FileWriter(KeyFile);
            BufferedWriter bw = new BufferedWriter(fw);
            Set<Map.Entry<Location, String>> keySet = inverted.getIndexFilesLines().entrySet();
            Iterator keyIterator = keySet.iterator();

            while (keyIterator.hasNext() )
            {
                Map.Entry<Location, String> entry = (Map.Entry<Location, String>) keyIterator.next();
                bw.write(entry.getKey() + "\t" + entry.getValue() + "\n");
            }
            bw.close(); // Cerramos el fichero.
        } catch (IOException e) {
            System.err.println("Error creating FilesLines contents file: " + outputDirectory + inverted.getDFileLinesName() + "\n");
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
