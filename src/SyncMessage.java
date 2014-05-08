import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
	
/**
 * Control Messages Implementations
 * @author Abhi
 *
 */
public class SyncMessage implements Message{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Type of the message
	 * @author Abhi
	 *
	 */
	public static enum Type{
		/**
		 * This type of message is sent while requesting 
		 * streams from different candidate parents
		 */
		STREAM_REQUEST,
		
		/**
		 * This message is sent in reply of STREAM_REQUEST
		 * offering the stream to the client 
		 */
		STREAM_OFFER,
		
		/**
		 * After getting STREAM_OFFER, client sends this message finalizing 
		 * the connection. 
		 */
		STREAM_ACCEPT,
		
		/**
		 * If on getting STREAM_REQUEST, a node can't share it's stream, it sends
		 * this message.
		 */
		STREAM_REJECT,
		
		/**
		 * This will be sent by a node to bootstrap to get nodehandle of free streamers
		 */
		SEND_FREE_NODE_INFO,
		
		/**
		 * This is reply to SEND_FREE_NODE_INFO request with the nodehandle 
		 * of the free node 
		 */
		FREE_NODE_INFO ,
		
		/**
		 * This message is sent by a client to its parent while leaving the network
		 */
		CLIENT_DYING
	}
	
	
	private Type type;
	private String IPAddress;
	private int VLCPort;
	private NodeHandle ServerHandle;
	private short attempt;
	private Id bootstrapNodeID;
	
	public int getVLCPort(){
		return VLCPort;
	}
	
	public void setVLCPort(int port){
		this.VLCPort = port;
	}
	
	public SyncMessage() {
        
	}
	
	public Type getType(){
		return type;
	}
	
	public String getIP(){
		return IPAddress;
	}
	
	public void setIP(String IP){
		this.IPAddress = IP;
	}
	public void setType(Type t){
		this.type = t;
	}
	
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.HIGH_PRIORITY;
	}
	
	public void setHandle(NodeHandle handle){
		this.ServerHandle = handle;
	}
	
	public NodeHandle getHandle(){
		return ServerHandle;
	}

	public short getAttempt() {
		return attempt;
	}

	public void setAttempt(short attempt) {
		this.attempt = attempt;
	}

	public Id getBootstrapNodeID() {
		return bootstrapNodeID;
	}

	public void setBootstrapNodeID(Id bootstrapNodeID) {
		this.bootstrapNodeID = bootstrapNodeID;
	}
}
