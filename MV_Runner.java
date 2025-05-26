import java.io.IOException;
import java.util.Random;

public class MV_Runner {

    public static void main(String[] args) throws IOException, InterruptedException {


        String schelling_MQadress_SS     = "schelling/avgQuantities_schellingAutoRD.multiquatex";
        String schelling_MQadress_trans     = "schelling/avgQuantities_schellingtransient.multiquatex";
        String boids_MQadress     = "boids/boid_flockers.multiquatex";

        String jarFilePath  = "multivesta.jar";
        String showPlots    = "-vp false ";
        String multiQuaTEx_schelling_trans  = "-f "+schelling_MQadress_trans+" ";
        String multiQuaTEx_schelling_SS  = "-f "+schelling_MQadress_SS+" ";
        String multiQuaTEx_boids  = "-f "+boids_MQadress+" ";
        String parallelism  = "-l 4 ";
        String seedOfSeeds  = "-sots 1 ";
        String significance = "-a 0.05 ";
        String stateDescriptor = "-sd vesta.python.simpy.SimPyState ";
        String blockSize    = "-bs 30 ";
        String delta1       = "-d1 0.5 ";

        String schelling_command_trans =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_schelling_trans + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir -1 -ms 300";
        String schelling_command_SS =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_schelling_SS + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir -1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String boid_command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx_boids + parallelism + seedOfSeeds + stateDescriptor + showPlots + blockSize + delta1 + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir 1 -ms 600";
        String formals = "java -jar";

        schelling_command_trans = formals + " " + jarFilePath + " " + schelling_command_trans;
        schelling_command_SS = formals + " " + jarFilePath + " " + schelling_command_SS;
        boid_command =   formals + " " + jarFilePath + " " + boid_command;

        Random rd = new Random();

        // BASELINE
        int baseParameter = 6;
        //int baseParameter = rd.nextInt(9);

        System.out.println("Base parameter set to " + String.valueOf(baseParameter));
        Model_Runner.update_Model(baseParameter);

        double[] baseline = Model_Runner.run_One_MV(schelling_command_SS)[0];

        Genetic_Search(baseline, baseParameter, schelling_command_SS);
        
    }


    private static void Genetic_Search(double[] baseline, int baseParameter, String command) throws InterruptedException, IOException{
        // init SEARCH
        
        Random rd = new Random();
        int parentGene = rd.nextInt(9);

        // Ensure rd doesn't give the same int again.
        while (parentGene == baseParameter) {
            parentGene = rd.nextInt(9);
        }

        parentGene = 2;

        System.out.println("Search starting at " + String.valueOf(parentGene));
        
        Model_Runner.update_Model(parentGene);

        double[] offspring;
        int offspringGene;
        double[] parent;
        int searchIteration = 0;


        parent = Model_Runner.run_One_MV(command)[0];

        // SEARCH
        while (true) { 

            System.out.println("Starting iteration: " + String.valueOf(searchIteration));

            offspringGene = mutate(parentGene, rd); 
            Model_Runner.update_Model(offspringGene);

            offspring = Model_Runner.run_One_MV(command)[0];

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

            System.out.println("offspring T-stat : " + Double.toString(fitNew));
            System.out.println("parent T-stat : " + Double.toString(fitOld));

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
            change = rd.nextInt(3) - 1;             
        }

        int offspring = parent + change;
        
        System.out.println("Offspring mutated by " + String.valueOf(change) + " resulting in parameter " + String.valueOf(offspring));
        return offspring;
    }

    //Assuming n = 30 and significance of 0.05
    //These parameters should not have bad formats. The Arguments should be shaped so the parameters can be åbne
    public static double test_result(double[] base, double[] model) {
        double dut = (base[0] - model[0])/ Math.sqrt((base[1] + model[1])/30);
        //System.out.println("Testing model evaluation resulted in t stat: " + String.valueOf(dut));
        return Math.abs(dut);
    }

    //Assuming 60 dof instead of 58, and significance of 0.05
    private static boolean accept_null(double t_stat) {
        return t_stat <= 2;
    }

}

