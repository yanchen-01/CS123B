import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ChangeNames {
    private static Map<String, String> dict;
    public static void main(String[] args) throws IOException {
        // Get dictionary to get the common name of the species
        dict = getDict();
        changeName("basic_data_set.fasta");
        changeName("related_data_set.fasta");
    }

    private static void changeName(String filename) throws IOException {
        String new_filename = filename.replace("fasta", "fa");
        BufferedReader br =
                new BufferedReader(new FileReader(filename));
        BufferedWriter bw =
                new BufferedWriter(new FileWriter(new_filename));

        String line;
        while ((line = br.readLine()) != null) {
            // Change the name of the sequence to the common name of the species
            if (line.contains(">")) {
                line = line.replace(">", "");
                String[] values = line.split(" ");
                line = ">" + dict.get(values[0]);
            }
            // write to the new file
            bw.write(line + "\n");
        }

        br.close();
        bw.close();

        System.out.println(filename + " replaced successfully.");
    }

    private static Map<String, String> getDict() throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("TNF_orthologs.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String key = values[0];
                String value = values[1].equals("") ?
                        values[2] : values[1];
                value = value.replace(" ", "_");
                map.put(key, value);
            }
        }
        return map;
    }
}