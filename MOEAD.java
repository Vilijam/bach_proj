import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class MOEAD {
    int m;  // Count of dimensions in objective space
    int N;  // Count of subproblem and population
    int T;
    Random rd;
    double[][] target;        // Point in objective space. Means of true sample. Also contains var and CI, but it should prob be removed
    String command;         // MultiVeStA command
    double[][] pop;         // count of pop X count of parameters
    double[][] wvs;         // count of pop X count of objectives
    int nbh[][];            // count of pop X count of neighboors. Contains indeces for other matrices
    double[][] pop_evals;   // count of pop X count of objectives
    ArrayList<EP_Member> external_pop;// count of non-dominated solutions/parameter pairings
    
    long initTime;
    ArrayList<Long> laptimes;     

    public MOEAD(Random rd, int m, int N, int T, double[][] target, String command, double[] ancestor) throws InterruptedException, IOException {
        this.rd = rd;
        this.m = m;
        this.N = N;
        this.T = T;
        this.target = target;
        this.command = command;
        this.laptimes = new ArrayList<>();

        this.initTime = System.currentTimeMillis();

        System.out.println("Generating Population");
        this.pop = generate_population(ancestor);

        System.out.println("Generating Weight Vectors");
        this.wvs = generate_weight_vectors();

        System.out.println("Initializing neighborhood");
        this.nbh = find_weight_vector_neighbs();

        System.out.println("Evaluating Population");
        this.pop_evals = evaluate_population();

        this.external_pop = new ArrayList<>();

        this.initTime = (System.currentTimeMillis() - initTime);

    }

    public ArrayList<EP_Member> run_algo(int max_runs) throws InterruptedException, IOException {

        long startLap = System.currentTimeMillis();
        for (int i = 0; i < max_runs; i++) {
            System.out.println("Starting Loop: " + Integer.toString(i));

            System.out.println("Printing population parameters: ");
            for (double[] elem : pop) {
                System.out.println(Arrays.toString(elem));
            }

            System.out.println("Printing population evaluations: ");
            for (double[] elem : pop_evals) {
                System.out.println(Arrays.toString(elem));
            }

            System.out.println("Printing external population: ");
            for (int j = 0; j < external_pop.size(); j++) {
                external_pop.get(j).print_state();
            }


            if (moead_loop() == 1) {break;}
        }

        laptimes.add(System.currentTimeMillis() - startLap);

        return external_pop;
    }

    private int moead_loop() throws InterruptedException, IOException {

    // Loop over all solutions/subproblem
    for (int i = 0; i < N; i++) {

        System.out.println("evaluating child " + Integer.toString(i));

        // get neighboors
        int[] nbs = nbh[i];

        // select two neighboor at random.
        int first_parent = rd.nextInt(nbs.length);
        int second_parent = rd.nextInt(nbs.length);
        while (second_parent == first_parent) {second_parent = rd.nextInt(nbs.length);}

        // mate
        double[] child = genetic_operation(pop[nbs[first_parent]], pop[nbs[second_parent]]);

        System.out.println("child mutated to: " + Arrays.toString(child));

        // update and evaluate
        Model_Runner.update_Model_Multiparam(child[0], child[1]);
        double[][] child_evaluation = Model_Runner.run_One_MV(command);

        System.out.println("child " + Integer.toString(i) + " evaluated");

        // calculate fitness of child
        double[] solution_fitness = new double[m];
        for (int j = 0; j < m; j++) {
            solution_fitness[j] = Math.abs(child_evaluation[j][0] - target[j][0]);
        }

        System.out.println("child's fitness " + Arrays.toString(solution_fitness));

        // replace all worse neighboors with child. 
        for (int j = 0; j < nbh[i].length; j++) {
            int index_of_neighboor = nbh[i][j];
            double wSum_of_child = weighted_sum(wvs[index_of_neighboor], solution_fitness);
            double wSum_neighboor = weighted_sum(wvs[index_of_neighboor], pop_evals[nbh[i][j]]);
            if (wSum_neighboor <= wSum_of_child) {
                pop[nbh[i][j]] = child;
                pop_evals[nbh[i][j]] = solution_fitness;
                
            }
        }

        update_external_population(solution_fitness, child);

        if (check_CI(solution_fitness) && external_pop.size() > 2) {
            System.out.println("BREAKING");
            return 1;
        }
    }

    return 0;
    }

    private void update_external_population(double[] solution_fitness, double[] child) {
        boolean solution_not_dominated = true;
        for (int i = external_pop.size()-1; i >= 0 ; i--) {
            
            // is solution dominated by a EP-member
            if (does_X_dom_Y(external_pop.get(i).fitness, solution_fitness)) {
                solution_not_dominated = false;
            }

            // remove the dominated solutions
            if (does_X_dom_Y(solution_fitness, external_pop.get(i).fitness)) {
                external_pop.remove(i);
            }
            
        }

        if (solution_not_dominated) { external_pop.add(new EP_Member(child, solution_fitness)); }
    }

    // Lower values dominate is better
    private boolean does_X_dom_Y(double[] X, double[] Y) {
        boolean proven_unequal = false;
        for (int i = 0; i < X.length; i++) {
            if (X[i] > Y[i]) {
                return false;
            } else if (X[i] != Y[i]) {
                proven_unequal = true;
            }
        }
        
        // Redundant but expressive
        if (proven_unequal) {
            return true;
            
        } else {
            return false;
        }
    }

    private double[][] generate_population(double[] ancestor) {

        // mutations to generate a population from an ancestor
        double[] mutations = { 1.2,0.75, 1.5,0.5, 1.8,0.2, 1.1,0.9, 1.3,0.7, 2, 0.3};

        double[][] population = new double[N][ancestor.length];
        population[0] = ancestor;

        for (int i = 1; i < N; i++) {

            // 50% to mutate any gene of the ancestor
            double[] next_child = ancestor.clone();
            for (int j = 0; j < ancestor.length; j++) {

                if (rd.nextInt(2) == 0) {
                    if (j == 0) {
                        next_child[j] = Math.min(8.0, Math.round(next_child[j] * mutations[rd.nextInt(12)]));
                    } else {
                        next_child[j] = Math.min(Math.max(next_child[j] * mutations[rd.nextInt(12)], 0), 1.0);
                    }
                }
            }

            population[i] = next_child;
        }

        return population;

    }

    private double[][] generate_weight_vectors() {

        double[][] weightVectors = new double[N][m];
        
        for (int i = 0; i < N; i++) {
            

            double[] steps = new double[m];
            for (int j = 0; j < m-1; j++) {
                steps[j] = rd.nextFloat();
            }

            steps[m-1] = 1;

            Arrays.sort(steps);
        
            double floor = 0;
            for (int j = 0; j < m; j++) {
                
                weightVectors[i][j] = steps[j] - floor;

                floor = steps[j];
            }
        }

        return weightVectors;
    } 

    // Calculating the B function. Vector Neighboorhood
    private int[][] find_weight_vector_neighbs() {

        // B(i). 1st level is all vectors. 2nd is the T closest neighboors' index in weighVectors
        int[][] neighboorArray = new int[wvs.length][T];


        // GO through all vectors
        for (int i = 0; i < wvs.length; i++) {
            

            // Find ith vectors distance to all others. -- replicates work dist(a,b) = dist(b,a). Distance matrix?
            double[] distances = new double[wvs.length];
            for (int j = 0; j < wvs.length; j++) {
                distances[j] = euclidean_distance(wvs[i], wvs[j]);
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

    // Assuming one and two lenght =
    private static double euclidean_distance(double[] one, double[] two) {

        double sum = 0;
        for (int i = 0; i < one.length; i++) {
            sum = Math.pow((one[i] - two[i]), 2);
        }

        return Math.sqrt(sum);
    }

    private double[][] evaluate_population() throws InterruptedException, IOException {
        double[][] populaltion_evaluation = new double[N][m];
        
        for (int i = 0; i < pop.length; i++) {
            Model_Runner.update_Model_Multiparam(pop[i][0], pop[i][1]);
            double[][] child_evaluation = Model_Runner.run_One_MV(command);

            double[] solution_fitness = new double[m];

            for (int j = 0; j < m; j++) {
                solution_fitness[j] = Math.abs(child_evaluation[j][0] - target[j][0]);
            }

            populaltion_evaluation[i] = solution_fitness;

            System.out.println("pop " + Integer.toString(i) +  " evaluated");
        }

        return populaltion_evaluation;
    }

    private static double weighted_sum(double[] weight, double[] solution) {
        double sum = 0.0;
        for (int i = 0; i < weight.length; i++) {
            sum += weight[i] * solution[i];
        }
        return sum;
    }

    // check is solutions distance to target mean is within CI
    private boolean check_CI(double[] solution_fitness) {
        System.out.println();

        for (int i = 0; i < m; i++) {
            if (solution_fitness[i] > target[i][1]) {
                // solution is outside CI
                return false;
            } 
        }
        // solution is not outside CI
        return true;

    }

    private double[] genetic_operation(double[] x1, double[] x2) {

        double[] mutations = { 1.2,0.75, 1.5,0.5, 1.8,0.2, 1.1,0.9, 1.3,0.7, 2,0.3};

        double mutation_rate = 0.10;
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
        // Bullshit mutation. First gene should be integer

        for (int j = 0; j < child.length; j++) {

            if (rd.nextInt(2) == 0) {
                if (j == 0) {
                    child[j] = Math.min(8.0, Math.round(child[j] * mutations[rd.nextInt(12)]));
                } else {
                    child[j] = Math.min(Math.max(child[j] * mutations[rd.nextInt(12)], 0), 1.0);
                }
            }
        }

        return child;
    }

}
