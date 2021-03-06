/*******************************************************************************
 * Copyright (c) 2000, 2001, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * @see IPackageFragmentRoot
 */
public class PackageFragmentRoot extends Openable implements IPackageFragmentRoot {

	/**
	 * The delimiter between the source path and root path in the
	 * attachment server property.
	 */
	protected final static char ATTACHMENT_PROPERTY_DELIMITER= '*';

	/**
	 * The resource associated with this root.
	 * @see IResource
	 */
	protected IResource fResource;
	
/**
 * Constructs a package fragment root which is the root of the java package
 * directory hierarchy.
 */
protected PackageFragmentRoot(IResource resource, IJavaProject project) {
	this(resource, project, resource.getProjectRelativePath().toString());
	fResource = resource;
}

/**
 * Constructs a package fragment root which is the root of the java package
 * directory hierarchy.
 */
protected PackageFragmentRoot(IResource resource, IJavaProject project, String path) {
	super(PACKAGE_FRAGMENT_ROOT, project, path);
	fResource = resource;
}

/**
 * @see IPackageFragmentRoot
 */
public void attachSource(IPath sourcePath, IPath rootPath, IProgressMonitor monitor) throws JavaModelException {
	try {
		verifyAttachSource(sourcePath);
		if (monitor != null) {
			monitor.beginTask(Util.bind("element.attachingSource"), 2); //$NON-NLS-1$
		}
		SourceMapper mapper= null;
		SourceMapper oldMapper= getSourceMapper();
		IWorkspace workspace= getJavaModel().getWorkspace();
		boolean rootNeedsToBeClosed= false;

		if (sourcePath == null) {
			//source being detached
			rootNeedsToBeClosed= true;
		/* Disable deltas (see 1GDTUSD)
			// fire a delta to notify the UI about the source detachement.
			JavaModelManager manager = (JavaModelManager) JavaModelManager.getJavaModelManager();
			JavaModel model = (JavaModel) getJavaModel();
			JavaElementDelta attachedSourceDelta = new JavaElementDelta(model);
			attachedSourceDelta .sourceDetached(this); // this would be a PackageFragmentRoot
			manager.registerResourceDelta(attachedSourceDelta );
			manager.fire(); // maybe you want to fire the change later. Let us know about it.
		*/
		} else {
		/*
			// fire a delta to notify the UI about the source attachement.
			JavaModelManager manager = (JavaModelManager) JavaModelManager.getJavaModelManager();
			JavaModel model = (JavaModel) getJavaModel();
			JavaElementDelta attachedSourceDelta = new JavaElementDelta(model);
			attachedSourceDelta .sourceAttached(this); // this would be a PackageFragmentRoot
			manager.registerResourceDelta(attachedSourceDelta );
			manager.fire(); // maybe you want to fire the change later. Let us know about it.
		 */

			//check if different from the current attachment
			IPath storedSourcePath= getSourceAttachmentPath();
			IPath storedRootPath= getSourceAttachmentRootPath();
			if (monitor != null) {
				monitor.worked(1);
			}
			if (storedSourcePath != null) {
				if (!(storedSourcePath.equals(sourcePath) && rootPath.equals(storedRootPath))) {
					rootNeedsToBeClosed= true;
				}
			}
			// check if source path is valid
			Object target = JavaModel.getTarget(workspace.getRoot(), sourcePath, false);
			if (target == null) {
				if (monitor != null) {
					monitor.done();
				}
				throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_PATH, sourcePath));
			}
			mapper= new SourceMapper(
				sourcePath, 
				rootPath.toOSString(), 
				this.isExternal() ? JavaCore.getOptions() : this.getJavaProject().getOptions(true)); // only project options if associated with resource
		}
		setSourceMapper(mapper);
		if (sourcePath == null) {
			setSourceAttachmentProperty(null); //remove the property
		} else {
			//set the property to the path of the mapped source
			setSourceAttachmentProperty(sourcePath.toString() + ATTACHMENT_PROPERTY_DELIMITER + rootPath.toString());
		}
		if (rootNeedsToBeClosed) {
			if (oldMapper != null) {
				oldMapper.close();
			}
			BufferManager manager= BufferManager.getDefaultBufferManager();
			Enumeration openBuffers= manager.getOpenBuffers();
			while (openBuffers.hasMoreElements()) {
				IBuffer buffer= (IBuffer) openBuffers.nextElement();
				IOpenable possibleMember= buffer.getOwner();
				if (isAncestorOf((IJavaElement) possibleMember)) {
					buffer.close();
				}
			}
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	} catch (JavaModelException e) {
		setSourceAttachmentProperty(null); // loose info - will be recomputed
		throw e;
	} finally {
		if (monitor != null) {
			monitor.done();
		}
	}
}

