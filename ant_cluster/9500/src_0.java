/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.ResourceUtils;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Ant task for sending files to remote machine over ssh/scp.
 *
 * @since Ant 1.6
 */
public class Scp extends SSHBase {

    private static final String[] FROM_ATTRS = {
        "file", "localfile", "remotefile" };

    private static final String[] TO_ATTRS = {
        "todir", "localtodir", "remotetodir", "localtofile", "remotetofile" };

    private String fromUri;
    private String toUri;
    private boolean preserveLastModified = false;
    private List<ResourceCollection> rcs = null;
    private boolean isFromRemote, isToRemote;
    private boolean isSftp = false;
    private Integer fileMode, dirMode;

    /**
     * Sets the file to be transferred.  This can either be a remote
     * file or a local file.  Remote files take the form:<br>
     * <i>user:password@host:/directory/path/file.example</i><br>
     * Files to transfer can also include a wildcard to include all
     * files in a remote directory.  For example:<br>
     * <i>user:password@host:/directory/path/*</i><br>
     * @param aFromUri a string representing the file to transfer.
     */
    public void setFile(final String aFromUri) {
        setFromUri(aFromUri);
        this.isFromRemote = isRemoteUri(this.fromUri);
    }

    /**
     * Sets the location where files will be transferred to.
     * This can either be a remote directory or a local directory.
     * Remote directories take the form of:<br>
     * <i>user:password@host:/directory/path/</i><br>
     * This parameter is required.

     * @param aToUri a string representing the target of the copy.
     */
    public void setTodir(final String aToUri) {
        setToUri(aToUri);
        this.isToRemote = isRemoteUri(this.toUri);
    }

    /**
     * Similar to {@link #setFile setFile} but explicitly states that
     * the file is a local file.  This is the only way to specify a
     * local file with a @ character.
     * @param aFromUri a string representing the source of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalFile(final String aFromUri) {
        setFromUri(aFromUri);
        this.isFromRemote = false;
    }

    /**
     * Similar to {@link #setFile setFile} but explicitly states that
     * the file is a remote file.
     * @param aFromUri a string representing the source of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteFile(final String aFromUri) {
        validateRemoteUri("remoteFile", aFromUri);
        setFromUri(aFromUri);
        this.isFromRemote = true;
     }

    /**
     * Similar to {@link #setTodir setTodir} but explicitly states
     * that the directory is a local.  This is the only way to specify
     * a local directory with a @ character.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalTodir(final String aToUri) {
        setToUri(aToUri);
        this.isToRemote = false;
    }

    /**
     * Sets flag to determine if file timestamp from
     * remote system is to be preserved during copy.
     * @since Ant 1.8.0
     */
    public void setPreservelastmodified(final boolean yesOrNo) {
        this.preserveLastModified = yesOrNo;
    }

    /**
     * Similar to {@link #setTodir setTodir} but explicitly states
     * that the directory is a remote.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteTodir(final String aToUri) {
        validateRemoteUri("remoteToDir", aToUri);
        setToUri(aToUri);
        this.isToRemote = true;
    }

    private static void validateRemoteUri(final String type, final String aToUri) {
        if (!isRemoteUri(aToUri)) {
            throw new BuildException(type + " '" + aToUri + "' is invalid. "
                                     + "The 'remoteToDir' attribute must "
                                     + "have syntax like the "
                                     + "following: user:password@host:/path"
                                     + " - the :password part is optional");
        }
    }

    /**
     * Changes the file name to the given name while receiving it,
     * only useful if receiving a single file.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalTofile(final String aToUri) {
        setToUri(aToUri);
        this.isToRemote = false;
    }

    /**
     * Changes the file name to the given name while sending it,
     * only useful if sending a single file.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteTofile(final String aToUri) {
        validateRemoteUri("remoteToFile", aToUri);
        setToUri(aToUri);
        this.isToRemote = true;
    }

    /**
     * Setting this to true to use sftp protocol.
     *
     * @param yesOrNo if true sftp protocol will be used.
     */
    public void setSftp(final boolean yesOrNo) {
        isSftp = yesOrNo;
    }

    /**
     * Set the file mode, defaults to "644".
     * @since Ant 1.9.5
     */
    public void setFileMode(String fileMode) {
        this.fileMode = Integer.parseInt(fileMode, 8);
    }

    /**
     * Set the dir mode, defaults to "755".
     * @since Ant 1.9.5
     */
    public void setDirMode(String dirMode) {
        this.dirMode = Integer.parseInt(dirMode, 8);
    }

    /**
     * Adds a FileSet transfer to remote host.  NOTE: Either
     * addFileSet() or setFile() are required.  But, not both.
     *
     * @param set FileSet to send to remote host.
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Adds a ResourceCollection of local files to transfer to remote host.
     * @param set ResourceCollection to send to remote host.
     * @since Ant 1.9.7
     */
    public void add(ResourceCollection res) {
        if (rcs == null) {
            rcs = new LinkedList<ResourceCollection>();
        }
        rcs.add(res);
    }

