import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class MV_Runner {

    public static void main(String[] args) throws IOException, InterruptedException {


        String MQadress     = "schelling/avgQuantities_schellingAutoRD.multiquatex";

        String jarFilePath  = "multivesta.jar";
        String showPlots    = "-vp false ";
        String multiQuaTEx  = "-f "+MQadress+" ";
        String parallelism  = "-l 1 ";
        String seed         = "-sots 1 ";
        String significance = "-a 0.05 ";

        String command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx + parallelism + seed + "-sd vesta.python.simpy.SimPyState " + showPlots  + "-bs 30 -d1 0.1 " + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String formals = "java -jar";

        command = formals + " " + jarFilePath + " " + command;

        // BASELINE

        Random rd = new Random();
        int baseParameter = rd.nextInt(9);

        System.out.println("Base parameter set to " + String.valueOf(baseParameter));
        update_Model(baseParameter);
        float[] baseline = run_One_MV(command);

        // init SEARCH
        int change = rd.nextInt(9);

        // Ensure rd doesn't give the same int again.
        while (change == baseParameter) {
            change = rd.nextInt(9);
        }

        System.out.println("Search starting at " + String.valueOf(change));
        
        update_Model(change);

        float[] offspring;
        float[] parent;
        int searchIteration = 0;


        parent = run_One_MV(command);
        // SEARCH
        while (true) { 

            System.out.println("Starting iteration: " + String.valueOf(searchIteration));

            update_Model(mutate(change, rd));

            offspring = run_One_MV(command);
            System.out.println("Offspring evaluation: " + Arrays.toString(offspring));

            // Test if new mean value is within CI of baseline CI
            if (test_result(baseline, offspring)) {searchIteration++; break; }

            
            // Check if mutation has better fitness. If so, it becomes parent.
            if (searchIteration != 0) {
                float fitNew = fitness_function(baseline, offspring);
                float fitOld = fitness_function(baseline, parent);

                if (fitNew > fitOld) { 
                    
                    System.out.println("Offspring won!");
                    parent = offspring; 
                
                } else {
                    System.out.println("Parent won!");
                }
            }

            searchIteration++;
        }

        // Create population
        
        // Run MV
        // Calculate fitness of population
        // Cull and multiply
        // Mutate
        // Repeat


    }

    private static int mutate(int parent, Random rd) {
        int change = rd.nextInt(7) - 3;
        int offspring = parent + change;
        System.out.println("Offspring mutated by " + String.valueOf(change) + " resulting in parameter " + String.valueOf(offspring));
        return offspring;
    }

    private static float fitness_function(float[] target, float[] geneExpression) {
        
        return target[0] - geneExpression[0]; 
    }

    private static boolean  test_result(float[] base, float[] model) {
        return base[0] - base[2] < model[0] && model[0] < base[0] + base[2];
    }

    private static float[] run_One_MV(String command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                    

        Process p = pb.start();

        // Read the output of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        String result = "";
        while ((line = reader.readLine()) != null) {
            if (line.equals(" steady-state value of MyProperty(avgHappy_Noise):")) {
                
                result = reader.readLine();
            }
        }

        // Wait for the process to complete
        int exitCode = p.waitFor();
        System.out.println("Process exited with code: " + exitCode);

        return decipher_MV_Output(result);
    }
            
    private static float[] decipher_MV_Output(String input) {

        String[] vals = input.split(" ");
        
        float[] result = new float[3];  
        
        // Mean
        result[0] = Float.parseFloat(vals[0]);
        // Variance
        result[1] = Float.parseFloat(vals[2].replace(",", ""));
        // CI/2
        result[2] = Float.parseFloat(vals[4].replace(",", ""));
        
        return result;

    }

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
}