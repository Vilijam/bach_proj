
import java.util.Arrays;

public class EP_Member {
    public double[] parameters;
    public double[] fitness;

    public EP_Member (double[] p, double[] f) {
        this.parameters = p;
        this.fitness = f;
    }

    public void print_state() {
        System.out.println("Parameters: " + Arrays.toString(parameters));
        System.out.println("Fitness: " + Arrays.toString(fitness));
    }
}