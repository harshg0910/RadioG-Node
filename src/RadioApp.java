import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;

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
	
	public static RadioApp getRadioApp(){
		if(radioApp!=null){
			return radioApp;
		}
		return null;
	}
	
	
	public void setStream(String src) {
		hasStream = true;
		Player.startVLCStreaming(LocalIPAddress, VLCStreamingPort, src);
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
			System.out.println("****"+s.getLocalAddress().getHostAddress());
			s.close();
		}
		LocalIPAddress = localhost.getHostAddress();
		System.out.println("Your IP Address is: " + LocalIPAddress);
		System.out.println("Your ID is " + node.getId());
	
	}

	public void startLivenessCheck(){
		livenessChecker.startLivenessCheck();
	}

	
	@Override
	public void deliver(Id id, Message msg) {
		// TODO Auto-generated method stub
		
		if(msg instanceof SyncMessage){
			SyncMessage synMsg = (SyncMessage) msg;
			System.out.println("Got message " + synMsg.getType());
			switch (synMsg.getType()) {
			case STREAM_REQUEST:
				// Forward the stream if I have one
				if (hasStream && listeners.getNoOfListeners() <= Listeners.MAX_LISTENER) {
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setVLCPort(VLCStreamingPort);
					reply.setType(SyncMessage.Type.STREAM_OFFER);
					replyMessage(synMsg, reply);
					System.out.println("Offering Stream to " + synMsg.getIP());
				}
				else{
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setVLCPort(VLCStreamingPort);
					reply.setType(SyncMessage.Type.STREAM_REJECT);
					replyMessage(synMsg, reply);
					System.out.println("Rejecting request of " + synMsg.getIP());
				}
				break;
	
			case STREAM_OFFER:
				if (!ServerFound) {
	
					String VLCStream = "mmsh://" + synMsg.getIP() + ":"
							+ synMsg.getVLCPort();
					VLCStreamingServer = synMsg.getHandle();
					Radio.setGetStreamLabel(((SyncMessage) msg).getHandle().toString());
					setStream(VLCStream);
					SyncMessage reply = new SyncMessage();
					reply.setIP(LocalIPAddress);
					reply.setHandle(endpoint.getLocalNodeHandle());
					reply.setType(SyncMessage.Type.STREAM_ACCEPT);
					replyMessage(synMsg, reply);
					System.out.println("------------Accepting stream from "
							+ synMsg.getHandle());
					ServerFound = true;
					setServerAlive(true);
				}
				break;
	
			case STREAM_ACCEPT:
				listeners.addClient(synMsg.getHandle());
				System.out.println("-----------Streaming to " + synMsg.getHandle());
				break;
				
			case STREAM_REJECT:			
				try {
					sendStreamRequest();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			default:
				break;
			}
		}
		else if(msg instanceof HeartBeat){
			setServerAlive(true);
		}

	}

	@Override
	public boolean forward(RouteMessage arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	public void replyMessage(Message incoming, Message reply) {
		Id to = ((SyncMessage) incoming).getHandle().getId();
		endpoint.route(to, reply, null);
	}

	@Override
	public void update(NodeHandle handle, boolean joined) {
		if (joined)
			System.out.println("New node " + handle);
		else {
			System.out.println("Node Left " + handle);
			if(handle == VLCStreamingServer){
				hasStream = false;
				Player.stopServer();
				Player.stopListening();
				System.out.println("Steaming Server Left");
				RadioNode.getRadioNode().updateLeafSet();
				try {
					ServerFound = false;
					sendStreamRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (listeners.isClient(handle)){
				listeners.removeClient(handle);
			}
		}
	}
	
	
	//Do call updateLeafSet before this function
	// also Make serverfound = false
	public void sendStreamRequest() throws Exception {
		if(!RadioNode.isBootStrapeNode && !ServerFound && !isAlreadySearching){
			isAlreadySearching = true;
			if(lastCheckedServer < RadioNode.getRadioNode().leafSet.cwSize() && 
				RadioNode.getRadioNode().leafSet.get(lastCheckedServer)!=RadioNode.getLocalNodeHandle()){
				SyncMessage msg = new SyncMessage();
				msg.setIP(getLocalIP());
				msg.setType(SyncMessage.Type.STREAM_REQUEST);
				msg.setHandle(endpoint.getLocalNodeHandle());
				NodeHandle nh = RadioNode.getRadioNode().leafSet.get(lastCheckedServer);
				System.out.println("Sending request for stream to " + nh);
				
				sendMessage(nh.getId(), msg);
				lastCheckedServer++;
				// select the item
			}
			else{
				lastCheckedServer = -RadioNode.getRadioNode().leafSet.ccwSize();
				System.out.println("Starting search from begining of the leafSet");
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
	

}