/**
 * Compute the package fragment children of this package fragment root.
 * 
 * @exception JavaModelException  The resource associated with this package fragment root does not exist
 */
protected boolean computeChildren(OpenableElementInfo info) throws JavaModelException {
	try {
		// the underlying resource may be a folder or a project (in the case that the project folder
		// is actually the package fragment root)
		if (fResource.getType() == IResource.FOLDER || fResource.getType() == IResource.PROJECT) {
			ArrayList vChildren = new ArrayList(5);
			char[][] exclusionPatterns = fullExclusionPatternChars();
			computeFolderChildren((IContainer) fResource, "", vChildren, exclusionPatterns); //$NON-NLS-1$
			IJavaElement[] children = new IJavaElement[vChildren.size()];
			vChildren.toArray(children);
			info.setChildren(children);
		}
	} catch (JavaModelException e) {
		//problem resolving children; structure remains unknown
		info.setChildren(new IJavaElement[]{});
		throw e;
	}
	return true;
}

/**
 * Starting at this folder, create package fragments and add the fragments that are not exclused
 * to the collection of children.
 * 
 * @exception JavaModelException  The resource associated with this package fragment does not exist
 */
protected void computeFolderChildren(IContainer folder, String prefix, ArrayList vChildren, char[][] exclusionPatterns) throws JavaModelException {
	IPackageFragment pkg = getPackageFragment(prefix);
	vChildren.add(pkg);
	try {
		IPath outputLocationPath = getJavaProject().getOutputLocation();
		IResource[] members = folder.members();
		for (int i = 0, max = members.length; i < max; i++) {
			IResource member = members[i];
			String memberName = member.getName();
			if (member.getType() == IResource.FOLDER 
				&& Util.isValidFolderNameForPackage(memberName)
				&& !Util.isExcluded(member, exclusionPatterns)) {
					
				String newPrefix;
				if (prefix.length() == 0) {
					newPrefix = memberName;
				} else {
					newPrefix = prefix + "." + memberName; //$NON-NLS-1$
				}
				// eliminate binary output only if nested inside direct subfolders
				if (!member.getFullPath().equals(outputLocationPath)) {
					computeFolderChildren((IFolder) member, newPrefix, vChildren, exclusionPatterns);
				}
			}
		}
	} catch(IllegalArgumentException e){
		throw new JavaModelException(e, IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST); // could be thrown by ElementTree when path is not found
	} catch (CoreException e) {
		throw new JavaModelException(e);
	}
}

/**
 * Returns a new element info for this element.
 */
protected OpenableElementInfo createElementInfo() {
	return new PackageFragmentRootInfo();
}

/**
 * @see IPackageFragmentRoot
 */
public IPackageFragment createPackageFragment(String name, boolean force, IProgressMonitor monitor) throws JavaModelException {
	CreatePackageFragmentOperation op = new CreatePackageFragmentOperation(this, name, force);
	runOperation(op, monitor);
	return getPackageFragment(name);
}

/**
 * Returns the root's kind - K_SOURCE or K_BINARY, defaults
 * to K_SOURCE if it is not on the classpath.
 *
 * @exception NotPresentException if the project and root do
 * 		not exist.
 */
protected int determineKind(IResource underlyingResource) throws JavaModelException {
	IClasspathEntry[] entries= ((JavaProject)getJavaProject()).getExpandedClasspath(true);
	for (int i= 0; i < entries.length; i++) {
		IClasspathEntry entry= entries[i];
		if (entry.getPath().equals(underlyingResource.getFullPath())) {
			return entry.getContentKind();
		}
	}
	return IPackageFragmentRoot.K_SOURCE;
}

