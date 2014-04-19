import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

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

	// To keep track of nodehandles and their level
	class FreeNode implements Comparable<FreeNode> {
		NodeHandle handle;
		int level;

		FreeNode(NodeHandle nh, int l) {
			handle = nh;
			level = l;
		}

		public int compareTo(FreeNode arg0) {
			return arg0.level - level;
		}

	}

	// To manage access by different threads
	// Keeps track of free nodes with their level ( Bootstrap : 0th level)
	Queue<FreeNode> queue = new PriorityBlockingQueue<FreeNode>();

	// When a new node join the network, mark as free
	public void addNode(NodeHandle n, int level) {
		
		System.out.println(queue.toString());
		
		queue.add(new FreeNode(n, level));
	}

	// When a node leaves the network, delete corresponding entry
	public void removeNode(NodeHandle n) {
		for (FreeNode fn : queue) {
			if (fn.handle == n) {
				queue.remove(fn);
				break;
			}
		}
	}

	// When all slots are full, use this method to remove
	public void removeNode(NodeHandle n, int level) {
		queue.remove(new FreeNode(n, level));
	}

	// Returns the 'attempt+1'th free node
	// Returns null when attempt is negative or greater than the
	// total free nodes in the network
	public NodeHandle getFreeStreamer(short attempt) {

		// TODO : Apply more efficient node selection algorithm
		// like randomized or proximity based
		// It will return the node with required lowest free level in the tree

		return (NodeHandle) queue.toArray()[attempt % queue.size()];

		/*
		 * Iterator<NodeHandle> ItrKeys = map.keySet().iterator();
		 * 
		 * NodeHandle freeNode = null;
		 * 
		 * // After this loop freeNode will contain 'attempt+1'th free node
		 * while (attempt >= 0) { if (ItrKeys.hasNext()) freeNode =
		 * ItrKeys.next(); else freeNode = null;
		 * 
		 * // This while loop will find the next free node while
		 * (ItrKeys.hasNext() && map.get(freeNode) != true) { freeNode =
		 * ItrKeys.next(); } attempt--; } return freeNode;
		 */
	}
}
