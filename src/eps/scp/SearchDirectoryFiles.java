package eps.scp;

import java.io.File;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class SearchDirectoryFiles implements Runnable{
    private String dirpath;
    private InvertedIndex inverted;

    private final List<File> FilesList;

    private CountDownLatch latch;

    public SearchDirectoryFiles(String dirpath, InvertedIndex inverted, List<File> FilesList, CountDownLatch latch){
        this.dirpath = dirpath;
        this.inverted = inverted;
        this.FilesList = FilesList;
        this.latch = latch;

    }

    @Override
    public void run() {
        File file=new File(dirpath);
        File content[] = file.listFiles();
        if (content != null) {
            boolean[] comprovador = new boolean[content.length];
            Thread[] threads =  new Thread[content.length];
            CountDownLatch thread_latch = new CountDownLatch(content.length);
            for (int i = 0; i < content.length; i++) {

                if (content[i].isDirectory()) {
                    // Si es un directorio, procesarlo recursivamente.
                    //Indicamos que el comprovador de si hay hilos a cierto
                    comprovador[i] = true;
                    //creamos la tarea
                    SearchDirectoryFiles task1 = new SearchDirectoryFiles(content[i].getAbsolutePath(),inverted,FilesList,thread_latch);
                    threads[i] = Thread.startVirtualThread(task1);
                }
                else {
                    comprovador[i]= false;
                    // Si es un fichero de texto, añadirlo a la lista para su posterior procesamiento.
                    thread_latch.countDown();
                    if (inverted.checkFile(content[i].getName())){
                        synchronized (FilesList) {
                            FilesList.add(content[i]);
                        }
                    }
                }
            }
            try {
                thread_latch.await();
            } catch (InterruptedException e) {
                inverted.cancelAllThreads(threads);
                throw new RuntimeException(e);
            }

        }
        else{
            System.err.printf("Directorio %s no existe.\n",file.getAbsolutePath());
        }
        latch.countDown();
    }
}