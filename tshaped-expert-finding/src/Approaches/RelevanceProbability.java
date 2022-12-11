package Approaches;

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
        String dataSetName, answersIndexPath, skillAreasPath, result_path, astm1_translations, astm2_translations;

        RelevanceProbability rp = new RelevanceProbability();

        dataSetName = "java";
        answersIndexPath = "./files/Index/java/Answers";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        astm1_translations = "./translations/" + dataSetName + "/astm1.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA1/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm1_translations, t);
        }
        astm2_translations = "./translations/" + dataSetName + "/astm2.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA2/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm2_translations, t);
        }

        dataSetName = "android";
        answersIndexPath = "./files/Index/android/Answers";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        astm1_translations = "./translations/" + dataSetName + "/astm1.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA1/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm1_translations, t);
        }
        astm2_translations = "./translations/" + dataSetName + "/astm2.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA2/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm2_translations, t);
        }

        dataSetName = "c#";
        answersIndexPath = "./files/Index/c#/Answers";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        astm1_translations = "./translations/" + dataSetName + "/astm1.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA1/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm1_translations, t);
        }
        astm2_translations = "./translations/" + dataSetName + "/astm2.txt";
        for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
            System.out.println("top: " + t);
            result_path = "./files/Relevance/TRBA2/" + dataSetName + "/top" + t + "/";
            rp.start(dataSetName, answersIndexPath, skillAreasPath, result_path, astm2_translations, t);
        }
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

    private HashMap<String, Double> calculate_term_probablities(TreeMap<String, ArrayList<String>> SkillArea_TagsList, Path answersIndexPathFormat) throws IOException, ParseException {
        HashMap<String, Double> term_probabilities = new HashMap<>();
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
        return term_probabilities;
    }

    public HashMap<String, ArrayList<ProbTranslate>> loadTagTranslations(String astm_translations, int countTranslations, TreeMap<String, ArrayList<String>> SkillArea_TagsList) throws IOException {
        int max_translations = 200;

        HashMap<String, ArrayList<ProbTranslate>> tags = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(astm_translations));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.contains("Type=Word, Dataset=Test, Label="))
                continue;
            String[] parts = line.split("Type=Word, Dataset=Test, Label=");
            parts = parts[1].split(", Translations=");
            String label = parts[0];
            String translations = parts[1].replace("[(", "").replace(")]", "");

            ArrayList<String> termsOfLabel = new ArrayList<>(Arrays.asList(label.split("-")));

            ArrayList<ProbTranslate> e = new ArrayList<>();
            String[] tr_score = translations.split("\\), \\(");
            if (tr_score.length > 1) {

                for (int i = 0; i < max_translations; i++) {
                    String translate = tr_score[i].substring(1, tr_score[i].lastIndexOf(',') - 1);
                    String score = tr_score[i].substring(tr_score[i].lastIndexOf(',') + 2);

                    // method 3 - version 2
                    if (termsOfLabel.contains(translate))
                        continue;
                    if (e.size() == countTranslations)
                        break;

                    // System.out.println(translate + "->" + score);
                    e.add(new ProbTranslate(translate, Double.parseDouble(score), label));
                }
            }
            tags.put(label, e);
        }
        return tags;
    }

    private void start(String dataSetName, String answersIndexPath, String skillAreasPath, String result_path, String sc_translations, int countTranslations) throws IOException, ParseException {
        File directory = new File(result_path);
        if (!directory.exists())
            directory.mkdirs();

        PrintWriter outfile = new PrintWriter(result_path + "log.txt");

        double lambda_d = 0.5;
        Path answersIndexPathFormat = new File(answersIndexPath).toPath();
        TreeMap<String, ArrayList<String>> SkillArea_TagsList = getSkillAreaTagsFile(skillAreasPath);
        System.out.println("SkillArea_TagsList:" + SkillArea_TagsList.toString());
        outfile.println("SkillArea_TagsList:" + SkillArea_TagsList.toString());
        HashMap<String, ArrayList<ProbTranslate>> tag_translations = loadTagTranslations(sc_translations, countTranslations, SkillArea_TagsList);
        System.out.println("TagTranslations:" + tag_translations.toString());
        outfile.println("TagTranslations:" + tag_translations.toString());

        HashMap<String, Double> term_probabilities = calculate_term_probablities(SkillArea_TagsList, answersIndexPathFormat);
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            ArrayList<String> tags = item.getValue();
            System.out.println("skill area: " + skillArea + "  tags: " + tags);
            outfile.println("skill area: " + skillArea + "  tags: " + tags);
            for (String tag : tags) {
                if (!tag_translations.containsKey(tag)) {
                    ArrayList<ProbTranslate> e = new ArrayList<>();
                    tag_translations.put(tag, e);
                }
                System.out.println("tag: " + tag + "  translation: " + tag_translations.get(tag));
                outfile.println("tag: " + tag + "  translation: " + tag_translations.get(tag));
            }
            TranslationLM(answersIndexPathFormat, skillArea, tags, dataSetName, lambda_d, result_path, term_probabilities, tag_translations);
            System.gc();
        }
        outfile.close();

