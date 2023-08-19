package Approaches;

import Utils.*;
import Evaluation.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class TRBA_EBA {

    public static void main(String[] args) throws IOException, ParseException {
        String dataSetName, skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath;

        TRBA_EBA e = new TRBA_EBA();

        dataSetName = "java";
        answerIndexPath = "./files/Index/java/Answers";
        questionIndexPath = "./files/Index/java/Questions";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        skillShapesXMLPath = "./files/Golden/java/JavaSkillShapes.xml";
        for (String model_type : Arrays.asList("TRBA1", "TRBA2", "SAST")) {
            for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
                try {
                    RelevanceProbabilityPath = "./files/Relevance/" + model_type + "/" + dataSetName + "/top" + t + "/";
                    System.out.println(RelevanceProbabilityPath);
                    ResultPath = "./files/Result/" + dataSetName + "/" + model_type + "_EBA/top" + t + "/";
                    e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);
                } catch (IOException ex) {
                    System.out.println("exception!");
                }
            }
        }

        dataSetName = "android";
        answerIndexPath = "./files/Index/android/Answers";
        questionIndexPath = "./files/Index/android/Questions";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        skillShapesXMLPath = "./files/Golden/android/AndroidSkillShapes.xml";
        for (String model_type : Arrays.asList("TRBA1", "TRBA2", "SAST")) {
            for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
                try {
                    RelevanceProbabilityPath = "./files/Relevance/" + model_type + "/" + dataSetName + "/top" + t + "/";
                    System.out.println(RelevanceProbabilityPath);
                    ResultPath = "./files/Result/" + dataSetName + "/" + model_type + "_EBA/top" + t + "/";
                    e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);
                } catch (IOException ex) {
                    System.out.println("exception!");
                }
            }
        }

        dataSetName = "c#";
        answerIndexPath = "./files/Index/c#/Answers";
        questionIndexPath = "./files/Index/c#/Questions";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        skillShapesXMLPath = "./files/Golden/c#/C#SkillShapes.xml";
        for (String model_type : Arrays.asList("TRBA1", "TRBA2", "SAST")) {
            for (int t : Arrays.asList(1, 2, 3, 4, 5)) {
                try {
                    RelevanceProbabilityPath = "./files/Relevance/" + model_type + "/" + dataSetName + "/top" + t + "/";
                    System.out.println(RelevanceProbabilityPath);
                    ResultPath = "./files/Result/" + dataSetName + "/" + model_type + "_EBA/top" + t + "/";
                    e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);
                } catch (IOException ex) {
                    System.out.println("exception!");
                }
            }
        }
    }

    private TreeMap<String, ArrayList<String>> loadSkillAreaTagsFile(String skillAreasPath) throws IOException {
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

    private LinkedHashMap<String, String> getTagSkillArea(String skillAreasPath) throws IOException {
        LinkedHashMap<String, String> tag_SkillArea = new LinkedHashMap<>();
        LineNumberReader reader = new LineNumberReader(new FileReader(new File(skillAreasPath)));
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (!line.equals("SkillArea,Tag")) {
                String skillArea = line.split(",")[0];
                String tag = line.split(",")[1];
                tag_SkillArea.put(tag, skillArea);
            }
        }
        reader.close();
        return tag_SkillArea;
    }

    private void start(String skillAreasPath, String skillShapesXMLPath, String QuestionsIndexPath, String AnswersIndexPath, String RelevanceProbabilityPath, String EBAFolderPath) throws IOException, ParseException {
        File directory = new File(EBAFolderPath);
        if (!directory.exists())
            directory.mkdirs();

        TreeMap<String, ArrayList<String>> SkillArea_TagsList = loadSkillAreaTagsFile(skillAreasPath);
        LinkedHashMap<String, String> tag_SkillArea = getTagSkillArea(skillAreasPath);

        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityRGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            System.out.println("skillArea: " + skillArea);
            String balog_dbm_path = RelevanceProbabilityPath + skillArea + "_sorted_user_probability.txt";
            LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = load_SA_SortedUId_probability(balog_dbm_path);
            SA__UId_ProbabilityRGivenExpertAndSA.put(skillArea, SortedUId_probabilitySAGivenExpert);
            System.gc();
        }

        GoldenUsersShapes G = new GoldenUsersShapes(skillAreasPath, skillShapesXMLPath);
        LinkedHashMap<String, LinkedHashMap<Integer, String>> SA__UId_GoldenShapes = G.getGoldenUsersShapesList();

        LinkedHashMap<Integer, ArrayList<String>> QId_SAList = findSAsOfQuestions(tag_SkillArea, new File(QuestionsIndexPath).toPath());
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount = CalculateDsaAnde(new File(AnswersIndexPath).toPath(), QId_SAList, EBAFolderPath);
        LinkedHashMap<Integer, Integer> UId_DeCount = calculateDe(UId_DeAndDsaCount, EBAFolderPath);
        LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde = calculatePsaAnde(UId_DeAndDsaCount, UId_DeCount, EBAFolderPath);
        LinkedHashMap<Integer, Double> UId_He = calculateHe(UId__SA_PsaAnde, EBAFolderPath);
        LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert = calculateNormalizedProbabilityTGivenExpert(UId_DeCount, UId_He, EBAFolderPath);

        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityTandRGivenExpertAndSA = calculateProbabilityTandRGivenExpertAndSA(UId_ProbabilityTGivenExpert, SA__UId_ProbabilityRGivenExpertAndSA, EBAFolderPath);
        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_ProbabilityTandRGivenExpertAndSA.entrySet()) {
            String SkillArea = SAItem.getKey();
            System.out.println("skill area: " + SkillArea);
            PrintWriter writer = new PrintWriter(EBAFolderPath + SkillArea + "Users.csv");
            writer.println("UserId,Shape");
            LinkedHashMap<Integer, Double> UId_Probability = SAItem.getValue();
            for (Map.Entry<Integer, Double> UserItem : UId_Probability.entrySet()) {
                int userId = UserItem.getKey();
                double userProbability = UserItem.getValue();
                String Shape = SA__UId_GoldenShapes.get(SkillArea).getOrDefault(userId, "NonExpert");
                writer.println(userId + "," + Shape + "," + userProbability);
            }
            writer.close();
        }
        SA__UId_ProbabilityRGivenExpertAndSA.clear();

        NDCG ndcg = new NDCG(EBAFolderPath, skillAreasPath, skillShapesXMLPath);
        NDCG_R ndcg_1 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 1);
        NDCG_R ndcg_5 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 5);
        NDCG_R ndcg_10 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 10);
        NDCG_R ndcg_15 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 15);
        NDCG_R ndcg_20 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 20);
        NDCG_R ndcg_30 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 30);
        NDCG_R ndcg_50 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 50);
        NDCG_R ndcg_100 = new NDCG_R(EBAFolderPath, skillAreasPath, skillShapesXMLPath, 100);
        System.out.println("NDCG was Calculated!");

        MRR mrr = new MRR(EBAFolderPath, skillAreasPath);
        MRR_R mrr_1 = new MRR_R(EBAFolderPath, skillAreasPath, 1);
        MRR_R mrr_5 = new MRR_R(EBAFolderPath, skillAreasPath, 5);
        MRR_R mrr_10 = new MRR_R(EBAFolderPath, skillAreasPath, 10);
        MRR_R mrr_15 = new MRR_R(EBAFolderPath, skillAreasPath, 15);
        MRR_R mrr_20 = new MRR_R(EBAFolderPath, skillAreasPath, 20);
        MRR_R mrr_30 = new MRR_R(EBAFolderPath, skillAreasPath, 30);
        MRR_R mrr_50 = new MRR_R(EBAFolderPath, skillAreasPath, 50);
        MRR_R mrr_100 = new MRR_R(EBAFolderPath, skillAreasPath, 100);
        System.out.println("MRR was Calculated!");

        ERR err = new ERR(EBAFolderPath, skillAreasPath);
        ERR_R err_1 = new ERR_R(EBAFolderPath, skillAreasPath, 1);
        ERR_R err_5 = new ERR_R(EBAFolderPath, skillAreasPath, 5);
        ERR_R err_10 = new ERR_R(EBAFolderPath, skillAreasPath, 10);
        ERR_R err_15 = new ERR_R(EBAFolderPath, skillAreasPath, 15);
        ERR_R err_20 = new ERR_R(EBAFolderPath, skillAreasPath, 20);
        ERR_R err_30 = new ERR_R(EBAFolderPath, skillAreasPath, 30);
        ERR_R err_50 = new ERR_R(EBAFolderPath, skillAreasPath, 50);
        ERR_R err_100 = new ERR_R(EBAFolderPath, skillAreasPath, 100);
        System.out.println("ERR was Calculated!");
    }

    private LinkedHashMap<Integer, Double> load_SA_SortedUId_probability(String file_path) throws IOException {
        LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = new LinkedHashMap<>();
        LineNumberReader reader = new LineNumberReader(new FileReader(new File(file_path)));
        String line = "";
        double max_probability = -100;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\t");
            int userId = Integer.parseInt(parts[0]);
            double probability = Double.parseDouble(parts[1]);
            if (max_probability == -100)
                max_probability = probability;
            SortedUId_probabilitySAGivenExpert.put(userId, probability);
        }
        reader.close();
        System.out.println(" #user_scores:" + SortedUId_probabilitySAGivenExpert.size() + " max_probability:" + max_probability);
        return SortedUId_probabilitySAGivenExpert;
    }

    private LinkedHashMap<Integer, ArrayList<String>> findSAsOfQuestions(
            LinkedHashMap<String, String> tag_SkillArea, Path QuestionsIndexPath) throws IOException, ParseException {
        LinkedHashMap<Integer, ArrayList<String>> QId_SAList = new LinkedHashMap<>();
        Searcher searcher = new Searcher(QuestionsIndexPath, "Tags");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int QId = Integer.parseInt(doc.getField("Id").stringValue());
            String[] tags = doc.getValues("Tags");
            ArrayList<String> skillAreaList = new ArrayList<>();
            for (String tag : tags) {
                if (tag_SkillArea.containsKey(tag)) {
                    String SA = tag_SkillArea.get(tag);
                    if (!skillAreaList.contains(SA)) {
                        skillAreaList.add(SA);
                    }
                }
            }
            if (!skillAreaList.isEmpty()) {
                QId_SAList.put(QId, skillAreaList);
            }
        }
        return QId_SAList;
    }

    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> CalculateDsaAnde(Path answersIndexPath, LinkedHashMap<Integer, ArrayList<String>> QId_SAList, String EBAFolderPath) throws IOException, ParseException {
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount = new LinkedHashMap<>();
        Searcher searcher = new Searcher(answersIndexPath, "Id");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int parantId = Integer.parseInt(doc.getField("ParentId").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            if (QId_SAList.containsKey(parantId) && userId != -1) {
                addToDsaAndeList(userId, QId_SAList.get(parantId), UId_DeAndDsaCount);
            }
        }

//        PrintWriter writer = new PrintWriter(EBAFolderPath + "DsaAnde.txt");
//        writer.println("UserId,skillArea,answerCount");
//        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> Item : UId_DeAndDsaCount.entrySet()) {
//            int userId = Item.getKey();
//            for (Map.Entry<String, Integer> Item2 : Item.getValue().entrySet()) {
//                String skillArea = Item2.getKey();
//                Integer answerCount = Item2.getValue();
//                writer.println(userId + "," + skillArea + "," + answerCount);
//            }
//        }
//        writer.close();
        return UId_DeAndDsaCount;
    }

    private void addToDsaAndeList(int userId, ArrayList<String> SAList, LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount) {
        for (String skillArea : SAList) {
            if (UId_DeAndDsaCount.containsKey(userId)) {
                LinkedHashMap<String, Integer> skillArea_AnswerCount = UId_DeAndDsaCount.get(userId);
                if (skillArea_AnswerCount.containsKey(skillArea)) {
                    int answerCount = skillArea_AnswerCount.get(skillArea);
                    answerCount++;
                    skillArea_AnswerCount.replace(skillArea, answerCount);
                } else {
                    skillArea_AnswerCount.put(skillArea, 1);
                }
                UId_DeAndDsaCount.replace(userId, skillArea_AnswerCount);
            } else {
                LinkedHashMap<String, Integer> skillArea_AnswerCount = new LinkedHashMap<>();
                skillArea_AnswerCount.put(skillArea, 1);
                UId_DeAndDsaCount.put(userId, skillArea_AnswerCount);
            }
        }
    }

    private LinkedHashMap<Integer, Integer> calculateDe(LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount, String EBAFolderPath) throws IOException {
        LinkedHashMap<Integer, Integer> UId_DeCount = new LinkedHashMap<>();
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> userItem : UId_DeAndDsaCount.entrySet()) {
            int userId = userItem.getKey();
            int sumAnswerCount = 0;
            LinkedHashMap<String, Integer> SkillArea_AnswerCount = userItem.getValue();
            for (Map.Entry<String, Integer> SAItem : SkillArea_AnswerCount.entrySet()) {
                int count = SAItem.getValue();
                sumAnswerCount += count;
            }
            UId_DeCount.put(userId, sumAnswerCount);
        }

