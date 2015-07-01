package cl.tesis;

import cl.tesis.input.CSVFileWriter;
import cl.tesis.ssl.ConnectionThreads;
import cl.tesis.input.CSVFileReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws InterruptedException, IOException {
        LogManager.getLogManager().readConfiguration(new FileInputStream("src/cl/tesis/logger.properties"));
        logger.info("Start Scanning");

        String readFile = "src/cl/tesis/input/test.csv",
               writeFile = "src/cl/tesis/out.csv";

        try (CSVFileReader reader = new CSVFileReader(readFile);
             CSVFileWriter writer =  new CSVFileWriter(writeFile)) {

            List<Thread> lista = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Thread t = new ConnectionThreads(reader, writer);
                t.start();
                lista.add(t);
            }

            for (Thread thread : lista) {
                thread.join();
            }

        } catch (IOException e) {
            logger.info("Problems to close read or write files");
        }

        logger.info("End Scanning");

    }
}