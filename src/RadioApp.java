import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;
import rice.pastry.routing.RouteSet;

public class RadioApp implements Application {

	/**
	 * Singleton instance of RadioApp
	 */
	private static RadioApp radioApp = null;

	/**
	 * Endpoint of this node in the network.
	 */
	protected static Endpoint endpoint = null;

	/**
	 * Flag indication the availability of the parent stream
	 */
	public boolean hasStream = false;

	/**
	 * Local IP Address of the system
	 */
	private String LocalIPAddress;

	/**
	 * PastryNode instance of for node
	 */
	private PastryNode node = null;

	/**
	 * Flag to indicate whether the parent is alive
	 */
	private static boolean isServerAlive = false;

	private CheckLIveness livenessChecker;
	private Listeners listeners;
	private Object lock = new Object();

	/* Streaming Server Variables */

	/**
	 * Port at which VLC will stream
	 */
	private int VLCStreamingPort = 7456;

	/**
	 * Node handle of the parent from which this node is getting the stream
	 */
	private static NodeHandle VLCStreamingServer;

	@SuppressWarnings("unused")
	/**
	 * Port at which application is bound
	 */
	private static int bindport;

	public static boolean ServerFound = false;

	/**
	 * True if the node is already searching for a server. It prevents from
	 * multiple concurrent search attempt.
	 */
	public boolean isAlreadySearching = false;

	/**
	 * Clock hand for the last checked server
	 */
	private int lastCheckedServer = 0;

	/**
	 * Current row of the routing table
	 */
	private int rowOffset = 0;

	/**
	 * Maximum number of rows to be checked
	 */
	private int MAX_ROW_OFFSET = 3;

	/**
	 * Streaming start time
	 */
	public static long streamStartedAt = 0;

	/**
	 * URL of streaming parent. It is of the form of mmsh://IP:Port
	 */
	private String VLCServerStream = "";

	/**
	 * Data structure used by bootstrap to maintain free nodes
	 */
	public FreeStreamers freeStreamers;

	/**
	 * Ancestors of the node in the streaming tree
	 */
	private static Ancestors ancestors;

	private long serverLatency = 0; // server's latency
	private long PingTime = 0;
	private long PongTime = 0;

	/**
	 * ID of the bootstrap node
	 */
	private static Id bootstrapNodeID = null;

	/**
	 * To get attempt-th free node from bootstrap when client fails to get
	 * stream from all entries from routing table and attempt-1 nodes sent by
	 * bootstrap
	 */
	private short attempt = 0;

	private int totalUserCount = 0;
	private int currentUserCount = 0;

	/**
	 * Returns instance of RadioApp
	 */
	public static RadioApp getRadioApp() {
		if (radioApp != null) {
			return radioApp;
		}
		return null;
	}

	/**
	 * 
	 * @return - The ancestor list
	 */
	public Ancestors getAncestors() {
		return ancestors;
	}

	public Id getBootstrapNodeId() {
		return bootstrapNodeID;
	}

	/***
	 * Start listening from the src and streaming simultaneously
	 * 
	 * @param src
	 *            Path to the file or a network stream
	 */
	public void setStream(String src) {
		hasStream = true;
		Player.startVLCStreaming(LocalIPAddress, VLCStreamingPort, src);
		Radio.logger.log(Level.CONFIG, "Receiving_Stream " + src);
		Radio.logger.log(Level.CONFIG, "Streaming_Port " + VLCStreamingPort);
		System.out.println("Streaming at port " + VLCStreamingPort);
		Player.startListen(src);
	}

	public String getLocalIP() {
		return LocalIPAddress;
	}

	/**
	 * @return Node handle of the streaming server
	 */
	public NodeHandle getStreamingServer() {
		return VLCStreamingServer;
	}

