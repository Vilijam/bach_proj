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
        String parallelism  = "-l 2 ";
        String seedOfSeeds  = "-sots 1 ";
        String significance = "-a 0.05 ";
        String stateDescriptor = "-sd vesta.python.simpy.SimPyState ";
        String blockSize    = "-bs 30 ";
        String delta1       = "-d1 0.1 ";


 

        String schelling_command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_schelling + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir -1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String boid_command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_boids + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python homophily 3 \" -ir 1 -ms 600";
        String formals = "java -jar";

        schelling_command = formals + " " + jarFilePath + " " + schelling_command;
        boid_command =   formals + " " + jarFilePath + " " + boid_command;

        Random rd = new Random();


        /*
         // BASELINE
         int baseParameter = rd.nextInt(9);

         System.out.println("Base parameter set to " + String.valueOf(baseParameter));
         update_Model(baseParameter);
         float[] baseline = run_One_MV(schelling_command);
 
         Genetic_Search(baseline, baseParameter, schelling_command);

         */

        
        int m = 8 ; // objectives
        int N = 10; // Amount of sub-problems and pop-size
        int T = 3;  // neighboors
        double[] target = new double[m]; 


        double[][] gamer = generate_weight_vectors(rd, N, m);

        for (int i = 0; i < N; i++) {

            System.out.println(Arrays.toString(gamer[i]));
            
        }

        int[][] nbh = find_weight_vector_neighbs(T, gamer);

        double[] poop = {22, 0.5, 1000};
        double[][] pop = generate_population(N, poop, rd);

        System.out.println(Arrays.toString(poop));            

        for (double[] pop1 : pop) {
            System.out.println(Arrays.toString(pop1));
        } 

    }

    private static double[][] moead_loop(Random rd, int N, int m, int T, int[][] nbh, double[][] pop) {
        double[][] external_pop = new double[N][m];

        for (int i = 0; i < N; i++) {
            int[] nbs = nbh[0];
            int first_parent = rd.nextInt(nbs.length);
            int second_parent = rd.nextInt(nbs.length);
            while (second_parent == first_parent) {second_parent = rd.nextInt(nbs.length);}

            double[] child = genetic_operation(pop[nbs[first_parent]], pop[nbs[second_parent]], rd);


            // repair child?

            // eval child
            
            // update z


            




            

        }

        return external_pop;
    }

    private static double[] genetic_operation(double[] x1, double[] x2, Random rd) {

        double mutation_rate = 0.05;
        int crossover_point = rd.nextInt(x1.length);

        double[] child = new double[x1.length];
        
        // Crossover of the two parents
        for (int i = 0; i < x1.length; i++) {
            if (i < crossover_point) {
                child[i] = x1[i];
            } else {
                child[i] = x2[i];
            } 
        }
        // Bullshit mutation
        for (int i = 0; i < child.length; i++) {
            if (rd.nextDouble() < mutation_rate) {
                
                child[i] = child[i] * 1.1;         
            }   
        }
        return child;
    }

    private static double[][] generate_population(int popCount, double[] ancestor, Random rd) {

        // mutations to generate a population from an ancestor
        double[] mutations = { 1.2,0.75, 1.5,0.5, 1.8,0.2, 1.1,0.9, 1.3,0.7};

        double[][] population = new double[popCount][ancestor.length];
        population[0] = ancestor;

        for (int i = 1; i < popCount; i++) {

            // 50% to mutate any gene of the ancestor
            double[] next_child = ancestor.clone();
            for (int j = 0; j < ancestor.length; j++) {

                if (rd.nextInt(2) == 0) {
                    next_child[j] = next_child[j] * mutations[rd.nextInt(10)];
                }
            }

            population[i] = next_child;
        }

        return population;

    }

    // Assuming one and two lenght =
    private static double euclidean_distance(double[] one, double[] two) {

        double sum = 0;
        for (int i = 0; i < one.length; i++) {
            sum = Math.pow((one[i] - two[i]), 2);
        }

        return Math.sqrt(sum);
    }

    // Calculating the B function. Vector Neighboorhood
    private static int[][] find_weight_vector_neighbs(int T, double[][] weightVectors) {

        // B(i). 1st level is all vectors. 2nd is the T closest neighboors' index in weighVectors
        int[][] neighboorArray = new int[weightVectors.length][T];


        // GO through all vectors
        for (int i = 0; i < weightVectors.length; i++) {
            

            // Find ith vectors distance to all others. -- replicates work dist(a,b) = dist(b,a). Distance matrix?
            double[] distances = new double[weightVectors.length];
            for (int j = 0; j < weightVectors.length; j++) {
                distances[j] = euclidean_distance(weightVectors[i], weightVectors[j]);
            }

            
            // the T lowest ideces
            int[] indeces_of_T_lowest_dists = new int[T];

            // init'ing closest neighboors array
            for (int j = 0; j < T; j++) {
                indeces_of_T_lowest_dists[j] = j;
            }

            // finding closest neighboors. Assuming T is larger than count of WV
            for (int j = T; j < distances.length; j++) {
                
                // find the worst neighboor in the neighboorhood. --replicate work. When curr WV is further, the furthest neighboor does not change.
                int furthestNeigh = 0;
                for (int l = 1; l < T; l++) {
                    if (distances[indeces_of_T_lowest_dists[furthestNeigh]] < distances[indeces_of_T_lowest_dists[l]]) {
                        furthestNeigh = l;
                    } 
                }

                // Update nbh if curr wv is closer
                if (distances[j] < distances[indeces_of_T_lowest_dists[furthestNeigh]]) {
                    indeces_of_T_lowest_dists[furthestNeigh] = j;
                }
            }
            
            // append nbh is nbh array
            neighboorArray[i] = indeces_of_T_lowest_dists;

        }

        return neighboorArray;
    }

    private static double[][] generate_weight_vectors(Random rd, int vCount, int oCount) {

        double[][] weightVectors = new double[vCount][oCount];
        
        for (int i = 0; i < vCount; i++) {
            

            double[] steps = new double[oCount];
            for (int j = 0; j < oCount-1; j++) {
                steps[j] = rd.nextFloat();
            }

            steps[oCount-1] = 1;

            Arrays.sort(steps);
        
            double floor = 0;
            for (int j = 0; j < oCount; j++) {
                
                weightVectors[i][j] = steps[j] - floor;

                floor = steps[j];
            }
        }

        return weightVectors;
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
    //These parameters should not have bad formats. The Arguments should be shaped so the parameters can be åbne
    public static double test_result(float[] base, float[] model) {
        double dut = (base[0] - model[0])/ Math.sqrt((base[1] + model[1])/30);
        //System.out.println("Testing model evaluation resulted in t stat: " + String.valueOf(dut));
        return Math.abs(dut);
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