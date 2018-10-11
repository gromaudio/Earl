package com.einmalfel.earl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.einmalfel.earl.tools.NPTParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class Utils {
  static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
  static final String MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/";
  static final String ITUNES_NAMESPACE = "http://www.itunes.com/dtds/podcast-1.0.dtd";
  static final String CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/";

  private static final String TAG = "Earl.Utils";
  private static final DateFormat[] itunesDurationFormats = {
      new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH),
      new SimpleDateFormat("H:mm:ss", Locale.ENGLISH),
      new SimpleDateFormat("mm:ss", Locale.ENGLISH),
      new SimpleDateFormat("m:ss", Locale.ENGLISH),
      };

  static {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    for (DateFormat format : itunesDurationFormats) {
      format.setTimeZone(utc);
    }
  }


  private Utils() {}

  @Nullable
  static Integer parseRFC2326NPT(@NonNull String string) {
    try {
      return (int) new NPTParser(string).parse();
    } catch (ParseException exception) {
      Logger.w(TAG, "Failed to parse media:rating time", exception);
      return null;
    }
  }

  /**
   * @param dateString time string to parse
   * @return episode duration in seconds or null if parsing fails
   */
  @Nullable
  static Integer parseItunesDuration(@NonNull String dateString) {
    for (DateFormat format : itunesDurationFormats) {
      try {
        Date date = format.parse(dateString);
        return (int) (date.getTime() / 1000);
      } catch (ParseException ignored) {
        // ignore exceptions: if date won't match any format, test if it is integer value in seconds
      }
    }
    return tryParseInt(dateString);
  }

  @Nullable
  static Integer parseMediaRssTime(@NonNull String time) {
    // MRSS spec doesn't always clarify which time format is used.
    // In examples it looks quite like itunes duration.
    Integer result = parseItunesDuration(time);
    if (result == null) {
      result = parseRFC2326NPT(time);
    } else {
      // Itunes duration is in [s]
      result *= 1000;
    }
    return result;
  }

  /**
   * Fast-forward parser to the end of current tag (last tag whose START_TAG we passed),
   * skipping all nested tags.
   */
  static void finishTag(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
    while (parser.getEventType() != XmlPullParser.END_TAG) {
      if (parser.getEventType() == XmlPullParser.START_TAG) {
        skipTag(parser);
      }
      parser.next();
    }
  }

  /**
   * Skip next tag (we are currently at its START_TAG).
   * Copied from http://developer.android.com/training/basics/network-ops/xml.html#skip
   */
  static void skipTag(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
    if (parser.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException("Unexpected parser event " + parser.getEventType());
    }
    int depth = 1;
    while (depth != 0) {
      switch (parser.next()) {
        case XmlPullParser.END_TAG:
          depth--;
          break;
        case XmlPullParser.START_TAG:
          depth++;
          break;
        default: // ignore other tags
      }
    }
  }

  @Nullable
  static Integer tryParseInt(@Nullable String string) {
    if (string == null) {
      return null;
    } else {
      try {
        return Integer.valueOf(string);
      } catch (NumberFormatException exception) {
        Logger.w(TAG, "Error parsing integer value '" + string + '\'', exception);
        return null;
      }
    }
  }

  @NonNull
  static Integer nonNullInt(@Nullable String string) {
    if (string == null) {
      Logger.w(TAG, "Unexpectedly got null string. -1 returned", new NullPointerException());
      return -1;
    }
    try {
      return Integer.valueOf(string);
    } catch (NumberFormatException exception) {
      Logger.w(TAG, "Malformed integer string replaced with '-1'", exception);
      return -1;
    }
  }

  @NonNull
  static String nonNullString(@Nullable String string) {
    if (string == null) {
      Logger.w(TAG, "Unexpectedly got null string. Replaced with empty", new NullPointerException());
      return "";
    } else {
      return string;
    }
  }

  @Nullable
  static URL tryParseUrl(@Nullable String string) {
    if (string == null) {
      Logger.w(TAG, "Null value while parsing url", new NullPointerException());
      return null;
    } else {
      try {
        return new URL(string);
      } catch (MalformedURLException exception) {
        Logger.w(TAG, "Error parsing url value '" + string + '\'', exception);
        return null;
      }
    }
  }

  @NonNull
  static URL nonNullUrl(@Nullable String string) {
    URL result = tryParseUrl(string);
    if (result == null) {
      Logger.w(TAG, "Malformed URL replaced with 'http://'");
      try {
        result = new URL("http://");
      } catch (MalformedURLException ignored) {
        throw new AssertionError("Should never get here");
      }
    }
    return result;
  }

  @Nullable
  static URI tryParseUri(@Nullable String string) {
    if (string == null) {
      Logger.w(TAG, "Null value while parsing uri", new NullPointerException());
      return null;
    } else {
      try {
        return new URI(string);
      } catch (URISyntaxException exception) {
        Logger.w(TAG, "Error parsing uri value '" + string + '\'', exception);
        return null;
      }
    }
  }

  @NonNull
  static URI nonNullUri(@Nullable String string) {
    URI result = tryParseUri(string);
    if (result == null) {
      Logger.w(TAG, "Malformed URI replaced with 'http://'");
      try {
        result = new URI("http:///");
      } catch (URISyntaxException ignored) {
        throw new AssertionError("Should never get here");
      }
    }
    return result;
  }
}
