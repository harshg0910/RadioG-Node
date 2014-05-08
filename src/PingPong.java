import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
/**
 * Messages to calculate delay from parent
 * @author Abhi
 *
 */

@SuppressWarnings("serial")
public class PingPong implements Message{
	
	public static enum Type{
		PING,
		PONG
	}
	private Type type;
	private NodeHandle handle;
	public PingPong(Type type,NodeHandle handle) {
		this.setType(type);
		this.setHandle(handle);
	}
	@Override
	public int getPriority() {
		return 0;
	}
	/**
	 * 
	 * @return - returns type of the message
	 */
	public Type getType() {
		return type;
	}
	/**
	 * set type of message
	 * @param type - type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}
	/**
	 * 
	 * @return returns node handle in the node
	 */
	public NodeHandle getHandle() {
		return handle;
	}
	/**
	 * sets handle in the message
	 * @param handle -  handle to set in message
	 */
	public void setHandle(NodeHandle handle) {
		this.handle = handle;
	}

}
