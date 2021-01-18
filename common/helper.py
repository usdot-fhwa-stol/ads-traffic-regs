#!/usr/bin/env python
"""Helper functions for main functions
"""
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>

import math
import re

import carla


def find_weather_presets():
    """Method to find weather presets"""
    rgx = re.compile('.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)')

    def name(x): return ' '.join(m.group(0) for m in rgx.finditer(x))

    presets = [x for x in dir(carla.WeatherParameters) if re.match('[A-Z].+', x)]
    return [(getattr(carla.WeatherParameters, x), name(x)) for x in presets]


def get_actor_display_name(actor, truncate=250):
    """Method to get actor display name"""
    name = ' '.join(actor.type_id.replace('_', '.').title().split('.')[1:])
    return (name[:truncate - 1] + u'\u2026') if len(name) > truncate else name


def arrange_yaw(yaw):
    """Method to re-arrange yaw within 0 to 360"""
    return 360 + yaw if yaw < 0 else yaw


def is_turning_right(future_yaw, current_yaw):
    """determine if the vehicle is turning right """
    return True if future_yaw > current_yaw else (current_yaw - future_yaw <= 180)


def distance_to_ego(npc_loc, ego_transform):
    """ determine the distance between ego and current NPC """
    return math.sqrt((npc_loc.x - ego_transform.location.x) ** 2 + (npc_loc.y - ego_transform.location.y)
                     ** 2 + (npc_loc.z - ego_transform.location.z) ** 2)


def transform_loc(x, y):
    """
    function that transform simulation coordinates to lat lon coordinates
    #-------------------------------- location transform note ------------------------------
        1. Simulation Right turn on red leagal area: x~(-47.53, 27.46); y~(-84.63, -4.63)
        2. ADS Regs right turn on red leagal area: lat~(38.4, 41.98); lon~(-84.81, -80.51)
        3. Simulation Coordinates to lat/lon transform: from x->lat; y->lon
    # --------------------------------------------------------------------------------------
    """
    lat = ((x - (-47.53 - 5)) / (27.46 + 47.53 + 10)) * (41.98 - 38.4) + 38.4
    lon = ((y - (-84.63 - 5)) / (84.63 - 4.63 + 10)) * (84.81 - 80.51) + (-84.81)
    return lat, lon


