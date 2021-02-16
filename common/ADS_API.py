#!/usr/bin/env python
"""ADS Utility Library
"""
# Author: Xu Han <hanx4@mail.uc.edu>, Runsheng Xu <rxx3386@ucla.edu>, Jiaqi Ma <jiaqima@ucla.edu>

import requests
import json


# helper func
# tion to print json text
def jprint(obj):
    """
    create a formatted string of the Python JSON object
    """
    text = json.dumps(obj, sort_keys=True, indent=4)
    print(text)


# function to read ads regulations
def read_info(ads_regs):
    """
    function that read ADS regulation string and return 1x4 regulation vector
    """
    ads_list = ads_regs.split(' ')

    # 1. find action info
    action = ads_list[0]

    # 2. find direction
    if len(ads_list) >= 2:
        direction = ads_list[1]
    # elif: direction == 'at': direction
    else:
        direction = ''

    # 3. find situation info
    if 'at' in ads_list:
        situation_index = ads_list.index('at') + 1
        situation = ads_list[situation_index]
    elif 'in' in ads_list:
        situation_index = ads_list.index('in') + 1
        situation = ads_list[situation_index]
        if situation == 'bi-directional': situation = situation + ' ' + ads_list[situation_index + 1]
    else:
        situation = ''

    # 4. find signal info
    if 'with' in ads_list:
        signal_index = ads_list.index('with') + 1
        signal = ads_list[signal_index]
        # account for road sign
        if signal == 'stop' or signal == 'yield' or signal == 'flashing':
            signal = signal + ' ' + ads_list[signal_index + 1]
    else:
        signal = ''

    # construct ADS behavior vector
    ads_vector = [action, direction, situation, signal]

    return ads_vector


# function to read all ADS behaviors 
def read_all_ADS_behavior(lat=39, lon=-82, jur_id_index=1):
    """
    function that read all ADS behavior based on location
    "lat=39, lon=-82" is the default coordinate provided in the testing page
    """

    # 1. reqest jurisdiction info
    latlon_parameters = {"Lon": lon,
                         "Lat": lat}
    response_jurisdiction = requests.get("https://adsregs.data-env.com/api/jurisdictions",
                                         params=latlon_parameters)
    jurisdiction_json = response_jurisdiction.json()

    # read boundaries
    boundaries = []
    labels = []
    for key, val in jurisdiction_json.items():
        boundaries.append(key)
        labels.append(val)

    # 2. request boundaries
    curr_boundary = boundaries[jur_id_index]
    boundary_param = {"id": curr_boundary}
    response_boundary = requests.post("https://adsregs.data-env.com/api/boundaries",
                                      params=boundary_param)
    # find location range
    minlon = response_boundary.json()[curr_boundary][0]['minlon']
    maxlon = response_boundary.json()[curr_boundary][0]['maxlon']
    minlat = response_boundary.json()[curr_boundary][0]['minlat']
    maxlat = response_boundary.json()[curr_boundary][0]['maxlat']
    loc_range = [minlat, maxlat, minlon, maxlon]

    # 3. request situation
    situation_param = {"id": curr_boundary}
    response_situation = requests.post("https://adsregs.data-env.com/api/situations",
                                       params=situation_param)

    # 4. read all situations
    # Note: situation is a list of dictionaries. Each dictionary contains metadata for one scenario
    for key, val in response_situation.json().items():
        situations = val

    return loc_range, situations


def filter_moves_from_regs(ego_vector, all_ads_regulations):
    """
    function that read ADS suggested behavior based on current situation
    return: ADS behavior vector
            the ADS behavio vector = [action, direction, situation, signal]
    """
    # read ADS behavior
    all_available_moves = []
    all_moves = []
    for behavior in all_ads_regulations:
        curr_ads_move = behavior['label']
        curr_vector = read_info(curr_ads_move)
        all_moves.append(curr_vector)
        if ego_vector[2] == curr_vector[2]:
            all_available_moves.append(curr_vector)

    return all_moves
