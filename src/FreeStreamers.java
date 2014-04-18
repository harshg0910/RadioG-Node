import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import rice.p2p.commonapi.NodeHandle;

/**
 * This class is used to maintain the free streams across the 
 * network so that when any node asks this node for free streams. 
 */

/**
 * @author Harsh
 * 
 */
public class FreeStreamers {

	// Keeps track of whether a node has free slots or not
	private Map<NodeHandle, Boolean> map = new HashMap<NodeHandle, Boolean>();

	// Returns the set of all nodes present in the network 
	public Set<NodeHandle> getAllNodes(){
		return map.keySet();
	}
	// Check whether a node has free slot or not
	public boolean hasFreeSlot(NodeHandle n) {
		return map.get(n).booleanValue();
	}

	// When a new node join the network, mark all slots as free
	public void addNode(NodeHandle n) {
		map.put(n, true);
	}

	// When a node leaves the network, delete corresponding entry
	public void removeNode(NodeHandle n) {
		map.remove(n);
	}

	// Update value corresponding to a node
	public void set(NodeHandle n, boolean t) {
		map.put(n, t);
	}

	//	Returns  the 'attempt+1'th free node
	//	Returns null when attempt is negative or greater than the 
	// 	total free nodes in the network
	public NodeHandle getStreamer(short attempt) {

		// TODO : Apply more efficient node selection algorithm
		// like randomized or proximity based
		Iterator<NodeHandle> ItrKeys = map.keySet().iterator();
		
		NodeHandle freeNode = null;
		
		// After this loop freeNode will contain 'attempt+1'th free node
		while (attempt >= 0) {
			if(ItrKeys.hasNext())
				freeNode = ItrKeys.next();
			else
				freeNode = null;
			
			// This while loop will find the next free node
			while (ItrKeys.hasNext() && map.get(freeNode) != true){
				freeNode = ItrKeys.next();
			}
			attempt--;
		}
		return freeNode;
	}
}