/**
 * Compares two objects for equality;
 * for <code>PackageFragmentRoot</code>s, equality is having the
 * same <code>JavaModel</code>, same resources, and occurrence count.
 *
 */
public boolean equals(Object o) {
	if (this == o)
		return true;
	if (!(o instanceof PackageFragmentRoot))
		return false;
	PackageFragmentRoot other = (PackageFragmentRoot) o;
	return getJavaModel().equals(other.getJavaModel()) && 
			fResource.equals(other.fResource) &&
			fOccurrenceCount == other.fOccurrenceCount;
}

/**
 * @see IJavaElement
 */
public boolean exists() {
	return super.exists() 
				&& isOnClasspath();
}

public IClasspathEntry findSourceAttachmentRecommendation() {
	try {
		IPath rootPath = this.getPath();
		IClasspathEntry entry;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		// try on enclosing project first
		JavaProject parentProject = (JavaProject) getJavaProject();
		try {
			entry = parentProject.getClasspathEntryFor(rootPath);
			if (entry != null){
				Object target = JavaModel.getTarget(workspaceRoot, entry.getSourceAttachmentPath(), true);
				if (target instanceof IFile){
					IFile file = (IFile) target;
					if (Util.isArchiveFileName(file.getName())){
						return entry;
					}
				} else if (target instanceof IFolder) {
					return entry;
				}
				if (target instanceof java.io.File){
					java.io.File file = (java.io.File) target;
					if (file.isFile()) {
						if (Util.isArchiveFileName(file.getName())){
							return entry;
						}
					} else {
						// external directory
						return entry;
					}
				}
			}
		} catch(JavaModelException e){
		}
		
		// iterate over all projects
		IJavaModel model = getJavaModel();
		IJavaProject[] jProjects = model.getJavaProjects();
		for (int i = 0, max = jProjects.length; i < max; i++){
			JavaProject jProject = (JavaProject) jProjects[i];
			if (jProject == parentProject) continue; // already done
			try {
				entry = jProject.getClasspathEntryFor(rootPath);
				if (entry != null){
					Object target = JavaModel.getTarget(workspaceRoot, entry.getSourceAttachmentPath(), true);
					if (target instanceof IFile){
						IFile file = (IFile) target;
						if (Util.isArchiveFileName(file.getName())){
							return entry;
						}
					} else if (target instanceof IFolder) {
						return entry;
					}
					if (target instanceof java.io.File){
						java.io.File file = (java.io.File) target;
						if (file.isFile()) {
							if (Util.isArchiveFileName(file.getName())){
								return entry;
							}
						} else {
							// external directory
							return entry;
						}
					}
				}
			} catch(JavaModelException e){
			}
		}
	} catch(JavaModelException e){
	}

	return null;
}

/*
 * Returns the exclusion patterns from the classpath entry associated with this root. */
char[][] fullExclusionPatternChars() {
	try {
		ClasspathEntry entry = (ClasspathEntry)getRawClasspathEntry();
		if (entry == null) {
			return null;
		} else {
			return entry.fullExclusionPatternChars();
		}
	} catch (JavaModelException e) { 
		return null;
	}
}		

/**
 * @see Openable
 */
protected boolean generateInfos(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
	
	((PackageFragmentRootInfo) info).setRootKind(determineKind(underlyingResource));
	return computeChildren(info);
}

/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_PACKAGEFRAGMENTROOT;
}

/**
 * @see IPackageFragmentRoot
 */
public int getKind() throws JavaModelException {
	return ((PackageFragmentRootInfo)getElementInfo()).getRootKind();
}

/**
 * Returns an array of non-java resources contained in the receiver.
 */
public Object[] getNonJavaResources() throws JavaModelException {
	return ((PackageFragmentRootInfo) getElementInfo()).getNonJavaResources(getJavaProject(), getResource(), this);
}

/**
 * @see IPackageFragmentRoot
 */
public IPackageFragment getPackageFragment(String packageName) {
	if (packageName.indexOf(' ') != -1) { // tolerate package names with spaces (e.g. 'x . y') (http://bugs.eclipse.org/bugs/show_bug.cgi?id=21957)
		char[][] compoundName = Util.toCompoundChars(packageName);
		StringBuffer buffer = new StringBuffer(packageName.length());
		for (int i = 0, length = compoundName.length; i < length; i++) {
			buffer.append(CharOperation.trim(compoundName[i]));
			if (i != length-1) {
				buffer.append('.');
			}
		}
		packageName = buffer.toString();
	}
	return new PackageFragment(this, packageName);
}

