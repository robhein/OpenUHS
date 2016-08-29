package net.vhati.openuhs.androidreader.downloader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vhati.openuhs.androidreader.downloader.DownloadableUHS;
import net.vhati.openuhs.core.DefaultUHSErrorHandler;
import net.vhati.openuhs.core.UHSErrorHandler;


/**
 * A parser for catalogs downloaded from the official UHS server.
 *
 * Note: The catalog text varies depending on the user-agent that downloaded it!
 *
 * This is what "Mozilla" user-agents see:
 * <pre>&lt;FILE&gt;&lt;FTITLE&gt;The 11th Hour&lt;/FTITLE&gt;
 * &lt;FURL&gt;http://www.uhs-hints.com/rfiles/11thhour.zip&lt;/FURL&gt;
 * &lt;FNAME&gt;11thhour.uhs&lt;/FNAME&gt;&lt;FDATE&gt;24-Jan-96 00:01:39&lt;/FDATE&gt;
 * &lt;FSIZE&gt;25024&lt;/FSIZE&gt;
 * &lt;FFULLSIZE&gt;51278&lt;/FFULLSIZE&gt;&lt;/FILE&gt;</pre>
 *
 * This is what "UHSWIN/5.2" user-agents see:
 * <pre>&lt;MESSAGE&gt;A new version of the UHS Reader is now available.  Version 6.00 offers [...]&lt;/MESSAGE&gt;
 * &lt;FILE&gt;&lt;FTITLE&gt;The 11th Hour&lt;/FTITLE&gt;
 * &lt;FURL&gt;http://www.uhs-hints.com/rfiles/11thhour.zip&lt;/FURL&gt;
 * &lt;FNAME&gt;11thhour.uhs&lt;/FNAME&gt;&lt;FDATE&gt;23-Jan-96&lt;/FDATE&gt;
 * &lt;FSIZE&gt;25024&lt;/FSIZE&gt;
 * &lt;FFULLSIZE&gt;51278&lt;/FFULLSIZE&gt;&lt;/FILE&gt;</pre>
 *
 * MESSAGE only appears for old UHS user-agents since "UHSWIN/4.0".
 * FDATE is usually just date. Except for "Mozilla", it's both date AND time (and +1 day!?).
 *
 * So FDATE can be "dd-MMM-yy" or "dd-MMM-yy HH:mm:ss".
 */
public class CatalogParser {
  public static final String DEFAULT_CATALOG_URL = "http://www.uhs-hints.com:80/cgi-bin/update.cgi";

  // According to the server's Content-Type HTTP response header.
  public static final String DEFAULT_CATALOG_ENCODING = "ISO-8859-1";

  public static final String DEFAULT_USER_AGENT = "UHSWIN/5.2";


  /*
   * SimpleDateFormat stops reading strings longer than the pattern.
   * So just parse date. The time segment, if present, will be ignored.
   */
  private DateFormat goofyDateFormat = new SimpleDateFormat("dd-MMM-yy");

  private UHSErrorHandler errorHandler = new DefaultUHSErrorHandler(System.err);


  public CatalogParser() {
  }


  /**
   * Sets the error handler to notify of exceptions.
   * This is a convenience for logging/muting.
   * The default handler prints to System.err.
   *
   * @param eh the error handler, or null, for quiet parsing
   */
  public void setErrorHandler(UHSErrorHandler eh) {
    errorHandler = eh;
  }


  /**
   *
   * Parses the catalog of available hint files.
   *
   * @param catalogString the xml-like string downloaded from the server
   * @return a List of DownloadableUHS objects
   */
  public List<DownloadableUHS> parseCatalog(String catalogString) {
    errorHandler.log(UHSErrorHandler.INFO, null, "Catalog parse started", 0, null);

    List<DownloadableUHS> catalog = new ArrayList<DownloadableUHS>();

    if (catalogString == null || catalogString.length() == 0) return catalog;


    Pattern msgPtn = Pattern.compile("<MESSAGE>(.*?)</MESSAGE>");

    Pattern fileChunkPtn = Pattern.compile("(?s)<FILE>(.*?)</FILE>\\s*");
    Pattern titlePtn = Pattern.compile("<FTITLE>(.*?)</FTITLE>");
    Pattern urlPtn = Pattern.compile("<FURL>(.*?)</FURL>");
    Pattern namePtn = Pattern.compile("<FNAME>(.*?)</FNAME>");
    Pattern datePtn = Pattern.compile("<FDATE>(.*?)</FDATE>");
    Pattern compressedSizePtn = Pattern.compile("<FSIZE>(.*?)</FSIZE>");
    Pattern fullSizePtn = Pattern.compile("<FFULLSIZE>(.*?)</FFULLSIZE>");

    // Let chunk's find() skip the <MESSAGE> tag, if present.
    // Could've done find(index) from the end of the message.
    //   and used "\\G" (previous match) for strict back-to-back <FILE> parsing.

    Matcher fileChunkMatcher = fileChunkPtn.matcher(catalogString);
    Matcher m = null;
    while (fileChunkMatcher.find()) {
      String fileChunk = fileChunkMatcher.group(1);
      DownloadableUHS tmpUHS = new DownloadableUHS();

      m = titlePtn.matcher(fileChunk);
      if (m.find()) tmpUHS.setTitle(m.group(1));

      m = urlPtn.matcher(fileChunk);
      if (m.find()) tmpUHS.setUrl(m.group(1));

      m = namePtn.matcher(fileChunk);
      if (m.find()) tmpUHS.setName(m.group(1));

      m = datePtn.matcher(fileChunk);
      if (m.find()) {
        try {
          tmpUHS.setDate(goofyDateFormat.parse(m.group(1)));
        }
        catch (ParseException e) {
          errorHandler.log(UHSErrorHandler.ERROR, null, String.format("Unexpected date format: '%s'", m.group(1)), 0, null);
        }
      }

      m = compressedSizePtn.matcher(fileChunk);
      if (m.find()) tmpUHS.setCompressedSize(m.group(1));

      m = fullSizePtn.matcher(fileChunk);
      if (m.find()) tmpUHS.setFullSize(m.group(1));

      catalog.add(tmpUHS);
    }

    errorHandler.log(UHSErrorHandler.INFO, null, String.format("Catalog parse finished (count: %d)", catalog.size()), 0, null);

    return catalog;
  }
}