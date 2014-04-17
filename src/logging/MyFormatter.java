package logging;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class MyFormatter extends Formatter {
	public String format(LogRecord rec) {
		StringBuffer buf = new StringBuffer(1000);
		buf.append("<tr>");
	    buf.append("<td>");
	    if (rec.getLevel().intValue() >= Level.WARNING.intValue()) {
	        buf.append("<b>");
	        buf.append("<font color=\"red\">");
	        buf.append(rec.getLevel());
	        buf.append("</font>");
	        buf.append("</b>");
	      } 
	    else if(rec.getLevel().intValue() >= Level.SEVERE.intValue()){
	    	buf.append("<b>");
	        buf.append("<font color=\"red\">");
	        buf.append(rec.getLevel());
	        buf.append("</font>");
	        buf.append("</b>");
	      }
	    else{
	    	buf.append("<b>");
	        buf.append("<font color=\"green\">");
	        buf.append(rec.getLevel());
	        buf.append("</font>");
	        buf.append("</b>");
	    }
	    
	    buf.append("</td>");
	    buf.append("<td>");
	    buf.append((rec.getMillis()));
	    buf.append("</td>");
	    buf.append("<td>");
	    buf.append(formatMessage(rec));
	    buf.append('\n');
	    buf.append("<td>");
	    buf.append("</tr>\n");

	    return buf.toString();
	}
//	private String calcDate(long millisecs) {
////		SimpleTimeSource date_format = new SimpleTimeSource("MMM dd,yyyy HH:mm:ss");
////	    Date resultdate = new Date(millisecs);
////	    return date_format.format(resultdate);
//	  }

	// This method is called just after the handler using this
	  // formatter is created
	  public String getHead(Handler h) {
	    return "<HTML>\n<HEAD>\n" + (new Date()) 
	        + "\n</HEAD>\n<BODY>\n<PRE>\n"
	        + "<table width=\"100%\" border>\n  "
	        + "<tr><th>Level</th>" +
	        "<th>Time</th>" +
	        "<th>Log Message</th>" +
	        "</tr>\n";
	  }

	  // This method is called just after the handler using this
	  // formatter is closed
	  public String getTail(Handler h) {
	    return "</table>\n  </PRE></BODY>\n</HTML>\n";
	  }

}