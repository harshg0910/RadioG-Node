import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * A File server to accept log files
 * @author harsh
 *
 */
public class FileServer {
	static long i = 0;
    public static void main(String[] args) throws Exception {
        // create socket
        ServerSocket servsock = new ServerSocket(13267);
        while (true) {
        	try{
            System.out.println("Waiting...");

            Socket sock = servsock.accept();
            System.out.println("Accepted connection : " + sock);
            OutputStream os = sock.getOutputStream();
            //new FileServer().send(os);
            InputStream is = sock.getInputStream();
            new FileServer().receiveFile(is);
            sock.close();
        	}catch(Exception e){
        		System.out.println(e.getMessage());
        	}
        }
    }

    public void send(OutputStream os) throws Exception {
        // sendfile
        File myFile = new File("/home/nilesh/opt/eclipse/about.html");
        byte[] mybytearray = new byte[(int) myFile.length() + 1];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(mybytearray, 0, mybytearray.length);
        System.out.println("Sending...");
        os.write(mybytearray, 0, mybytearray.length);
        os.flush();
    }

    public void receiveFile(InputStream is) throws Exception {
        int filesize = 6022386;
        int bytesRead;
        int current = 0;
        byte[] mybytearray = new byte[filesize];
        i++;
        FileOutputStream fos = new FileOutputStream("files/"+i+".xml");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bytesRead = is.read(mybytearray, 0, mybytearray.length);
        current = bytesRead;

        do {
            bytesRead = is.read(mybytearray, current,
                    (mybytearray.length - current));
            if (bytesRead >= 0)
                current += bytesRead;
        } while (bytesRead > -1);

        bos.write(mybytearray, 0, current);
        bos.flush();
        bos.close();
    }
} 