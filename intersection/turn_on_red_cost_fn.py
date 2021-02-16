#!/usr/bin/env python
"""Cost function for turning on red
"""
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>

import math

import numpy as np

def determine_lane(ego_yaw, veh_yaw):
    """
    determine lanes of nearby vehicle's by orientation
    :param ego_yaw: the yaw angle of ego veh:
    :param veh_yaw: the yaw angle of nearby veh
    :return: veh-lane: the lane assignment of the nearby veh
    """
    err = 20
    # target lane
    target_lane_yaw = ego_yaw + 90
    neg_target_lane_yaw = target_lane_yaw - 360
    # opposite lane
    opposite_lane_yaw = ego_yaw + 180
    neg_opposite_yaw = opposite_lane_yaw - 360
    # determine lane
    if (target_lane_yaw - err <= veh_yaw <= target_lane_yaw + err) or \
            (neg_target_lane_yaw - err <= veh_yaw <= neg_target_lane_yaw + err):
        # target lane
        veh_lane = 'target'
    elif (opposite_lane_yaw - err <= veh_yaw <= opposite_lane_yaw + err) or \
            (neg_opposite_yaw - err <= veh_yaw <= neg_opposite_yaw + err):
        # opposite lane
        veh_lane = 'opposite'
    else:
        # other
        veh_lane = 'other'
    return veh_lane

def turn_on_red_cost(world, agent):
    # my position
    # player ==> vehicle
    ego_vehicle = agent.vehicle
    ego_yaw = ego_vehicle.get_transform().rotation.yaw
    ego_vehicle_loc = ego_vehicle.get_location()
    ego_id = ego_vehicle.id
    world = ego_vehicle.get_world()
    # speed limit
    speed_limit = ego_vehicle.get_speed_limit()

    # read nearby vehicle
    vehicle_list = world.get_actors().filter("*vehicle*")

    # helper function to find distance
    def dist(v):
        return math.sqrt((v.get_location().x - ego_vehicle_loc.x) ** 2 + \
                         (v.get_location().y - ego_vehicle_loc.y) ** 2 + \
                         (v.get_location().z - ego_vehicle_loc.z) ** 2)
    # find opposite vehicle
    opposite_lane_vlist = [[(dist(v)), v] for v in vehicle_list \
                           if dist(v) < 15 and v.id != ego_id and \
                           determine_lane(ego_yaw, v.get_transform().rotation.yaw) == 'opposite']
    # sort veh list based on distance
    opposite_lane_vlist.sort()

    # find target lane vehicle
    target_lane_vlist = [[dist(v), v] for v in vehicle_list \
                         if dist(v) < 15 and v.id != ego_id and \
                         determine_lane(ego_yaw, v.get_transform().rotation.yaw) == 'target']
    # sort based on distance
    target_lane_vlist.sort()

    # helper function to transfer velocity from 3D to 1D
    def transfer_3d(velocity):
        return math.sqrt(velocity.x ** 2 + velocity.y ** 2 + velocity.z ** 2)

    # target lane traffic
    if len(target_lane_vlist) > 1:
        min_2_dist_tar = np.sum([target_lane_vlist[0][0], target_lane_vlist[1][0]])
        min_veh_speed_tar = np.mean([transfer_3d(target_lane_vlist[0][1].get_velocity()),
                                     transfer_3d(target_lane_vlist[1][1].get_velocity())])
    else:
        min_2_dist_tar = 0
        min_veh_speed_tar = 0

    # opposite lane traffic
    if len(opposite_lane_vlist) > 1:
        min_distance_opposite = opposite_lane_vlist[0][0]
        min_veh_speed_opposite = transfer_3d(opposite_lane_vlist[0][1].get_velocity())
    else:
        min_distance_opposite = 0
        min_veh_speed_opposite = 0

    # total cost
    cost = 1 / (1 + min_2_dist_tar) + (min_veh_speed_tar / speed_limit) \
           + 1 / (1 + min_distance_opposite) + (min_veh_speed_opposite / speed_limit)

    return cost
