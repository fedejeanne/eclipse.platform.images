/*******************************************************************************
 * (c) Copyright 2016, 2025 l33t labs LLC and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     l33t labs LLC and others - initial contribution
 *******************************************************************************/

package org.eclipse.images.renderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Creates an icon CSS theme based on the current "stock" theme.
 */
@Mojo(name="create-css-theme")
@Execute(goal="create-css-theme", phase = LifecyclePhase.GENERATE_RESOURCES)
public class CreateCSSThemeMojo extends AbstractMojo {

	/** Maven logger */
	Log log;

	/** */
	private String newThemeName;

	/** */
	private String targetDir;

	/** */
	private File styleDirectoryRoot;

	/**
	 * 
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		log = getLog();

		newThemeName = System.getProperty("eclipse.svg.newThemeName");
		if (newThemeName == null) {
			throw new MojoExecutionException("Undefined theme name, specify -Declipse.svg.newThemeName=themeName");
		}

		targetDir = "eclipse-css";

		styleDirectoryRoot = new File(targetDir + "/");

		if (!styleDirectoryRoot.exists()) {
			throw new MojoExecutionException("Source directory' " + targetDir + "' does not exist.");
		}

		Arrays.stream(styleDirectoryRoot.listFiles()).filter(this::isThemeFile).forEach(this::generateThemeDir);

	}

	/**
	 * 
	 * @param file
	 * @return
	 */
	private boolean isThemeFile(File file) {
		return !file.isDirectory() || !"styles".equals(file.getName());
	}

	/**
	 * 
	 * @param file
	 */
	private void generateThemeDir(File file) {
		String dirName = file.getName();

		// Where to place the rendered icon
		String child = dirName;
		File outputBase = new File(targetDir, child);

		File styleDir = new File(outputBase, "styles/");
		File stockDir = new File(styleDir, "stock/");

		if (!stockDir.exists()) {
			log.error("Source dir doesn't exist: " + stockDir.getAbsolutePath());
			return;
		}

		File newThemeDir = new File(styleDir, newThemeName + "/");

		if (newThemeDir.exists() && !newThemeDir.delete()) {
			log.error("Error deleting existing theme directory.");
			return;
		}

		try {
			FileUtils.copyDirectory(stockDir, newThemeDir);

			renameImports(newThemeDir, newThemeName);

			File mainThemeFile = new File(styleDirectoryRoot, "/styles/stock.scss");
			File newThemeFile = new File(styleDirectoryRoot, "/styles/" + newThemeName + ".scss");

			String stockStr = Files.readString(mainThemeFile.toPath());
			stockStr = stockStr.replaceAll("Stock.scss provides the original Eclipse icon styles.",
					newThemeName + ".scss <enter description here>");
			Files.writeString(newThemeFile.toPath(), stockStr);
		} catch (IOException e) {
			log.error("Error creating new theme directory: " + e.getMessage(), e);
		}
	}

	/**
	 * 
	 * @param newThemeDir
	 */
	private void renameImports(File newThemeDir, String newThemeName) {
		for (File file : newThemeDir.listFiles()) {
			if (file.isDirectory()) {
				renameImports(file, newThemeName);
				continue;
			}

			if (file.getName().endsWith(".scss")) {
				try {
					String stockStr = new String(Files.readAllBytes(file.toPath()));
					stockStr = stockStr.replaceAll("@import \"stock\"", "@import \"" + newThemeName + "\"");
					Files.writeString(file.toPath(), stockStr);
				} catch (IOException e) {
					log.error("Error creating theme icon style: " + e.getMessage(), e);
				}
			}
		}
	}

}