/**
 * Returns the package name for the given folder
 * (which is a decendent of this root).
 */
protected String getPackageName(IFolder folder) throws JavaModelException {
	IPath myPath= getPath();
	IPath pkgPath= folder.getFullPath();
	int mySegmentCount= myPath.segmentCount();
	int pkgSegmentCount= pkgPath.segmentCount();
	StringBuffer name = new StringBuffer(IPackageFragment.DEFAULT_PACKAGE_NAME);
	for (int i= mySegmentCount; i < pkgSegmentCount; i++) {
		if (i > mySegmentCount) {
			name.append('.');
		}
		name.append(pkgPath.segment(i));
	}
	return name.toString();
}

/**
 * @see IJavaElement
 */
public IPath getPath() {
	return fResource.getFullPath();
}

/*
 * @see IPackageFragmentRoot 
 */
public IClasspathEntry getRawClasspathEntry() throws JavaModelException {
	IPath path= this.getPath();
	IClasspathEntry[] entries= this.getJavaProject().getRawClasspath();
	for (int i= 0; i < entries.length; i++) {
		IClasspathEntry entry = entries[i];
	
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_PROJECT:
				// a root's project always refers directly to the root
				// no need to follow the project reference
				continue;
			case IClasspathEntry.CPE_CONTAINER:
				IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), this.getJavaProject());
				if (container != null){
					IClasspathEntry[] containerEntries = container.getClasspathEntries();
					for (int j = 0; j < containerEntries.length; j++){
						IClasspathEntry containerEntry = JavaCore.getResolvedClasspathEntry(containerEntries[j]);
						if (containerEntry != null && path.equals(containerEntry.getPath())) {
							return entry; // answer original entry
						}
					}
				}
				break;
			case IClasspathEntry.CPE_VARIABLE:
				entry = JavaCore.getResolvedClasspathEntry(entry);
				// don't break so as to run default
			default:
				if (entry != null && path.equals(entry.getPath())) {
					return entries[i];
				}
		}
	}
	return null;
}

/*
 * @see IJavaElement
 */
public IResource getResource() {
	return fResource;
}

/**
 * @see IPackageFragmentRoot
 */
public IPath getSourceAttachmentPath() throws JavaModelException {
	if (getKind() != K_BINARY) return null;
	
	String serverPathString= getSourceAttachmentProperty();
	if (serverPathString == null) {
		return null;
	}
	int index= serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
	if (index < 0) return null;
	String serverSourcePathString= serverPathString.substring(0, index);
	return new Path(serverSourcePathString);
}

/**
 * Returns the server property for this package fragment root's
 * source attachement.
 */
protected String getSourceAttachmentProperty() throws JavaModelException {
	String propertyString = null;
	QualifiedName qName= getSourceAttachmentPropertyName();
	try {
		propertyString = getWorkspace().getRoot().getPersistentProperty(qName);
		
		// if no existing source attachment information, then lookup a recommendation from classpath entries
		if (propertyString == null || propertyString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER) < 0){
			IClasspathEntry recommendation = findSourceAttachmentRecommendation();
			if (recommendation != null){
				propertyString = recommendation.getSourceAttachmentPath().toString() 
									+ ATTACHMENT_PROPERTY_DELIMITER 
									+ (recommendation.getSourceAttachmentRootPath() == null ? "" : recommendation.getSourceAttachmentRootPath().toString()); //$NON-NLS-1$
				setSourceAttachmentProperty(propertyString);
			}
		}
		return propertyString;
	} catch (CoreException ce) {
		throw new JavaModelException(ce);
	}
}
	
public void setSourceAttachmentProperty(String property){
	try {
		getWorkspace().getRoot().setPersistentProperty(this.getSourceAttachmentPropertyName(), property);
	} catch (CoreException ce) {
	}
}

/**
 * For use by <code>AttachSourceOperation</code> only.
 * Sets the source mapper associated with this root.
 */
