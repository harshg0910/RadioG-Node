import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Level;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;
import rice.pastry.routing.RouteSet;

public class RadioApp implements Application {

	private static RadioApp radioApp = null; // singleton instance of RadioApp
	protected static Endpoint endpoint = null; // Endpoint of this node in the
												// network
	public boolean hasStream = false; // flag indicating the availibilty of the
										// stream
	private String LocalIPAddress; // local IP address
	private PastryNode node = null; // pastrynode instance of this node
	private static boolean isServerAlive = false; // flag to indicate server
													// aliveness
	private CheckLIveness livenessChecker;
	private Listeners listeners;
	private Object lock = new Object();

	/* Streaming Server Variables */
	private int VLCStreamingPort = 7456; // port at which VLC Server will listen
	private static NodeHandle VLCStreamingServer; // node handle of the
													// streaming server
	@SuppressWarnings("unused")
	private int bindport; // port at which application is bound

	public static boolean ServerFound = false;
	public boolean isAlreadySearching = false; // true if the node is already
												// searching for a server
												// prevents from multiple
												// concurrent search attempt
	private int lastCheckedServer = 0; // clock hand for the last checked server
	private int rowOffset = 0;
	private int MAX_ROW_OFFSET = 3;

	public static long streamStartedAt = 0; // streaming start time
	private String VLCServerStream = ""; // MRL of streaming server
	public FreeStreamers freeStreamers;

	private static Ancestors ancestors; // Ancestors of the node in streaming
										// tree
	private long serverLatency = 0; // server's latency
	private long PingTime = 0;
	private long PongTime = 0;

	private static Id bootstrapNodeID = null;
	private short attempt = 0;

	private int totalUserCount = 0;
	private int currentUserCount = 0;

	/*
	 * Returns insance of RadioApp
	 */
	public static RadioApp getRadioApp() {
		if (radioApp != null) {
			return radioApp;
		}
		return null;
	}

	public Ancestors getAncestors() {
		return ancestors;
	}

	public Id getBootstrapNodeId() {
		return bootstrapNodeID;
	}

	/*
	 * Start listening from the src and streaming simultaneously
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

	/*
	 * Returns node handle of the streaming server
	 */
	public NodeHandle getStreamingServer() {
		return VLCStreamingServer;
	}

	public RadioApp(PastryNode node, int VLCStreamingPort, int bindPort)
			throws IOException {
		freeStreamers = new FreeStreamers();
		radioApp = this;
		// We are only going to use one instance of this application on each
		// PastryNode
		RadioApp.endpoint = node.buildEndpoint(this, "myinstance");
		this.node = node;
		this.VLCStreamingPort = VLCStreamingPort;
		this.bindport = bindPort;
		// the rest of the initialization code could go here

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
		if (localhost.isLoopbackAddress()) {
			Socket s;
			s = new Socket("202.141.80.14", 80);
			localhost = s.getLocalAddress();
			System.out.println(s.getLocalAddress().getHostAddress());
			s.close();
		}
		LocalIPAddress = localhost.getHostAddress();

	}

	/*
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
				if (!RadioNode.isBootStrapNode && hasStream
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
					 * prepare and send stream rejection
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

					VLCServerStream = "mmsh://" + synMsg.getIP() + ":"
							+ synMsg.getVLCPort(); // prepare streaming url
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
				Radio.logger.log(Level.INFO, "Sending ancestor list to"
						+ synMsg.getHandle());
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
			case SEND_STREAM:
				if (RadioNode.isBootStrapNode) {
					NodeHandle freeNode = freeStreamers.getFreeStreamer(synMsg
							.getAttempt());
					SyncMessage reply = new SyncMessage();
					reply.setHandle(freeNode);
					reply.setType(SyncMessage.Type.FREE_STREAM);
					replyMessage(synMsg, reply);
					Radio.logger.log(Level.INFO, "Sending Free node "
							+ freeNode + " to " + synMsg.getHandle());
				}
				break;
			case FREE_STREAM:

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
			} else if (hb.type == HeartBeat.Type.DYING) {
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
			Radio.logger.log(Level.INFO, "Ancestor List Received");
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

	@Override
	public void update(NodeHandle handle, boolean joined) {
		if (joined) {
			Radio.logger.log(Level.INFO, "Node Joined " + handle);
			System.out.println("New node " + handle);
			totalUserCount++;
			currentUserCount++;
			Radio.setCount(totalUserCount, currentUserCount);
			//change count value in the gui
			
		}  {
			
			Radio.logger.log(Level.INFO, "Node Left " + handle);
			System.out.println("Node Left " + handle);
			currentUserCount--;
			// Bootstrap node will maintain the details of all nodes
			if (RadioNode.isBootStrapNode)
				freeStreamers.removeNode(handle);

			if (handle == VLCStreamingServer) {
				hasStream = false;
				Player.stopServer();
				Player.stopListening();
				Radio.logger.log(Level.INFO, "Streaming Server Dead " + handle);
				System.out.println("Steaming Server Left");
				RadioNode.getRadioNode().updateLeafSet();
				try {
					setUpServerSearch();
					sendStreamRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (listeners.isClient(handle)) {
				Radio.logger.log(Level.INFO, "Client left " + handle);
				listeners.removeClient(handle);
			}
		}
	}

	// Do call updateLeafSet before this function
	// also Make serverfound = false
	public void sendStreamRequest() throws Exception {

		if (!RadioNode.isBootStrapNode && !ServerFound && !isAlreadySearching) {
			isAlreadySearching = true;

			NodeHandle candidateServer = getCandiadteServer();

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

				SyncMessage msg = new SyncMessage();
				msg.setType(SyncMessage.Type.SEND_STREAM);
				msg.setHandle(endpoint.getLocalNodeHandle());
				msg.setAttempt(attempt);
				endpoint.route(bootstrapNodeID, msg, null);
				attempt++;
			}
			isAlreadySearching = false;
		}
	}

	private boolean validateCandidateServer(NodeHandle node) {
		/*
		 * Conditions to be satisfied 1. should not be same as the node itself
		 * 2. Should not be one of the receiving clients
		 */
		return (node != this.node.getLocalNodeHandle() && !listeners
				.getListeningClients().contains(node));
	}

	public NodeHandle getCandiadteServer() {
		try {
			NodeHandle candidateServer = null;
			if (rowOffset >= MAX_ROW_OFFSET) {
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

	public boolean checkServerLiveness() {
		if (VLCStreamingServer != null && endpoint.isAlive(VLCStreamingServer)) {
			return true;
		}
		return false;
	}

	public void sendMessage(Id id, Message msg) {
		endpoint.route(id, msg, null);
	}

	public String toString() {
		return "End Point ID  " + endpoint.getId();
	}

	public boolean isServerAlive() {
		return isServerAlive;
	}

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
		}
	}

	public void setUpServerSearch() {
		hasStream = false;
		ServerFound = false;
		setServerAlive(false);
		Radio.upTime = 0;
		Player.stopServer();
		Player.stopListening();
	}


	
}
