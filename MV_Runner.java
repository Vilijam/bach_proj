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


        String schelling_MQadress     = "schelling/avgQuantities_schellingAutoRD.multiquatex";
        String boids_MQadress     = "boids/boid_flockers.multiquatex";

        String jarFilePath  = "multivesta.jar";
        String showPlots    = "-vp false ";
        String multiQuaTEx_schelling  = "-f "+schelling_MQadress+" ";
        String multiQuaTEx_boids  = "-f "+boids_MQadress+" ";
        String parallelism  = "-l 1 ";
        String seedOfSeeds  = "-sots 1 ";
        String significance = "-a 0.05 ";
        String stateDescriptor = "-sd vesta.python.simpy.SimPyState ";
        String blockSize    = "-bs 30 ";
        String delta1       = "-d1 0.1 ";

        String schelling_command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_schelling + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String boid_command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_boids + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String formals = "java -jar";

        schelling_command = formals + " " + jarFilePath + " " + schelling_command;
        boid_command =   formals + " " + jarFilePath + " " + boid_command;


        
        // BASELINE
        Random rd = new Random();
        //int baseParameter = rd.nextInt(9);
        int baseParameter = 2;

        System.out.println("Base parameter set to " + String.valueOf(baseParameter));
        update_Model(baseParameter);
        float[] baseline = run_One_MV(schelling_command);

        Genetic_Search(baseline, baseParameter, schelling_command);

    }


    private static void Genetic_Search(float[] baseline, int baseParameter, String command) throws InterruptedException, IOException{
        // init SEARCH
        
        Random rd = new Random();
        int parentGene = rd.nextInt(9);

        // Ensure rd doesn't give the same int again.
        while (parentGene == baseParameter) {
            parentGene = rd.nextInt(9);
        }

        System.out.println("Search starting at " + String.valueOf(parentGene));
        
        update_Model(parentGene);

        float[] offspring;
        int offspringGene;
        float[] parent;
        int searchIteration = 0;


        parent = run_One_MV(command);

        // SEARCH
        while (true) { 

            System.out.println("Starting iteration: " + String.valueOf(searchIteration));

            offspringGene = mutate(parentGene, rd); 
            update_Model(offspringGene);

            offspring = run_One_MV(command);
            System.out.println("Offspring evaluation: " + Arrays.toString(offspring));

            // Check null hypothesis
            double offspringTStat = test_result(baseline, offspring); 
            if (accept_null(offspringTStat)) {
                searchIteration++; 
                System.out.println("Gene of " + String.valueOf(offspringGene));
                break; 
            }
            

            // Check if mutation has better fitness. If so, it becomes parent.
            double fitNew = offspringTStat;
            double fitOld = test_result(baseline, parent);

            if (fitNew < fitOld) { 
                
                System.out.println("Offspring won!");
                parent = offspring; 
                parentGene = offspringGene;
            
            } else {
                System.out.println("Parent won!");
            }
            

            searchIteration++;
        }
    }

    private static int mutate(int parent, Random rd) {
        
        // Ensuring offspring is different from parent
        // Ensuring gene € [0,8]
        int change = 0;
        while (change == 0 || 0 > parent + change || parent + change > 8) { 
            change = rd.nextInt(7) - 3;             
        }

        int offspring = parent + change;
        
        System.out.println("Offspring mutated by " + String.valueOf(change) + " resulting in parameter " + String.valueOf(offspring));
        return offspring;
    }

    //Assuming n = 30 and significance of 0.05
    private static double test_result(float[] base, float[] model) {
        double dut = (base[0] - model[0])/ Math.sqrt((base[1] + model[1])/30);
        System.out.println("Testing model evaluation resulted in t stat: " + String.valueOf(dut));
        return dut;
    }

    //Assuming 60 dof instead of 58, and significance of 0.05
    private static boolean accept_null(double t_stat) {
        return t_stat <= 2;
    }

    public static float[] run_One_MV(String command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                    
        Process p = pb.start();

        // Read the output of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        String result = "";
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
            if (line.equals(" steady-state value of MyProperty(avgHappy_Noise):")) {
                
                result = reader.readLine();
                //System.out.println(result);
                
            }
        }

        // Wait for the process to complete
        int exitCode = p.waitFor();
        System.out.println("Process exited with code: " + exitCode);

        return decipher_MV_Output(result);
    }
            
    public static float[] decipher_MV_Output(String input) {

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