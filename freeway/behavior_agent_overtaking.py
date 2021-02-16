#!/usr/bin/env python
""" This module implements an agent that roams around a track following random
waypoints and avoiding other vehicles. The agent also responds to traffic lights,
traffic signs, and has different possible configurations. """
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>

import math
import random

import carla
import numpy as np

from freeway.agent_overtake import Agent
from agents.navigation.local_planner_behavior import LocalPlanner, RoadOption
from agents.navigation.global_route_planner import GlobalRoutePlanner
from agents.navigation.global_route_planner_dao import GlobalRoutePlannerDAO
from agents.tools.misc import get_speed, positive

from common.types_behavior import Cautious, Aggressive, Normal_follow, Normal_overtake


class BehaviorAgent(Agent):
    """
    BehaviorAgent implements an agent that navigates scenes to reach a given
    target destination, by computing the shortest possible path to it.
    This agent can correctly follow traffic signs, speed limitations,
    traffic lights, while also taking into account nearby vehicles. Lane changing
    decisions can be taken by analyzing the surrounding environment,
    such as overtaking or tailgating avoidance. Adding to these are possible
    behaviors, the agent can also keep safety distance from a car in front of it
    by tracking the instantaneous time to collision and keeping it in a certain range.
    Finally, different sets of behaviors are encoded in the agent, from cautious
    to a more aggressive ones.
    """

    def __init__(self, vehicle, ignore_traffic_light=False, behavior='normal_overtake'):
        """
        Constructor method.

            :param vehicle: actor to apply to local planner logic onto
            :param ignore_traffic_light: boolean to ignore any traffic light
            :param behavior: type of agent to apply
        """

        super(BehaviorAgent, self).__init__(vehicle)
        self.vehicle = vehicle
        self.ignore_traffic_light = ignore_traffic_light
        self._local_planner = LocalPlanner(self)
        self._grp = None
        self.look_ahead_steps = 0

        # Vehicle information
        self.speed = 0
        self.speed_limit = 0
        self.direction = None
        self.incoming_direction = None
        self.incoming_waypoint = None
        self.start_waypoint = None
        self.end_waypoint = None
        self.is_at_traffic_light = 0
        self.light_state = "Green"
        self.light_id_to_ignore = -1
        self.min_speed = 5
        self.behavior = None
        self._sampling_resolution = 4.5

        self.is_overtaking = False
        self.is_overtaking_prolong = False
        self.overtake_counter = 0
        self.tot_target_reached = 0
        # if stay at overtaking lane 
        self.is_stay_left = False

        # Parameters for agent behavior
        if behavior == 'cautious':
            self.behavior = Cautious()

        elif behavior == 'normal_follow':
            self.behavior = Normal_follow()

        elif behavior == 'normal_overtake':
            self.behavior = Normal_overtake()

        elif behavior == 'aggressive':
            self.behavior = Aggressive()

    def set_overtaking_behavior(self, is_stay_at_left):
        self.is_stay_left = is_stay_at_left

    def update_information(self, world):
        """
        This method updates the information regarding the ego
        vehicle based on the surrounding world.

            :param world: carla.world object
        """
        self.speed = get_speed(self.vehicle)
        self.speed_limit = world.player.get_speed_limit()

        self._local_planner.set_speed(self.speed_limit)
        self.direction = self._local_planner.target_road_option
        if self.direction is None:
            self.direction = RoadOption.LANEFOLLOW

        self.look_ahead_steps = int(self.speed_limit / 10)

        self.incoming_waypoint, self.incoming_direction = self._local_planner.get_incoming_waypoint_and_direction(
            steps=self.look_ahead_steps)
        if self.incoming_direction is None:
            self.incoming_direction = RoadOption.LANEFOLLOW

        self.is_at_traffic_light = world.player.is_at_traffic_light()
        if self.ignore_traffic_light:
            self.light_state = "Green"
        else:
            # This method also includes stop signs and intersections.
            self.light_state = str(self.vehicle.get_traffic_light_state())

    def set_destination(self, start_location, end_location, clean=False):
        """
        This method creates a list of waypoints from agent's position to destination location
        based on the route returned by the global router.

            :param start_location: initial position
            :param end_location: final position
            :param clean: boolean to clean the waypoint queue
        """
        if clean:
            self._local_planner.waypoints_queue.clear()
        self.start_waypoint = self._map.get_waypoint(start_location)
        self.end_waypoint = self._map.get_waypoint(end_location)

        route_trace = self._trace_route(self.start_waypoint, self.end_waypoint)

        self._local_planner.set_global_plan(route_trace)

    def reroute(self, spawn_points):
        """
        This method implements re-routing for vehicles approaching its destination.
        It finds a new target and computes another path to reach it.

            :param spawn_points: list of possible destinations for the agent
        """

        print("Target almost reached, setting new destination...")
        random.shuffle(spawn_points)
        new_start = self._local_planner.waypoints_queue[-1][0].transform.location
        destination = spawn_points[0].location if spawn_points[0].location != new_start else spawn_points[1].location
        print("New destination: " + str(destination))

        self.set_destination(new_start, destination)

    def _trace_route(self, start_waypoint, end_waypoint):
        """
        This method sets up a global router and returns the
        optimal route from start_waypoint to end_waypoint.

            :param start_waypoint: initial position
            :param end_waypoint: final position
        """
        # Setting up global router
        if self._grp is None:
            wld = self.vehicle.get_world()
            dao = GlobalRoutePlannerDAO(
                wld.get_map(), sampling_resolution=self._sampling_resolution)
            grp = GlobalRoutePlanner(dao)
            grp.setup()
            self._grp = grp

        # Obtain route plan
        route = self._grp.trace_route(
            start_waypoint.transform.location,
            end_waypoint.transform.location)

        return route

    def traffic_light_manager(self, waypoint):
        """
        This method is in charge of behaviors for red lights and stops.

        WARNING: What follows is a proxy to avoid having a car brake after running a yellow light.
        This happens because the car is still under the influence of the semaphore,
        even after passing it. So, the semaphore id is temporarely saved to
        ignore it and go around this issue, until the car is near a new one.

            :param waypoint: current waypoint of the agent
        """

        light_id = self.vehicle.get_traffic_light().id if self.vehicle.get_traffic_light() is not None else -1

        if self.light_state == "Red":
            if not waypoint.is_junction and (self.light_id_to_ignore != light_id or light_id == -1):
                return 1
            elif waypoint.is_junction and light_id != -1:
                self.light_id_to_ignore = light_id
        if self.light_id_to_ignore != light_id:
            self.light_id_to_ignore = -1
        return 0

    def _overtake(self, location, waypoint, vehicle_list):
        """
        This method is in charge of overtaking behaviors.

            :param location: current location of the agent
            :param waypoint: current waypoint of the agent
            :param vehicle_list: list of all the nearby vehicles
        """

        left_turn = waypoint.left_lane_marking.lane_change
        right_turn = waypoint.right_lane_marking.lane_change

        left_wpt = waypoint.get_left_lane()
        right_wpt = waypoint.get_right_lane()

        # left overtake 
        if (left_turn == carla.LaneChange.Left or left_turn == carla.LaneChange.Both) \
                and waypoint.lane_id * left_wpt.lane_id > 0 and left_wpt.lane_type == carla.LaneType.Driving:
            # check next vehicle hazard state 
            new_vehicle_state, target_vehicle, distance = self._bh_is_vehicle_hazard(
                waypoint, location, vehicle_list,
                max(
                    self.behavior.min_proximity_threshold,
                    self.speed_limit / 3),
                up_angle_th=180, lane_offset=-1)
            # overtake if next state is safe 
            if not new_vehicle_state:
                print(" !! Overtaking to the left !!")
                self.behavior.overtake_counter = 200

                # move temp target further 
                dx = self.vehicle.get_velocity().x  # m/s
                dy = self.vehicle.get_velocity().y
                dz = self.vehicle.get_velocity().z
                # a delta time in second to set the overtaking temp target
                dt = 2.83

                if abs(dy) >= 6:
                    print('Y axis speed high, adjust with higher delta t')
                    dt = 3.55

                distance = math.sqrt((dx * dt) ** 2 + (dy * dt) ** 2 + (dz * dt) ** 2)
                left_next_loc = left_wpt.next(distance)[0].transform.location
                self.set_destination(left_next_loc,
                                     self.end_waypoint.transform.location, clean=True)
                self.is_overtaking = True

        # right overtake
        elif (right_turn == carla.LaneChange.Right or right_turn == carla.LaneChange.Both)\
                and waypoint.lane_id * right_wpt.lane_id > 0 \
                and right_wpt.lane_type == carla.LaneType.Driving:
            print('!!! threshold pass for right overtake !!!')

            new_vehicle_state, _, _ = self._bh_is_vehicle_hazard(waypoint, location, vehicle_list, max(
                self.behavior.min_proximity_threshold, self.speed_limit / 3), up_angle_th=180, lane_offset=1)
            if not new_vehicle_state:
                print("Overtaking to the right!")
                self.behavior.overtake_counter = 200
                self.set_destination(right_wpt.transform.location,
                                     self.end_waypoint.transform.location, clean=True)
                print('Right waypoint location: ' + str(right_wpt.transform.location))
                # set overtake indicator 
                self.is_overtaking = True

    def _tailgating(self, location, waypoint, vehicle_list):
        """
        This method is in charge of tailgating behaviors.

            :param location: current location of the agent
            :param waypoint: current waypoint of the agent
            :param vehicle_list: list of all the nearby vehicles
        """

        left_turn = waypoint.left_lane_marking.lane_change
        right_turn = waypoint.right_lane_marking.lane_change

        left_wpt = waypoint.get_left_lane()
        right_wpt = waypoint.get_right_lane()

        behind_vehicle_state, behind_vehicle, _ = self._bh_is_vehicle_hazard(waypoint, location, vehicle_list, max(
            self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=180, low_angle_th=160)

        if behind_vehicle_state and self.speed < get_speed(behind_vehicle):
            if (right_turn == carla.LaneChange.Right or right_turn ==
                carla.LaneChange.Both) and waypoint.lane_id * right_wpt.lane_id > 0\
                    and right_wpt.lane_type == carla.LaneType.Driving:
                new_vehicle_state, _, _ = self._bh_is_vehicle_hazard(waypoint, location, vehicle_list, max(
                    self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=180, lane_offset=1)
                if not new_vehicle_state:
                    print("Tailgating, moving to the right!")
                    self.behavior.tailgate_counter = 200
                    self.set_destination(right_wpt.transform.location,
                                         self.end_waypoint.transform.location, clean=True)

            elif left_turn == carla.LaneChange.Left and waypoint.lane_id * left_wpt.lane_id > 0\
                    and left_wpt.lane_type == carla.LaneType.Driving:

                new_vehicle_state, _, _ = self._bh_is_vehicle_hazard(waypoint, location, vehicle_list, max(
                    self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=180, lane_offset=-1)

                if not new_vehicle_state:
                    print("Tailgating, moving to the left!")
                    self.behavior.tailgate_counter = 200
                    self.set_destination(left_wpt.transform.location,
                                         self.end_waypoint.transform.location, clean=True)

    def collision_and_car_avoid_manager(self, location, waypoint):
        """
        This module is in charge of warning in case of a collision
        and managing possible overtaking or tailgating chances.

            :param location: current location of the agent
            :param waypoint: current waypoint of the agent
            :return vehicle_state: True if there is a vehicle nearby, False if not
            :return vehicle: nearby vehicle
            :return distance: distance to nearby vehicle
        """

        vehicle_list = self._world.get_actors().filter("*vehicle*")

        def dist(v):
            return v.get_location().distance(waypoint.transform.location)

        # og dist threshold is 45
        vehicle_list = [v for v in vehicle_list if dist(v) < 25 and v.id != self.vehicle.id]

        if self.direction == RoadOption.CHANGELANELEFT:
            # Change lane to left
            vehicle_state, vehicle, distance = self._bh_is_vehicle_hazard(
                waypoint, location, vehicle_list, max(
                    self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=180, lane_offset=-1)
        elif self.direction == RoadOption.CHANGELANERIGHT:
            # Change lane to right
            vehicle_state, vehicle, distance = self._bh_is_vehicle_hazard(
                waypoint, location, vehicle_list, max(
                    self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=180, lane_offset=1)
        else:
            # Overtaking 
            vehicle_state, vehicle, distance = self._bh_is_vehicle_hazard(
                waypoint, location, vehicle_list,
                max(  # increase up_angle_th to account for goal waypoint appear after turn
                    self.behavior.min_proximity_threshold, self.speed_limit / 3),
                up_angle_th=30)  # up_angle_th=100) # og up th: 30)

            # customized overtake
            if vehicle_state and self.speed > 10 and self.behavior.overtake_counter == 0 and \
                    self.speed > get_speed(vehicle):
                self._overtake(location, waypoint, vehicle_list)

            # Check for tailgating
            elif not vehicle_state and self.direction == RoadOption.LANEFOLLOW \
                    and not waypoint.is_junction and self.speed > 10 \
                    and self.behavior.tailgate_counter == 0:
                self._tailgating(location, waypoint, vehicle_list)

        return vehicle_state, vehicle, distance

    def pedestrian_avoid_manager(self, location, waypoint):
        """
        This module is in charge of warning in case of a collision
        with any pedestrian.

            :param location: current location of the agent
            :param waypoint: current waypoint of the agent
            :return vehicle_state: True if there is a walker nearby, False if not
            :return vehicle: nearby walker
            :return distance: distance to nearby walker
        """

        walker_list = self._world.get_actors().filter("*walker.pedestrian*")

        def dist(w):
            return w.get_location().distance(waypoint.transform.location)

        walker_list = [w for w in walker_list if dist(w) < 10]

        if self.direction == RoadOption.CHANGELANELEFT:
            walker_state, walker, distance = self._bh_is_vehicle_hazard(waypoint, location, walker_list, max(
                self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=90, lane_offset=-1)
        elif self.direction == RoadOption.CHANGELANERIGHT:
            walker_state, walker, distance = self._bh_is_vehicle_hazard(waypoint, location, walker_list, max(
                self.behavior.min_proximity_threshold, self.speed_limit / 2), up_angle_th=90, lane_offset=1)
        else:
            walker_state, walker, distance = self._bh_is_vehicle_hazard(waypoint, location, walker_list, max(
                self.behavior.min_proximity_threshold, self.speed_limit / 3), up_angle_th=60)

        return walker_state, walker, distance

    def car_following_manager(self, vehicle, distance, debug=False):
        """
        Module in charge of car-following behaviors when there's
        someone in front of us.

            :param vehicle: car to follow
            :param distance: distance from vehicle
            :param debug: boolean for debugging
            :return control: carla.VehicleControl
        """

        vehicle_speed = get_speed(vehicle)
        delta_v = max(1, (self.speed - vehicle_speed) / 3.6)
        ttc = distance / delta_v if delta_v != 0 else distance / np.nextafter(0., 1.)

        # Under safety time distance, slow down.
        if 0.4 * self.behavior.safety_time > ttc > 0.0:
            print('CAR FOLLOW: slow down ...')
            control = self._local_planner.run_step(
                target_speed=min(positive(vehicle_speed - self.behavior.speed_decrease),
                                 min(self.behavior.max_speed, self.speed_limit - self.behavior.speed_lim_dist)),
                debug=debug)

        # Actual safety distance area, try to follow the speed of the vehicle in front.
        elif 0.9 * self.behavior.safety_time > ttc >= self.behavior.safety_time:
            print('CAR FOLLOW: Regular Car following ...')
            control = self._local_planner.run_step(
                target_speed=min(max(self.min_speed, vehicle_speed),
                                 min(self.behavior.max_speed, self.speed_limit - self.behavior.speed_lim_dist)),
                debug=debug)
        # Normal behavior.
        else:
            # normal behavior, not following
            control = self._local_planner.run_step(
                target_speed=min(self.behavior.max_speed, self.speed_limit - self.behavior.speed_lim_dist), debug=debug)

        return control

    def run_step(self, debug=False):
        """
        Execute one step of navigation.

            :param debug: boolean for debugging
            :return control: carla.VehicleControl
        """
        if self.behavior.tailgate_counter > 0:
            self.behavior.tailgate_counter -= 1
        if self.behavior.overtake_counter > 0:
            self.behavior.overtake_counter -= 1

        ego_vehicle_loc = self.vehicle.get_location()
        ego_vehicle_wp = self._map.get_waypoint(ego_vehicle_loc)

        # 1: Red lights and stops behavior

        if self.traffic_light_manager(ego_vehicle_wp) != 0:
            return self.emergency_stop()

        # 2.1: Pedestrian avoidancd behaviors

        walker_state, walker, w_distance = self.pedestrian_avoid_manager(
            ego_vehicle_loc, ego_vehicle_wp)

        if walker_state:
            # Distance is computed from the center of the two cars,
            # we use bounding boxes to calculate the actual distance
            distance = w_distance - max(
                walker.bounding_box.extent.y, walker.bounding_box.extent.x) - max(
                self.vehicle.bounding_box.extent.y, self.vehicle.bounding_box.extent.x)

            # Emergency brake if the car is very close.
            if distance < self.behavior.braking_distance:
                return self.emergency_stop()

        # 2.2: Car following behaviors
        vehicle_state, vehicle, distance = self.collision_and_car_avoid_manager(
            ego_vehicle_loc, ego_vehicle_wp)

        # ----------------------------3. Overtaking Controls ------------------------------------
        
        # read all closrby vehicles
        vehicles = self._world.get_actors().filter("*vehicle*")
        transform = self.vehicle.get_transform()

        def dist(l):
            return math.sqrt((l.x - transform.location.x) ** 2 + (l.y - transform.location.y)
                             ** 2 + (l.z - transform.location.z) ** 2)

        vehicles = [(dist(x.get_location()), x) for x in vehicles if x.id != self.vehicle.id]
        # front vehicle distance 
        min_dist, min_vehicle = sorted(vehicles)[0]
        min_wpt = self._map.get_waypoint(min_vehicle.get_location())
        min_lane_ID = min_wpt.lane_id
        ego_lane_ID = ego_vehicle_wp.lane_id

        # determine if necessary to prolong the time in overtaking lane 
        # Note: Determine conditions: 1.Ego in left lane. 2.Leading vehicle in right lane.
        # 3.Minimum distance smaller than 10
        is_delay_lnchg = (min_lane_ID == 3) and (ego_lane_ID == 2) and (min_dist <= 10)

        # reassign waypoint at current lane if need to delay merge back
        if is_delay_lnchg and self.is_overtaking and not self.is_stay_left:
            if self.overtake_counter == 0:
                # reset the target to the current lane
                dx = self.vehicle.get_velocity().x  # m/s
                dy = self.vehicle.get_velocity().y
                dz = self.vehicle.get_velocity().z
                dt = 1

                distance = math.sqrt((dx * dt) ** 2 + (dy * dt) ** 2 + (dz * dt) ** 2)
                temp_start = ego_vehicle_wp.next(distance)[0].transform.location
                temp_goal = ego_vehicle_wp.next(5 * distance)[0].transform.location

                # update temp goal 
                self._local_planner.waypoints_queue.clear()
                self._local_planner._waypoint_buffer.clear()

                self.set_destination(temp_start, temp_goal, clean=True)
                self.is_overtaking_prolong = True
                self.overtake_counter += 1

                # print('current counter: ' + str(self.overtake_counter))
                print('Set new destination: ' + str(temp_goal))

        if vehicle_state:
            # Distance is computed from the center of the two cars,
            # we use bounding boxes to calculate the actual distance
            distance = distance - max(
                vehicle.bounding_box.extent.y, vehicle.bounding_box.extent.x) - max(
                self.vehicle.bounding_box.extent.y, self.vehicle.bounding_box.extent.x)

            # Emergency brake if the car is very close.
            if distance < 0.8 * self.behavior.braking_distance:
                print('EEEE Brake ! Emergency brake for too close !!')
                return self.emergency_stop()
            else:
                control = self.car_following_manager(vehicle, distance)

        # 4: Normal behavior

        # Calculate controller based on no turn, traffic light or vehicle in front
        else:
            control = self._local_planner.run_step(
                target_speed=min(self.behavior.max_speed, self.speed_limit - self.behavior.speed_lim_dist), debug=debug)

        return control

    @property
    def local_planner(self):
        return self._local_planner
