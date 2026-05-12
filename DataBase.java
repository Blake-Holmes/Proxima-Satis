
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


public class DataBase{

    int k;
    int b;
    String[] query;
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


    public void getQuery(String search) throws IOException{
        Tokenizer q = new Tokenizer();
        HashMap<String, Integer> queryMap = new HashMap<String, Integer>();

        if(search != null){
            queryMap = q.parseQuery(search);
        }

        query = new String[queryMap.size()];
        int i = 0;

        for(String word: queryMap.keySet()){
            System.out.println(word);
            query[i] = word;
            i++;
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
                    numDoc ++;
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


    public double getIDF(String term){

        // total number of docs
        double N = this.numDoc;

        //number of documents containing term
        double dTerm = 0;
        if(invertedIndex.get(term) != null){

            dTerm = invertedIndex.get(term).size();

        }




        double idf = Math.log((N-dTerm +.5) / (dTerm + .5));



        return idf;


    }


    public void getTF(String term){



    }







    public static void main(String[] args) throws IOException {


        DataBase data = new DataBase();
        data.getQuery("HELLO World");
    }

}
