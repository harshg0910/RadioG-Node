/**
 * Check system configuration
 * @author Abhi
 *
 */

public class CheckSystem {
	/**
	 * 
	 * @return returns Operating system name
	 */
	public static String getOS(){
		return System.getProperty("os.name");
	}
	/**
	 * 
	 * @return - get JRE architecture
	 */
	public static String getJREArch(){
		 return System.getProperty("sun.arch.data.model");
	}
	/**
	 * 
	 * @return returns OS Architecture
	 */
	public static String getOsArch(){
		return System.getProperty("os.arch");
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(getOS());
		System.out.println(getJREArch());
	}
	
}
