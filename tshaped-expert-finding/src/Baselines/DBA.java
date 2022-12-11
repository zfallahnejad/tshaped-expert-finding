package Baselines;

import Utils.*;
import Evaluation.*;

import java.io.*;
import java.util.*;

public class DBA {

    public static void main(String[] args) throws IOException {
        String dataSetName, skillAreasPath, skillShapesXMLPath, ResultPath, RelevanceProbabilityPath;
        DBA d = new DBA();

        dataSetName = "java";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        skillShapesXMLPath = "./files/Golden/java/JavaSkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA/";
        d.start(dataSetName, skillAreasPath, skillShapesXMLPath, RelevanceProbabilityPath, ResultPath, false);

        dataSetName = "android";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        skillShapesXMLPath = "./files/Golden/android/AndroidSkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA/";
        d.start(dataSetName, skillAreasPath, skillShapesXMLPath, RelevanceProbabilityPath, ResultPath, false);

        dataSetName = "c#";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        skillShapesXMLPath = "./files/Golden/c#/C#SkillShapes.xml";
        RelevanceProbabilityPath = "./files/Relevance/DBA/" + dataSetName + "/";
        ResultPath = "./files/Result/" + dataSetName + "/DBA/";
        d.start(dataSetName, skillAreasPath, skillShapesXMLPath, RelevanceProbabilityPath, ResultPath, false);
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

    private void start(String dataSetName, String skillAreasPath, String skillShapesXMLPath, String RelevanceProbabilityPath, String DBA_ResultPath, boolean applySoftMax) throws IOException {
        File directory = new File(DBA_ResultPath);
        if (!directory.exists())
            directory.mkdirs();

        TreeMap<String, ArrayList<String>> SkillArea_TagsList = getSkillAreaTagsFile(skillAreasPath);
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> SA__UId_ProbabilityRGivenExpertAndSA = new LinkedHashMap<>();
        for (Map.Entry<String, ArrayList<String>> item : SkillArea_TagsList.entrySet()) {
            String skillArea = item.getKey();
            String balog_dbm_path = RelevanceProbabilityPath + skillArea + "_balog_sorted_user_probability.txt";
            LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpert = load_SA_SortedUId_probability(balog_dbm_path, dataSetName, skillArea, applySoftMax);
            SA__UId_ProbabilityRGivenExpertAndSA.put(skillArea, SortedUId_probabilitySAGivenExpert);
            System.gc();
        }

        GoldenUsersShapes G = new GoldenUsersShapes(skillAreasPath, skillShapesXMLPath);
        LinkedHashMap<String, LinkedHashMap<Integer, String>> SA__UId_GoldenShapes = G.getGoldenUsersShapesList();

        for (Map.Entry<String, LinkedHashMap<Integer, Double>> SAItem : SA__UId_ProbabilityRGivenExpertAndSA.entrySet()) {
            String SkillArea = SAItem.getKey();
            System.out.println("skill area: " + SkillArea);
            PrintWriter writer = new PrintWriter(DBA_ResultPath + SkillArea + "Users.csv");
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

        MRR mrr = new MRR(DBA_ResultPath, skillAreasPath);
        MRR_R mrr_1 = new MRR_R(DBA_ResultPath, skillAreasPath, 1);
        MRR_R mrr_5 = new MRR_R(DBA_ResultPath, skillAreasPath, 5);
        MRR_R mrr_10 = new MRR_R(DBA_ResultPath, skillAreasPath, 10);
        MRR_R mrr_15 = new MRR_R(DBA_ResultPath, skillAreasPath, 15);
        MRR_R mrr_20 = new MRR_R(DBA_ResultPath, skillAreasPath, 20);
        MRR_R mrr_30 = new MRR_R(DBA_ResultPath, skillAreasPath, 30);
        MRR_R mrr_50 = new MRR_R(DBA_ResultPath, skillAreasPath, 50);
        MRR_R mrr_100 = new MRR_R(DBA_ResultPath, skillAreasPath, 100);
        System.out.println("MRR was Calculated!");

        ERR err = new ERR(DBA_ResultPath, skillAreasPath);
        ERR_R err_1 = new ERR_R(DBA_ResultPath, skillAreasPath, 1);
        ERR_R err_5 = new ERR_R(DBA_ResultPath, skillAreasPath, 5);
        ERR_R err_10 = new ERR_R(DBA_ResultPath, skillAreasPath, 10);
        ERR_R err_15 = new ERR_R(DBA_ResultPath, skillAreasPath, 15);
        ERR_R err_20 = new ERR_R(DBA_ResultPath, skillAreasPath, 20);
        ERR_R err_30 = new ERR_R(DBA_ResultPath, skillAreasPath, 30);
        ERR_R err_50 = new ERR_R(DBA_ResultPath, skillAreasPath, 50);
        ERR_R err_100 = new ERR_R(DBA_ResultPath, skillAreasPath, 100);
        System.out.println("ERR was Calculated!");

        NDCG ndcg = new NDCG(DBA_ResultPath, skillAreasPath, skillShapesXMLPath);
        NDCG_R ndcg_1 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 1);
        NDCG_R ndcg_5 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 5);
        NDCG_R ndcg_10 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 10);
        NDCG_R ndcg_15 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 15);
        NDCG_R ndcg_20 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 20);
        NDCG_R ndcg_30 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 30);
        NDCG_R ndcg_50 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 50);
        NDCG_R ndcg_100 = new NDCG_R(DBA_ResultPath, skillAreasPath, skillShapesXMLPath, 100);
        System.out.println("NDCG was Calculated!");
    }

    private LinkedHashMap<Integer, Double> load_SA_SortedUId_probability(String file_path, String dataSetName, String skillArea, boolean applySoftMax) throws IOException {
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
        System.out.println("dataSetName:" + dataSetName + " skillArea:" + skillArea + " #user_scores:" +
                SortedUId_probabilitySAGivenExpert.size() + " max_probability:" + max_probability);
        if (!applySoftMax)
            return SortedUId_probabilitySAGivenExpert;

        System.out.println("Starting to apply exponential to user_probability-max_probability");
        double sum_exp_diff_prob = 0.0;
        LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpertExpDiff = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> UserItem : SortedUId_probabilitySAGivenExpert.entrySet()) {
            int userId = UserItem.getKey();
            double probability = UserItem.getValue();
            double exp_diff_probability = Math.exp(probability - max_probability);
            sum_exp_diff_prob += exp_diff_probability;
            SortedUId_probabilitySAGivenExpertExpDiff.put(userId, exp_diff_probability);
        }
        System.out.println("sum_exp_diff_prob:" + sum_exp_diff_prob);

        LinkedHashMap<Integer, Double> SortedUId_probabilitySAGivenExpertSoftMax = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> UserItem : SortedUId_probabilitySAGivenExpertExpDiff.entrySet()) {
            int userId = UserItem.getKey();
            double probability = UserItem.getValue();
            SortedUId_probabilitySAGivenExpertSoftMax.put(userId, probability / sum_exp_diff_prob);
        }
        return SortedUId_probabilitySAGivenExpertSoftMax;
    }

}