public void setSourceMapper(SourceMapper mapper) throws JavaModelException {
	((PackageFragmentRootInfo) getElementInfo()).setSourceMapper(mapper);
}

/**
 * Returns the qualified name for the source attachment property
 * of this root.
 */
protected QualifiedName getSourceAttachmentPropertyName() throws JavaModelException {
	return new QualifiedName(JavaCore.PLUGIN_ID, "sourceattachment: " + this.getPath().toOSString()); //$NON-NLS-1$
}

/**
 * @see IPackageFragmentRoot
 */
public IPath getSourceAttachmentRootPath() throws JavaModelException {
	if (getKind() != K_BINARY) return null;
	
	String serverPathString= getSourceAttachmentProperty();
	if (serverPathString == null) {
		return null;
	}
	int index= serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
	String serverRootPathString= IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH;
	if (index != serverPathString.length() - 1) {
		serverRootPathString= serverPathString.substring(index + 1);
	}
	return new Path(serverRootPathString);
}

/**
 * @see JavaElement
 */
public SourceMapper getSourceMapper() {
	try {
		return ((PackageFragmentRootInfo) getElementInfo()).getSourceMapper();
	} catch (JavaModelException e) {
		return null;
	}
}

/**
 * @see IJavaElement
 */
public IResource getUnderlyingResource() throws JavaModelException {
	if (!exists()) throw newNotPresentException();
	return fResource;
}

public int hashCode() {
	return fResource.hashCode();
}

/**
 * @see IPackageFragmentRoot
 */
public boolean isArchive() {
	return false;
}

/**
 * @see IPackageFragmentRoot
 */
public boolean isExternal() {
	return false;
}

/*
 * Returns whether this package fragment root is on the classpath of its project.
 */
protected boolean isOnClasspath() {
	if (this.getElementType() == IJavaElement.JAVA_PROJECT){
		return true;
	}
	
	IPath path = this.getPath();
	try {
		// check package fragment root on classpath of its project
		IJavaProject project = this.getJavaProject();
		IClasspathEntry[] classpath = project.getResolvedClasspath(true);	
		for (int i = 0, length = classpath.length; i < length; i++) {
			IClasspathEntry entry = classpath[i];
			if (entry.getPath().equals(path)) {
				return true;
			}
		}
	} catch(JavaModelException e){
		// could not read classpath, then assume it is outside
	}
	return false;
}

protected void openWhenClosed(IProgressMonitor pm) throws JavaModelException {
	if (!this.resourceExists() || !this.isOnClasspath()) throw newNotPresentException();
	super.openWhenClosed(pm);
	try {
		//restore any stored attached source
		IPath sourcePath= getSourceAttachmentPath();
		if (sourcePath != null) {
			IPath rootPath= getSourceAttachmentRootPath();
			attachSource(sourcePath, rootPath, pm);
		}
	} catch(JavaModelException e){ // no attached source
	}
}

/**
 * Recomputes the children of this element, based on the current state
 * of the workbench.
 */
public void refreshChildren() {
	try {
		OpenableElementInfo info= (OpenableElementInfo)getElementInfo();
		computeChildren(info);
	} catch (JavaModelException e) {
		// do nothing.
	}
}

/*
 * @see JavaElement#rootedAt(IJavaProject)
 */
public IJavaElement rootedAt(IJavaProject project) {
	return
		new PackageFragmentRoot(
			fResource,
			project, 
			fName);
}

/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info) {
	buffer.append(this.tabString(tab));
	if (getElementName().length() == 0) {
		buffer.append("[project root]"); //$NON-NLS-1$
	} else {
		buffer.append(getElementName());
	}
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	}
}

/**
 * Possible failures: <ul>
 *  <li>RELATIVE_PATH - the path supplied to this operation must be
 *      an absolute path
 *  <li>ELEMENT_NOT_PRESENT - the root supplied to the operation
 *      does not exist
 * </ul>
 */
protected void verifyAttachSource(IPath sourcePath) throws JavaModelException {
	if (!exists()) {
		throw newNotPresentException();
	} else if (sourcePath != null && !sourcePath.isAbsolute()) {
		throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.RELATIVE_PATH, sourcePath));
	}
}

}
