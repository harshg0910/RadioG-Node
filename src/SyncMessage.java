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
		STREAM_REJECT
	}
	
	private Type type;
	private String IPAddress;
	private int VLCPort;
	private NodeHandle ServerHandle;
	
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
}
