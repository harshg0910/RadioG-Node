import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

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
		public NodeHandle handle;
		public int level;

		FreeNode(NodeHandle nh, int l) {
			handle = nh;
			level = l;
		}

		public int compareTo(FreeNode arg0) {
			return level - arg0.level;
		}

		public boolean equals(FreeNode arg0) {
			return arg0.handle.equals(handle) && arg0.level == level;
		}

	}

	// To manage access by different threads
	// Keeps track of free nodes with their level ( Bootstrap : 0th level)
	Queue<FreeNode> queue = new PriorityBlockingQueue<FreeNode>();

	// When a new node join the network, mark as free
	public void addNode(NodeHandle n, int level) {
		queue.add(new FreeNode(n, level));
		//Radio.logger.log(Level.INFO, "Slot Free : " + n);
		System.out.println("-------Printing Queue :");
		for (FreeNode fn : queue) {
			System.out.println(fn.handle + " Level: " + fn.level);
		}
		System.out.println("-------End Queue :");
	}

	// Update the level
	public void updateNode(NodeHandle n, int level) {
		removeNode(n);
		addNode(n, level);
	}

	// When a node leaves the network, delete corresponding entry
	public void removeNode(NodeHandle n) {

		for (FreeNode fn : queue) {
			if (fn.handle == n) {
				queue.remove(fn);
				break;
			}
		}
//		Radio.logger.log(Level.INFO, "Node Left : " + n);
		System.out.println("-------Printing Queue :");
		for (FreeNode fn : queue) {
			System.out.println(fn.handle + " Level: " + fn.level);
		}
		System.out.println("-------End Queue :");
	}

	// // When all slots are full, use this method to remove
	// public void removeNode(NodeHandle n, int level) {
	//
	// queue.remove(new FreeNode(n, level));
	// Radio.logger.log(Level.INFO, "Slots Full : " + n);
	// System.out.println("-------Printing Queue :");
	// for (FreeNode fn : queue) {
	// System.out.println(fn.handle + " Level: " + fn.level);
	// }
	// System.out.println("-------End Queue :");
	// }

	// Returns the 'attempt+1'th free node
	// Returns null when attempt is negative or greater than the
	// total free nodes in the network
	public NodeHandle getFreeStreamer(short attempt) {

		// TODO : Apply more efficient node selection algorithm
		// like randomized or proximity based
		// It will return the node with required lowest free level in the tree
		if (attempt < queue.size() && attempt >= 0) {
			return ((FreeNode) queue.toArray()[attempt]).handle;
		}
		else {
			return null;
		}
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
