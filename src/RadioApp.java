import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;

/*
 * pange :
 * 1. lister <=> steramer
 * 2. backup free nodes
 * 3. flash crowd
 * 4. rejecting stream bug
 * 
 * 
 */

public class RadioApp implements Application {

	private static RadioApp radioApp = null;
	protected Endpoint endpoint;
	public boolean hasStream = false;
	private String LocalIPAddress;
	public static int MAX_LISTENER = 3;
	private PastryNode node;
	private static boolean isServerAlive = false;
	private CheckLIveness livenessChecker;
	private Listeners listeners;
	private Object lock = new Object();
	/*Streaming Server Variables*/
	private int VLCStreamingPort = 7456;
	private static NodeHandle VLCStreamingServer;
	private int bindport;
//	private String vlcPath = "";
	public static boolean ServerFound = false;
	private int lastCheckedServer ;
	public boolean isAlreadySearching = false;
	public static long streamStartedAt = 0; 
	
	private String VLCServerStream = "";

	public static RadioApp getRadioApp(){
		if(radioApp!=null){
			return radioApp;
		}
		return null;
	}
	
	public void setStream(String src) {
		hasStream = true;
		Player.startVLCStreaming(LocalIPAddress, VLCStreamingPort, src);
		Radio.logger.log(Level.CONFIG,"Receiving_Stream "+src);
		Radio.logger.log(Level.CONFIG,"Streaming_Port " + VLCStreamingPort);
		System.out.println("Streaming at port " + VLCStreamingPort);
		Player.startListen(src);
	}

	public String getLocalIP() {
		return LocalIPAddress;
	}

	public NodeHandle getStreamingServer(){
		return VLCStreamingServer;
	}

	public RadioApp(PastryNode node, int VLCStreamingPort, int bindPort)
			throws IOException {
		
		radioApp = this;
		// We are only going to use one instance of this application on each
		// PastryNode
		this.endpoint = node.buildEndpoint(this, "myinstance");
		this.node = node;
		this.VLCStreamingPort = VLCStreamingPort;
		this.bindport = bindPort;
		lastCheckedServer =  -RadioNode.getRadioNode().leafSet.ccwSize();
		// the rest of the initialization code could go here

		// now we can receive messages
		this.endpoint.register();

		listeners = new Listeners();
		livenessChecker = new CheckLIveness();
		
		InetAddress localhost = InetAddress.getLocalHost();
		if(localhost.isLoopbackAddress()){
			Socket s;
			s = new Socket("202.141.80.14", 80);
			localhost = s.getLocalAddress();
			System.out.println(s.getLocalAddress().getHostAddress());
			s.close();
		}
		LocalIPAddress = localhost.getHostAddress();
	
	}

	public void startLivenessCheck(){
		livenessChecker.startLivenessCheck();
	}

	@Override
	public void deliver(Id id, Message msg) {
		if(msg instanceof SyncMessage){
			SyncMessage synMsg = (SyncMessage) msg;
			System.out.println("Got message " + synMsg.getType());
			switch (synMsg.getType()) {
			case STREAM_REQUEST:
				// Forward the stream if I have one
				Radio.logger.log(Level.INFO,
						"Stream_Request from " + synMsg.getHandle());
				if (hasStream
						&& listeners.getNoOfListeners() < Listeners.MAX_LISTENER) {
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setVLCPort(VLCStreamingPort);
					reply.setType(SyncMessage.Type.STREAM_OFFER);
					replyMessage(synMsg, reply);
					Radio.logger.log(Level.INFO,
							"Offering_to " + synMsg.getHandle());
					System.out.println("Offering Stream to " + synMsg.getIP());
				} else {
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
							+ synMsg.getVLCPort();
					VLCStreamingServer = synMsg.getHandle();
					Radio.setGetStreamLabel(((SyncMessage) msg).getHandle()
							.toString());
					setStream(VLCServerStream);
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setType(SyncMessage.Type.STREAM_ACCEPT);
					replyMessage(synMsg, reply);
					Radio.logger.log(Level.INFO,
							"Accepting " + synMsg.getHandle());
					System.out.println("------------Accepting stream from "
							+ synMsg.getHandle());
					ServerFound = true;
					setServerAlive(true);
					checkDelay(VLCStreamingServer);
				}
				break;
	
			case STREAM_ACCEPT:
				listeners.addClient(synMsg.getHandle());
				Radio.logger.log(Level.INFO,
						"Streaming to " + synMsg.getHandle());
				System.out.println("-----------Streaming to "
						+ synMsg.getHandle());
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
			default:
				break;
			}
		} else if (msg instanceof HeartBeat) {
			setServerAlive(true);
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
		if (joined){
			Radio.logger.log(Level.INFO,"Node Joined " + handle);
			System.out.println("New node " + handle);
		} else {
			Radio.logger.log(Level.INFO,"Node Left " + handle);
			System.out.println("Node Left " + handle);
			if(handle == VLCStreamingServer){
				hasStream = false;
				Player.stopServer();
				Player.stopListening();
				Radio.logger.log(Level.INFO,"Streaming Server Dead " + handle);
				System.out.println("Steaming Server Left");
				RadioNode.getRadioNode().updateLeafSet();
				try {
					ServerFound = false;
					sendStreamRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (listeners.isClient(handle)) {
				Radio.logger.log(Level.INFO,"Cleint left " + handle);
				listeners.removeClient(handle);
			}
		}
	}
	
	//Do call updateLeafSet before this function
	// also Make serverfound = false
	public void sendStreamRequest() throws Exception {
		
		if(!RadioNode.isBootStrapeNode && !ServerFound && !isAlreadySearching){
			isAlreadySearching = true;

			/*
			 * Conditions to be satisfied 1. Within leafset bound. 2. should not
			 * be same as the node itself 3. Should not be one of the receiving
			 * clients
			 */

			if (lastCheckedServer < RadioNode.getRadioNode().leafSet.cwSize()
					&& RadioNode.getRadioNode().leafSet.get(lastCheckedServer) != RadioNode
							.getLocalNodeHandle()
					&& !listeners.getListeningClients().contains(
							RadioNode.getRadioNode().leafSet
									.get(lastCheckedServer))) {
				SyncMessage msg = new SyncMessage();
				msg.setIP(getLocalIP());
				msg.setType(SyncMessage.Type.STREAM_REQUEST);
				msg.setHandle(endpoint.getLocalNodeHandle());
				NodeHandle nh = RadioNode.getRadioNode().leafSet
						.get(lastCheckedServer);
				//use routing table
				if (nh != null) {
				Radio.logger.log(Level.INFO,"Sending Request to " + nh);
				System.out.println("Sending request for stream to " + nh);
				sendMessage(nh.getId(), msg);
				}
				lastCheckedServer++;
				// select the item
			} else {
				lastCheckedServer = -RadioNode.getRadioNode().leafSet.ccwSize();
				System.out
						.println("Starting search from begining of the leafSet");
			}
			isAlreadySearching = false;	
		}	
	}
	
	public boolean checkServerLiveness(){
		if(VLCStreamingServer!=null && endpoint.isAlive(VLCStreamingServer)){
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
	
	public boolean isServerAlive(){
		return isServerAlive;
	}
	
	public void setServerAlive(boolean val){
		synchronized (lock) {
			isServerAlive = val;
		}
	}
	
	public void checkDelay(NodeHandle handle){
		
	}

	public String getVLCServerStream() {
		return VLCServerStream;
	}

}
