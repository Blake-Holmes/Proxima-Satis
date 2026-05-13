
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
    HashMap<String, Integer> lexicon;

    public Tokenizer() throws IOException {
        //this.filePath = Paths.get(path); //   or:    .\\Wet\\CC-MAIN-20260206181458-20260206211458-00001.warc.wet
        this.lexicon = new HashMap<>();
    }

    public void parse(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
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
           
        }

        fis.close();
        isr.close();
        br.close();

        pruneLexicon();
    }

    public HashMap<String, Integer> parseQuery(String query) {
        HashMap<String, Integer> queryMap = new HashMap<>();
           System.out.println(query);
        if (query != null && !query.trim().isEmpty()) {
            String[] tokens = query.split("\\s+");

        for (String token : tokens) {

            token = clean(token);
            if (isValidToken(token)) {

                queryMap.put(token, queryMap.getOrDefault(token, 0) + 1);
                }
            }
        }

        return queryMap;
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

    public HashMap<String, Integer> getLex(){

         HashMap<String, Integer> temp = this.lexicon;
         this.lexicon = new HashMap<String, Integer>();
         return temp;
    }

    private void pruneLexicon() throws IOException {
        int k = 50000;
        PriorityQueue<HashMap.Entry<String, Integer>> heap = new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
        for (HashMap.Entry<String, Integer> entry : lexicon.entrySet()) {
           /* if (entry.getValue() < 2) {
                System.out.println("Pruned: " + entry.getKey());
                continue;
            }

            */ //could be nessicary if doc content size too big
            // with smaller documents we are using helps with search
            if (heap.size() < k) {
                heap.add(entry);
            } else if (entry.getValue() > heap.peek().getValue()) {
                heap.poll();
                heap.add(entry);
            }
        }
        HashMap<String, Integer> newLex = new HashMap<>();
        while (!heap.isEmpty()) {
            HashMap.Entry<String, Integer> entry = heap.poll();
            newLex.put(entry.getKey(), entry.getValue());
        }
        lexicon = newLex;
        revealLexicon();
    }

    private void revealLexicon() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("tokens.txt"));
        for (Map.Entry<String, Integer> entry : lexicon.entrySet()) {
            //stem.out.println(entry.getKey());
            bw.write(entry.getKey() + ": " + entry.getValue());
            bw.newLine();
        }

        bw.flush();
        bw.close();
    }

    public static void main(String[] args) throws IOException {


    }
}
