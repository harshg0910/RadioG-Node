import java.util.Properties;
import java.util.Vector;

import rice.p2p.commonapi.NodeHandle;

/**
 * Implements all the listening clients of a node. Max Limit on the number of
 * clients is 3. It can be reset as per requirement. Here insertion and deletion
 * in the list needed to be synchronized as they are shared among main thread
 * and liveness check thread.
 * 
 * @author Abhi
 * 
 */
public class Listeners {
	/**
	 * Vector of currently listening clients
	 */

	private Vector<NodeHandle> listeningClients = new Vector<>();
	/**
	 * Max limit on the number of listener
	 */
	public static final int MAX_LISTENER = Configure.MAX_LISTENERS;
	/**
	 * current number of listeners
	 */
	private int noOfListener = 0;
	/**
	 * Current instance of Listeners. To keep class singleton
	 */
	private static Listeners listeners = null;

	/**
	 * Lock for shared variable listeningClients
	 */
	private Object Lock = new Object();

	public Listeners() {
		listeners = this;
	}

	/**
	 * Adds a new client in the client list
	 * 
	 * @param client
	 *            - client to be added
	 */
	public void addClient(NodeHandle client) {
		/**
		 * add only when number of clients are less than max listener
		 */
		if (!listeningClients.contains(client) && noOfListener < MAX_LISTENER) {
			/**
			 * Lock used for synchronizing shared variable listeningClients
			 */
			synchronized (Lock) {
				listeningClients.add(client);
				noOfListener++;
				/**
				 * If all slots are used notify the bootstrap node, so that it
				 * can remove this node's entry from free node list
				 */
				if (noOfListener == MAX_LISTENER) {
					/**
					 * Sending message to bootstrap to be recognized as free
					 * node
					 */
					StreamUpdateMessage uMsg = new StreamUpdateMessage();
					uMsg.setInfo(StreamUpdateMessage.Type.STREAM_FULL);
					uMsg.setLevel(RadioApp.getRadioApp().getAncestors()
							.getLevel());
					uMsg.setNode(RadioApp.endpoint.getLocalNodeHandle());
					RadioApp.endpoint.route(RadioApp.getRadioApp()
							.getBootstrapNodeId(), uMsg, null);
				}
			}
			System.out.println("Client " + client + " added");
			System.out.println("Current Clients " + listeningClients + " "
					+ noOfListener);
			/**
			 * Refresh the GUI component for showing listening clients
			 */
			Radio.refrestClientList(listeningClients);

		} else {
			System.out.println("Max listener limit reached");
		}

	}

	/**
	 * remove a client from client list
	 * 
	 * @param client
	 *            - cleint to be removed
	 */
	public void removeClient(NodeHandle client) {
		System.out.print("Removing " + client);
		synchronized (Lock) {
			if (listeningClients.removeElement(client)) {
				noOfListener--;
				Radio.refrestClientList(listeningClients);
				System.out.println("Client " + client + " removed");
				/**
				 * If a slot has become free and list was full earlier send
				 * bootstrap node a message to add this node in free node list
				 */
				if (noOfListener == MAX_LISTENER - 1) {
					// Sending message to bootstrap to be recognized as free
					// node
					StreamUpdateMessage uMsg = new StreamUpdateMessage();
					uMsg.setInfo(StreamUpdateMessage.Type.STREAM_FREE);
					uMsg.setLevel(RadioApp.getRadioApp().getAncestors()
							.getLevel());
					uMsg.setNode(RadioApp.endpoint.getLocalNodeHandle());
					RadioApp.endpoint.route(RadioApp.getRadioApp()
							.getBootstrapNodeId(), uMsg, null);
				}
			} else {
				System.out.println("Client " + client + " not removed");
			}
		}
	}

	/**
	 * update client list by looking for dead cleints. Not implemented yet.
	 */
	public void update() {
		System.out.println("Updating client list " + listeningClients + " "
				+ noOfListener);
		for (int i = 0; i < noOfListener; i++) {
			/* removing dead clients */
			NodeHandle client = listeningClients.get(i);
			System.out.println("Checking for " + client);
			if (!RadioApp.endpoint.isAlive(client)) {
				System.out.println("Client: " + client + " is dead");
				removeClient(client);
			}
		}
	}

	/**
	 * send heartbeat to all the clients
	 * 
	 * @param type
	 *            - type of the heartbeat message.
	 */
	public void sendHeartBeat(HeartBeat.Type type) {
		for (int i = 0; i < noOfListener; i++) {
			HeartBeat heartBeat = new HeartBeat(type);
			RadioApp.endpoint.route(null, heartBeat, listeningClients.get(i));
		}
	}

	/**
	 * @return returns number of clients listening
	 */
	public int getNoOfListeners() {
		return noOfListener;
	}

	/**
	 * 
	 * @return returns current instance of listeners
	 */
	public static Listeners getListener() {
		if (listeners == null) {
			listeners = new Listeners();
		}
		return listeners;

	}

	/**
	 * checks if handle is a cleint or not.
	 * 
	 * @param handle
	 *            - client to be checked
	 * @return returns true if client is in list. otherwise false.
	 */
	public boolean isClient(NodeHandle handle) {
		return listeningClients.contains(handle);
	}

	public Vector<NodeHandle> getListeningClients() {
		return listeningClients;
	}

	public void broadCastAncestor(Ancestors ancestors, NodeHandle handle) {
		if (listeningClients.size() > 0) {
			AncestorMessage ancMsg = new AncestorMessage(ancestors, handle, 0);
			for (NodeHandle client : listeningClients) {
				RadioApp.getRadioApp();
				RadioApp.endpoint.route(null, ancMsg, client);
			}
		}
	}
}
