import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Model_Runner {
    
    public static void update_Model(int change) {
    try {
        File inputFile = new File("schelling/schelling_model.py");
        File tempFile = new File("schelling/temp.txt");
        
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        
        writer.write("x = " + String.valueOf(change));
        writer.newLine();
        
        reader.readLine();
        
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }
        
        writer.close();
        reader.close();
        
        if (inputFile.delete()) {
            tempFile.renameTo(inputFile);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

    public static void update_Model_Multiparam(double change1, double change2) {
    try {
        File inputFile = new File("schelling/schelling_model.py");
        File tempFile = new File("schelling/temp.txt");
        
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        
        writer.write("x, y = " + String.valueOf(change1) + ", "+ String.valueOf(change2));
        writer.newLine();
        
        reader.readLine();
        
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }
        
        writer.close();
        reader.close();
        
        if (inputFile.delete()) {
            tempFile.renameTo(inputFile);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

    public static double[][] run_One_MV(String command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                    
        Process p = pb.start();

        // Read the output of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        double[][] results = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.equals(" steady-state value of MyProperty(avgHappy_Noise):")) {
                
                results = new double[1][3];
                results[0] = decipher_MV_Output_SS(reader.readLine());
                

            } else if (line.equals("Results:")) {

                boolean MV_printing_results = true;
                ArrayList<double[]> resultss = new ArrayList<>();
                String[] lines;

                while (MV_printing_results) {
                    line = reader.readLine();

                    line = line.strip();
                    lines = line.split(" "); 
                    if (lines[0].equals("result")) {

                        resultss.add(decipher_MV_Output_trans(lines));

                    } else {
                        MV_printing_results = false;
                    }
                }

                results = new double[resultss.size()][3];
                for (int i = 0; i < resultss.size(); i++) {
                    results[i] = resultss.get(i);
                }
            }
        }

        // Wait for the process to complete
        int exitCode = p.waitFor();
        System.out.println("Process exited with code: " + exitCode);

        return results;
    }

    public static double[] decipher_MV_Output_trans(String[] input) {
 
        double[] result = new double[3];  

        // Mean
        result[0] = Double.parseDouble(input[2]);
        // Variance
        result[1] = Double.parseDouble(input[4].replace(",", ""));
        // CI/2
        result[2] = Double.parseDouble(input[6].replace(",", ""));
        
        return result;

    }
    
    public static double[] decipher_MV_Output_SS(String input) {

        String[] vals = input.split(" ");
        
        double[] result = new double[3];  
        
        // Mean
        result[0] = Double.parseDouble(vals[0]);
        // Variance
        result[1] = Double.parseDouble(vals[2].replace(",", ""));
        // CI/2
        result[2] = Double.parseDouble(vals[4].replace(",", ""));
        
        return result;

    }

}