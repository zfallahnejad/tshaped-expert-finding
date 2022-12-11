package Baselines;

import Utils.*;
import Evaluation.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class DBA_XEBA {

    public static void main(String[] args) throws IOException, ParseException {
        String dataSetName, skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath;

        DBA_XEBA e = new DBA_XEBA();

        dataSetName = "java";
        answerIndexPath = "./files/Index/java/Answers";
        questionIndexPath = "./files/Index/java/Questions";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        skillShapesXMLPath = "./files/Golden/java/JavaSkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA_XEBA/";
        e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);

        dataSetName = "android";
        answerIndexPath = "./files/Index/android/Answers";
        questionIndexPath = "./files/Index/android/Questions";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        skillShapesXMLPath = "./files/Golden/android/AndroidSkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA_XEBA/";
        e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);

        dataSetName = "c#";
        answerIndexPath = "./files/Index/c#/Answers";
        questionIndexPath = "./files/Index/c#/Questions";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        skillShapesXMLPath = "./files/Golden/c#/C#SkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA_XEBA/";
        e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, RelevanceProbabilityPath, ResultPath);

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

    private void start(String skillAreasPath, String skillShapesXMLPath, String QuestionsIndexPath, String AnswersIndexPath, String RelevanceProbabilityPath, String XEBAFolderPath) throws IOException, ParseException {
        File directory = new File(XEBAFolderPath);
        if (!directory.exists())
            directory.mkdirs();

        TreeMap<String, ArrayList<String>> SkillArea_TagsList = loadSkillAreaTagsFile(skillAreasPath);
        LinkedHashMap<String, String> tag_SkillArea = getTagSkillArea(skillAreasPath);

        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityRGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            System.out.println("skillArea: " + skillArea);
            String balog_dbm_path = RelevanceProbabilityPath + skillArea + "_balog_sorted_user_probability.txt";
            LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = load_SA_SortedUId_probability(balog_dbm_path);
            SA__UId_ProbabilityRGivenExpertAndSA.put(skillArea, SortedUId_probabilitySAGivenExpert);
            System.gc();
        }

        GoldenUsersShapes G = new GoldenUsersShapes(skillAreasPath, skillShapesXMLPath);
        LinkedHashMap<String, LinkedHashMap<Integer, String>> SA__UId_GoldenShapes = G.getGoldenUsersShapesList();

        LinkedHashMap<Integer, ArrayList<String>> QId_SAList = findSAsOfQuestions(tag_SkillArea, new File(QuestionsIndexPath).toPath());
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount = CalculateDsaAnde(new File(AnswersIndexPath).toPath(), QId_SAList, XEBAFolderPath);
        LinkedHashMap<Integer, Integer> UId_DeCount = calculateDe(UId_DeAndDsaCount, XEBAFolderPath);
        LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde = calculatePsaAnde(UId_DeAndDsaCount, UId_DeCount, XEBAFolderPath);
        LinkedHashMap<Integer, Double> UId_He = calculateHe(UId__SA_PsaAnde, XEBAFolderPath);
        LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert = calculateNormalizedProbabilityTGivenExpert(UId_DeCount, UId_He, XEBAFolderPath);

        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityTandRGivenExpertAndSA = calculateEBAProbabilityTandRGivenExpertAndSA(UId_ProbabilityTGivenExpert, SA__UId_ProbabilityRGivenExpertAndSA, XEBAFolderPath);

        LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_ProbabilitySAGivenExpertAndT = calculateProbabilitySAGivenExpertAndT(UId__SA_PsaAnde, XEBAFolderPath);
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_XEBA_ProbabilityRandTGivenExpertAndSA = calculateXEBAProbabilityTandRGivenExpertAndSA(SA__UId_ProbabilityTandRGivenExpertAndSA, UId__SA_ProbabilitySAGivenExpertAndT, XEBAFolderPath);
        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_XEBA_ProbabilityRandTGivenExpertAndSA.entrySet()) {
            String SkillArea = SAItem.getKey();
            System.out.println("skill area: " + SkillArea);
            PrintWriter writer = new PrintWriter(XEBAFolderPath + SkillArea + "Users.csv");
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
        UId__SA_ProbabilitySAGivenExpertAndT.clear();

        NDCG ndcg = new NDCG(XEBAFolderPath, skillAreasPath, skillShapesXMLPath);
        NDCG_R ndcg_1 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 1);
        NDCG_R ndcg_5 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 5);
        NDCG_R ndcg_10 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 10);
        NDCG_R ndcg_15 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 15);
        NDCG_R ndcg_20 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 20);
        NDCG_R ndcg_30 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 30);
        NDCG_R ndcg_50 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 50);
        NDCG_R ndcg_100 = new NDCG_R(XEBAFolderPath, skillAreasPath, skillShapesXMLPath, 100);
        System.out.println("NDCG was Calculated!");

        MRR mrr = new MRR(XEBAFolderPath, skillAreasPath);
        MRR_R mrr_1 = new MRR_R(XEBAFolderPath, skillAreasPath, 1);
        MRR_R mrr_5 = new MRR_R(XEBAFolderPath, skillAreasPath, 5);
        MRR_R mrr_10 = new MRR_R(XEBAFolderPath, skillAreasPath, 10);
        MRR_R mrr_15 = new MRR_R(XEBAFolderPath, skillAreasPath, 15);
        MRR_R mrr_20 = new MRR_R(XEBAFolderPath, skillAreasPath, 20);
        MRR_R mrr_30 = new MRR_R(XEBAFolderPath, skillAreasPath, 30);
        MRR_R mrr_50 = new MRR_R(XEBAFolderPath, skillAreasPath, 50);
        MRR_R mrr_100 = new MRR_R(XEBAFolderPath, skillAreasPath, 100);
        System.out.println("MRR was Calculated!");

        ERR err = new ERR(XEBAFolderPath, skillAreasPath);
        ERR_R err_1 = new ERR_R(XEBAFolderPath, skillAreasPath, 1);
        ERR_R err_5 = new ERR_R(XEBAFolderPath, skillAreasPath, 5);
        ERR_R err_10 = new ERR_R(XEBAFolderPath, skillAreasPath, 10);
        ERR_R err_15 = new ERR_R(XEBAFolderPath, skillAreasPath, 15);
        ERR_R err_20 = new ERR_R(XEBAFolderPath, skillAreasPath, 20);
        ERR_R err_30 = new ERR_R(XEBAFolderPath, skillAreasPath, 30);
        ERR_R err_50 = new ERR_R(XEBAFolderPath, skillAreasPath, 50);
        ERR_R err_100 = new ERR_R(XEBAFolderPath, skillAreasPath, 100);
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

    private LinkedHashMap<Integer, ArrayList<String>> findSAsOfQuestions(LinkedHashMap<String, String> tag_SkillArea, Path QuestionsIndexPath) throws IOException, ParseException {
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

    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> CalculateDsaAnde(Path answersIndexPath, LinkedHashMap<Integer, ArrayList<String>> QId_SAList, String XEBAFolderPath) throws IOException, ParseException {
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

//        PrintWriter writer = new PrintWriter(XEBAFolderPath + "DsaAnde.txt");
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

    private LinkedHashMap<Integer, Integer> calculateDe(LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount, String XEBAFolderPath) throws IOException {
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

//        PrintWriter writer = new PrintWriter(XEBAFolderPath + "De.txt");
//        writer.println("UserId,SumAnswerCount");
//        for (Map.Entry<Integer, Integer> Item : UId_DeCount.entrySet()) {
//            int userId = Item.getKey();
//            int sumAnswerCount = Item.getValue();
//            writer.println(userId + "," + sumAnswerCount);
//        }
//        writer.close();
        return UId_DeCount;
    }

    private LinkedHashMap<Integer, LinkedHashMap<String, Double>> calculatePsaAnde(LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount, LinkedHashMap<Integer, Integer> UId_DeCount, String XEBAFolderPath) throws IOException {
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

//        PrintWriter writer = new PrintWriter(XEBAFolderPath + "PsaAnde.txt");
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

    private LinkedHashMap<Integer, Double> calculateHe(LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde, String XEBAFolderPath) throws IOException {
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

//        PrintWriter writer = new PrintWriter(XEBAFolderPath + "He.txt");
//        writer.println("UserId,Entropy");
//        for (Map.Entry<Integer, Double> Item : UId_He.entrySet()) {
//            int userId = Item.getKey();
//            double entropy = Item.getValue();
//            writer.println(userId + "," + entropy);
//        }
//        writer.close();
        return UId_He;
    }

    private LinkedHashMap<Integer, Double> calculateNormalizedProbabilityTGivenExpert(LinkedHashMap<Integer, Integer> UId_DeCount, LinkedHashMap<Integer, Double> UId_He, String XEBAFolderPath) throws IOException {
        LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert = new LinkedHashMap<>();
        double maxProbabilityTGivenExpert = calculateProbabilityTGivenExpert(UId_ProbabilityTGivenExpert, UId_DeCount, UId_He, XEBAFolderPath);
        for (Map.Entry<Integer, Double> userItem : UId_ProbabilityTGivenExpert.entrySet()) {
            int userId = userItem.getKey();
            double ProbabilityTGivenExpert = userItem.getValue();
            double NormalizedProbabilityTGivenExpert = ProbabilityTGivenExpert / maxProbabilityTGivenExpert;
            UId_ProbabilityTGivenExpert.replace(userId, NormalizedProbabilityTGivenExpert);
        }
//        PrintWriter writer2 = new PrintWriter(XEBAFolderPath + "ProbabilityTGivenExpert_Normal.txt");
//        writer2.println("UserId,P(T|e)");
//        for (Map.Entry<Integer, Double> Item : UId_ProbabilityTGivenExpert.entrySet()) {
//            int userId = Item.getKey();
//            double probability = Item.getValue();
//            writer2.println(userId + "," + probability);
//        }
//        writer2.close();
        return UId_ProbabilityTGivenExpert;
    }

    private double calculateProbabilityTGivenExpert(LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert, LinkedHashMap<Integer, Integer> UId_DeCount, LinkedHashMap<Integer, Double> UId_He, String XEBAFolderPath) throws IOException {
        PrintWriter writer = new PrintWriter(XEBAFolderPath + "ProbabilityTGivenExpert.txt");
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

    private LinkedHashMap<String, LinkedHashMap<Integer, Double>> calculateEBAProbabilityTandRGivenExpertAndSA(LinkedHashMap<Integer, Double> UId_ProbabilityTGivenExpert, LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityRGivenExpertAndSA, String XEBAFolderPath) throws IOException {
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityTandRGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_ProbabilityRGivenExpertAndSA.entrySet()) {
            String skillArea = SAItem.getKey();

//            PrintWriter writer = new PrintWriter(XEBAFolderPath + skillArea + "_EBA_Probability.txt");
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

    private LinkedHashMap<Integer, Double> sortHashMap(LinkedHashMap<Integer, Double> UId_ProbabilityTandRGivenExpertAndSA) {
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

    private TreeMap<Double, ArrayList<Integer>> getUserListGroupBySortedProbability(LinkedHashMap<Integer, Double> UId_ProbabilityTandRGivenExpertAndSA) {
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

    private LinkedHashMap<Integer, LinkedHashMap<String, Double>> calculateProbabilitySAGivenExpertAndT(LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_PsaAnde, String XEBAFolderPath) throws IOException {
        LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_ProbabilitySAGivenExpertAndT = new LinkedHashMap<>();
        PrintWriter writer = new PrintWriter(XEBAFolderPath + "SAGivenExpertAndT.txt");
        writer.println("UserId,SkillArea,P_sa_e,Max(P_sa_e),P(sa|e and T=1)");
        for (Map.Entry<Integer, LinkedHashMap<String, Double>> userItem : UId__SA_PsaAnde.entrySet()) {
            int userId = userItem.getKey();
            LinkedHashMap<String, Double> SA_PsaAnde = userItem.getValue();
            LinkedHashMap<String, Double> SA_ProbabilitySAGivenExpertAndT = new LinkedHashMap<>();
            double maxPsaAnde = maxPsaAndeForEachUserId(SA_PsaAnde);
            for (Map.Entry<String, Double> SAItem : SA_PsaAnde.entrySet()) {
                String skillArea = SAItem.getKey();
                double PsaAnde = SAItem.getValue();
                double ProbabilitySAGivenExpertAndT = PsaAnde / maxPsaAnde;
                SA_ProbabilitySAGivenExpertAndT.put(skillArea, ProbabilitySAGivenExpertAndT);
                writer.println(userId + "," + skillArea + "," + PsaAnde + "," + maxPsaAnde + "," + ProbabilitySAGivenExpertAndT);
            }
            UId__SA_ProbabilitySAGivenExpertAndT.put(userId, SA_ProbabilitySAGivenExpertAndT);
        }
        writer.close();
        return UId__SA_ProbabilitySAGivenExpertAndT;
    }

    private double maxPsaAndeForEachUserId(LinkedHashMap<String, Double> SA_PsaAnde) {
        double max = -1000;
        for (double PsaAnde : SA_PsaAnde.values()) {
            if (PsaAnde > max)
                max = PsaAnde;
        }
        return max;
    }

    private LinkedHashMap<String, LinkedHashMap<Integer, Double>> calculateXEBAProbabilityTandRGivenExpertAndSA(LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_EBA, LinkedHashMap<Integer, LinkedHashMap<String, Double>> UId__SA_ProbabilitySAGivenExpertAndT, String XEBAFolderPath) throws IOException {
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_XEBA_ProbabilityRandTGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_EBA.entrySet()) {
            String skillArea = SAItem.getKey();
//            PrintWriter writer = new PrintWriter(XEBAFolderPath + skillArea + "_XEBA_Probability.txt");
//            writer.println("UserId,EBA,P(sa|e and T=1),XEBA");

            LinkedHashMap<Integer, Double> userId_EBA = SAItem.getValue();
            LinkedHashMap<Integer, Double> userId_ProbabilityTandRGivenExpertAndSA = new LinkedHashMap<>();
            for (Map.Entry<Integer, Double> userItem : userId_EBA.entrySet()) {
                int userId = userItem.getKey();
                double EBA = userItem.getValue();
                double ProbabilitySAGivenExpertAndT = ((UId__SA_ProbabilitySAGivenExpertAndT.containsKey(userId) &&
                        UId__SA_ProbabilitySAGivenExpertAndT.get(userId).containsKey(skillArea)) ?
                        UId__SA_ProbabilitySAGivenExpertAndT.get(userId).get(skillArea) : 0.001);
                double ProbabilityTandRGivenExpertAndSA = EBA * ProbabilitySAGivenExpertAndT;
                userId_ProbabilityTandRGivenExpertAndSA.put(userId, ProbabilityTandRGivenExpertAndSA);
//                writer.println(userId + "," + EBA + "," + ProbabilitySAGivenExpertAndT + "," + ProbabilityTandRGivenExpertAndSA);
            }
            LinkedHashMap<Integer, Double> SortedUId_ProbabilityTandRGivenExpertAndSA = sortHashMap(userId_ProbabilityTandRGivenExpertAndSA);
            SA__UId_XEBA_ProbabilityRandTGivenExpertAndSA.put(skillArea, SortedUId_ProbabilityTandRGivenExpertAndSA);
//            writer.close();
        }
        return SA__UId_XEBA_ProbabilityRandTGivenExpertAndSA;
    }
}
