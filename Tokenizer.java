
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Tokenizer {

    Path filePath;
    Map<String, Integer> lexicon;

    public Tokenizer(String path) throws IOException {
        this.filePath = Paths.get(path); //   or:    .\\Wet\\CC-MAIN-20260206181458-20260206211458-00001.warc.wet
        this.lexicon = new HashMap<>();
        parse();
    }

    private void parse() throws IOException {
        FileInputStream fis = new FileInputStream(filePath.toFile());
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        String line;
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            String cleanLine = "";
            for (String token : tokens) {
                token = clean(token);
                if (isValidToken(token)) {
                    lexicon.put(token, lexicon.getOrDefault(token, 0) + 1);
                    cleanLine += token + " ";
                }
            }
            if (!cleanLine.isBlank()) {
                writeLine(cleanLine.trim());
            }
        }

        fis.close();
        isr.close();
        br.close();

        pruneLexicon();
    }

    private String clean(String token) {
        return token.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    private boolean isValidToken(String token) {
        if (token.isEmpty()) {
            return false;
        }
        if (token.length() < 2) {
            return false;
        }
        if (token.length() > 20) {
            return false;
        }
        return true;
    }

    private void writeLine(String line) {
        final int LINES_PER_FILE = 5000;
        final int FILES_PER_DIR = 10;
        final String OUTPUT_ROOT = ".\\output";

        try {
            if (Holder.writer == null || Holder.linesWritten >= LINES_PER_FILE) {
                if (Holder.writer != null) {
                    Holder.writer.close();
                }
                Holder.linesWritten = 0;
                if (Holder.filesWritten >= FILES_PER_DIR || Holder.writer == null) {
                    Holder.filesWritten = 0;
                    File dir = new File(OUTPUT_ROOT, "dir" + Holder.dirIndex++);
                    dir.mkdirs();
                }
                File file = new File(OUTPUT_ROOT + File.separator + "dir" + (Holder.dirIndex - 1),
                        "file" + Holder.filesWritten++ + ".txt");
                Holder.writer = new BufferedWriter(new FileWriter(file));
            }
            Holder.writer.write(line);
            Holder.writer.newLine();
            Holder.linesWritten++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pruneLexicon() throws IOException {
        int k = 50000;
        PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
        for (Map.Entry<String, Integer> entry : lexicon.entrySet()) {
            if (entry.getValue() < 5) {
                continue;
            }
            if (heap.size() < k) {
                heap.add(entry);
            } else if (entry.getValue() > heap.peek().getValue()) {
                heap.poll();
                heap.add(entry);
            }
        }
        Map<String, Integer> newLex = new HashMap<>();
        while (!heap.isEmpty()) {
            Map.Entry<String, Integer> entry = heap.poll();
            newLex.put(entry.getKey(), entry.getValue());
        }
        lexicon = newLex;
        revealLexicon();
    }

    private void revealLexicon() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("tokens.txt"));
        for (Map.Entry<String, Integer> entry : lexicon.entrySet()) {
            bw.write(entry.getKey() + ": " + entry.getValue());
            bw.newLine();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Provide a folder of files.");
            System.out.println("Usage: java Tokenizer <file_path>");
            return;
        }

        Tokenizer t = new Tokenizer(args[0]);
    }
}
