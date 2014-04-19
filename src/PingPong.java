import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


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
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public NodeHandle getHandle() {
		return handle;
	}
	public void setHandle(NodeHandle handle) {
		this.handle = handle;
	}

}
