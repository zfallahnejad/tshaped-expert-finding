package Utils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class Utils {
    private LinkedHashMap<String, String> tag_SkillArea = new LinkedHashMap<>();
    private LinkedHashMap<Integer, ArrayList<String>> QId_SAList = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_DeAndDsaCount = new LinkedHashMap<>();

    private LinkedHashMap<String, Integer> SA_AnswerCount = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> SA_QuestionCount = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> SA_AnswerScore = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> SA_QuestionScore = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_e_and_sa_AnswerCount = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_e_and_sa_QuestionCount = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_e_and_sa_AnswerScore = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, Integer>> UId_e_and_sa_QuestionScore = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException, ParseException {
        String dataSetName, skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, ResultsPath;

        dataSetName = "java";
        answerIndexPath = "./files/Index/java/Answers";
        questionIndexPath = "./files/Index/java/Questions";
        skillAreasPath = "./files/Golden/java/javaCluster.csv";
        skillShapesXMLPath = "./files/Golden/java/JavaSkillShapes.xml";
        ResultsPath = "./files/Temp/" + dataSetName + "/";
        Utils e = new Utils();
        e.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, ResultsPath);

        dataSetName = "android";
        answerIndexPath = "./files/Index/android/Answers";
        questionIndexPath = "./files/Index/android/Questions";
        skillAreasPath = "./files/Golden/android/AndroidCluster.csv";
        skillShapesXMLPath = "./files/Golden/android/AndroidSkillShapes.xml";
        ResultsPath = "./files/Temp/" + dataSetName + "/";
        Utils e2 = new Utils();
        e2.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, ResultsPath);

        dataSetName = "c#";
        answerIndexPath = "./files/Index/c#/Answers";
        questionIndexPath = "./files/Index/c#/Questions";
        skillAreasPath = "./files/Golden/c#/C#Cluster.csv";
        skillShapesXMLPath = "./files/Golden/c#/C#SkillShapes.xml";
        ResultsPath = "./files/Temp/" + dataSetName + "/";
        Utils e3 = new Utils();
        e3.start(skillAreasPath, skillShapesXMLPath, questionIndexPath, answerIndexPath, ResultsPath);
    }

    private void start(String skillAreasPath, String skillShapesXMLPath, String QuestionsIndexPath, String AnswersIndexPath, String ResultsPath) throws IOException, ParseException {
        File directory = new File(ResultsPath);
        if (!directory.exists())
            directory.mkdirs();
        System.out.println("skillAreasPath:"+ skillAreasPath);

        TreeMap<String, ArrayList<String>> SkillArea_TagsList = loadSkillAreaTagsFile(skillAreasPath);

        document_count_e_and_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath);
        document_score_e_and_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath);
        document_count_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath, false);
        document_count_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath, true);
        document_score_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath, false);
        document_score_sa(new File(QuestionsIndexPath).toPath(), new File(AnswersIndexPath).toPath(), ResultsPath, true);
    }

    private TreeMap<String, ArrayList<String>> loadSkillAreaTagsFile(String skillAreasPath) throws IOException {
        TreeMap<String, ArrayList<String>> SkillArea_TagsList = new TreeMap<>();
        LineNumberReader reader = new LineNumberReader(new FileReader(new File(skillAreasPath)));
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (!line.equals("SkillArea,Tag")) {
                String skillArea = line.split(",")[0];
                String tag = line.split(",")[1];
                tag_SkillArea.put(tag, skillArea);
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

    private void document_count_e_and_sa(Path QuestionsIndexPath, Path answersIndexPath, String ResultsPath) throws IOException, ParseException {
        Searcher searcher = new Searcher(QuestionsIndexPath, "Tags");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int QId = Integer.parseInt(doc.getField("Id").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
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

                if (userId != -1) {
                    for (String skill_area : skillAreaList) {
                        if (UId_e_and_sa_QuestionCount.containsKey(userId)) {
                            LinkedHashMap<String, Integer> skillArea_QuestionCount = UId_e_and_sa_QuestionCount.get(userId);
                            if (skillArea_QuestionCount.containsKey(skill_area)) {
                                int questionCount = skillArea_QuestionCount.get(skill_area);
                                questionCount++;
                                skillArea_QuestionCount.replace(skill_area, questionCount);
                            } else {
                                skillArea_QuestionCount.put(skill_area, 1);
                            }
                            UId_e_and_sa_QuestionCount.replace(userId, skillArea_QuestionCount);
                        } else {
                            LinkedHashMap<String, Integer> skillArea_QuestionCount = new LinkedHashMap<>();
                            skillArea_QuestionCount.put(skill_area, 1);
                            UId_e_and_sa_QuestionCount.put(userId, skillArea_QuestionCount);
                        }

                    }
                }
            }
        }

        Searcher searcher2 = new Searcher(answersIndexPath, "Id");
        TopDocs hits2 = searcher2.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = searcher2.getDocument(scoreDoc);
            int parentId = Integer.parseInt(doc.getField("ParentId").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            if (QId_SAList.containsKey(parentId) && userId != -1) {
                for (String skillArea : QId_SAList.get(parentId)) {
                    if (UId_e_and_sa_AnswerCount.containsKey(userId)) {
                        LinkedHashMap<String, Integer> skillArea_AnswerCount = UId_e_and_sa_AnswerCount.get(userId);
                        if (skillArea_AnswerCount.containsKey(skillArea)) {
                            int answerCount = skillArea_AnswerCount.get(skillArea);
                            answerCount++;
                            skillArea_AnswerCount.replace(skillArea, answerCount);
                        } else {
                            skillArea_AnswerCount.put(skillArea, 1);
                        }
                        UId_e_and_sa_AnswerCount.replace(userId, skillArea_AnswerCount);
                    } else {
                        LinkedHashMap<String, Integer> skillArea_AnswerCount = new LinkedHashMap<>();
                        skillArea_AnswerCount.put(skillArea, 1);
                        UId_e_and_sa_AnswerCount.put(userId, skillArea_AnswerCount);
                    }
                }
            }
        }

        PrintWriter writer = new PrintWriter(ResultsPath + "D_e_and_sa_questions.txt");
        writer.println("UserId,skillArea,questionCount");
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> Item : UId_e_and_sa_QuestionCount.entrySet()) {
            int userId = Item.getKey();
            for (Map.Entry<String, Integer> Item2 : Item.getValue().entrySet()) {
                String skillArea = Item2.getKey();
                Integer questionCount = Item2.getValue();
                writer.println(userId + "," + skillArea + "," + questionCount);
            }
        }
        writer.close();

        PrintWriter writer2 = new PrintWriter(ResultsPath + "D_e_and_sa_answers.txt");
        writer2.println("UserId,skillArea,answerCount");
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> Item : UId_e_and_sa_AnswerCount.entrySet()) {
            int userId = Item.getKey();
            for (Map.Entry<String, Integer> Item2 : Item.getValue().entrySet()) {
                String skillArea = Item2.getKey();
                Integer answerCount = Item2.getValue();
                writer2.println(userId + "," + skillArea + "," + answerCount);
            }
        }
        writer2.close();
    }

    private void document_score_e_and_sa(Path QuestionsIndexPath, Path answersIndexPath, String ResultsPath) throws IOException, ParseException {
        Searcher searcher = new Searcher(QuestionsIndexPath, "Tags");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int QId = Integer.parseInt(doc.getField("Id").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int score = Integer.parseInt(doc.getField("Score").stringValue());
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

                if (userId != -1) {
                    for (String skill_area : skillAreaList) {
                        if (UId_e_and_sa_QuestionScore.containsKey(userId)) {
                            LinkedHashMap<String, Integer> skillArea_QuestionScore = UId_e_and_sa_QuestionScore.get(userId);
                            int questionScore = skillArea_QuestionScore.getOrDefault(skill_area, 0);
                            skillArea_QuestionScore.put(skill_area, questionScore + score);
                            UId_e_and_sa_QuestionScore.replace(userId, skillArea_QuestionScore);
                        } else {
                            LinkedHashMap<String, Integer> skillArea_QuestionScore = new LinkedHashMap<>();
                            skillArea_QuestionScore.put(skill_area, score);
                            UId_e_and_sa_QuestionScore.put(userId, skillArea_QuestionScore);
                        }
                    }
                }
            }
        }

        Searcher searcher2 = new Searcher(answersIndexPath, "Id");
        TopDocs hits2 = searcher2.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = searcher2.getDocument(scoreDoc);
            int parentId = Integer.parseInt(doc.getField("ParentId").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int score = Integer.parseInt(doc.getField("Score").stringValue());
            if (QId_SAList.containsKey(parentId) && userId != -1) {
                for (String skillArea : QId_SAList.get(parentId)) {
                    if (UId_e_and_sa_AnswerScore.containsKey(userId)) {
                        LinkedHashMap<String, Integer> skillArea_AnswerScore = UId_e_and_sa_AnswerScore.get(userId);
                        int answerScore = skillArea_AnswerScore.getOrDefault(skillArea, 0);
                        skillArea_AnswerScore.put(skillArea, answerScore + score);
                        UId_e_and_sa_AnswerScore.replace(userId, skillArea_AnswerScore);
                    } else {
                        LinkedHashMap<String, Integer> skillArea_AnswerScore = new LinkedHashMap<>();
                        skillArea_AnswerScore.put(skillArea, score);
                        UId_e_and_sa_AnswerScore.put(userId, skillArea_AnswerScore);
                    }
                }
            }
        }

        PrintWriter writer = new PrintWriter(ResultsPath + "Score_e_and_sa_questions.txt");
        writer.println("UserId,skillArea,questionScore");
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> Item : UId_e_and_sa_QuestionScore.entrySet()) {
            int userId = Item.getKey();
            for (Map.Entry<String, Integer> Item2 : Item.getValue().entrySet()) {
                String skillArea = Item2.getKey();
                Integer questionScore = Item2.getValue();
                writer.println(userId + "," + skillArea + "," + questionScore);
            }
        }
        writer.close();

        PrintWriter writer2 = new PrintWriter(ResultsPath + "Score_e_and_sa_answers.txt");
        writer2.println("UserId,skillArea,answerScore");
        for (Map.Entry<Integer, LinkedHashMap<String, Integer>> Item : UId_e_and_sa_AnswerScore.entrySet()) {
            int userId = Item.getKey();
            for (Map.Entry<String, Integer> Item2 : Item.getValue().entrySet()) {
                String skillArea = Item2.getKey();
                Integer answerScore = Item2.getValue();
                writer2.println(userId + "," + skillArea + "," + answerScore);
            }
        }
        writer2.close();
    }

    private void document_count_sa(Path QuestionsIndexPath, Path answersIndexPath, String ResultsPath, boolean without_minus_one_userId) throws IOException, ParseException {
        SA_AnswerCount.clear();
        SA_QuestionCount.clear();

        Searcher searcher = new Searcher(QuestionsIndexPath, "Tags");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int QId = Integer.parseInt(doc.getField("Id").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            String[] tags = doc.getValues("Tags");
            ArrayList<String> skillAreaList = new ArrayList<>();
            for (String tag : tags) {
                if (tag_SkillArea.containsKey(tag)) {
                    String SA = tag_SkillArea.get(tag);
                    if (without_minus_one_userId) {
                        if (!skillAreaList.contains(SA)) {
                            skillAreaList.add(SA);
                        }
                    } else {
                        skillAreaList.add(SA);

                    }
                }
            }
            if (!skillAreaList.isEmpty()) {
                QId_SAList.put(QId, skillAreaList);

                if (without_minus_one_userId) {
                    if (userId != -1) {
                        for (String skill_area : skillAreaList) {
                            int count = SA_QuestionCount.getOrDefault(skill_area, 0);
                            count++;
                            SA_QuestionCount.put(skill_area, count);
                        }
                    }
                } else {
                    for (String skill_area : skillAreaList) {
                        int count = SA_QuestionCount.getOrDefault(skill_area, 0);
                        count++;
                        SA_QuestionCount.put(skill_area, count);
                    }
                }
            }
        }

        Searcher searcher2 = new Searcher(answersIndexPath, "Id");
        TopDocs hits2 = searcher2.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = searcher2.getDocument(scoreDoc);
            int parentId = Integer.parseInt(doc.getField("ParentId").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            if (QId_SAList.containsKey(parentId)) {
                if (without_minus_one_userId) {
                    if (userId != -1) {
                        for (String skill_area : QId_SAList.get(parentId)) {
                            int count = SA_AnswerCount.getOrDefault(skill_area, 0);
                            count++;
                            SA_AnswerCount.put(skill_area, count);
                        }
                    }
                } else {
                    for (String skill_area : QId_SAList.get(parentId)) {
                        int count = SA_AnswerCount.getOrDefault(skill_area, 0);
                        count++;
                        SA_AnswerCount.put(skill_area, count);
                    }
                }
            }
        }

        String file_name_question = "D_sa_questions_all.txt";
        String file_name_answer = "D_sa_answers_all.txt";
        if (without_minus_one_userId) {
            file_name_question = "D_sa_questions.txt";
            file_name_answer = "D_sa_answers.txt";
        }
        PrintWriter writer = new PrintWriter(ResultsPath + file_name_question);
        writer.println("skillArea,questionCount");
        for (Map.Entry<String, Integer> Item2 : SA_QuestionCount.entrySet()) {
            String skillArea = Item2.getKey();
            Integer questionCount = Item2.getValue();
            writer.println(skillArea + "," + questionCount);
        }
        writer.close();

        PrintWriter writer2 = new PrintWriter(ResultsPath + file_name_answer);
        writer2.println("skillArea,answerCount");
        for (Map.Entry<String, Integer> Item2 : SA_AnswerCount.entrySet()) {
            String skillArea = Item2.getKey();
            Integer answerCount = Item2.getValue();
            writer2.println(skillArea + "," + answerCount);
        }
        writer2.close();
    }

    private void document_score_sa(Path QuestionsIndexPath, Path answersIndexPath, String ResultsPath, boolean without_minus_one_userId) throws IOException, ParseException {
        SA_AnswerScore.clear();
        SA_QuestionScore.clear();

        Searcher searcher = new Searcher(QuestionsIndexPath, "Tags");
        TopDocs hits = searcher.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            int QId = Integer.parseInt(doc.getField("Id").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int score = Integer.parseInt(doc.getField("Score").stringValue());
            String[] tags = doc.getValues("Tags");
            ArrayList<String> skillAreaList = new ArrayList<>();
            for (String tag : tags) {
                if (tag_SkillArea.containsKey(tag)) {
                    String SA = tag_SkillArea.get(tag);
                    if (without_minus_one_userId) {
                        if (!skillAreaList.contains(SA)) {
                            skillAreaList.add(SA);
                        }
                    } else {
                        skillAreaList.add(SA);
                    }
                }
            }
            if (!skillAreaList.isEmpty()) {
                QId_SAList.put(QId, skillAreaList);

                if (without_minus_one_userId) {
                    if (userId != -1) {
                        for (String skill_area : skillAreaList) {
                            SA_QuestionScore.put(
                                    skill_area, SA_QuestionScore.getOrDefault(skill_area, 0) + score
                            );
                        }
                    }
                } else {
                    for (String skill_area : skillAreaList) {
                        SA_QuestionScore.put(
                                skill_area, SA_QuestionScore.getOrDefault(skill_area, 0) + score
                        );
                    }
                }
            }
        }

        Searcher searcher2 = new Searcher(answersIndexPath, "Id");
        TopDocs hits2 = searcher2.querySearch("*:*", Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = searcher2.getDocument(scoreDoc);
            int parentId = Integer.parseInt(doc.getField("ParentId").stringValue());
            int userId = Integer.parseInt(doc.getField("OwnerUserId").stringValue());
            int score = Integer.parseInt(doc.getField("Score").stringValue());
            if (QId_SAList.containsKey(parentId)) {
                if (without_minus_one_userId) {
                    if (userId != -1) {
                        for (String skill_area : QId_SAList.get(parentId)) {
                            SA_AnswerScore.put(
                                    skill_area, SA_QuestionScore.getOrDefault(skill_area, 0) + score
                            );
                        }
                    }
                } else {
                    for (String skill_area : QId_SAList.get(parentId)) {
                        SA_AnswerScore.put(
                                skill_area, SA_QuestionScore.getOrDefault(skill_area, 0) + score
                        );
                    }
                }
            }
        }

        String file_name_question = "Score_sa_questions_all.txt";
        String file_name_answer = "Score_sa_answers_all.txt";
        if (without_minus_one_userId) {
            file_name_question = "Score_sa_questions.txt";
            file_name_answer = "Score_sa_answers.txt";
        }
        PrintWriter writer = new PrintWriter(ResultsPath + file_name_question);
        writer.println("skillArea,questionScore");
        for (Map.Entry<String, Integer> Item2 : SA_QuestionScore.entrySet()) {
            String skillArea = Item2.getKey();
            Integer questionScore = Item2.getValue();
            writer.println(skillArea + "," + questionScore);
        }
        writer.close();

        PrintWriter writer2 = new PrintWriter(ResultsPath + file_name_answer);
        writer2.println("skillArea,answerScore");
        for (Map.Entry<String, Integer> Item2 : SA_AnswerScore.entrySet()) {
            String skillArea = Item2.getKey();
            Integer answerScore = Item2.getValue();
            writer2.println(skillArea + "," + answerScore);
        }
        writer2.close();
    }
}
