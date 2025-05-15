x, y = 4.0, 0.13499999999999998

import mesa
import random
import math


"""
Mesa Schelling Segregation Model

Adapted from: https://github.com/projectmesa/mesa-examples/blob/main/examples/schelling/model.py

"""

class SchellingAgent(mesa.Agent):
    """
    Schelling segregation agent
    """

    def __init__(self, pos, model, agent_type):
        """
        Create a new Schelling agent.

        Args:
           unique_id: Unique identifier for the agent.
           pos: Agent initial location.
           agent_type: Indicator for the agent's type (minority=1, majority=0)
        """
        super().__init__(model)
        self.pos = pos
        self.type = agent_type

    def step(self):
        similar = 0
        
        #for neighbor in self.model.grid.iter_neighbors(self.pos, moore=True, radius=self.model.radius):
        for neighbor in self.model.grid.iter_neighbors(self.pos, True):
            if neighbor.type == self.type:
                similar += 1
        
        # If unhappy, move:
        if similar < self.model.homophily:
            self.model.grid.move_to_empty(self)
        else:
            self.model.happy += 1


class Schelling(mesa.Model):
    """
    Model class for the Schelling segregation model.
    """

    def __init__(self, width=20, height=20, density=0.8, minority_pc=0.2, homophily=x):
        super().__init__()
        self.width = width
        self.height = height
        self.homophily = homophily
        self.density = density
        self.minority_pc = minority_pc

        #self.grid = mesa.space.SingleGrid(width, height, torus=True)
        
        self.happy = 0

    def set_simulator_for_new_simulation(self, seed):
        random.seed(seed)
        self.random.seed(seed)
        self.reset_randomizer(seed)

        if(len(self.agents)) > 0:
            for agent in self.agents:
                self.grid.remove_agent(agent)
            self.remove_all_agents()
        self.grid = mesa.space.SingleGrid(self.width, self.height, torus=True)

        self.happy = 0

        # Set up agents
        # We use a grid iterator that returns
        # the coordinates of a cell as well as
        # its contents. (coord_iter)
        for _, pos in self.grid.coord_iter():
            if self.random.random() < self.density:
                agent_type = 1 if self.random.random() < self.minority_pc else 0
                agent = SchellingAgent(pos, self, agent_type)
                self.grid.remove_agent(agent)
                self.grid.place_agent(agent, pos)

    def eval(self, obs):
        """
        Evaluate method.
        """
        if obs == 'avgHappy_Noise':
            avg=float(self.happy / len(self.agents))
            avg=avg + random.normalvariate(0,1)
            return avg
        else:
            return 0.0
        
    def step(self):
        """
        Run one step of the model.
        """
        self.happy = 0  # Reset counter of happy agents
        self.agents.shuffle_do("step")
        #not needed with MV
        #self.datacollector.collect(self)

        if self.happy == len(self.agents):
            self.running = False
        
    def getTime(self):
        return self.steps   
    
    def updateParameters(self, change):
        self.homophily = change
