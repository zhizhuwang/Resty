// Copyright (C) 1998-2001 by Jason Hunter <jhunter_AT_acm_DOT_org>.
// All rights reserved.  Use of this class is limited.
// Please see the LICENSE for more information.

package cn.dreampie.upload;

import cn.dreampie.common.http.HttpRequest;
import cn.dreampie.log.Logger;
import cn.dreampie.upload.multipart.FilePart;
import cn.dreampie.upload.multipart.FileRenamePolicy;
import cn.dreampie.upload.multipart.MultipartParser;
import cn.dreampie.upload.multipart.Part;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A utility class to handle <code>multipart/form-data</code> requests,
 * the kind of requests that support file uploads.  This class emulates the
 * interface of <code>HttpRequest</code>, making it familiar to use.
 * It uses a "push" model where any incoming files are read and saved directly
 * to disk in the constructor. If you wish to have more flexibility, e.g.
 * write the files to a database, use the "pull" model
 * <code>MultipartParser</code> instead.
 * <p/>
 * This class can receive arbitrarily large files (up to an artificial limit
 * you can set), and fairly efficiently too.
 * It cannot handle nested data (multipart content within multipart content).
 * It <b>can</b> now with the latest release handle internationalized content
 * (such as non Latin-1 filenames).
 * <p/>
 * To avoid collisions and have fine control over file placement, there's a
 * constructor variety that takes a pluggable FileRenamePolicy implementation.
 * A particular policy can choose to rename or change the location of the file
 * before it's written.
 * <p/>
 * See the included upload.war for an example of how to use this class.
 * <p/>
 * The full file upload specification is contained in experimental RFC 1867,
 * available at <a href="http://www.ietf.org/rfc/rfc1867.txt">
 * http://www.ietf.org/rfc/rfc1867.txt</a>.
 *
 * @author Jason Hunter
 * @author Geoff Soutter
 * @version 1.0, 1998/09/18<br>
 * @see MultipartParser
 */
public class MultipartRequest {

  private static final Logger logger = Logger.getLogger(MultipartRequest.class);
  private static final int DEFAULT_MAX_POST_SIZE = 1024 * 1024;  // 1 Meg

