package routing.ocgr.metrics;

import core.SimClock;
import routing.ocgr.Vertex;
import routing.ocgr.util;

/**
 *  
 * 
 * the channel was idle during the contact opportunity.
 * Together with the contact size one can derive the transmission occupancy.
 * Additionally, together with the buffer freeCapPrediction, one can predict
 * the expected volume for some contact.
 * Taking in account the expected frequency of the contact and those above explained
 * infos, one can improve the network throughput and avoid congestion
 * 
 * How:
 * 	- on connection up (from a host):
 * 		reset the busy_counter for the vertice of this connection
 *      
 *  - on connection down (from a host):
 *      update vertex statistics
 *
 *  - on every update in which the channel is busy for a node h:
 *    if channel just become busy: // on every update verify if the state changed for this node
 *      set last_time -1.0
 *    for each CONNECTED vertex that contains this host:
 *      if last_time == -1.0:
 *      	busy_counter += increment
 *      elif last_time != now :
 *      	busy_counter += now - last_time 		//add increment size to busy counter
 *      last_time = now
 *   
 * @author jose
 *
 */
public class ContactOccupancyPrediction extends Prediction {

	double counter;
	double last_trans_start;
	
	public ContactOccupancyPrediction(Vertex v) {
		super(v);
		setName();
	}

	@Override
	public void update() {}

	@Override
	protected void setName() {
		super.setName("TransmissionFreeCapacityPred");
	}
	
	@Override
	public void connUp() {
		counter = 0.0;
	}

	@Override
	public void connDown() {}
}
