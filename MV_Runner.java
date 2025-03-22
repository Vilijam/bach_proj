import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MV_Runner {

    public static void main(String[] args) throws IOException, InterruptedException {

        String jarFilePath  = "multivesta.jar";
        String showPlots    = "-vp false ";
        String multiQuaTEx  = "-f schelling/avgQuantities_schellingAutoRD.multiquatex ";
        String parallelism  = "-l 1 ";
        String seed         = "-sots 1 ";
        String significance = "-a 0.05 ";

        String command =  "-c -m schelling/MV_python_integrator_schelling.py -sm true " + multiQuaTEx + parallelism + seed + "-sd vesta.python.simpy.SimPyState " + showPlots  + "-bs 30 -d1 0.1 " + significance + "-otherParams \"/Users/William/AppData/Local/Programs/Python/Python313/python\" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8";
        String formals = "java -jar";

        command = formals + " " + jarFilePath + " " + command;

        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                    
        pb.redirectErrorStream(true);

        Process p = pb.start();

        // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String result = "";
            while ((line = reader.readLine()) != null) {
                if (line.equals(" steady-state value of MyProperty(avgHappy_Noise):")) {
                    result = reader.readLine();
                }
                System.out.println(line);
            }

            // Wait for the process to complete
            int exitCode = p.waitFor();
            System.out.println("Process exited with code: " + exitCode);
            String[] vals = result.split(" ");
            
            float value = Float.parseFloat(vals[0]);
            float var = Float.parseFloat(vals[2].replace(",", ""));
            float CIhalf = Float.parseFloat(vals[4].replace(",", ""));
 
            System.out.println("mean of " + value + " with var of " + var + " and CI/2 of " + CIhalf);
            
        }
  }