	/**
	 * Intialize radio application instance
	 * 
	 * @param node
	 *            - Instance of the local pastry node
	 * @param VLCStreamingPort
	 *            - Port at which VLC will start streaming
	 * @param bindPort
	 *            - Port at which application binds
	 * @throws IOException
	 */
	public RadioApp(PastryNode node, int VLCStreamingPort, int bindPort)
			throws IOException {

		freeStreamers = new FreeStreamers();
		radioApp = this;
		// We are only going to use one instance of this application on each
		// PastryNode
		RadioApp.endpoint = node.buildEndpoint(this, "myinstance");
		this.node = node;
		this.VLCStreamingPort = VLCStreamingPort;
		RadioApp.bindport = bindPort;

		// now we can receive messages
		RadioApp.endpoint.register();

		bootstrapNodeID = node.getId();

		// listener instance
		listeners = new Listeners();

		livenessChecker = new CheckLIveness();
		if (ancestors == null) {
			ancestors = new Ancestors();
		}

		// initialize the ancestor of bootstrap node as nill
		if (RadioNode.isBootStrapNode) {
			ancestors.initAncestors(new Vector<NodeHandle>());
			freeStreamers.addNode(node.getLocalNodeHandle(), 0);
		}

		InetAddress localhost = InetAddress.getLocalHost();

		// This returns the IP address according to the packets received by
		// external URL
		if (localhost.isLoopbackAddress()) {
			Socket s;
			s = new Socket(Configure.getSetting("CheckURL"), 80);
			localhost = s.getLocalAddress();
			System.out.println(s.getLocalAddress().getHostAddress());
			s.close();
		}
		LocalIPAddress = localhost.getHostAddress();
	}

	/**
	 * Start liveness check for the servers
	 */
	public void startLivenessCheck() {
		livenessChecker.startLivenessCheck();
	}

