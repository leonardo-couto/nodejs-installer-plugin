package com.github.leonardocouto;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
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

    @Parameter(defaultValue = "${node.os}")
    private String os;

    @Parameter(defaultValue = "${node.arch}")
    private String arch;
    
    @Parameter(property = "binary-group", defaultValue = "${project.groupId}")
    private String binaryGroup;
    
    @Parameter(property = "binary-artifact", defaultValue = "nodejs-binaries")
    private String binaryArtifact;
    
    @Parameter(property = "binary-version", defaultValue = "${node.version}")
    private String binaryVersion;
    
    // TODO: os, arch e binversion estao vindo nulos
    
    @Component
    private PluginDescriptor plugin;
    
    @Override
	public void execute() throws MojoExecutionException {
    	Log logger = getLog();
    	logger.error("*********************************************************************");
    	logger.error("*********************************************************************");

    	logger.error("npm: " + ((npm == null) ? "null" : Arrays.toString(this.npm)));
    	logger.error("target: " + ((target == null) ? "null" : target.toString()));
    	logger.error("os: " + targetOs());
    	logger.error("arch: " + targetArch());
    	logger.error("binary-group: " + ((binaryGroup == null) ? "null" : binaryGroup));
    	logger.error("binary-artifact: " + ((binaryArtifact == null) ? "null" : binaryArtifact));
    	logger.error("binaryVersion: " + ((binaryVersion == null) ? "null" : binaryVersion.toString()));
    	
    	logger.error("*********************************************************************");
    	logger.error("*********************************************************************");
    	
    	if (!this.alreadyInstalled()) {
    		this.install();
    	}
    	
	}
    
    private void install() {
    	this.target.mkdirs();
    	
    	Artifact binaryArtifact = this.binaryArtifact();
    	File file = binaryArtifact.getFile();
    	
    	TarGZipUnArchiver unarchiver = new TarGZipUnArchiver(file);
    	
    	unarchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "console"));
    	unarchiver.setDestDirectory(this.target);
    	unarchiver.extract();
    	this.renameExtracted();
    }
    
    private void renameExtracted() {
    	String path = this.target.getPath();
    	
        for (File f : this.target.listFiles()) {
        	if (Files.isDirectory(f.toPath()) && (f.getName().startsWith("node-v"))) {
				String newName = path + "/" +  this.installationDirectoryName();
        		f.renameTo(new File(newName));
        		return;
            }
        }
    }

    private boolean alreadyInstalled() throws MojoExecutionException {
    	if (!this.target.exists()) return false;
    	
    	if (this.target.isFile()) {
    		String error = String.format("target '%s' is a file", this.target.toString());
			throw new MojoExecutionException(error);
    	}
    	
    	for (File f : this.target.listFiles()) {
    		if (Files.isDirectory(f.toPath())) {
    			String name = f.getName();
    			if (this.installationDirectoryName().equals(name)) {
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
    
    private String installationDirectoryName() {
    	return "node-v" + this.binaryVersion;
    }
    
    private String targetArch() {
    	if (this.arch != null) {
    		return this.arch.toLowerCase();
    		
    	} else {
    		boolean x64 = System.getProperty("os.arch").contains("64");
    		return x64 ? "x64" : "x86";
    	}
    }
    
    private String targetOs() {
    	if (this.os != null)  {
    		return this.os.toLowerCase();
    		
    	} else {
    		return "linux";    		
    	}
    }
    
    
}
