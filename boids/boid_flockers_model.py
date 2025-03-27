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


class Boid(mesa.Agent):
    """
    A Boid-style flocker agent.

    The agent follows three behaviors to flock:
        - Cohesion: steering towards neighboring agents.
        - Separation: avoiding getting too close to any other agent.
        - Alignment: try to fly in the same direction as the neighbors.

    Boids have a vision that defines the radius in which they look for their
    neighbors to flock with. Their speed (a scalar) and direction (a vector)
    define their movement. Separation is their desired minimum distance from
    any other Boid.
    """

    def __init__(
        self,
        model,
        pos,
        speed,
        direction,
        vision,
        separation,
        cohere=0.03,
        separate=0.015,
        match=0.05,
    ):
        """
        Create a new Boid flocker agent.

        Args:
            unique_id: Unique agent identifier.
            pos: Starting position
            speed: Distance to move per step.
            direction: numpy vector for the Boid's direction of movement.
            vision: Radius to look around for nearby Boids.
            separation: Minimum distance to maintain from other Boids.
            cohere: the relative importance of matching neighbors' positions
            separate: the relative importance of avoiding close neighbors
            match: the relative importance of matching neighbors' headings
        """
        super().__init__(model)
        self.pos = np.array(pos)
        self.speed = speed
        self.direction = direction
        self.vision = vision
        self.separation = separation
        self.cohere_factor = cohere
        self.separate_factor = separate
        self.match_factor = match
        self.neighbors = None

    def step(self):
        """
        Get the Boid's neighbors, compute the new vector, and move accordingly.
        """

        self.neighbors = self.model.space.get_neighbors(self.pos, self.vision, False)
        n = 0
        match_vector, separation_vector, cohere = np.zeros((3, 2))
        for neighbor in self.neighbors:
            n += 1
            heading = self.model.space.get_heading(self.pos, neighbor.pos)
            cohere += heading
            if self.model.space.get_distance(self.pos, neighbor.pos) < self.separation:
                separation_vector -= heading
            match_vector += neighbor.direction
        n = max(n, 1)
        cohere = cohere * self.cohere_factor
        separation_vector = separation_vector * self.separate_factor
        match_vector = match_vector * self.match_factor
        self.direction += (cohere + separation_vector + match_vector) / n
        self.direction /= np.linalg.norm(self.direction)
        new_pos = self.pos + self.direction * self.speed
        self.model.space.move_agent(self, new_pos)