//        PrintWriter out2 = new PrintWriter(result_path + "docNum_userId.txt");
//        PrintWriter out3 = new PrintWriter(result_path + "docNum_postId.txt");
//        Searcher searcher = new Searcher(answersIndexPathFormat, "Body");
//        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
//        for (ScoreDoc scoreDoc : hits.scoreDocs) {
//            Document doc = searcher.getDocument(scoreDoc);
//            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
//            int docNum = scoreDoc.doc;
//            int postId = Integer.parseInt(doc.getField("Id").stringValue());
//            out2.println(docNum + "\t" + userId);
//            out3.println(docNum + "\t" + postId);
//        }
//        out2.close();
//        out3.close();
    }

    public void TranslationLM(Path answerPath, String skillArea, ArrayList<String> tagsOfSkillArea, String dataSetName, double lambda, String result_path, HashMap<String, Double> term_probabilities, HashMap<String, ArrayList<ProbTranslate>> tag_translations) throws IOException, ParseException {
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
                AddDocProbabilityForEachUser(indexReader, userId, docNum, UId_probabilitySAGivenExpert, tagsOfSkillArea, lambda, term_probabilities, tag_translations);
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
    }

    private void AddDocProbabilityForEachUser(IndexReader indexReader, int userId, int docNum, LinkedHashMap<Integer, Double> UId_probabilitySAGivenExpert, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, Double> term_probabilities, HashMap<String, ArrayList<ProbTranslate>> tag_translations) throws IOException {
        double currentProbabilityForEachDoc = CalculateProbabilityForEachDoc(indexReader, docNum, tagsOfSkillArea, lambda, term_probabilities, tag_translations);
        if (UId_probabilitySAGivenExpert.containsKey(userId)) {
            double previousProbability = UId_probabilitySAGivenExpert.get(userId);
            double newProbability = currentProbabilityForEachDoc + previousProbability;
            UId_probabilitySAGivenExpert.replace(userId, newProbability);
        } else
            UId_probabilitySAGivenExpert.put(userId, currentProbabilityForEachDoc);
    }

    private double CalculateProbabilityForEachDoc(IndexReader indexReader, int docNum, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, Double> term_probabilities, HashMap<String, ArrayList<ProbTranslate>> tag_translations) throws IOException {
        double probabilityDocGivenExpert = 1;
        double probabilitySAGivenDocAndExpert = CalculateProbabilitySAGivenDocAndExpert(indexReader, docNum, tagsOfSkillArea, lambda, term_probabilities, tag_translations);
        double probabilityForEachDoc = probabilityDocGivenExpert * probabilitySAGivenDocAndExpert;
        return probabilityForEachDoc;
    }

    private double CalculateProbabilitySAGivenDocAndExpert(IndexReader indexReader, int docNum, ArrayList<String> tagsOfSkillArea, double lambda, HashMap<String, Double> term_probabilities, HashMap<String, ArrayList<ProbTranslate>> tag_translations) throws IOException {
        double productProbability = 1;
        for (String tag : tagsOfSkillArea) {
            double probabilityTagGivenDocAndExpert = CalculateProbabilityTagGivenThetaDoc(indexReader, tag, docNum, lambda, term_probabilities, tag_translations.get(tag));
            productProbability *= probabilityTagGivenDocAndExpert;
        }
        return productProbability;
    }

    private double CalculateProbabilityTagGivenThetaDoc(IndexReader indexReader, String tag, int docNum, double lambda, HashMap<String, Double> term_probabilities, ArrayList<ProbTranslate> translations) throws IOException {
        ArrayList<String> terms = new ArrayList<>(Arrays.asList(tag.split("-")));
        double probabilityTagGivenDoc = CalculateProbabilityTagGivenDoc(indexReader, docNum, terms, translations);

        double probabilityTag = 1;
        for (String termItem : terms) {
            if (!IsExistStopWord(termItem)) {
                String term = CorrectTermText(termItem.trim());
//                double probabilityTerm = 0.0;
//                if (term_probabilities.containsKey(term)) {
//                    probabilityTerm = term_probabilities.get(term);
//                }else{
//                    probabilityTerm=CalculateProbabilityTerm(indexReader, "Body", term);
//                }
//                probabilityTag *= probabilityTerm;
                probabilityTag *= term_probabilities.get(term);
            }
        }

        return ((1 - lambda) * probabilityTagGivenDoc) + (lambda * probabilityTag);
    }


    private double CalculateProbabilityTagGivenDoc(IndexReader indexReader, int docNum, ArrayList<String> termsOfTag, ArrayList<ProbTranslate> translations) throws IOException {
        ArrayList<String> translation_terms = new ArrayList<>();
        for (ProbTranslate term_prob : translations) {
            translation_terms.add(term_prob.word);
        }

        HashMap<String, Long> relevant_terms_freq = new HashMap<>();
        Terms termVector = indexReader.getTermVector(docNum, "Body");
        TermsEnum itr = termVector.iterator();
        BytesRef term_byte = null;
        long docLength = 0;
        while ((term_byte = itr.next()) != null) {
            String term = term_byte.utf8ToString();
            long termFreq = itr.totalTermFreq();
            if (termsOfTag.contains(term)) { // 1
                relevant_terms_freq.put(term, termFreq);
            }
            if (translation_terms.contains(term)){ // 2
                relevant_terms_freq.put(term, termFreq);
            }
            docLength += termFreq;
        }

        double probabilitySAGivenDoc = 0.0;
        for (String term : relevant_terms_freq.keySet()) {
            double probability_u_given_doc = (double) relevant_terms_freq.get(term) / (double) docLength;
            probabilitySAGivenDoc += probability_u_given_doc;
        }
        return probabilitySAGivenDoc;
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

    class ProbTranslate {
        private String tag;
        private String word;
        private double prob;

        public double getProb() {
            return prob;
        }

        public String getWord() {
            return word;
        }

        public String getTag() {
            return tag;
        }

        public ProbTranslate(String word, double prob, String tag) {
            this.word = word;
            this.prob = prob;
            this.tag = tag;
        }

        public String toString() {
            return "(word=" + word + ",prob=" + prob + ")";
        }
    }

    static class ProbTranslateComparator implements Comparator<ProbTranslate> {
        @Override
        public int compare(ProbTranslate w1, ProbTranslate w2) {
            // return w1.getProb() < w2.getProb() ? 1 : -1;
            if (w1.getProb() < w2.getProb())
                return 1;
            else if (w1.getProb() > w2.getProb())
                return -1;
            return 0;
        }
    }
}
