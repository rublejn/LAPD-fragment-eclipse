/* (c) Copyright 2013 and following years, Venera Arnaoudova,
 * Polytechnique Montreal.
 * 
 * Use and copying of this software and preparation of derivative works
 * based upon this software are permitted. Any copy of this software or
 * of any derivative work must include the above copyright notice of
 * the author, this paragraph and the one after it.
 * 
 * This software is made available AS IS, and THE AUTHOR DISCLAIMS
 * ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, AND NOT WITHSTANDING ANY OTHER PROVISION CONTAINED HEREIN,
 * ANY LIABILITY FOR DAMAGES RESULTING FROM THE SOFTWARE OR ITS USE IS
 * EXPRESSLY DISCLAIMED, WHETHER ARISING IN CONTRACT, TORT (INCLUDING
 * NEGLIGENCE) OR STRICT LIABILITY, EVEN IF THE AUTHOR IS ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 * 
 * All Rights Reserved.
 * @author <a href="mailto:Venera.Arnaoudova@gmail.com">Venera Arnaoudova</a>
 * @author <a href="mailto:Alexis.Debourdieu@gmail.com@gmail.com">Alexis Debourdieu</a>
 */

package utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.JWNLRuntimeException;
import net.didion.jwnl.dictionary.file.DictionaryFile;
import net.didion.jwnl.dictionary.file_manager.FileManagerImpl;
import net.didion.jwnl.util.factory.Param;

public class PluginFileManager extends FileManagerImpl {

  public PluginFileManager() {
  }

  public PluginFileManager(final String searchDir, final Class dictionaryFileType)
      throws IOException {

    // URL url = new URL(searchDir);

    // url.openConnection().getInputStream();
    super(searchDir, dictionaryFileType);
  }

  @Override
  public Object create(final Map params) throws JWNLException {
    Class fileClass = null;
    try {
      fileClass = Class.forName(((Param) params.get(FileManagerImpl.FILE_TYPE)).getValue());
    } catch (final ClassNotFoundException ex) {
      throw new JWNLRuntimeException("DICTIONARY_EXCEPTION_002", ex);
    }
    this.checkFileType(fileClass);

    final String path = ((Param) params.get(FileManagerImpl.PATH)).getValue();

    try {
      final URL url = FileLocator.toFileURL(new URL(path));
      final String dir = new File(new URI(url.getProtocol(), url.getAuthority(), null,
          url.getPort(), url.getPath(), url.getQuery(), url.getRef())).getPath() + File.separator;
      // System.out.println("PATH:" + dir);
      return new PluginFileManager(dir, fileClass);

    } catch (final IOException ex) {
      throw new JWNLException("DICTIONARY_EXCEPTION_016", fileClass, ex);
    } catch (final URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Checks the type to ensure it's valid.
   * 
   * @param c
   */
  private void checkFileType(final Class c) {
    if (!DictionaryFile.class.isAssignableFrom(c)) {
      throw new JWNLRuntimeException("DICTIONARY_EXCEPTION_003", c);
    }
  }
}