  protected Hashtable<String, UploadedFile> files = new Hashtable<String, UploadedFile>();       // name - UploadedFile

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to 1 Megabyte.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @throws java.io.IOException if the uploaded content is larger than 1 Megabyte
   *                             or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory) throws IOException {
    this(request, saveDirectory, DEFAULT_MAX_POST_SIZE);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param maxPostSize   the maximum size of the POST content.
   * @throws java.io.IOException if the uploaded content is larger than
   *                             <tt>maxPostSize</tt> or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory,
                          int maxPostSize) throws IOException {
    this(request, saveDirectory, maxPostSize, null, null);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param encoding      the encoding of the response, such as ISO-8859-1
   * @throws java.io.IOException if the uploaded content is larger than
   *                             1 Megabyte or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory,
                          String encoding) throws IOException {
    this(request, saveDirectory, DEFAULT_MAX_POST_SIZE, encoding, null);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param maxPostSize   the maximum size of the POST content.
   * @param policy        change file name
   * @throws java.io.IOException if the uploaded content is larger than
   *                             <tt>maxPostSize</tt> or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory,
                          int maxPostSize,
                          FileRenamePolicy policy) throws IOException {
    this(request, saveDirectory, maxPostSize, null, policy);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param maxPostSize   the maximum size of the POST content.
   * @param encoding      the encoding of the response, such as ISO-8859-1
   * @throws java.io.IOException if the uploaded content is larger than
   *                             <tt>maxPostSize</tt> or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory,
                          int maxPostSize,
                          String encoding) throws IOException {
    this(request, saveDirectory, maxPostSize, encoding, null);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   * <p/>
   * To avoid file collisions, this constructor takes an implementation of the
   * FileRenamePolicy interface to allow a pluggable rename policy.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param maxPostSize   the maximum size of the POST content.
   * @param encoding      the encoding of the response, such as ISO-8859-1
   * @param policy        a pluggable file rename policy
   * @throws java.io.IOException if the uploaded content is larger than
   *                             <tt>maxPostSize</tt> or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          String saveDirectory,
                          int maxPostSize,
                          String encoding,
                          FileRenamePolicy policy) throws IOException {
    this(request, new File(saveDirectory), maxPostSize, encoding, policy);
  }

  /**
   * Constructs a new MultipartRequest to handle the specified request,
   * saving any uploaded files to the given directory, and limiting the
   * upload size to the specified length.  If the content is too large, an
   * IOException is thrown.  This constructor actually parses the
   * <tt>multipart/form-data</tt> and throws an IOException if there's any
   * problem reading or parsing the request.
   * <p/>
   * To avoid file collisions, this constructor takes an implementation of the
   * FileRenamePolicy interface to allow a pluggable rename policy.
   *
   * @param request       the servlet request.
   * @param saveDirectory the directory in which to save any uploaded files.
   * @param maxPostSize   the maximum size of the POST content.
   * @param encoding      the encoding of the response, such as ISO-8859-1
   * @param policy        a pluggable file rename policy
   * @throws java.io.IOException if the uploaded content is larger than
   *                             <tt>maxPostSize</tt> or there's a problem reading or parsing the request.
   */
  public MultipartRequest(HttpRequest request,
                          File saveDirectory,
                          int maxPostSize,
                          String encoding,
                          FileRenamePolicy policy) throws IOException {
    // Sanity check values
    if (request == null)
      throw new IllegalArgumentException("request cannot be null");
    if (maxPostSize <= 0) {
      throw new IllegalArgumentException("maxPostSize must be positive");
    }

    // Check saveDirectory is truly a directory
    if (!saveDirectory.isDirectory())
      throw new IllegalArgumentException("Not a directory: " + saveDirectory);

    // Check saveDirectory is writable
    if (!saveDirectory.canWrite())
      throw new IllegalArgumentException("Not writable: " + saveDirectory);

    // Parse the incoming multipart, storing files in the dir provided, 
    // and populate the meta objects which describe what we found
    MultipartParser parser =
        new MultipartParser(request, maxPostSize, true, true, encoding);

    Part part;
    while ((part = parser.readNextPart()) != null) {
      String name = part.getName();
      if (part.isFile()) {
        // It's a file part
        FilePart filePart = (FilePart) part;
        String fileName = filePart.getFileName();
        if (fileName != null) {
          filePart.setRenamePolicy(policy);  // null policy is OK
          // The part actually contained a file
          filePart.writeTo(saveDirectory);
          files.put(name, new UploadedFile(saveDirectory.toString(),
              filePart.getFileName(),
              fileName,
              filePart.getContentType()));
          logger.info("Upload success. file \"" + filePart.getFileName() + "\" type \"" + filePart.getContentType() + "\"");
        } else {
          // The field did not contain a file
          files.put(name, new UploadedFile(null, null, null, null));
          logger.info("Upload empty file.");
        }
      }
    }
  }

  /**
   * Constructor with an old signature, kept for backward compatibility.
   * Without this constructor, a servlet compiled against a previous version
   * of this class (pre 1.4) would have to be recompiled to link with this
   * version.  This constructor supports the linking via the old signature.
   * Callers must simply be careful to pass in an HttpRequest.
   */
  public MultipartRequest(ServletRequest request,
                          String saveDirectory) throws IOException {
    this(new HttpRequest((HttpServletRequest) request), saveDirectory);
  }

  /**
   * Constructor with an old signature, kept for backward compatibility.
   * Without this constructor, a servlet compiled against a previous version
   * of this class (pre 1.4) would have to be recompiled to link with this
   * version.  This constructor supports the linking via the old signature.
   * Callers must simply be careful to pass in an HttpRequest.
   */
  public MultipartRequest(ServletRequest request,
                          String saveDirectory,
                          int maxPostSize) throws IOException {
    this(new HttpRequest((HttpServletRequest) request), saveDirectory, maxPostSize);
  }


  /**
   * Returns the names of all the uploaded files as an Enumeration of
   * Strings.  It returns an empty Enumeration if there are no uploaded
   * files.  Each file name is the name specified by the form, not by
   * the user.
   *
   * @return the names of all the uploaded files as an Enumeration of Strings.
   */
  public Enumeration getFileNames() {
    return files.keys();
  }

  /**
   * Returns the filesystem name of the specified file, or null if the
   * file was not included in the upload.  A filesystem name is the name
   * specified by the user.  It is also the name under which the file is
   * actually saved.
   *
   * @param name the file name.
   * @return the filesystem name of the file.
   */
  public String getFilesystemName(String name) {
    try {
      UploadedFile file = files.get(name);
      return file.getFilesystemName();  // may be null
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the original filesystem name of the specified file (before any
   * renaming policy was applied), or null if the file was not included in
   * the upload.  A filesystem name is the name specified by the user.
   *
   * @param name the file name.
   * @return the original file name of the file.
   */
  public String getOriginalFileName(String name) {
    try {
      UploadedFile file = files.get(name);
      return file.getOriginalFileName();  // may be null
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the content type of the specified file (as supplied by the
   * client browser), or null if the file was not included in the upload.
   *
   * @param name the file name.
   * @return the content type of the file.
   */
  public String getContentType(String name) {
    try {
      UploadedFile file = files.get(name);
      return file.getContentType();  // may be null
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns a File object for the specified file saved on the server's
   * filesystem, or null if the file was not included in the upload.
   *
   * @param name the file name.
   * @return a File object for the named file.
   */
  public File getFile(String name) {
    try {
      UploadedFile file = files.get(name);
      return file.getFile();  // may be null
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns all File objects for the specified file saved on the server's
   * filesystem
   *
   * @return a File objects.
   */
  public Hashtable<String, UploadedFile> getFiles() {
    return files;
  }
}


