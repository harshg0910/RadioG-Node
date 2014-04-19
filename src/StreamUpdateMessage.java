import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


public class StreamUpdateMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static enum Type{
		STREAM_FREE,
		STREAM_FULL,
	}
	
	public Type info;
	private NodeHandle node;
	private int level;
	
	
	public Type getInfo(){
		return info;
	}
	
	public void setInfo(Type inf){
		this.info = inf;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	}

	public NodeHandle getNode() {
		return node;
	}

	public void setNode(NodeHandle node) {
		this.node = node;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

}