class BoidFlockers(mesa.Model):
    """
    Flocker model class. Handles agent creation, placement and scheduling.
    """

    def __init__(
        self,
        seed=None,
        population=100,
        width=100,
        height=100,
        vision=10,
        speed=1,
        separation=1,
        cohere=0.03,
        separate=0.015,
        match=0.05,
    ):
        """
        Create a new Flockers model.

        Args:
            population: Number of Boids
            width, height: Size of the space.
            speed: How fast should the Boids move.
            vision: How far around should each Boid look for its neighbors
            separation: What's the minimum distance each Boid will attempt to
                    keep from any other
            cohere, separate, match: factors for the relative importance of
                    the three drives.
        """
        super().__init__(seed=seed)
        self.population = population
        self.height = height
        self.width = width
        self.cohere = cohere
        self.vision = vision
        self.match =  match
        self.speed = speed
        self.separation = separation
        self.separate = separate
        self.factors = {"cohere": self.cohere, "separate": self.separate, "match": self.match}
        

        
       
        
        
    def set_simulator_for_new_simulation(self, seed):
        random.seed(seed)
        self.random.seed(seed)
        self.reset_randomizer(seed)
        # No need to redefine self.schedule here
        # Create scheduler and assign it to the model
        if(len(self.agents)) > 0:
            for agent in self.agents:
                agent.remove()
        self.space = mesa.space.ContinuousSpace(self.width, self.height, True) # moved here
        self.make_agents()
        
        

    def make_agents(self):
        """
        Create self.population agents, with random positions and starting headings.
        """
        for i in range(self.population):
            x = self.random.random() * self.space.x_max
            y = self.random.random() * self.space.y_max
            pos = np.array((x, y))
            direction = np.random.random(2) * 2 - 1
            boid = Boid(
                model=self,
                pos=pos,
                speed=self.speed,
                direction=direction,
                vision=self.vision,
                separation=self.separation,
                **self.factors,
            )
            self.space.place_agent(boid, pos)
            
    def step(self):
        self.agents.do("step")
        if self.get_time() == 200  or self.get_time() == 400:
            #print("Predator encounter event triggered at step",self.get_time())
            self.predator_encounter_event()
        
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
                        sum_pos_neigh += neighbor.pos
                    centroid = sum_pos_neigh / float(len(current_agent.neighbors))
                    cumul_distance+=self.space.get_distance(current_agent.pos, centroid)
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
            agent.cohere_factor=0-agent.cohere_factor
            





    # def predator_encounter_event(self):
    #     # print("Predator encounter event triggered")  # Messaggio di controllo
    #     # print(f"At step {self.get_time()}: The flock encounters some predators.")
    #     # for agent in self.schedule.agents:
    #     #     # Randomly scatter each agent
    #     #     new_x = self.random.random() * self.space.x_max
    #     #     new_y = self.random.random() * self.space.y_max
    #     #     agent.pos = np.array((new_x, new_y))
    #     #     # Update the space with the new position
    #     #     self.space.move_agent(agent, agent.pos)
    #     #print("Predator encounter event triggered")  # Messaggio di controllo
    #     #print(f"At step {self.get_time()}: The flock encounters some predators.")
    #     for agent in self.schedule.agents:
    #         agent.cohere_factor=0-agent.cohere_factor
    #         #agent.self.separate_factor=agent.self.separate_factor*10
    #         # Randomly scatter each agent
    #         #new_x = self.random.random() * self.space.x_max
    #         #new_y = self.random.random() * self.space.y_max
    #         #agent.pos = np.array((new_x, new_y))
    #         # Update the space with the new position
        




# def eval(self, obs):
#         """
#         Evaluate the centroid for the entire flock and return distances of each agent from the centroid.
#         """
        
