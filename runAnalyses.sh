##########
#PREAMBLE#
##########

# Before running any analysis, it is necessary to install the following python libraries:
# - The ones specified here: https://github.com/andrea-vandin/MultiVeStA/wiki/Integration-with-Python-model-simulator
# #python3 -m pip install numpy
# #python3 -m pip install matplotlib
# #python3 -m pip install py4j
# 
# - Mesa: 
# #%pip install mesa


#Now it is possible to run the MultiVeStA analyses presented in the paper. 
#For all analyses: open a terminal and navigate to this folder.

################################
#Analysis of Boids flocks model#
################################


#This command will run analyses for Figure 4 and 5 of the draft: transient analysis.
#	You shall change the python3 path to the correct one (here I am using a local virtual environment where I have installed the necessary libraries)
#
#File MV_boid_flockers_integrator.py contains the default parameter 0.03 for coherence (cohere=0.03, line 72). Change it to 0.09 and rerun the analysis to get the results for the right part of the figures.
                                                                                                                                                                                                        
java -jar multivesta.jar -c -m boids/MV_boid_flockers_integrator.py -sm true -f boids/boid_flockers.multiquatex -l 6 -sots 1 -sd vesta.python.simpy.SimPyState -vp true -bs 30 -d1 1 -a 0.05 -otherParams "/Users/William/AppData/Local/Programs/Python/Python313/python" -ir 1 -ms 600

#This command will run analyses for Figure 6: counterfactual analysis.
# Please change the names of the csv files with the ones obtained for the two experiments for cohere=0.03 and cohere=0.09

#java -jar multivesta.jar -pp -w mean-comparison -t transient -f1 MultiVeStA_OUTPUT/parametricMultiQuaTExExprResult_20240509_15-59-48_lowCoherence.csv  -f2 MultiVeStA_OUTPUT/parametricMultiQuaTExExprResult_20240509_16-40-16_highCoherence.csv  -fOut lowVSHighCoherence_avgdistancecentroid.csv -ds [1] -pl [1] -a 0.05



#########################################
#Analysis of Schelling segregation model#
#########################################

#This command will run the autoRD analysis in Section 6. This also prints the result of the normality tests on the horizontal means. You shall change the python3 path as discussed above

#java -jar multivesta.jar -c -m schelling/MV_python_integrator_schelling.py -sm true -f schelling/avgQuantities_schellingAutoRD.multiquatex -l 1 -sots 1 -sd vesta.python.simpy.SimPyState -vp true -bs 30 -d1 0.1 -a 0.05 -otherParams "/Users/William/AppData/Local/Programs/Python/Python313/python" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8

#This command will run the autoBM analysis in Section 6. You shall change the python3 path as discussed above

#java -jar multivesta.jar -c -m schelling/MV_python_integrator_schelling.py -sm true -f schelling/avgQuantities_schellingAutoBM.multiquatex -l 1 -sots 1 -sd vesta.python.simpy.SimPyState -vp true -bs 30 -d1 0.1 -a 0.05 -otherParams "/Users/andrea/Documents/python_envs/env_research/bin/python3" -ir 1 -mvad 7E-3 -wm 2 -pw 1 -nb 128 -ibs 8
