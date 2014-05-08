import java.util.Vector;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * This class is implements ancestor messages, send to all the clients.
 * 
 * @author Abhi
 * 
 */
@SuppressWarnings("serial")
public class AncestorMessage implements Message {
	/**
	 * @param ancestorList
	 *            - list of ancestors
	 */
	private Vector<NodeHandle> ancestorList;
	/**
	 * End-to-end delay = RTT/2 RTT - Send a ping to parent and then wait for a
	 * pong from it
	 */
	private long delay;

	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param ancestors - current node's own ancestor list 
	 * @param node - node handle of the current node.
	 * @param delay - end to end delay
	 */
	public AncestorMessage(Ancestors ancestors, NodeHandle node, long delay) {
		this.ancestorList = (Vector<NodeHandle>) ancestors.getAncestorsList()
				.clone();	//init ancestor list with ancestors
		this.ancestorList.add(node);	//add this node to ancestor list for child
		this.delay = delay; 
	}

	/**
	 * get ancetor list in the message
	 * 
	 * @return returns ancetor list recieved from parent
	 */
	public Vector<NodeHandle> getAncestorList() {
		return ancestorList;
	}

	/**
	 * get delay in the message
	 * 
	 * @return returns delay in the message
	 */
	public long getDelay() {
		return delay;
	}

	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

}