#         if obs == 'compactness_of_flock':
#             total_pos = np.zeros(2)
#             for agent in self.agents:
#                 total_pos += agent.pos
#             centroid = total_pos / len(self.agents)
#             distances=np.zeros(len(self.agents))
#             i=0
#             for agent in self.agents:
#                 distance = self.space.get_distance(agent.pos, centroid)
#                 distances[i]=distance
#                 i+=1
#             #compactness = math.sqrt(sum(distance ** 2 for distance in distances))
#             compactness = np.sqrt(np.sum(distances**2))
#             return compactness
#         elif obs == 'compactness_of_flock_neighbours':
#             sum_compactness=0
#             sum_compactness_scaled = 0 
#             for current_agent in self.agents:
#                 #neighbors = current_agent.neighbors
#                 neighbors = self.space.get_neighbors(current_agent.pos, current_agent.vision, False)
#                 n_neighbors = len(neighbors)
#                 compactness=0
#                 compactness_scaled=0
#                 if n_neighbors == 0:
#                     compactness=0
#                 else:
#                     total_pos = np.zeros(2)    
#                     for neighbor in neighbors:
#                         total_pos += neighbor.pos
#                     centroid = total_pos / float(n_neighbors)
#                     distances=np.zeros(n_neighbors)
#                     i=0
#                     for neighbor in neighbors:
#                         distance = self.space.get_distance(neighbor.pos, centroid)
#                         distances[i]=distance
#                         i+=1
#                     #compactness = math.sqrt(sum(distance ** 2 for distance in distances))
#                     compactness = np.sqrt(np.sum(distances**2))
#                     compactness_scaled = compactness / n_neighbors
#                 sum_compactness+=compactness
#                 sum_compactness_scaled += compactness_scaled
#             #return sum_compactness/float(len(self.agents))
#             return sum_compactness_scaled
#         #################################################
#         #################################################
#         #################################################
#         elif obs == 'avg_distance_from_centroid':
#             #https://vergenet.net/~conrad/boids/pseudocode.html
#             #Rule 1: Boids try to fly towards the centre of mass of neighbouring boids.
#             cumul_distance=0
#             for current_agent in self.agents:
#                 if len(current_agent.neighbors) > 0:
#                     sum_pos_neigh=np.zeros(2)    
#                     for neighbor in current_agent.neighbors:
#                         sum_pos_neigh += neighbor.pos
#                     centroid = sum_pos_neigh / float(len(current_agent.neighbors))
#                     cumul_distance+=self.space.get_distance(current_agent.pos, centroid)
#             return cumul_distance/float(len(self.agents))
#         elif obs == 'avg_norm_fly_towards_center':
#             #https://vergenet.net/~conrad/boids/pseudocode.html
#             #Rule 1: Boids try to fly towards the centre of mass of neighbouring boids.
#             cumul_norm_fly_towards_center=0
#             for current_agent in self.agents:
#                 if len(current_agent.neighbors) > 0:
#                     sum_pos_neigh=np.zeros(2)    
#                     for neighbor in current_agent.neighbors:
#                         sum_pos_neigh += neighbor.pos
#                     centroid = sum_pos_neigh / float(len(current_agent.neighbors))
#                     centroid_minus_pos = centroid - current_agent.pos
#                     cumul_norm_fly_towards_center += np.linalg.norm(centroid_minus_pos)
#             return cumul_norm_fly_towards_center/float(len(self.agents))
#         elif obs == 'avg_norm_sep_vector':
#             #https://vergenet.net/~conrad/boids/pseudocode.html
#             #Rule 2: Birds try to keep a small distance away from other birds    
#             cumul_separation_vector_norm=0
#             for current_agent in self.agents:
#                 separation_vector = np.zeros(2)
#                 for neighbor in current_agent.neighbors:
#                     if self.space.get_distance(current_agent.pos, neighbor.pos) < current_agent.separation:
#                         separation_vector -= (current_agent.pos - neighbor.pos)
#                 cumul_separation_vector_norm += np.linalg.norm(separation_vector)
#             return cumul_separation_vector_norm/float(len(self.agents))
#             #################################################
#             #################################################
#             #################################################
#         elif obs == 'avg_norm_coherence':
#             norm_cohere=0
#             for current_agent in self.agents:
#                 cohere=0
#                 for neighbor in current_agent.neighbors:
#                     cohere += self.space.get_heading(current_agent.pos, neighbor.pos)
#                 cohere=cohere/max(1, len(current_agent.neighbors))
#                 norm_cohere+=np.linalg.norm(cohere)
#             return norm_cohere/float(len(self.agents))
#         elif obs == 'avg_neighbor_too_close':
#             cumul_ratio_neighbor_too_close=0
#             for current_agent in self.agents:
#                 neighbor_too_close=0
#                 for neighbor in current_agent.neighbors:
#                     if self.space.get_distance(current_agent.pos, neighbor.pos) < current_agent.separation:
#                         neighbor_too_close+=1
#                 ratio_neighbor_too_close=neighbor_too_close/max(1, len(current_agent.neighbors))
#                 cumul_ratio_neighbor_too_close+=ratio_neighbor_too_close
#             return cumul_ratio_neighbor_too_close/float(len(self.agents))
#         elif obs == 'avg_visible_neighbours':
#             total_neighbours = 0
#             for current_agent in self.agents:
#                 neighbors = current_agent.neighbors
#                 #neighbors = self.space.get_neighbors(current_agent.pos, current_agent.vision, False)
#                 total_neighbours += len(neighbors)
#             return total_neighbours / float(len(self.agents))
#         # elif obs == 'centroid_x':
#         #     return centroid[0]
        
#         # elif obs == 'centroid_y':    
#         #     return centroid[1]
#         elif obs == 'n_agents':
            return len(self.agents)        