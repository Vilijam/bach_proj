#The following libraries shall be installed 
#%pip install mesa --quiet
import mesa
import random
import math
import numpy as np

"""
Flockers
=============================================================
A Mesa implementation of Craig Reynolds's Boids flocker model.
Uses numpy arrays to represent vectors.
https://github.com/projectmesa/mesa-examples/blob/main/examples/boid_flockers/boid_flockers/model.py
"""


from mesa.experimental.continuous_space import ContinuousSpaceAgent
from mesa.experimental.continuous_space import ContinuousSpace

"""A Boid (bird-oid) agent for implementing Craig Reynolds's Boids flocking model.

This implementation uses numpy arrays to represent vectors for efficient computation
of flocking behavior.
"""

class Boid(ContinuousSpaceAgent):
    """A Boid-style flocker agent.

    The agent follows three behaviors to flock:
        - Cohesion: steering towards neighboring agents
        - Separation: avoiding getting too close to any other agent
        - Alignment: trying to fly in the same direction as neighbors

    Boids have a vision that defines the radius in which they look for their
    neighbors to flock with. Their speed (a scalar) and direction (a vector)
    define their movement. Separation is their desired minimum distance from
    any other Boid.
    """

    def __init__(
        self,
        model,
        space,
        position=(0, 0),
        speed=1,
        direction=(1, 1),
        vision=1,
        separation=1,
        cohere=0.09,
        separate=0.015,
        match=0.05,
    ):
        """Create a new Boid flocker agent.

        Args:
            model: Model instance the agent belongs to
            speed: Distance to move per step
            direction: numpy vector for the Boid's direction of movement
            vision: Radius to look around for nearby Boids
            separation: Minimum distance to maintain from other Boids
            cohere: Relative importance of matching neighbors' positions (default: 0.03)
            separate: Relative importance of avoiding close neighbors (default: 0.015)
            match: Relative importance of matching neighbors' directions (default: 0.05)
        """
        super().__init__(space, model)
        self.position = position
        self.speed = speed
        self.direction = direction
        self.vision = vision
        self.separation = separation
        self.cohere_factor = cohere
        self.separate_factor = separate
        self.match_factor = match
        self.neighbors = []

    def step(self):
        """Get the Boid's neighbors, compute the new vector, and move accordingly."""
        neighbors, distances = self.get_neighbors_in_radius(radius=self.vision)
        self.neighbors = [n for n in neighbors if n is not self]

        # If no neighbors, maintain current direction
        if not neighbors:
            self.position += self.direction * self.speed
            return

        delta = self.space.calculate_difference_vector(self.position, agents=neighbors)

        cohere_vector = delta.sum(axis=0) * self.cohere_factor
        separation_vector = (
            -1 * delta[distances < self.separation].sum(axis=0) * self.separate_factor
        )
        match_vector = (
            np.asarray([n.direction for n in neighbors]).sum(axis=0) * self.match_factor
        )

        # Update direction based on the three behaviors
        self.direction += (cohere_vector + separation_vector + match_vector) / len(
            neighbors
        )

        # Normalize direction vector
        self.direction /= np.linalg.norm(self.direction)

        # Move boid
        self.position += self.direction * self.speed

class BoidFlockers(mesa.Model):

    def __init__(
        self,
        population_size=100,
        width=100,
        height=100,
        speed=1,
        vision=10,
        separation=2,
        cohere=0.09,
        separate=0.015,
        match=0.05,
        seed=None,
    ):
        
        super().__init__(seed=seed)
        self.population_size = population_size
        self.height = height
        self.width = width
        self.cohere = cohere
        self.vision = vision
        self.match =  match
        self.speed = speed
        self.separation = separation
        self.separate = separate
        
    def set_simulator_for_new_simulation(self, seed):
        self.random.seed(seed)
        self.reset_randomizer(seed)
                    
        self.remove_all_agents()
                
        self.space = ContinuousSpace(
            [[0, self.width], [0, self.height]],
            torus=True,
            random=self.random,
            n_agents=self.population_size,
        )

        self.make_agents()

    def make_agents(self):
        # Create and place the Boid agents
        positions = self.rng.random(size=(self.population_size, 2)) * self.space.size
        directions = self.rng.uniform(-1, 1, size=(self.population_size, 2))
        Boid.create_agents(
            self,
            self.population_size,
            self.space,
            position=positions,
            direction=directions,
            cohere=self.cohere,
            separate=self.separate,
            match=self.match,
            speed=self.speed,
            vision=self.vision,
            separation=self.separation,
        )

    def step(self):
        
        self.agents.shuffle_do("step")
        if self.steps == 100  or self.steps == 200:

            print("Predator encounter event triggered at step",self.steps)
            #self.predator_encounter_event()

    def get_time(self):
        return self.steps

    def eval(self, obs):
        """
        Evaluate observations on simulation states
        """
        if obs == 'avg_distance_from_centroid':
            #https://vergenet.net/~conrad/boids/pseudocode.html
            #Rule 1: Boids try to fly towards the centre of mass of neighbouring boids.
            cumul_distance=0
            for current_agent in self.agents:
                if len(current_agent.neighbors) > 0:
                    sum_pos_neigh=np.zeros(2)    
                    for neighbor in current_agent.neighbors:
                        sum_pos_neigh += neighbor.position
                    
                    centroid = sum_pos_neigh / float(len(current_agent.neighbors))
                    cumul_distance+=self.space.calculate_distances(centroid, [current_agent])[0][0]
            return cumul_distance/float(len(self.agents))
        
        elif obs == 'avg_visible_neighbours':
            total_neighbours = 0
            for current_agent in self.agents:
                neighbors = current_agent.neighbors
                #neighbors = self.space.get_neighbors(current_agent.pos, current_agent.vision, False)
                total_neighbours += len(neighbors)
            return total_neighbours / float(len(self.agents))
    
    def predator_encounter_event(self):
        for agent in self.agents:
            agent.cohere_factor = 0 - agent.cohere_factor