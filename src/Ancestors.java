import java.util.Vector;
import java.util.logging.Level;

import rice.p2p.commonapi.NodeHandle;

/**
 * Maintains list of ancestors in streaming tree
 * 
 * @author Abhi
 * 
 */
public class Ancestors {
	private static Vector<NodeHandle> ancestorList = null;

	/**
	 * add node to ancetor list
	 * 
	 * @param node
	 *            - NodeHandle of the node to add
	 */
	public void addAncetor(NodeHandle node) {
		if (ancestorList != null) {
			ancestorList.add(node);
		} else {
			Radio.logger.log(Level.SEVERE, "Ancestors not initialized");
		}
	}

	/**
	 * 
	 * @return returns level of the node in tree
	 */
	public int getLevel() {
		if (ancestorList != null)
			return ancestorList.size();
		else
			return 0;
	}

	/**
	 * remove a node from ancestor list
	 * 
	 * @param node
	 */
	public void removeAncestor(NodeHandle node) {
		if (ancestorList != null && ancestorList.contains(node)) {
			ancestorList.remove(node);
		}
	}

	/**
	 * 
	 * @return - returns ancestor
	 */
	public Vector<NodeHandle> getAncestorsList() {
		return ancestorList;
	}

	@SuppressWarnings("unchecked")
	/**
	 * initialize ancestorList with a given ancestor list
	 * @param ancestors - list to be cloned
	 */
	public void initAncestors(Vector<NodeHandle> ancestors) {
		if (ancestorList != null)
			ancestorList.removeAllElements();
		ancestorList = (Vector<NodeHandle>) ancestors.clone();
	}

	/**
	 * to check a node is ancestor or not
	 * 
	 * @param handle
	 *            - handle of the node to check
	 * @return - returns true if the given node is an ancestor
	 */
	public boolean isAncestor(NodeHandle handle) {
		if (ancestorList != null) {
			return ancestorList.contains(handle);
		}
		return false;
	}
	
	/**
	 * print ancestor list
	 */
	public void printAncestors() {
		System.out.println(".....Printing Ancestor List....");
		if (ancestorList != null) {
			for (NodeHandle node : ancestorList) {
				System.out.println(node);
			}
		}
	}
}