    /**
     * Initialize this task.
     * @throws BuildException on error
     */
    @Override
    public void init() throws BuildException {
        super.init();
        this.toUri = null;
        this.fromUri = null;
        this.rcs = null;
    }

    /**
     * Execute this task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        if (toUri == null) {
            throw exactlyOne(TO_ATTRS);
        }
        if (fromUri == null && rcs == null) {
            throw exactlyOne(FROM_ATTRS, "one or more nested filesets");
        }
        try {
            if (isFromRemote && !isToRemote) {
                download(fromUri, toUri);
            } else if (!isFromRemote && isToRemote) {
                if (rcs != null) {
                    upload(rcs, toUri);
                } else {
                    upload(fromUri, toUri);
                }
            } else if (isFromRemote && isToRemote) {
                throw new BuildException(
                    "Copying from a remote server to a remote server is not supported.");
            } else {
                throw new BuildException("'todir' and 'file' attributes "
                    + "must have syntax like the following: "
                    + "user:password@host:/path");
            }
        } catch (final Exception e) {
            if (getFailonerror()) {
                if(e instanceof BuildException) {
                    final BuildException be = (BuildException) e;
                    if(be.getLocation() == null) {
                        be.setLocation(getLocation());
                    }
                    throw be;
                } else {
                    throw new BuildException(e);
                }
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        }
    }

    private void download(final String fromSshUri, final String toPath)
        throws JSchException, IOException {
        final String file = parseUri(fromSshUri);

        Session session = null;
        try {
            session = openSession();
            ScpFromMessage message = null;
            if (!isSftp) {
                message =
                    new ScpFromMessage(getVerbose(), session, file,
                                       getProject().resolveFile(toPath),
                                       fromSshUri.endsWith("*"),
                                       preserveLastModified);
            } else {
                message =
                    new ScpFromMessageBySftp(getVerbose(), session, file,
                                             getProject().resolveFile(toPath),
                                             fromSshUri.endsWith("*"),
                                             preserveLastModified);
            }
            log("Receiving file: " + file);
            message.setLogListener(this);
            message.execute();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void upload(final List<ResourceCollection> rcs, final String toSshUri)
        throws IOException, JSchException {
        final String file = parseUri(toSshUri);

        Session session = null;
        try {
            final List<Directory> list = new ArrayList<Directory>(rcs.size());
            for (final Iterator<ResourceCollection> i = rcs.iterator(); i.hasNext();) {
                final ResourceCollection rc = (ResourceCollection) i.next();
                if (rc instanceof FileSet && rc.isFilesystemOnly()) {
                    FileSet fs = (FileSet) rc;
                    final Directory d = createDirectory(fs);
                    if (d != null) {
                        list.add(d);
                    }
                } else {
                       List<Directory> ds = createDirectoryCollection(rc);
                       if (ds !=null) {
                               list.addAll(ds);
                       }
                }
            }
            if (!list.isEmpty()) {
                session = openSession();
                ScpToMessage message = null;
                if (!isSftp) {
                    message = new ScpToMessage(getVerbose(), session,
                                               list, file);
                } else {
                    message = new ScpToMessageBySftp(getVerbose(), session,
                                                     list, file);
                }
                message.setLogListener(this);
                if (fileMode != null) {
                    message.setFileMode(fileMode.intValue());
                }
                if (dirMode != null) {
                    message.setDirMode(dirMode.intValue());
                }
                message.execute();
            }
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void upload(final String fromPath, final String toSshUri)
        throws IOException, JSchException {
        final String file = parseUri(toSshUri);

        Session session = null;
        try {
            session = openSession();
            ScpToMessage message = null;
            if (!isSftp) {
                message =
                    new ScpToMessage(getVerbose(), session,
                                     getProject().resolveFile(fromPath), file);
            } else {
                message =
                    new ScpToMessageBySftp(getVerbose(), session,
                                           getProject().resolveFile(fromPath),
                                           file);
            }
            message.setLogListener(this);
            if (fileMode != null) {
                message.setFileMode(fileMode.intValue());
            }
            if (dirMode != null) {
                message.setDirMode(dirMode.intValue());
            }
            message.execute();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String parseUri(final String uri) {

        int indexOfAt = uri.indexOf('@');
        final int indexOfColon = uri.indexOf(':');

        if (indexOfColon > -1 && indexOfColon < indexOfAt) {
            // user:password@host:/path notation
            // everything upto the last @ before the last : is considered
            // password. (so if the path contains an @ and a : it will not work)
            int indexOfCurrentAt = indexOfAt;
            final int indexOfLastColon = uri.lastIndexOf(':');
            while (indexOfCurrentAt > -1 && indexOfCurrentAt < indexOfLastColon)
            {
                indexOfAt = indexOfCurrentAt;
                indexOfCurrentAt = uri.indexOf('@', indexOfCurrentAt + 1);
            }
            setUsername(uri.substring(0, indexOfColon));
            setPassword(uri.substring(indexOfColon + 1, indexOfAt));
        } else if (indexOfAt > -1) {
            // no password, will require keyfile
            setUsername(uri.substring(0, indexOfAt));
        } else {
            throw new BuildException("no username was given.  Can't authenticate.");
        }

        if (getUserInfo().getPassword() == null
            && getUserInfo().getKeyfile() == null) {
            throw new BuildException("neither password nor keyfile for user "
                                     + getUserInfo().getName() + " has been "
                                     + "given.  Can't authenticate.");
        }

        final int indexOfPath = uri.indexOf(':', indexOfAt + 1);
        if (indexOfPath == -1) {
            throw new BuildException("no remote path in " + uri);
        }

        setHost(uri.substring(indexOfAt + 1, indexOfPath));
        String remotePath = uri.substring(indexOfPath + 1);
        if (remotePath.equals("")) {
            remotePath = ".";
        }
        return remotePath;
    }

    private static boolean isRemoteUri(final String uri) {
        boolean isRemote = true;
        final int indexOfAt = uri.indexOf('@');
        if (indexOfAt < 0) {
            isRemote = false;
        }
        return isRemote;
    }

    private Directory createDirectory(final FileSet set) {
        final DirectoryScanner scanner = set.getDirectoryScanner(getProject());
        Directory root = new Directory(scanner.getBasedir());
        final String[] files = scanner.getIncludedFiles();
        if (files.length != 0) {
            for (int j = 0; j < files.length; j++) {
                final String[] path = Directory.getPath(files[j]);
                Directory current = root;
                File currentParent = scanner.getBasedir();
                for (int i = 0; i < path.length; i++) {
                    final File file = new File(currentParent, path[i]);
                    if (file.isDirectory()) {
                        current.addDirectory(new Directory(file));
                        current = current.getChild(file);
                        currentParent = current.getDirectory();
                    } else if (file.isFile()) {
                        current.addFile(file);
                    }
                }
            }
        } else {
            // skip
            root = null;
        }
        return root;
    }

    private List<Directory> createDirectoryCollection(final ResourceCollection rc) {
        // not a fileset or contains non-file resources
        if (!rc.isFilesystemOnly()) {
            throw new BuildException("Only FileSystem resources are supported.");
        }

        List<Directory> ds = new ArrayList<Directory>();
        for (Resource r : rc) {
               if (!r.isExists()) {
                throw new BuildException("Could not find resource " + r.toLongString() + " to scp.");
            }

            FileProvider fp = r.as(FileProvider.class);
            if (fp == null) {
                throw new BuildException("Resource " + r.toLongString() + " is not a file.");
            }

            FileResource fr = ResourceUtils.asFileResource(fp);
            File baseDir = fr.getBaseDir();
            if (baseDir == null) {
                throw new BuildException("basedir for resource " + r.toLongString() + " is undefined.");
            }

            // if the basedir is set, the name will be relative to that
            String name = r.getName();
            Directory root = new Directory(baseDir);
            Directory current = root;
            File currentParent = baseDir;
            final String[] path = Directory.getPath(name);
            for (int i = 0; i < path.length; i++) {
                final File file = new File(currentParent, path[i]);
                if (file.isDirectory()) {
                    current.addDirectory(new Directory(file));
                    current = current.getChild(file);
                    currentParent = current.getDirectory();
                } else if (file.isFile()) {
                    current.addFile(file);
                }
            }
            ds.add(root);
       }
        return ds;
    }

    private void setFromUri(final String fromUri) {
        if (this.fromUri != null) {
            throw exactlyOne(FROM_ATTRS);
        }
        this.fromUri = fromUri;
    }

    private void setToUri(final String toUri) {
        if (this.toUri != null) {
            throw exactlyOne(TO_ATTRS);
        }
        this.toUri = toUri;
    }

    private BuildException exactlyOne(final String[] attrs) {
        return exactlyOne(attrs, null);
    }

    private BuildException exactlyOne(final String[] attrs, final String alt) {
        final StringBuffer buf = new StringBuffer("Exactly one of ").append(
                '[').append(attrs[0]);
        for (int i = 1; i < attrs.length; i++) {
            buf.append('|').append(attrs[i]);
        }
        buf.append(']');
        if (alt != null) {
            buf.append(" or ").append(alt);
        }
        return new BuildException(buf.append(" is required.").toString());
    }
}
