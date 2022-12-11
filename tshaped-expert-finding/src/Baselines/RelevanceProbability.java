package Baselines;

import Utils.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class RelevanceProbability {

    public static void main(String[] args) throws IOException, ParseException {
        String dataSetName, answersIndexPath, skillAreasPath, result_path, probabilities_path;

        RelevanceProbability rp = new RelevanceProbability();

        dataSetName = "java";
        answersIndexPath = "./files/Index/java/Answers";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        probabilities_path = "./files/RelevanceNew/" + dataSetName + "/";
        result_path = "./files/Relevance/DBA/" + dataSetName + "/";
        rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, probabilities_path);

        dataSetName = "android";
        answersIndexPath = "./files/Index/android/Answers";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        probabilities_path = "./files/RelevanceNew/" + dataSetName + "/";
        result_path = "./files/Relevance/DBA/" + dataSetName + "/";
        rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, probabilities_path);

        dataSetName = "c#";
        answersIndexPath = "./files/Index/c#/Answers";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        probabilities_path = "./files/RelevanceNew/" + dataSetName + "/";
        result_path = "./files/Relevance/DBA/" + dataSetName + "/";
        rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, probabilities_path);
    }

    private TreeMap<String, ArrayList<String>> getSkillAreaTagsFile(String skillAreasPath) throws IOException {
        TreeMap<String, ArrayList<String>> SkillArea_TagsList = new TreeMap<>();

        LineNumberReader reader = new LineNumberReader(new FileReader(new File(skillAreasPath)));
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (!line.equals("SkillArea,Tag")) {
                String skillArea = line.split(",")[0];
                String tag = line.split(",")[1];
                if (SkillArea_TagsList.containsKey(skillArea)) {
                    ArrayList<String> tagsList = SkillArea_TagsList.get(skillArea);
                    tagsList.add(tag);
                    SkillArea_TagsList.replace(skillArea, tagsList);
                } else {
                    ArrayList<String> tagsList = new ArrayList<>();
                    tagsList.add(tag);
                    SkillArea_TagsList.put(skillArea, tagsList);
                }
            }
        }
        reader.close();
        return SkillArea_TagsList;
    }

    private HashMap<String, Double> calculate_term_probablities(TreeMap<String, ArrayList<String>> SkillArea_TagsList, Path answersIndexPathFormat, String probabilities_path) throws IOException, ParseException {
        HashMap<String, Double> term_probabilities = new HashMap<>();

        String term_probabilities_file_path = probabilities_path + "term_probabilities.txt";
        File f = new File(term_probabilities_file_path);
        if (f.exists()) {
            LineNumberReader reader = new LineNumberReader(new FileReader(new File(term_probabilities_file_path)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                String term = parts[0];
                Double probability = Double.parseDouble(parts[1].trim());
                term_probabilities.put(term, probability);
            }
            reader.close();
        }

        PrintWriter out = new PrintWriter(term_probabilities_file_path);
        Directory indexDirectory = FSDirectory.open(answersIndexPathFormat);
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            ArrayList<String> tags = item.getValue();
            // System.out.println("skill area: " + skillArea + "  tags: " + tags);
            for (String tag : tags) {
                //System.out.println(tag);
                String[] terms = tag.split("-");
                for (String termItem : terms) {
                    // System.out.println("term=" + termItem);
                    if (IsExistStopWord(termItem))
                        continue;
                    String term = CorrectTermText(termItem.trim());
                    if (term_probabilities.containsKey(term))
                        continue;
                    double probabilityTerm = CalculateProbabilityTerm(indexReader, "Body", term);
                    term_probabilities.put(term, probabilityTerm);
                }
            }
        }
        indexReader.close();
        for (Map.Entry<String, Double> item : term_probabilities.entrySet()) {
            String term = item.getKey();
            double probability = item.getValue();
            // System.out.println("term:" + term + " probability: " + probability);
            out.println(term + "\t" + probability);
        }
        out.close();
        return term_probabilities;
    }

    private void start(String dataSetName, String answersIndexPath, String skillAreasPath, String result_path, String probabilities_path) throws IOException, ParseException {
        File directory = new File(result_path);
        if (!directory.exists())
            directory.mkdirs();
        File directory2 = new File(probabilities_path);
        if (!directory2.exists())
            directory2.mkdirs();

        double lambda_d = 0.5;
        Path answersIndexPathFormat = new File(answersIndexPath).toPath();
        TreeMap<String, ArrayList<String>> SkillArea_TagsList = getSkillAreaTagsFile(skillAreasPath);
        System.out.println(SkillArea_TagsList.toString());

        HashMap<String, Double> term_probabilities = calculate_term_probablities(SkillArea_TagsList, answersIndexPathFormat, probabilities_path);
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            ArrayList<String> tags = item.getValue();
            System.out.println("skill area: " + skillArea + "  tags: " + tags);
            Balog(answersIndexPathFormat, skillArea, tags, dataSetName, lambda_d, result_path, probabilities_path, term_probabilities);
            System.gc();
        }

        PrintWriter out2 = new PrintWriter(result_path + "docNum_userId.txt");
        PrintWriter out3 = new PrintWriter(result_path + "docNum_postId.txt");
        Searcher searcher = new Searcher(answersIndexPathFormat, "Body");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int docNum = scoreDoc.doc;
            int postId = Integer.parseInt(doc.getField("Id").stringValue());
            out2.println(docNum + "\t" + userId);
            out3.println(docNum + "\t" + postId);
        }
        out2.close();
        out3.close();
    }

    public void Balog(Path answerPath, String skillArea, ArrayList<String> tagsOfSkillArea, String dataSetName, double lambda, String result_path, String probabilities_path, HashMap<String, Double> term_probabilities) throws IOException, ParseException {
        CalculateProbabilitySAGivenExpert(answerPath, skillArea, tagsOfSkillArea, dataSetName, lambda, result_path, probabilities_path, term_probabilities);
    }

    private HashMap<String, HashMap<Integer, Double>> loadTermDocProbabilities(String path, ArrayList<String> tagsOfSkillArea) throws IOException, ParseException {
        HashMap<String, HashMap<Integer, Double>> term_doc_probabilities = new HashMap<>();

        String probabilities_path = path + "term_doc_probabilities/";
        File dir = new File(probabilities_path);
        if (!dir.exists()) {
            dir.mkdir();
            return term_doc_probabilities;
        }

        for (String tag : tagsOfSkillArea) {
            System.out.println(tag);
            String[] terms = tag.split("-");
            for (String termItem : terms) {
                if (IsExistStopWord(termItem))
                    continue;
                String term = CorrectTermText(termItem.trim());

                String term_doc_probabilities_file = probabilities_path + term + "_doc_probabilities.txt";
                File f = new File(term_doc_probabilities_file);
                if (f.exists()) {
                    LineNumberReader reader = new LineNumberReader(new FileReader(new File(term_doc_probabilities_file)));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        Integer docNum = Integer.parseInt(parts[0]);
                        Double probability = Double.parseDouble(parts[1].trim());
                        if (term_doc_probabilities.containsKey(term)) {
                            term_doc_probabilities.get(term).put(docNum, probability);
                        } else {
                            HashMap<Integer, Double> probabilities = new HashMap<>();
                            probabilities.put(docNum, probability);
                            term_doc_probabilities.put(term, probabilities);
                        }
                    }
                    reader.close();
                }
            }
        }
        return term_doc_probabilities;
    }

    private void saveTermDocProbabilities(HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, String probabilities_path) throws IOException, ParseException {
        for (Map.Entry<String, HashMap<Integer, Double>> item1 : term_doc_probabilities.entrySet()) {
            String term = item1.getKey();
            PrintWriter out = new PrintWriter(probabilities_path + "term_doc_probabilities/" + term + "_doc_probabilities.txt");
            for (Map.Entry<Integer, Double> item2 : item1.getValue().entrySet()) {
                Integer docNum = item2.getKey();
                double probability = item2.getValue();
                //System.out.println("docNum:" + docNum + " term:" + term + " probability: " + probability);
                out.println(docNum + "\t" + probability);
            }
            out.close();
        }
    }

    public void CalculateProbabilitySAGivenExpert(Path answerPath, String skillArea, ArrayList<String> tagsOfSkillArea, String dataSetName, double lambda, String result_path, String probabilities_path, HashMap<String, Double> term_probabilities) throws IOException, ParseException {
        HashMap<String, HashMap<Integer, Double>> term_doc_probabilities = loadTermDocProbabilities(probabilities_path, tagsOfSkillArea);
        PrintWriter out = new PrintWriter(result_path + skillArea + "_balog_sorted_user_probability.txt");

        LinkedHashMap<Integer, Double> UId_probabilitySAGivenExpert = new LinkedHashMap<>();
        Searcher searcher = new Searcher(answerPath, "Body");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        Directory indexDirectory = FSDirectory.open(answerPath);
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        int count = getAnswersCount(dataSetName);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int docNum = scoreDoc.doc;
            count--;
            if (count % 10000 == 0)
                System.out.println(count);
            if (userId != -1) {
                AddDocProbabilityForEachUser(indexReader, userId, docNum, UId_probabilitySAGivenExpert, tagsOfSkillArea, lambda, term_doc_probabilities, term_probabilities);
            }
        }
        indexReader.close();

        LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = SortUsersProbability(UId_probabilitySAGivenExpert);
        UId_probabilitySAGivenExpert.clear();
        for (Map.Entry<Integer, Double> user_ProbabilityItem : SortedUId_probabilitySAGivenExpert.entrySet()) {
            int userId = user_ProbabilityItem.getKey();
            double probability = user_ProbabilityItem.getValue();
            out.println(userId + "\t" + probability);
        }
        out.close();
        saveTermDocProbabilities(term_doc_probabilities, probabilities_path);
    }

    private void AddDocProbabilityForEachUser(IndexReader indexReader, int userId, int docNum, LinkedHashMap<Integer, Double> UId_probabilitySAGivenExpert, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, HashMap<String, Double> term_probabilities) throws IOException {
        double currentProbabilityForEachDoc = CalculateProbabilityForEachDoc(indexReader, docNum, tagsOfSkillArea, lambda, term_doc_probabilities, term_probabilities);
        if (UId_probabilitySAGivenExpert.containsKey(userId)) {
            double previousProbability = UId_probabilitySAGivenExpert.get(userId);
            double newProbability = currentProbabilityForEachDoc + previousProbability;
            UId_probabilitySAGivenExpert.replace(userId, newProbability);
        } else
            UId_probabilitySAGivenExpert.put(userId, currentProbabilityForEachDoc);
    }

    private double CalculateProbabilityForEachDoc(IndexReader indexReader, int docNum, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, HashMap<String, Double> term_probabilities) throws IOException {
        double probabilityDocGivenExpert = 1;
        double probabilitySAGivenDocAndExpert = CalculateProbabilitySAGivenDocAndExpert(indexReader, docNum, tagsOfSkillArea, lambda, term_doc_probabilities, term_probabilities);
        double probabilityForEachDoc = probabilityDocGivenExpert * probabilitySAGivenDocAndExpert;
        return probabilityForEachDoc;
    }

    private double CalculateProbabilitySAGivenDocAndExpert(IndexReader indexReader, int docNum, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, HashMap<String, Double> term_probabilities) throws IOException {
        double productProbability = 1;
        for (String tag : tagsOfSkillArea) {
            double probabilityTagGivenDocAndExpert = CalculateProbabilityTagGivenThetaDoc(indexReader, tag.split("-"), docNum, lambda, term_doc_probabilities, term_probabilities);
            productProbability *= probabilityTagGivenDocAndExpert;
        }
        return productProbability;
    }

    private double CalculateProbabilityTagGivenThetaDoc(IndexReader indexReader, String[] terms, int docNum, double lambda, HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, HashMap<String, Double> term_probabilities) throws IOException {
        double productProbability = 1;
        for (String termItem : terms) {
            if (!IsExistStopWord(termItem))
                productProbability *= CalculateProbabilityTermGivenThetaDoc(indexReader, "Body", CorrectTermText(termItem.trim()), docNum, lambda, term_doc_probabilities, term_probabilities);
        }
        return productProbability;
    }

    private boolean IsExistStopWord(String term) {
        switch (term) {
            case "in":
                return true;
            case "on":
                return true;
            case "of":
                return true;
            case "at":
                return true;
            default:
                return false;
        }
    }

    private String CorrectTermText(String text) {
        text = CorrectText(text, "c#", "csharp");
        text = CorrectText(text, ".net", "dotnet");
        text = CorrectText(text, "c++", "cplusplus");
        text = CorrectText(text, ".htaccess", "dothtaccess");
        text = CorrectText(text, "three.js", "threedotjs");
        text = CorrectText(text, "socket.io", "socketdotio");
        text = CorrectText(text, "node.js", "nodedotjs");
        text = CorrectText(text, "knockout.js", "knockoutdotjs");
        text = CorrectText(text, "d3.js", "d3dotjs");
        text = CorrectText(text, "backbone.js", "backbonedotjs");
        text = CorrectText(text, "underscore.js", "underscoredotjs");
        text = CorrectText(text, "ember.js", "emberdotjs");
        text = CorrectText(text, "handlebars.js", "handlebarsdotjs");
        return text;
    }

    private String CorrectText(String text, String oldWord, String newWord) {
        String CorrectedText = text.toLowerCase();
        if (text.toLowerCase().contains(oldWord))
            CorrectedText = text.toLowerCase().replace(oldWord, newWord);
        return CorrectedText;
    }

    private double CalculateProbabilityTermGivenThetaDoc(IndexReader indexReader, String field, String term, int docNum, double lambdaD, HashMap<String, HashMap<Integer, Double>> term_doc_probabilities, HashMap<String, Double> term_probabilities) throws IOException {
        double probabilityTermGivenDoc;
        try {
            probabilityTermGivenDoc = term_doc_probabilities.get(term).get(docNum);
        } catch (Exception ex) {
            probabilityTermGivenDoc = CalculateProbabilityTermGivenDoc(indexReader, field, term, docNum);
            if (term_doc_probabilities.containsKey(term)) {
                term_doc_probabilities.get(term).put(docNum, probabilityTermGivenDoc);
            } else {
                HashMap<Integer, Double> probabilities = new HashMap<>();
                probabilities.put(docNum, probabilityTermGivenDoc);
                term_doc_probabilities.put(term, probabilities);
            }
        }

        //double probabilityTerm = CalculateProbabilityTerm(indexReader, field, term);
        double probabilityTerm = term_probabilities.get(term);
        double probabilityTermGivenThetaDoc = ((1 - lambdaD) * probabilityTermGivenDoc) + (lambdaD * probabilityTerm);
        return probabilityTermGivenThetaDoc;
    }

    private double CalculateProbabilityTermGivenDoc(IndexReader indexReader, String field, String term, int docNum) throws IOException {
        double termFreq = (double) (CalculateTermFreq(indexReader, field, term, docNum));
        double docLength = (double) (CalculateDocLength(indexReader, field, docNum));
        double probabilityTermGivenDoc = termFreq / docLength;
        return probabilityTermGivenDoc;
    }

    private long CalculateTermFreq(IndexReader indexReader, String field, String termOfQuery, int docNum) throws IOException {
        Terms termVector = indexReader.getTermVector(docNum, field);
        TermsEnum itr = termVector.iterator();
        long termFreq = SearchTermFreqInDoc(itr, termOfQuery);
        return termFreq;
    }

    private long SearchTermFreqInDoc(TermsEnum itr, String termOfQuery) throws IOException {
        BytesRef term = null;
        long termFreq = 0;
        while ((term = itr.next()) != null) {
            String termName = term.utf8ToString();
            if (termName.equals(termOfQuery)) {
                termFreq = itr.totalTermFreq();
                break;
            }
        }
        return termFreq;
    }

    private long CalculateDocLength(IndexReader indexReader, String field, int docNum) throws IOException {
        Terms termVector = indexReader.getTermVector(docNum, field);
        TermsEnum itr = termVector.iterator();
        long docLength = SumDocTermsFreq(itr);
        return docLength;
    }

    private long SumDocTermsFreq(TermsEnum itr) throws IOException {
        BytesRef term = null;
        long sumTermsFreq = 0;
        while ((term = itr.next()) != null) {
            sumTermsFreq += itr.totalTermFreq();
        }
        return sumTermsFreq;
    }

    private double CalculateProbabilityTerm(IndexReader indexReader, String field, String term) throws IOException {
        double totalTermFreq = (double) (CalculateTotalTermFreq(indexReader, field, term));
        double collectionLength = (double) (CalculateCollectionLength(indexReader, field));
        double probabilityTerm = totalTermFreq / collectionLength;
        return probabilityTerm;
    }

    private long CalculateTotalTermFreq(IndexReader indexReader, String field, String term) throws IOException {
        long totalTermFreq = indexReader.totalTermFreq(new Term(field, term));
        return totalTermFreq;
    }

    private long CalculateCollectionLength(IndexReader indexReader, String field) throws IOException {
        long collectionLength = indexReader.getSumTotalTermFreq(field);
        return collectionLength;
    }

    private int getAnswersCount(String dataSetName) {
        switch (dataSetName) {
            case "c#":
                return 1453649;
            case "java":
                return 1510812;
            case "android":
                return 917924;
            default:
                return 0;
        }
    }

    private LinkedHashMap<Integer, Double> SortUsersProbability(LinkedHashMap<Integer, Double> UId_probabilitySAGivenExpert) {
        TreeMap<Double, ArrayList<Integer>> probability_UserList = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<Integer, Double> user_ProbabilityItem : UId_probabilitySAGivenExpert.entrySet()) {
            int userId = user_ProbabilityItem.getKey();
            double probabilityValue = user_ProbabilityItem.getValue();
            if (probability_UserList.containsKey(probabilityValue)) {
                ArrayList<Integer> UsersIdList = probability_UserList.get(probabilityValue);
                UsersIdList.add(userId);
                probability_UserList.replace(probabilityValue, UsersIdList);
            } else {
                ArrayList<Integer> UsersIdList = new ArrayList<>();
                UsersIdList.add(userId);
                probability_UserList.put(probabilityValue, UsersIdList);
            }
        }

        LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = new LinkedHashMap<>();
        for (Map.Entry<Double, ArrayList<Integer>> item : probability_UserList.entrySet()) {
            double probabilityValue = item.getKey();
            ArrayList<Integer> userList = item.getValue();
            for (int user : userList) {
                SortedUId_probabilitySAGivenExpert.put(user, probabilityValue);
            }
        }
        return SortedUId_probabilitySAGivenExpert;
    }
}
