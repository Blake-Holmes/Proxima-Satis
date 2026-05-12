
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


public class DataBase{

    int k;
    int b;
    HashMap<String, HashMap<String, Integer>> tokenMap;
    HashMap<String, HashMap<String, Integer>> invertedIndex;

    HashMap<String, Float> idf;
    HashMap<String, Float> tf;
    int numDoc;
    int totalCharacter;





    public DataBase(){

        try{
            buildDataBase();
            buildInvertedIndex();
        }
        catch(IOException e){
                e.printStackTrace();
        }
    }

    public void buildDataBase() throws IOException{

        tokenMap = new HashMap<String, HashMap<String, Integer>>();

        File folder = new File("/home/jack/SearchProject/Proxima-Satis/Corpus");
        File[] docs = folder.listFiles();
        Tokenizer tok = new Tokenizer();

        if(docs != null){

            for(File file: docs){
                if (file.isFile()) {  // skip subdirectories

                    try{
                    tok.parse(file.getPath());
                    tokenMap.put(file.getName(), tok.getLex());
                    } catch(IOException e){
                        e.printStackTrace();
                    }

            }
        }


        }




    }

    public void buildInvertedIndex(){

        invertedIndex = new HashMap<String, HashMap<String, Integer>>();


        for(String docEntry: tokenMap.keySet()){
            String docId = docEntry;
            HashMap<String, Integer> termFreq = tokenMap.get(docEntry);


            for(String term: termFreq.keySet()){

                int tf = termFreq.get(term);

                invertedIndex
                    .computeIfAbsent(term, k -> new HashMap<>())
                    .put(docId, tf);
            }


        }


        for (Map.Entry<String, HashMap<String, Integer>> entry : invertedIndex.entrySet()) {

            String term = entry.getKey();
            HashMap<String, Integer> docs = entry.getValue();

            System.out.print(term + " -> ");

            for (Map.Entry<String, Integer> docEntry : docs.entrySet()) {
            System.out.print(docEntry.getKey() + ":" + docEntry.getValue() + " ");
            }

        System.out.println();
        }
    }







    public static void main(String[] args) {


        DataBase data = new DataBase();
    }

}
