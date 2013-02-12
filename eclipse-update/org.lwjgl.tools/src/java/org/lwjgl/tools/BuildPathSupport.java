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
import org.osgi.framework.Constants;

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

	public static class LWJGLPluginDescription {
		private final String strBundleId;

		private Bundle bundle = null;

		public LWJGLPluginDescription(String bundleId) {
			strBundleId = bundleId;

		}

		public Bundle getBundle() {
			if (bundle == null)
				bundle = Platform.getBundle(strBundleId);
			return bundle;
		}

		public String getBundleId() {
			return strBundleId;
		}
	}

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

	private static LibPathEntryData libdata(String jarLocation,
			IPath srcLocation, String docLocation, String nativeLocation) {
		return new LibPathEntryData(LWJGL_LIBS.append(jarLocation),
				srcLocation, docLocation, nativeLocation);
	}

	private static LibPathEntryData libdata(String jarLocation) {
		return new LibPathEntryData(LWJGL_LIBS.append(jarLocation), null, null,
				null);
	}

	public static final LWJGLPluginDescription LWJGL_PLUGIN = new LWJGLPluginDescription(
			"org.lwjgl"); //$NON-NLS-1$

	public static final LWJGLPluginDescription LWJGL_SRC_PLUGIN = new LWJGLPluginDescription(
			"org.lwjgl.source"); //$NON-NLS-1$

	public static final LWJGLPluginDescription LWJGL_DOC_PLUGIN = new LWJGLPluginDescription(
			"org.lwjgl.doc"); //$NON-NLS-1$

	public static IPath LWJGL_LIBS = getBundleLocation(LWJGL_PLUGIN).append(
			"lib");

	public static final LibPathEntryData[] LIBS = new LibPathEntryData[] {
			libdata("lwjgl.jar", getBundleLocation(LWJGL_SRC_PLUGIN),
					getBundleLocation(LWJGL_DOC_PLUGIN).toOSString(),
					getNativeLocation()),
			libdata("lwjgl_util.jar", getBundleLocation(LWJGL_SRC_PLUGIN),
					null, null),
			libdata("lwjgl_util_applet.jar",
					getBundleLocation(LWJGL_SRC_PLUGIN), null, null),
			libdata("jinput.jar", null, null, getNativeLocation()),
			libdata("jutils.jar"), libdata("lzma.jar"),
			libdata("asm-debug-all.jar"), libdata("AppleJavaExtensions.jar") };

	public static IPath getBundleLocation(LWJGLPluginDescription pluginDesc) {
		Bundle bundle = pluginDesc.getBundle();
		if (bundle == null)
			return null;

		URL local = null;
		try {
			local = FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
		String fullPath = new File(local.getPath()).getAbsolutePath();
		return Path.fromOSString(fullPath);
	}

	public static IPath getLWJGLSourceLocation() {
		Bundle bundleSrc = LWJGL_SRC_PLUGIN.getBundle();
		if (bundleSrc == null)
			return null;

		String version = (String) bundleSrc.getHeaders().get(
				Constants.BUNDLE_VERSION);
		if (version == null) {
			return null;
		}

		String bundlePath = getURL(bundleSrc);
		if (bundlePath == null) {
			return null;
		}

		File bundleLoc = new File(bundlePath);
		if (bundleLoc.isDirectory()) {
			String fullPath = bundleLoc.getAbsolutePath();
			return Path.fromOSString(fullPath);
		} else if (bundleLoc.isFile()) {
			return Path.fromOSString(bundleLoc.getAbsolutePath());
		}

		return null;
	}

	public static String getLWJGLJavadocLocation(String filename) {
		Bundle bundleDoc = LWJGL_DOC_PLUGIN.getBundle();
		if (bundleDoc == null)
			return null;

		String version = (String) bundleDoc.getHeaders().get(
				Constants.BUNDLE_VERSION);
		if (version == null) {
			return null;
		}

		String bundlePath = getURL(bundleDoc);
		if (bundlePath == null) {
			return null;
		}

		File bundleLoc = new File(bundlePath);
		if (bundleLoc.isDirectory()) {
			String fullPath = "jar:file:" + bundleLoc.getAbsolutePath() + "!"
					+ filename;
			return fullPath;
		} else if (bundleLoc.isFile()) {
			return bundleLoc.getAbsolutePath();
		}

		return null;
	}

	public static String getNativeLocation() {

		String basePath;
		try {
			basePath = LibraryPathUtil.getRelativeLWJGLLibraryPath();
		} catch (OSNotSupportedException ex) {
			log.warning(ex.toString()); //$NON-NLS-1$
			return null;
		}

		Bundle bundle = LWJGL_PLUGIN.getBundle();
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

	public static IClasspathEntry getLWJGLClasspathEntry() {
		return JavaCore
				.newContainerEntry(LWJGLClasspathContainerInitializer.LWJGL_LIBRARY_PATH);
	}

	public static IClasspathEntry[] getLWJGLLibraryEntries() {
		IPath bundleBase = getBundleLocation(LWJGL_PLUGIN);
		if (bundleBase != null) {
			IClasspathEntry[] entries = new IClasspathEntry[LIBS.length];
			for (int i = 0; i < LIBS.length; i++) {
				IAccessRule[] accessRules = {};
				IClasspathAttribute[] attributes = { //
						JavaCore.newClasspathAttribute(
								IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
								LIBS[i].docLocation),
						JavaCore.newClasspathAttribute(
								JavaRuntime.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY,
								LIBS[i].nativeLocation) };

				entries[i] = JavaCore.newLibraryEntry(LIBS[i].jarLocation, LIBS[i].srcLocation,
						null, accessRules, attributes, false);
			}
			return entries;

		}
		return null;
	}
}
