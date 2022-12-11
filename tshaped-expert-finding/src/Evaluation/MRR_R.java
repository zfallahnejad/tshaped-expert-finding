package Evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashSet;

public class MRR_R {
    private HashSet<String> SAList = new HashSet<>();

    public MRR_R(String approachPath, String tagsOfSkillAreaPath, int rank) throws IOException {
        ReadClusteringFile(tagsOfSkillAreaPath);
        calculateAndPrintMRR(approachPath, rank);
    }

    private void ReadClusteringFile(String tagsOfSkillAreaPath) throws IOException {
        LineNumberReader reader = new LineNumberReader(new FileReader(new File(tagsOfSkillAreaPath)));
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (!line.equals("SkillArea,Tag")) {
                String skillArea = line.split(",")[0];
                if (!SAList.contains(skillArea))
                    SAList.add(skillArea);
            }
        }
        reader.close();
    }

    private void calculateAndPrintMRR(String approachPath, int rank) throws IOException {
        PrintWriter writer = new PrintWriter(approachPath + "MRR_" + rank + ".csv");
        writer.println("skillArea,MRR");
        double sum = 0;
        for (String skillArea : SAList) {
            double MRR = calculateMRR(skillArea, approachPath, rank);
            sum += MRR;
            writer.println(skillArea + "," + MRR);
        }
        double avg = sum / (double) SAList.size();
        writer.println("All," + avg);
        writer.close();
        SAList.clear();
    }

    private double calculateMRR(String skillArea, String approachPath, int rank) throws IOException {
        String resultPath = approachPath + skillArea + "Users.csv";
        LineNumberReader reader = new LineNumberReader(new FileReader(new File(resultPath)));
        String line = "";
        double MRR = 0;
        int count = 1;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("UserId,Shape"))
                continue;
            String shape = line.split(",")[1];
            // System.out.println(rank + "->" + count + "->" + skillArea + "->" + line.trim() + "->" + shape);
            if (shape.equals("T")) {
                MRR = 1.0 / (double) count;
                break;
            }
            if (count == rank)
                break;
            count++;
        }
        reader.close();
        return MRR;
    }

}