	@Override
	public void deliver(Id id, Message msg) {

		if (msg instanceof SyncMessage) {
			SyncMessage synMsg = (SyncMessage) msg;
			System.out.println("Got message " + synMsg.getType());
			switch (synMsg.getType()) {
			case STREAM_REQUEST:
				// Forward the stream if I have one
				Radio.logger.log(Level.INFO,
						"Stream_Request from " + synMsg.getHandle());
				if (hasStream
						&& listeners.getNoOfListeners() < Listeners.MAX_LISTENER
						&& !ancestors.isAncestor(synMsg.getHandle())) {

					/*
					 * Prepare and send stream offer
					 */
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setVLCPort(VLCStreamingPort);
					reply.setType(SyncMessage.Type.STREAM_OFFER);
					reply.setBootstrapNodeID(bootstrapNodeID);
					replyMessage(synMsg, reply);
					Radio.logger.log(Level.INFO,
							"Offering_to " + synMsg.getHandle());
					System.out.println("Offering Stream to " + synMsg.getIP());
				} else {

					/*
					 * Prepare and send stream rejection
					 */
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setVLCPort(VLCStreamingPort);
					reply.setType(SyncMessage.Type.STREAM_REJECT);
					replyMessage(synMsg, reply);
					Radio.logger.log(Level.INFO,
							"Rejecting " + synMsg.getHandle());
					System.out
							.println("Rejecting request of " + synMsg.getIP());
				}
				break;

			case STREAM_OFFER:
				if (!ServerFound) {
					// prepare streaming url
					VLCServerStream = "mmsh://" + synMsg.getIP() + ":"
							+ synMsg.getVLCPort();
					VLCStreamingServer = synMsg.getHandle();
					Radio.setGetStreamLabel(((SyncMessage) msg).getHandle()
							.toString());
					setStream(VLCServerStream);
					bootstrapNodeID = synMsg.getBootstrapNodeID();

					/*
					 * Prepare and send stream acceptance
					 */
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setType(SyncMessage.Type.STREAM_ACCEPT);

					replyMessage(synMsg, reply);

					Radio.logger.log(Level.INFO,
							"Accepting " + synMsg.getHandle());
					System.out.println("------------Accepting stream from "
							+ synMsg.getHandle());

					// Sending message to bootstrap to be recognized as free
					// node
					StreamUpdateMessage uMsg = new StreamUpdateMessage();
					uMsg.setInfo(StreamUpdateMessage.Type.STREAM_FREE);
					uMsg.setLevel(ancestors.getLevel());
					uMsg.setNode(node.getLocalNodeHandle());
					endpoint.route(bootstrapNodeID, uMsg, null);

					// Set variables
					ServerFound = true;
					setServerAlive(true);
					lastCheckedServer = 0;
					rowOffset = 0;
					serverLatency = 0;
					attempt = 0;
					checkDelay(VLCStreamingServer);
				}
				break;

			case STREAM_ACCEPT:
				listeners.addClient(synMsg.getHandle());
				Radio.logger.log(Level.INFO,
						"Streaming to " + synMsg.getHandle());
				System.out.println("-----------Streaming to "
						+ synMsg.getHandle());

				/*
				 * Prepare and send ancestor list to the child
				 */

				// Radio.logger.log(Level.INFO, "Sending ancestor list to"
				// + synMsg.getHandle());
				AncestorMessage ancMsg = new AncestorMessage(ancestors,
						node.getLocalNodeHandle(), serverLatency);
				replyMessage(synMsg, ancMsg);
				break;

			case STREAM_REJECT:
				Radio.logger.log(Level.INFO,
						"Rejected by " + synMsg.getHandle());
				try {
					sendStreamRequest();
				} catch (Exception e) {
					Radio.logger.log(Level.SEVERE, e.getMessage());
					e.printStackTrace();
				}
			case SEND_FREE_NODE_INFO:
				/*
				 * send free node to requester
				 */
				if (RadioNode.isBootStrapNode) {
					NodeHandle freeNode = freeStreamers.getFreeStreamer(synMsg
							.getAttempt());
					SyncMessage reply = new SyncMessage();
					reply.setHandle(freeNode);
					reply.setType(SyncMessage.Type.FREE_NODE_INFO);
					replyMessage(synMsg, reply);
					// Radio.logger.log(Level.INFO, "Sending Free node "
					// + freeNode + " to " + synMsg.getHandle());
				}
				break;
			case FREE_NODE_INFO:

				// Send request to the node replied by the bootstrap server
				if (synMsg.getHandle() == null) {
					lastCheckedServer = 0;
					rowOffset = 0;
					attempt = 0;

					try {
						// You have exhausted all the possible paths to find
						// free nodes.
						// Now wait quietly for the salvation of the lord.
						// May God guide you for your next search.
						endpoint.getEnvironment().getTimeSource().sleep(1000);
						sendStreamRequest();
					} catch (Exception e) {
						Radio.logger.log(Level.SEVERE, e.getMessage());
						e.printStackTrace();
					}

				} else if (validateCandidateServer(synMsg.getHandle())) {
					/*
					 * Send STREAM_REQUEST to the received candidate parent
					 */
					SyncMessage msgRequest = new SyncMessage();
					msgRequest.setIP(getLocalIP());
					msgRequest.setType(SyncMessage.Type.STREAM_REQUEST);
					msgRequest.setHandle(endpoint.getLocalNodeHandle());
					Radio.logger.log(Level.INFO,
							"Got Free node " + synMsg.getHandle()
									+ " from bootstrap.");
					Radio.logger.log(Level.INFO,
							"Sending Request to " + synMsg.getHandle());

					System.out.println("Sending request for stream to "
							+ synMsg.getHandle());
					sendMessage(synMsg.getHandle().getId(), msg);
				} else
					try {
						sendStreamRequest();
					} catch (Exception e) {
						Radio.logger.log(Level.SEVERE, e.getMessage());
						e.printStackTrace();
					}
				break;
			case CLIENT_DYING:
				Radio.logger.log(Level.INFO,
						"Client left " + synMsg.getHandle());
				listeners.removeClient(synMsg.getHandle());
				break;
			default:
				break;
			}
		} else if (msg instanceof HeartBeat) {
			HeartBeat hb = (HeartBeat) msg;
			if (hb.type == HeartBeat.Type.ALIVE) {
				setServerAlive(true);
			} else if (hb.type == HeartBeat.Type.DYING
					&& !RadioNode.isSurrogate) {
				Radio.logger.log(Level.INFO, "Server leaving..");
				setUpServerSearch();
				try {
					sendStreamRequest();
				} catch (Exception e) {
					Radio.logger.log(Level.SEVERE, e.getMessage());
					e.printStackTrace();
				}
			}
		} else if (msg instanceof StreamUpdateMessage) {
			StreamUpdateMessage uMsg = (StreamUpdateMessage) msg;
			switch (uMsg.info) {
			case STREAM_FREE:
				freeStreamers.addNode(uMsg.getNode(), uMsg.getLevel());
				break;
			case STREAM_FULL:
				freeStreamers.removeNode(uMsg.getNode());
				break;
			case LEVEL_UPDATE:
				freeStreamers.updateNode(uMsg.getNode(), uMsg.getLevel());
				break;
			default:
				break;
			}
		} else if (msg instanceof AncestorMessage) {
			// Radio.logger.log(Level.INFO, "Ancestor List Received");
			AncestorMessage ancMsg = (AncestorMessage) msg;
			ancestors.initAncestors(ancMsg.getAncestorList());
			ancestors.printAncestors();
			listeners.broadCastAncestor(ancestors, node.getLocalNodeHandle());
			serverLatency += ancMsg.getDelay();

			// Update ancestor list to bootstrap
			if (listeners.getNoOfListeners() < Listeners.MAX_LISTENER) {
				StreamUpdateMessage uMsg = new StreamUpdateMessage();
				uMsg.setInfo(StreamUpdateMessage.Type.LEVEL_UPDATE);
				uMsg.setLevel(RadioApp.getRadioApp().getAncestors().getLevel());
				uMsg.setNode(RadioApp.endpoint.getLocalNodeHandle());
				RadioApp.endpoint.route(RadioApp.getRadioApp()
						.getBootstrapNodeId(), uMsg, null);
			}

		} else if (msg instanceof PingPong) {
			PingPong pingpong = (PingPong) msg;
			if (pingpong.getType() == PingPong.Type.PING) {
				Radio.logger.log(Level.INFO, "Ping message received from "
						+ pingpong.getHandle() + "at " + PongTime);
				PingPong pong = new PingPong(PingPong.Type.PONG,
						node.getLocalNodeHandle());
				endpoint.route(null, pong, pingpong.getHandle());
				Radio.logger.log(Level.INFO,
						"Pong message sent to " + pingpong.getHandle());
			} else if (pingpong.getType() == PingPong.Type.PONG) {
				PongTime = endpoint.getEnvironment().getTimeSource()
						.currentTimeMillis();
				serverLatency += (PongTime - PingTime) / 2;
				Radio.logger.log(Level.INFO, "Pong message received at "
						+ PongTime);
				Radio.logger.log(Level.INFO, "End to end delay "
						+ serverLatency);
			}
		}

	}

