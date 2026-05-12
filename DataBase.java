
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
    static HashMap<String, HashMap<String, Integer>> tokenMap;
    static HashMap<String, HashMap<String, Integer>> invertedIndex;

    HashMap<String, Float> idf;
    HashMap<String, Float> tf;
    static int numDoc;
    static int totalCharacter;





    public DataBase(){

        try{
            buildDataBase();
            buildInvertedIndex();
            getTotalTokens();
        }
        catch(IOException e){
                e.printStackTrace();
        }
    }

    public static void getTotalTokens(){
        totalCharacter = 0;

        for(String doc: tokenMap.keySet()){

            HashMap<String, Integer> tokM = tokenMap.get(doc);

            for(String word: tokM.keySet()){
                totalCharacter += tokM.get(word);
            }
        }
    }


    public void getQuery(String search) throws IOException{
        Tokenizer q = new Tokenizer();
        HashMap<String, Integer> queryMap = new HashMap<String, Integer>();

        if(search != null){
            queryMap = q.parseQuery(search);

            for(String word: queryMap.keySet()){
                System.out.println(word);
            }
        }
        else{
            System.out.println("No query");
            return;
        }

        query = new String[queryMap.size()];
        int i = 0;

        for(String word: queryMap.keySet()){
            query[i] = word;
            i++;
        }



    }

    public void buildDataBase() throws IOException{

        tokenMap = new HashMap<String, HashMap<String, Integer>>();

        File folder = new File("/home/jack/SearchProject/Proxima-Satis/PADocs");
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


    }


    public static double getIDF(String term){

        // total number of docs
        double N = numDoc;

        //number of documents containing term
        double dTerm = 0;
        if(invertedIndex.get(term) != null){

            dTerm = invertedIndex.get(term).size();

        }




        double idf = Math.log((N-dTerm +.5) / (dTerm + .5) + 1);



        return idf;


    }


    public double getTF(String term, String document){

        if (invertedIndex.get(term) == null || invertedIndex.get(term).get(document) == null) {
            return 0.0;
        }
        double termFreq = invertedIndex.get(term).get(document);
        double k1 = 1.0;
        double b = 1.0;
        double avgDocLength = totalCharacter/numDoc;


        //get doc doc length
        double docLength = 0;
        HashMap<String, Integer> docMap = tokenMap.get(document);

        for(String word: docMap.keySet()){

                docLength += docMap.get(word);
        }


        double tf = (termFreq)/(termFreq + (k1 * (1 - b + b * (docLength/avgDocLength))));

        return tf;

    }


    public HashMap<String, Double> conductSearch(){


    //get getIDF
    HashMap<String, Double> idfs = new HashMap<String, Double>();


    for(String word: query){
        //System.out.println("Word: " + word + " Idf: " + getIDF(word));
        idfs.put(word,getIDF(word));
    }


    //getTfs
    HashMap<String, HashMap<String, Double>> docQTF = new HashMap<String, HashMap<String, Double>>();

    for(String document: tokenMap.keySet()){

        HashMap<String, Double> tf = new HashMap<String, Double>();

        for(String queryWord: query){

            tf.put(queryWord, getTF(queryWord, document));

        }

        docQTF.put(document, tf);

    }

    //calculate scores

    HashMap<String, Double> Scores = new HashMap<String, Double>();

    for(String document: tokenMap.keySet()){

        double score = 0.0;
        HashMap<String, Double> tfMap = docQTF.get(document);

        for(String word: tfMap.keySet()){

            double tf = tfMap.get(word);
            double idf = idfs.get(word);


            double bm25 = tf * idf;
          //  System.out.println("TF " + tf + " idf: " + idf  );
            score += bm25;

        }

        Scores.put(document, score);



    }


    for (String document : Scores.keySet()) {
    System.out.println("Document: " + document + ", Score: " + Scores.get(document));
    }

    return Scores;

    }





    public static void main(String[] args) throws IOException {



        DataBase data = new DataBase();
        data.getQuery(arg[0]);
        data.conductSearch();
    }

}