//        PrintWriter writer = new PrintWriter(EBAFolderPath + "De.txt");
//        writer.println("UserId,SumAnswerCount");
//        for (Map.Entry<Integer, Integer> Item : UId_DeCount.entrySet()) {
//            int userId = Item.getKey();
//            int sumAnswerCount = Item.getValue();
//            writer.println(userId + "," + sumAnswerCount);
//        }
//        writer.close();
        return UId_DeCount;
    }

    private LinkedHashMap<Integer, LinkedHashMap<String, Double>> calculatePsaAnde(LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount, LinkedHashMap<Integer, Integer> UId_DeCount, String EBAFolderPath) throws IOException {
        LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde = new LinkedHashMap<>();
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> userItem : UId_DeAndDsaCount.entrySet()) {
            LinkedHashMap<String, Double> SA_PsaAnde = new LinkedHashMap<>();
            int userId = userItem.getKey();
            LinkedHashMap<String, Integer> SA_AnswerCount = userItem.getValue();
            for (Map.Entry<String, Integer> SAItem : SA_AnswerCount.entrySet()) {
                String skillArea = SAItem.getKey();
                int count = SAItem.getValue();
                double PsaAnde = (double) count / (double) UId_DeCount.get(userId);
                SA_PsaAnde.put(skillArea, PsaAnde);
            }
            UId__SA_PsaAnde.put(userId, SA_PsaAnde);
        }
        UId_DeAndDsaCount.clear();

