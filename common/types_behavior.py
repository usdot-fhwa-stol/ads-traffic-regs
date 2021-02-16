#!/usr/bin/env python
""" This module contains the different parameters sets for each behavior.
"""
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>


class Cautious(object):
    """Class for Cautious agent."""
    max_speed = 40
    speed_lim_dist = 6
    speed_decrease = 12
    safety_time = 4 + 2
    min_proximity_threshold = 11
    braking_distance = 6
    overtake_counter = 0
    tailgate_counter = 0


class Normal(object):
    """Class for Normal agent."""
    max_speed = 40
    speed_lim_dist = 4
    speed_decrease = 10
    # for car following: 
    safety_time = 5
    min_proximity_threshold = 10
    braking_distance = 3.5
    overtake_counter = 0
    tailgate_counter = 0


class Normal_follow(object):
    """Class for Normal agent."""
    max_speed = 40
    speed_lim_dist = 4
    speed_decrease = 10
    # for car following: 
    safety_time = 5
    min_proximity_threshold = 10
    braking_distance = 3.5
    overtake_counter = 0
    tailgate_counter = 0


class Normal_overtake(object):
    """Class for Normal agent. Highway use case"""
    max_speed = 28
    speed_lim_dist = 4  # 4
    # dont brake too much 
    speed_decrease = 6  # 8
    # for car overtake 
    safety_time = 3.5  # 2.5 #3 need smaller for later overtake (smaller --> later hazard)
    # activate hazard status ealier  
    min_proximity_threshold = 8  # 10 should be smaller to envourage overtaking and lane change
    braking_distance = 5  # 3.5 need smaller for later overtake (smaller --> later hazard)
    overtake_counter = 0
    tailgate_counter = 0


class Aggressive(object):
    """Class for Aggressive agent."""
    max_speed = 70
    speed_lim_dist = 1
    speed_decrease = 8
    safety_time = 3
    min_proximity_threshold = 8
    braking_distance = 3  # 4
    overtake_counter = 0
    tailgate_counter = -1
