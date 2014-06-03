package com.github.leonardocouto;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Install node if this version is not already installed.
 * Also execute global npm installs (if not already installed)
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.NONE)
public class InstallMojo extends AbstractMojo {
	
    @Parameter(property = "npm", defaultValue = "")
    private String[] npm;
    
    @Parameter(property = "target", defaultValue = "${basedir}/gen")
    private File target;

    @Parameter(property = "node-os", defaultValue = "linux")
    private String os;

    @Parameter(property = "node-arch")
    private String arch;
    
    @Parameter(property = "binary-group", defaultValue = "${project.groupId}")
    private String binaryGroup;
    
    @Parameter(property = "binary-artifact", defaultValue = "nodejs-binaries")
    private String binaryArtifact;
    
    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor plugin;
    
    @Override
	public void execute() throws MojoExecutionException {
    	Log logger = getLog();
    	logger.error("*********************************************************************");
    	logger.error("*********************************************************************");

    	logger.error("npm: " + ((npm == null) ? "null" : Arrays.toString(this.npm)));
    	logger.error("target: " + ((target == null) ? "null" : target.toString()));
    	logger.error("os: " + this.os);
    	logger.error("arch: " + targetArch());
    	logger.error("binary-group: " + ((binaryGroup == null) ? "null" : binaryGroup));
    	logger.error("binary-artifact: " + ((binaryArtifact == null) ? "null" : binaryArtifact));
    	
    	Artifact binaryArtifact = this.binaryArtifact();
    	String version = binaryArtifact.getVersion();
    	
    	logger.error("binaryVersion: " + version);
    	
    	logger.error("*********************************************************************");
    	logger.error("*********************************************************************");
    	
    	if (!this.alreadyInstalled(version)) {
    		this.install(binaryArtifact);
    	}
    	
    	List<String> packages = this.nodePackages(version);
    	for (String pkg : this.npm) {
    		if (!packages.contains(pkg)) {
    			this.installPackage(pkg, version);
    		}
    	}
	}
    
    private void installPackage(String pkg, String nodeVersion) {
    	
    	Log logger = getLog();
    	logger.error("************************ INSTALANDO " + pkg + "*********************************************");
    	logger.error("*********************************************************************");
    	
    	String npmPath = this.npmPath(nodeVersion);
    	String install = npmPath + " install -g " + pkg;
    	Runtime run = Runtime.getRuntime();
    	try {
			Process process = run.exec(install);
			InputStreamReader reader = new InputStreamReader(process.getErrorStream());
			BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while (line != null) {
				logger.error(line);
				line = br.readLine();
			}
			br.close();
			
			reader = new InputStreamReader(process.getInputStream());
			br = new BufferedReader(reader);
			line = br.readLine();
			while (line != null) {
				logger.warn(line);
				line = br.readLine();
			}
			br.close();
			
		} catch (IOException e) {
			new MojoExecutionException("Problems installing node package " + pkg, e);		
		}
    	
    }
    
    private List<String> nodePackages(String version) {
    	String template = "%s/%s/lib/node_modules";
		String path = String.format(template, this.target.getPath(), installationDirectoryName(version));
		File nodeModules = new File(path);
		
		List<String> packages = new ArrayList<>();
		for (File f : nodeModules.listFiles()) {
			packages.add(f.getName());
		}
		return packages;
    }

	private void install(Artifact binaryArtifact) {
    	this.target.mkdirs();
    	
    	File file = binaryArtifact.getFile();
    	TarGZipUnArchiver unarchiver = new TarGZipUnArchiver(file);
    	
    	unarchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "console"));
    	unarchiver.setDestDirectory(this.target);
    	unarchiver.extract();
    	this.renameExtracted(binaryArtifact.getVersion());
    }
    
    private void renameExtracted(String version) {
    	String path = this.target.getPath();
    	
        for (File f : this.target.listFiles()) {
        	if (Files.isDirectory(f.toPath()) && (f.getName().startsWith("node-v"))) {
				String newName = path + "/" +  this.installationDirectoryName(version);
        		f.renameTo(new File(newName));
        		return;
            }
        }
    }

    private boolean alreadyInstalled(String version) throws MojoExecutionException {
    	if (!this.target.exists()) return false;
    	
    	if (this.target.isFile()) {
    		String error = String.format("target '%s' is a file", this.target.toString());
			throw new MojoExecutionException(error);
    	}
    	
    	for (File f : this.target.listFiles()) {
    		if (Files.isDirectory(f.toPath())) {
    			String name = f.getName();
    			if (this.installationDirectoryName(version).equals(name)) {
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    private Artifact binaryArtifact() {
    	String key = this.binaryGroup + ":" + this.binaryArtifact;
    	return this.plugin.getArtifactMap().get(key);
    }
    
//    private String nodePath(String version) {
//    	return this.baseExecutablePath(version) + "/node";
//    }
    
    private String npmPath(String version) {
    	return this.baseExecutablePath(version) + "/npm";
    }
    
    private String baseExecutablePath(String version) {
    	String template = "%s/%s/bin/";
    	return String.format(template, this.target.getPath(), this.installationDirectoryName(version));
    }
    
    private String installationDirectoryName(String version) {
    	return "node-v" + version;
    }
    
    private String targetArch() {
    	if (this.arch != null) {
    		return this.arch.toLowerCase();
    		
    	} else {
    		boolean x64 = System.getProperty("os.arch").contains("64");
    		return x64 ? "x64" : "x86";
    	}
    }
    
}
