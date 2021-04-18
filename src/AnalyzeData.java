import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnalyzeData {

    private static final Map<String, Double> training = new HashMap<>();
    private static final Map<String, Double> test_same = new HashMap<>();
    private static final Map<String, Double> test_diff = new HashMap<>();
    private static final String B = "basic_data";
    private static final String R = "related_data";
    private static int length;
    private static double min;
    private static double max;

    public static void main(String[] args) throws IOException {
        System.out.println("------Training HMM using basic data------");
        // Print the average length before alignment
        printLength(B, "before", (int)getAverageLength(B));
        String output = getOutput(B);
        setMaps(output);
        // Print the length after alignment
        printLength(B, "after", length);
        printResult(B);

        System.out.println("\n=================\n");
        System.out.println("------Training HMM using related data------");
        printLength(R, "before", (int)getAverageLength(R));
        output = getOutput(R);
        setMaps(output);
        printLength(R, "after", length);
        printResult(R);
    }

    private static double getAverageLength(String filename) throws IOException {
        // Name of the raw data file ends with _set
        filename = filename + "_set.fa";

        int total_length = 0;
        int num_of_sequences = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(">")) {
                    num_of_sequences++;
                } else {
                    total_length += line.length();
                }
            }
        }
        return 1.0 * total_length / num_of_sequences;
    }

    private static void printLength(String data, String status, int length) {
        String avg = status.equals("after")? "":" average";
        System.out.printf("The%s length of %s %s alignment is %d\n",
                avg, data, status, length);
    }

    private static String getOutput(String filename) throws IOException {
        // Name of the training data file ends with _training
        filename = filename + "_training.fa";
        // Execute CS123B hmm in a separate process
        String command = "java -jar hmm2021.jar " + filename;
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command);

        // Store the output in a Buffered reader
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        // Retrieve the output from Buffered reader and return as a string
        String s, result = "";
        while ((s = stdInput.readLine()) != null) {
           // System.out.println(s);
            result = result.concat(s + "\n");
        }
        return result;
    }

    private static void setMaps(String content) {
        // Clear all maps
        training.clear();
        test_same.clear();
        test_diff.clear();

        String[] lines = content.split("\n");
        // Get the key and value from the content
        // Every 2 lines represents a sequence
        for (int i = 0; i < lines.length; i += 2) {
            if (!content.startsWith("*") && i == 0) i = 1;
            String key = lines[i];
            key = key.replace("*******  >", "");

            // Also get the length of the aligned sequence
            if((i == 0 || i == 1) && lines[i].contains("=")){
                String l = lines[i].substring(lines[i].indexOf("=") + 1);
                length = Integer.parseInt(l);
            }
            String valueString = lines[i + 1];
            valueString = valueString.replace(" .. score = ", "");

            // The key (the name of the sequence) is the first string before a space
            key = key.split(" ")[0];
            // After deleting .. score =, rest of the line is the value (score the the sequence)
            double value = Double.parseDouble(valueString);

            // First 40 lines are for the training set
            // (20 sequences * 2 line/sequence)
            if (i < 40) {
                training.put(key, value);
            }
            // The next 10 lines are for testing from the same set
            else if (i < 50) {
                test_same.put(key, value);
            }
            // The final 10 lines are for testing from the different set
            else {
                test_diff.put(key, value);
            }
        }
    }

    private static double getMax(Collection<Double> scores) {
        double result = Double.NEGATIVE_INFINITY;
        for (double s : scores) {
            if (s >= result) {
                result = s;
            }
        }
        return result;
    }

    private static double getMin(Collection<Double> scores) {
        double result = Double.POSITIVE_INFINITY;
        for (double s : scores) {
            if (s <= result) {
                result = s;
            }
        }
        return result;
    }

    private static double getAverage(Collection<Double> scores) {
        double result = 0.0;
        for (double s : scores) {
            result += s;
        }
        return result / scores.size();
    }

    private static void printResult(String data_name) {
        Collection<Double> values = training.values();
        max = getMax(values);
        min = getMin(values);
        double average = getAverage(values);

        // Print a warning if the scores seems very small
        if (Math.abs(average) < 0.001) {
            System.out.println("WARNING: you may forgot to check Log-odds");
        }

        // Print the max, min and average score of the training set
        print(data_name, "max", max);
        print(data_name, "min", min);
        print(data_name, "average", average);

        // Print the result of the testing set
        print(data_name, B);
        print(data_name, R);
    }

    private static void print(String data_name, String type, double value) {
        System.out.printf("\tThe %s score of %s training data is: %.2f\n",
                type, data_name, value);
    }

    private static void print(String train, String test) {
        System.out.printf("\n\t***Use %s trained HMM to score %s***\n",
                train, test);
        // Print the scores based on if the train and test sequences
        // are from the same data set or not
        if (train.equals(test)) {
            printScores(test_same, "same");
        } else {
            printScores(test_diff, "diff");
        }
    }

    private static void printScores(Map<String, Double> map, String type) {
        int correct = 0;

        for (String name : map.keySet()) {
            double score = map.get(name);
            // Print score for each sequence
            System.out.printf("\tScore for %s : %.2f", name, score);

            // Check if the result is as expected
            boolean inRange = score >= min && score <= max;
            String result = inRange ? "" : " doesn't";
            boolean expected = type.equals("same") == inRange;

            if (expected) {
                correct++;
                System.out.printf("\n\t\t> Expected: Score%s fall into the range.\n", result);
            } else {
                System.out.printf("\n\t\t> NOT expected: Score%s fall into the range.\n", result);
            }
        }

        // Print the number of results that are expected
        System.out.printf("\t%d out of 5 (%.0f%%) sequences got the score as expected\n",
                correct, correct * 100.0 / 5);

    }
}