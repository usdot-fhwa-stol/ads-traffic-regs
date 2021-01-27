# ads-traffic-regs
The Automated Driving Systems Regulations (ADSRegs) system uses blockchain to publish situations that are referred to by regulations to requesting ADS.

## Documentation

### APIs

1. Jurisdictions - The inputs are a lon/lat geo-coordinate. The response is an object that contains key value pairs. The keys are the ids of the jurisdictions that the given point is inside and the values are objects that contain the label and the boundaries of the jurisdictions with the timestamp of when each boundary was last updated. 
2. Boundaries - The input is a list of jurisdiction ids. The response is an object that contains key value pairs. The keys are the ids of the jurisdictions and the values are arrays of boundaries that make up the geometry of the jurisdiction.
3. Situations - The input is a list of jurisdiction ids. The response is an object that contains key value pairs. The keys are the ids of the jurisdictions and the values are arrays of situations that are referred to by the regulations of the jurisdiction.

### System Objects

All objects are implemented using blockchain. The following fields are shared by all objects:
* specid (String) - the specification id is used to determine the type and the fields of the object.
* currid (String) - the current id is this object's id.
* previd (String) - the previous id is the id of the object that comes before this object in the blockchain.
* seq (Integer) - the sequence number is the position of this object in the blockchain.
* created-dt (String) - the created date describes when this object was created.

#### Jurisdiction
A jurisdiction consists of one or more boundary and is an entity that governs traffic regulations.
* label (String) - The label or name of the jurisdiction.

#### Boundary
A boundary describes the geometries of jurisdictions.
* jurisdiction (String) - The id of the jurisdiction that this boundary describes.
* timestamp (String) - The time the boundary was last updated.
* lon (Double[]) - Array of longitude ordinates that make up the geometry of the boundary when paired with the latitudes. The first and last ordinate should be the same.
* lat (Double[]) - Array of latitude ordinates that make up the geometry of the boundary when paired with the longitudes. The first and last ordinate should be the same.
* elev (Integer[]) - Array of elevations for each lon/lat coordinate.
* minlon (Double) - The minimum longitude of the boundary.
* minlat (Double) - The minimum latitude of the boundary.
* maxlon (Double) - The maximum longitude of the boundary.
* maxlat (Double) - The maximum latitude of the boundary.

#### Title
A title is a document that is published by a jurisdiction that contains instructions that describe the traffic regulations legislated by the jurisdiction.
* jurisdiction (String) - The id of the jurisdiction that published this title.
* label (String) - The label or name of the title.
* biblioref (String) - The bibliographic reference for the title.
* urlref (String) - If available, the URL where the title is available on the internet.

#### Instruction
An instruction is part of a title that describes one or more traffic regulation.
* title (String) - The id of the title that includes this instruction.
* label (String) - The label or name of the instruction.
* biblioref (String) - The bibliographic reference for the instruction
* urlref (String) - If available, the URL where the instruction is available on the internet.
* content (String) - An html representation of the contents of the instruction.
* situations (String[]) - A list of situations referred to by this instruction.

#### Situation
A situation describes different states that an ADS could be in while driving like passing on the left.
* label (String) - The label or name of the situation

#### TcdType
A traffic control device type represents the different types of traffic control devices deployed by a jurisdiction.
* jurisdiction (String) - The id of the jurisdiction that deploys this traffic control device type.
* label (String) - The label of the traffic control device type.
* descr (String) - A description of the traffic control device type.
* instrs (String[]) - A list of the instructions that refer to this traffic control device type.
* units (String) - The unit of measurement used to construct this traffic control device type.
* svg (String) - The html svg tag that renders this traffic control device type.

## Website Screenshots

### Home
![Home](/screenshots/home.png)

### Regulations
![Regulations List](/screenshots/regulations_list.png)

![Regulations Dialog](/screenshots/regulations_dialog.png)

### Situations
![Situations](/screenshots/situations.png)

![Situations Dialog](/screenshots/situations_dialog.png)

### Traffic Controls
![Traffic Controls List](/screenshots/traffic_controls_list.png)

![Traffic Controls Dialog](/screenshots/traffic_controls_dialog.png)

### Create
![Create Jurisdiction](/screenshots/create_jurisdiction.png)

![Create Title](/screenshots/create_title.png)

![Create Instruction](/screenshots/create_instruction.png)

![Create Situation](/screenshots/create_situation.png)

![Create Traffic Control Device Type](/screenshots/create_traffic_control_device_type.png)

## Traffic Regulations Database for Automated Driving Systems

### Introduction
This is the code for Automated Driving Systems (ADS) regulation database testing scenarios.
The test was designed to demonstrate a collaborative implementation of autonomous vehicle (AV) integration with Traffic laws and Regulations database framework.

With the assumption of a complete sensor suite and wireless communication, AV can extract traffic laws and regulations from the database and make appropriate judgments.

The test contains two scenarios, right turn on red and freeway overtake. With the same database, the AV is expected to follow different strategies at different locations.

### Environment Setup
Install the latest version of the CARLA simulator.
https://carla.readthedocs.io/en/latest/start_quickstart/

Then install all dependencies based on the requirements.
 ```Shell
  cd root/of/TRD-ADS
  pip install -r networkx
  pip install -r numpy
  pip install -r pygame
  pip install -r requests
  ```
### Test Freeway Scenrio
This part is used to test overtaking behavior on freeway. To run the algorithm:

 ```Shell
  cd root/of/TRD-ADS
  python -m freeway.overtaking_mid_lane -l
 ```
To throw more vehicles to test the algorithm, after run overtaking_mid_lane.py,  
open another tab and run followings:

  ```Shell
  python -m freeway.spawn_ NPC_for_overtaking
  ```

### Test Intersection Scenrio
This part is used to test overtaking behavior on freeway. To run the algorithm:

 ```Shell
  cd root/of/TRD-ADS
  python -m intersection.turn_on_red_scenario -l
 ```
To throw more vehicles to test the algorithm, after run overtaking_mid_lane.py,  
open another tab and run followings:

  ```Shell
  python -m intersection.spawn_ NPC_for_intersection
  ```

## Deployment


## Configuration


## Code of Conduct 
Please read our [Code of Conduct](Code_of_Conduct.md) which outlines our expectations for participants within the community, as well as steps to reporting unacceptable behavior. We are committed to providing a welcoming and inspiring community for all and expect our code of conduct to be honored. Anyone who violates this code of conduct may be banned from the community.

## Attribution
The development team would like to acknowledge the people who have made direct contributions to the design and code in this repository. [Attribution](ATTRIBUTION.md)

## License


## Contact
