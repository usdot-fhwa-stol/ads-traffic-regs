#!/usr/bin/env python
"""Spawn NPCs into the simulation for freeway scenario
"""
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>

import argparse
import logging
import random
import time

import carla


def main():
    argparser = argparse.ArgumentParser(
        description=__doc__)
    argparser.add_argument(
        '--host',
        metavar='H',
        default='127.0.0.1',
        help='IP of the host server (default: 127.0.0.1)')
    argparser.add_argument(
        '-p', '--port',
        metavar='P',
        default=2000,
        type=int,
        help='TCP port to listen to (default: 2000)')
    argparser.add_argument(
        '-n', '--number-of-vehicles',
        metavar='N',
        default=7,
        type=int,
        help='number of vehicles (default: 10)')
    argparser.add_argument(
        '--safe',
        action='store_true',
        help='avoid spawning vehicles prone to accidents')
    argparser.add_argument(
        '--filterv',
        metavar='PATTERN',
        default='vehicle.*',
        help='vehicles filter (default: "vehicle.*")')
    argparser.add_argument(
        '--tm-port',
        metavar='P',
        default=8000,
        type=int,
        help='port to communicate with TM (default: 8000)')
    argparser.add_argument(
        '--sync',
        action='store_true',
        help='Synchronous mode execution')
    argparser.add_argument(
        '--hybrid',
        action='store_true',
        help='Enanble')

    args = argparser.parse_args()

    logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)

    vehicles_list = []

    client = carla.Client(args.host, args.port)
    client.set_timeout(10.0)
    synchronous_master = False

    try:
        # read world of the behavioral agent 
        world = client.get_world()

        # initiate traffic manager for background traffic control
        traffic_manager = client.get_trafficmanager(args.tm_port)
        traffic_manager.set_global_distance_to_leading_vehicle(2.0)
        if args.hybrid:
            traffic_manager.set_hybrid_physics_mode(True)

        # set synchronous mode 
        if args.sync:
            settings = world.get_settings()
            traffic_manager.set_synchronous_mode(True)
            if not settings.synchronous_mode:
                synchronous_master = True
                settings.synchronous_mode = True
                settings.fixed_delta_seconds = 0.05
                world.apply_settings(settings)
            else:
                synchronous_master = False

        # read blueprints and spawn vehicles
        blueprints = world.get_blueprint_library().filter(args.filterv)
        blueprints = [x for x in blueprints if int(x.get_attribute('number_of_wheels')) == 4]

        # vehicles for safe condition
        if args.safe:
            blueprints = [x for x in blueprints if not x.id.endswith('isetta')]
            blueprints = [x for x in blueprints if not x.id.endswith('carlacola')]
            blueprints = [x for x in blueprints if not x.id.endswith('cybertruck')]
            blueprints = [x for x in blueprints if not x.id.endswith('t2')]

        # Use command on batch
        SpawnActor = carla.command.SpawnActor
        SetAutopilot = carla.command.SetAutopilot
        FutureActor = carla.command.FutureActor

        # --------------
        # Spawn vehicles
        # --------------
        spawn_point_179 = carla.Transform(carla.Location(x=193.540588, y=13.658502, z=0.300000),
                                          carla.Rotation(pitch=0.000000, yaw=90.664635, roll=0.000000))

        spawn_point_189 = carla.Transform(carla.Location(x=190.040817, y=13.617901, z=0.300000),
                                          carla.Rotation(pitch=0.000000, yaw=90.664635, roll=0.000000))

        spawn_point_193 = carla.Transform(carla.Location(x=42.396034, y=-186.632599, z=0.300000),
                                          carla.Rotation(pitch=0.000000, yaw=-0.242950, roll=0.000000))

        spawn_point_262 = carla.Transform(carla.Location(x=69.415787, y=-190.247162, z=0.300000),
                                          carla.Rotation(pitch=0.000000, yaw=-0.242950, roll=0.00000))

        spawn_point_263 = carla.Transform(carla.Location(x=69.430626, y=-186.747208, z=0.300000),
                                          carla.Rotation(pitch=0.000000, yaw=-0.242950, roll=0.000000))

        spawn_point_269 = carla.Transform(carla.Location(x=7.145866, y=-186.483154, z=0.300000),
                                          carla.Rotation(pitch=-0.767242, yaw=-0.242950, roll=0))

        # 1. next to 261, left lane
        pos_263 = spawn_point_263
        new_spwan_location_1 = carla.Location(pos_263.location.x + 40,
                                              pos_263.location.y,
                                              pos_263.location.z)
        new_spwan_1 = carla.Transform(new_spwan_location_1, pos_263.rotation)

        # 2. next to 261, mid lane
        pos_262 = spawn_point_262

        # 3. next to 261, in curve 
        new_spwan_location_3 = carla.Location(pos_263.location.x + 60,
                                              pos_263.location.y + 5,
                                              pos_263.location.z)
        new_spwan_3 = carla.Transform(new_spwan_location_3, pos_262.rotation)

        # 4. left lane
        start_pos = spawn_point_269
        spwan_location = carla.Location(start_pos.location.x + 90,
                                        start_pos.location.y - 6,
                                        start_pos.location.z)
        spawn_point_mid = carla.Transform(spwan_location, start_pos.rotation)

        # 5, 6 further locations
        pos_179 = spawn_point_179
        pos_189 = spawn_point_189

        new_spwan_location_6 = carla.Location(pos_179.location.x,
                                              pos_179.location.y - 140,
                                              pos_179.location.z)
        new_spwan_location_7 = carla.Location(pos_189.location.x,
                                              pos_189.location.y - 140,
                                              pos_189.location.z)

        new_spwan_6 = carla.Transform(new_spwan_location_6, pos_179.rotation)
        new_spwan_7 = carla.Transform(new_spwan_location_7, pos_179.rotation)

        # store all spawn points 
        selected_spawn_points = [spawn_point_193, pos_263, new_spwan_1, new_spwan_3,
                                 spawn_point_mid, new_spwan_6, new_spwan_7]
        batch = []

        # spawn vehicles
        for n, transform in enumerate(selected_spawn_points):
            if n >= args.number_of_vehicles:
                break

            blueprint = random.choice(blueprints)
            if blueprint.has_attribute('color'):
                color = random.choice(blueprint.get_attribute('color').recommended_values)
                blueprint.set_attribute('color', color)
            blueprint.set_attribute('role_name', 'autopilot')

            # spawn the cars and set their autopilot and light state
            batch.append(SpawnActor(blueprint, transform)
                         .then(SetAutopilot(FutureActor, True, traffic_manager.get_port())))

        # log reponse 
        for response in client.apply_batch_sync(batch, synchronous_master):
            if response.error:
                logging.error(response.error)
            else:
                vehicles_list.append(response.actor_id)
                cur_actor = world.get_actor(response.actor_id)
                traffic_manager.auto_lane_change(cur_actor, False)

        # set background traffic speed
        traffic_manager.global_percentage_speed_difference(50.0)

        while True:
            if args.sync and synchronous_master:
                world.tick()
            else:
                world.wait_for_tick()

    # clear simulation when finished 
    finally:

        if args.sync and synchronous_master:
            settings = world.get_settings()
            settings.synchronous_mode = False
            settings.fixed_delta_seconds = None
            world.apply_settings(settings)

        print('\n destroying %d vehicles' % len(vehicles_list))
        client.apply_batch([carla.command.DestroyActor(x) for x in vehicles_list])

        time.sleep(0.5)


if __name__ == '__main__':

    try:
        main()
    except KeyboardInterrupt:
        pass
    finally:
        print('\ndone.')