//        PrintWriter writer = new PrintWriter(EBAFolderPath + "PsaAnde.txt");
//        writer.println("UserId,skillArea,PsaAnde");
//        for (Map.Entry<Integer, LinkedHashMap<String, Double>> Item : UId__SA_PsaAnde.entrySet()) {
//            int userId = Item.getKey();
//            for (Map.Entry<String, Double> Item2 : Item.getValue().entrySet()) {
//                String skillArea = Item2.getKey();
//                double probability = Item2.getValue();
//                writer.println(userId + "," + skillArea + "," + probability);
//            }
//        }
//        writer.close();
        return UId__SA_PsaAnde;
    }

    private LinkedHashMap<Integer, Double> calculateHe(LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde, String EBAFolderPath) throws IOException {
        LinkedHashMap<Integer, Double> UId_He = new LinkedHashMap<>();
        for (Map.Entry<Integer, LinkedHashMap<String, Double>> userItem : UId__SA_PsaAnde.entrySet()) {
            int userId = userItem.getKey();
            LinkedHashMap<String, Double> SA_PsaAnde = userItem.getValue();
            double sumHe = 0;
            for (Map.Entry<String, Double> SAItem : SA_PsaAnde.entrySet()) {
                String skillArea = SAItem.getKey();
                double PsaAnde = SAItem.getValue();
                double uncertainty = -PsaAnde * ((Math.log10(PsaAnde)) / (Math.log10(2)));
                sumHe += uncertainty;
            }
            UId_He.put(userId, sumHe);
        }

//        PrintWriter writer = new PrintWriter(EBAFolderPath + "He.txt");
//        writer.println("UserId,Entropy");
//        for (Map.Entry<Integer, Double> Item : UId_He.entrySet()) {
//            int userId = Item.getKey();
//            double entropy = Item.getValue();
//            writer.println(userId + "," + entropy);
//        }
//        writer.close();
        return UId_He;
    }

    private LinkedHashMap<Integer, Double> calculateNormalizedProbabilityTGivenExpert
            (LinkedHashMap<Integer, Integer> UId_DeCount, LinkedHashMap<Integer, Double> UId_He, String
                    EBAFolderPath) throws IOException {
        LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert = new LinkedHashMap<>();
        double maxProbabilityTGivenExpert = calculateProbabilityTGivenExpert(UId_ProbabilityTGivenExpert, UId_DeCount, UId_He, EBAFolderPath);
        for (Map.Entry<Integer, Double> userItem : UId_ProbabilityTGivenExpert.entrySet()) {
            int userId = userItem.getKey();
            double ProbabilityTGivenExpert = userItem.getValue();
            double NormalizedProbabilityTGivenExpert = ProbabilityTGivenExpert / maxProbabilityTGivenExpert;
            UId_ProbabilityTGivenExpert.replace(userId, NormalizedProbabilityTGivenExpert);
        }
//        PrintWriter writer2 = new PrintWriter(EBAFolderPath + "ProbabilityTGivenExpert_Normal.txt");
//        writer2.println("UserId,P(T|e)");
//        for (Map.Entry<Integer, Double> Item : UId_ProbabilityTGivenExpert.entrySet()) {
//            int userId = Item.getKey();
//            double probability = Item.getValue();
//            writer2.println(userId + "," + probability);
//        }
//        writer2.close();
        return UId_ProbabilityTGivenExpert;
    }

    private double calculateProbabilityTGivenExpert
            (LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert, LinkedHashMap<Integer, Integer> UId_DeCount, LinkedHashMap<Integer, Double> UId_He, String
                    EBAFolderPath) throws IOException {
        PrintWriter writer = new PrintWriter(EBAFolderPath + "ProbabilityTGivenExpert.txt");
        writer.println("UserId,P(T|e)");

        double max = -1000;
        for (Map.Entry<Integer, Double> userItem : UId_He.entrySet()) {
            int userId = userItem.getKey();
            double He = userItem.getValue();
            double De = UId_DeCount.get(userId);
            double logDe = Math.log10(De) / Math.log10(2);
            double ProbabilityTGivenExpert = logDe / (He + 0.01);
            if (ProbabilityTGivenExpert > max) {
                max = ProbabilityTGivenExpert;
            }
            writer.println(userId + "," + ProbabilityTGivenExpert);
            UId_ProbabilityTGivenExpert.put(userId, ProbabilityTGivenExpert);
        }
        writer.close();
        return max;
    }

    private LinkedHashMap<String, LinkedHashMap<Integer, Double>> calculateProbabilityTandRGivenExpertAndSA(
            LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert, LinkedHashMap<String,
            LinkedHashMap<Integer, Double>> SA__UId_ProbabilityRGivenExpertAndSA, String EBAFolderPath) throws IOException {
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityTandRGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_ProbabilityRGivenExpertAndSA.entrySet()) {
            String skillArea = SAItem.getKey();

//            PrintWriter writer = new PrintWriter(EBAFolderPath + skillArea + "_EBA_Probability.txt");
//            writer.println("UserId,P(R=1|e and sa),P(T=1|e and sa),P(R=1 and T=1|e and sa)");

            LinkedHashMap<Integer, Double> userId_ProbabilityRGivenExpertAndSA = SAItem.getValue();
            LinkedHashMap<Integer, Double> userId_ProbabilityTandRGivenExpertAndSA = new LinkedHashMap<>();
            for (Map.Entry<Integer, Double> userItem : userId_ProbabilityRGivenExpertAndSA.entrySet()) {
                int userId = userItem.getKey();
                double probabilityRGivenExpertAndSA = userItem.getValue();
                double probabilityTandRGivenExpertAndSA = probabilityRGivenExpertAndSA *
                        (UId_ProbabilityTGivenExpert.getOrDefault(userId, 0.001));
                userId_ProbabilityTandRGivenExpertAndSA.put(userId, probabilityTandRGivenExpertAndSA);
//                writer.println(userId + "," + probabilityRGivenExpertAndSA + "," +
//                        UId_ProbabilityTGivenExpert.getOrDefault(userId, 0.001) + "," +
//                        probabilityTandRGivenExpertAndSA);
            }
            LinkedHashMap<Integer, Double> SortedUId_ProbabilityTandRGivenExpertAndSA = sortHashMap(userId_ProbabilityTandRGivenExpertAndSA);
            SA__UId_ProbabilityTandRGivenExpertAndSA.put(skillArea, SortedUId_ProbabilityTandRGivenExpertAndSA);
//            writer.close();
        }
        return SA__UId_ProbabilityTandRGivenExpertAndSA;
    }

    private LinkedHashMap<Integer, Double> sortHashMap
            (LinkedHashMap<Integer, Double> UId_ProbabilityTandRGivenExpertAndSA) {
        LinkedHashMap<Integer, Double> UId_Probability = new LinkedHashMap<>();
        TreeMap<Double, ArrayList<Integer>> probability_UserList = getUserListGroupBySortedProbability(UId_ProbabilityTandRGivenExpertAndSA);
        for (Map.Entry<Double, ArrayList<Integer>> item : probability_UserList.entrySet()) {
            double probabilityValue = item.getKey();
            ArrayList<Integer> userList = item.getValue();
            for (int userId : userList) {
                UId_Probability.put(userId, probabilityValue);
            }
        }
        probability_UserList.clear();
        return UId_Probability;
    }

    private TreeMap<Double, ArrayList<Integer>> getUserListGroupBySortedProbability
            (LinkedHashMap<Integer, Double> UId_ProbabilityTandRGivenExpertAndSA) {
        TreeMap<Double, ArrayList<Integer>> probability_UserList = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<Integer, Double> userItem : UId_ProbabilityTandRGivenExpertAndSA.entrySet()) {
            int userId = userItem.getKey();
            double probabilityValue = userItem.getValue();
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
        return probability_UserList;
    }
}
