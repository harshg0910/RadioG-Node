import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


public class SyncMessage implements Message{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum Type{
		STREAM_REQUEST,
		STREAM_OFFER,
		STREAM_ACCEPT,
		STREAM_REJECT,
		SEND_STREAM, // This will be sent by a node to bootstrap one to get nodehandle of available stream 
		FREE_STREAM  // This will contain the nodehandle of the free stream
	}
	
	private Type type;
	private String IPAddress;
	private int VLCPort;
	private NodeHandle ServerHandle;
	private short attempt;
	
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
}