	@Override
	public boolean forward(RouteMessage arg0) {
		return true;
	}

	public void replyMessage(Message incoming, Message reply) {
		Id to = ((SyncMessage) incoming).getHandle().getId();
		endpoint.route(to, reply, null);
	}

	/**
	 * This function is called when a node leaves or joins the node.
	 * 
	 * @Override
	 */
	public void update(NodeHandle handle, boolean joined) {
		if (joined) {
			Radio.logger.log(Level.INFO, "Node Joined " + handle);
			System.out.println("New node " + handle);
			totalUserCount++;
			currentUserCount++;
			Radio.setCount(totalUserCount, currentUserCount);
			// change count value in the gui

		} else {

			Radio.logger.log(Level.INFO, "Node Left " + handle);
			System.out.println("Node Left " + handle);
			currentUserCount--;
			// Bootstrap node will maintain the details of all nodes
			if (RadioNode.isBootStrapNode)
				freeStreamers.removeNode(handle);
			/**
			 * If the node left is parent of this node then look for a new
			 * parent.
			 */
			if (handle == VLCStreamingServer) {
				hasStream = false;
				Player.stopServer();
				Player.stopListening();
				Radio.logger.log(Level.INFO, "Streaming Server Dead " + handle);
				System.out.println("Steaming Server Left");
				try {
					/**
					 * Set up server seraching options
					 */
					setUpServerSearch();
					sendStreamRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			/**
			 * if the node left is one of the client
			 */
			else if (listeners.isClient(handle)) {
				Radio.logger.log(Level.INFO, "Client left " + handle);
				listeners.removeClient(handle);
			}
		}
	}

	/**
	 * Sends stream request to a candidate node First explores all the node in
	 * last MAX_NUM_ROWS rows of routing table, if could not find any stream
	 * offer then requests bootstrap server for a free node.
	 * 
	 * @throws Exception
	 */
	public void sendStreamRequest() throws Exception {

		if (!RadioNode.isBootStrapNode && !ServerFound && !isAlreadySearching) {
			isAlreadySearching = true;

			NodeHandle candidateServer = getCandiadteServer();

			/**
			 * candidateServer is null then it means the routing table is
			 * exaushted now ask bootstrap node for free node
			 */
			if (candidateServer != null) {
				if (validateCandidateServer(candidateServer)) {
					SyncMessage msg = new SyncMessage();
					msg.setIP(getLocalIP());
					msg.setType(SyncMessage.Type.STREAM_REQUEST);
					msg.setHandle(endpoint.getLocalNodeHandle());
					Radio.logger.log(Level.INFO, "Sending Request to "
							+ candidateServer);
					System.out.println("Sending request for stream to "
							+ candidateServer);
					sendMessage(candidateServer.getId(), msg);
				} else {
					isAlreadySearching = false;
					sendStreamRequest();
				}
			} else {
				// We didn't get the stream using raouting table
				// Asking bootstrap for free node

				SyncMessage msg = new SyncMessage();
				msg.setType(SyncMessage.Type.SEND_FREE_NODE_INFO);
				msg.setHandle(endpoint.getLocalNodeHandle());
				msg.setAttempt(attempt);
				endpoint.route(bootstrapNodeID, msg, null);
				attempt++;
			}
			isAlreadySearching = false;
		}
	}

	/**
	 * Conditions to be satisfied 1. should not be same as the node itself 2.
	 * Should not be one of the receiving clients
	 */
	private boolean validateCandidateServer(NodeHandle node) {
		return (node != this.node.getLocalNodeHandle() && !listeners
				.getListeningClients().contains(node));
	}

	/**
	 * Iterate last MAX_ROW_OFFSET of routing table to get a candidate parent
	 * 
	 * @return NodeHandle of the candidate node
	 */
	public NodeHandle getCandiadteServer() {
		try {
			NodeHandle candidateServer = null;

			if (rowOffset >= MAX_ROW_OFFSET) {
				// Searched through routng table
				return null;
			}
			RouteSet row[] = node.getRoutingTable().getRow(
					node.getRoutingTable().numRows() - rowOffset - 1);

			if (lastCheckedServer < row.length) {
				System.out.println(lastCheckedServer + " " + row.length);
				RouteSet entry = row[lastCheckedServer];
				while (entry == null) {
					lastCheckedServer++;
					if (lastCheckedServer >= row.length) {
						lastCheckedServer = 0;
						rowOffset++;
						return getCandiadteServer();
					} else {
						entry = row[lastCheckedServer];
					}
				}
				if (!entry.isEmpty())
					candidateServer = entry.getHandle(0);
				if (candidateServer == null) {
					lastCheckedServer++;
					return getCandiadteServer();
				}
				lastCheckedServer++;
				return candidateServer;
			} else {
				lastCheckedServer = 0;
				rowOffset++;
				return getCandiadteServer();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Send message to node with nodeID = id
	 * @param id - Destination nodeID
	 * @param msg - message to send
	 */
	public void sendMessage(Id id, Message msg) {
		endpoint.route(id, msg, null);
	}

	public String toString() {
		return "End Point ID  " + endpoint.getId();
	}

	public boolean isServerAlive() {
		return isServerAlive;
	}
	
	/**
	 * Set ServerAlive to val
	 * @param val value to set
	 */
	public void setServerAlive(boolean val) {
		synchronized (lock) {
			isServerAlive = val;
		}
	}

	public void checkDelay(NodeHandle handle) {
		PingPong ping = new PingPong(PingPong.Type.PING,
				node.getLocalNodeHandle());
		PingTime = endpoint.getEnvironment().getTimeSource()
				.currentTimeMillis();
		Radio.logger.log(Level.INFO, "Send ping to " + handle + " at "
				+ PingTime);
		endpoint.route(null, ping, handle); // send message directly to handle
	}

	public String getVLCServerStream() {
		return VLCServerStream;
	}

	/**
	 * Pre connection close tasks
	 */
	public static void close_connection() {
		if (endpoint != null) {
			// tell children i am dying
			Radio.logger.log(Level.INFO, "Telling clients i am dying");
			Listeners.getListener().sendHeartBeat(HeartBeat.Type.DYING);
			// tell server i am dying
			Radio.logger.log(Level.INFO, "Telling server i am dying");
			SyncMessage CDmsg = new SyncMessage();
			CDmsg.setType(SyncMessage.Type.CLIENT_DYING);
			CDmsg.setHandle(endpoint.getLocalNodeHandle());
			if (!RadioNode.isBootStrapNode) {
				endpoint.route(null, CDmsg, VLCStreamingServer);
				// tell bootstrap i am dying if i have some free slot
				// then only i can have slot there
				Radio.logger.log(Level.INFO, "Telling bootstrap i am dying");
				if (Listeners.getListener().getNoOfListeners() < Listeners.MAX_LISTENER) {
					StreamUpdateMessage uMsg = new StreamUpdateMessage();
					uMsg.setInfo(StreamUpdateMessage.Type.STREAM_FULL);
					uMsg.setLevel(getRadioApp().getAncestors().getLevel());
					uMsg.setNode(endpoint.getLocalNodeHandle());
					RadioApp.endpoint.route(RadioApp.getRadioApp()
							.getBootstrapNodeId(), uMsg, null);
				}
			}
			LogManager.getLogManager().reset();
		}
		sendLogs();
	}

	/**
	 * Called before searching for a streaming server
	 */
	public void setUpServerSearch() {
		hasStream = false;
		ServerFound = false;
		setServerAlive(false);
		Radio.upTime = 0;
		Player.stopServer();
		Player.stopListening();
	}

	/**
	 * Sends the log files to the server at the address and port configured in
	 * config.param
	 */
	public static void sendLogs() {
		Socket sock;
		try {
			sock = new Socket(Configure.getSetting("SendLogsIP"),
					Integer.parseInt(Configure.getSetting("SendLogsPort")));
			System.out.println("Connecting...");
			OutputStream os = sock.getOutputStream();
			send(os);
			long end = System.currentTimeMillis();
			sock.close();
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	/**
	 * Used by sendLogs as a helper function
	 * 
	 * @param os
	 *            - Output Stream
	 * @throws Exception
	 */
	public static void send(OutputStream os) throws Exception {
		File myFile = new File("logger" + bindport + ".xml");
		System.out.println(myFile.getAbsolutePath());
		if (myFile.exists()) {
			byte[] mybytearray = new byte[(int) myFile.length() + 1];
			FileInputStream fis = new FileInputStream(myFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(mybytearray, 0, mybytearray.length);
			System.out.println("Sending...");
			os.write(mybytearray, 0, mybytearray.length);
			os.flush();
		} else {
			System.out.println("Not exists");
		}
	}

}
