/*******************************************************************************
 * Copyright (c) 2011 LWJGL Project and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html, and under the terms of the 
 * BSD license, see http://lwjgl.org/license.php for details.
 *
 * Contributors:
 *    Jens von Pilgrim - initial implementation
 ******************************************************************************/
package org.lwjgl.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.lwjgl.plugin.LibraryPathUtil;
import org.lwjgl.plugin.OSNotSupportedException;
import org.osgi.framework.Bundle;

/**
 * This class resolves the actual LWJGL library path, that is the location of
 * the jars in the org.lwjgl plugin.
 * 
 * Following the "monkey sees, monkey does"-rule, this code was copied and
 * modified from the plugin org.eclipse.jdt.junit . This code is published under
 * the EPL and (c) by IBM and others.
 * 
 * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport
 * @see org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport
 */
@SuppressWarnings("restriction")
public class BuildPathSupport {
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(BuildPathSupport.class
			.getName());

	public static final String LWJGL_PLUGIN_ID = "org.lwjgl";
	public static final String LWJGL_SOURCE_PLUGIN_ID = "org.lwjgl.source";
	public static final String LWJGL_DOC_PLUGIN_ID = "org.lwjgl.doc";

	private static class LibPathEntryData {
		public final IPath jarLocation;
		public final IPath srcLocation;
		public final String docLocation;
		public final String nativeLocation;

		public LibPathEntryData(IPath jarLocation, IPath srcLocation,
				String docLocation, String nativeLocation) {
			super();
			this.jarLocation = jarLocation;
			this.srcLocation = srcLocation;
			this.docLocation = docLocation;
			this.nativeLocation = nativeLocation;
		}

	}

	private static LibPathEntryData libdata(IPath LWJGL_LIBS,
			String jarLocation, IPath srcLocation, String docLocation,
			String nativeLocation) {
		return new LibPathEntryData(LWJGL_LIBS.append(jarLocation),
				srcLocation, docLocation, nativeLocation);
	}

	private static LibPathEntryData libdata(IPath LWJGL_LIBS, String jarLocation) {
		return new LibPathEntryData(LWJGL_LIBS.append(jarLocation), null, null,
				null);
	}

	public final static LibPathEntryData[] createLibData() {

		String lwjglPath = getBundleLocation(LWJGL_PLUGIN_ID);
		if (lwjglPath == null) {
			return null;
		}
		String lwjglSrcPath = lwjglPath.replaceFirst(LWJGL_PLUGIN_ID,
				LWJGL_SOURCE_PLUGIN_ID)+".jar";
		String lwjglDocPath = lwjglPath.replaceFirst(LWJGL_PLUGIN_ID,
				LWJGL_DOC_PLUGIN_ID)+".jar";

		IPath lwjPath = Path.fromOSString(lwjglPath);
		IPath srcPath = Path.fromOSString(lwjglSrcPath);
		IPath docPath = Path.fromOSString(lwjglDocPath);
		IPath libPath = lwjPath.append("libs");

		if (!srcPath.toFile().exists()) {
			srcPath = null;
		}
		if (!docPath.toFile().exists()) {
			lwjglDocPath = null;
		} else {
			lwjglDocPath = "jar:file:" + lwjglDocPath +"!/html/api";
		}

		String nativeLocation = getNativeLocation();

		return new LibPathEntryData[] {
				libdata(libPath, "lwjgl.jar", srcPath, lwjglDocPath,
						nativeLocation),
				libdata(libPath, "lwjgl_util.jar", srcPath, lwjglDocPath, null),
				libdata(libPath, "lwjgl_util_applet.jar", srcPath,
						lwjglDocPath, null),
				libdata(libPath, "jinput.jar", null, null, nativeLocation),
				libdata(libPath, "jutils.jar"), libdata(libPath, "lzma.jar"),
				libdata(libPath, "asm-debug-all.jar"),
				libdata(libPath, "AppleJavaExtensions.jar") };
	}

	private static String getBundleLocation(String symbolicName) {

		Bundle bundle = Platform.getBundle(symbolicName);
		if (bundle == null)
			return null;

		URL local = null;
		try {
			local = FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
		String fullPath = new File(local.getPath()).getAbsolutePath();
		return fullPath;
		// return Path.fromOSString(fullPath);
	}

	private static String getNativeLocation() {

		String basePath;
		try {
			basePath = LibraryPathUtil.getRelativeLWJGLLibraryPath();
		} catch (OSNotSupportedException ex) {
			log.warning(ex.toString()); //$NON-NLS-1$
			return null;
		}

		Bundle bundle = Platform.getBundle(LWJGL_PLUGIN_ID);
		if (bundle == null)
			return null;

		String bundlePath = getURL(bundle);
		if (bundlePath == null) {
			return null;
		}

		File bundleLoc = new File(bundlePath);
		if (bundleLoc.isDirectory()) {
			String fullPath = bundleLoc.getAbsolutePath() + File.separator
					+ basePath;
			return fullPath;
		} else if (bundleLoc.isFile()) {
			return null;
		}

		return null;
	}

	private static String getURL(Bundle bundle) {
		try {
			URL fileURL = FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
			if (fileURL != null) {
				return fileURL.getFile();
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	public static IClasspathEntry[] getLWJGLLibraryEntries() {

		LibPathEntryData[] libData = createLibData();
		if (libData == null) {
			return null;
		}
		IClasspathEntry[] entries = new IClasspathEntry[libData.length];
		for (int i = 0; i < libData.length; i++) {
			IAccessRule[] accessRules = {};
			ArrayList<IClasspathAttribute> attributes = new ArrayList<IClasspathAttribute>();
			if (libData[i].docLocation != null) {
				attributes.add(JavaCore.newClasspathAttribute(
						IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
						libData[i].docLocation));
			}
			if (libData[i].nativeLocation != null) {
				attributes.add(JavaCore.newClasspathAttribute(
						JavaRuntime.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY,
						libData[i].nativeLocation));
			}
			IClasspathAttribute[] ca = new IClasspathAttribute[attributes
					.size()];
			attributes.toArray(ca);
			entries[i] = JavaCore.newLibraryEntry(libData[i].jarLocation,
					libData[i].srcLocation, null, accessRules, ca, false);
		}
		return entries;

	}
}
