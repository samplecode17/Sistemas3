package eps.scp;

import java.io.*;
import java.text.Normalizer;
import java.util.HashSet;

import java.util.concurrent.*;


public class addFileWords2Index implements Runnable{


    private File file;

    InvertedIndex inverted;
    int InternalTotalProcessedFiles = 0;

    long InternalTotalLines = 0;
    long InternalTotalKeysFound=0;

    long InternalTotalLocations = 0;

    long InternalTotalWords= 0;
    private final Phaser phaser;

    private Semaphore semaphore;
    int fileId;

    public addFileWords2Index(File file, int fileId, InvertedIndex inverted,Phaser phaser, Semaphore semaphore){

        this.file = file;
        this.fileId = fileId;
        this.inverted = inverted;
        this.phaser = phaser;
        this.phaser.register();
        this.semaphore = semaphore;

    }

    @Override
    public void run() {


        //Fase 1;
        Statistics FileStatistics = new Statistics("_");
        System.out.printf("Processing %3dth file %s (Path: %s)\n", fileId, file.getName(), file.getAbsolutePath());
        InternalTotalProcessedFiles++;
        FileStatistics.incProcessingFiles();


        // Crear buffer reader para leer el fichero a procesar.
        try(BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            int lineNumber = 0;  // inicializa contador de líneas a 0.
            while( (line = br.readLine()) !=null)   // Leemos siguiente línea de texto del fichero.
            {

                lineNumber++;
                InternalTotalLines++;
                FileStatistics.incProcessedLines();
                if (Indexing.Verbose) System.out.printf("Procesando linea %d fichero %d: ",lineNumber,fileId);
                Location newLocation = new Location(fileId, lineNumber);
                semaphore.acquire();
                try{
                    inverted.addIndexFilesLine(newLocation, line);
                } finally {
                    // Liberar el semáforo
                    semaphore.release();
                }


                // Eliminamos carácteres especiales de la línea del fichero.
                line = Normalizer.normalize(line, Normalizer.Form.NFD);
                line = line.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                String filter_line = line.replaceAll("[^a-zA-Z0-9áÁéÉíÍóÓúÚäÄëËïÏöÖüÜñÑ ]","");
                // Dividimos la línea en palabras.
                String[] words = filter_line.split("\\W+");
                //String[] words = line.split("(?U)\\p{Space}+");
                // Procesar cada palabra
                for(String word:words)
                {
                    if (Indexing.Verbose) System.out.printf("%s ",word);
                    word = word.toLowerCase();
                    // Obtener entrada correspondiente en el Indice Invertido
                    ///Lockear

                    semaphore.acquire();
                    try{
                        HashSet<Location> locations = inverted.getHash().get(word);
                        if (locations == null)
                        {   // Si no existe esa palabra en el indice invertido, creamos una lista vacía de Localizaciones y la añadimos al Indice
                            locations = new HashSet<Location>();
                            if (!inverted.getHash().containsKey(word)) {
                                FileStatistics.incKeysFound();
                                InternalTotalKeysFound++; // Modificado!!
                            }
                            inverted.getHash().put(word, locations);
                        }
                        ////UnLockear

                        InternalTotalWords++;   // Modificado!!
                        FileStatistics.incProcessedWords();   // Modificado!!
                        // Añadimos nueva localización en la lista de localizaciomes asocidada con ella.
                        int oldLocSize = locations.size();
                        locations.add(newLocation);
                        if (locations.size()>oldLocSize) {
                            InternalTotalLocations++;
                            FileStatistics.incProcessedLocations();
                        }
                    } finally {

                        semaphore.release();


                    }
                }
                if (Indexing.Verbose) System.out.println();
            }
        } catch (FileNotFoundException e) {
            System.err.printf("Fichero %s no encontrado.\n",file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.printf("Error lectura fichero %s.\n",file.getAbsolutePath());
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        phaser.arriveAndAwaitAdvance();

        try{
            semaphore.acquire();

            //Fase 2
            inverted.setTotalKeysFound(inverted.getTotalKeysFound()+InternalTotalKeysFound);
            inverted.setTotalLines(inverted.getTotalLines()+InternalTotalLines);
            inverted.setTotalLocations(inverted.getTotalLocations()+InternalTotalLocations);
            inverted.setTotalWords(inverted.getTotalWords()+InternalTotalWords);
            inverted.setTotalProcessedFiles(inverted.getTotalProcessedFiles()+InternalTotalProcessedFiles);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            semaphore.release();
        }
        phaser.arriveAndAwaitAdvance();
        FileStatistics.incProcessedFiles();
        FileStatistics.decProcessingFiles();
        try {
            semaphore.acquire();

            inverted.setMostPopularWord(FileStatistics);
            inverted.getGlobalStatistics().addStatistics(FileStatistics);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            semaphore.release();
        }
        FileStatistics.print(file.getName());

        phaser.arriveAndDeregister();
        ///Unlockear
    }
}
