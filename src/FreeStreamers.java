import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

import rice.p2p.commonapi.NodeHandle;

/**
 * @author Harsh
 * 
 *         This class is used to maintain the free streams across the network so
 *         that when any node asks this node for free streams. This data
 *         structure will be maintained by bootstrap node
 * 
 */

public class FreeStreamers {

	/**
	 * @author harsh 
	 * To keep track of nodehandles and their level
	 */
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

	/**
	 * To manage access by different threads. It keeps track of free nodes with
	 * their level ( Bootstrap : 0th level)
	 */
	Queue<FreeNode> queue = new PriorityBlockingQueue<FreeNode>();

	/**
	 * When a new node join the network, mark as free
	 * @param nodeHandle
	 * @param level The level of the node in the tree
	 * 
	 */
	public void addNode(NodeHandle nodeHandle, int level) {
		queue.add(new FreeNode(nodeHandle, level));
		// Radio.logger.log(Level.INFO, "Slot Free : " + n);
		// System.out.println("-------Printing Queue :");
		// for (FreeNode fn : queue) {
		// System.out.println(fn.handle + " Level: " + fn.level);
		// }
		// System.out.println("-------End Queue :");
	}

	/**
	 * Update the level
	 * @param nodeHandle
	 * @param level
	 */
	public void updateNode(NodeHandle nodeHandle, int level) {
		removeNode(nodeHandle);
		addNode(nodeHandle, level);
	}

	/**
	 * When a node leaves the network, delete corresponding entry
	 * @param nodeHandle
	 */
	public void removeNode(NodeHandle nodeHandle) {

		for (FreeNode fn : queue) {
			if (fn.handle == nodeHandle) {
				queue.remove(fn);
				break;
			}
		}
		// Radio.logger.log(Level.INFO, "Node Left : " + nodeHandle);
		// System.out.println("-------Printing Queue :");
		// for (FreeNode fn : queue) {
		// System.out.println(fn.handle + " Level: " + fn.level);
		// }
		// System.out.println("-------End Queue :");
	}

	/**
	 * Returns the 'attempt+1'th free node. 
	 * Returns null when attempt is negative or greater than the
	 * total free nodes in the network
	 * @param attempt
	 * @return NodeHandle of the free node
	 */
	public NodeHandle getFreeStreamer(short attempt) {

		// TODO : Apply more efficient node selection algorithm
		// like randomized or proximity based
		// It will return the node with required lowest free level in the tree
		if (attempt < queue.size() && attempt >= 0) {
			return ((FreeNode) queue.toArray()[attempt]).handle;
		} else {
			return null;
		}
	}
}
